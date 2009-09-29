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
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.reactor.ReactorHandler;
import com.cosylab.epics.caj.impl.reactor.lf.LeaderFollowersHandler;
import com.cosylab.epics.caj.impl.requests.HostNameRequest;
import com.cosylab.epics.caj.impl.requests.UserNameRequest;
import com.cosylab.epics.caj.impl.requests.VersionRequest;
import com.cosylab.epics.caj.impl.sync.NamedLockPattern;

/**
 * Channel Access TCP connector.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAConnector implements Connector {

	/**
	 * Context instance.
	 */
	private CAJContext context;
	
	/**
	 * Context instance.
	 */
	private NamedLockPattern namedLocker;

	/**
	 * Context instance.
	 */
	private static final int LOCK_TIMEOUT = 10 * 6000;	// 10s

	/**
	 * @param context
	 */
	public CAConnector(CAJContext context) {
		this.context = context;
		namedLocker = new NamedLockPattern();
	}
	
	/**
	 * @see com.cosylab.epics.caj.impl.Connector#connect(com.cosylab.epics.caj.impl.TransportClient, com.cosylab.epics.caj.impl.ResponseHandler, java.net.InetSocketAddress, short, short)
	 */
	public Transport connect(TransportClient client, ResponseHandler responseHandler,
							 InetSocketAddress address, short transportRevision, short priority)
		throws ConnectionException
	{
		SocketChannel socket = null;
		
		// first try to check cache w/o named lock...
		CATransport transport = (CATransport)context.getTransportRegistry().get(address, priority);
		if (transport != null)
		{
			context.getLogger().finer("Reusing existant connection to CA server: " + address);
			if (transport.acquire(client))
				return transport;
		}

		boolean lockAcquired = namedLocker.acquireSynchronizationObject(address, LOCK_TIMEOUT);
		if (lockAcquired)
		{ 
			try
			{   
				// ... transport created during waiting in lock 
				transport = (CATransport)context.getTransportRegistry().get(address, priority);
				if (transport != null)
				{
					context.getLogger().finer("Reusing existant connection to CA server: " + address);
					if (transport.acquire(client))
						return transport;
				}
				     
				context.getLogger().finer("Connecting to CA server: " + address);
				
				socket = tryConnect(address, 3);
	
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
				transport = new CATransport(context, client, responseHandler, socket, transportRevision, priority);
				ReactorHandler handler = transport;
				if (context.getLeaderFollowersThreadPool() != null)
				    handler = new LeaderFollowersHandler(context.getReactor(), handler, context.getLeaderFollowersThreadPool());
				
				// register to reactor
				context.getReactor().register(socket, SelectionKey.OP_READ, handler);
				
				// issue version including priority, username and local hostname
				new VersionRequest(transport, priority).submit();
				new UserNameRequest(transport).submit();
				new HostNameRequest(transport).submit();
				
				context.getLogger().finer("Connected to CA server: " + address);
	
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
	
				throw new ConnectionException("Failed to connect to '" + address + "'.", address, th);
			}
			finally
			{
				namedLocker.releaseSynchronizationObject(address);	
			}
		}
		else
		{     
			throw new ConnectionException("Failed to obtain synchronization lock for '" + address + "', possible deadlock.", address, null);
		}
	}

	/**
	 * Tries to connect to the given adresss.
	 * @param address
	 * @param tries
	 * @return
	 * @throws IOException
	 */
	private SocketChannel tryConnect(InetSocketAddress address, int tries)
		throws IOException
	{
		
		IOException lastException = null;
				
		for (int tryCount = 0; tryCount < tries; tryCount++)
		{

			// sleep for a while
			if (tryCount > 0)
			{
				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {}
			}

			context.getLogger().finest("Openning socket to CA server " + address + ", attempt " + (tryCount+1) + ".");

			try
			{
				return SocketChannel.open(address);
			}
			catch (IOException ioe)
			{
				lastException = ioe;
			}


		}

		throw lastException;
	}

}
