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

import java.nio.ByteBuffer;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.DBREncoder;
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA write notify.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class WriteRequest extends AbstractCARequest {

	/**
	 * Context.
	 */
	protected CAJContext context;

	/**
	 * Channel.
	 */
	protected CAJChannel channel;

	/**
	 * @param channel 
	 * @param transport
	 * @param sid
	 * @param cid
	 * @param dataType
	 * @param dataCount
	 * @param value
	 */
	public WriteRequest(CAJChannel channel,
							 Transport transport, int sid, int cid, int dataType, int dataCount,
							 Object value) {
		super(transport);

		this.channel = channel;
		context = (CAJContext)transport.getContext(); // or channel.getContext()
		
		int calculatedPayloadSize = DBREncoder.calculatePayloadSize((short)dataType, dataCount, value);
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
										(short)4, alignedPayloadSize, (short)dataType, dataCount,
										sid, cid);
											
		// append value and align message
		DBREncoder.insertPayload(requestMessage, (short)dataType, dataCount, value);
		requestMessage = alignBuffer(8, requestMessage);
	}

}
