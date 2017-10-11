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
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA search request.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class SearchRequest extends AbstractCARequest {

	/**
	 * @param transport
	 */
	public SearchRequest(Transport transport, String name, int cid) {
		super(transport);
		requestMessage = generateSearchRequestMessage(transport, null, name, cid);
	}

	/**
	 * Generate search request message.
	 * @param transport
	 * @param requestMessage
	 * @param name
	 * @param cid
	 * @return <code>null</code> if failed to put message on existing <code>requestMessage</code>.
	 */
	public static final ByteBuffer generateSearchRequestMessage(Transport transport, ByteBuffer requestMessage,
			String name, int cid)
	{
		if (name.length() > Math.min(CAConstants.MAX_UDP_SEND - CAConstants.CA_MESSAGE_HEADER_SIZE, 0xFFFF))
			throw new IllegalArgumentException("name too long");

		int alignedMessageSize = calculateAlignedSize(8, CAConstants.CA_MESSAGE_HEADER_SIZE + name.length() + 1);
		if (requestMessage == null)
			requestMessage = ByteBuffer.allocate(alignedMessageSize);
		else if (requestMessage.remaining() < alignedMessageSize)
			return null;
		requestMessage = insertCAHeader(transport, requestMessage,
										(short)6, (short)(alignedMessageSize - CAConstants.CA_MESSAGE_HEADER_SIZE),
										CAConstants.CA_SEARCH_DONTREPLY, transport.getMinorRevision(),
										cid, cid);
		// append zero-terminated string and align message
		requestMessage.put(name.getBytes());
		requestMessage.put((byte)0);
		requestMessage = alignBuffer(8, requestMessage);
		
		return requestMessage;
	}
}
