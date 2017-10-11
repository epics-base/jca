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

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.util.HexDump;

/**
 * Bad request handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class BadResponse extends AbstractCASResponseHandler {

	/**
	 * @param context
	 */
	public BadResponse(CAJServerContext context) {
		super(context, "Bad request");
	}

	/**
	 * @see com.cosylab.epics.caj.impl.handlers.AbstractCAJResponseHandler#internalHandleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	protected void internalHandleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response) {
		// noop
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseHandler#handleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	public void handleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response)
	{
			ByteBuffer headerBuffer = response[0];
			int startPos = headerBuffer.position();
			int command = headerBuffer.getShort();
			
			context.getLogger().fine("Undecipherable message (bad request type " + command + ") from " + responseFrom + ".");

			// TODO remove debug output
			HexDump.hexDump(description, headerBuffer.array(), startPos, headerBuffer.limit() - startPos);
			
			// flush all header buffer (needed for UDP packed responses)
			headerBuffer.position(headerBuffer.limit());
	}

}
