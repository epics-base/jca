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
import gov.aps.jca.Monitor;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableReadCallback;
import gov.aps.jca.cas.ServerChannel;
import gov.aps.jca.dbr.ACK;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.CASServerMonitor;
import com.cosylab.epics.caj.cas.CASTransport;
import com.cosylab.epics.caj.cas.requests.EventAddRequest;
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA event add request handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class EventAddResponse extends AbstractCASResponseHandler {

	/**
	 * @param context
	 */
	public EventAddResponse(CAJServerContext context) {
		super(context, "Event add request");
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
			channel = casTransport.getChannel(parameter1);
			int cid = (channel == null) ? 0 : channel.getCID();
			sendException(transport, cid, cse.getStatus(), response[0], "add event request");
			return;
		}

		// check monitor mask
		final int MASK_OFFSET = 12;
		short mask = response[1].getShort(MASK_OFFSET);
		if ((mask & (Monitor.ALARM | Monitor.LOG | Monitor.VALUE | Monitor.PROPERTY)) == 0)
		{
			sendException(transport, channel.getCID(), CAStatus.BADMASK, response[0], 
					"event add req with mask=" + Integer.toHexString(mask));
			return;
		}

		//
		// Attempt to read the first monitored value prior to creating
		// the monitor object so that if the server tool chooses
		// to postpone asynchronous IO we can safely restart this
		// request later.
		//

		// create DBR
		DBR value = createDBRforReading(channel.getProcessVariable(), dataType, dataCount);

		// check read rights
		if (!channel.readAccess())
		{
			eventAddFailureResponse(transport, dataType, dataCount, parameter1, parameter2, CAStatus.NORDACCESS, value);
			return;
		}

		CAStatus status = null;
		try
		{
			ProcessVariableReadCallback pvrc = new ProcessVariableReadCallbackImpl(channel, transport, dataType, dataCount, parameter1, parameter2, mask, value);
			status = channel.read(value, pvrc);
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when calling ServerChannel.read() for: " + channel.getProcessVariable().getName(), th);
			eventAddFailureResponse(transport, dataType, dataCount, parameter1, parameter2, CAStatus.DEFUNCT, value);
			return;
		}
		
		if (status == null)
		{
			// async, noop
			return;
		}
		else
			eventAddResponse(channel, transport, dataType, dataCount, parameter1, parameter2, mask, value, status);
	}

	/**
	 * @param channel
	 * @param transport
	 * @param dataType
	 * @param dataCount
	 * @param sid
	 * @param ioid
	 * @param mask
	 * @param value
	 * @param status
	 */
	private void eventAddResponse(ServerChannel channel, Transport transport, short dataType, int dataCount,
								  int sid, int ioid, short mask, DBR value, CAStatus status)
	{
		if (status != CAStatus.NORMAL)
		{
			eventAddFailureResponse(transport, dataType, dataCount, sid, ioid, status, value);
			return;
		}

		eventResponse(channel.getProcessVariable(), transport, dataType, dataCount, sid, ioid, value);

		// create monitor
		new CASServerMonitor(channel, ioid, mask, this, (CASTransport)transport, dataType, dataCount);
	}

	/**
	 * @param transport
	 * @param dataType
	 * @param dataCount
	 * @param sid
	 * @param ioid
	 * @param value
	 */
	public void eventResponse(ProcessVariable processVariable, Transport transport, short dataType, int dataCount, int sid, int ioid, DBR value) {

		// convert to required type
		try
		{
			value = value.convert(DBRType.forValue(dataType));
		} catch (CAStatusException cse) {
			eventAddFailureResponse(transport, dataType, dataCount, sid, ioid, cse.getStatus(), value);
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
			new EventAddRequest(transport, ioid, dataType, dataCount, CAStatus.NORMAL, value).submit();
			
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when responding to event add request for channel with SID : " + sid, th);
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
	private void eventAddFailureResponse(Transport transport, short dataType, int dataCount,
											int sid, int ioid, CAStatus errorStatus, DBR value)
	{
		try
		{
			new EventAddRequest(transport, ioid, dataType, dataCount, errorStatus, value).submit();
			
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when responding with error to event add request for channel with SID : " + sid, th);
		}
	}
	
	
	/**
	 * Async. completion callback support.
	 * @author msekoranja
	 */
	class ProcessVariableReadCallbackImpl implements ProcessVariableReadCallback
	{
		private ServerChannel channel;
		private Transport transport;
		private short dataType;
		private int dataCount;
		private int sid;
		private int ioid;
		private short mask;
		private DBR value;
		
		/**
		 * @param channel
		 * @param transport
		 * @param dataType
		 * @param dataCount
		 * @param sid
		 * @param ioid
		 * @param mask
		 * @param value
		 */
		public ProcessVariableReadCallbackImpl(ServerChannel channel, Transport transport, short dataType, int dataCount, int sid, int ioid, short mask, DBR value) {
			this.channel = channel;
			this.transport = transport;
			this.dataType = dataType;
			this.dataCount = dataCount;
			this.sid = sid;
			this.ioid = ioid;
			this.mask = mask;
			this.value = value;
		}
		
		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.ProcessVariableReadCallback#processVariableReadCompleted(gov.aps.jca.CAStatus)
		 */
		public void processVariableReadCompleted(CAStatus status) {
			if (status == null)
				status = CAStatus.DEFUNCT;
			
			eventAddResponse(channel, transport, dataType, dataCount, sid, ioid, mask, value, status);
		}
		
		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.CompletionCallback#canceled()
		 */
		public void canceled() {
			eventAddFailureResponse(transport, dataType, dataCount, sid, ioid, CAStatus.DEFUNCT, value);
		}	
		
	}
}
