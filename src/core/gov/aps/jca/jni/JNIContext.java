/**********************************************************************
 *
 *      Original Author: Eric Boucher
 *      Date:            05/05/2003
 *
 *      Experimental Physics and Industrial Control System (EPICS)
 *
 *      Copyright 1991, the University of Chicago Board of Governors.
 *
 *      This software was produced under  U.S. Government contract
 *      W-31-109-ENG-38 at Argonne National Laboratory.
 *
 *      Beamline Controls & Data Acquisition Group
 *      Experimental Facilities Division
 *      Advanced Photon Source
 *      Argonne National Laboratory
 *
 *
 * $Id: JNIContext.java,v 1.6 2006-11-03 11:01:47 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */


package gov.aps.jca.jni;

import gov.aps.jca.*;
import gov.aps.jca.dbr.*;
import gov.aps.jca.event.*;

import java.util.*;
import java.lang.ref.WeakReference;
import java.util.logging.*;



abstract public class JNIContext extends Context {

  protected boolean _initialized=false;
  protected boolean _preemptive_callback=true;
  protected String _addr_list="";
  protected boolean _auto_addr_list=true;
  protected float _connection_timeout=30.0f;
  protected float _beacon_period=15.0f;
  protected int _repeater_port=5065;
  protected int _server_port=5064;
  protected int _max_array_bytes=16384;
  protected EventDispatcher _eventDispatcher= new DirectEventDispatcher();

  protected Logger _logger=null;
  
  protected long _ctxtID=0;

  protected boolean _jca_use_env=false;
  
