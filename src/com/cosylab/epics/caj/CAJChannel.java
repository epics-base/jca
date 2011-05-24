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

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRFactory;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.event.AccessRightsEvent;
import gov.aps.jca.event.AccessRightsListener;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.MonitorListener;
import gov.aps.jca.event.PutListener;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.CATransport;
import com.cosylab.epics.caj.impl.ResponseRequest;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.TransportClient;
import com.cosylab.epics.caj.impl.requests.ClearChannelRequest;
import com.cosylab.epics.caj.impl.requests.CreateChannelRequest;
import com.cosylab.epics.caj.impl.requests.EventAddRequest;
import com.cosylab.epics.caj.impl.requests.ReadNotifyRequest;
import com.cosylab.epics.caj.impl.requests.SearchRequest;
import com.cosylab.epics.caj.impl.requests.WriteNotifyRequest;
import com.cosylab.epics.caj.impl.requests.WriteRequest;
import com.cosylab.epics.caj.util.ArrayFIFO;

/**
 * Implementation of CAJ JCA <code>Channel</code>.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJChannel extends Channel implements TransportClient {
	
	// Get Logger
	private static final Logger logger = Logger.getLogger(CAJChannel.class.getName());
	
	/**
	 * Client channel ID.
	 */
	protected int channelID;

	/**
	 * Context.
	 */
	protected CAJContext context;

	/**
	 * Channel name.
	 */
	protected String name;

	/**
	 * Channel access rights.
	 */
	protected int accessRights = 0;

	/**
	 * Process priority.
	 */
	protected short priority;

	/**
	 * Last reported connection status.
	 */
	protected boolean lastReportedConnectionState = false;

	/**
	 * Connection status.
	 */
	protected ConnectionState connectionState = ConnectionState.NEVER_CONNECTED;

	/**
	 * Channel data type.
	 */
	protected DBRType type = DBRType.UNKNOWN;

	/**
	 * Data type element count.
	 */
	protected int elementCount = 0;
	
	/**
	 * List of connection listeners.
	 */
	// TODO consider using weak references
	// TODO use them
	protected ArrayList connectionListeners = new ArrayList();

	/**
	 * List of access rights listeners.
	 */
	// TODO consider using weak references
	// TODO use them
	protected ArrayList accessRightsListeners = new ArrayList();

	/**
	 * Sync. connection (i.e. w/ callback) flag.
	 */
	protected boolean syncConnection = false;

	/**
	 * IO sequence number.
	 */
	protected int sequenceNumberIO;

	/**
	 * Map of channels (keys are CIDs).
	 * Lazy initialization used, since not all channel will have monitors.
	 */
	// TODO consider using WeakHashMap (and call Channel.destroy() in finalize() method).
	protected Map monitors = null;

	/**
	 * List of all channels pending requests. 
	 */
	protected Map responseRequests = new HashMap();
	
	/**
	 * Number of channel search tries. 
	 */
	protected int searchTries = 0;

	/**
	 * Allow reconnection flag. 
	 */
	protected boolean allowCreation = true;

	/**
	 * Reference counting. 
	 */
	protected int references = 1;

	/* ****************** */
	/* CA protocol fields */ 
	/* ****************** */

	/**
	 * Server transport.
	 */
	protected CATransport transport = null;

	/**
	 * Server channel ID.
	 */
	protected int serverChannelID = 0xFFFFFFFF;

	/* ****************** */
	 
	/**
	 * Constructor.
	 * @param context
	 * @param name
	 * @param listener
	 * @throws CAException
	 */
	protected CAJChannel(CAJContext context, int channelID, String name,
			ConnectionListener listener, short priority) throws CAException
	{
		this.context = context;
		this.channelID = channelID;
		this.name = name;
		this.priority = priority;

		// register before issuing search request
		context.registerChannel(this);

		// add listener
		if (listener != null)
			addConnectionListener(listener);
		else
		{
			// add pending request, if sync (i.e. w/o callback)
			syncConnection = true;
			sequenceNumberIO = context.incrementPendingRequests();
		}

		// this has to be submitted immediately
		initiateSearch();
	}

	/**
	 * Create a channel, i.e. submit create channel request to the server.
	 * This method is called after seatch is complete.
	 * <code>sid</code>, <code>typeCode</code>, <code>elementCount</code> might not be
	 * valid, this depends on protocol revision.
	 * @param transport
	 * @param sid
	 * @param typeCode
	 * @param elementCount
	 */
	public synchronized void createChannel(CATransport transport, int sid, short typeCode, int elementCount) 
	{

		// do not allow duplicate creation to the same transport
		if (!allowCreation)
			return;
		allowCreation = false;
		
		// TODO is this really necesarry... 1. priority is to take channel from the existing one,
		// so transport must be the same... or until disconnected, i.e. transport is null
		// remote address check was already done in context... here reference check is done
		// check existing transport
		if (this.transport != null && this.transport != transport)
		{
			disconnectPendingIO(false);
			this.transport.release(this);
		}
		else if (this.transport == transport)
		{
			// request to sent create request to same transport, ignore
			// this happens when server is slower (processing search requests) than client generating it
			return;
		}
		
		this.transport = transport;
		
		// revision < v4.4 supply this info already now
		if (transport.getMinorRevision() < 4)
		{
			this.serverChannelID = sid;
			this.type = DBRType.forValue(typeCode);
			if (this.type == null)
				this.type = DBRType.UNKNOWN;
			this.elementCount = elementCount;
		}

		try
		{
			// submit (immediately)
			new CreateChannelRequest(transport, name, channelID).submit();
		}
		catch (IOException ioex)
		{
			createChannelFailed();
		}
	}
	

	/**
	 * Cancelation status.
	 */
	protected volatile boolean canceled = false;

	/**
	 * Called when connecton completed (successfully or not). 
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#cancel()
	 */
	public synchronized void cancel() {
		if (canceled)
			return;
		canceled = true;
		if (syncConnection)
		{
			syncConnection = false;
			context.decrementPendingRequests(sequenceNumberIO);
		}
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ResponseRequest#timeout()
	 */
	public void timeout() {
		createChannelFailed();
	}

	/**
	 * Create channel failed.
	 */
	public synchronized void createChannelFailed()
	{
		cancel();
		// ... and search again
		initiateSearch();
	}

	/**
	 * Called when channel crated succeeded on the server.
	 * <code>sid</code> might not be valid, this depends on protocol revision.
	 * @param sid
	 * @param typeCode
	 * @param elementCount
	 * @throws IllegalStateException
	 */
	public synchronized void connectionCompleted(int sid, short typeCode, int elementCount) 
		throws IllegalStateException
	{
		try
		{
			// do this silently
			//checkNotClosedState();
			if (connectionState == ConnectionState.CLOSED)
				return;
			// TODO revise this reconnection...
			//else if (connectionState == ConnectionState.CONNECTED)
			//	throw new IllegalStateException("Channel already connected.");
			
			// revision < v4.1 do not have access rights, grant all
			if (transport.getMinorRevision() < 1)
				setAccessRights(CAConstants.CA_PROTO_ACCESS_RIGHT_READ |
								CAConstants.CA_PROTO_ACCESS_RIGHT_WRITE);
		
			// revision < v4.4 supply this info already now
			if (transport.getMinorRevision() >= 4)
				serverChannelID = sid;
		
			this.type = DBRType.forValue(typeCode);
			if (this.type == null)
				this.type = DBRType.UNKNOWN;
			this.elementCount = elementCount;

			// user might create monitors in listeners, so this has to be done before this can happen
			// however, it would not be nice if events would come before connection event is fired
			// but this cannot happen since transport (TCP) is serving in this thread 
			resubscribeSubscriptions();
			setConnectionState(ConnectionState.CONNECTED);
		}
		finally
		{
			// end connection request
			cancel();
		}
	}

	/**
	 * @see gov.aps.jca.Channel#destroy()
	 */
	public synchronized void destroy() throws CAException, IllegalStateException {
		destroy(false);
	}

	/**
	 * @param force force destruction regardless of reference count
	 * @see gov.aps.jca.Channel#destroy()
	 */
	public synchronized void destroy(boolean force) throws CAException, IllegalStateException {
		
		if (connectionState == ConnectionState.CLOSED)
			throw new IllegalStateException("Channel already destroyed.");
			
		// do destruction via context
		context.destroyChannel(this, force);
		
	}

	/**
	 * Increment reference.
	 */
	public synchronized void acquire() {
		references++;
	}

	/**
	 * Actual destory method, to be called <code>CAJContext</code>.
	 * @param force force destruction regardless of reference count
	 * @throws CAException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public synchronized void destroyChannel(boolean force) throws CAException, IllegalStateException, IOException {

		if (connectionState == ConnectionState.CLOSED)
			throw new IllegalStateException("Channel already destroyed.");

		references--;
		if (references > 0 && !force)
			return;
		
		// stop searching...
		context.getChannelSearchManager().unregisterChannel(this);
		cancel();

		destroyAllMonitors();
		
		disconnectPendingIO(true);

		if (connectionState == ConnectionState.CONNECTED)
		{
			if (transport != null)
			{
				try
				{
					new ClearChannelRequest(transport, channelID, serverChannelID).submit();
				}
				catch (IOException ioex)
				{
					// TODO remove?
					logger.log(Level.SEVERE, "", ioex);
				}
			}
			
			// TODO transport is closed immedately,
			// so it might happen that ClearChannelRequest is not sent/response received
			disconnect(false);
		}
		else if (transport != null)
		{
			// unresponsive state, do not forget to release transport
			transport.release(this);
			transport = null;
		}

		setConnectionState(ConnectionState.CLOSED);
		
		// unregister
		context.unregisterChannel(this);

		synchronized (accessRightsListeners)
		{
			accessRightsListeners.clear();
		}
				
		/*
		// this makes problem to the queued dispatchers...
		synchronized (connectionListeners)
		{
			connectionListeners.clear();
		}
		*/
	}

	/**
	 * Disconnected notification.
	 * @param initiateSearch	flag to indicate if searching (connect) procedure should be initiated
	 */
	public synchronized void disconnect(boolean initiateSearch) {
//System.err.println("CHANNEL disconnect");
		
		if (connectionState != ConnectionState.CONNECTED && transport == null)
			return;
			
		setConnectionState(ConnectionState.DISCONNECTED);

		disconnectPendingIO(false);

		// release transport
		if (transport != null)
		{
			transport.release(this);
			transport = null;
		}
		
		if (initiateSearch)
			initiateSearch();

	}
	
	/**
	 * Initiate search (connect) procedure.
	 */
	public synchronized void initiateSearch()
	{
		allowCreation = true;
		context.getChannelSearchManager().registerChannel(this);
	}

	/**
	 * Send search message.
	 * @return success status.  
	 */
	public synchronized boolean generateSearchRequestMessage(Transport transport, ByteBuffer buffer)
	{
		ByteBuffer result = SearchRequest.generateSearchRequestMessage(transport, buffer, name, channelID);
		if (result == null)
			return false;
		
		if (searchTries < Integer.MAX_VALUE)
			searchTries++;
		
		return true;
	}
	 
	/**
	 * @see com.cosylab.epics.caj.impl.TransportClient#transportClosed()
	 */
	public void transportClosed() {
//System.err.println("CHANNEL transportClosed");
		disconnect(true);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.TransportClient#transportChanged()
	 */
	public void transportChanged() {
//System.err.println("CHANNEL transportChanged");
		initiateSearch();
	}

	/**
	 * @see com.cosylab.epics.caj.impl.TransportClient#transportResponsive(com.cosylab.epics.caj.impl.Transport)
	 */
	public synchronized void transportResponsive(Transport transport) {
//System.err.println("CHANNEL transportResponsive");
		if (connectionState == ConnectionState.DISCONNECTED)
		{
			/*
			// responsive again, stop searching
			context.getChannelSearchManager().unregisterChannel(this);
			 */
			 
			updateSubscriptions();
			
			// reconnect using existing IDs, data
			connectionCompleted(serverChannelID, (short)type.getValue(), elementCount);
		}
	}

	/**
	 * @see com.cosylab.epics.caj.impl.TransportClient#transportUnresponsive()
	 */
	public synchronized void transportUnresponsive() {
//System.err.println("CHANNEL transportUnresponsive");
		if (connectionState == ConnectionState.CONNECTED)
		{
			
			// NOTE: 2 types of disconnected state - distinguish them
			setConnectionState(ConnectionState.DISCONNECTED);

			// ... CA notifies also w/ no access rights callback, although access right are not changed 
			
			
			//transportClosed();
		}
	}

	/**
	 * @see gov.aps.jca.Channel#getConnectionListeners()
	 */
	public ConnectionListener[] getConnectionListeners()
		throws IllegalStateException {
		synchronized (connectionListeners)
		{
			ConnectionListener[] listeners = new ConnectionListener[connectionListeners.size()];
			return (ConnectionListener[])connectionListeners.toArray(listeners);
		}
	}

	/**
	 * @see gov.aps.jca.Channel#addConnectionListener(gov.aps.jca.event.ConnectionListener)
	 */
	public void addConnectionListener(ConnectionListener l)
		throws CAException, IllegalStateException {
		checkNotClosedState();

		if (l == null)
			throw new IllegalArgumentException("l == null");
			
		synchronized (connectionListeners)
		{
			if (!connectionListeners.contains(l))
				connectionListeners.add(l);
		}
	}

	/**
	 * @see gov.aps.jca.Channel#addConnectionListener(gov.aps.jca.event.ConnectionListener)
	 */
	public synchronized void addConnectionListenerAndFireIfConnected(ConnectionListener l)
		throws CAException, IllegalStateException {
		checkNotClosedState();

		if (l == null)
			throw new IllegalArgumentException("l == null");
		
		if (connectionState == ConnectionState.CONNECTED)
			context.getEventDispatcher().dispatch(new ConnectionEvent(this, true), l);

		addConnectionListener(l);
	}

	/**
	 * @see gov.aps.jca.Channel#removeConnectionListener(gov.aps.jca.event.ConnectionListener)
	 */
	public void removeConnectionListener(ConnectionListener l)
		throws CAException, IllegalStateException {
		checkNotClosedState();

		if (l == null)
			throw new IllegalArgumentException("l == null");
		
		synchronized (connectionListeners)
		{
			connectionListeners.remove(l);
		}
	}

	/**
	 * Set connection state and if changed, notifies listeners.
	 * @param newState	state to set.
	 */
	private synchronized void setConnectionState(ConnectionState connectionState)
	{
		if (this.connectionState != connectionState)
		{
			this.connectionState = connectionState;
			
			boolean connectionStatusToReport = (connectionState == ConnectionState.CONNECTED);
			if (connectionStatusToReport != lastReportedConnectionState)
			{
				lastReportedConnectionState = connectionStatusToReport;
				synchronized (connectionListeners)
				{
					context.getEventDispatcher().dispatch(
						new ConnectionEvent(this, connectionStatusToReport),
						connectionListeners
					);
				}
			}
		}
	}
	
	/**
	 * @see gov.aps.jca.Channel#getAccessRightsListeners()
	 */
	public AccessRightsListener[] getAccessRightsListeners()
		throws IllegalStateException {
		synchronized (accessRightsListeners)
		{
			AccessRightsListener[] listeners = new AccessRightsListener[accessRightsListeners.size()];
			return (AccessRightsListener[])accessRightsListeners.toArray(listeners);
		}
	}

	/**
	 * @see gov.aps.jca.Channel#addAccessRightsListener(gov.aps.jca.event.AccessRightsListener)
	 */
	public void addAccessRightsListener(AccessRightsListener l)
		throws CAException, IllegalStateException {
		checkNotClosedState();

		if (l == null)
			throw new IllegalArgumentException("l == null");
			
		synchronized (accessRightsListeners)
		{
			if (!accessRightsListeners.contains(l))
				accessRightsListeners.add(l);
		}
	}

	/**
	 * @see gov.aps.jca.Channel#removeAccessRightsListener(gov.aps.jca.event.AccessRightsListener)
	 */
	public void removeAccessRightsListener(AccessRightsListener l)
		throws CAException, IllegalStateException {
		checkNotClosedState();

		if (l == null)
			throw new IllegalArgumentException("l == null");
	
		synchronized (accessRightsListeners)
		{
			accessRightsListeners.remove(l);
		}
	}

	/**
	 * Set access rights.
	 * @param accessRights	access rights to set
	 */
	public synchronized void setAccessRights(int accessRights) 
	{
		if (this.accessRights != accessRights)
		{
			this.accessRights = accessRights;

			// do not use access rights accessors since they check state
			boolean readAccess  = (accessRights & CAConstants.CA_PROTO_ACCESS_RIGHT_READ) != 0; 
			boolean writeAccess = (accessRights & CAConstants.CA_PROTO_ACCESS_RIGHT_WRITE) != 0; 

			synchronized (accessRightsListeners)
			{
				context.getEventDispatcher().dispatch(
					new AccessRightsEvent(this, readAccess, writeAccess),
					accessRightsListeners
				);
			}
		}
	}

	/**
	 * @see gov.aps.jca.Channel#getName()
	 */
	public String getName() throws IllegalStateException {
		return name;
	}

	/**
	 * @see gov.aps.jca.Channel#getFieldType()
	 */
	public synchronized DBRType getFieldType() throws IllegalStateException {
		checkState();
		return type;
	}

	/**
	 * @see gov.aps.jca.Channel#getElementCount()
	 */
	public synchronized int getElementCount() throws IllegalStateException {
		checkState();
		return elementCount;
	}

	/**
	 * @see gov.aps.jca.Channel#getConnectionState()
	 */
	public synchronized ConnectionState getConnectionState() throws IllegalStateException {
		return connectionState;
	}

	/**
	 * NOTE: synchronization guarantees that <code>transport</code> is non-<code>null</code> and <code>state == CONNECTED</code>.
	 * @see gov.aps.jca.Channel#getHostName()
	 */
	public synchronized String getHostName() throws IllegalStateException {
		connectionRequiredCheck();
		return transport.getRemoteAddress().getHostName();
	}

	/**
	 * @see gov.aps.jca.Channel#getReadAccess()
	 */
	public synchronized boolean getReadAccess() throws IllegalStateException {
		checkState();
		return (accessRights & CAConstants.CA_PROTO_ACCESS_RIGHT_READ) != 0;
	}

	/**
	 * @see gov.aps.jca.Channel#getWriteAccess()
	 */
	public synchronized boolean getWriteAccess() throws IllegalStateException {
		checkState();
		return (accessRights & CAConstants.CA_PROTO_ACCESS_RIGHT_WRITE) != 0;
	}

	/**
	 * @see gov.aps.jca.Channel#put(byte[])
	 */
	public void put(byte[] value) throws CAException, IllegalStateException {
		put(DBRType.BYTE, value.length, value);
	}

	/**
	 * @see gov.aps.jca.Channel#put(byte[], gov.aps.jca.event.PutListener)
	 */
	public void put(byte[] value, PutListener l)
		throws CAException, IllegalStateException {
		put(DBRType.BYTE, value.length, value, l);
	}

	/**
	 * @see gov.aps.jca.Channel#put(short[])
	 */
	public void put(short[] value) throws CAException, IllegalStateException {
		put(DBRType.SHORT, value.length, value);
	}

	/**
	 * @see gov.aps.jca.Channel#put(short[], gov.aps.jca.event.PutListener)
	 */
	public void put(short[] value, PutListener l)
		throws CAException, IllegalStateException {
		put(DBRType.SHORT, value.length, value, l);
	}

	/**
	 * @see gov.aps.jca.Channel#put(int[])
	 */
	public void put(int[] value) throws CAException, IllegalStateException {
		put(DBRType.INT, value.length, value);
	}

	/**
	 * @see gov.aps.jca.Channel#put(int[], gov.aps.jca.event.PutListener)
	 */
	public void put(int[] value, PutListener l)
		throws CAException, IllegalStateException {
			put(DBRType.INT, value.length, value, l);
	}

	/**
	 * @see gov.aps.jca.Channel#put(float[])
	 */
	public void put(float[] value) throws CAException, IllegalStateException {
		put(DBRType.FLOAT, value.length, value);
	}

	/**
	 * @see gov.aps.jca.Channel#put(float[], gov.aps.jca.event.PutListener)
	 */
	public void put(float[] value, PutListener l)
		throws CAException, IllegalStateException {
		put(DBRType.FLOAT, value.length, value, l);
	}

	/**
	 * @see gov.aps.jca.Channel#put(double[])
	 */
	public void put(double[] value) throws CAException, IllegalStateException {
		put(DBRType.DOUBLE, value.length, value);
	}

	/**
	 * @see gov.aps.jca.Channel#put(double[], gov.aps.jca.event.PutListener)
	 */
	public void put(double[] value, PutListener l)
		throws CAException, IllegalStateException {
		put(DBRType.DOUBLE, value.length, value, l);
	}

	/**
	 * @see gov.aps.jca.Channel#put(java.lang.String[])
	 */
	public void put(String[] value) throws CAException, IllegalStateException {
		put(DBRType.STRING, value.length, value);
	}

	/**
	 * @see gov.aps.jca.Channel#put(java.lang.String[], gov.aps.jca.event.PutListener)
	 */
	public void put(String[] value, PutListener l)
		throws CAException, IllegalStateException {
		put(DBRType.STRING, value.length, value, l);
	}

	/**
	 * @see gov.aps.jca.Channel#putACKS(gov.aps.jca.dbr.Severity, gov.aps.jca.event.PutListener)
	 */
	public void putACKS(Severity severity, PutListener l)
		throws CAException, IllegalStateException {
		put(DBRType.PUT_ACKS, 1, new short[] { (short)severity.getValue()}, l);
	}

	/**
	 * @see gov.aps.jca.Channel#putACKS(gov.aps.jca.dbr.Severity)
	 */
	public void putACKS(Severity severity) throws CAException, IllegalStateException {
		put(DBRType.PUT_ACKS, 1, new short[] { (short)severity.getValue()} );
	}

	/**
	 * @see gov.aps.jca.Channel#putACKT(boolean, gov.aps.jca.event.PutListener)
	 */
	public void putACKT(boolean value, PutListener l) throws CAException, IllegalStateException {
		put(DBRType.PUT_ACKT, 1, new short[] { value ? (short)1 : (short)0 }, l);
	}

	/**
	 * @see gov.aps.jca.Channel#putACKT(boolean)
	 */
	public void putACKT(boolean value) throws CAException, IllegalStateException {
		put(DBRType.PUT_ACKT, 1, new short[] { value ? (short)1 : (short)0 });
	}

	/**
	 * @see gov.aps.jca.Channel#get(gov.aps.jca.dbr.DBRType, int)
	 */
	public DBR get(DBRType type, int count)
		throws CAException, IllegalStateException {
		connectionRequiredCheck();

		if (!getReadAccess())
			throw new CAException("No read access rights granted."); 

		Transport t = getTransport();
		if (t != null)
		{
			try
			{
				DBR retVal = DBRFactory.create(type, count);
				new ReadNotifyRequest(this, null, retVal, t, getServerChannelID(), type.getValue(), count).submit();
				return retVal;
			}
			catch (IOException ioex)
			{
				throw new CAException("Failed to retrieve value.", ioex);
			} 
		}
		else
			throw new IllegalStateException("No channel transport available, channel disconnected.");
	}

	/**
	 * @see gov.aps.jca.Channel#get(gov.aps.jca.dbr.DBRType, int, gov.aps.jca.event.GetListener)
	 */
	public void get(DBRType type, int count, GetListener l)
		throws CAException, IllegalStateException {
		connectionRequiredCheck();

		if (!getReadAccess())
			throw new CAException("No read access rights granted."); 

		Transport t = getTransport();
		if (t != null)
		{
			try
			{
				new ReadNotifyRequest(this, l, null, t, getServerChannelID(), type.getValue(), count).submit();
			}
			catch (IOException ioex)
			{
				throw new CAException("Failed to retrieve value.", ioex);
			} 
		}
		else
			throw new IllegalStateException("No channel transport available, channel disconnected.");
	}

	/**
	 * Put value.
	 * @param type
	 * @param count
	 * @param value
	 * @throws CAException
	 * @throws IllegalStateException
	 */
	public void put(DBRType type, int count, Object value)
		throws CAException, IllegalStateException {
		connectionRequiredCheck();

		if (!getWriteAccess())
			throw new CAException("No write access rights granted."); 

		Transport t = getTransport();
		if (t != null)
		{
			try
			{
				new WriteRequest(this, t, getServerChannelID(), channelID, type.getValue(), count, value).submit();
			}
			catch (IOException ioex)
			{
				throw new CAException("Failed to set value.", ioex);
			} 
		}
		else
			throw new IllegalStateException("No channel transport available, channel disconnected.");
	}

	/**
	 * Put value.
	 * @param type
	 * @param count
	 * @param value
	 * @param l
	 * @throws CAException
	 * @throws IllegalStateException
	 */
	public void put(DBRType type, int count, Object value, PutListener l)
		throws CAException, IllegalStateException {
		connectionRequiredCheck();
		
		if (!getWriteAccess())
			throw new CAException("No write access rights granted."); 

		Transport t = getTransport();
		if (t != null)
		{
			try
			{
				if (l == null)
					new WriteRequest(this, t, getServerChannelID(), channelID, type.getValue(), count, value).submit();
				else
					new WriteNotifyRequest(this, l, t, getServerChannelID(), type.getValue(), count, value).submit();
			}
			catch (IOException ioex)
			{
				throw new CAException("Failed to set value.", ioex);
			} 
		}
		else
			throw new IllegalStateException("No channel transport available, channel disconnected.");

	}

	/**
	 * @see gov.aps.jca.Channel#addMonitor(gov.aps.jca.dbr.DBRType, int, int, gov.aps.jca.event.MonitorListener)
	 */
	public synchronized Monitor addMonitor(
		DBRType type,
		int count,
		int mask,
		MonitorListener l)
		throws CAException, IllegalStateException {
		checkNotClosedState();
		return new CAJMonitor(context, type, count, this, l, mask);
	}

	/**
	 * Get client channel ID.
	 * @return client channel ID.
	 */
	public int getChannelID() {
		return channelID;
	}

	/**
	 * @see gov.aps.jca.Channel#getContext()
	 */
	public Context getContext() throws IllegalStateException {
		return context;
	}

	/**
	 * Checks if channel is in connected state,
	 * if not throws <code>IllegalStateException</code> if not.
	 */
	private void connectionRequiredCheck()
	{
		if (connectionState != ConnectionState.CONNECTED)
			throw new IllegalStateException("Channel not connected.");
	}

	/**
	 * Checks if channel is in connected or disconnected state,
	 * if not throws <code>IllegalStateException</code> if not.
	 */
	private void checkState()
	{
		// connectionState is always non-null
		if (connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.DISCONNECTED)
			throw new IllegalStateException("Channel not in connected or disconnected state, state = '" + connectionState.getName() + "'.");
	}
	
	/**
	 * Checks if channel is not it closed state.
	 * if not throws <code>IllegalStateException</code> if not.
	 */
	private void checkNotClosedState()
	{
		if (connectionState == ConnectionState.CLOSED)
			throw new IllegalStateException("Channel closed.");
	}

	/**
	 * @see gov.aps.jca.Channel#printInfo(java.io.PrintStream)
	 */
	public synchronized void printInfo(PrintStream out) throws IllegalStateException {
		if (connectionState != ConnectionState.CONNECTED)
		{
			out.println("CHANNEL  : " + name);
			out.println("TYPE     : " + type);
			out.println("COUNT    : " + elementCount);
			out.println("STATE    : " + connectionState);
		}
		else
			super.printInfo(out);
	}

	/**
	 * Get transport used by this channel.
	 * @return transport used by this channel.
	 */
	public synchronized CATransport getTransport() {
		return transport;
	}

	/**
	 * Get SID.
	 * @return SID.
	 */
	public synchronized int getServerChannelID() {
		return serverChannelID;
	}

	/**
	 * Register monitor.
	 * @param monitor
	 */
	void registerMonitor(CAJMonitor monitor)
	{
		// lazy initialization (double-check sync. pattern)
		if (monitors == null)
		{
			synchronized (this)
			{
				if (monitors == null)
					monitors = new HashMap();
			}
		}
			
		synchronized (monitors)
		{
			monitors.put(new Integer(monitor.getSID()), monitor);
		}
	}

	/**
	 * Unregister monitor.
	 * @param monitor
	 */
	void unregisterMonitor(CAJMonitor monitor)
	{
		if (monitors == null)
			return;

		synchronized (monitors)
		{
			monitors.remove(new Integer(monitor.getSID()));
		}
	}

	/**
	 * Destroy all monitors.
	 */
	private void destroyAllMonitors() {
		
		if (monitors == null)
			return;
			
		CAJMonitor[] monitorsArray;
		synchronized (monitors)
		{
			monitorsArray = new CAJMonitor[monitors.size()];
			monitors.values().toArray(monitorsArray);
			monitors.clear();
		}

		for (int i = 0; i < monitorsArray.length; i++)
		{
			try
			{
				monitorsArray[i].clear();
			}
			catch (Throwable th)
			{
				logger.log(Level.SEVERE, "", th);
			}
		}
	}


	/** 
	 * Register a response request.
	 * @param responseRequest response request to register.
	 */
	public void registerResponseRequest(ResponseRequest responseRequest)
	{
		synchronized (responseRequests)
		{
			responseRequests.put(responseRequest, null);
		}
	}

	/* 
	 * Unregister a response request.
	 * @param responseRequest response request to unregister.
	 */
	public void unregisterResponseRequest(ResponseRequest responseRequest)
	{
		synchronized (responseRequests)
		{
			responseRequests.remove(responseRequest);
		}
	}
	
	/**
	 * Disconnects (destroys) all channels pending IO.
	 * @param destroy	<code>true</code> if channel is being destroyed.
	 */
	private void disconnectPendingIO(boolean destroy)
	{
		CAStatus status;
		if (destroy)
			status = CAStatus.CHANDESTROY;
		else
			status = CAStatus.DISCONN;
			
		ResponseRequest[] rrs;
		synchronized (responseRequests)
		{
			rrs = new ResponseRequest[responseRequests.size()];
			responseRequests.keySet().toArray(rrs);
		}
		
		for (int i = 0; i < rrs.length; i++)
		{
			try
			{
				rrs[i].exception(status.getStatusCode(), null);
			}
			catch (Throwable th)
			{
				// TODO remove
				logger.log(Level.SEVERE, "", th);
			}
		}
		
	}
	
	/**
	 * Resubscribe subscriptions. 
	 */
	private void resubscribeSubscriptions()
	{
		synchronized (responseRequests)
		{
			// sync get
			Transport transport = getTransport();
			
			ResponseRequest[] rrs = new ResponseRequest[responseRequests.size()];
			responseRequests.keySet().toArray(rrs);
			for (int i = 0; i < rrs.length; i++)
			{
				try
				{
					if (rrs[i] instanceof EventAddRequest)
					{
						EventAddRequest ear = (EventAddRequest)rrs[i];
						ear.resubscribeSubscription(transport);
					}
				}
				catch (Throwable th)
				{
					// TODO remove
					logger.log(Level.SEVERE, "", th);
				}
			}
		}
	}

	/**
	 * Update subscribtions. 
	 */
	private void updateSubscriptions()
	{
		synchronized (responseRequests)
		{
			ResponseRequest[] rrs = new ResponseRequest[responseRequests.size()];
			responseRequests.keySet().toArray(rrs);
			for (int i = 0; i < rrs.length; i++)
			{
				try
				{
					if (rrs[i] instanceof EventAddRequest)
					{
						EventAddRequest ear = (EventAddRequest)rrs[i];
						ear.updateSubscription();
					}
				}
				catch (Throwable th)
				{
					// TODO remove
					logger.log(Level.SEVERE, "", th);
				}
			}
		}
	}

	/**
	 * Get number of search tried for this channel.
	 * @return number of search tried for this channel.
	 */
	public int getSearchTries() {
		return searchTries;
	}

	/**
	 * Get process priority.
	 * @return process priority.
	 */
	public short getPriority() {
		return priority;
	}

	
	protected Object ownerLock = new Object();
	protected ArrayFIFO owner = null;
	protected int ownerIndex = -1;
	
	public void unsetListOwnership() {
		synchronized (ownerLock) {
			owner = null;
		}
	}
	
	public void addAndSetListOwnership(ArrayFIFO newOwner, int index) {
		synchronized (newOwner) {
			synchronized (ownerLock) {
				//System.out.println("changing list ownership of " + name + " to index:" + index);			
				newOwner.push(this);
				owner = newOwner;
				ownerIndex = index;
			}
		}
	}

	public void removeAndUnsetListOwnership() {
		if (owner == null)
			return;
		
		synchronized (owner) {
			synchronized (ownerLock) {
				if (owner != null) {
					owner.remove(this);
					owner = null;
				}
			}
		}
	}
	
	public final int getOwnerIndex() {
		synchronized (ownerLock) {
			return ownerIndex;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return getClass().getSimpleName() + " = { name = " + name + ", connectionState = " + connectionState.getName() + " }";
	}
}
