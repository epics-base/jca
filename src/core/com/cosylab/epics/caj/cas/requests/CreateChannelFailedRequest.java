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

import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.requests.AbstractCARequest;

/**
 * CA create failed response.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CreateChannelFailedRequest extends AbstractCARequest {

	/**
	 * @param transport
	 */
	public CreateChannelFailedRequest(Transport transport, int cid) {
		super(transport);
		
		requestMessage = insertCAHeader(transport, null,
										(short)26, (short)0, (short)0, (short)0,
										cid, 0);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}

}