  protected JNIContext() {
    JCALibrary jca=JCALibrary.getInstance();
    
    String ed;
    String cn=gov.aps.jca.jni.JNIContext.class.getName();
    if (Boolean.getBoolean("jca.use_env") || jca.getPropertyAsBoolean(cn+".jca.use_env", getJcaUseEnv()))
    {
    	// Context default configuration
        String dcn=gov.aps.jca.Context.class.getName();
    	setPreemptiveCallback(jca.getPropertyAsBoolean( dcn+ ".preemptive_callback", getPreemptiveCallback() ));
    	ed = jca.getProperty( dcn+".event_dispatcher", "" );

        // JNIContext specific config (overrides default)
        setPreemptiveCallback(jca.getPropertyAsBoolean( cn+ ".preemptive_callback", getPreemptiveCallback() ));
        ed = jca.getProperty( cn+".event_dispatcher", ed );
    	
        String tmp = System.getenv("EPICS_CA_ADDR_LIST");
        if (tmp != null) setAddrList(tmp);
        
    	tmp = System.getenv("EPICS_CA_AUTO_ADDR_LIST");
    	if (tmp != null)
    		setAutoAddrList(!tmp.equalsIgnoreCase("NO"));
    	else
    		setAutoAddrList(true);
    	
    	tmp = System.getenv("EPICS_CA_CONN_TMO");
    	if (tmp != null) setConnectionTimeout(Float.parseFloat(tmp));
    	
    	tmp = System.getenv("EPICS_CA_BEACON_PERIOD");
       	if (tmp != null) setBeaconPeriod(Float.parseFloat(tmp));
           	
    	tmp = System.getenv("EPICS_CA_REPEATER_PORT");
    	if (tmp != null) setRepeaterPort(Integer.parseInt(tmp));
    	
    	tmp = System.getenv("EPICS_CA_SERVER_PORT");
    	if (tmp != null) setServerPort(Integer.parseInt(tmp));

    	tmp = System.getenv("EPICS_CA_MAX_ARRAY_BYTES");
    	if (tmp != null) setMaxArrayBytes(Integer.parseInt(tmp));
    }
    else
    {
		// Context default configuration
	    String dcn=gov.aps.jca.Context.class.getName();
	    setPreemptiveCallback(jca.getPropertyAsBoolean( dcn+ ".preemptive_callback", getPreemptiveCallback() ));
	    setAddrList(jca.getProperty( dcn+".addr_list", getAddrList() ));
	    setAutoAddrList(jca.getPropertyAsBoolean( dcn+".auto_addr_list",  getAutoAddrList() ));
	    setConnectionTimeout(jca.getPropertyAsFloat( dcn+".connection_timeout", getConnectionTimeout() ));
	    setBeaconPeriod(jca.getPropertyAsFloat( dcn+".beacon_period", getBeaconPeriod() ));
	    setRepeaterPort(jca.getPropertyAsInt( dcn+".repeater_port", getRepeaterPort() ));
	    setServerPort(jca.getPropertyAsInt( dcn+".server_port", getServerPort() ));
	    setMaxArrayBytes(jca.getPropertyAsInt( dcn+".max_array_bytes", getMaxArrayBytes() ));
	    ed = jca.getProperty( dcn+".event_dispatcher", "" );
	    
	    // JNIContext specific config (overrides default)
	    setPreemptiveCallback(jca.getPropertyAsBoolean( cn+ ".preemptive_callback", getPreemptiveCallback() ));
	    setAddrList(jca.getProperty( cn+".addr_list", getAddrList() ));
	    setAutoAddrList(jca.getPropertyAsBoolean( cn+".auto_addr_list",  getAutoAddrList() ));
	    setConnectionTimeout(jca.getPropertyAsFloat( cn+".connection_timeout", getConnectionTimeout() ));
	    setBeaconPeriod(jca.getPropertyAsFloat( cn+".beacon_period", getBeaconPeriod() ));
	    setRepeaterPort(jca.getPropertyAsInt( cn+".repeater_port", getRepeaterPort() ));
	    setServerPort(jca.getPropertyAsInt( cn+".server_port", getServerPort() ));
	    setMaxArrayBytes(jca.getPropertyAsInt( cn+".max_array_bytes", getMaxArrayBytes() ));
	    ed = jca.getProperty( cn+".event_dispatcher", ed );
    }
    
    try {
      setEventDispatcher( ( EventDispatcher )Class.forName( ed ).newInstance());
    } catch( ClassNotFoundException ex1 ) {
//      System.out.println(ex1);
    } catch( IllegalAccessException ex1 ) {
//      System.out.println(ex1);
    } catch( InstantiationException ex1 ) {
//      System.out.println(ex1);
    }

    String logger= jca.getProperty( cn+".logger", null);
    setLogger(logger==null?getLogger():Logger.getLogger(logger));

  }


  /** 
   * Initialize the context.
   *
   * @throws IllegalStateException if the context has already been initialized or has been destroyed.
   */  
  public void initialize() throws CAException {
    assertState(!isInitialized(), "Context Already Initialized");
    
    try {
      JNI.init();

      setCtxtID( contextInitialize() );

      _initialized=true;

      addContextMessageListener( new DefaultContextMessageListener() );
      addContextExceptionListener( new DefaultContextExceptionListener() );

    } catch( Throwable th ) {
//      if(getLogger()!=null) getLogger().log(Level.SEVERE, "Unable to initialize context", th);
      throw new CAException( "Unable to initialize context", th );
    } 
  }

  
  /** 
   * Test whether the context has been initialized.
   * @return true if the context is initialized, false otherwise.
   * @throws IllegalStateException if the context has been destroyed.
   */  
  final public boolean isInitialized() throws IllegalStateException {
//    assertState(isValid(),  "Context has been destroyed");
    return _initialized;
  }


  
  /* Silently initialize the context
   * @throws IllegalStateException if the context could not be initialized.
   */
  protected void _initialize() throws IllegalStateException {
    if( _initialized ) {
      return;
    }
    try {
      initialize();
    } catch( Throwable th ) {
      throw new IllegalStateException( "Context not initialized", th );
    }
  }

  /**
   * Devived class should implement this method to perform context initialization.
   */
  abstract protected long contextInitialize() throws CAException;

  
  
