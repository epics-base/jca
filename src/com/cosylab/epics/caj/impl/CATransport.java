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

package com.cosylab.epics.caj.impl;

import gov.aps.jca.CAStatus;
import gov.aps.jca.Context;
import gov.aps.jca.event.ContextExceptionListener;
import gov.aps.jca.event.ContextVirtualCircuitExceptionEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.reactor.ReactorHandler;
import com.cosylab.epics.caj.impl.reactor.lf.LeaderFollowersThreadPool;
import com.cosylab.epics.caj.impl.requests.EchoRequest;
import com.cosylab.epics.caj.impl.requests.EventsOffRequest;
import com.cosylab.epics.caj.impl.requests.EventsOnRequest;
import com.cosylab.epics.caj.util.Timer;

/**
 * CA transport implementation.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CATransport implements Transport, ReactorHandler, Timer.TimerRunnable {

	// Get Logger
	private static final Logger logger = Logger.getLogger(CATransport.class.getName());
	
	/**
	 * Connection status.
	 */
	private volatile boolean closed = false;

	/**
	 * Context instance.
	 */
	private CAJContext context;

	/**
	 * CA reponse handler.
	 */
	protected ResponseHandler responseHandler = null;

	/**
	 * Corresponding channel.
	 */
	private SocketChannel channel;

	/**
	 * Cached socket address.
	 */
	private InetSocketAddress socketAddress;

	/**
	 * Receive buffer.
	 */
	private ByteBuffer[] receiveBuffer;

	/**
	 * Flow control "buffer full" count limit.
	 */
	private final static int FLOW_CONTROL_BUFFER_FULL_COUNT_LIMIT = 4;

	/**
	 * Flow control status.
	 */
	private boolean flowControlActive = false;
	
	/**
	 * Local receive (socket) buffer.
	 */
	private ByteBuffer socketBuffer;
	
	/**
	 * Send queue.
	 */
	private LinkedList sendQueue;
	
	/**
	 * Remote side transport revision.
	 */
	private short remoteTransportRevision;

	/**
	 * Owners (users) of the transport.
	 */
	private Map owners;
	
	/**
	 * Send sync. object lock.
	 */
	private Object sendLock = new Object();

	/**
	 * Flu8sh pending status.
	 */
	private volatile boolean flushPending = false;
	
	/**
	 * Current active send buffer.
	 */
	private ByteBuffer sendBuffer;
	
	/**
	 * Byte buffer allocator.
	 */
	private CachedByteBufferAllocator bufferAllocator;

	/**
	 * Last active send buffer.
	 * This is used to possibly optimize high frequency flushes, it merges current buffer with the last 
	 * (if it there is enough of space and it is wainting in the sendQueue). 
	 */
	private ByteBuffer lastActiveSendBuffer = null;

	/**
	 * Process priority.
	 */
	protected short priority;

	/**
	 * @param context
	 * @param responseHandler
	 * @param client
	 * @param channel
	 * @param remoteTransportRevision
	 * @param priority
	 */
	public CATransport(CAJContext context, TransportClient client, ResponseHandler responseHandler,
					   SocketChannel channel, short remoteTransportRevision, short priority) {
		this.context = context;
		this.responseHandler = responseHandler;
		this.channel = channel;
		this.remoteTransportRevision = remoteTransportRevision;
		this.priority = priority;

		socketAddress = (InetSocketAddress)channel.socket().getRemoteSocketAddress();
		
		// initialize buffers
		receiveBuffer = new ByteBuffer[] {
							ByteBuffer.allocate(CAConstants.CA_EXTENDED_MESSAGE_HEADER_SIZE),
							ByteBuffer.allocate(Math.max(CAConstants.MAX_TCP_RECV, context.getMaxArrayBytes()))
						};
		// first limit to a reading of an standard message header
		receiveBuffer[0].limit(CAConstants.CA_MESSAGE_HEADER_SIZE);

		socketBuffer = ByteBuffer.allocate(CAConstants.MAX_TCP_RECV);
		
		
		// initialize owners list, send queue
		owners = new HashMap();
		acquire(client);

		sendQueue = new LinkedList();
		bufferAllocator = context.getCachedBufferAllocator();
		sendBuffer = bufferAllocator.get();
		
		// read beacon timeout and start timer (watchdog)
		connectionTimeout = (long)(context.getConnectionTimeout() * 1000);
		taskID = context.getTimer().executeAfterDelay(connectionTimeout, this);
		
		// add to registry
		context.getTransportRegistry().put(socketAddress, this);
	}
	
	/** 
	 * Close connection.
	 * @param forced	flag indicating if forced (e.g. forced disconnect) is required 
	 */
	public void close(boolean forced) {

		synchronized (this)
		{
			// already closed check
			if (closed)
				return;
			closed = true;
	
			Timer.cancel(taskID);
			
			// remove from registry
			context.getTransportRegistry().remove(socketAddress, priority);
		}

		// flush first
		if (!forced)
			flushInternal();
		
		freeSendBuffers();

		if (forced)
			closedNotifyContext();
		
		closedNotifyClients();
		
		context.getLogger().finer("Connection to " + socketAddress + " closed.");

		context.getReactor().unregisterAndClose(channel);
	}

	/**
	 * Free all send buffers (return them to the cached buffer allocator).
	 */
	private void freeSendBuffers() {
		synchronized (sendQueue) {
			sendBuffer = null;
			lastActiveSendBuffer = null;
			while (sendQueue.size() > 0)
			    bufferAllocator.put((ByteBuffer)sendQueue.removeFirst());
		}
	}
	
	/**
	 * Notifies context listeners about forceful disconnect.
	 */
	private void closedNotifyContext() {

		ContextVirtualCircuitExceptionEvent cvcee =
	    	new ContextVirtualCircuitExceptionEvent((Context)context, socketAddress.getAddress(), CAStatus.DISCONN);
	    ContextExceptionListener[] listeners = context.getContextExceptionListeners();
		for (int i = 0; i < listeners.length; i++)
		{
			try
			{
				listeners[i].contextVirtualCircuitException(cvcee);
			}
			catch (Throwable th)
			{
				// TODO remove
				logger.log(Level.SEVERE, "", th);
			}
		}
	}
	
	/**
	 * Notifies clients about disconnect.
	 */
	private void closedNotifyClients() {
		synchronized (owners)
		{
			// check if still acquired
			int refs = owners.size();
			if (refs > 0)
			{ 
				context.getLogger().fine("Transport to " + socketAddress + " still has " + refs + " client(s) active and closing...");
				TransportClient[] clients = new TransportClient[refs];
				owners.keySet().toArray(clients);
				for (int i = 0; i < clients.length; i++)
				{
					try
					{
						clients[i].transportClosed();
					}
					catch (Throwable th)
					{
						// TODO remove
						logger.log(Level.SEVERE, "", th);
					}
				}
			}
			
			owners.clear();
		}
	}

	/** 
	 * Acquires transport.
	 * @param client client (channel) acquiring the transport
	 * @return <code>true</code> if transport was granted, <code>false</code> otherwise.
	 */
	public synchronized boolean acquire(TransportClient client) {

		if (closed)
			return false;
			
		context.getLogger().finer("Acquiring transport to " + socketAddress + ".");

		synchronized (owners)
		{
			if (closed)
				return false;
				
			owners.put(client, null);
		}
		
		return true;
	}

	/** 
	 * Releases transport.
	 * @param client client (channel) releasing the transport
	 */
	public synchronized void release(TransportClient client) {

		if (closed)
			return;
			
		context.getLogger().finer("Releasing transport to " + socketAddress + ".");

		synchronized (owners)
		{
			owners.remove(client);

			// not used anymore
			if (owners.size() == 0)
				close(false);
		}
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Transport#getMinorRevision()
	 */
	public short getMinorRevision() {
		return remoteTransportRevision;
	}

	/**
	 * Handle IO event.
	 * @see com.cosylab.epics.caj.impl.reactor.ReactorHandler#handleEvent(java.nio.channels.SelectionKey)
	 */
	public void handleEvent(SelectionKey key) {
		if (key.isValid() && key.isReadable())
			processRead();
			
		if (key.isValid() && key.isWritable())
			processWrite();
	}

	/**
	 * Process input (read) IO event.
	 * Alsp handles subscription flow control.
	 */
	protected void processRead() {
		try
		{
			int bufferFullCount = 0;
			
			while (!closed)
			{
				// clear buffer
				socketBuffer.clear();
				
				// read
				int bytesRead = channel.read(socketBuffer);
				if (bytesRead < 0)
				{
					// error (disconnect, end-of-stream) detected
					close (true);
					return; 
				}
				else if (bytesRead == 0)
				{
					// no more data, disable flow control
					bufferFullCount = 0;
					if (flowControlActive)
						disableFlowControl();
					break;
				}
				
				// flow control check
				if (socketBuffer.hasRemaining())
				{
					// buffer not full, disable flow control
					bufferFullCount = 0;
					if (flowControlActive)
						disableFlowControl();
				}
				else
				{
					// buffer full, too many times?
					if (bufferFullCount >= FLOW_CONTROL_BUFFER_FULL_COUNT_LIMIT)
					{
						// enable flow control
						if (!flowControlActive)
							enableFlowControl();
					}
					else
						bufferFullCount++;
				}
				
				// prepare for reading
				socketBuffer.flip();
				
				// read from buffer
				processRead(socketBuffer);
			}
			
		} catch (IOException ioex) {
			// close connection
			close(true);
		}
	}

	/**
	 * Process input.
	 */
	protected void processRead(ByteBuffer socketBuffer)
	{
		while (true)
		{
			final ByteBuffer headerBuffer = receiveBuffer[0];
			ByteBuffer payloadBuffer = receiveBuffer[1];
			
			// are we reading the header
			if (headerBuffer.hasRemaining())
			{
				// TODO can be optimized simply by wrapping...
				readFromByteBuffer(socketBuffer, headerBuffer);
				
				// not done reading the header...
				if (headerBuffer.hasRemaining())
					break;
				
				// peek for payload size (convert unsigned short to signed int) 
				int payloadSize = headerBuffer.getShort(2) & 0xFFFF;
				
				// extended message header
				if (payloadSize == 0xFFFF)
				{
					// already extended
					if (headerBuffer.limit() == CAConstants.CA_EXTENDED_MESSAGE_HEADER_SIZE)
					{
						// peek for extended payload
						payloadSize = headerBuffer.getInt(CAConstants.CA_MESSAGE_HEADER_SIZE);
						// if (payloadSize < 0) { /* this is way too much */ }
					}
					else
					{
						// extend to extended message header and re-read
						headerBuffer.limit(CAConstants.CA_EXTENDED_MESSAGE_HEADER_SIZE);
						continue;
					}
				}

				// check payload buffer capacity
				if (payloadSize > payloadBuffer.capacity()) {
					receiveBuffer[1] = ByteBuffer.allocate(payloadSize);
					payloadBuffer = receiveBuffer[1];
				}

				// reset payload buffer
				payloadBuffer.clear();
				payloadBuffer.limit(payloadSize);
			}

			// are we reading the payload
			if (payloadBuffer.limit() == 0)
			{
				try
				{
					// prepare buffer for reading
					headerBuffer.flip();

					// handle response					
					responseHandler.handleResponse(socketAddress, this, receiveBuffer);
				}
				catch (Throwable th)
				{
					// catch all bad code responses...	
					logger.log(Level.SEVERE, "", th);
				}

				// reset header buffer
				headerBuffer.clear();
				headerBuffer.limit(CAConstants.CA_MESSAGE_HEADER_SIZE);
			}
			else if (payloadBuffer.hasRemaining())
			{
				// TODO can be optimized simply by wrapping...
				readFromByteBuffer(socketBuffer, payloadBuffer);
				
				// not done reading the payload...
				if (payloadBuffer.hasRemaining())
					break; 
					
				// prepare buffer for reading
				headerBuffer.flip();

				// prepare buffer for reading
				payloadBuffer.flip();
				
				try
				{
					// handle response
					responseHandler.handleResponse(socketAddress, this, receiveBuffer);
				}
				catch (Throwable th)
				{
					// catch all bad code responses...	
					logger.log(Level.SEVERE, "", th);
				}

				// reset header buffer
				headerBuffer.clear();
				headerBuffer.limit(CAConstants.CA_MESSAGE_HEADER_SIZE);
			}
		}
	}

	/**
	 * Read (copy) to buffer from buffer.
	 * @param socketBuffer
	 * @param payloadBuffer
	 */
	private static final void readFromByteBuffer(ByteBuffer srcBuffer, ByteBuffer destBuffer) {
		int srcBufferPosition = srcBuffer.position();
		int destPosition = destBuffer.position();
		int bytesToRead = Math.min(destBuffer.remaining(), srcBuffer.remaining());
		System.arraycopy(srcBuffer.array(), srcBufferPosition, destBuffer.array(), destPosition, bytesToRead);
		destBuffer.position(destPosition + bytesToRead);
		srcBuffer.position(srcBufferPosition + bytesToRead);
	}

	/**
	 * Process output (write) IO event.
	 */
	protected void processWrite() {
		flushInternal();                
	}

	/**
	 * Disable flow control (enables events).
	 */
	protected void disableFlowControl()
	{
		try {
			new EventsOnRequest(this).submit();
			flowControlActive = false;
			//System.out.println("disableFlowControl");
		} catch (IOException e) {
			// TODO remove
			logger.log(Level.SEVERE, "", e);
		}
	}
	
	/**
	 * Enable flow control (disables events).
	 */
	protected void enableFlowControl()
	{
		try {
			new EventsOffRequest(this).submit();
			flowControlActive = true;
			//System.out.println("enableFlowControl");
		} catch (IOException e) {
			// TODO remove
			logger.log(Level.SEVERE, "", e);
		}
	}

	/**
	 * Send a buffer through the transport.
	 * NOTE: TCP sent buffer/sending has to be synchronized. 
	 * @param buffer	buffer to be sent
	 * @throws IOException 
	 */
	// TODO optimize !!!
	public void send(ByteBuffer buffer) throws IOException
	{
		synchronized (sendLock)
		{
			try
			{
				// prepare buffer
				buffer.flip();

				final int SEND_BUFFER_LIMIT = 16000;
				int bufferLimit = buffer.limit();

				// TODO remove?!
				context.getLogger().finest("Sending " + bufferLimit + " bytes to " + socketAddress + ".");

				// limit sending large buffers, split the into parts
				int parts = (buffer.limit()-1) / SEND_BUFFER_LIMIT + 1;
				for (int part = 1; part <= parts; part++)
				{
					if (parts > 1)
					{
						buffer.limit(Math.min(part * SEND_BUFFER_LIMIT, bufferLimit));
						context.getLogger().finest("[Parted] Sending (part " + part + "/" + parts + ") " + (buffer.limit()-buffer.position()) + " bytes to " + socketAddress + ".");
					}
					
					final int TRIES = 10;
					for (int tries = 0; /* tries <= TRIES */ ; tries++)
					{
						
						// send
						/*int bytesSent =*/ channel.write(buffer);
						// bytesSend == buffer.position(), so there is no need for flip()
						if (buffer.position() != buffer.limit())
						{
							if (tries == TRIES)
							{ 
								// TODO do sth ... and do not close transport
								context.getLogger().warning("Failed to send message to " + socketAddress + " - buffer full.");
								return;
							}
							
							// flush & wait for a while...
							context.getLogger().finest("Send buffer full for " + socketAddress + ", waiting...");
							channel.socket().getOutputStream().flush();
							try {
								Thread.sleep(10+tries*100);
							} catch (InterruptedException e) {
								// noop
							}
							continue;
						}
						else
							break;
					}
				
				}
				
			}
			catch (IOException ioex) 
			{
				// close connection
				close(true);
				throw ioex;
			}
		}
	}

	/**
	 * Flush task (to be executed by an thread pool).
	 */
	private Runnable flushTask =
		new Runnable() {
	        /**
	    	 * @see java.lang.Runnable#run()
	    	 */
	    	public void run() {
	    		flushInternal();
	    	}
		};

	/**
	 * Flush send buffer.
	 * ... by enabling SelectionKey.OP_WRITE and process in reactor. 
	 * @see com.cosylab.epics.caj.impl.Transport#flush()
	 */
	public synchronized boolean flush()
	{

	    // add to queue and flush
		synchronized(sendQueue)
		{
		    if (closed || sendBuffer == null)
		        return false;
		    
	        // noop check
	        if (sendBuffer.position() == 0)
	            return true;
	        else
	        {
	            // reuse old buffer
	            if (lastActiveSendBuffer != null &&
	                lastActiveSendBuffer.position()+sendBuffer.position() <= lastActiveSendBuffer.capacity())
	            {
	                sendBuffer.flip();
	                lastActiveSendBuffer.put(sendBuffer);
	                sendBuffer.clear();
	                return true;
	            }
	            
	            sendQueue.add(sendBuffer);

	            // acquire new buffer
	            lastActiveSendBuffer = sendBuffer;
	    		sendBuffer = bufferAllocator.get();
	        }
	        
		    if (flushPending)
		        return true;
		    // NOTE: must be sure that code below will not fail
		    flushPending = true;
		}
	    
	    return spawnFlushing();
	}
	
	/**
     * @return success flag.
     */
    private boolean spawnFlushing()
    {
        LeaderFollowersThreadPool lftp = context.getLeaderFollowersThreadPool();
	    if (lftp != null)
	    {
		    // reuse LF threadpool to do async flush
	        lftp.execute(flushTask);
	        return true;
	    }
	    else
	    {
		    // enable SelectionKey.OP_WRITE via reactor (this will also enable OP_READ, but its OK)
		    context.getReactor().setInterestOps(channel, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		    return true;
	    }
    }

    /**
	 * Flush send buffer (blocks until flushed).
	 * @return success flag.
	 * @see com.cosylab.epics.caj.impl.Transport#flush()
	 */
	public boolean flushInternal()
	{
	    // tricky closed check
	    if (sendBuffer == null)
	        return false;
	    
		try
		{
			while (sendQueue.size() > 0)
			{
				ByteBuffer buf;
				// dont want to block while sending...
				synchronized (sendQueue)
				{
					if (sendQueue.size() == 0)
						return true;
						
					buf = (ByteBuffer)sendQueue.removeFirst();
					// 'deactivate' lastActiveSendBuffer
					if (buf == lastActiveSendBuffer)
					    lastActiveSendBuffer = null;
				}
				
				try {
				    send(buf);
				}
				finally {
				    // return back to the cache
				    bufferAllocator.put(buf);
				}
			}
			
			return true;
		}
		catch (IOException ioex)
		{
			// close connection
			close(true);
			return false;
		}
		finally
		{
			synchronized (sendQueue)
			{
			    // ack
			    flushPending = false;
			    
			    // possible race condition check
				if (!closed && sendQueue.size() > 0)
				    spawnFlushing();
			}
		}
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Transport#submit(com.cosylab.epics.caj.impl.Request)
	 */
	public void submit(Request requestMessage) throws IOException {
		ByteBuffer message = requestMessage.getRequestMessage();
			
		// empty message
		if (message.capacity() == 0)
			return;
				
		// send or enqueue
		if (requestMessage.getPriority() == Request.SEND_IMMEDIATELY_PRIORITY)
			send(message);
		else
		{
			message.flip();

			synchronized (sendQueue) {

			    if (sendBuffer == null)
				    throw new IllegalStateException("transport closed");
			    
			    // forced flush check
				if (message.limit()+sendBuffer.position() > sendBuffer.capacity())
				    flush();
				
				// TODO !!! check message size, it can exceed sendBuffer capacity
				sendBuffer.put(message);
			}
		}
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Transport#getContext()
	 */
	public CAContext getContext() {
		return context;
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Transport#getRemoteAddress()
	 */
	public InetSocketAddress getRemoteAddress() {
		return socketAddress;
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Transport#getPriority()
	 */
	public short getPriority() {
		return priority;
	}

	/* ********************* [ Beacons ] ************************ */
	
	/**
	 * Probe response (state-of-health message) sent and waiting for response.
	 */
	private boolean probeResponsePending = false;
	
	/**
	 * Probe response (state-of-health message) did not respond - timeout.
	 */
	private boolean probeTimeoutDetected = false;
			
	/**
	 * Probe lock.
	 */
	private Object probeLock = new Object();

	/**
	 * Beacon anomaly flag.
	 */
	private long connectionTimeout;

	/**
	 * Unresponsive transport flag.
	 */
	private boolean unresponsiveTransport = false;

	/**
	 * Timer task node.
	 */
	private Object taskID;

	/**
	 * Beacon arrival.
	 */
	public void beaconArrivalNotify()
	{
		if (!probeResponsePending)
			rescheduleTimer(connectionTimeout);
	}

	/*
	 * Message arrival (every message send to transport). 
	 *//* this is called to oftnen (e.g. first this and then immediately beaconArrivalNotify)
	private void messageArrivalNotify()
	{
		synchronized(probeLock)
		{
			if (!probeResponsePending) 
				rescheduleTimer(connectionTimeout);
		}
	}*/

	/**
	 * Rechedule timer for timeout ms.
	 * @param timeout	timeout in ms.
	 */
	private void rescheduleTimer(long timeout)
	{
		Timer.cancel(taskID);
		if (!closed)
			taskID = context.getTimer().executeAfterDelay(timeout, this);
	}
	
	/**
	 * Beacon timer.
	 * @see com.cosylab.epics.caj.util.Timer.TimerRunnable#timeout(long)
	 */
	public void timeout(long timeToRun)
	{
		synchronized(probeLock)
		{
			if (probeResponsePending)
			{
				probeTimeoutDetected = true;
				unresponsiveTransport();
			}
			else
			{
				sendEcho();
			}
		}
	}

	/**
	 * Called when echo request (state-of-health message) was responed.
	 */
	public void echoNotify()
	{
		synchronized(probeLock)
		{
			if (probeResponsePending)
			{
				if (probeTimeoutDetected)
				{
					// try again
					sendEcho();
				}
				else
				{
					// transport is responsive
					probeTimeoutDetected = false;
					probeResponsePending = false;
					responsiveTransport();
					rescheduleTimer(connectionTimeout);
				}
			}
		}
	}
	
	/**
	 * Sends echo (state-of-health message)
	 */
	private void sendEcho() {
		synchronized(probeLock)
		{
			probeTimeoutDetected = false;
			probeResponsePending = remoteTransportRevision >= 3;
			try
			{
				new EchoRequest(this).submit();
			}
			catch (IOException ex)
			{
				probeResponsePending = false;
			}
			rescheduleTimer(CAConstants.CA_ECHO_TIMEOUT);
		}
	}

	/**
	 * Responsive transport notify. 
	 */
	private void responsiveTransport()
	{
		if (unresponsiveTransport)
		{
		    unresponsiveTransport = false;
			synchronized (owners)
			{
				TransportClient[] clients = new TransportClient[owners.size()];
				owners.keySet().toArray(clients);
				for (int i = 0; i < clients.length; i++)
				{
					try
					{
						clients[i].transportResponsive(this);
					}
					catch (Throwable th)
					{
						// TODO remove
						logger.log(Level.SEVERE, "", th);
					}
				}
			}
		}
	}

	/**
	 * Unresponsive transport notify. 
	 */
	private void unresponsiveTransport()
	{
		if (!unresponsiveTransport)
		{
		    unresponsiveTransport = true;

		    ContextVirtualCircuitExceptionEvent cvcee =
		    	new ContextVirtualCircuitExceptionEvent((Context)context, socketAddress.getAddress(), CAStatus.UNRESPTMO);
		    ContextExceptionListener[] listeners = context.getContextExceptionListeners();
			for (int i = 0; i < listeners.length; i++)
			{
				try
				{
					listeners[i].contextVirtualCircuitException(cvcee);
				}
				catch (Throwable th)
				{
					// TODO remove
					logger.log(Level.SEVERE, "", th);
				}
			}
		    
			synchronized (owners)
			{
				TransportClient[] clients = new TransportClient[owners.size()];
				owners.keySet().toArray(clients);
				for (int i = 0; i < clients.length; i++)
				{
					try
					{
						clients[i].transportUnresponsive();
					}
					catch (Throwable th)
					{
						// TODO remove
						logger.log(Level.SEVERE, "", th);
					}
				}
			}
		}
	}


	/**
	 * Changed transport (server restared) notify. 
	 */
	public void changedTransport()
	{
		synchronized (owners)
		{
			TransportClient[] clients = new TransportClient[owners.size()];
			owners.keySet().toArray(clients);
			for (int i = 0; i < clients.length; i++)
			{
				try
				{
					clients[i].transportChanged();
				}
				catch (Throwable th)
				{
					// TODO remove
					logger.log(Level.SEVERE, "", th);
				}
			}
		}
	}
}
