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

package com.cosylab.epics.caj.cas.handlers;

import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableWriteCallback;
import gov.aps.jca.cas.ServerChannel;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBR_PutAckS;
import gov.aps.jca.dbr.DBR_PutAckT;
import gov.aps.jca.dbr.Severity;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.CASTransport;
import com.cosylab.epics.caj.cas.requests.WriteNotifyRequest;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.DBRDecoder;
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA write request handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class WriteNotifyResponse extends AbstractCASResponseHandler {

	/**
	 * @param context
	 */
	public WriteNotifyResponse(CAJServerContext context) {
		super(context, "Write notify request");
	}

	/**
	 * @see com.cosylab.epics.caj.impl.handlers.AbstractCAJResponseHandler#internalHandleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	protected void internalHandleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response) {

		CASTransport casTransport = (CASTransport)transport;
		
		ServerChannel channel;
		try
		{
			channel = casTransport.getChannelAndVerifyRequest(parameter1, dataType, dataCount);
		} catch (CAStatusException cse) {
			writeNotifyFailureResponse(transport, dataType, dataCount, parameter1, parameter2, cse.getStatus());
			return;
		}
		
		// check write access rights
		if (!channel.writeAccess())
		{
			CAStatus errorStatus = (transport.getMinorRevision() < 1) ? CAStatus.PUTFAIL : CAStatus.NOWTACCESS;
			writeNotifyFailureResponse(transport, dataType, dataCount, parameter1, parameter2, errorStatus);
			return;
		}
		
		// decode value
		DBR value = DBRDecoder.getDBR(null, dataType, dataCount, response[1]);

		// ackS/T support
		if (value instanceof DBR_PutAckT)
		{
			final short tValue = ((DBR_PutAckT)value).getShortValue()[0]; 
			channel.getProcessVariable().setAckT(tValue != 0);
			writeNotifyResponse(transport, dataType, dataCount, parameter1, channel.getCID(), parameter2, CAStatus.NORMAL);
			return;
		}
		else if (value instanceof DBR_PutAckS)
		{
			final short sValue = ((DBR_PutAckS)value).getShortValue()[0];
			final Severity severity = Severity.forValue(sValue);
			// check severity value
			if (severity != null)
			{
				channel.getProcessVariable().setAckS(severity);
				writeNotifyResponse(transport, dataType, dataCount, parameter1, channel.getCID(), parameter2, CAStatus.NORMAL);
			}
			else
				writeNotifyFailureResponse(transport, dataType, dataCount, parameter1, parameter2, CAStatus.PUTFAIL);
			return;
		}

		// convert
		try
		{
			ProcessVariable pv = channel.getProcessVariable();
			value = value.convert(pv.getType(), pv.getEnumLabels());
		} catch (CAStatusException cse) {
			writeNotifyFailureResponse(transport, dataType, dataCount, parameter1, parameter2, cse.getStatus());
			return;
		}

		CAStatus status = null;
		try
		{
			ProcessVariableWriteCallback pvwc = new ProcessVariableWriteCallbackImpl(transport, dataType, dataCount, parameter1, channel.getCID(), parameter2);
			status = channel.write(value, pvwc);
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when calling ServerChannel.write() for: " + channel.getProcessVariable().getName(), th);
			writeNotifyFailureResponse(transport, dataType, dataCount, parameter1, parameter2, CAStatus.DEFUNCT);
			return;
		}
		
		if (status == null)
		{
			// async, noop
			return;
		}
		else if (status != CAStatus.NORMAL)
		{
			writeNotifyFailureResponse(transport, dataType, dataCount, parameter1, parameter2, status);
			return;
		}
		else
			writeNotifyResponse(transport, dataType, dataCount, parameter1, channel.getCID(), 
								parameter2, CAStatus.NORMAL);
		
	}

	/**
	 * @param transport
	 * @param dataType
	 * @param dataCount
	 * @param sid
	 * @param cid
	 * @param ioid
	 */
	private void writeNotifyResponse(Transport transport, short dataType, int dataCount,
											int sid, int cid, int ioid, CAStatus completionStatus)
	{
		
		CAStatus status = CAStatus.NORMAL;
		if (completionStatus != CAStatus.NORMAL)
			status = CAStatus.PUTFAIL;
		
		try
		{
			new WriteNotifyRequest(transport, dataType, dataCount, status, ioid).submit();
			
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when responding with error to write notify request for channel with SID : " + sid, th);
		}
		
		// send independent warning exception to the client
		if (completionStatus != CAStatus.NORMAL)
		{
			// TODO this is not pefect (zero payload, extended header issue), but enough for client!
			
			// regenerate request header
			ByteBuffer previousHeader = ByteBuffer.allocate(CAConstants.CA_MESSAGE_HEADER_SIZE);
			previousHeader.putShort((short)19);
			previousHeader.putShort((short)0); 
			previousHeader.putShort(dataType);
			previousHeader.putShort((short)dataCount);
			previousHeader.putInt(sid);
			previousHeader.putInt(ioid);

			sendException(transport, cid, completionStatus, previousHeader, null);
		}
	}

	/**
	 * @param transport
	 * @param dataType
	 * @param dataCount
	 * @param sid
	 * @param ioid
	 * @param errorStatus
	 */
	private void writeNotifyFailureResponse(Transport transport, short dataType, int dataCount,
											int sid, int ioid, CAStatus errorStatus)
	{
		try
		{
			new WriteNotifyRequest(transport, dataType, dataCount, errorStatus, ioid).submit();
			
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when responding with error to write notify request for channel with SID : " + sid, th);
		}
	}
	
	
	/**
	 * Async. completion callback support.
	 * @author msekoranja
	 */
	class ProcessVariableWriteCallbackImpl implements ProcessVariableWriteCallback
	{
		private Transport transport;
		private short dataType;
		private int dataCount;
		private int sid;
		private int cid;
		private int ioid;
		
		/**
		 * @param transport
		 * @param dataType
		 * @param dataCount
		 * @param sid
		 * @param cid
		 * @param ioid
		 */
		public ProcessVariableWriteCallbackImpl(Transport transport, short dataType, int dataCount, int sid, int cid, int ioid) {
			this.transport = transport;
			this.dataType = dataType;
			this.dataCount = dataCount;
			this.sid = sid;
			this.cid = cid;
			this.ioid = ioid;
		}
		
		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.ProcessVariableWriteCallback#processVariableWriteCompleted(gov.aps.jca.CAStatus)
		 */
		public void processVariableWriteCompleted(CAStatus status) {
			if (status == null)
				status = CAStatus.DEFUNCT;
			writeNotifyResponse(transport, dataType, dataCount, sid, cid, ioid, status);
		}
		
		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.CompletionCallback#canceled()
		 */
		public void canceled() {
			writeNotifyFailureResponse(transport, dataType, dataCount, sid, ioid, CAStatus.PUTFAIL);
		} 		
	}
}
