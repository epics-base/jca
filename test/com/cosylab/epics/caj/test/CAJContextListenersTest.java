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

package com.cosylab.epics.caj.test;

import java.net.InetAddress;
import java.util.Vector;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.event.ContextExceptionEvent;
import gov.aps.jca.event.ContextExceptionListener;
import gov.aps.jca.event.ContextMessageEvent;
import gov.aps.jca.event.ContextMessageListener;
import gov.aps.jca.event.ContextVirtualCircuitExceptionEvent;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;

import junit.framework.TestCase;

/**
 * CAJContext listeners (registration and deregistration) test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJContextListenersTest extends TestCase {

    /**
     * Dummy implementation of ContextMessageListener.
     */
    private class ContextMessageListenerImpl implements ContextMessageListener {
        
        /**
         * @see gov.aps.jca.event.ContextMessageListener#contextMessage(gov.aps.jca.event.ContextMessageEvent)
         */
        public void contextMessage(ContextMessageEvent ev) {
           // noop
        }
    }
    
    /**
     * Dummy implementation of ContextExceptioListener.
     */
    private class ContextExceptionListenerImpl implements ContextExceptionListener {
        
        /**
         * List of all received notifications.
         */
        private Vector notifications = new Vector();
        
        /**
         * Get list of all received notifications.
         * @return list of all received notifications.
         */
        public Vector getNotifications()
        {
            return notifications;
        }

        /**
         * @see gov.aps.jca.event.ContextExceptionListener#contextException(gov.aps.jca.event.ContextExceptionEvent)
         */
        public synchronized void contextException(ContextExceptionEvent ev) {
            notifications.add(ev);
            this.notifyAll();
        }
        
		/**
		 * @see gov.aps.jca.event.ContextExceptionListener#contextVirtualCircuitException(gov.aps.jca.event.ContextVirtualCircuitExceptionEvent)
		 */
		public synchronized void contextVirtualCircuitException(ContextVirtualCircuitExceptionEvent ev) {
            notifications.add(ev);
            this.notifyAll();
		}
    }

    /**
	 * Context to be tested.
	 */
	private CAJContext context;
	
	/**
	 * Constructor for CAJContextListenersTest.
	 * @param methodName
	 */
	public CAJContextListenersTest(String methodName) {
		super(methodName);
	}

	/**
	 * Register context message listener.
	 */
	public void testContextMessageListeners() throws CAException {
	    
	    try {
	        context.addContextMessageListener(null);
	        fail("null listener accepted");
	    } catch (IllegalArgumentException iae) {
	        // this is ok
		}
	    assertEquals(0, context.getContextMessageListeners().length);

	    ContextMessageListener cml1 = new ContextMessageListenerImpl();
	    context.addContextMessageListener(cml1);
	    assertEquals(1, context.getContextMessageListeners().length);
	    
	    ContextMessageListener cml2 = new ContextMessageListenerImpl();
	    context.addContextMessageListener(cml2);
	    assertEquals(2, context.getContextMessageListeners().length);
	    
	    // duplicate registration
	    context.addContextMessageListener(cml2);
	    assertEquals(2, context.getContextMessageListeners().length);

	    ContextMessageListener[] listeners = context.getContextMessageListeners();
	    assertTrue(listeners[0] == cml1);
	    assertTrue(listeners[1] == cml2);
	    

	    try {
	        context.removeContextMessageListener(null);
	        fail("null listener removed");
	    } catch (IllegalArgumentException iae) {
	        // this is ok
		}
	    assertEquals(2, context.getContextMessageListeners().length);
	    
	    context.removeContextMessageListener(cml1);
	    listeners = context.getContextMessageListeners();
	    assertEquals(1, listeners.length);
	    assertTrue(listeners[0] == cml2);

	    // removing twice (non registered) does not raise any error
	    context.removeContextMessageListener(cml1);
	    listeners = context.getContextMessageListeners();
	    assertEquals(1, listeners.length);
	    assertTrue(listeners[0] == cml2);

	    context.removeContextMessageListener(cml2);
	    assertEquals(0, context.getContextMessageListeners().length);
	}
	

	/**
	 * Register context Exception listener.
	 */
	public void testContextExceptionListeners() throws CAException {
	    
	    try {
	        context.addContextExceptionListener(null);
	        fail("null listener accepted");
	    } catch (IllegalArgumentException iae) {
	        // this is ok
		}
	    assertEquals(0, context.getContextExceptionListeners().length);

	    ContextExceptionListener cel1 = new ContextExceptionListenerImpl();
	    context.addContextExceptionListener(cel1);
	    assertEquals(1, context.getContextExceptionListeners().length);
	    
	    ContextExceptionListener cel2 = new ContextExceptionListenerImpl();
	    context.addContextExceptionListener(cel2);
	    assertEquals(2, context.getContextExceptionListeners().length);
	    
	    // duplicate registration
	    context.addContextExceptionListener(cel2);
	    assertEquals(2, context.getContextExceptionListeners().length);

	    ContextExceptionListener[] listeners = context.getContextExceptionListeners();
	    assertTrue(listeners[0] == cel1);
	    assertTrue(listeners[1] == cel2);
	    

	    try {
	        context.removeContextExceptionListener(null);
	        fail("null listener removed");
	    } catch (IllegalArgumentException iae) {
	        // this is ok
		}
	    assertEquals(2, context.getContextExceptionListeners().length);
	    
	    context.removeContextExceptionListener(cel1);
	    listeners = context.getContextExceptionListeners();
	    assertEquals(1, listeners.length);
	    assertTrue(listeners[0] == cel2);

	    // removing twice (non registered) does not raise any error
	    context.removeContextExceptionListener(cel1);
	    listeners = context.getContextExceptionListeners();
	    assertEquals(1, listeners.length);
	    assertTrue(listeners[0] == cel2);

	    context.removeContextExceptionListener(cel2);
	    assertEquals(0, context.getContextExceptionListeners().length);
	}

	/**
	 * Check context exception listener notification reception.
	 */
	public void testContextExceptionOnIOCDown() throws Throwable {
	    
		ContextExceptionListenerImpl cel1 = new ContextExceptionListenerImpl();
	    context.addContextExceptionListener(cel1);
	    assertEquals(1, context.getContextExceptionListeners().length);

	    Channel channel = context.createChannel("record1");
	    context.pendIO(5.0);
	    
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());

	    CAJChannel cajChannel = (CAJChannel)channel;

	    InetAddress vc = cajChannel.getTransport().getRemoteAddress().getAddress();
	    
	    // simulate ioc unresponsive
	    int prio = Thread.currentThread().getPriority();
	    // make it quite likely to happen
	    // so that responsiveness will not be detected too fast
	    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
	    try
		{
		    cajChannel.getTransport().timeout(0);
	    	cajChannel.getTransport().timeout(0);

	    	synchronized(cel1) {
	            if (cel1.getNotifications().size() == 0)
	                cel1.wait(3000);
	        }
	        
		    assertEquals(1, cel1.getNotifications().size());
		    ContextVirtualCircuitExceptionEvent cvcee = (ContextVirtualCircuitExceptionEvent)cel1.getNotifications().firstElement();
		    assertEquals(context, cvcee.getSource());
		    assertEquals(CAStatus.UNRESPTMO, cvcee.getStatus());
		    assertEquals(vc, cvcee.getVirtualCircuit());
		}
	    finally
		{
	    	Thread.currentThread().setPriority(prio);
		}
	    
	    
	    // simulate ioc down, force disconnect
	    cajChannel.getTransport().close(true);

	    synchronized(cel1) {
    	    if (cel1.getNotifications().size() == 1)
                cel1.wait(3000);
        }
        
	    assertEquals(2, cel1.getNotifications().size());
	    ContextVirtualCircuitExceptionEvent cvcee = (ContextVirtualCircuitExceptionEvent)cel1.getNotifications().lastElement();
	    assertEquals(context, cvcee.getSource());
	    assertEquals(CAStatus.DISCONN, cvcee.getStatus());
	    assertEquals(vc, cvcee.getVirtualCircuit());

	    channel.destroy();
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		context = new CAJContext();
		context.initialize();
		assertTrue(context.isInitialized());
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		if (!context.isDestroyed())
			context.destroy();
		context = null;
	}

	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(CAJContextListenersTest.class);
	}
}
