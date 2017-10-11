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

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.requests.AbstractCARequest;
import com.cosylab.epics.caj.util.InetAddressUtil;

/**
 * CA beacon (SRV_IS_UP) request.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class BeaconRequest extends AbstractCARequest {

	/**
	 * @param transport
	 */
	public BeaconRequest(Transport transport, int beaconSequenceID) {
		super(transport);
		
		// set server IP address
		int serverIP = 0;
		InetAddress serverAddress = ((CAJServerContext)(transport.getContext())).getServerInetAddress();
		if (serverAddress != null && !serverAddress.isAnyLocalAddress())
			serverIP = InetAddressUtil.ipv4AddressToInt(serverAddress);

		requestMessage = insertCAHeader(transport, null,
										(short)13, (short)0, (short)transport.getContext().getServerPort(), (short)0,
										beaconSequenceID, serverIP);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}

}
