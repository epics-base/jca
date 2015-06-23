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

import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.requests.AbstractCARequest;
import com.cosylab.epics.caj.util.InetAddressUtil;

/**
 * CA search response.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class SearchRequest extends AbstractCARequest {

	/**
	 * @param transport
	 */
	public SearchRequest(Transport transport, short clientMinorVersion, int cid) {
		super(transport);

		// add minor version payload (aligned by 8)
		final int alignedMessageSize = CAConstants.CA_MESSAGE_HEADER_SIZE + 8;
		requestMessage = ByteBuffer.allocate(alignedMessageSize);

		// set server IP address
		int serverIP = 0xFFFFFFFF;
		if (clientMinorVersion >= 8)
		{
			InetAddress serverAddress = ((CAJServerContext)(transport.getContext())).getServerInetAddress();
			if (serverAddress != null && !serverAddress.isAnyLocalAddress())
				serverIP = InetAddressUtil.ipv4AddressToInt(serverAddress);
		}
		
		requestMessage = insertCAHeader(transport, requestMessage,
										(short)6, (short)8, (short)transport.getContext().getServerPort(), 0,
										serverIP, cid);
		
		requestMessage.putShort(CAConstants.CAS_MINOR_PROTOCOL_REVISION);
		// clear rest of the message (security)
		requestMessage.putShort((short)0);
		requestMessage.putInt(0);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}

}
