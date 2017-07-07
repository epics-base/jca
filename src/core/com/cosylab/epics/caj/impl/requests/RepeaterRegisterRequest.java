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

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.util.InetAddressUtil;

/**
 * CA repeater register request.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class RepeaterRegisterRequest extends AbstractCARequest {

	/**
	 * @param transport
	 */
	public RepeaterRegisterRequest(Transport transport)  throws UnknownHostException {
		super(transport);

		int localAddress = InetAddressUtil.ipv4AddressToInt(InetAddress.getByName("127.0.0.1"));
		requestMessage = insertCAHeader(transport, null,
										(short)24, (short)0, (short)0, (short)0,
										0, localAddress);
	}

}
