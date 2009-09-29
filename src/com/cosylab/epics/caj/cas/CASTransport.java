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

package com.cosylab.epics.caj.cas;

import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.Channel;
import gov.aps.jca.cas.ServerChannel;
import gov.aps.jca.dbr.DBR_STSACK_String;
import gov.aps.jca.dbr.DBR_String;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.CAContext;
import com.cosylab.epics.caj.impl.CachedByteBufferAllocator;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.ResponseHandler;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.reactor.ReactorHandler;
import com.cosylab.epics.caj.impl.reactor.lf.LeaderFollowersThreadPool;
import com.cosylab.epics.caj.util.IntHashMap;

/**
 * CAS transport implementation.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CASTransport implements Transport, ReactorHandler, Runnable {

	/**
	 * Connection status.
	 */
	private volatile boolean closed = false;

	/**
	 * Context instance.
	 */
	private CAJServerContext context;

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
	 * Send queue.
	 */
	private LinkedList sendQueue;
	
	/**
	 * Remote side transport revision.
	 */
	private short remoteTransportRevision;

	/**
	 * Priority.
	 */
	private short priority;

	/**
	 * Send sync. object lock.
	 */
	private Object sendLock = new Object();

	/**
	 * Flush pending status.
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
	 * CAS reponse handler.
	 */
	protected ResponseHandler responseHandler = null;

	/**
	 * Last active send buffer.
	 * This is used to possibly optimize high frequency flushes, it merges current buffer with the last 
	 * (if it there is enough of space and it is wainting in the sendQueue). 
	 */
	private ByteBuffer lastActiveSendBuffer = null;

	/**
	 * Client hostname, <code>null</code> if not unset.
	 */
	private String clientHostname = null;

	/**
	 * Client username, <code>null</code> if not unset.
	 */
	private String clientUsername = null;
	
	/**
	 * New events will replace the last event on the queue for a particular monitor.
	 */
	private volatile boolean replaceEventPolicy = false;
	
	/**
	 * Process event flag.
	 */
	private volatile boolean processEvents = true;

	/**
	 * Process event thread.
	 */
	private Thread processEventThread;
	
	/**
	 * Event queue.
	 */
	private final LinkedList eventQueue = new LinkedList();

	/**
	 * Channel table (SID -> channel mapping).
	 */
	private IntHashMap channels;

	/**
	 * @param context
	 * @param channel
	 */
	public CASTransport(CAContext context, 
					   SocketChannel channel) {
		this.context = (CAJServerContext)context;
		this.channel = channel;
		this.remoteTransportRevision = 0;

		final int INITIAL_SIZE = 64;
		channels = new IntHashMap(INITIAL_SIZE);
		
		// initialize buffers
		receiveBuffer = new ByteBuffer[] {
							ByteBuffer.allocate(CAConstants.CA_EXTENDED_MESSAGE_HEADER_SIZE),
							ByteBuffer.allocate(Math.max(CAConstants.MAX_TCP_RECV, this.context.getMaxArrayBytes() + 8))
						};
		// first limit to a reading of an standard message header
		receiveBuffer[0].limit(CAConstants.CA_MESSAGE_HEADER_SIZE);
		
		sendQueue = new LinkedList();
		bufferAllocator = context.getCachedBufferAllocator();
		sendBuffer = bufferAllocator.get();

		responseHandler = new CASResponseHandler(this.context);
		
		socketAddress = (InetSocketAddress)channel.socket().getRemoteSocketAddress();
		

		// start event dispatcher thread
		processEventThread = new Thread(this, socketAddress + "event dispatcher");

		// set default priority
		setPriority(CAConstants.CA_DEFAULT_PRIORITY);

		processEventThread.start();

		// add to registry, with priority CAConstants.CA_DEFAULT_PRIORITY
		context.getTransportRegistry().put(socketAddress, this);
	}
	
	/** 
	 * Close connection.
	 * @param forced	flag indicating if forced (e.g. forced disconnect) is required 
	 */
	public synchronized void close(boolean forced) {

		// already closed check
		if (closed)
			return;
		closed = true;

		// remove from registry, with priortiy CAConstants.CA_DEFAULT_PRIORITY (as registered!)
		context.getTransportRegistry().remove(socketAddress, CAConstants.CA_DEFAULT_PRIORITY);

		// destroy all channels (and its monitors)
		destroyAllChannels();
		
		// shutdown event processor
		// wake up the processing thread
		synchronized (eventQueue) {
			eventQueue.notify();
		}

		// flush first
		if (!forced)
			flushInternal();
		
		freeSendBuffers();

		context.getLogger().finer("Connection to " + socketAddress + " closed.");

		context.getReactor().unregisterAndClose(channel);
	}

	/**
	 * Destroy all channels.
	 */
	private void destroyAllChannels() {

		ServerChannel[] channelsArray;
		synchronized (channels)
		{
    		// resource allocation optimization
    		if (channels.size() == 0)
    			return;

    		channelsArray = new ServerChannel[channels.size()];
			channels.toArray(channelsArray);

			channels.clear();
		}

		context.getLogger().fine("Transport to " + socketAddress + " still has " + channelsArray.length + " channel(s) active and closing...");
		
		for (int i = 0; i < channelsArray.length; i++)
		{
			try
			{
				channelsArray[i].destroy();
			}
			catch (Throwable th)
			{
				th.printStackTrace();
			}
		}
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
	 * @see com.cosylab.epics.caj.impl.Transport#getMinorRevision()
	 */
	public short getMinorRevision() {
		return remoteTransportRevision;
	}

	/**
	 * Set minor revision number.
	 * @param minorRevision	minor revision number.
	 */
	public void setMinorRevision(short minorRevision) {
		this.remoteTransportRevision = minorRevision;
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
	 */
	// TODO buffered read as in CAJ
	protected void processRead() {
		try
		{ 
			while (true)
			{
				final ByteBuffer headerBuffer  = receiveBuffer[0];
				ByteBuffer payloadBuffer = receiveBuffer[1];
				
				// are we reading the header
				if (headerBuffer.hasRemaining())
				{
					if (channel.read(headerBuffer) < 0) {
						// error (disconnect, end-of-stream) detected
						close (true);
						return; 
					}
					
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
						th.printStackTrace();
					}

					// reset header buffer
					headerBuffer.clear();
					headerBuffer.limit(CAConstants.CA_MESSAGE_HEADER_SIZE);
				}
				else if (payloadBuffer.hasRemaining())
				{
					channel.read(payloadBuffer);

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
						th.printStackTrace();
					}

					// reset header buffer
					headerBuffer.clear();
					headerBuffer.limit(CAConstants.CA_MESSAGE_HEADER_SIZE);
				}
			}
			
		} catch (IOException ioex) {
			//ioex.printStackTrace();
			// close connection
			close(true);
		} 
	}


	/**
	 * Process output (write) IO event.
	 */
	protected void processWrite() {
		flushInternal();                
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

				//context.getLogger().finest("Sending " + bufferLimit + " bytes to " + socketAddress + ".");

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
			//ioex.printStackTrace();
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

	/**
	 * CA server event process thread minimal priority.
	 */
	final static int CA_SERVER_THREAD_PRIORITY_MIN = Thread.NORM_PRIORITY - 1;

	/**
	 * CA server event process thread maximal priority.
	 */
	final static int CA_SERVER_THREAD_PRIORITY_MAX = Thread.MAX_PRIORITY - 1;
	
	/**
	 * Calculate native thread priority from CA priortities.
	 * @param priority	CA priority
	 * @return native thread priority
	 */
	private final static int getNativeThreadPriority(short priority)
	{
		int nativePriority = priority - Channel.PRIORITY_MIN;
		nativePriority *= CA_SERVER_THREAD_PRIORITY_MAX - CA_SERVER_THREAD_PRIORITY_MIN;
		nativePriority /=  Channel.PRIORITY_MAX - Channel.PRIORITY_MIN;
		nativePriority += Thread.MIN_PRIORITY;
		return nativePriority;
	}
	
	/**
	 * Set transport priority.
	 */
	public void setPriority(short priority) {

		// check for bounds
		if (priority < Channel.PRIORITY_MIN)
			priority = Channel.PRIORITY_MIN;
		else if (priority > Channel.PRIORITY_MAX)
			priority = Channel.PRIORITY_MAX;
		
		this.priority = priority;
		
		int nativePriority = getNativeThreadPriority(priority);
		if (nativePriority != Thread.currentThread().getPriority())
			Thread.currentThread().setPriority(nativePriority);
	}


	/**
	 * Get client hostname.
	 * @return client hostname, <code>null</code> if unset.
	 */
	public String getClientHostname() {
		return clientHostname;
	}
	
	/**
	 * Get client username.
	 * @return client username, <code>null</code> if unset.
	 */
	public String getClientUsername() {
		return clientUsername;
	}

	/**
	 * Set client hostname.
	 * @param clientHostname	client hostname.
	 */
	public void setClientHostname(String clientHostname) {
		this.clientHostname = clientHostname;
		context.getLogger().fine("Client " + socketAddress + " is setting hostname to '" + clientHostname + "'.");
	}

	/**
	 * Set client username.
	 * @param clientUsername	client username.
	 */
	public void setClientUsername(String clientUsername) {
		this.clientUsername = clientUsername;
		context.getLogger().fine("Client " + socketAddress + " is setting username to '" + clientUsername + "'.");
	}

	/**
	 * Preallocate new channel SID.
	 * @return new channel server id (SID).
	 */
	public int preallocateChannelSID()
	{
		synchronized (channels) {
			// search first free (theoretically possible loop of death)
			// C++ version does not do that check, it relies on large range of values
			int sid = context.generateChannelSID();
			while (channels.containsKey(sid))
				sid = context.generateChannelSID();
			return sid;
		}
	}

	/**
	 * De-preallocate new channel SID.
	 * @param sid preallocated channel SID. 
	 */
	public void depreallocateChannelSID(int sid)
	{
		// noop
	}

	/**
	 * Register a new channel.
	 * @param sid preallocated channel SID. 
	 * @param channel channel to register.
	 */
	public void registerChannel(int sid, ServerChannel channel)
	{
		synchronized (channels) {
			channels.put(sid, channel);
		}
	}
	
	/**
	 * Unregister a new channel (and deallocates its handle).
	 * @param sid SID
	 */
	public void unregisterChannel(int sid)
	{
		synchronized (channels) {
			channels.remove(sid);
		}
	}

	/**
	 * Get channel by its SID.
	 * @param sid channel SID
	 * @return channel with given SID, <code>null</code> otherwise
	 */
	public ServerChannel getChannel(int sid)
	{
		synchronized (channels) {
			return (ServerChannel)channels.get(sid);
		}
	}

	/**
	 * Get channel by its SID and do some additional checks.
	 * @param sid channel SID
	 * @return channel with given SID, exception with status is thrown otherwise
	 */
	public ServerChannel getChannelAndVerifyRequest(int sid, short dataType, int dataCount)
		throws CAStatusException
	{
		// get channel
		ServerChannel channel = getChannel(sid);
		if (channel == null)
			throw new CAStatusException(CAStatus.BADCHID);
		
		// is data type valid?
		if (dataType < DBR_String.TYPE.getValue() ||
			dataType > DBR_STSACK_String.TYPE.getValue())
//			dataType > DBR_CTRL_Double.TYPE.getValue())
			throw new CAStatusException(CAStatus.BADTYPE);
			
		// is data count in range?
		// TODO this is really strange, there is a code in CA to chekc upper limit
		// but it has no effect?!
		if (dataCount <= 0 /*|| dataCount > channel.getProcessVariable().getDimensionSize(0)*/)
			throw new CAStatusException(CAStatus.BADCOUNT);

		return channel;
	}

	/**
	 * Get channel count.
	 * @return channel count.
	 */
	public int getChannelCount() 
	{
		synchronized (channels) {
			return channels.size();
		}
	}
	
	/**
	 * Turn event dispatching off.
	 */
	public void eventsOff()
	{
		replaceEventPolicy = true;
		processEvents = false;
	}

	/**
	 * Turn event dispatching on.
	 */
	public void eventsOn()
	{
		replaceEventPolicy = false;
		processEvents = true;

		// wake up the processing thread
		synchronized (eventQueue) {
			eventQueue.notify();
		}
	}

	/**
	 * Get replace event policy flag.
	 * @return replace event policy flag.
	 */
	public final boolean hasReplaceEventPolicy()
	{
		return replaceEventPolicy;
	}
	
	/**
	 * Process event.
	 * @param event	event to be processed.
	 * @return success flag.
	 */
	public boolean processEvents(Runnable event)
	{
		if (!processEvents || closed)
			return false;

		// put into the queue to process
        synchronized(eventQueue) {
        	eventQueue.addLast(event);
        	eventQueue.notify();
        }

        return true;
	}

    /**
	 * Event dispatching is done here.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		while (true)
		{
			Runnable event;
		
			synchronized (eventQueue)
			{
				// wait for new event ...
				while ((eventQueue.isEmpty() || !processEvents) && !closed) {
					try { 
						eventQueue.wait();
					} catch (InterruptedException ignored) { /* noop */ }
				}

				// check if closed
				if (closed) {
					eventQueue.clear();
					return;
				}

				event = (Runnable) eventQueue.removeFirst();
			}

			// ... and process
			try {
				event.run();
			} catch (Throwable th) {
				th.printStackTrace();
			}
		}
	}
	
}
