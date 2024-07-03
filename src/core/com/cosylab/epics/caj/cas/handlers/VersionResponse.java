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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.CASTransport;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.CAConstants;

/**
 * Version (request) handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class VersionResponse extends AbstractCASResponseHandler {

	/**
	 * @param context
	 */
	public VersionResponse(CAJServerContext context) {
		super(context, "Version request");
	}

	/**
	 * @see com.cosylab.epics.caj.impl.handlers.AbstractCAJResponseHandler#internalHandleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	protected void internalHandleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response) {

		boolean udpResponse = (response.length == 1); 

		// UDP request
		if (udpResponse)
		{
			// isSequenceValid flag
			if (dataCount != 0) 
			{
				// CA v4.11 or newer
				if (dataCount >= 11)
					context.setLastReceivedSequenceNumber(parameter1);
			}
		}
		else
		{
			// TCP only
			((CASTransport)transport).setPriority(dataType);

			// By responding to client with version info we indicate support for TCP name search
			// https://docs.epics-controls.org/en/latest/internal/ca_protocol.html#tcp-search
			// "CA_PROTO_SEARCH messages MUST NOT be sent on a Circuit unless a CA_PROTO_VERSION message has been received indicating >= CA_V412."

			ByteBuffer my_response = ByteBuffer.allocate(16);
			my_response.putShort((short)0); // Command: Version
			my_response.putShort((short)0); // Payload Size: nothing
			my_response.putShort((short)0); // Priority
			my_response.putShort(CAConstants.CA_MINOR_PROTOCOL_REVISION);
			my_response.putInt(0);          // Reserved parameter 1
			my_response.putInt(0);          // Reserved parameter 2
			try
			{
				((CASTransport)transport).send(my_response);
			}
			catch (Exception ex)
			{
				Logger.global.log(Level.WARNING, "Server cannot send version response", ex);
			}
		}
	}

}
