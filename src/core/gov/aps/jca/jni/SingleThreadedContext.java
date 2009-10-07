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
 * $Id: SingleThreadedContext.java,v 1.6 2008-10-27 09:40:16 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.jni;

import gov.aps.jca.*;
import gov.aps.jca.dbr.*;
import gov.aps.jca.event.*;
import gov.aps.jca.configuration.*;

import java.util.*;
import java.util.logging.Logger;

final public class SingleThreadedContext extends JNIContext implements Configurable {
  
  protected ArrayList _threadList=new ArrayList();
  
  public static final Version VERSION = new Version("JNI/SingleThreadedContext Channel Access", "Java w/ JNI using native C++ code",
          JCALibrary.getInstance().getVersion(), JCALibrary.getInstance().getRevision(),
          JCALibrary.getInstance().getModification(), 0);
  
  /**
   * @see Context#getVersion()
   */
  public Version getVersion() {
      return VERSION;
  }
  
  public SingleThreadedContext() {
    String cn=gov.aps.jca.jni.SingleThreadedContext.class.getName();
    JCALibrary jca=JCALibrary.getInstance();
    
	String logger= jca.getProperty( cn+".logger", null);
	setLogger(logger==null?getLogger():Logger.getLogger(logger));
	
    if (Boolean.getBoolean(System.getProperty("jca.use_env")))
    {
	    setPreemptiveCallback(jca.getPropertyAsBoolean( cn+ ".preemptive_callback", getPreemptiveCallback() ));
    }
    else
    {
	    setPreemptiveCallback(jca.getPropertyAsBoolean( cn+ ".preemptive_callback", getPreemptiveCallback() ));
	    setAddrList(jca.getProperty( cn+".addr_list", getAddrList() ));
	    setAutoAddrList(jca.getPropertyAsBoolean( cn+".auto_addr_list",  getAutoAddrList() ));
	    setConnectionTimeout(jca.getPropertyAsFloat( cn+".connection_timeout", getConnectionTimeout() ));
	    setBeaconPeriod(jca.getPropertyAsFloat( cn+".beacon_period", getBeaconPeriod() ));
	    setRepeaterPort(jca.getPropertyAsInt( cn+".repeater_port", getRepeaterPort() ));
	    setServerPort(jca.getPropertyAsInt( cn+".server_port", getServerPort() ));
	    setMaxArrayBytes(jca.getPropertyAsInt( cn+".max_array_bytes", getMaxArrayBytes() ));
    }
    
    try {
      EventDispatcher ed= ( EventDispatcher )Class.forName( jca.getProperty( cn+".event_dispatcher", "" ) ).newInstance();
      setEventDispatcher(ed);
    } catch( ClassNotFoundException ex1 ) {
    } catch( IllegalAccessException ex1 ) {
    } catch( InstantiationException ex1 ) {
    }
  }
  
