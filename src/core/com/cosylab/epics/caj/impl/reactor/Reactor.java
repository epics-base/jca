/*
 * Copyright (c) 2004 by Cosylab
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file "LICENSE-CAJ". If the license is not included visit Cosylab web site,
 * <http://www.cosylab.com>.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package com.cosylab.epics.caj.impl.reactor;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of reactor pattern using <code>java.nio.channels.Selector</code>.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class Reactor {

	// Get Logger
	private static final Logger logger = Logger.getLogger(Reactor.class.getName());
	
    /**
     * Simple internal request interface.
     */
    private interface InternalRequest {
        /**
         * Process the request.
         */
        public void process();
    }
    
	/**
	 * Registration request to selector. 
	 */
	private class RegistrationRequest implements InternalRequest
	{
		private SelectableChannel selectableChannel;
		private int interestOps;
		private ReactorHandler handler;
		
		private SelectionKey key = null; 
		private ClosedChannelException exception = null; 
		private boolean done = false;
		
		/**
		 * Contructor.
		 * @param selectableChannel
		 * @param interestOps
		 * @param handler
		 */
		public RegistrationRequest(SelectableChannel selectableChannel, int interestOps, ReactorHandler handler)
		{
			this.selectableChannel = selectableChannel;
			this.interestOps = interestOps;
			this.handler = handler;
		}
		
		/**
		 * Process the registration resulting set <code>key</code> or <code>exception</code> fields.
		 */
		public synchronized void process()
		{
			try
			{
				// do the registration...
				key = selectableChannel.register(selector, interestOps, handler);
			}
			catch (ClosedChannelException cce)
			{
				exception = cce;	
			}
			finally
			{
				// notify about completed registration process
				done = true;
				notifyAll();
			}
		}
		
		/**
		 * Cancel process of registration.
		 */
		public synchronized void cancelRegistration()
		{
			// notify about canceled registration process
			done = true;
			notifyAll();
		}

		/**
		 * Exception thrown during registration, <code>null</code> if none.
		 * @return exception thrown during registration.
		 */
		public ClosedChannelException getException() {
			return exception;
		}

		/**
		 * Obtained key, <code>null</code> on failure.
		 * @return obtained key.
		 */
		public SelectionKey getKey() {
			return key;
		}
		
		/**
		 * Checks if registration is done (or canceled).
		 * Note: note synced to this.
		 * @return
		 */
		public boolean isDone() {
			return done;
		}

	}

	/**
	 * Registration request to selector. 
	 */
	private class DeregistrationRequest implements InternalRequest
	{
		private SelectionKey key = null; 
		
		/**
		 * Contructor.
		 * @param key
		 */
		public DeregistrationRequest(SelectionKey key)
		{
			this.key = key;
		}
		
		/**
		 * Deregisters and closes.
		 */
		public void process() 
		{
			key.cancel();
			try {
                key.channel().close();
            } catch (IOException e) { /* noop */ }
		}
	}

	/**
	 * Chaning operation of interests request. 
	 */
	private class InterestOpsChangeRequest implements InternalRequest
	{
	    private SelectionKey selectionKey;
	    private int interestOps;
	    
        /**
         * @param selectionKey
         * @param interestOps
         */
        public InterestOpsChangeRequest(SelectionKey selectionKey, int interestOps) {
            this.selectionKey = selectionKey;
            this.interestOps = interestOps;
        }
        
        /**
         * Process change of interests ops.
         */
        public void process()
        {
            selectionKey.interestOps(interestOps);
        }
	}
	
	/**
	 * IO selector (e.g. UNIX select() implementation).
	 */
	private Selector selector;

	/**
	 * List of pending registration request(s).
	 */
	private LinkedList registrationRequests = new LinkedList(); 
	
	/**
	 * List of pending registration request(s).
	 */
	private LinkedList deregistrationRequests = new LinkedList(); 

	/**
	 * List of pending registration request(s).
	 */
	private LinkedList interestOpsChangeRequests = new LinkedList(); 

	/**
	 * Map of disabled keys, pairs (SelectionKey key, Integer interestOps). 
	 */
	private HashMap disabledKeys = new HashMap(); 

	/**
	 * Selector status.
	 */
	private AtomicInteger selectorPending = new AtomicInteger(0);

	/**
	 * Shutdown status.
	 */
	private volatile boolean shutdown = false;
	
	/**
	 * Shutdown monitor (condition variable).
	 */
	private volatile Object shutdownMonitor = new Object();

	/**
	 * Creates a new instance of reactor.
	 * @throws IOException
	 */  
	public Reactor() throws IOException {
		initialize();
	}

	/**
	 * Initialize reactor.
	 * @throws IOException
	 */
	private void initialize() throws IOException
	{
		// create an instance of selector
		selector = Selector.open();
	}

	/**
	 * Processes internal request list.
	 * Also takes care of sync.
	 * @param list	list of interal requests to be processed.
	 */
	private static void processInternalRequest(LinkedList list)
	{
		if (!list.isEmpty())
		{
			synchronized (list)
			{
				while (!list.isEmpty())
				{
					try
					{
						((InternalRequest)list.removeFirst()).process();
					}
					catch (Throwable th)
					{
						// noop (just not to lose control)
					}
				}
			}
		}
	}
	
	/**
	 * Process requests.
	 * NOTE: this method has to be called until <code>false</code> is returned.
	 * @return <code>true</code> if selector is stil active, <code>false</code> when shutdown. 
	 */
	public boolean process()
	{
		// do while reactor is open
		if (selector.isOpen() && !shutdown)
		{
		    processInternal();
		}
		
		// if closed, do the cleanup 
		if (!selector.isOpen() || shutdown)
		{
			// cancel pending registration requests
			if (!registrationRequests.isEmpty())
			{
				synchronized (registrationRequests)
				{
					while (!registrationRequests.isEmpty())
						((RegistrationRequest)registrationRequests.removeFirst()).cancelRegistration();
				}
			}

            // check pending deregistration requests (not to forget to close channels)
            processInternalRequest(deregistrationRequests);

            synchronized (shutdownMonitor)
            {
            	shutdownMonitor.notifyAll();
			}
            
            //System.out.println();
			//System.out.println("[Selector closed.]");
			
			return false;
		}
		else
		    return true;
	}
	
	/**
	 * Process requests (internal method).
	 * NOTE: Selector objects are thread-safe, but the key sets they contain are not. The key sets
	 * returned by the keys( ) and selectedKeys( ) methods are direct references to private
	 * Set objects inside the Selector object. These sets can change at any time.
	 */
	private void processInternal()
	{
		//System.err.println("[processInternal] " + Thread.currentThread().getName());

		try
		{
		
		    int numSelectedKeys = 0;
            while (numSelectedKeys == 0 && !shutdown)
            {

                // check pending deregistration requests
                processInternalRequest(deregistrationRequests);

                // check pending interestOps change requests
                processInternalRequest(interestOpsChangeRequests);

                //System.err.println("[select] " + Thread.currentThread().getName());

			    selectorPending.incrementAndGet();
			    try
			    {
		            // wait for selection, but only if necessary
	                /* int */numSelectedKeys = selector.selectedKeys().size();
	                if (numSelectedKeys == 0)
	                    numSelectedKeys = selector.select();
			    }
			    finally
			    {
			        selectorPending.decrementAndGet();
			    }
			    
                // check shutdown status
                if (shutdown)
                {
                    selector.close();
                    return;
                }

                // check pending registration requests
                processInternalRequest(registrationRequests);

            }
            
			// wakeup or forced selectNow()
			if (numSelectedKeys == 0 || shutdown)
				return; 

			//System.out.println();
			//System.out.println("[Selector selected # keys: " + numSelectedKeys + "]");

			Iterator selectedKeys = selector.selectedKeys().iterator();
			
			// process only one request per select (to support concurrent processing)
			SelectionKey selectedKey = (SelectionKey) selectedKeys.next();

			// the definition of OP_WRITE in select agrees with the Unix definition, ie. not edge triggered like Win32
			// this means that you must add and remove OP_WRITE from the interestOps depending on the actual ability to write
			// clear SelectionKey.OP_WRITE here...
			int ops = 0;
			try
			{
				ops = selectedKey.interestOps(); 
				if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE)
				    selectedKey.interestOps(ops & (~SelectionKey.OP_WRITE));
			} catch (CancelledKeyException cke) {
				// noop
			}
			
			// get handler as attachment
			ReactorHandler handler = (ReactorHandler) selectedKey.attachment();

			// request accepted, remove key from set
			// NOTE: this has to be done before processing (to support concurrent processing)
			selectedKeys.remove();
			
			// process request
			handler.handleEvent(selectedKey);
			
		}
		catch (Throwable th)
		{
			// TODO report exception
			logger.log(Level.SEVERE, "", th);
		}
		
		//System.err.println("[processInternal done] " + Thread.currentThread().getName());
	}
	
	/**
	 * Deregisters <code>SelectableChannel</code> from the reactor.
	 * @param selectableChannel	channel to be unregistered.
	 */
	public void unregisterAndClose(SelectableChannel selectableChannel)
	{
		SelectionKey key = selectableChannel.keyFor(selector);
		synchronized (deregistrationRequests)
		{
			deregistrationRequests.add(new DeregistrationRequest(key));
		}
		selector.wakeup();
	}

	/**
	 * Registers <code>SelectableChannel</code> to the reactor.
	 * @param selectableChannel	channel to be registered.
	 * @param interestOps	operations suppored by channel (i.e. operations of interest).
	 * @param handler		handle to process requests.
	 * @return selector selection key.
	 * @throws ClosedChannelException
	 */
	public SelectionKey register(SelectableChannel selectableChannel, int interestOps, ReactorHandler handler)
		throws ClosedChannelException
	{
		RegistrationRequest rr = new RegistrationRequest(selectableChannel, interestOps, handler);
		synchronized (rr)
		{
			// pend registration request... 
			synchronized (registrationRequests)
			{
				registrationRequests.add(rr);
			}

			// check for shutdown status
			// NOTE: this has to be done in synchronization block after registering request
			if (shutdown)
				throw new IllegalStateException("Reactor is already shutdown.");

			// ... and wakeup selector to have this registration valid immediately 
			selector.wakeup();

			// if already awake process request immediately,
			// this is race-condition proof, since wakeup quarantees
			// that thread will we awaken immediately and will not select
			if (selectorPending.get() == 0) 
				rr.process();
			else
			{
				// wait for completion
				try	{
					while (!rr.isDone())
						rr.wait();
				} catch(InterruptedException ie) { /* noop */ }
			}
		}

		// registration during shutdown
		if (shutdown)
			throw new IllegalStateException("Reactor is shutting down.");
		// exception occured during registration
		else if (rr.getException() != null)
			throw rr.getException();
		// return obtained key
		else
			return rr.getKey();
	}
	
	/**
	 * Change <code>SelectionKey</code> operations of interest.
	 * @param selectionKey 
	 * @param interestOps
	 */
	private void setInterestOpsInternal(SelectionKey selectionKey, int interestOps)
	{
	    // NOTE: interestOps blocks in W2K if there is a select active on the key
		// selectionKey.interestOps(interestOps);
	    synchronized (interestOpsChangeRequests)
        {
	        interestOpsChangeRequests.add(new InterestOpsChangeRequest(selectionKey, interestOps));
        }
		selector.wakeup(); 
	} 

	/**
	 * Disable selection key - sets its interest ops to 0 and stores its current interest ops (to be restored). 
	 * NOTE: to be called only (otherwise it can block) when select is not pending
	 * @param key	selection key to be disabled.
	 */
	public void disableSelectionKey(SelectionKey key) 
	{
	    synchronized (disabledKeys)
	    {
	        if (disabledKeys.containsKey(key))
	            return;
	    
	        // save current operations of interest
	        int interestOps = key.interestOps();
	        disabledKeys.put(key, new Integer(interestOps));

	        // disable the key
	        // since this can be called only when select is not pending, we can do this
	        key.interestOps(0);
	    }
	}

	/**
	 * Enable selection key - restore its interest ops. 
	 * NOTE: can be called when select is pending
	 * @param key	selection key to be enabled.
	 */
	public void enableSelectionKey(SelectionKey key) 
	{
	    synchronized (disabledKeys)
	    {
	        Integer ops = (Integer)disabledKeys.remove(key);
	        if (ops != null)
	            setInterestOpsInternal(key, ops.intValue());
	    }
	}

	/**
	 * Change <code>SelectionKey</code> operations of interest.
	 * @param channel 
	 * @param interestOps
	 */
	public void setInterestOps(AbstractSelectableChannel channel, int interestOps)
	{
	    SelectionKey key = channel.keyFor(selector);
	    if (key != null)
	        setInterestOps(key, interestOps);
	} 

	/**
	 * Change <code>SelectionKey</code> operations of interest.
	 * If channel is disabled, it changes its stored interest ops.
	 * @param selectionKey 
	 * @param interestOps
	 */
	private void setInterestOps(SelectionKey selectionKey, int interestOps)
	{
	    synchronized (disabledKeys)
	    {
	        Integer ops = (Integer)disabledKeys.get(selectionKey);
	        if (ops != null)
		        disabledKeys.put(selectionKey, new Integer(interestOps));
	        else
	            setInterestOpsInternal(selectionKey, interestOps);
	    }
	} 

	/**
	 * Shutdown the reactor.
	 */
	public void shutdown()
	{
		if (shutdown)
			return;
		
		synchronized (shutdownMonitor) {
			shutdown = true;
			selector.wakeup();
			
			try {
				shutdownMonitor.wait();
			} catch (InterruptedException e) { /* noop */ }
			
		}
	}
	
	/**
	 * Get shutdown status.
	 * @return shutdown status.
	 */
    public boolean isShutdown()
    {
        return shutdown;
    }
}

