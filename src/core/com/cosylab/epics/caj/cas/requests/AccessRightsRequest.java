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

import gov.aps.jca.cas.ServerChannel;

import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.requests.AbstractCARequest;

/**
 * CA access rights response.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class AccessRightsRequest extends AbstractCARequest {

	/**
	 * @param transport
	 */
	public AccessRightsRequest(Transport transport, ServerChannel channel) {
		super(transport);
		
		int accessRights = 0;
		if (channel.readAccess())
			accessRights |= CAConstants.CA_PROTO_ACCESS_RIGHT_READ;
		if (channel.writeAccess())
			accessRights |= CAConstants.CA_PROTO_ACCESS_RIGHT_WRITE;
		
		requestMessage = insertCAHeader(transport, null,
										(short)22, (short)0, (short)0, (short)0,
										channel.getCID(), accessRights);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}

}
