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

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import com.cosylab.epics.caj.impl.reactor.ReactorHandler;
import com.cosylab.epics.caj.impl.reactor.lf.LeaderFollowersHandler;

/**
 * Channel Access broadcast/repeater UDP connector.
 * It actially does not bind to any address, but it registers to the local repeater.
 * This isn't real connector actually, since it does not connect (bind).
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class BroadcastConnector implements Connector {

	/**
	 * Context instance.
	 */
	private CAContext context;
	
	/**
	 * @param context
	 */
	public BroadcastConnector(CAContext context) {
		this.context = context;
	}
	
	/**
	 * NOTE: transport client is ignored for broadcast (UDP). 
	 * @see com.cosylab.epics.caj.impl.Connector#connect(com.cosylab.epics.caj.impl.TransportClient, com.cosylab.epics.caj.impl.ResponseHandler, java.net.InetSocketAddress, short, short)
	 */
	public Transport connect(TransportClient client, ResponseHandler responseHandler, InetSocketAddress connectAddress, short transportRevision, short priority)
		throws ConnectionException
	{
		context.getLogger().finer("Creating datagram socket to: " + connectAddress);
		
		DatagramChannel socket = null;
		try
		{        
			socket = DatagramChannel.open();

			// use non-blocking channel (no need for soTimeout)			
			socket.configureBlocking(false);
		
			// set SO_BROADCAST
			socket.socket().setBroadcast(true);
			
			// TODO tune buffer sizes?! Win32 defaults are 8k, which is OK
			//socket.socket().setReceiveBufferSize();
			//socket.socket().setSendBufferSize();

			// create transport
			BroadcastTransport transport = new BroadcastTransport(context, responseHandler, socket, connectAddress, transportRevision);
			ReactorHandler handler = transport;
			if (context.getLeaderFollowersThreadPool() != null)
			    handler = new LeaderFollowersHandler(context.getReactor(), handler, context.getLeaderFollowersThreadPool());
			
			// register to reactor  
			context.getReactor().register(socket, SelectionKey.OP_READ, handler);

			return transport;
		}
		catch (Throwable th)
		{
			// close socket, if open
			try
			{
				if (socket != null)
					socket.close();
			}
			catch (Throwable t) { /* noop */ }

			throw new ConnectionException("Failed to connect to '" + connectAddress + "'.", connectAddress, th);
		}
            
	}


}
