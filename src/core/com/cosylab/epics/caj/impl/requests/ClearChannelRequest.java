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

import com.cosylab.epics.caj.impl.Transport;

/**
 * CA clear channel.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ClearChannelRequest extends AbstractCARequest {
	/**
	 * @param transport
	 * @param cid
	 * @param sid
	 */
	public ClearChannelRequest(Transport transport, int cid, int sid) {
		super(transport);
		requestMessage = insertCAHeader(transport, null,
										(short)12, (short)0, (short)0, (short)0,
										sid, cid);
	}

}