  public void printInfo( java.io.PrintStream out ) {
    super.printInfo( out );
    out.println( "PREEMPTIVE_CALLBACK : "+getPreemptiveCallback() );
    out.println( "ADDR_LIST : "+getAddrList() );
    out.println( "AUTO_ADDR_LIST : "+getAutoAddrList() );
    out.println( "CONNECTION_TIMEOUT : "+getConnectionTimeout() );
    out.println( "BEACON_PERIOD : "+getBeaconPeriod() );
    out.println( "REPEATER_PORT : "+getRepeaterPort() );
    out.println( "SERVER_PORT : "+getServerPort() );
    out.println( "MAX_ARRAY_BYTES : "+getMaxArrayBytes() );
    out.println( "EVENT_DISPATCHER: "+getEventDispatcher() );
  }
  
  protected void setPreemptiveCallback( boolean enable ) {
    _preemptive_callback=enable;
  }

  public boolean getPreemptiveCallback() {
    return _preemptive_callback;
  }

  protected void setAddrList( String list ) {
    _addr_list=list;
  }

  public String getAddrList() {
    return _addr_list;
  }

  protected void setAutoAddrList( boolean auto ) {
    _auto_addr_list=auto;
  }

  public boolean getAutoAddrList() {
    return _auto_addr_list;
  }

  protected void setConnectionTimeout( float timeout ) {
    _connection_timeout=timeout;
  }

  public float getConnectionTimeout() {
    return _connection_timeout;
  }

  protected void setBeaconPeriod( float period ) {
    _beacon_period=period;
  }

  public float getBeaconPeriod() {
    return _beacon_period;
  }

  protected void setRepeaterPort( int port ) {
    _repeater_port=port;
  }

  public int getRepeaterPort() {
    return _repeater_port;
  }

  protected void setServerPort( int port ) {
    _server_port=port;
  }

  public int getServerPort() {
    return _server_port;
  }

  protected void setMaxArrayBytes( int max ) {
    _max_array_bytes=max;
  }

  public int getMaxArrayBytes() {
    return _max_array_bytes;
  }
  
  public boolean getJcaUseEnv() {
    return _jca_use_env;
  }
 
  protected void setEventDispatcher(EventDispatcher eventDispatcher) {
    if(eventDispatcher==null) throw new IllegalArgumentException("EventDispatcher cannot be null");
    _eventDispatcher= eventDispatcher;
  }

  public EventDispatcher getEventDispatcher() {
    return _eventDispatcher;
  }

  protected void setLogger(Logger logger) {
    _logger=logger;
  }

  public Logger getLogger() {
    return _logger;
  }


  
  
  
  protected ArrayList _msgListenerList= new ArrayList();

  public void addContextMessageListener(ContextMessageListener l) throws CAException, IllegalStateException  {
    _initialize();
    assertState(isValid(), "Invalid context");

    if(l!=null) _msgListenerList.add(l);

    if(_msgListenerList.size()==1) setMessageCallback( new JNIContextMessageCallback(this, _eventDispatcher, _msgListenerList));
  }

  public void removeContextMessageListener(ContextMessageListener l) throws CAException, IllegalStateException  {
    _initialize();
    assertState(isValid(), "Invalid context");

    if(l!=null) _msgListenerList.remove(l);

    if(_msgListenerList.size()==0) setMessageCallback(null);
  }

  public ContextMessageListener[] getContextMessageListeners() {
    return (ContextMessageListener[]) _msgListenerList.toArray(new ContextMessageListener[0]);
  }


  abstract void setMessageCallback(JNIContextMessageCallback callback) throws CAException;



  protected ArrayList _exceptionListenerList= new ArrayList();


  public void addContextExceptionListener(ContextExceptionListener l) throws CAException, IllegalStateException {
    _initialize();
    assertState(isValid(), "Invalid context");
    if(l!=null) _exceptionListenerList.add(l);

    if(_exceptionListenerList.size()==1) setExceptionCallback(new JNIContextExceptionCallback(this, _eventDispatcher, _exceptionListenerList));
  }

