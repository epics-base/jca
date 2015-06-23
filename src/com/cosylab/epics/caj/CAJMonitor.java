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

package com.cosylab.epics.caj;

import java.io.IOException;
import java.util.ArrayList;

import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.requests.EventAddRequest;
import com.cosylab.epics.caj.impl.requests.EventCancelRequest;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

/**
 * Implementation of CAJ JCA <code>Monitor</code>. 
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJMonitor extends Monitor implements MonitorListener {

	/**
	 * Context.
	 */
	protected CAJContext context;

	/**
	 * Monitored value type.
	 */
	protected DBRType type;

	/**
	 * Number of elements (size of array) of monitored value type.
	 */
	protected int count;
	
	/**
	 * Monitored channel.
	 */
	protected CAJChannel channel;
	
	/**
	 * Event add request.
	 */
	protected EventAddRequest eventAddRequest = null;
	
	/**
	 * Monitor mask.
	 */
	protected int mask;

	/**
	 * Monitor listeners.
	 */
	protected ArrayList listeners = new ArrayList();
	
	/**
	 * Subscription ID.
	 */
	protected int subsid;

	/**
	 * Cleared flag.
	 */
	protected volatile boolean cleared = false;
	
	/**
	 * Constructor.
	 * @param context
	 * @param type
	 * @param count
	 * @param channel
	 * @param listener
	 * @param mask
	 * @throws CAException
	 */
	public CAJMonitor(CAJContext context, DBRType type, int count,
					  CAJChannel channel, MonitorListener listener, int mask) throws CAException
	{
		this.context = context;
		this.type = type;
		this.count = count;
		this.channel = channel;
		this.mask = mask;

		// register listener if non-null
		if (listener != null)
			addMonitorListener(listener);

		internalConnect();
		
		// register to the channel
		channel.registerMonitor(this);		
	}

	/**
	 * Subscribe monitor.
	 */
	private void internalConnect() throws CAException {
		
		try {
			eventAddRequest = new EventAddRequest(channel, this, channel.getTransport(), channel.getServerChannelID(), type.getValue(), count, mask);
			eventAddRequest.submit();
			subsid = eventAddRequest.getIOID();
		} catch (IOException ioex) {
			throw new CAException("Failed to create monitor.", ioex);
		}

	}

	/**
	 * @see gov.aps.jca.Monitor#clear()
	 */
	public synchronized void clear() throws CAException {
	    
	    if (cleared)
	        return;
	    cleared = true;
		
		Transport t = channel.getTransport();
		if (t != null)
		{
			try {
				new EventCancelRequest(t, channel.getServerChannelID(), subsid, type.getValue(), count).submit();
			} catch (IOException ioex) {
				// OK if transport/channel is closed
				// throw new CAException("Failed to clear monitor.", ioex);
			}
		}
		
	    // Cancel the EventAddRequest, unregistering it.
		// This prevents resubscriptions.
	    eventAddRequest.cancel();

	    // unregister from the channel
		channel.unregisterMonitor(this);		
	}

	/**
	 * @see gov.aps.jca.Monitor#getContext()
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * @see gov.aps.jca.Monitor#getChannel()
	 */
	public Channel getChannel() {
		return channel;
	}

	/**
	 * @see gov.aps.jca.Monitor#getType()
	 */
	public DBRType getType() {
		return type;
	}

	/**
	 * @see gov.aps.jca.Monitor#getCount()
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @see gov.aps.jca.Monitor#getMask()
	 */
	public int getMask() {
		return mask;
	}

	/**
	 * @see gov.aps.jca.Monitor#getMonitorListener()
	 */
	public MonitorListener getMonitorListener() {
		synchronized (listeners)
		{
			if (listeners.size() == 0)
				return null;
			else
				return (MonitorListener)listeners.get(0);
		}
	}

	/**
	 * @see gov.aps.jca.Monitor#addMonitorListener(gov.aps.jca.event.MonitorListener)
	 */
	public void addMonitorListener(MonitorListener l) {
		
		if (l == null)
			throw new IllegalArgumentException("l == null");
		
		synchronized (listeners)
		{
			if (!listeners.contains(l))
				listeners.add(l);
		}
	}

	/**
	 * @see gov.aps.jca.Monitor#removeMonitorListener(gov.aps.jca.event.MonitorListener)
	 */
	public void removeMonitorListener(MonitorListener l) {

		if (l == null)
			throw new IllegalArgumentException("l == null");

		synchronized (listeners)
		{
			listeners.remove(l);
		}
	}

	/**
	 * @see gov.aps.jca.Monitor#getMonitorListeners()
	 */
	public MonitorListener[] getMonitorListeners() {
		synchronized (listeners)
		{
			MonitorListener[] l = new MonitorListener[listeners.size()];
			return (MonitorListener[])listeners.toArray(l);
		}
	}

	/**
	 * @see gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.MonitorEvent)
	 */
	public void monitorChanged(MonitorEvent ev) {
		context.getEventDispatcher().dispatch(ev, listeners);
	}

	/**
	 * Get this monitor subscription ID.
	 * @return monitor subscription ID
	 */
	public int getSID() {
		return subsid;
	}

	/**
	 * Get cleared flag.
	 * @return cleared flag.
	 */
    public boolean isCleared() {
        return cleared;
    }
}
