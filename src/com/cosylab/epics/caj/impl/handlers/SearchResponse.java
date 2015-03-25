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

package com.cosylab.epics.caj.impl.handlers;

import gov.aps.jca.dbr.DBR;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.util.InetAddressUtil;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class SearchResponse extends AbstractCAJResponseHandler {

	/**
	 * @param context
	 */
	public SearchResponse(CAJContext context) {
		super(context, "Search response");
	}

	/**
	 * UDP response. 
	 * @see com.cosylab.epics.caj.impl.handlers.AbstractCAJResponseHandler#internalHandleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	protected void internalHandleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response) {

		//
		// determine server address
		//

		short minorVersion = CAConstants.CA_UNKNOWN_MINOR_PROTOCOL_REVISION;
		
		// Starting with CA V4.1 the minor version number is
		// appended to the end of each search reply.
		int payloadStart = response[0].position();
		if (payloadSize >= 2 /* short size = 2 bytes */)
		{ 
			// UDP response (all in buffer 0)
			minorVersion = response[0].getShort();
		}
			
		// read rest of the playload (needed for UDP)
		response[0].position(payloadStart + payloadSize);
		
		// signed short conversion -> signed int 
		int port = dataType & 0xFFFF;

		// CA v4.8 or newer
		if (minorVersion >= 8)
		{
			InetAddress addr;
			
			// get address
			final int INADDR_BROADCAST = 0xFFFFFFFF; 
			if (parameter1 != INADDR_BROADCAST)
				addr = InetAddressUtil.intToIPv4Address(parameter1);
			else
				addr = responseFrom.getAddress(); 			
			
			responseFrom = new InetSocketAddress(addr, port);
		}
		// CA v4.5 - 4.7
		else if (minorVersion >= 5 )
		{
			responseFrom = new InetSocketAddress(responseFrom.getAddress(), port);
		}
		// CA v4.1 - 4.6
		else
		{
			responseFrom = new InetSocketAddress(responseFrom.getAddress(), context.getServerPort());
		}



		// CA v4.2 or newer
		if (minorVersion >= 2)
		{
			/** cid, sid, type, count, minorVersion, serverAddress */
			context.searchResponse(parameter2, parameter1, (short)DBR.TYPE.getValue(), 0,
								   minorVersion, responseFrom );
		}
		else
		{
			/** cid, sid, type, count, minorVersion, serverAddress */
			context.searchResponse(parameter2, parameter1, dataType, dataCount,
								   minorVersion, responseFrom );
		}

	}

}
