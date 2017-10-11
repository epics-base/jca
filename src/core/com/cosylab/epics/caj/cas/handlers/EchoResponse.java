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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.requests.EchoRequest;
import com.cosylab.epics.caj.impl.Transport;

/**
 * Echo request handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class EchoResponse extends AbstractCASResponseHandler {

	/**
	 * Initial header buffer position.
	 */
	protected int initialHeaderBufferPosition = 0;
	
	/**
	 * @param context
	 */
	public EchoResponse(CAJServerContext context) {
		super(context, "Echo request");
	}

	/**
	 * @see com.cosylab.epics.caj.impl.handlers.AbstractCAJResponseHandler#internalHandleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	protected void internalHandleResponse(
		InetSocketAddress responseFrom,
		Transport  transport,
		ByteBuffer[] response) {

		ByteBuffer headerBuffer = ByteBuffer.wrap(response[0].array(), initialHeaderBufferPosition, response[0].position());
		ByteBuffer payloadBuffer = null;
		
		boolean udpResponse = (response.length == 1); 
		
		// UDP response
		if (udpResponse)
		{
			if (payloadSize > 0)
			{
				ByteBuffer postHeaderBuffer = response[0];
				int startPos = postHeaderBuffer.position();
				payloadBuffer = ByteBuffer.wrap(postHeaderBuffer.array(), startPos, payloadSize);
				
				// set position to a right place (needed for packed UDP reponses)
				postHeaderBuffer.position(startPos + payloadSize);
			}
		}
		// TCP reponse (payload already prepared)
		else 
		{
			payloadBuffer = response[1];
		}
		
		// send back
		try
		{
			EchoRequest echoRequest = new EchoRequest(transport, headerBuffer, payloadBuffer);
			if (udpResponse)
				context.getBroadcastTransport().send(echoRequest, responseFrom);
			else
				echoRequest.submit();
		} catch (Throwable th) {
			// context.getLogger().log(Level.WARNING, "Failed to send back echo response to: " + responseFrom, th);
		}
	}

	/**
	 * @see com.cosylab.epics.caj.impl.handlers.AbstractCAResponseHandler#handleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	public void handleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response)
	{
		// remeber initial headerBuffer position (since it can be in the middle of an UDP packet)
		initialHeaderBufferPosition = response[0].position();
		
		super.handleResponse(responseFrom, transport, response);
	}

}