  public void configure(Configuration conf) throws ConfigurationException {
    
    boolean preemptive_callback= getPreemptiveCallback();
    try {
      preemptive_callback= conf.getChild("preemptive_callback", false).getValueAsBoolean();
    } catch(Exception ex) {
      preemptive_callback= conf.getAttributeAsBoolean("preemptive_callback", preemptive_callback);
    }
    setPreemptiveCallback(preemptive_callback);
    
    String addr_list= getAddrList();
    try {
      addr_list= conf.getChild("addr_list", false).getValue();
    } catch(Exception ex) {
      addr_list= conf.getAttribute("addr_list", addr_list);
    }
    setAddrList(addr_list);
    
    boolean auto_addr_list= getAutoAddrList();
    try {
      auto_addr_list= conf.getChild("auto_addr_list", false).getValueAsBoolean();
    } catch(Exception ex) {
      auto_addr_list= conf.getAttributeAsBoolean("auto_addr_list", auto_addr_list);
    }
    setAutoAddrList(auto_addr_list);
    
    
    float connection_timeout= getConnectionTimeout();
    try {
      connection_timeout= conf.getChild("connection_timeout", false).getValueAsFloat();
    } catch(Exception ex) {
      connection_timeout= conf.getAttributeAsFloat("connection_timeout", connection_timeout);
    }
    setConnectionTimeout(connection_timeout);
    
    
    float beacon_period= getBeaconPeriod();
    try {
      beacon_period= conf.getChild("beacon_period", false).getValueAsFloat();
    } catch(Exception ex) {
      beacon_period= conf.getAttributeAsFloat("beacon_period", beacon_period);
    }
    setBeaconPeriod(beacon_period);
    
    int repeater_port= getRepeaterPort();
    try {
      repeater_port= conf.getChild("repeater_port", false).getValueAsInteger();
    } catch(Exception ex) {
      repeater_port= conf.getAttributeAsInteger("repeater_port", repeater_port);
    }
    setRepeaterPort(repeater_port);
    
    int server_port= getServerPort();
    try {
      server_port= conf.getChild("server_port", false).getValueAsInteger();
    } catch(Exception ex) {
      server_port= conf.getAttributeAsInteger("server_port", server_port);
    }
    setServerPort(server_port);
    
    int max_array_bytes= getMaxArrayBytes();
    try {
      max_array_bytes= conf.getChild("max_array_bytes", false).getValueAsInteger();
    } catch(Exception ex) {
      max_array_bytes= conf.getAttributeAsInteger("max_array_bytes", max_array_bytes);
    }
    setMaxArrayBytes(max_array_bytes);
    
    
    
    try {
      Configuration evConf= conf.getChild("event_dispatcher", false);
      if(evConf==null) return;
      String evClass= evConf.getAttribute("class");
      EventDispatcher event_dispatcher= (EventDispatcher) Class.forName(evClass).newInstance();
      if(event_dispatcher instanceof Configurable) {
        ((Configurable)event_dispatcher).configure(evConf);
      }
      setEventDispatcher(event_dispatcher);
    } catch(ConfigurationException ex) {
      throw ex;
    } catch(Exception ex) {
      throw new ConfigurationException("Unable to configure context", ex);
    }
  }
  
  
  protected long contextInitialize() throws CAException, IllegalStateException {
    assertState(!isInitialized(), "Context already initialized");
    
    _threadList.add( Thread.currentThread() );
    
      synchronized( JNI.lock() ) {
        try {
          JNI.setenv( "EPICS_CA_ADDR_LIST", _addr_list );
          JNI.setenv( "EPICS_CA_AUTO_ADDR_LIST", ( _auto_addr_list?"YES":"NO" ) );
          JNI.setenv( "EPICS_CA_CONN_TMO", ""+_connection_timeout );
          JNI.setenv( "EPICS_CA_BEACON_PERIOD", ""+_beacon_period );
          JNI.setenv( "EPICS_CA_REPEATER_PORT", ""+_repeater_port );
          JNI.setenv( "EPICS_CA_SERVER_PORT", ""+_server_port );
          JNI.setenv( "EPICS_CA_MAX_ARRAY_BYTES", ""+_max_array_bytes );
          
          return JNI.ctxt_contextCreate( _preemptive_callback );
        } catch(JNIException ex) {
          throw new CAException("Unable to initialize context", ex);
        }
      }
  }
  
  public void destroy() throws CAException, IllegalStateException {
    if(getCtxtID()==0) return;
    assertThread();
      super.destroy();
      try {
        JNI.ctxt_contextDestroy();
      } catch(JNIException jniex) {
        throw new CAException("Failed to destroy context", jniex);
      } finally {
        setCtxtID(0);
      }
  }
  
  void setMessageCallback( JNIContextMessageCallback callback ) throws CAException, IllegalStateException {
    _initialize();
    assertState(isValid(), "Invalid context");
    assertThread();
      try {
        JNI.ctxt_setMessageCallback( callback );
      } catch(JNIException jniex) {
        throw new CAException("Failed to set message callback handler", jniex);
      }
  }
  
  void setExceptionCallback( JNIContextExceptionCallback callback ) throws CAException, IllegalStateException {
    _initialize();
    assertState(isValid(), "Invalid context");
    assertThread();
      try {
        JNI.ctxt_setExceptionCallback( callback );
      } catch(JNIException jniex) {
        throw new CAException("Failed to set context exception's handler", jniex);
      }
  }
  
  public void pendIO( double timeout ) throws TimeoutException, CAException, IllegalStateException {
    _initialize();
    assertState(isValid(), "Invalid context");
    assertThread();
      synchronized( _GetQueue ) {
        try {
          JNI.ctxt_pendIO( timeout );
        } catch( JNIException ex ) {
          if(ex.getStatus()==CAStatus.TIMEOUT) throw new TimeoutException("pendIO timed out", ex);
          else throw new CAException("pendIO failed", ex);
        } finally {
          clearGetQueue();
        }
      }
  }
  
  public boolean testIO() throws CAException, IllegalStateException {
    _initialize();
    assertState(isValid(), "Invalid context");
    assertThread();
      try {
        return JNI.ctxt_testIO();
      } catch(JNIException ex) {
        throw new CAException("testIO failed", ex);
      }
  }
  
