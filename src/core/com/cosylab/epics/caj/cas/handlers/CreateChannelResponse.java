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
import gov.aps.jca.cas.ProcessVariableAttachCallback;
import gov.aps.jca.cas.ServerChannel;
import gov.aps.jca.dbr.DBR_STS_String;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.CASTransport;
import com.cosylab.epics.caj.cas.ProcessVariableEventDispatcher;
import com.cosylab.epics.caj.cas.requests.AccessRightsRequest;
import com.cosylab.epics.caj.cas.requests.CreateChannelFailedRequest;
import com.cosylab.epics.caj.cas.requests.CreateChannelRequest;
import com.cosylab.epics.caj.cas.requests.ExceptionRequest;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.Transport;

/**
 * Create channel request handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CreateChannelResponse extends AbstractCASResponseHandler {

	/**
	 * @param context
	 */
	public CreateChannelResponse(CAJServerContext context) {
		super(context, "Create channel request");
	}

	/**
	 * @see com.cosylab.epics.caj.impl.handlers.AbstractCAJResponseHandler#internalHandleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	protected void internalHandleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response) {

		CASTransport casTransport = (CASTransport)transport;
		
		// The available field is used (abused)
		// here to communicate the miner version number
		// starting with CA 4.1. The field was set to zero prior to 4.1
		short minorVersion = 0;
		if (parameter2 < 0xFFFF)
			minorVersion = (short)parameter2;
		casTransport.setMinorRevision(minorVersion);

		// We shouldnt be receiving a connect message from 
		// an R3.11 client because we will not respond to their
		// search requests (if so we disconnect)
		if (minorVersion < 4)
		{
			sendException(transport, parameter1, CAStatus.DEFUNCT, response[0],
					"R3.11 connect sequence from old client was ignored.");
			disconnect(transport);
			return;
		}

		// zero length name
		if (payloadSize <= 1)
		{
			context.getLogger().warning("Zero length channel name, disconnecting client: " + transport.getRemoteAddress());
			disconnect(transport);
			return;
		}
			
		String channelName = extractString(response[1], 0, payloadSize, false);

		// check channel name lenght
		if (channelName.length() > CAConstants.UNREASONABLE_CHANNEL_NAME_LENGTH)
		{
			context.getLogger().warning("Unreasonable channel name length, disconnecting client: " + transport.getRemoteAddress());
			disconnect(transport);
			return;
		}

		//
		// ask the server to attach this PV
		//
		ProcessVariable pv = null;
		ProcessVariableEventDispatcher pved = new ProcessVariableEventDispatcher(null);
		try
		{
			ProcessVariableAttachCallback pvac = new ProcessVariableAttachCallbackImpl(transport, channelName, parameter1, pved);
			pv = context.getServer().processVariableAttach(channelName, pved, pvac);
		} catch (CAStatusException cse) {
			context.getLogger().log(Level.WARNING, "Exception caught when calling Server.processVariableAttach() for: " + channelName, cse);
			createChannelFailedResponse(transport, channelName, parameter1, cse.getStatus(), cse.getMessage());
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when calling Server.processVariableAttach() for: " + channelName, th);
			createChannelFailedResponse(transport, channelName, parameter1, CAStatus.DEFUNCT, th.getMessage());
		}

		// respond, if async completion not requested
		if (pv != null)
			createChannelResponse(transport, channelName, parameter1, pv, pved);
		
	}
		
	/**
	 * @param cid
	 * @param pv
	 */
	private void createChannelResponse(Transport transport, String channelName, int cid, ProcessVariable pv, ProcessVariableEventDispatcher pved)
	{
		ServerChannel channel = null;
		try
		{
		
			// null check
			if (pv == null)
				throw new CAStatusException(CAStatus.DEFUNCT, "ProcessVariable.processVariableAttach() must return non-null value.");
				
			// get best type (and check if it is pure DBR)
			if (pv.getType().getValue() >= DBR_STS_String.TYPE.getValue())
				throw new CAStatusException(CAStatus.BADTYPE, "ProcessVariable.getType() must return pute DBR type.");
			
			// set serving PV
			pved.setProcessVariable(pv);

			CASTransport casTransport = (CASTransport)transport;

			//
			// create a new channel instance
			//
			int sid = casTransport.preallocateChannelSID();
			try
			{
				channel = pv.createChannel(cid, sid,
						casTransport.getClientUsername(), casTransport.getClientHostname());
				
				// null check
				if (channel == null)
					throw new CAStatusException(CAStatus.DEFUNCT, "null channel returned.");

				// ack allocation and register
				casTransport.registerChannel(sid, channel);
				
			} catch (Throwable th) {
				// depreallocate and rethrow
				casTransport.depreallocateChannelSID(sid);
				throw th;
			}
			
			// ack create of new channel
			new AccessRightsRequest(transport, channel).submit();
			new CreateChannelRequest(transport, channel).submit();
			
		} catch (CAStatusException cse) {
			context.getLogger().log(Level.WARNING, "Exception caught when creating channel: " + channelName, cse);
			createChannelFailedResponse(transport, channelName, cid, cse.getStatus(), cse.getMessage());
			if (channel != null)
				channel.destroy();
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when creating channel: " + channelName, th);
			createChannelFailedResponse(transport, channelName, cid, CAStatus.DEFUNCT, th.getMessage());
			if (channel != null)
				channel.destroy();
		}
	}
	
	/**
	 * @param channelName
	 * @param cid
	 * @param message
	 * @param errorStatus
	 */
	private void createChannelFailedResponse(Transport transport, String channelName, int cid,
											 CAStatus errorStatus, String message)
	{
		try
		{
			// respond with create channel failed message
			if (transport.getMinorRevision() >= 6)
			{
				new CreateChannelFailedRequest(transport, cid).submit();
			}
			// otherwise respond with exception
			else
			{
				// regenerate request header
				com.cosylab.epics.caj.impl.requests.CreateChannelRequest ccr =
					new com.cosylab.epics.caj.impl.requests.CreateChannelRequest(transport, channelName, cid);
				
				ExceptionRequest exceptionRequest = 
					new ExceptionRequest(transport, cid, errorStatus, ccr.generateRequestMessage(), message);
				exceptionRequest.submit();
			}
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Failed to send channel failed to create response for: " + channelName, th);
		}
	}
	
	/**
	 * Disconnect.
	 */
	private void disconnect(Transport transport)
	{
		((CASTransport)transport).close(true);
	}
	
	
	/**
	 * Async. completion callback support.
	 * @author msekoranja
	 */
	class ProcessVariableAttachCallbackImpl implements ProcessVariableAttachCallback
	{
		private Transport transport;
		private String channelName;
		private int cid;
		private ProcessVariableEventDispatcher pved;
		
		/**
		 * @param transport
		 * @param channelName
		 * @param cid
		 * @param pved
		 */
		public ProcessVariableAttachCallbackImpl(Transport transport, String channelName, int cid, ProcessVariableEventDispatcher pved) {
			this.transport = transport;
			this.channelName = channelName;
			this.cid = cid;
			this.pved = pved;
		}
		
		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.ProcessVariableAttachCallback#processVariableAttachCompleted(gov.aps.jca.cas.ProcessVariable)
		 */
		public void processVariableAttachCompleted(ProcessVariable processVariable) {
			createChannelResponse(transport, channelName, cid, processVariable, pved);
		}
		
		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.CompletionCallback#canceled()
		 */
		public void canceled() {
			createChannelFailedResponse(transport, channelName, cid, CAStatus.DEFUNCT, "Async IO canceled.");
		}
	}
	
}