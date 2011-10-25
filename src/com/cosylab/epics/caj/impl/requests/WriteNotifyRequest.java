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

import gov.aps.jca.CAStatus;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.event.ContextExceptionEvent;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;

import java.nio.ByteBuffer;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.DBREncoder;
import com.cosylab.epics.caj.impl.NotifyResponseRequest;
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA write notify.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class WriteNotifyRequest extends AbstractCARequest implements NotifyResponseRequest {

	/**
	 * Context.
	 */
	protected CAJContext context;

	/**
	 * I/O ID given by the context when registered.
	 */
	protected int ioid;

	/**
	 * Response callback listener.
	 */
	protected PutListener callback;

	/**
	 * Channel.
	 */
	protected CAJChannel channel;

	/**
	 * Requested data type.
	 */
	protected int requestedDataType;
	
	/**
	 * Requested data count.
	 */
	protected int requestedDataCount;

	/**
	 * @param channel 
	 * @param callback
	 * @param transport
	 * @param sid
	 * @param dataType
	 * @param dataCount
	 * @param value
	 */
	public WriteNotifyRequest(CAJChannel channel, PutListener callback,
							 Transport transport, int sid, int dataType, int dataCount,
							 Object value) {
		super(transport);

		this.channel = channel;
		this.callback = callback; 
		this.requestedDataType = dataType;
		this.requestedDataCount = dataCount;
		// TODO not clean
		context = (CAJContext)transport.getContext(); // or channel.getContext()
		ioid = context.registerResponseRequest(this);
		channel.registerResponseRequest(this);
		
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
										(short)19, alignedPayloadSize, (short)dataType, dataCount,
										sid, ioid);
											
		// append value and align message
		DBREncoder.insertPayload(requestMessage, (short)dataType, dataCount, value);
		requestMessage = alignBuffer(8, requestMessage);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#getIOID()
	 */
	public int getIOID() {
		return ioid;
	}

	/**
	 * @see com.cosylab.epics.caj.impl.NotifyResponseRequest#response(int, short, int, java.nio.ByteBuffer)
	 */
	public void response(
		int status,
		short dataType,
		int dataCount,
		ByteBuffer dataPayloadBuffer) {
			
		try
		{
			if (callback != null)
			{
				DBRType type = DBRType.forValue(dataType);
				CAStatus caStatus = CAStatus.forStatusCode(status);
			
				// notify
				context.getEventDispatcher().dispatch(
				        new PutEvent(channel, type, dataCount, caStatus),
						callback
					);
			}
		}
		finally
		{
			// allways cancel request
			cancel();
		}
		
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#cancel()
	 */
	public void cancel() {
		// unregister response request
		context.unregisterResponseRequest(this);
		channel.unregisterResponseRequest(this);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#timeout()
	 */
	public void timeout() {
		cancel();
		// ... and notify
		if (callback != null)
			context.getEventDispatcher().dispatch(
			        new PutEvent(channel, DBRType.UNKNOWN, 0, CAStatus.TIMEOUT),
					callback
				);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#exception(int, java.lang.String)
	 */
	public void exception(int errorCode, String errorMessage) {
		cancel();

		// TODO no status is dispatched
		if (errorMessage == null)
		{
			CAStatus status = CAStatus.forStatusCode(errorCode);
			if (status != null)
				errorMessage = status.getMessage();
		}
		ContextExceptionEvent cee = new ContextExceptionEvent(context, channel,
				DBRType.forValue(requestedDataType), requestedDataCount, null, errorMessage);
		context.notifyException(cee);

		/*
		// ... and notify
		if (callback != null)
		{
			CAStatus status = CAStatus.forValue(errorCode);
			if (status == null)
			    status = CAStatus.PUTFAIL;

			context.getEventDispatcher().dispatch(
			        new PutEvent(channel, DBRType.UNKNOWN, 0, status),
			        callback
				);
		}
		*/
	}

}
