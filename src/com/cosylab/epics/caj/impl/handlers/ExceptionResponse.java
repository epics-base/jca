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

import gov.aps.jca.CAStatus;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.cosylab.epics.caj.CAJConstants;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.ExceptionHandler;
import com.cosylab.epics.caj.impl.Transport;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ExceptionResponse extends AbstractCAJResponseHandler
	implements ExceptionHandler {

	/**
	 * Table of exception handlers for each command ID.
	 */
	private ExceptionHandler[] handlerTable;

	/**
	 * Debug flag.
	 */
	protected boolean debug = false;

	/**
	 * @param context
	 */
	public ExceptionResponse(CAJContext context) {
		super(context, "Exception respone");
		intializeHandlerTable();

		// TODO tmp
		debug = System.getProperties().containsKey(CAJConstants.CAJ_DEBUG);
	}

	/**
	 * Initialize handler table.
	 */
	private void intializeHandlerTable()
	{
		ExceptionHandler defaultException = this;
		ResponseRequestExceptionHandler rreh = new ResponseRequestExceptionHandler(context);
		
		handlerTable = new ExceptionHandler[]
			{
				defaultException, /*  0 */
				rreh, /*  1 */
				defaultException, /*  2 */
				defaultException, /*  3 */
				defaultException, /*  4 */
				defaultException, /*  5 */
				defaultException, /*  6 */
				defaultException, /*  7 */
				defaultException, /*  8 */
				defaultException, /*  9 */
				defaultException, /* 10 */
				defaultException, /* 11 */
				defaultException, /* 12 */
				defaultException, /* 13 */
				defaultException, /* 14 */
				rreh, /* 15 */
				defaultException, /* 16 */
				defaultException, /* 17 */
				defaultException, /* 18 */
				rreh, /* 19 */
				defaultException, /* 20 */
				defaultException, /* 21 */
				defaultException, /* 22 */
				defaultException, /* 23 */
				defaultException, /* 24 */
				defaultException, /* 25 */
				defaultException, /* 26 */
				defaultException, /* 27 */
			};
	}

	/**
	 * @see com.cosylab.epics.caj.impl.handlers.AbstractCAJResponseHandler#internalHandleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	protected void internalHandleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response) {
			
		ByteBuffer originalHeaderBuffer = null;
		String errorMessage = null;

		ByteBuffer payloadBuffer;
		if (response.length == 1)
			// UDP response
			payloadBuffer = response[0];
		else
			// TCP response
			payloadBuffer = response[1];
			
		int payloadStart = payloadBuffer.position();
			
		// zero termination string expected, i.e. at least CAConstants.CA_MESSAGE_HEADER_SIZE + 1 bytes
		if (payloadSize > CAConstants.CA_MESSAGE_HEADER_SIZE)
		{
			int originalHeaderPayloadSize = payloadBuffer.getShort(payloadStart + 2) & 0xFFFF;
					
			// extended message header check
			int originalHeaderSize;
			if (originalHeaderPayloadSize == 0xFFFF)
				originalHeaderSize = CAConstants.CA_EXTENDED_MESSAGE_HEADER_SIZE;
			else
				originalHeaderSize = CAConstants.CA_MESSAGE_HEADER_SIZE;
			
			originalHeaderBuffer = payloadBuffer.slice();
                        originalHeaderBuffer.limit(originalHeaderSize);

			// find zero-termination (from end is efficient, but not error-proof)
			int errorMessageStart = payloadStart + originalHeaderSize;
			int errorMessageEnd = errorMessageStart;
			while (payloadBuffer.get(errorMessageEnd) != 0)
				errorMessageEnd++;
                        payloadBuffer.position(errorMessageStart);
                        ByteBuffer errorMessageBuffer = payloadBuffer.slice();
                        errorMessageBuffer.limit(errorMessageEnd - errorMessageStart);
                        errorMessage = Charset.defaultCharset().decode(errorMessageBuffer).toString();
		}
		
		// read rest of the payload (needed for UDP)
		payloadBuffer.position(payloadStart + payloadSize);
		
		//
		// delegate
		//
		
		// peek for command ID from original header
		short commandID = (originalHeaderBuffer != null ? originalHeaderBuffer.getShort(0) : -1);
		if (commandID < 0 || commandID >= handlerTable.length)
		{
			context.getLogger().fine("Invalid (or unsupported) exception command: " + commandID + ".");
			this.handleException(parameter2, parameter1, errorMessage, originalHeaderBuffer);
			return;
		}
		
		// TODO remove
		if (debug)
			context.getLogger().fine("Exception occured, code: " + CAStatus.forStatusCode(parameter2) + ", message: '" + errorMessage + "'.");
System.err.println("Exception occured, code: " + CAStatus.forStatusCode(parameter2) + ", message: '" + errorMessage + "'.");	
		//delegate
		/* exception code, channel id, error message, original header) */
		handlerTable[commandID].handleException(parameter2, parameter1, errorMessage, originalHeaderBuffer);
		
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ExceptionHandler#handleException(int, int, java.lang.String, java.nio.ByteBuffer)
	 */
	public void handleException(int errorCode, int cid,
								String errorMessage, ByteBuffer originalHeaderBuffer) {
		
		// TODO log or not to log?
		context.getLogger().fine("Exception occured, code: " + CAStatus.forStatusCode(errorCode) + ", message: '" + errorMessage + "'.");
	}

}
