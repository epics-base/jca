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

import java.io.IOException;
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CreateChannelRequest extends AbstractCARequest {

	/**
	 * Client channel ID.
	 */
	protected int cid;
	
	/**
	 * @param transport
	 * @param channelName
	 * @param cid
	 */
	public CreateChannelRequest(Transport transport, String channelName, int cid) {
		super(transport);
		
		this.cid = cid;
		
		// v4.4+ or newer
		if (transport.getMinorRevision() < 4)
		{
			// no name used, since cid as already a sid
			channelName = null;
		}
		 
		int binaryNameLength = 0;
		if (channelName != null)
			binaryNameLength = channelName.length() + 1;
		 
		int alignedMessageSize = calculateAlignedSize(8, CAConstants.CA_MESSAGE_HEADER_SIZE + binaryNameLength);
		requestMessage = ByteBuffer.allocate(alignedMessageSize);
		requestMessage = insertCAHeader(transport, requestMessage,
										(short)18, (short)(alignedMessageSize - CAConstants.CA_MESSAGE_HEADER_SIZE),
										(short)0, (short)0,
										cid, transport.getMinorRevision());
										
		if (binaryNameLength > 0)
		{
			// append zero-terminated string and align message
			requestMessage.put(channelName.getBytes());
			requestMessage.put((byte)0);
		}
		requestMessage = alignBuffer(8, requestMessage);
	}

	/**
	 * If sync. request (i.e. w/o callback), additionally increment context pending requests.
	 * @see com.cosylab.epics.caj.impl.Request#submit()
	 */
	public void submit() throws IOException {
		super.submit();
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}


}
