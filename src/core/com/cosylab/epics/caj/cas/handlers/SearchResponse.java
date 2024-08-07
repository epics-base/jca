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

import gov.aps.jca.cas.ProcessVariableExistanceCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCompletion;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.CASTransport;
import com.cosylab.epics.caj.cas.requests.SearchFailedRequest;
import com.cosylab.epics.caj.cas.requests.SearchRequest;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.Transport;

/**
 * Search request handler (UDP only).
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class SearchResponse extends AbstractCASResponseHandler {

	/**
	 * @param context
	 */
	public SearchResponse(CAJServerContext context) {
		super(context, "Search request");
	}

	/**
	 * @see com.cosylab.epics.caj.impl.handlers.AbstractCAJResponseHandler#internalHandleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	protected void internalHandleResponse(
		InetSocketAddress responseFrom,
		Transport transport,
		ByteBuffer[] response) {

		// For a UDP client, BroadcastTransport.processRead will call this with response=[buffer].
		// That one buffer holds version info, search header and search payload, positioned at start of payload:
        //
		// 00 00 00 00  00 01 00 0D  00 00 00 01  00 00 00 00  .... .... .... .... 
		// 00 06 00 08  00 05 00 0D  00 00 00 01  00 00 00 01  .... .... .... .... 
		// 54 45 53 54  00 00 00 00                            TEST .... 
		// ^^
        //
		// For a TCP client, CASTransport.processRead will call this with a response=[headerBuffer, payloadBuffer].
		// headerBuffer is positioned at the end, and a payloadBuffer positioned at the start:
        //
		// 00 06 00 08  00 05 00 0D  00 00 00 01  00 00 00 01  .... .... .... .... 
        //                                                   ^^
		// 54 45 53 54  00 00 00 00                            TEST .... 
        // ^^
		// We need the payload, which holds the zero-padded channel name
		ByteBuffer buffer = response.length == 2 ? response[1] : response[0];

		final int start = buffer.position();
		final int bufferEnd = start + payloadSize;

		// to support multiple messages in one UDP packet
		buffer.position(bufferEnd);

		// check channel name size
		if (payloadSize <= 1) {
			context.getLogger().fine("Empty channel name search request from: " + responseFrom);
			return;
		}

		String channelName = extractString(buffer, start, payloadSize, false);
		
		// empty name check
		if (channelName.length() == 0) {
			context.getLogger().fine("Zero length channel name search request from: " + responseFrom);
			return;
		}

		context.getLogger().fine(responseFrom + " is searching for " + channelName);

		/*
		short minorVersion = (short)dataCount;
		int cid = parameter1; // == parameter2
		 */
		
		//
		// ask the server if this PV exists
		//
		ProcessVariableExistanceCompletion completion = null;
		try
		{
			ProcessVariableExistanceCallback pvec = new ProcessVariableExistanceCallbackImpl(responseFrom, dataType, dataCount, parameter1);
			completion = context.getServer().processVariableExistanceTest(channelName, responseFrom, pvec);
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Exception caught when calling Server.processVariableExistanceTest() for: " + channelName, th);
		}
		
		if (completion == ProcessVariableExistanceCompletion.EXISTS_HERE ||
			completion == ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE ||
			completion.doesExistElsewhere())
		{	// Reply via TCP?
			CASTransport tcp = transport instanceof CASTransport ? (CASTransport) transport : null;
			searchResponse(responseFrom, tcp, dataType, dataCount, parameter1, completion);
		}
		// in case of ProcessVariableExistanceCompletion.ASYNC_COMPLETION callback will call searchResponse method
		// in case of null, do nothing
	}

	/**
	 * Respond to search response.
	 * @param responseFrom
	 * @param tcp Use TCP CASTransport?
	 * @param dataType
	 * @param dataCount
	 * @param cid
	 * @param completion
	 */
	private void searchResponse(InetSocketAddress responseFrom, CASTransport tcp, short dataType, int dataCount, int cid, ProcessVariableExistanceCompletion completion) {

		//
		// ... respond
		//
		if (completion == ProcessVariableExistanceCompletion.EXISTS_HERE)
		{
			// send back
			try
			{
				// TODO prepend version header (if context.getLastReceivedSequenceNumber() != 0)
				// UDP includes payload (version) in reply, TCP has no payload
				if (tcp == null)
				{
					SearchRequest searchRequest = new SearchRequest(context.getBroadcastTransport(), null, true, (short)dataCount, cid);
					context.getLogger().log(Level.FINE, "UDP EXISTS_HERE search reply");
					context.getBroadcastTransport().send(searchRequest, responseFrom);
				}
				else
				{
					SearchRequest searchRequest = new SearchRequest(context.getBroadcastTransport(), null, false, (short)dataCount, cid);
					context.getLogger().log(Level.FINE, "TCP EXISTS_HERE search reply");
				    tcp.send(searchRequest.getRequestMessage());
				}
			} catch (Throwable th) {
				context.getLogger().log(Level.WARNING, "Failed to send back search response to: " + responseFrom, th);
			}
		}
		else if (completion.doesExistElsewhere())
		{
			// Same comments as for EXISTS_HERE
			try
			{
				if (tcp == null)
				{
					SearchRequest searchRequest = new SearchRequest(context.getBroadcastTransport(), completion.getOtherAddress(), true, (short)dataCount, cid);
					context.getLogger().log(Level.FINE, "UDP EXISTS_ELSEWHERE search reply: " + completion.getOtherAddress());
					context.getBroadcastTransport().send(searchRequest, responseFrom);
				}
				else
				{
					SearchRequest searchRequest = new SearchRequest(context.getBroadcastTransport(), completion.getOtherAddress(), false, (short)dataCount, cid);
					context.getLogger().log(Level.FINE, "TCP EXISTS_ELSEWHERE search reply: " + completion.getOtherAddress());
				    tcp.send(searchRequest.getRequestMessage());
				}
			} catch (Throwable th) {
				context.getLogger().log(Level.WARNING, "Failed to send back search response to: " + responseFrom, th);
			}
		}
		else if (completion == ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE)
		{
			// check if not found reply is expected - allowed only when not broadcast
			// TODO C code does never response to this request ?!!!
			boolean notBroadcast = responseFrom.getAddress().isMulticastAddress(); 
			boolean notFoundReplyRequired = notBroadcast && (dataType == CAConstants.CA_SEARCH_DOREPLY);
			if (notFoundReplyRequired)
			{
				// send back
				try
				{
					SearchFailedRequest searchFailedRequest =
							new SearchFailedRequest(context.getBroadcastTransport(), (short)dataCount, cid, cid);
					context.getBroadcastTransport().send(searchFailedRequest, responseFrom);
				} catch (Throwable th) {
					context.getLogger().log(Level.WARNING, "Failed to send back search failed response to: " + responseFrom, th);
				}
			}
		}
		else if (completion == ProcessVariableExistanceCompletion.ASYNC_COMPLETION)
		{
			context.getLogger().log(Level.SEVERE, "Invalid ProcessVariableExistanceCompletion value, should not be ASYNC_COMPLETION!");
		}
		/*
		else
		{
			// we do not know anything, do nothing
			return;
		}
		*/
	}

	/**
	 * Async. completion callback support.
	 * @author msekoranja
	 */
	class ProcessVariableExistanceCallbackImpl implements ProcessVariableExistanceCallback
	{
		private InetSocketAddress responseFrom;
		private short dataType;
		private int dataCount;
		private int cid;
		
		/**
		 * @param responseFrom
		 * @param dataType
		 * @param dataCount
		 * @param cid
		 */
		public ProcessVariableExistanceCallbackImpl(InetSocketAddress responseFrom, 
													short dataType, int dataCount, int cid) {
			this.responseFrom = responseFrom;
			this.dataType = dataType;
			this.dataCount = dataCount;
			this.cid = cid;
		}

		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.ProcessVariableExistanceCallback#processVariableExistanceTestCompleted(gov.aps.jca.cas.ProcessVariableExistanceCompletion)
		 */
		public void processVariableExistanceTestCompleted(ProcessVariableExistanceCompletion completion) {
			searchResponse(responseFrom, null /* not TCP */, dataType, dataCount, cid, completion);
		}

		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.CompletionCallback#canceled()
		 */
		public void canceled() {
			// noop
		} 
	}
	
}
