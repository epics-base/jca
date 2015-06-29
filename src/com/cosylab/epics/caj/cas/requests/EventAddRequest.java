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

import gov.aps.jca.CAStatus;
import gov.aps.jca.dbr.DBR;

import java.nio.ByteBuffer;

import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.DBREncoder;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.requests.AbstractCARequest;

/**
 * CA event add response notify.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class EventAddRequest extends AbstractCARequest {

	/**
	 * @param transport
	 * @param ioid
	 * @param dataType
	 * @param dataCount
	 * @param status
	 * @param value
	 */
	public EventAddRequest(Transport transport, int ioid, short dataType, int dataCount, CAStatus status, DBR value)
	{
		super(transport);

		int calculatedPayloadSize = DBREncoder.calculatePayloadSize((short)dataType, dataCount, value.getValue());
		int alignedPayloadSize = calculateAlignedSize(8, calculatedPayloadSize);
		int alignedMessageSize = alignedPayloadSize + CAConstants.CA_MESSAGE_HEADER_SIZE;
		
		boolean extendedHeader = calculatedPayloadSize >= 0xFFFF || dataCount >= 0xFFFF;
		if (extendedHeader)
		{
			if (transport.getMinorRevision() < 9)
				throw new IllegalStateException("Message payload size " + alignedPayloadSize + " or element count " + dataCount + " out of bounds for CA revision " + transport.getMinorRevision() + ".");

			// add additional payload fields				
			alignedMessageSize += 8;
		}
		
		requestMessage = ByteBuffer.allocate(alignedMessageSize);

		requestMessage = insertCAHeader(transport, requestMessage,
										(short)1, alignedPayloadSize, dataType, dataCount,
										status.getStatusCode(), ioid);
											
		// append value and align message
		DBREncoder.insertPayload(requestMessage, dataType, dataCount, value);
		
		// it can happen that value.getCount() < dataCount and
		// insertPayload will not fill-up the whole message
		requestMessage.position(alignedMessageSize);
		requestMessage = alignBuffer(8, requestMessage);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}

}
