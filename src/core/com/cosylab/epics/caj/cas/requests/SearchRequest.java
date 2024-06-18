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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	private static final Logger logger = Logger.getLogger(SearchRequest.class.getName());

	/**
	 * @param transport Transport to use
	 * @param other_address Optional other address to report, null to use this transport
	 * @param with_version Include minor version in payload (for UDP), or no payload (for TCP)?
	 * @param clientMinorVersion
	 * @param cid Channel ID
	 */
	public SearchRequest(Transport transport, InetSocketAddress other_address, boolean with_version, short clientMinorVersion, int cid) {
		super(transport);

		// add minor version payload (aligned by 8)
		final int alignedMessageSize = CAConstants.CA_MESSAGE_HEADER_SIZE + 8;
		requestMessage = ByteBuffer.allocate(alignedMessageSize);

		// set server IP address
		int serverIP = 0xFFFFFFFF;
		int port = other_address == null
		         ? transport.getContext().getServerPort()
				 : other_address.getPort();

		if (clientMinorVersion >= 8)
		{
			InetAddress serverAddress = other_address == null
			                          ? ((CAJServerContext)(transport.getContext())).getServerInetAddress()
									  : other_address.getAddress();
			if (serverAddress != null && !serverAddress.isAnyLocalAddress())
				serverIP = InetAddressUtil.ipv4AddressToInt(serverAddress);
		}

		requestMessage = insertCAHeader(transport, requestMessage,
										(short)6, (short)(with_version ? 8 : 0), (short)port, 0,
										serverIP, cid);
		if (with_version)
		{
			requestMessage.putShort(CAConstants.CAS_MINOR_PROTOCOL_REVISION);
			// clear rest of the message (security)
			requestMessage.putShort((short)0);
			requestMessage.putInt(0);
			logger.log(Level.FINE, "Replying to search with " + serverIP + ":" + port + ", minor " + CAConstants.CAS_MINOR_PROTOCOL_REVISION);
		}
		logger.log(Level.FINE, "Replying to search with " + serverIP + ":" + port + ", no payload");
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}

}