  public void pendEvent( double time ) throws CAException, IllegalStateException {
    _initialize();
    assertState(isValid(), "Invalid context");
    assertThread();
      synchronized( _GetQueue ) {
        try {
          JNI.ctxt_pendEvent( time );
        } catch( JNIException ex ) {
          throw new CAException("pendEvent failed", ex);
        } finally {
          clearGetQueue();
        }
      }
  }
  
  public void poll() throws CAException {
    _initialize();
    assertState(isValid(), "Invalid context");
    assertThread();
      try {
        JNI.ctxt_poll();
      } catch(JNIException ex) {
        throw new CAException("poll failed", ex);
      }
  }
  
  public void flushIO() throws CAException {
    _initialize();
    assertState(isValid(), "Invalid context");
    assertThread();
      try {
        JNI.ctxt_flushIO();
      } catch(JNIException ex) {
        throw new CAException("flushIO failed", ex);
      }
  }
  
  public void attachCurrentThread() throws IllegalStateException {
    _initialize();
    assertState(isValid(), "Invalid context");
      if( _threadList.contains( Thread.currentThread() ) ) {
        return;
      }
      if( !_preemptive_callback ) {
        throw new IllegalStateException("Unable to attach thread. Preemptive callback is disabled");
      }
      try {
        JNI.ctxt_attachThread( _ctxtID );
      } catch (JNIException ex) {
      }
      synchronized( _threadList ) {
        _threadList.add( Thread.currentThread() );
      }
  }
  
  public Channel createChannel( String name, ConnectionListener l, short priority ) throws CAException, IllegalStateException {
    _initialize();
    assertState(isValid(), "Invalid context");
    if( name==null|| ( name=name.trim() ).equals( "" ) ) {
      throw new IllegalArgumentException( "Channel's name is null or empty" );
    }
    if(priority<Channel.PRIORITY_MIN || priority>Channel.PRIORITY_MAX) throw new IllegalArgumentException("Priority out of bounds");
    assertThread();
      JNIChannel channel=new JNIChannel( this, name, l, priority );
      JNIConnectionCallback callback=channel.getConnectionCallback();
      
      synchronized( JNI.lock() ) {
        JNI.setenv( "EPICS_CA_ADDR_LIST", _addr_list );
        JNI.setenv( "EPICS_CA_AUTO_ADDR_LIST", ( _auto_addr_list?"YES":"NO" ) );
        JNI.setenv( "EPICS_CA_CONN_TMO", ""+_connection_timeout );
        JNI.setenv( "EPICS_CA_BEACON_PERIOD", ""+_beacon_period );
        JNI.setenv( "EPICS_CA_REPEATER_PORT", ""+_repeater_port );
        JNI.setenv( "EPICS_CA_SERVER_PORT", ""+_server_port );
        JNI.setenv( "EPICS_CA_MAX_ARRAY_BYTES", ""+_max_array_bytes );
        
        try {
          channel.setChannelID( JNI.ch_channelCreate( name, callback, priority ) );
        } catch(JNIException ex) {
          throw new CAException("createChannel failed", ex);
        }
      }
      registerChannel(channel);
      return channel;
  }
  
  void ch_channelDestroy( JNIChannel ch ) throws JNIException {
    assertState(isValid(), "Invalid context");
    assertThread();
      JNI.ch_channelDestroy( ch.getChannelID() );
  }
  
  void ch_setConnectionCallback( JNIChannel ch, JNIConnectionCallback cb ) throws JNIException {
    assertState(isValid(), "Invalid context");
    assertThread();
      JNI.ch_setConnectionCallback( ch.getChannelID(), cb );
  }
  
  void ch_setAccessRightsCallback( JNIChannel ch, JNIAccessRightsCallback cb ) throws JNIException {
    assertState(isValid(), "Invalid context");
    assertThread();
      JNI.ch_setAccessRightsCallback( ch.getChannelID(), cb );
  }
  
  int ch_getFieldType( JNIChannel ch ) {
    assertState(isValid(), "Invalid context");
    assertThread();
      return JNI.ch_getFieldType( ch.getChannelID() );
  }
  
  int ch_getElementCount( JNIChannel ch ) {
    assertState(isValid(), "Invalid context");
    assertThread();
      return JNI.ch_getElementCount( ch.getChannelID() );
  }
  
  int ch_getState( JNIChannel ch ) {
    assertState(isValid(), "Invalid context");
    assertThread();
      return JNI.ch_getState( ch.getChannelID() );
  }
  
