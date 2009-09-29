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

import gov.aps.jca.CAException;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.cosylab.epics.caj.impl.CAContext;
import com.cosylab.epics.caj.impl.reactor.ReactorHandler;
import com.cosylab.epics.caj.impl.reactor.lf.LeaderFollowersHandler;

/**
 * Channel Access Server TCP acceptor.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CASAcceptor implements ReactorHandler {

	/**
	 * Context instance.
	 */
	private CAContext context;
	
	/**
	 * Bind server socket address.
	 */
	private InetSocketAddress bindAddress = null;

	/**
	 * Server socket channel.
	 */
	private ServerSocketChannel serverSocketChannel = null;
	
	/**
	 * @param context
	 */
	public CASAcceptor(CAContext context, int port) throws CAException {
		this.context = context;
		
		int assignedPort = initialize(port);
		
		// update port, if dynamically assigned port is used
		if (assignedPort != port)
			((CAJServerContext)context).setServerPort(assignedPort);
	}

	/**
	 * Handle IO event.
	 * @see com.cosylab.epics.caj.impl.reactor.ReactorHandler#handleEvent(java.nio.channels.SelectionKey)
	 */
	public void handleEvent(SelectionKey key) {
		if (key.isValid() && key.isAcceptable())
		{
			try
			{
				ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel(); 
				SocketChannel socket = serverSocketChannel.accept();
				
				SocketAddress address = socket.socket().getRemoteSocketAddress();
				context.getLogger().finer("Accepted connection from CA client: " + address);
				
				// use non-blocking channel (no need for soTimeout)			
				socket.configureBlocking(false);
			
				// enable TCP_NODELAY (disable Nagle's algorithm)
				socket.socket().setTcpNoDelay(true);
				
				// enable TCP_KEEPALIVE
				socket.socket().setKeepAlive(true);
			
				// TODO tune buffer sizes?! Win32 defaults are 8k, which is OK
				//socket.socket().setReceiveBufferSize();
				//socket.socket().setSendBufferSize();

				// create transport
				CASTransport transport = new CASTransport(context, socket);
				ReactorHandler handler = transport;
				if (context.getLeaderFollowersThreadPool() != null)
				    handler = new LeaderFollowersHandler(context.getReactor(), handler, context.getLeaderFollowersThreadPool());
				
				// register to reactor
				context.getReactor().register(socket, SelectionKey.OP_READ, handler);
				
				context.getLogger().finer("Serving to CA client: " + address);

			} 
			catch (Throwable th)
			{
				th.printStackTrace();
			}
		}
	}

	/**
	 * Initialize connection acception. 
	 * @return port where server is listening
	 */
	private int initialize(int port) throws CAException
	{
		// specified bind address
		bindAddress = new InetSocketAddress(port);

		int tryCount = 0;
		while (true)
		{
			tryCount++;
			
			try
			{
				context.getLogger().finer("Creating acceptor to " + bindAddress + ".");
	
				serverSocketChannel = ServerSocketChannel.open();
				serverSocketChannel.socket().bind(bindAddress);
				serverSocketChannel.configureBlocking(false);
				
				// update bind address, if dynamically port selection was used
				if (bindAddress.getPort() == 0)
				{
					bindAddress = new InetSocketAddress(serverSocketChannel.socket().getLocalPort());
					context.getLogger().info("Using dynamically assigned TCP port " + bindAddress.getPort() + ".");
				}
				
				// create handler
				ReactorHandler handler = this;
				if (context.getLeaderFollowersThreadPool() != null)
				    handler = new LeaderFollowersHandler(context.getReactor(), handler, context.getLeaderFollowersThreadPool());
				
				// register to reactor
				context.getReactor().register(serverSocketChannel, SelectionKey.OP_ACCEPT, handler);
	
				// rise level if port is assigned dynamically
				context.getLogger().finer("Accepting connections at " + bindAddress + ".");

				// all OK, return
				return bindAddress.getPort();
			}
			catch (BindException be)
			{
				// failed to bind to specified bind address,
				// try to get port dynamically, but only once
				if (tryCount == 1)
				{
					context.getLogger().info("Configured TCP port " + port + " is unavailable, trying to assign it dynamically.");
					bindAddress = new InetSocketAddress(0);
				}
				else
				{
					throw new CAException("Failed to create acceptor to " + bindAddress, be);
				}				
			}
			catch (Throwable th)
			{
				throw new CAException("Failed to create acceptor to " + bindAddress, th);
			}
		}
		
	}
	
	/**
	 * Bind socket address.
	 * @return bind socket address, <code>null</code> if not binded.
	 */
	public InetAddress getBindAddress() 
	{
		return (serverSocketChannel != null) ? 
				serverSocketChannel.socket().getInetAddress() : null;
	}
	
	/**
	 * Destroy acceptor (stop listening).
	 */
	public void destroy()
	{
		if (serverSocketChannel != null)
		{
			context.getLogger().finer("Stopped accepting connections at " + bindAddress + ".");

			context.getReactor().unregisterAndClose(serverSocketChannel);
		}
	}
	
}
