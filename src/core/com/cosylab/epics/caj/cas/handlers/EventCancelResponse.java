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

package com.cosylab.epics.caj.cas.handlers;

import gov.aps.jca.CAStatus;
import gov.aps.jca.cas.ServerChannel;
import gov.aps.jca.cas.ServerMonitor;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.CASTransport;
import com.cosylab.epics.caj.cas.requests.EventCancelRequest;
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA event cancel request handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class EventCancelResponse extends AbstractCASResponseHandler {

	/**
	 * @param context
	 */
	public EventCancelResponse(CAJServerContext context) {
		super(context, "Event cancel request");
	}

	/**
	 * @see com.cosylab.epics.caj.impl.handlers.AbstractCAJResponseHandler#internalHandleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	protected void internalHandleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response) {

		CASTransport casTransport = (CASTransport)transport;
		
		// get channel by SID
		ServerChannel channel = casTransport.getChannel(parameter1);
		if (channel == null) 
		{
			sendException(transport, parameter1, CAStatus.BADCHID, response[0], null);
			disconnect(transport);
			return;
		} 

		// get monitor by IOID
		ServerMonitor monitor = channel.getMonitor(parameter2);
		if (monitor == null) 
		{
			sendException(transport, parameter1, CAStatus.BADMONID, response[0], null);
			disconnect(transport);
			return;
		} 
		
		// destroy
		monitor.destroy();
		
		// send response back
		try
		{
			new EventCancelRequest(transport, parameter1, parameter2, dataType, dataCount).submit();

		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when responding to event cancel request for channel with SID : " + parameter1, th);
		}
		
	}

	/**
	 * Disconnect.
	 */
	private void disconnect(Transport transport)
	{
		((CASTransport)transport).close(true);
	}
	
}
