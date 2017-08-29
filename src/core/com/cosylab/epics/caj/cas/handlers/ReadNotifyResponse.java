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
import gov.aps.jca.cas.ProcessVariableReadCallback;
import gov.aps.jca.cas.ServerChannel;
import gov.aps.jca.dbr.ACK;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRFactory;
import gov.aps.jca.dbr.DBRType;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.CASTransport;
import com.cosylab.epics.caj.cas.requests.ReadNotifyRequest;
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA read notify request handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ReadNotifyResponse extends AbstractCASResponseHandler {

	/**
	 * @param context
	 */
	public ReadNotifyResponse(CAJServerContext context) {
		super(context, "Read notify request");
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
			readNotifyFailureResponse(transport, dataType, dataCount, parameter1, parameter2, cse.getStatus());
			return;
		}
		
		// check read access rights
		if (!channel.readAccess())
		{
			readNotifyFailureResponse(transport, dataType, dataCount, parameter1, parameter2, CAStatus.NORDACCESS);
			return;
		}

		// create DBR
		final ProcessVariable processVariable = channel.getProcessVariable();
		DBR value = createDBRforReading(processVariable, dataType, dataCount);
		
		CAStatus status = null;
		try
		{
			ProcessVariableReadCallback pvrc = new ProcessVariableReadCallbackImpl(processVariable, transport, dataType, dataCount, parameter1, parameter2, value);
			status = channel.read(value, pvrc);
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when calling ServerChannel.read() for: " + channel.getProcessVariable().getName(), th);
			readNotifyFailureResponse(transport, dataType, dataCount, parameter1, parameter2, CAStatus.DEFUNCT);
			return;
		}
		
		if (status == null)
		{
			// async, noop
			return;
		}
		else
			readNotifyResponse(processVariable, transport, dataType, dataCount, parameter1, parameter2, value, status);
	}

	/**
	 * @param transport
	 * @param ioid
	 * @param value
	 */
	private void readNotifyResponse(ProcessVariable processVariable, Transport transport, short dataType, int dataCount, int sid, int ioid, DBR value, CAStatus status)
	{
		// check status
		if (status != CAStatus.NORMAL)
		{
			readNotifyFailureResponse(transport, dataType, dataCount, sid, ioid, status);
			return;
		}
		
		// convert to required type
		try
		{
			value = value.convert(DBRType.forValue(dataType));
		} catch (CAStatusException cse) {
			readNotifyFailureResponse(transport, dataType, dataCount, sid, ioid, cse.getStatus());
			return;
		}
		
		// ackS/T support
		if (value instanceof ACK)
		{
			ACK ack = (ACK)value;
			ack.setAckS(processVariable.getAckS());
			ack.setAckT(processVariable.isAckT());
		}

		try
		{
			new ReadNotifyRequest(transport, CAStatus.NORMAL.getStatusCode(), ioid,
								  dataType, dataCount, value ).submit();
			
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when responding to read notify request for channel with SID : " + sid, th);
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
	private void readNotifyFailureResponse(Transport transport, short dataType, int dataCount,
											int sid, int ioid, CAStatus errorStatus)
	{
		try
		{
			// create zero value 
			DBR value = DBRFactory.create(dataType, dataCount);
			new ReadNotifyRequest(transport, errorStatus.getStatusCode(), ioid,
							dataType, dataCount, value ).submit();
			
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when responding with error to read notify request for channel with SID : " + sid, th);
		}
	}
	
	/**
	 * Async. completion callback support.
	 * @author msekoranja
	 */
	class ProcessVariableReadCallbackImpl implements ProcessVariableReadCallback
	{
		private ProcessVariable processVariable;
		private Transport transport;
		private short dataType;
		private int dataCount;
		private int sid;
		private int ioid;
		private DBR value;
		
		/**
		 * @param transport
		 * @param dataType
		 * @param dataCount
		 * @param sid
		 * @param ioid
		 * @param value
		 */
		public ProcessVariableReadCallbackImpl(ProcessVariable processVariable, Transport transport, short dataType, int dataCount, int sid, int ioid, DBR value) {
			this.processVariable = processVariable;
			this.transport = transport;
			this.dataType = dataType;
			this.dataCount = dataCount;
			this.sid = sid;
			this.ioid = ioid;
			this.value = value;
		}

		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.ProcessVariableReadCallback#processVariableReadCompleted(gov.aps.jca.CAStatus)
		 */
		public void processVariableReadCompleted(CAStatus status) {
			if (status == null)
				status = CAStatus.DEFUNCT;

			readNotifyResponse(processVariable, transport, dataType, dataCount, sid, ioid, value, status);
		}

		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.CompletionCallback#canceled()
		 */
		public void canceled() {
			readNotifyFailureResponse(transport, dataType, dataCount, sid, ioid, CAStatus.GETFAIL);
		}
		
		
	}
	
}
