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
import gov.aps.jca.event.MonitorEvent;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.CAJMonitor;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.DBRDecoder;
import com.cosylab.epics.caj.impl.NotifyResponseRequest;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;

/**
 * CA event add request (creates a monitor on channel).
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class EventAddRequest extends AbstractCARequest implements NotifyResponseRequest {

	/**
	 * Context.
	 */
	protected final CAJContext context;

	/**
	 * Subscription ID given by the context when registered.
	 */
	protected final int subsid;

	/**
	 * Response callback listener.
	 */
	protected final CAJMonitor monitor;

	/**
	 * Channel.
	 */
	protected final CAJChannel channel;

	/**
	 * Request send priority.
	 */
	protected byte priority = Request.DEFAULT_USER_PRIORITY;
	
	/**
	 * Subscription update flag.
	 */
	private boolean subscriptionUpdateNeeded = false;

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
	 * @param monitor
	 * @param transport
	 * @param sid
	 * @param dataType
	 * @param dataCount
	 * @param mask
	 */
	public EventAddRequest(CAJChannel channel, CAJMonitor monitor,
							 Transport transport, int sid, int dataType, int dataCount, int mask) {
		super(transport);

		this.channel = channel;
		this.monitor = monitor;
		this.requestedDataType = dataType;
		this.requestedDataCount = dataCount;
		// TODO not clean
		context = (CAJContext)channel.getContext(); // transport.getContext();
		// using ioid for subsio, which is OK 
		subsid = context.registerResponseRequest(this);
		channel.registerResponseRequest(this);

		if (dataCount < 0xFFFF)
		{
		    requestMessage = ByteBuffer.allocate(CAConstants.CA_MESSAGE_HEADER_SIZE +  16);
			requestMessage = insertCAHeader(transport, requestMessage,
					(short)1, 16, (short)dataType, dataCount,
					sid, subsid);
		}
		else 
		{
			requestMessage = ByteBuffer.allocate(CAConstants.CA_EXTENDED_MESSAGE_HEADER_SIZE + 16);
			requestMessage = insertCAHeader(transport, requestMessage,
					(short)1, 16, (short)dataType, dataCount,
					sid, subsid);
		}

		// low, high, to - all 0.0
		requestMessage.putFloat((float)0.0);
		requestMessage.putFloat((float)0.0);
		requestMessage.putFloat((float)0.0);
		// mask and alignment
		requestMessage.putShort((short)mask);
		requestMessage.putShort((short)0);
	}

	/**
	 * Update subscription.
	 * NOTE: not sync.
	 */
	public void updateSubscription() throws IOException
	{
		// immediate send
		if (subscriptionUpdateNeeded)
		{
			subscriptionUpdateNeeded = false;
			new SubscriptionUpdateRequest(transport,
					   requestMessage.getInt(8), requestMessage.getInt(12),
					   requestedDataType, requestedDataCount).submit();
		}
	}

	/**
	 * Submit request to the corresponding transport. 
	 * This methods checks for <code>null</code> transport since it is allowed.
	 * @see com.cosylab.epics.caj.impl.requests.AbstractCARequest#submit()
	 */
	public void submit() throws IOException
	{
		if (transport == null)
			return;
		else
			super.submit();
	}

	/**
	 * Resubscribe subscription.
	 * NOTE: not sync.
	 * @param transport	transport to be used.
	 */
	public void resubscribeSubscription(Transport transport) throws IOException
	{
		this.transport = transport;
		// update channel sid
		requestMessage.putInt(8, channel.getServerChannelID());
		// immediate send (increase priority - all subsequent sends will be done immediately).
		priority = Request.SEND_IMMEDIATELY_PRIORITY;
		transport.submit(this);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#getIOID()
	 */
	public int getIOID() {
		return subsid;
	}

	/**
	 * Called everytime on monitor changed event.
	 * @see com.cosylab.epics.caj.impl.NotifyResponseRequest#response(int, short, int, java.nio.ByteBuffer)
	 */
	public void response(
		int status,
		short dataType,
		int dataCount,
		ByteBuffer dataPayloadBuffer) {

		DBR readVal = null;
			
		CAStatus caStatus = CAStatus.forStatusCode(status);
		if (caStatus == CAStatus.NORMAL)     
		{
			subscriptionUpdateNeeded = false;
			if (dataPayloadBuffer.limit() > 0)
				readVal = DBRDecoder.getDBR(readVal, dataType, dataCount, dataPayloadBuffer);
			else
			{
				// TODO is this OK, but is this OK for older/other versions?!
				// this response is completely different on destruction
				// event destroyed
				cancel();
				return;
			}
		}
		else
		{
			cancel();
		}
			
		// notify
		context.getEventDispatcher().dispatch(
		        new MonitorEvent(channel, readVal, caStatus),
				monitor
			);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#cancel()
	 */
	public synchronized void cancel() {
		context.unregisterResponseRequest(this);
		channel.unregisterResponseRequest(this);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#timeout()
	 */
	public void timeout() {
		cancel();
		// ... and notify
		context.getEventDispatcher().dispatch(
		        new MonitorEvent(channel, null, CAStatus.TIMEOUT),
				monitor
			);
	}
	
	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#exception(int, java.lang.String)
	 */
	public void exception(int errorCode, String errorMessage) {
		CAStatus status = CAStatus.forStatusCode(errorCode);
		
		// destroy only on channel destroy
		// NOTE: this does not really destroy subscription on the server - can be done via CAJMonitor.clear()	
		if (status == CAStatus.CHANDESTROY) {
			cancel();
			return;
		} else {
			// noop if monitor is cleared
			if (monitor.isCleared())
			    return;
			subscriptionUpdateNeeded = true;
		}
					
		// do not dispatch if "only" disconnected
		if (status != CAStatus.DISCONN)
		{
			// TODO no status is dispatched 
			if (errorMessage == null && status != null)
				errorMessage = status.getMessage();
			ContextExceptionEvent cee = new ContextExceptionEvent(context, channel,
					DBRType.forValue(requestedDataType), requestedDataCount, null, errorMessage);
			context.notifyException(cee);
		}
		
		/*
		if (status == null)
		    status = CAStatus.ADDFAIL;
		
		context.getEventDispatcher().dispatch(
		        new MonitorEvent(channel, null, status),
				monitor
			);
		*/
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return priority;
	}
	
}
