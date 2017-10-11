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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.ResponseHandler;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.cas.handlers.*;
import com.cosylab.epics.caj.util.HexDump;

/**
 * CAS request handler - main handler which dispatches requests to appripriate handlers.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CASResponseHandler implements ResponseHandler {

	/**
	 * Table of response handlers for each command ID.
	 */
	private ResponseHandler[] handlerTable;

	/**
	 * Context instance.
	 */
	private CAJServerContext context;
	
	/**
	 * @param context
	 */
	public CASResponseHandler(CAJServerContext context) {
		this.context = context;
		intializeHandlerTable();
	}

	/**
	 * Initialize handler table.
	 */
	private void intializeHandlerTable()
	{
		ResponseHandler badResponse = new BadResponse(context);
		
		handlerTable = new ResponseHandler[]
			{
				new VersionResponse(context), /*  0 */
				new EventAddResponse(context), /*  1 */
				new EventCancelResponse(context), /*  2 */
				badResponse, /*  3 */
				new WriteResponse(context), /*  4 */
				badResponse, /*  5 */
				new SearchResponse(context), /*  6 */
				badResponse, /*  7 */
				new EventsOffResponse(context), /*  8 */
				new EventsOnResponse(context), /*  9 */
				badResponse, /* 10 */
				badResponse, /* 11 */
				new ClearChannelResponse(context), /* 12 */
				badResponse, /* 13 */
				badResponse, /* 14 */
				new ReadNotifyResponse(context), /* 15 */
				new NoopResponse(context, "Read build request"), /* 16 */
				badResponse, /* 17 */
				new CreateChannelResponse(context), /* 18 */
				new WriteNotifyResponse(context), /* 19 */
				new UsernameResponse(context), /* 20 */
				new HostnameResponse(context), /* 21 */
				badResponse, /* 22 */
				new EchoResponse(context), /* 23 */
				badResponse, /* 24 */
				badResponse, /* 25 */
				badResponse, /* 26 */
				badResponse, /* 27 */
			};
	}
	
	/**
	 * @see com.cosylab.epics.caj.impl.ResponseHandler#handleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	public void handleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response) {
			
		ByteBuffer headerBuffer = response[0];
		int headerBufferSize = headerBuffer.limit() - headerBuffer.position();
		if (headerBufferSize < CAConstants.CA_MESSAGE_HEADER_SIZE)
		{
			context.getLogger().fine("Invalid response header (" + CAConstants.CA_MESSAGE_HEADER_SIZE + " bytes expected, " + headerBufferSize + " received) from " + responseFrom + ".");
			// TODO remove debug output
			HexDump.hexDump("Invalid CA Header", headerBuffer.array(), headerBuffer.position(), headerBufferSize);
			// flush all header buffer (needed for UDP packed responses)
			headerBuffer.position(headerBuffer.limit());
			return;
		}

		// peek for reponse ID
		short commandID = headerBuffer.getShort(headerBuffer.position());
		if (commandID < 0 || commandID >= handlerTable.length)
		{
			context.getLogger().fine("Invalid (or unsupported) command: " + commandID + ".");
			// TODO remove debug output
			HexDump.hexDump("Invalid CA Header", headerBuffer.array(), headerBuffer.position(), headerBufferSize);
			// flush all header buffer (needed for UDP packed responses)
			headerBuffer.position(headerBuffer.limit());
			return;
		}
		
		//delegate
		handlerTable[commandID].handleResponse(responseFrom, transport, response);

	}

}
