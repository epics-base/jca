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

import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA echo request.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class EchoRequest extends AbstractCARequest {

	/**
	 * @param transport
	 */
	public EchoRequest(Transport transport) {
		super(transport);
		
		if (transport.getMinorRevision() >= 3)
			requestMessage = insertCAHeader(transport, null,
											(short)23, (short)0, (short)0, (short)0,
											0, 0);
		else
			// use CA version request as echo 
			requestMessage = insertCAHeader(transport, null,
											(short)0, (short)0, (short)0, transport.getMinorRevision(),
											0, 0);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}

}
