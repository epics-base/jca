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
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cosylab.epics.caj.impl.BroadcastConnector;
import com.cosylab.epics.caj.impl.BroadcastTransport;
import com.cosylab.epics.caj.impl.CABeaconHandler;
import com.cosylab.epics.caj.impl.CAConnector;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.CAContext;
import com.cosylab.epics.caj.impl.CAResponseHandler;
import com.cosylab.epics.caj.impl.CATransport;
import com.cosylab.epics.caj.impl.CATransportRegistry;
import com.cosylab.epics.caj.impl.CachedByteBufferAllocator;
import com.cosylab.epics.caj.impl.ChannelSearchManager;
import com.cosylab.epics.caj.impl.ConnectionException;
import com.cosylab.epics.caj.impl.RepeaterRegistrationTask;
import com.cosylab.epics.caj.impl.ResponseRequest;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.TransportClient;
import com.cosylab.epics.caj.impl.reactor.Reactor;
import com.cosylab.epics.caj.impl.reactor.lf.LeaderFollowersThreadPool;
import com.cosylab.epics.caj.impl.sync.NamedLockPattern;
import com.cosylab.epics.caj.util.InetAddressUtil;
import com.cosylab.epics.caj.util.IntHashMap;
import com.cosylab.epics.caj.util.Timer;
import com.cosylab.epics.caj.util.logging.ConsoleLogHandler;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.Version;
import gov.aps.jca.configuration.Configurable;
import gov.aps.jca.configuration.Configuration;
import gov.aps.jca.configuration.ConfigurationException;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.ContextExceptionEvent;
import gov.aps.jca.event.ContextExceptionListener;
import gov.aps.jca.event.ContextMessageListener;
import gov.aps.jca.event.DirectEventDispatcher;
import gov.aps.jca.event.EventDispatcher;