  public void removeContextExceptionListener(ContextExceptionListener l) throws CAException {
    _initialize();
    assertState(isValid(), "Invalid context");
    if(l!=null) _exceptionListenerList.remove(l);

    if(_exceptionListenerList.size()==0) setExceptionCallback(null);
  }

  public ContextExceptionListener[] getContextExceptionListeners() {
    return (ContextExceptionListener[]) _exceptionListenerList.toArray(new ContextExceptionListener[0]);
  }


  abstract void setExceptionCallback(JNIContextExceptionCallback callback) throws CAException;




  void setCtxtID(long id) {
    _ctxtID=id;
  }

  final long getCtxtID() {
    return _ctxtID;
  }

  final boolean isValid() {
    return _ctxtID!=0;
  }

  final protected void assertState(boolean cond, String msg) {
    if(!cond) throw new IllegalStateException(msg);
  }
  
  HashMap _channelMap= new HashMap();

  public Channel[] getChannels() {
    WeakReference[] refs= (WeakReference[]) _channelMap.values().toArray(new WeakReference[0]);
    ArrayList channels= new ArrayList(refs.length);
    for(int t=0; t<refs.length; ++t) {
      Channel ch= (Channel) refs[t].get();
      if(ch!=null) channels.add(ch);
    }
    return (Channel[]) channels.toArray(new Channel[0]);
  }
  
  

  void registerChannel(JNIChannel ch) {
    if(ch==null) return;
    _channelMap.put(new Long(ch.getChannelID()), new WeakReference(ch));
  }

  JNIChannel lookupChannel(long channelID) {
    WeakReference ref= (WeakReference) _channelMap.get(new Long(channelID));
    if(ref==null) return null;
    return (JNIChannel) ref.get();
  }

  void unregisterChannel(JNIChannel ch) {
    if(ch==null) return;
    _channelMap.remove(new Long(ch.getChannelID()));
  }

  public void destroy() throws CAException {
    WeakReference[] channelRefs= (WeakReference[])_channelMap.values().toArray(new WeakReference[0]);
    for(int t=0; t<channelRefs.length; ++t) {
      Channel ch= (Channel)channelRefs[t].get();
      if(ch!=null) {
        try {
          ch.destroy();
        } catch(Exception ex) {
        }
      }
    }

    // dispose event dispatcher
    if (_eventDispatcher != null)
    	_eventDispatcher.dispose();

  }



  abstract void ch_channelDestroy(JNIChannel ch) throws JNIException;

  abstract void ch_setConnectionCallback(JNIChannel ch, JNIConnectionCallback cb) throws JNIException;

  abstract void ch_setAccessRightsCallback(JNIChannel ch, JNIAccessRightsCallback cb) throws JNIException;

  abstract int ch_getFieldType(JNIChannel ch);

  abstract int ch_getElementCount(JNIChannel ch);

  abstract int ch_getState(JNIChannel ch);

  abstract String ch_getHostName(JNIChannel ch);

  abstract boolean ch_getReadAccess(JNIChannel ch);

  abstract boolean ch_getWriteAccess(JNIChannel ch);

  abstract void ch_arrayPut(DBRType type, int count, JNIChannel ch, Object value) throws JNIException;

  abstract void ch_arrayPut(DBRType type, int count, JNIChannel ch, Object value, PutListener l) throws JNIException;

  abstract void ch_arrayGet(DBR dbr, JNIChannel ch) throws JNIException;

  abstract void ch_arrayGet(DBRType type, int count, JNIChannel ch, GetListener l) throws JNIException;

  abstract JNIMonitor ch_addMonitor(DBRType type, int count, JNIChannel ch, MonitorListener l, int mask) throws JNIException;

  abstract void ch_clearMonitor(JNIMonitor monitor) throws JNIException;
  
  
}



