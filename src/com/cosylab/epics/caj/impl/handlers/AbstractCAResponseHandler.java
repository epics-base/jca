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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.CAJConstants;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.ResponseHandler;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.util.HexDump;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public abstract class AbstractCAResponseHandler implements ResponseHandler {

	/**
	 * CA response description.
	 */
	protected String description;

	/**
	 * Command field of the header.
	 */
	protected short command;

	/**
	 * Payload size field of the header.
	 */
	protected int payloadSize;

	/**
	 * Data type field of the header.
	 */
	protected short dataType;

	/**
	 * Data count field of the header.
	 * NOTE: extended 
	 */
	protected int dataCount;

	/**
	 * Parameter 1 field of the header.
	 */
	protected int parameter1;

	/**
	 * Parameter 2 field of the header.
	 */
	protected int parameter2;
	
	/**
	 * Debug flag.
	 */
	protected boolean debug = false;

	/**
	 * @param description
	 */
	public AbstractCAResponseHandler(String description) {
		this.description = description;
		
		// TODO tmp
		debug = System.getProperties().containsKey(CAJConstants.CAJ_DEBUG);
	}

	/**
	 * NOTE: (Extended) header buffer size validation should have been already done.
	 * <code>response</code> is array of maximum of 2 elments:
	 * #UDP: contains whole response
	 * #TCP: first element contains complete standard or extended response message header, second whole payload.
	 * No buffer size checking is done.
	 * @see com.cosylab.epics.caj.impl.ResponseHandler#handleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	public void handleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response)
	{
		parseHeader(response[0]);
		
		// TODO remove debug output
		if (debug)
		{
			// payload dump
			if (payloadSize > 0 && response.length > 1 && response[1].hasArray())
			{
				HexDump.hexDump(description + " payload", response[1].array(),
								response[1].position(),
								response[1].limit() - response[1].position()); 
			}
		}

		internalHandleResponse(responseFrom, transport, response);
	}
	
	/**
	 * Called after header is parsed by <code>parseHeader()</code> method.
	 * @see com.cosylab.epics.caj.impl.ResponseHandler#handleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	protected abstract void internalHandleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response);

	/**
	 * Parse CA response header.
	 * @param headerBuffer	response header to be parsed.
	 */
	protected void parseHeader(ByteBuffer headerBuffer)
	{
		// TODO remove debug 
		int startPos = headerBuffer.position();
		boolean isExtended = false;

		//
		// read fields
		//
		
		command     = headerBuffer.getShort();
		// signed short conversion -> signed int
		payloadSize = headerBuffer.getShort() & 0xFFFF;
		dataType    = headerBuffer.getShort();
		// signed short conversion -> signed int 
		dataCount   = headerBuffer.getShort() & 0xFFFF;
		parameter1  = headerBuffer.getInt();
		parameter2  = headerBuffer.getInt();


		// TODO remove debug 
		if (payloadSize == 0xFFFF)
			isExtended = true;


		// extended header
		if (payloadSize == 0xFFFF)
		{
			/*
			 * Because Java can't represent negative int as a 32 bit positive integer, it has to be promoted to a long:
			 *  (1) Assign it to a long. 
			 *  (2) Clear the upper 32 bit of the long by logical AND with 0x00000000FFFFFFFF. 
			 *  (3) The resulting long is a positive number.
			 * 
			 * Anyway, maximum buffer size is limited w/ Integer.MAX_VALUE,
			 * so int type us used and values > Integer.MAX_VALUE are not supported.  
			 */
			payloadSize = headerBuffer.getInt();
			dataCount   = headerBuffer.getInt();
		}
		
		// TODO remove debug output
		if (debug)
		{
			if (headerBuffer.hasArray())
			{
				// payload is stored in response[1]
				HexDump.hexDump(description, headerBuffer.array(),
								startPos,
								isExtended ? CAConstants.CA_EXTENDED_MESSAGE_HEADER_SIZE:
										 	 CAConstants.CA_MESSAGE_HEADER_SIZE);
			}
			else
			{
				int len = isExtended ? CAConstants.CA_EXTENDED_MESSAGE_HEADER_SIZE:
				 	 				   CAConstants.CA_MESSAGE_HEADER_SIZE;
				ByteBuffer bb = ByteBuffer.allocate(len);
				if (!isExtended)
				{
					bb.putShort((byte)command);
					bb.putShort((short)payloadSize);
					bb.putShort((short)dataType);
					bb.putShort((short)dataCount);
					bb.putInt((int)parameter1);
					bb.putInt((int)parameter2);
				}
				else
				{
					bb.putShort((byte)command);
					bb.putShort((short)0xFFFF);
					bb.putShort((short)dataType);
					bb.putShort((short)0x0000);
					bb.putInt((int)parameter1);
					bb.putInt((int)parameter2);
					bb.putInt((int)payloadSize);
					bb.putInt((int)dataCount);
				}
				HexDump.hexDump(description, bb.array(),
						0,
						len);
			}
		}

	}
	
}
