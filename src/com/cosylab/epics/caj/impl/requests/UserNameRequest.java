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

package com.cosylab.epics.caj.impl.requests;

import java.nio.ByteBuffer;

import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class UserNameRequest extends AbstractCARequest {

	/**
	 * @param transport
	 */
	public UserNameRequest(Transport transport) {
		super(transport);
		
		// compatibility check
		if ( transport.getMinorRevision() < 1)
			return;
		
		String userName = System.getProperty("user.name", "nobody");
		int alignedMessageSize = calculateAlignedSize(8, CAConstants.CA_MESSAGE_HEADER_SIZE + userName.length() + 1);
		requestMessage = ByteBuffer.allocate(alignedMessageSize);
		requestMessage = insertCAHeader(transport, requestMessage,
										(short)20, (short)(alignedMessageSize - CAConstants.CA_MESSAGE_HEADER_SIZE),
										(short)0, (short)0,
										0, 0);
		// append zero-terminated string and align message
		requestMessage.put(userName.getBytes());
		requestMessage.put((byte)0);
		requestMessage = alignBuffer(8, requestMessage);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}

}
