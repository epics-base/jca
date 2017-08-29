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
import com.cosylab.epics.caj.impl.DBRDecoder;
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA write request handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class WriteResponse extends AbstractCASResponseHandler {

	/**
	 * @param context
	 */
	public WriteResponse(CAJServerContext context) {
		super(context, "Write request");
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
			sendException(transport, cid, cse.getStatus(), response[0], "get request");
			return;
		}	
		
		// check write access rights
		if (!channel.writeAccess())
		{
			CAStatus errorStatus = (transport.getMinorRevision() < 1) ? CAStatus.PUTFAIL : CAStatus.NOWTACCESS;
			sendException(transport, channel.getCID(), errorStatus, response[0], "write access denied");
			return;
		}
		
		// decode value
		DBR value = DBRDecoder.getDBR(null, dataType, dataCount, response[1]);

		// ackS/T support
		if (value instanceof DBR_PutAckT)
		{
			final short tValue = ((DBR_PutAckT)value).getShortValue()[0]; 
			channel.getProcessVariable().setAckT(tValue != 0);
			return;
		}
		else if (value instanceof DBR_PutAckS)
		{
			final short sValue = ((DBR_PutAckS)value).getShortValue()[0];
			final Severity severity = Severity.forValue(sValue);
			// check severity value
			if (severity != null)
				channel.getProcessVariable().setAckS(severity);
			else
				sendException(transport, channel.getCID(), CAStatus.PUTFAIL, response[0], "Invalid severity value");
			return;
		}
		
		// convert
		try
		{
			ProcessVariable pv = channel.getProcessVariable();
			value = value.convert(pv.getType(), pv.getEnumLabels());
		} catch (CAStatusException cse) {
			sendException(transport, channel.getCID(), cse.getStatus(), response[0], cse.getMessage());
			return;
		}
		
		CAStatus status = null;
		try
		{
			ProcessVariableWriteCallback pvwci = new ProcessVariableWriteCallbackImpl(transport, channel.getCID(), response[0]);
			status = channel.write(value, pvwci);
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when calling ServerChannel.write() for: " + channel.getProcessVariable().getName(), th);
			sendException(transport, channel.getCID(), CAStatus.DEFUNCT, response[0], th.getMessage());
			return;
		}
		
		if (status != null && status != CAStatus.NORMAL)
		{
			sendException(transport, channel.getCID(), status, response[0], null);
			return;
		}
		
	}
	
	
	/**
	 * Async. completion callback support.
	 * @author msekoranja
	 */
	class ProcessVariableWriteCallbackImpl implements ProcessVariableWriteCallback
	{
		private Transport transport;
		private int cid;
		private ByteBuffer previousHeader;
		
		/**
		 * @param transport
		 * @param cid
		 * @param previousHeader
		 */
		public ProcessVariableWriteCallbackImpl(Transport transport, int cid, ByteBuffer previousHeader) {
			this.transport = transport;
			this.cid = cid;
			// copy
			this.previousHeader = ByteBuffer.allocate(previousHeader.limit());
			this.previousHeader.get(previousHeader.array(), 0, previousHeader.limit());
			
		}

		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.ProcessVariableWriteCallback#processVariableWriteCompleted(gov.aps.jca.CAStatus)
		 */
		public void processVariableWriteCompleted(CAStatus status) {
			if (status == null)
				sendException(transport, cid, CAStatus.DEFUNCT, previousHeader, "Non-null status expected.");
			else if (status != CAStatus.NORMAL)
				sendException(transport, cid, status, previousHeader, status.getMessage());
		}
		
		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.CompletionCallback#canceled()
		 */
		public void canceled() {
			sendException(transport, cid, CAStatus.DEFUNCT, previousHeader, "Async IO canceled.");
		}

	}
}
