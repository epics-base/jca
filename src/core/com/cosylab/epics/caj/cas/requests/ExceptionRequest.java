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

package com.cosylab.epics.caj.cas.requests;

import gov.aps.jca.CAStatus;

import java.nio.ByteBuffer;

import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.requests.AbstractCARequest;

/**
 * CA exception (error) response.
 * NOTE: extended header not supported (error messages are not so long).
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ExceptionRequest extends AbstractCARequest {

	/**
	 * @param transport
	 */
	public ExceptionRequest(Transport transport, int cid, CAStatus errorCode,
							ByteBuffer previousHeader, String errorMessage) {
		super(transport);

		// use default message if none provided
		int binaryMessageLength = 0;
		if (errorMessage != null)
			binaryMessageLength = errorMessage.length() + 1;
		 
		int alignedMessageSize = calculateAlignedSize(8, CAConstants.CA_MESSAGE_HEADER_SIZE + previousHeader.remaining() + binaryMessageLength);
		requestMessage = ByteBuffer.allocate(alignedMessageSize);
		requestMessage = insertCAHeader(transport, requestMessage,
										(short)11, (short)(alignedMessageSize - CAConstants.CA_MESSAGE_HEADER_SIZE),
										(short)0, (short)0,
										cid, errorCode.getStatusCode());
		
		// put previous header, i.e. header that caused an error
		requestMessage.put(previousHeader);								
		
		// put errorMessage
		if (binaryMessageLength > 0)
		{
			// append zero-terminated string and align message
			requestMessage.put(errorMessage.getBytes());
			requestMessage.put((byte)0);
		}
		
		requestMessage = alignBuffer(8, requestMessage);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}

}