  String ch_getHostName( JNIChannel ch ) {
    assertState(isValid(), "Invalid context");
    assertThread();
      return JNI.ch_getHostName( ch.getChannelID() );
  }
  
  boolean ch_getReadAccess( JNIChannel ch ) {
    assertState(isValid(), "Invalid context");
    assertThread();
      return JNI.ch_getReadAccess( ch.getChannelID() );
  }
  
  boolean ch_getWriteAccess( JNIChannel ch ) {
    assertState(isValid(), "Invalid context");
    assertThread();
    return JNI.ch_getWriteAccess( ch.getChannelID() );
  }
  
  void ch_arrayPut( DBRType type, int count, JNIChannel ch, Object value ) throws JNIException {
    assertState(isValid(), "Invalid context");
    assertThread();
    
      
      long dbrid=JNI.dbr_create( type.getValue(), count );
      JNI.dbr_setValue( dbrid, type.getValue(), count, value );
      try {
        JNI.ch_arrayPut( type.getValue(), count, ch.getChannelID(), dbrid );
      } finally {
        JNI.dbr_destroy( dbrid );
      }
  }
  
  void ch_arrayPut( DBRType type, int count, JNIChannel ch, Object value, PutListener l ) throws JNIException {
    assertState(isValid(), "Invalid context");
    assertThread();
    
      
      JNIPutCallback callback= new JNIPutCallback( ch, _eventDispatcher, l );
      long dbrid=JNI.dbr_create( type.getValue(), count );
      JNI.dbr_setValue( dbrid, type.getValue(), count, value );
      try {
        JNI.ch_arrayPutCallback( type.getValue(), count, ch.getChannelID(), dbrid, callback );
      } finally {
        JNI.dbr_destroy( dbrid );
      }
  }
  
  protected class Get {
    DBR dbr;
    long dbrid;
    public Get( DBR dbr, long dbrid ) {
      this.dbr=dbr;
      this.dbrid=dbrid;
    }
    
    public void update() {
      JNI.dbr_update( dbr, dbrid );
      JNI.dbr_destroy( dbrid );
    }
    
    public String toString() {
      return Get.class.getName()+"[ID: "+dbrid+" dbr="+dbr+"]";
    }
  }
  
  protected ArrayList _GetQueue=new ArrayList();
  
  protected void clearGetQueue() {
    for( Iterator it=_GetQueue.iterator(); it.hasNext(); ) {
    	try {
    		( ( Get )it.next() ).update();
    	} catch (Throwable th) {
    		// catch all the exceptions, so that request will be processed
    		th.printStackTrace();
    	}
    }
    _GetQueue.clear();
  }
  
  void ch_arrayGet( DBR dbr, JNIChannel ch ) throws JNIException {
    assertState(isValid(), "Invalid context");
    assertThread();
    
      
      int type=dbr.getType().getValue();
      int count=dbr.getCount();
      long dbrid=JNI.dbr_create( type, count );
      
      synchronized( _GetQueue ) {
        _GetQueue.add( new Get( dbr, dbrid ) );
        JNI.ch_arrayGet( type, count, ch.getChannelID(), dbrid );
      }
  }
  
  void ch_arrayGet( DBRType type, int count, JNIChannel ch, GetListener l ) throws JNIException {
    assertState(isValid(), "Invalid context");
    assertThread();
      JNIGetCallback callback= new JNIGetCallback( ch, _eventDispatcher, l );
      JNI.ch_arrayGetCallback( type.getValue(), count, ch.getChannelID(), callback );
  }
  
  JNIMonitor ch_addMonitor(DBRType type, int count, JNIChannel ch, MonitorListener l, int mask) throws JNIException {
    assertState(isValid(), "Invalid context");
    assertThread();
      JNIMonitor res=new JNIMonitor( this, type, count, ch, l, mask );
      
      JNIMonitorCallback callback= res.getMonitorCallback();
      
      res.setMonitorID( JNI.ch_addMonitor( type.getValue(), count, ch.getChannelID(),
      callback, mask ) );
      return res;
  }
  
  void ch_clearMonitor( JNIMonitor monitor ) throws JNIException {
    assertState(isValid(), "Invalid context");
    assertThread();
    JNI.ch_clearMonitor( monitor.getMonitorID() );
  }
  
  protected void assertThread() throws IllegalStateException {
    synchronized( _threadList ) {
      for( Iterator it=_threadList.iterator(); it.hasNext(); ) {
        if( Thread.currentThread()== ( Thread )it.next() ) {
          return;
        }
      }
    }
    
    throw new IllegalStateException( "Illegal calling thread" );
  }
  
  
}
