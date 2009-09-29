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
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.event.ContextExceptionEvent;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.DBRDecoder;
import com.cosylab.epics.caj.impl.NotifyResponseRequest;
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA read notify.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ReadNotifyRequest extends AbstractCARequest implements NotifyResponseRequest {

	/**
	 * Context.
	 */
	protected CAJContext context;

	/**
	 * I/O ID given by the context when registered.
	 */
	protected int ioid;

	/**
	 * Channel server ID.
	 */
	protected int sid;

	/**
	 * Response callback listener.
	 */
	protected GetListener callback;

	/**
	 * DBR to be set (sync. request).
	 */
	protected DBR dbr;

	/**
	 * Sync. request flag.
	 */
	protected boolean sync;

	/**
	 * IO sequence number.
	 */
	protected int sequenceNumberIO;

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
	 * Requested data count.
	 */
	protected boolean prohibitIOCancel = false;

	/**
	 * @param channel 
	 * @param callback
	 * @param dbr
	 * @param transport
	 * @param sid
	 * @param dataType
	 * @param dataCount
	 */
	public ReadNotifyRequest(CAJChannel channel, GetListener callback, DBR dbr,
							 Transport transport, int sid, int dataType, int dataCount) {
		super(transport);

		this.channel = channel;
		this.callback = callback;
		this.dbr = dbr; 
		this.sid = sid;
		this.requestedDataType = dataType;
		this.requestedDataCount = dataCount;
		sync = (dbr != null); 
		// TODO not clean
		context = (CAJContext)transport.getContext(); // or channel.getContext()
		ioid = context.registerResponseRequest(this);
		channel.registerResponseRequest(this);
		
		if (dataCount < 0xFFFF)
		{
		    requestMessage = insertCAHeader(transport, null,
		            						(short)15, (short)0, (short)dataType, (short)dataCount,
		            						sid, ioid);
		}
		else {
			requestMessage = ByteBuffer.allocate(CAConstants.CA_EXTENDED_MESSAGE_HEADER_SIZE);
		    requestMessage = insertCAHeader(transport, requestMessage,
		            						(short)15, (short)0xFFFF, (short)dataType, (short)0,
		            						sid, ioid);
		    requestMessage.putInt(0);
		    requestMessage.putInt(dataCount);
		}
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
			// HexDump.hexDump(description, dataPayloadBuffer.array(), dataPayloadBuffer.limit());
	
			CAStatus caStatus = CAStatus.forStatusCode(status);
			if (caStatus == CAStatus.NORMAL)
				dbr = DBRDecoder.getDBR(dbr, dataType, dataCount, dataPayloadBuffer);
			else
				// do not decrement pending IO, if non-callback read is used
				prohibitIOCancel = true;
			
			// notify
			if (callback != null )
				context.getEventDispatcher().dispatch(
				        new GetEvent(channel, dbr, caStatus),
						callback
					);
		}
		finally
		{
			// allways cancel request
			cancel();
		}
	}

	/**
	 * If sync. request (i.e. w/o callback), additionally increment context pending requests.
	 * @see com.cosylab.epics.caj.impl.Request#submit()
	 */
	public void submit() throws IOException {
		super.submit();
		if (sync)
			sequenceNumberIO = context.incrementPendingRequests();
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#cancel()
	 */
	public void cancel() {
		// unregister response request
		boolean alreadyCanceled = (context.unregisterResponseRequest(this) == null);
		if (!alreadyCanceled && sync && !prohibitIOCancel)
			context.decrementPendingRequests(sequenceNumberIO);
		channel.unregisterResponseRequest(this);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#timeout()
	 */
	public void timeout() {

		// do not decrement pending IO, if non-callback read is used
		prohibitIOCancel = true;

		cancel();
		// ... and notify
		if (callback != null)
			context.getEventDispatcher().dispatch(
			        new GetEvent(channel, null, CAStatus.TIMEOUT),
					callback
				);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#exception(int, java.lang.String)
	 */
	public void exception(int errorCode, String errorMessage) {
		
		// do not decrement pending IO, if non-callback read is used
		prohibitIOCancel = true;

		cancel();

		// TODO no status is dispatched 
		if (errorMessage == null)
		{
			CAStatus status = CAStatus.forStatusCode(errorCode);
			if (status != null)
				errorMessage = status.getMessage();
		}
		ContextExceptionEvent cee = new ContextExceptionEvent(context, channel,
				DBRType.forValue(requestedDataType), requestedDataCount, dbr, errorMessage);
		context.notifyException(cee);

		/*
		// ... and notify
		if (callback != null)
		{
			CAStatus status = CAStatus.forValue(errorCode);
			if (status == null)
			    status = CAStatus.GETFAIL;

			context.getEventDispatcher().dispatch(
			        new GetEvent(channel, null, status),
					callback
				);
		}
		*/
		
	}

}
