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
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.handlers.AccessRightsResponse;
import com.cosylab.epics.caj.impl.handlers.BadResponse;
import com.cosylab.epics.caj.impl.handlers.BeaconResponse;
import com.cosylab.epics.caj.impl.handlers.ChannelDisconnectedResponse;
import com.cosylab.epics.caj.impl.handlers.ClearChannelResponse;
import com.cosylab.epics.caj.impl.handlers.CreateChannelFailedResponse;
import com.cosylab.epics.caj.impl.handlers.CreateChannelResponse;
import com.cosylab.epics.caj.impl.handlers.EchoResponse;
import com.cosylab.epics.caj.impl.handlers.EventAddResponse;
import com.cosylab.epics.caj.impl.handlers.EventCancelResponse;
import com.cosylab.epics.caj.impl.handlers.ExceptionResponse;
import com.cosylab.epics.caj.impl.handlers.NoopResponse;
import com.cosylab.epics.caj.impl.handlers.ReadNotifyResponse;
import com.cosylab.epics.caj.impl.handlers.RepeaterConfirmResponse;
import com.cosylab.epics.caj.impl.handlers.SearchResponse;
import com.cosylab.epics.caj.impl.handlers.VersionResponse;
import com.cosylab.epics.caj.impl.handlers.WriteNotifyResponse;

/**
 * CA response handler - main handler which dispatches responses to appripriate handlers.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAResponseHandler implements ResponseHandler {

	/**
	 * Table of response handlers for each command ID.
	 */
	private ResponseHandler[] handlerTable;

	/**
	 * Context instance.
	 */
	private CAJContext context;
	
	/**
	 * @param context
	 */
	public CAResponseHandler(CAJContext context) {
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
				badResponse, /*  4 */
				badResponse, /*  5 */
				new SearchResponse(context), /*  6 */
				badResponse, /*  7 */
				badResponse, /*  8 */
				badResponse, /*  9 */
				badResponse, /* 10 */
				new ExceptionResponse(context), /* 11 */
				new ClearChannelResponse(context), /* 12 */
				new BeaconResponse(context), /* 13 */
				new NoopResponse(context, "Channel not found"), /* 14 */
				new ReadNotifyResponse(context), /* 15 */
				badResponse, /* 16 */
				new RepeaterConfirmResponse(context), /* 17 */
				new CreateChannelResponse(context), /* 18 */
				new WriteNotifyResponse(context), /* 19 */
				badResponse, /* 20 */
				badResponse, /* 21 */
				new AccessRightsResponse(context), /* 22 */
				new EchoResponse(context), /* 23 */
				badResponse, /* 24 */
				badResponse, /* 25 */
				new CreateChannelFailedResponse(context), /* 26 */
				new ChannelDisconnectedResponse(context), /* 27 */
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
			// XXX switching to direct buffers, headerBuffer.array() may not be available
			// HexDump.hexDump("Invalid CA Header", headerBuffer.array(), headerBuffer.position(), headerBufferSize);
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
			// XXX switching to direct buffers, headerBuffer.array() may not be available
			// HexDump.hexDump("Invalid CA Header", headerBuffer.array(), headerBuffer.position(), headerBufferSize);
			// flush all header buffer (needed for UDP packed responses)
			headerBuffer.position(headerBuffer.limit());
			return;
		}
		
		//delegate
		handlerTable[commandID].handleResponse(responseFrom, transport, response);

	}

}
