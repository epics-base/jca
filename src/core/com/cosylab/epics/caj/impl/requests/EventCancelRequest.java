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
 * CA event cacnel request (cancels a monitor on channel).
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class EventCancelRequest extends AbstractCARequest {

	/**
	 * @param transport
	 * @param sid
	 * @param subsid
	 * @param dataType
	 * @param dataCount
	 */
	public EventCancelRequest(Transport transport, int sid, int subsid, int dataType, int dataCount) {
		super(transport);

		requestMessage = insertCAHeader(transport, null,
										(short)2, (short)0, (short)dataType, (short)dataCount,
										sid, subsid);
	}

}
