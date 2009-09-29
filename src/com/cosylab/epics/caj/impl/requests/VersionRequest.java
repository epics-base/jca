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

import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA version request.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class VersionRequest extends AbstractCARequest {

	/**
	 * @param transport
	 * @param priority
	 */
	public VersionRequest(Transport transport, short priority) {
		this(transport, priority, 0, false);
	}

	/**
	 * Used for determining UDP roundtrip times.
	 * @param transport
	 * @param priority
	 * @param sequenceNumber
	 * @param isSequenceNumberValid
	 */
	public VersionRequest(Transport transport, short priority, int sequenceNumber, boolean isSequenceNumberValid) {
		super(transport);
		requestMessage = generateVersionRequestMessage(transport, null, priority, sequenceNumber, isSequenceNumberValid);
	}

	/**
	 * Generate version request message.
	 * @param transport
	 * @param buffer
	 * @param priority
	 * @param sequenceNumber
	 * @param isSequenceNumberValid
	 * @return generated version message buffer.
	 */
	public static final ByteBuffer generateVersionRequestMessage(
			Transport transport, ByteBuffer buffer, short priority, 
			int sequenceNumber, boolean isSequenceNumberValid)
	{
		short isSequenceNumberValidCode = isSequenceNumberValid ? (short)1 : (short)0;
		return insertCAHeader(transport, buffer,
				(short)0, 0, isSequenceNumberValid ? isSequenceNumberValidCode : priority, transport.getMinorRevision(),
				sequenceNumber, 0);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}

}
