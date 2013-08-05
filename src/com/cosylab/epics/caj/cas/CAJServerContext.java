/*
 * Copyright (c) 2006 by Cosylab
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

import gov.aps.jca.CAException;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Version;
import gov.aps.jca.cas.Server;
import gov.aps.jca.cas.ServerContext;
import gov.aps.jca.configuration.Configurable;
import gov.aps.jca.configuration.Configuration;
import gov.aps.jca.configuration.ConfigurationException;
import gov.aps.jca.event.ContextExceptionListener;
import gov.aps.jca.event.ContextMessageListener;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cosylab.epics.caj.CAJConstants;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.BroadcastConnector;
import com.cosylab.epics.caj.impl.BroadcastTransport;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.CAContext;
import com.cosylab.epics.caj.impl.CATransportRegistry;
import com.cosylab.epics.caj.impl.CachedByteBufferAllocator;
import com.cosylab.epics.caj.impl.ConnectionException;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.reactor.Reactor;
import com.cosylab.epics.caj.impl.reactor.ReactorHandler;
import com.cosylab.epics.caj.impl.reactor.lf.LeaderFollowersHandler;
import com.cosylab.epics.caj.impl.reactor.lf.LeaderFollowersThreadPool;
import com.cosylab.epics.caj.util.InetAddressUtil;
import com.cosylab.epics.caj.util.Timer;
import com.cosylab.epics.caj.util.logging.ConsoleLogHandler;

/**
 * Implementation of CAJ JCA <code>Context</code>. 
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJServerContext extends ServerContext implements CAContext, Configurable {

	static
	{
		// force only IPv4 sockets, since EPICS does not work right with IPv6 sockets
		// see http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

    /**
     * Major version.
     */
    private static final int CAS_VERSION_MAJOR = 1;
    
    /**
     * Minor version.
     */
    private static final int CAS_VERSION_MINOR = 1;

    /**
     * Maintenance version.
     */
    private static final int CAS_VERSION_MAINTENANCE = 13;

    /**
     * Development version.
     */
    private static final int CAS_VERSION_DEVELOPMENT = 0;

    /**
     * Version.
     */
    public static final Version VERSION = new Version(
            "Channel Access Server in Java", "Java",
            CAS_VERSION_MAJOR, CAS_VERSION_MINOR,
            CAS_VERSION_MAINTENANCE, CAS_VERSION_DEVELOPMENT);
	  
   
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
	 * State value of running context.
	 */
	private static final int RUNNING = 2;

	/**
	 * State value of runned context.
	 */
	private static final int RUNNED = 3;

	/**
	 * State value of destroyed context.
	 */
	private static final int DESTROYED = 4;

	/**
	 * Initialization status.
	 */
	private volatile int state = NOT_INITIALIZED;
	
	/**
	 * Context logger.
	 */
	protected Logger logger = Logger.global;

	/**
	 * A space-separated list of broadcast address which to send beacons.
	 * Each address must be of the form: ip.number:port or host.name:port
	 */
	protected String beaconAddressList = "";
	
	/**
	 * A space-separated list of address from which to ignore name resolution requests.
	 * Each address must be of the form: ip.number:port or host.name:port
	 */
	protected String ignoreAddressList = "";
	
	/**
	 * Define whether or not the network interfaces should be discovered at runtime. 
	 */
	protected boolean autoBeaconAddressList = true;
	
	/**
	 * Period in second between two beacon signals.
	 */
	protected float beaconPeriod = 15.0f;
	
	/**
	 * Port number for the repeater to listen to.
	 */
	protected int beaconPort = CAConstants.CA_REPEATER_PORT;
	
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
	 * Broadcast transport needed for channel searches.
	 */
	protected BroadcastTransport broadcastTransport = null;
	
	/**
	 * Beacon emitter..
	 */
	protected BeaconEmitter beaconEmitter = null;

	/**
	 * CAS acceptor (accepts CA virtual circuit).
	 */
	protected CASAcceptor acceptor = null;

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
	 * Last SID cache. 
	 */
	private AtomicInteger lastChannelSID = new AtomicInteger(0);

	/**
	 * Server.
	 */
	protected Server server;

	/**
	 * Run lock.
	 */
	protected Object runLock = new Object();
	
	/**
	 * Last UDP recived sequence number.
	 */
	private AtomicInteger lastReceivedSequenceNumber = new AtomicInteger(0);
	
	/**
	 * Constructor.
	 */
	public CAJServerContext()
	{
		initializeLogger();
		loadConfiguration();
	}
	
    /**
     * Get context version.
     * @return context version.
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
		
		if (System.getProperties().containsKey(CAJConstants.CAJ_DEBUG))
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

		/*
		// load env var configuration (move to JCA)
		beaconAddressList = getEnvironmentVariable("EPICS_CA_ADDR_LIST", beaconAddressList);
		beaconAddressList = getEnvironmentVariable("EPICS_CAS_BEACON_ADDR_LIST", beaconAddressList);
		autoBeaconAddressList = getEnvironmentVariable("EPICS_CA_AUTO_ADDR_LIST",  autoBeaconAddressList?"YES":"NO").equalsIgnoreCase("YES");
		autoBeaconAddressList = getEnvironmentVariable("EPICS_CAS_AUTO_BEACON_ADDR_LIST",  autoBeaconAddressList?"YES":"NO").equalsIgnoreCase("YES");
		beaconPeriod = Float.parseFloat(getEnvironmentVariable("EPICS_CA_BEACON_PERIOD", String.valueOf(beaconPeriod)));
		beaconPeriod = Float.parseFloat(getEnvironmentVariable("EPICS_CAS_BEACON_PERIOD", String.valueOf(beaconPeriod)));
		beaconPort = Integer.parseInt(getEnvironmentVariable("EPICS_CA_REPEATER_PORT", String.valueOf(beaconPort)));
		beaconPort = Integer.parseInt(getEnvironmentVariable("EPICS_CAS_BEACON_PORT", String.valueOf(beaconPort)));
		serverPort = Integer.parseInt(getEnvironmentVariable("EPICS_CA_SERVER_PORT", String.valueOf(serverPort)));
		serverPort = Integer.parseInt(getEnvironmentVariable("EPICS_CAS_SERVER_PORT", String.valueOf(serverPort)));
		maxArrayBytes = Integer.parseInt(getEnvironmentVariable("EPICS_CA_MAX_ARRAY_BYTES", String.valueOf(maxArrayBytes)));
        */
		
		// load default Context configuration
		String contextClassName = Context.class.getName();
		beaconAddressList = jcaLibrary.getProperty(contextClassName + ".addr_list", beaconAddressList);
		autoBeaconAddressList = jcaLibrary.getPropertyAsBoolean(contextClassName + ".auto_addr_list",  autoBeaconAddressList);
		beaconPeriod = jcaLibrary.getPropertyAsFloat(contextClassName + ".beacon_period", beaconPeriod);
		beaconPort = jcaLibrary.getPropertyAsInt(contextClassName + ".repeater_port", beaconPort);
		serverPort = jcaLibrary.getPropertyAsInt(contextClassName + ".server_port", serverPort);
		maxArrayBytes = jcaLibrary.getPropertyAsInt(contextClassName + ".max_array_bytes", maxArrayBytes);

		// load default CAJContext configuration
		contextClassName = CAJContext.class.getName();
		beaconAddressList = jcaLibrary.getProperty(contextClassName + ".addr_list", beaconAddressList);
		autoBeaconAddressList = jcaLibrary.getPropertyAsBoolean(contextClassName + ".auto_addr_list",  autoBeaconAddressList);
		beaconPeriod = jcaLibrary.getPropertyAsFloat(contextClassName + ".beacon_period", beaconPeriod);
		beaconPort = jcaLibrary.getPropertyAsInt(contextClassName + ".repeater_port", beaconPort);
		serverPort = jcaLibrary.getPropertyAsInt(contextClassName + ".server_port", serverPort);
		maxArrayBytes = jcaLibrary.getPropertyAsInt(contextClassName + ".max_array_bytes", maxArrayBytes);

		// load CAS specific configuration (overrides default)
		final String thisClassName = this.getClass().getName();
		beaconAddressList = jcaLibrary.getProperty(thisClassName + ".beacon_addr_list", beaconAddressList);
		autoBeaconAddressList = jcaLibrary.getPropertyAsBoolean(thisClassName + ".auto_beacon_addr_list",  autoBeaconAddressList);
		beaconPeriod = jcaLibrary.getPropertyAsFloat(thisClassName + ".beacon_period", beaconPeriod);
		beaconPort = jcaLibrary.getPropertyAsInt(thisClassName + ".beacon_port", beaconPort);
		serverPort = jcaLibrary.getPropertyAsInt(thisClassName + ".server_port", serverPort);
		maxArrayBytes = jcaLibrary.getPropertyAsInt(thisClassName + ".max_array_bytes", maxArrayBytes);
		ignoreAddressList = jcaLibrary.getProperty(thisClassName + ".ignore_addr_list",  ignoreAddressList);

	}


	/**
	 * @see gov.aps.jca.configuration.Configurable#configure(gov.aps.jca.configuration.Configuration)
	 */
	public void configure(Configuration configuration)
		throws ConfigurationException {

			// address list
			try {
			  beaconAddressList = configuration.getChild("beacon_addr_list", false).getValue();
			} catch(Exception ex) {
				beaconAddressList = configuration.getAttribute("beacon_addr_list", beaconAddressList);
			}
    
			// auto address list
			try {
				autoBeaconAddressList = configuration.getChild("auto_beacon_addr_list", false).getValueAsBoolean();
			} catch(Exception ex) {
				autoBeaconAddressList = configuration.getAttributeAsBoolean("auto_beacon_addr_list", autoBeaconAddressList);
			}

    		// beacon period
			try {
				beaconPeriod = configuration.getChild("beacon_period", false).getValueAsFloat();
			} catch(Exception ex) {
				beaconPeriod = configuration.getAttributeAsFloat("beacon_period", beaconPeriod);
			}

			// beacon port    
			try {
				beaconPort = configuration.getChild("beacon_port", false).getValueAsInteger();
			} catch(Exception ex) {
				beaconPort = configuration.getAttributeAsInteger("beacon_port", beaconPort);
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

			// ignore list
			try {
			  ignoreAddressList = configuration.getChild("ignore_addr_list", false).getValue();
			} catch(Exception ex) {
				ignoreAddressList = configuration.getAttribute("ignore_addr_list", ignoreAddressList);
			}
    
	}

	/**
	 * @see gov.aps.jca.Context#getContextMessageListeners()
	 * @return array of context message listeners.
	 * @throws IllegalStateException
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
	 * @throws CAException
	 * @throws IllegalStateException
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
	 * Check context state and tries to establish necessary state.
	 * @throws CAException
	 * @throws IllegalStateException
	 */
	protected void checkState() throws CAException, IllegalStateException {
		if (state == DESTROYED)
			throw new IllegalStateException("Context destroyed.");
	}

	/**
	 * Set server and initialize.
	 * @see gov.aps.jca.cas.ServerContext#initialize(gov.aps.jca.cas.Server)
	 */
	public synchronized void initialize(Server server) throws CAException, IllegalStateException
	{
		if (server == null)
			throw new IllegalArgumentException("non null server expected");
		
		if (state == DESTROYED)
			throw new IllegalStateException("Context destroyed.");
		else if (state == INITIALIZED || state == RUNNING || state == RUNNED)
			throw new IllegalStateException("Context already initialized.");

		this.server = server;
		
		internalInitialize();
		
		state = INITIALIZED;
	}

	/**
	 * @throws CAException
	 */
	private void internalInitialize() throws CAException {

		timer = new Timer();
		transportRegistry = new CATransportRegistry();

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
				        	
				        }, "CAS reactor").start();
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
		
		beaconEmitter = new BeaconEmitter(broadcastTransport, this, beaconPeriod);

		acceptor = new CASAcceptor(this, serverPort);
	}

	/**
	 * Initialized UDP transport (broadcast socket and repeater connection).
	 */
	private void initializeUDPTransport() {
		
		// setup UDP transport
		try
		{
			InetSocketAddress listenLocalAddress = new InetSocketAddress(serverPort);
		
			BroadcastConnector broadcastConnector = new BroadcastConnector(this);
			
			broadcastTransport = (BroadcastTransport)broadcastConnector.connect(
										null, new CASResponseHandler(this),
										listenLocalAddress, CAConstants.CA_MINOR_PROTOCOL_REVISION,
										CAConstants.CA_DEFAULT_PRIORITY);

			// moved from BroadcastConnector due to JDK7 problem
			ReactorHandler handler = broadcastTransport;
			if (getLeaderFollowersThreadPool() != null)
			    handler = new LeaderFollowersHandler(getReactor(), handler, getLeaderFollowersThreadPool());
			try {
				DatagramChannel channel = broadcastTransport.getChannel();
				
				broadcastTransport.bind(true);
				
				// and register to the selector
				getReactor().register(channel, SelectionKey.OP_READ, handler);
			} catch (Throwable e) {
				// TODO
				throw new RuntimeException(e);
			}

			/*
			// bind UDP socket
			try {
				broadcastTransport.bind(true);
			} catch (SocketException se) {
				logger.log(Level.WARNING, "Failed to bind UDP socket to: " + listenLocalAddress, se);
			}
			*/
			
			// set ignore address list
			if (ignoreAddressList != null && ignoreAddressList.length() > 0)
			{
				// we do not care about the port
				InetSocketAddress[] list = InetAddressUtil.getSocketAddressList(ignoreAddressList, 0);
				if (list != null && list.length > 0)
					broadcastTransport.setIgnoredAddresses(list);
			}
			// set broadcast address list
			if (beaconAddressList != null && beaconAddressList.length() > 0)
			{
				// if auto is true, add it to specified list
				InetSocketAddress[] appendList = null;
				if (autoBeaconAddressList == true)
					appendList = broadcastTransport.getBroadcastAddresses();
				
				InetSocketAddress[] list = InetAddressUtil.getSocketAddressList(beaconAddressList, beaconPort, appendList);
				if (list != null && list.length > 0)
					broadcastTransport.setBroadcastAddresses(list);
			}

		}
		catch (ConnectionException ce)
		{
			logger.log(Level.SEVERE, "Failed to initialize UDP transport.", ce);
		}

	}
	
	private boolean runTerminated;
	 
	/**
	 * Run server (process events).
	 * @param	seconds	time in seconds the server will process events (method will block), if <code>0</code>
	 * 				the method would block until <code>destory()</code> is called.
	 * @throws IllegalStateException	if server is already destroyed.
	 * @throws CAException
	 */
	public void run(int seconds) throws CAException, IllegalStateException
	{
		if (seconds < 0)
			throw new IllegalArgumentException("seconds cannot be negative.");
		
		if (state == NOT_INITIALIZED)
			throw new IllegalStateException("Context not initialized.");
		else if (state == DESTROYED)
			throw new IllegalStateException("Context destroyed.");
		else if (state == RUNNING)
			throw new IllegalStateException("Context is already running.");
		else if (state == RUNNED)
			throw new IllegalStateException("Context has already completed running.");
		
		synchronized (this)
		{
			if (state == RUNNED)
				throw new IllegalStateException("Context has already completed running.");

			state = RUNNING;
		}
		
		// run...
		beaconEmitter.start();
		synchronized (runLock)
		{
			runTerminated = false;
			try {
				final long timeToWait = seconds * 1000;
				final long start = System.currentTimeMillis();
				while (!runTerminated && (timeToWait == 0 || ((System.currentTimeMillis() - start) < timeToWait)))
					runLock.wait(timeToWait);
			} catch (InterruptedException e) { /* noop */ }
		}
		
		synchronized (this)
		{
			state = RUNNED;
		}
		
	}


	/**
	 * @see gov.aps.jca.cas.ServerContext#shutdown()
	 */
	public synchronized void shutdown() throws CAException, IllegalStateException {

		if (state == DESTROYED)
			throw new IllegalStateException("Context already destroyed.");

		// notify to stop running...
		synchronized (runLock)
		{
			runTerminated = true;
			runLock.notifyAll();
		}
	}

	/**
	 * @see gov.aps.jca.Context#destroy()
	 */
	public synchronized void destroy() throws CAException, IllegalStateException {

		if (state == DESTROYED)
			throw new IllegalStateException("Context already destroyed.");

		// shutdown if not already
		shutdown();
		
		// go into destroyed state ASAP			
		state = DESTROYED;
		
		internalDestroy();
				
	}

	/**
	 * @throws CAException
	 */
	private void internalDestroy() throws CAException {

			// stop accepting connections
		if (acceptor != null) 
			acceptor.destroy();

			// stop emitting beacons
		if (beaconEmitter != null) 
			beaconEmitter.destroy();

		// stop timer
		if (timer != null) 
			timer.shutDown();

		//
		// cleanup
		//
		
		// stop responding to search requests
		if (broadcastTransport != null) 
			broadcastTransport.close();
		
		// this will also destroy all channels
		destroyAllTransports();
		
		// shutdown reactor
		if (reactor != null)
			reactor.shutdown();
		
		// shutdown LF thread pool
		if (leaderFollowersThreadPool != null)
		    leaderFollowersThreadPool.shutdown();
		
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
	 * Destroy all transports.
	 */
	private void destroyAllTransports() {

		// not initialized yet
		if (transportRegistry == null)
			return;
		
		Transport[] transports = transportRegistry.toArray();
		
		if (transports.length == 0)
			return;
		
		logger.fine("Server context still has " + transports.length + " transport(s) active and closing...");

		for (int i = 0; i < transports.length; i++)
		{
			CASTransport transport = (CASTransport)transports[i];
			try
			{
				transport.close(true);
			} catch (Throwable th) {
				// do all exception safe, print stack in case of an error
				logger.log(Level.SEVERE, "", th);
			}
		}
	}
	
	/**
	 * @see gov.aps.jca.Context#printInfo(java.io.PrintStream)
	 */
	public void printInfo(PrintStream out) throws IllegalStateException {
		super.printInfo(out);
		out.println("SERVER : " + (server != null ? server.getClass().getName() : null));
		out.println("BEACON_ADDR_LIST : " + beaconAddressList);
		out.println("AUTO_BEACON_ADDR_LIST : " + autoBeaconAddressList);
		out.println("BEACON_PERIOD : " + beaconPeriod);
		out.println("BEACON_PORT : " + beaconPort);
		out.println("SERVER_PORT : " + serverPort);
		out.println("MAX_ARRAY_BYTES : " + maxArrayBytes);
		out.println("IGNORE_ADDR_LIST: " + ignoreAddressList);
		out.print("STATE : ");
		switch (state)
		{
			case NOT_INITIALIZED:
				out.println("NOT_INITIALIZED");
				break;
			case INITIALIZED:
				out.println("INITIALIZED");
				break;
			case RUNNING:
				out.println("RUNNING");
				break;
			case RUNNED:
				out.println("RUNNED");
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
		return state == INITIALIZED || state == RUNNED || state == RUNNING;
	}

	/**
	 * Get destruction status.
	 * @return destruction status.
	 */
	public boolean isDestroyed() {
		return state == DESTROYED;
	}
	
	/**
	 * Get beacon address list.
	 * @return beacon address list.
	 */
	public String getBeaconAddressList() {
		return beaconAddressList;
	}

	/**
	 * Get beacon address list auto flag.
	 * @return beacon address list auto flag.
	 */
	public boolean isAutoBeaconAddressList() {
		return autoBeaconAddressList;
	}

	/**
	 * Get beacon period (in seconds).
	 * @return beacon period (in seconds).
	 */
	public float getBeaconPeriod() {
		return beaconPeriod;
	}

	/**
	 * Get logger.
	 * @return logger.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Get max array payload size.
	 * @return max array payload size.
	 */
	public int getMaxArrayBytes() {
		return maxArrayBytes;
	}

	/**
	 * Get beacon port.
	 * @return beacon port.
	 */
	public int getBeaconPort() {
		return beaconPort;
	}

	/**
	 * Get server port.
	 * @return server port.
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * Set server port number.
	 * @param port new server port number.
	 */
	public void setServerPort(int port) {
		serverPort = port;
	}

	/**
	 * Get broadcast port.
	 * @return broadcast port.
	 */
	public int getBroadcastPort() {
		return getBeaconPort();
	}

	/**
	 * Get ignore search address list.
	 * @return ignore search addrresr list.
	 */
	public String getIgnoreAddressList() {
		return ignoreAddressList;
	}

	// ************************************************************************** //

	/**
	 * Get server newtwork (IP) address.
	 * @return server network (IP) address, <code>null</code> if not bounded. 
	 */
	public InetAddress getServerInetAddress() {
		return (acceptor != null) ? 
				acceptor.getBindAddress() : null;
	}

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
	 * Get server implementation.
	 * @return server implementation.
	 */
	public Server getServer() {
		return server;
	}

	/**
	 * Generate channel SID.
	 * @return channel SID.
	 */
	public int generateChannelSID()
	{
		return lastChannelSID.incrementAndGet();
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
}
