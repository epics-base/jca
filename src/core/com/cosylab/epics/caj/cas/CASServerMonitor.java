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

package com.cosylab.epics.caj.cas;

import java.util.LinkedList;

import com.cosylab.epics.caj.cas.handlers.EventAddResponse;

import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ServerChannel;
import gov.aps.jca.cas.ServerMonitor;
import gov.aps.jca.dbr.DBR;

/**
 * CAS monitor implementation.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CASServerMonitor extends ServerMonitor implements
		ProcessVariableEventCallback, Runnable {

	/**
	 * Monitor mask.
	 */
	protected short mask;
	
	protected EventAddResponse handler;
	protected CASTransport transport;
	protected short dataType;
	protected int dataCount;


	/**
	 * @param channel
	 * @param ioid
	 */
	public CASServerMonitor(ServerChannel channel, int ioid,
							short mask, EventAddResponse handler, CASTransport transport, short dataType, int dataCount) {
		super(channel, ioid);
		
		this.mask = mask;
		this.handler = handler;
		this.transport = transport;
		this.dataType = dataType;
		this.dataCount = dataCount;
		
		// register to the event dispatcher
		((ProcessVariableEventDispatcher)channel.getProcessVariable().getEventCallback()).registerEventListener(this);
	}

	/**
	 * @see gov.aps.jca.cas.ServerMonitor#destroy()
	 */
	public void destroy() {

		// set destroyed flag and cleat queue (to help GC)
		synchronized (queue) {
			destoryed = true;
			queue.clear();
		}
		
		// unregister from the event dispatcher
		((ProcessVariableEventDispatcher)channel.getProcessVariable().getEventCallback()).unregisterEventListener(this);

		super.destroy();
	}


	/**
	 * @see gov.aps.jca.cas.CompletionCallback#canceled()
	 */
	public void canceled() {
		destroy();
	}

	
	/************************************ DISPATCHING ************************************/
	
	/**
	 * Ordered dispatch queue.
	 */
	protected LinkedList queue = new LinkedList();

	/**
	 * Pending event. 
	 * NOTE: synchronized on <code>queue</code>'s monitor. 
	 */
	protected DBR pendingEvent = null;
	
	/**
	 * Flag indicating pending submit.
	 * NOTE: synchronized on <code>queue</code>'s monitor. 
	 */
	protected boolean submitPending = false;

	/**
	 * Flag indicating destroy status.
	 * NOTE: synchronized on <code>queue</code>'s monitor. 
	 */
	protected boolean destoryed = false;

	/**
	 * Max queue size.
	 * If limit is reached then older events are replaced with the newer.
	 */
	protected static final int MAX_QUEUE_SIZE = 100;

	/**
	 * NOT TO BE CHANGED.
	 * Processing is done one by one - to achieve better fairness.
	 */
	public final void run()
	{
		// get request
		synchronized (queue)
		{
			// if destroyed, bail out
			if (destoryed || queue.isEmpty())
				return;
			
			pendingEvent = (DBR)queue.removeFirst();
		}
		
		// send response
		handler.eventResponse(channel.getProcessVariable(), transport, dataType, dataCount, channel.getSID(), ioid, pendingEvent);

		// this line will always be reached - up code is (has to be) exception safe
		synchronized (queue)
		{
			// reset
			pendingEvent = null;
			submitPending = false;

			if (!queue.isEmpty())
				submit();
		}
		
	}

	/**
	 * Sumbit to be processed. 
	 */
	protected void submit()
	{
		synchronized (queue)
		{
			if (submitPending)
				return;

			// non blocking...
			submitPending = transport.processEvents(this);
			
			// check if request is already processed (in the same thread)
			if (pendingEvent == null)
				submitPending = false;
		}
	}

	/**
	 * @see gov.aps.jca.cas.ProcessVariableEventCallback#postEvent(int, gov.aps.jca.dbr.DBR)
	 */
	public void postEvent(int select, DBR event)
	{
			
		// filter
		if ((select & mask) == 0)
			return;

		synchronized (queue)
		{
			// if destroyed, bail out
			if (destoryed)
				return;

			// replace all previous requests in the queue, if replace policy is set
			if (transport.hasReplaceEventPolicy())
				queue.clear();

			// add to queue	
			queue.addLast(event);

			// discard the oldest one
			if (queue.size() > MAX_QUEUE_SIZE)
				queue.removeFirst();
			
			// initiate submit to dispatcher queue, if necessary
			if (!submitPending)
				submit();
		}
	}

}
