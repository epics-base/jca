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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import com.cosylab.epics.caj.impl.reactor.ReactorHandler;

/**
 * CA UDP transport implementation.
 * It receives datagrams from <code>BroadcastConnector</code> registered
 * repeater and sends broadcasts datagrams to given addresses.  
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
// TODO buffer out of memory bug (on massive connect)?! 
public class BroadcastTransport implements Transport, ReactorHandler {

	/**
	 * Context instance.
	 */
	private CAContext context;

	/**
	 * Corresponding channel.
	 */
	private DatagramChannel channel;

	/**
	 * Cached socket address.
	 */
	private InetSocketAddress socketAddress;

	/**
	 * Connect address.
	 */
	private InetSocketAddress connectAddress;

	/**
	 * Broadcast addresses.
	 */
	private InetSocketAddress[] broadcastAddresses;

	/**
	 * Ignore addresses.
	 */
	private InetSocketAddress[] ignoredAddresses = null;

	/**
	 * Receive buffer.
	 */
	private ByteBuffer receiveBuffer;

	/**
	 * Receive buffer.
	 */
	private ByteBuffer[] receiveBufferArray;

	/**
	 * Remote side transport revision.
	 */
	private short remoteTransportRevision;

	/**
	 * CAS reponse handler.
	 */
	protected ResponseHandler responseHandler = null;

	/**
	 * @param context
	 */
	public BroadcastTransport(CAContext context, ResponseHandler responseHandler, DatagramChannel channel,
							  InetSocketAddress connectAddress, short remoteTransportRevision) {
		this.context = context;
		this.responseHandler = responseHandler;
		this.channel = channel;
		this.connectAddress = connectAddress;
		this.remoteTransportRevision = remoteTransportRevision;
		
		// TODO consider broadcast address of subnet
		this.broadcastAddresses = 
		    	new InetSocketAddress[] { new InetSocketAddress("255.255.255.255", context.getBroadcastPort()) };

		socketAddress = (InetSocketAddress)channel.socket().getRemoteSocketAddress();

		// allocate receive buffer
		receiveBuffer = ByteBuffer.allocate(CAConstants.MAX_UDP_RECV);
		receiveBufferArray = new ByteBuffer[] { receiveBuffer };
	}
	
	/**
	 * Bind UDP socket to the <code>connectAddress</code>.
	 * @param reuseAddress resuse address option.
	 */
	public void bind(boolean reuseAddress) throws SocketException
	{
		channel.socket().setReuseAddress(reuseAddress);
		channel.socket().bind(connectAddress);
	}
	
	/**
	 * Close transport.
	 */
	public void close()
	{
		if (connectAddress != null)
			context.getLogger().finer("UDP connection to " + connectAddress + " closed.");
		context.getReactor().unregisterAndClose(channel);
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
	protected void processRead() {

		try
		{
			while (true)
			{
				
				// reset header buffer
				receiveBuffer.clear();

				// read to buffer
				// NOTE: If there are fewer bytes remaining in the buffer
				// than are required to hold the datagram then the remainder
				// of the datagram is silently discarded.
				InetSocketAddress fromAddress = (InetSocketAddress)channel.receive(receiveBuffer);
				
				// check if datagram not available
				// NOTE: If this channel is in non-blocking mode and a datagram is not
				// immediately available then this method immediately returns <tt>null</tt>.
				if (fromAddress == null)
				 break;

				// check if received from ignore address list
				if (ignoredAddresses != null)
				{
					// we do not care about the port
					InetAddress fromAddressOnly = fromAddress.getAddress();
					for (int i = 0; i < ignoredAddresses.length; i++)
						if (ignoredAddresses[i].getAddress().equals(fromAddressOnly))
							continue;
				}

				context.getLogger().finest("Received " + receiveBuffer.position() + " bytes from " + fromAddress + ".");

				// prepare buffer for reading
				receiveBuffer.flip();

				// invalidate last received sequence
				context.invalidateLastReceivedSequence();

				// handle response				
				while (receiveBuffer.limit() - receiveBuffer.position() >= CAConstants.CA_MESSAGE_HEADER_SIZE)
					responseHandler.handleResponse(fromAddress, this, receiveBufferArray);
 
			}
			
		} catch (IOException ioex) {
			// TODO what to do here
			//ioex.printStackTrace();
		}
	}

	/**
	 * Process output (write) IO event.
	 */
	protected void processWrite() {
		// noop (not used for datagrams)
	}

	/**
	 * Send a buffer through the transport.
	 * @param buffer	buffer to send. 
	 */
	protected void send(ByteBuffer buffer) 
	{
		for (int i = 0; i < broadcastAddresses.length; i++)
		{
			try
			{
				// prepare buffer
				buffer.flip();

				//context.getLogger().finest("Sending " + buffer.limit() + " bytes to " + broadcastAddresses[i] + ".");

				channel.send(buffer, broadcastAddresses[i]);
			}
			catch (IOException ioex) 
			{
				// TODO what to do here
				ioex.printStackTrace(); 
			}
		}
	}
	
	/**
	 * Send a buffer through the transport immediately. 
	 * @param buffer	buffer to send. 
	 * @param address	send address. 
	 * @throws IOException
	 */
	protected void send(ByteBuffer buffer, InetSocketAddress address) throws IOException
	{
		context.getLogger().finest("Sending " + buffer.limit() + " bytes to " + address + ".");
		buffer.flip();
		channel.send(buffer, address);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Transport#getMinorRevision()
	 */
	public short getMinorRevision() {
		return remoteTransportRevision;
	}

	/**
	 * Send request message(s) immediately.
	 * @see com.cosylab.epics.caj.impl.Transport#submit(com.cosylab.epics.caj.impl.Request)
	 */
	public void submit(Request requestMessage) {
		send(requestMessage.getRequestMessage());
	}

	/**
	 * Send a request message through the transport. 
	 * @param requestMessage	message to send. 
	 * @param address	send address. 
	 * @throws IOException
	 */
	public void send(Request requestMessage, InetSocketAddress address) throws IOException
	{
		send(requestMessage.getRequestMessage(), address);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Transport#getContext()
	 */
	public CAContext getContext() {
		return context;
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Transport#getPriority()
	 */
	public short getPriority() {
		return CAConstants.CA_DEFAULT_PRIORITY;
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Transport#flush()
	 */
	public boolean flush() {
		// noop since all UDP reqeuests are sent immediately
		return true;
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Transport#getRemoteAddress()
	 */
	public InetSocketAddress getRemoteAddress() {
		return socketAddress;
	}

    /**
     * Get list of broadcast addresses.
     * @return broadcast addresses.
     */
    public InetSocketAddress[] getBroadcastAddresses()
    {
        return broadcastAddresses;
    }

    /**
     * Get list of ignored addresses.
     * @return broadcast addresses.
     */
    public InetSocketAddress[] getIgnoredAddresses()
    {
        return ignoredAddresses;
    }
    
    /**
     * Get connect address.
     * @return connect address.
     */
    public InetSocketAddress getConnectAddress()
    {
        return connectAddress;
    }
    
	/**
	 * Set list of broadcast addresses.
	 * This overrides default <code>DEFAULT_BROADCAST_ADDRESSES</code> list. 
	 * @param addresses list of broadcast addresses, non-<code>null</code>.
	 */
	public void setBroadcastAddresses(InetSocketAddress[] addresses) {
		broadcastAddresses = addresses;
	}

	/**
	 * Set list of broadcast addresses.
	 * @param addresses list of ignored addresses.
	 */
	public void setIgnoredAddresses(InetSocketAddress[] addresses) {
		ignoredAddresses = addresses;
	}
}