/**
 * Implementation of CAJ JCA <code>Context</code>. 
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJContext extends Context implements CAContext, CAJConstants, Configurable {

    /**
     * Major version.
     */
    private static final int CAJ_VERSION_MAJOR = 1;
    
    /**
     * Minor version.
     */
    private static final int CAJ_VERSION_MINOR = 1;

    /**
     * Maintenance version.
     */
    private static final int CAJ_VERSION_MAINTENANCE = 7;

    /**
     * Development version.
     */
    private static final int CAJ_VERSION_DEVELOPMENT = 0;

    /**
     * Version.
     */
    public static final Version VERSION = new Version(
            "Channel Access in Java", "Java",
            CAJ_VERSION_MAJOR, CAJ_VERSION_MINOR,
            CAJ_VERSION_MAINTENANCE, CAJ_VERSION_DEVELOPMENT);
	  
    /**
     * String value of the JVM property key to turn on single threaded model. 
     */
    public static final String CAJ_SINGLE_THREADED_MODEL = "CAJ_SINGLE_THREADED_MODEL";
    
	/**
	 * State value of non-initialized context.
	 */
	private static final int NOT_INITIALIZED = 0;

	/**
	 * State value of initialized context.
	 */
	private static final int INITIALIZED = 1;

	/**
	 * State value of destroyed context.
	 */
	private static final int DESTROYED = 2;

	/**
	 * Initialization status.
	 */
	private volatile int state = NOT_INITIALIZED;
	
	/**
	 * Context logger.
	 */
	protected Logger logger = Logger.global;

	/**
	 * A space-separated list of broadcast address for process variable name resolution.
	 * Each address must be of the form: ip.number:port or host.name:port
	 */
	protected String addressList = "";
	
	/**
	 * Define whether or not the network interfaces should be discovered at runtime. 
	 */
	protected boolean autoAddressList = true;
	
	/**
	 * If the context doesn't see a beacon from a server that it is connected to for
	 * connectionTimeout seconds then a state-of-health message is sent to the server over TCP/IP.
	 * If this state-of-health message isn't promptly replied to then the context will assume that
	 * the server is no longer present on the network and disconnect.
	 */
	protected float connectionTimeout = 30.0f;
	
	/**
	 * Period in second between two beacon signals.
	 */
	protected float beaconPeriod = 15.0f;
	
	/**
	 * Port number for the repeater to listen to.
	 */
	protected int repeaterPort = CAConstants.CA_REPEATER_PORT;
	
	/**
	 * Port number for the server to listen to.
	 */
	protected int serverPort = CAConstants.CA_SERVER_PORT;
	
	/**
	 * Length in bytes of the maximum array size that may pass through CA.
	 */
	protected int maxArrayBytes = 16384;

	/**
	 * List of context message listeners.
	 */
	// TODO consider using weak references
	protected ArrayList contextMessageListeners = new ArrayList();

	/**
	 * List of context exception listeners.
	 */
	// TODO consider using weak references
	protected ArrayList contextExceptionListeners = new ArrayList();


	/**
	 * Event dispatcher.
	 */
	protected EventDispatcher eventDispatcher = new DirectEventDispatcher(); 

	/**
	 * Timer.
	 */
	protected Timer timer = null;

	/**
	 * Reactor.
	 */
	protected Reactor reactor = null;

	/**
	 * Leader/followers thread pool.
	 */
	protected LeaderFollowersThreadPool leaderFollowersThreadPool = null;

	/**
	 * Registration confirmation status.
	 */
	protected  boolean registrationConfirmed = false;

	/**
	 * Registration confirmation condition variable. 
	 */
	private Object registrationConfirmedCondition = new Object();

	/**
	 * Broadcast transport needed for channel searches.
	 */
	protected BroadcastTransport broadcastTransport = null;
	
	/**
	 * CA connector (creates CA virtual circuit).
	 */
	protected CAConnector connector = null;

	/**
	 * CA transport (virtual circuit) registry.
	 * This registry contains all active transports - connections to CA servers. 
	 */
	protected CATransportRegistry transportRegistry = null;

	/**
	 * Cached byte buffer allocator.
	 * Used by transports which as send buffers.
	 */
	protected CachedByteBufferAllocator cachedBufferAllocator = new CachedByteBufferAllocator();

	/**
	 * Context instance.
	 */
	private NamedLockPattern namedLocker;

	/**
	 * Context instance.
	 */
	private static final int LOCK_TIMEOUT = 20 * 1000;	// 20s

	/**
	 * Map of channels (keys are CIDs).
	 */
	// TODO consider using WeakHashMap (and call Channel.destroy() in finalize() method).
	protected IntHashMap channelsByCID = new IntHashMap();

	/**
	 * Map of channels (keys are names).
	 */
	// TODO consider using WeakHashMap (and call Channel.destroy() in finalize() method).
	protected Map channelsByName = new HashMap();

	/**
	 * Last CID cache. 
	 */
	private int lastCID = 0;

	/**
	 * Map of pending response requests (keys are IOID).
	 */
	// TODO consider using WeakHashMap (and call ResponseRequest.destroy() in finalize() method).
	protected IntHashMap pendingResponseRequests = new IntHashMap();

	/**
	 * Last IOID cache. 
	 */
	private int lastIOID = 0;


	/**
	 * Pending requests (get, create channel) counter. 
	 */
	private AtomicInteger pendingRequestsCount = new AtomicInteger(0);

	/**
	 * IO sequence number (to prevent future IO request to interfere with current pendIO). 
	 */
	private AtomicInteger sequenceNumberIO = new AtomicInteger(0);

	/**
	 * Zelo pending requests condition - triggered when counter drops to 0. 
	 */
	private Object zeroPendingRequestsCondition = new Object();

	/**
	 * Channel search manager.
	 * Manages UDP search requests.
	 */
	private ChannelSearchManager channelSearchManager;

	/**
	 * Beacon handler map.
	 */
	protected Map beaconHandlers = new HashMap();

	/**
	 * Last UDP recived sequence number.
	 */
	private AtomicInteger lastReceivedSequenceNumber = new AtomicInteger(0);
	
	/**
	 * Constructor.
	 */
	public CAJContext()
	{
		initializeLogger();
		loadConfiguration();
	}
	
    /**
     * Get context version.
     * @see Context#getVersion()
     */
    public Version getVersion()
    {
        return VERSION;
    }
    
	/**
	 * Initialize context logger.
	 */
	protected void initializeLogger()
	{
		JCALibrary jcaLibrary = JCALibrary.getInstance();
		String thisClassName = this.getClass().getName();
		String loggerName = jcaLibrary.getProperty(thisClassName + ".logger", thisClassName);
		logger = Logger.getLogger(loggerName);
		
		if (System.getProperties().containsKey(CAJ_DEBUG))
		{
			logger.setLevel(Level.ALL);
			logger.addHandler(new ConsoleLogHandler());
		}
	}

	/**
	 * Get system environment variable.
	 * @param name	env. var. name
	 * @param defaultValue	default value
	 * @return	value of env. var., default value if not defined
	 *
	private static String getEnvironmentVariable(String name, String defaultValue) {
	    String val = null;
	    try {
	        val = System.getenv(name);
	    } catch (Throwable th) { 
	    	// noop
		}
	    
	    if (val == null)
	        return defaultValue;
	    else
	        return val;
	}*/
	
	/**
	 * Load configuration.
	 */
	protected void loadConfiguration()
	{
		JCALibrary jcaLibrary = JCALibrary.getInstance();

		String eventDispatcherClassName = null;
		final String thisClassName = this.getClass().getName();
	    if (Boolean.getBoolean("jca.use_env"))
	    {
	    	// Context default configuration
	    	eventDispatcherClassName = jcaLibrary.getProperty( gov.aps.jca.Context.class.getName()+".event_dispatcher", eventDispatcherClassName );

	        String tmp = System.getenv("EPICS_CA_ADDR_LIST");
	        if (tmp != null) addressList = tmp;
	        
	    	tmp = System.getenv("EPICS_CA_AUTO_ADDR_LIST");
	    	if (tmp != null)
	    		autoAddressList = !tmp.equalsIgnoreCase("NO"); 
	    	else
	    		autoAddressList = true;
	    	
	    	tmp = System.getenv("EPICS_CA_CONN_TMO");
	    	if (tmp != null) connectionTimeout = Float.parseFloat(tmp);
	    	
	    	tmp = System.getenv("EPICS_CA_BEACON_PERIOD");
	       	if (tmp != null) beaconPeriod = Float.parseFloat(tmp);
	           	
	    	tmp = System.getenv("EPICS_CA_REPEATER_PORT");
	    	if (tmp != null) repeaterPort = Integer.parseInt(tmp);
	    	
	    	tmp = System.getenv("EPICS_CA_SERVER_PORT");
	    	if (tmp != null) serverPort = Integer.parseInt(tmp);

	    	tmp = System.getenv("EPICS_CA_MAX_ARRAY_BYTES");
	    	if (tmp != null) maxArrayBytes = Integer.parseInt(tmp);
	    }
	    else
	    {
			// load default Context configuration
			final String contextClassName = Context.class.getName();
			addressList = jcaLibrary.getProperty(contextClassName + ".addr_list", addressList);
			autoAddressList = jcaLibrary.getPropertyAsBoolean(contextClassName + ".auto_addr_list",  autoAddressList);
			connectionTimeout = jcaLibrary.getPropertyAsFloat(contextClassName + ".connection_timeout", connectionTimeout);
			beaconPeriod = jcaLibrary.getPropertyAsFloat(contextClassName + ".beacon_period", beaconPeriod);
			repeaterPort = jcaLibrary.getPropertyAsInt(contextClassName + ".repeater_port", repeaterPort);
			serverPort = jcaLibrary.getPropertyAsInt(contextClassName + ".server_port", serverPort);
			maxArrayBytes = jcaLibrary.getPropertyAsInt(contextClassName + ".max_array_bytes", maxArrayBytes);
			eventDispatcherClassName = jcaLibrary.getProperty(contextClassName + ".event_dispatcher");
	
			// load CAJ specific configuration (overrides default)
			addressList = jcaLibrary.getProperty(thisClassName + ".addr_list", addressList);
			autoAddressList = jcaLibrary.getPropertyAsBoolean(thisClassName + ".auto_addr_list",  autoAddressList);
			connectionTimeout = jcaLibrary.getPropertyAsFloat(thisClassName + ".connection_timeout", connectionTimeout);
			beaconPeriod = jcaLibrary.getPropertyAsFloat(thisClassName + ".beacon_period", beaconPeriod);
			repeaterPort = jcaLibrary.getPropertyAsInt(thisClassName + ".repeater_port", repeaterPort);
			serverPort = jcaLibrary.getPropertyAsInt(thisClassName + ".server_port", serverPort);
			maxArrayBytes = jcaLibrary.getPropertyAsInt(thisClassName + ".max_array_bytes", maxArrayBytes);
	    }
			
		eventDispatcherClassName = jcaLibrary.getProperty(thisClassName + ".event_dispatcher", eventDispatcherClassName);
		if (eventDispatcherClassName != null)
		{
			try
			{
				eventDispatcher = (EventDispatcher)Class.forName(eventDispatcherClassName).newInstance();
			} catch(Throwable th) {
				logger.log(Level.WARNING, "Failed to instantiate '" + eventDispatcherClassName + "' event dispatcher.", th);
			}
		}

	}


	/**
	 * @see gov.aps.jca.configuration.Configurable#configure(gov.aps.jca.configuration.Configuration)
	 */
	public void configure(Configuration configuration)
		throws ConfigurationException {

			// address list
			try {
			  addressList = configuration.getChild("addr_list", false).getValue();
			} catch(Exception ex) {
				addressList= configuration.getAttribute("addr_list", addressList);
			}
    
			// auto address list
			try {
				autoAddressList = configuration.getChild("auto_addr_list", false).getValueAsBoolean();
			} catch(Exception ex) {
				autoAddressList = configuration.getAttributeAsBoolean("auto_addr_list", autoAddressList);
			}

			// connection timeout
			try {
				connectionTimeout = configuration.getChild("connection_timeout", false).getValueAsFloat();
			} catch(Exception ex) {
				connectionTimeout = configuration.getAttributeAsFloat("connection_timeout", connectionTimeout);
			}
    
    		// beacon period
			try {
				beaconPeriod = configuration.getChild("beacon_period", false).getValueAsFloat();
			} catch(Exception ex) {
				beaconPeriod = configuration.getAttributeAsFloat("beacon_period", beaconPeriod);
			}

			// repeater port    
			try {
				repeaterPort = configuration.getChild("repeater_port", false).getValueAsInteger();
			} catch(Exception ex) {
				repeaterPort = configuration.getAttributeAsInteger("repeater_port", repeaterPort);
			}

			// server port    
			try {
				serverPort = configuration.getChild("server_port", false).getValueAsInteger();
			} catch(Exception ex) {
				serverPort = configuration.getAttributeAsInteger("server_port", serverPort);
			}
    
    		// max. array bytes
			try {
				maxArrayBytes = configuration.getChild("max_array_bytes", false).getValueAsInteger();
			} catch(Exception ex) {
				maxArrayBytes = configuration.getAttributeAsInteger("max_array_bytes", maxArrayBytes);
			}

			// event dispathcer
			Configuration conf = configuration.getChild("event_dispatcher", false);
			if (conf != null)
			{
				String eventDispatcherClassName = null;
				try {
				    eventDispatcherClassName = conf.getAttribute("class");
				} catch (ConfigurationException noAttribute) {
					logger.log(Level.WARNING, "Failed to obtain 'event_dispatcher' node's 'class' attribute.", noAttribute);
				}
				if (eventDispatcherClassName != null)
				{
					try
					{
						eventDispatcher = (EventDispatcher)Class.forName(eventDispatcherClassName).newInstance();
					} catch(Throwable th) {
						logger.log(Level.WARNING, "Failed to instantiate '" + eventDispatcherClassName + "' event dispatcher.", th);
					}
				}
			}
	}

	/**
	 * @see gov.aps.jca.Context#getContextMessageListeners()
	 */
	public ContextMessageListener[] getContextMessageListeners()
		throws IllegalStateException {
		synchronized (contextMessageListeners)
		{
			ContextMessageListener[] listeners = new ContextMessageListener[contextMessageListeners.size()]; 
			return (ContextMessageListener[])contextMessageListeners.toArray(listeners);
		}
	}

	/**
	 * @see gov.aps.jca.Context#addContextMessageListener(gov.aps.jca.event.ContextMessageListener)
	 */
	public void addContextMessageListener(ContextMessageListener l)
		throws CAException, IllegalStateException {
		checkState();
		
		if (l == null)
			throw new IllegalArgumentException("l == null");
			
		synchronized(contextMessageListeners)
		{
			if (!contextMessageListeners.contains(l))
				contextMessageListeners.add(l);
		}
	}

	/**
	 * @see gov.aps.jca.Context#removeContextMessageListener(gov.aps.jca.event.ContextMessageListener)
	 */
	public void removeContextMessageListener(ContextMessageListener l)
		throws CAException, IllegalStateException {
		checkState();

		if (l == null)
			throw new IllegalArgumentException("l == null");
			
		synchronized(contextMessageListeners)
		{
			contextMessageListeners.remove(l);
		}
	}

	/**
	 * @see gov.aps.jca.Context#getContextExceptionListeners()
	 */
	public ContextExceptionListener[] getContextExceptionListeners()
		throws IllegalStateException {
		synchronized (contextExceptionListeners)
		{
			ContextExceptionListener[] listeners = new ContextExceptionListener[contextExceptionListeners.size()];
			return (ContextExceptionListener[])contextExceptionListeners.toArray(listeners);
		}
	}

	/**
	 * @see gov.aps.jca.Context#addContextExceptionListener(gov.aps.jca.event.ContextExceptionListener)
	 */
	public void addContextExceptionListener(ContextExceptionListener l)
		throws CAException, IllegalStateException {
			checkState();
		
			if (l == null)
				throw new IllegalArgumentException("l == null");
			
			synchronized(contextExceptionListeners)
			{
				if (!contextExceptionListeners.contains(l))
					contextExceptionListeners.add(l);
			}
	}

	/**
	 * @see gov.aps.jca.Context#removeContextExceptionListener(gov.aps.jca.event.ContextExceptionListener)
	 */
	public void removeContextExceptionListener(ContextExceptionListener l)
		throws CAException, IllegalStateException {
		checkState();

		if (l == null)
			throw new IllegalArgumentException("l == null");
			
		synchronized(contextExceptionListeners)
		{
			contextExceptionListeners.remove(l);
		}
	}

	/**
	 * Notifies context listeners about exception.
	 * @param event	context exception event to be fired.	
	 */
	public void notifyException(ContextExceptionEvent event) {

	    ContextExceptionListener[] listeners = getContextExceptionListeners();
		for (int i = 0; i < listeners.length; i++)
		{
			try
			{
				listeners[i].contextException(event);
			}
			catch (Throwable th)
			{
				// TODO remove
				logger.log(Level.SEVERE, "", th);
			}
		}
	}

	/**
	 * Check context state and tries to establish necessary state.
	 * @throws CAException
	 * @throws IllegalStateException
	 */
	protected void checkState() throws CAException, IllegalStateException {
		if (state == DESTROYED)
			throw new IllegalStateException("Context destroyed.");
		else if (state == NOT_INITIALIZED)
		{
			// double-locking pattern used to prevent unnecessary initialization calls 
			synchronized (this)
			{
				if (state == NOT_INITIALIZED)
					initialize();
			}
		}
	}

	/**
	 * @see gov.aps.jca.Context#initialize()
	 */
	public synchronized void initialize() throws CAException {
		
		if (state == DESTROYED)
			throw new IllegalStateException("Context destroyed.");
		else if (state == INITIALIZED)
			throw new IllegalStateException("Context already initialized.");
		
		super.initialize();
		
		internalInitialize();
		
		state = INITIALIZED;
		
	}

	/**
	 * @throws CAException
	 */
	private void internalInitialize() throws CAException {
		
		try {
			CARepeater.startRepeater(repeaterPort);
		} catch (Throwable th) { /* noop */ }
		
		timer = new Timer();
		connector = new CAConnector(this);
		transportRegistry = new CATransportRegistry();
		namedLocker = new NamedLockPattern();

		try
		{
			reactor = new Reactor();
			
			if (System.getProperties().containsKey(CAJ_SINGLE_THREADED_MODEL))
			{
			    logger.config("Using single threaded model.");
			    
				// single thread processing
				new Thread(
				        new Runnable() {
				            /**
				        	 * @see java.lang.Runnable#run()
				        	 */
				        	public void run() {
				        		// do the work
				        		while (reactor.process());
				        	}
				        	
				        }, "CA reactor").start();
			}
			else
			{
			    // leader/followers processing
			    leaderFollowersThreadPool = new LeaderFollowersThreadPool();
				// spawn initial leader
				leaderFollowersThreadPool.promoteLeader(
				        new Runnable() {
				            /**
				        	 * @see java.lang.Runnable#run()
				        	 */
				        	public void run() {
				        		reactor.process();
				        	}
						}
				);
			}
			
		}
		catch (IOException ioex)
		{
			throw new CAException("Failed to initialize reactor.", ioex); 
		}
		
		// setup UDP transport
		initializeUDPTransport();

		// setup search manager
		channelSearchManager = new ChannelSearchManager(this);
	}

	/**
	 * Initialized UDP transport (broadcast socket and repeater connection).
	 */
	private void initializeUDPTransport() {
		// setup UDP transport
		try
		{
			InetSocketAddress repeaterLocalAddress = new InetSocketAddress("127.0.0.1", repeaterPort);
		
			BroadcastConnector broadcastConnector = new BroadcastConnector(this);
			
			broadcastTransport = (BroadcastTransport)broadcastConnector.connect(
						null, new CAResponseHandler(this),
						repeaterLocalAddress, CAConstants.CA_MINOR_PROTOCOL_REVISION,
						CAConstants.CA_DEFAULT_PRIORITY);
		
			// set broadcast address list
			if (addressList != null && addressList.length() > 0)
			{
				// if auto is true, add it to specified list
				InetSocketAddress[] appendList = null;
				if (autoAddressList == true)
					appendList = broadcastTransport.getBroadcastAddresses();
				
				InetSocketAddress[] list = InetAddressUtil.getSocketAddressList(addressList, serverPort, appendList);
				if (list != null && list.length > 0)
					broadcastTransport.setBroadcastAddresses(list);
			}

			RepeaterRegistrationTask registrationTask = new RepeaterRegistrationTask(this, repeaterLocalAddress);
			
			// try immediately (often repeater is OK and will confirm immediately)
			synchronized (registrationConfirmedCondition)
			{
				registrationTask.registrationRequest();
				
				try
				{
					// spurious wakeup wont hurt here... 
					if (!registrationConfirmed)
					    registrationConfirmedCondition.wait(100);
				} catch (InterruptedException ie) {}
			}
		
			// retry every second
			if (!registrationConfirmed)
				registrationTask.runInBackground(1000);
			
		}
		catch (ConnectionException ce)
		{
			logger.log(Level.SEVERE, "Failed to initialize UDP transport.", ce);
		}
		catch (UnknownHostException uhe)
		{
			logger.log(Level.SEVERE, "Failed to obtain local host address.", uhe);
		}
	}
	 
	/**
	 * @see gov.aps.jca.Context#destroy()
	 */
	public synchronized void destroy() throws CAException, IllegalStateException {

		if (state == DESTROYED)
			throw new IllegalStateException("Context already destroyed.");

		// go into destroyed state ASAP			
		state = DESTROYED;
		
		internalDestroy();
				
	}

	/**
	 * @throws CAException
	 */
	private void internalDestroy() throws CAException {

		// stop searching
		if (channelSearchManager != null)
			channelSearchManager.cancel();

		// stop timer
		if (timer != null) 
			timer.shutDown();
		 
		//
		// cleanup
		//
		
		// stop waiting
		synchronized (zeroPendingRequestsCondition)
		{
			zeroPendingRequestsCondition.notifyAll();
		}

		// this will also close all CA transports
		destroyAllChannels();
		
		// shutdown reactor
		if (reactor != null)
			reactor.shutdown();
		
		// shutdown LF thread pool
		if (leaderFollowersThreadPool != null)
		    leaderFollowersThreadPool.shutdown();
		
		// TODO still some events can be in queue (e.g. channel destroyed)
		// reposibility of the event dispatcher?
		// shutdown dispatcher
		if (eventDispatcher != null)
			eventDispatcher.dispose();
		
		synchronized (contextMessageListeners)
		{
			contextMessageListeners.clear();
		}
				
		synchronized (contextExceptionListeners)
		{
			contextExceptionListeners.clear();
		}
		
	}

	/**
	 * Destroy all channels.
	 */
	private void destroyAllChannels() {
		
		Channel[] channelsArray = getChannels();
		synchronized (channelsByCID)
		{
			channelsByCID.clear();
			channelsByName.clear();
		}
		
		for (int i = 0; i < channelsArray.length; i++)
		{
			try
			{
				// force destruction regardless of reference count
				((CAJChannel)channelsArray[i]).destroy(true);
			}
			catch (Throwable th)
			{
				logger.log(Level.SEVERE, "", th);
			}
		}
	}

	/**
	 * @see gov.aps.jca.Context#createChannel(java.lang.String, gov.aps.jca.event.ConnectionListener, short)
	 */
	public Channel createChannel(String name, ConnectionListener l, short priority)
		throws CAException, IllegalStateException {
		checkState();
		
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("null or empty channel name");
		else if (name.length() > Math.min(CAConstants.MAX_UDP_SEND - CAConstants.CA_MESSAGE_HEADER_SIZE, 0xFFFF))
			throw new CAException("name too long");
		
		if (priority < Channel.PRIORITY_MIN || priority > Channel.PRIORITY_MAX)
			throw new IllegalArgumentException("priority out of bounds");
		
		// lookup for channel w/o named lock
		CAJChannel channel = getChannel(name, priority, true);
		if (channel != null)
		{
			if (l != null)
				channel.addConnectionListenerAndFireIfConnected(l);
			return channel;
		}
			
		boolean lockAcquired = namedLocker.acquireSynchronizationObject(name, LOCK_TIMEOUT);
		if (lockAcquired)
		{ 
			try
			{   
				// ... lookup for channel, if created while acquiring lock
				channel = getChannel(name, priority, true);
				if (channel != null)
				{
					if (l != null)
						channel.addConnectionListenerAndFireIfConnected(l);
					return channel;
				}
									
				int cid = generateCID();
				channel = new CAJChannel(this, cid, name, l, priority);
				
				return channel;
			}
			finally
			{
				namedLocker.releaseSynchronizationObject(name);	
			}
		}
		else
		{     
			throw new CAException("Failed to obtain synchronization lock for '" + name + "', possible deadlock.", null);
		}
	}
	
	/**
	 * Destroy channel.
	 * @param channel
	 * @param force
	 * @throws CAException
	 * @throws IllegalStateException
	 */
	public void destroyChannel(CAJChannel channel, boolean force)
		throws CAException, IllegalStateException {
			
		boolean lockAcquired = namedLocker.acquireSynchronizationObject(channel.getName(), LOCK_TIMEOUT);
		if (lockAcquired)
		{ 
			try
			{   
				channel.destroyChannel(force);
			}
			catch (IOException ioex)
			{
				logger.log(Level.SEVERE, "Failed to cleanly destroy channel.", ioex);
				throw new CAException("Failed to cleanly destroy channel.", ioex);
			}
			finally
			{
				namedLocker.releaseSynchronizationObject(channel.getName());	
			}
		}
		else
		{     
			throw new CAException("Failed to obtain synchronization lock for '" + channel.getName() + "', possible deadlock.", null);
		}
	}

	/**
	 * Register channel.
	 * @param channel
	 */
	void registerChannel(CAJChannel channel)
	{
		synchronized (channelsByCID)
		{
			channelsByCID.put(channel.getChannelID(), channel);
			channelsByName.put(getUniqueChannelName(channel.getName(), channel.getPriority()), channel);
		}
	}

	/**
	 * Unregister channel.
	 * @param channel
	 */
	void unregisterChannel(CAJChannel channel)
	{
		synchronized (channelsByCID)
		{
			channelsByCID.remove(channel.getChannelID());
			channelsByName.remove(getUniqueChannelName(channel.getName(), channel.getPriority()));
		}
	}

	/**
	 * Searches for a channel with given channel ID.
	 * @param channelID CID.
	 * @return channel with given CID, <code>null</code> if non-existant.
	 */
	public CAJChannel getChannel(int channelID)
	{
		synchronized (channelsByCID)
		{
			return (CAJChannel)channelsByCID.get(channelID);
		}
	}

	/**
	 * Generate unique channel string from channel name and priority.
	 * @param name channel name.
	 * @param priority channel priority.
	 * @return unique channel string.
	 */
	private final String getUniqueChannelName(String name, short priority)
	{
		// this name is illegal for CA, so this funcion is unique
		return name + '\0' + priority;
	}
	
	/**
	 * Searches for a channel with given channel name.
	 * @param name channel name.
	 * @param priority channel priority.
	 * @param acquire whether to acquire ownership (increment ref. counting)
	 * @return channel with given name, <code>null</code> if non-existant.
	 */
	public CAJChannel getChannel(String name, short priority, boolean acquire)
	{
		synchronized (channelsByName)
		{
			CAJChannel channel = (CAJChannel)channelsByName.get(getUniqueChannelName(name, priority));
			if (channel != null && acquire)
				channel.acquire();
			return channel;
		}
	}

	/**
	 * @see gov.aps.jca.Context#getChannels()
	 */
	public Channel[] getChannels() {
		synchronized (channelsByCID)
		{
			Channel[] ch = new Channel[channelsByCID.size()];
			return (Channel[])channelsByCID.toArray(ch);
		}
	}

	/**
	 * @see gov.aps.jca.Context#pendIO(double)
	 */
	public void pendIO(double timeout)
		throws TimeoutException, CAException, IllegalStateException {
		checkState();

		final long time = System.currentTimeMillis();
		flushIO();
		
		long timeToWaitInMS = 0;
		if (timeout >= 0.0)
		{
			try
			{
				synchronized (zeroPendingRequestsCondition)
				{
					// wait until completed
					if (timeout == 0.0)
					{
						while (pendingRequestsCount.get() > 0 && state != DESTROYED)
							zeroPendingRequestsCondition.wait();
					}
					else
					{
						final long endTime = time + (long)(timeout*1000);
						while (pendingRequestsCount.get() > 0 && (timeToWaitInMS = (endTime - System.currentTimeMillis())) > 0 && state != DESTROYED)
						{
							zeroPendingRequestsCondition.wait(timeToWaitInMS);
						}
					}
				}
			} catch (InterruptedException e) { /* noop */ }
		}
		
		int stillPending;
		// reset pendingRequestsCount and increase sequenceNumberIO (new session)
		synchronized (sequenceNumberIO)
		{
			sequenceNumberIO.incrementAndGet();
			stillPending = pendingRequestsCount.getAndSet(0);
		}
		
		// throw timeout exception if not all requests where processed
		if (stillPending > 0)
		{
			if (state == DESTROYED)
				throw new CAException("context destroyed during pendIO");
				
			if (timeToWaitInMS <= 0)
				throw new TimeoutException("pendIO timed out");
		}
	}

	/**
	 * @see gov.aps.jca.Context#testIO()
	 */
	public boolean testIO() throws CAException, IllegalStateException {
		checkState();
		return pendingRequestsCount.get() == 0;
	}

	/**
	 * @see gov.aps.jca.Context#pendEvent(double)
	 */
	public void pendEvent(double timeout)
		throws CAException, IllegalStateException {
		checkState();

		long time = System.currentTimeMillis();
		flushIO();
		time = System.currentTimeMillis() - time;
		
		long timeToWaitInMS = (long)(timeout*1000) - time;
		if (timeout == 0.0 || timeToWaitInMS > 0)
		{
			// ... CAJ does not know any background activity (it is all preemptive), so just sleep
			try {
				if (timeout == 0.0)
					// sleep until forever (or interrupted)
					Thread.currentThread().join();
				else
					Thread.sleep(timeToWaitInMS);
			} catch (InterruptedException e) { /* noop */ }
		}

	}

	/**
	 * @see gov.aps.jca.Context#poll()
	 */
	public void poll() throws CAException, IllegalStateException {
		checkState();
		flushIO();
		// ... CAJ does not know any background activity (it is all preemptive) 
	}

	/**
	 * @see gov.aps.jca.Context#flushIO()
	 */
	public void flushIO() throws CAException, IllegalStateException {
		checkState();

		Transport[] transports = transportRegistry.toArray();
		for (int i = 0; i < transports.length; i++)
			((CATransport)transports[i]).flush();
	}

	/**
	 * @see gov.aps.jca.Context#attachCurrentThread()
	 */
	public void attachCurrentThread()
		throws CAException, IllegalStateException {
		checkState();
		// noop
	}

	/**
	 * @see gov.aps.jca.Context#printInfo(java.io.PrintStream)
	 */
	public void printInfo(PrintStream out) throws IllegalStateException {
		super.printInfo(out);
		out.println("ADDR_LIST : " + addressList);
		out.println("AUTO_ADDR_LIST : " + autoAddressList);
		out.println("CONNECTION_TIMEOUT : " + connectionTimeout);
		out.println("BEACON_PERIOD : " + beaconPeriod);
		out.println("REPEATER_PORT : " + repeaterPort);
		out.println("SERVER_PORT : " + serverPort);
		out.println("MAX_ARRAY_BYTES : " + maxArrayBytes);
		out.println("EVENT_DISPATCHER: " + eventDispatcher);
		out.print("STATE : ");
		switch (state)
		{
			case NOT_INITIALIZED:
				out.println("NOT_INITIALIZED");
				break;
			case INITIALIZED:
				out.println("INITIALIZED");
				break;
			case DESTROYED:
				out.println("DESTROYED");
				break;
			default:
				out.println("UNKNOWN");
		}
	}

	/**
	 * Get initialization status.
	 * @return initialization status.
	 */
	public boolean isInitialized() {
		return state == INITIALIZED;
	}

	/**
	 * Get destruction status.
	 * @return destruction status.
	 */
	public boolean isDestroyed() {
		return state == DESTROYED;
	}
	
	/**
	 * Get search address list.
	 * @return get search address list.
	 */
	public String getAddressList() {
		return addressList;
	}

	/**
	 * Get auto search-list flag.
	 * @return auto search-list flag.
	 */
	public boolean isAutoAddressList() {
		return autoAddressList;
	}

	/**
	 * Get beacon period (in seconds).
	 * @return beacon period (in seconds).
	 */
	public float getBeaconPeriod() {
		return beaconPeriod;
	}

	/**
	 * Get connection timeout (in seconds).
	 * @return connection timeout (in seconds). 
	 */
	public float getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * Get logger.
	 * @return logger.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Get max size of payload.
	 * @return max size of payload.
	 */
	public int getMaxArrayBytes() {
		return maxArrayBytes;
	}

	/**
	 * Get repeater port.
	 * @return repeater port.
	 */
	public int getRepeaterPort() {
		return repeaterPort;
	}

	/**
	 * Get server port.
	 * @return server port.
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * Get broadcast port.
	 * @return broadcast port.
	 */
	public int getBroadcastPort() {
		return getServerPort();
	}

	/**
	 * Get event dispatcher.
	 * @return event dispatcher.
	 */
	public final EventDispatcher getEventDispatcher() {
		return eventDispatcher;
	}

	// ************************************************************************** //

	/**
	 * Get context reactor.
	 * @return context reactor.
	 */
	public Reactor getReactor() {
		return reactor;
	}

	/**
	 * Broadcast transport.
	 * @return broadcast transport.
	 */
	public BroadcastTransport getBroadcastTransport() {
		return broadcastTransport;
	}

	/**
	 * Get CA transport (virtual circuit) registry.
	 * @return CA transport (virtual circuit) registry.
	 */
	public CATransportRegistry getTransportRegistry() {
		return transportRegistry;
	}

	/**
	 * Get timer.
	 * @return timer.
	 */
	public Timer getTimer() {
		return timer;
	}

	/**
	 * Get channel search manager.
	 * @return channel search manager.
	 */
	public ChannelSearchManager getChannelSearchManager() {
		return channelSearchManager;
	}

    /**
     * Get cached byte allocator.
     * @return cached byte allocator.
     */
    public CachedByteBufferAllocator getCachedBufferAllocator() {
        return cachedBufferAllocator;
    }

    /**
     * Get LF thread pool.
     * @return LF thread pool, can be <code>null</code> if disabled.
     */
    public LeaderFollowersThreadPool getLeaderFollowersThreadPool() {
        return leaderFollowersThreadPool;
    }

    /**
	 * Get repeater registration status.
	 * @return repeater registration status.
	 */
	public boolean isRegistrationConfirmed() {
		return registrationConfirmed;
	}

	/**
	 * Repeater registration confirmation.
	 * @param responseFrom response from address.
	 */
	public void repeaterConfirm(InetSocketAddress responseFrom)
	{
		logger.fine("Repeater " + responseFrom + " confirmed registration.");
		registrationConfirmed = true;
		
		// notify other regarding confirmation
		synchronized (registrationConfirmedCondition)
		{
		    registrationConfirmedCondition.notifyAll();
		}
	}
	
	/**
	 * Called each time beacon anomaly is detected. 
	 */
	public void beaconAnomalyNotify()
	{
		if (channelSearchManager != null)
			channelSearchManager.beaconAnomalyNotify();
	}
	
	/**
	 * Search response from server (channel found).
	 * @param cid	client channel ID.
	 * @param sid	server channel ID.
	 * @param type	channel type code.
	 * @param count	channel element count.
	 * @param minorRevision	server minor CA revision.
	 * @param serverAddress	server address.
	 */
	public void searchResponse(int cid, int sid, short type, int count,
							   short minorRevision, InetSocketAddress serverAddress)
	{
		CAJChannel channel = getChannel(cid);
		if (channel == null)
			return;

		// check for multiple responses
		synchronized (channel)
		{
			CATransport transport = channel.getTransport();
			if (transport != null)
			{
				// multiple defined PV or reconnect request (same server address)
				if (!transport.getRemoteAddress().equals(serverAddress))
				{
					logger.info("More than one PVs with name '" + channel.getName() +
								 "' detected, additional response from: " + serverAddress);
					return;
				}
			}
			
			// do not search anymore (also unregisters)
			int seqNo = lastReceivedSequenceNumber.get();
			channelSearchManager.searchResponse(channel, seqNo, seqNo != 0, System.currentTimeMillis());
			
			transport = getTransport(channel, serverAddress, minorRevision, channel.getPriority());
			if (transport == null)
			{
				channel.createChannelFailed();
				return;
			}

			// create channel
			channel.createChannel(transport, sid, type, count);
		}
			
	}

	/**
	 * Get, or create if necessary, transport of given server address.
	 * @param serverAddress	required transport address
	 * @param priority process priority.
	 * @return transport for given address
	 */
	private CATransport getTransport(TransportClient client, InetSocketAddress serverAddress, short minorRevision, short priority)
	{
		try
		{
			return (CATransport)connector.connect(client, new CAResponseHandler(this), serverAddress, minorRevision, priority);
		}
		catch (ConnectionException cex)
		{
			logger.log(Level.SEVERE, "Failed to create transport for: " + serverAddress, cex);
		}
			
		return null;
	}
   
	/**
	 * Generate Client channel ID (CID).
	 * @return Client channel ID (CID). 
	 */
	private int generateCID()
	{
		synchronized (channelsByCID)
		{
			// search first free (theoretically possible loop of death)
			while (getChannel(++lastCID) != null);
			// reserve CID
			channelsByCID.put(lastCID, null);
			return lastCID;
		}
	}

	/**
	 * Searches for a response request with given channel IOID.
	 * @param ioid	I/O ID.
	 * @return request response with given I/O ID.
	 */
	public ResponseRequest getResponseRequest(int ioid)
	{
		synchronized (pendingResponseRequests)
		{
			return (ResponseRequest)pendingResponseRequests.get(ioid);
		}
	}

	/**
	 * Register response request.
	 * @param request request to register.
	 * @return request ID (IOID).
	 */
	public int registerResponseRequest(ResponseRequest request)
	{
		synchronized (pendingResponseRequests)
		{
			int ioid = generateIOID();
			pendingResponseRequests.put(ioid, request);
			return ioid;
		}
	}

	/**
	 * Unregister response request.
	 * @param request
	 * @return removed object, can be <code>null</code>
	 */
	public ResponseRequest unregisterResponseRequest(ResponseRequest request)
	{
		synchronized (pendingResponseRequests)
		{
			return (ResponseRequest)pendingResponseRequests.remove(request.getIOID());
		}
	}

	/**
	 * Generate IOID.
	 * @return IOID. 
	 */
	private int generateIOID()
	{
		synchronized (pendingResponseRequests)
		{
			// search first free (theoretically possible loop of death)
			while (pendingResponseRequests.get(++lastIOID) != null);
			// reserve IOID
			pendingResponseRequests.put(lastIOID, null);
			return lastIOID;
		}
	}

	/**
	 * Increment pending requests counter.
	 * @return IO sequence number (pendIO sequence number).
	 */
	public int incrementPendingRequests()
	{
		synchronized (sequenceNumberIO)
		{
			pendingRequestsCount.incrementAndGet();
			return sequenceNumberIO.get();
		}
	}

	/**
	 * Decrement pending requests counter.
	 * @param usedSequenceNumberIO	IO sequence number returned by incrementPendingRequests
	 */
	public void decrementPendingRequests(int usedSequenceNumberIO)
	{
		if (usedSequenceNumberIO == sequenceNumberIO.get())
		{
			int count = pendingRequestsCount.decrementAndGet();
	
			// notify if zero
			if (count == 0)
			{
				synchronized (zeroPendingRequestsCondition)
				{
					zeroPendingRequestsCondition.notifyAll();
				}
			}
		}
	}
	
	/**
	 * Set last UDP recived sequence number.
	 * @param seqNo	last UDP recived sequence number.
	 */
	public final void setLastReceivedSequenceNumber(int seqNo)
	{
		lastReceivedSequenceNumber.set(seqNo);
	}
	
	/**
	 * Set last UDP recived sequence number.
	 * @param seqNo	last UDP recived sequence number.
	 */
	public final int getLastReceivedSequenceNumber(int seqNo)
	{
		return lastReceivedSequenceNumber.get();
	}

	/**
	 * @see com.cosylab.epics.caj.impl.CAContext#invalidateLastReceivedSequence()
	 */
	public final void invalidateLastReceivedSequence()
	{
		lastReceivedSequenceNumber.set(0);
	}

	/**
	 * Get (and if necessary create) beacon handler.
	 * @param responseFrom remote source address of received beacon.	
	 * @return beacon handler for particular server.
	 */
	public CABeaconHandler getBeaconHandler(InetSocketAddress responseFrom)
	{
		synchronized (beaconHandlers) {
			CABeaconHandler handler = (CABeaconHandler)beaconHandlers.get(responseFrom);
			if (handler == null)
			{
				handler = new CABeaconHandler(this, responseFrom);
				beaconHandlers.put(responseFrom, handler);
			}
			return handler;
		}
	}
}
