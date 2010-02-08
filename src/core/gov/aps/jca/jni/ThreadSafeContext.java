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
 * $Id: ThreadSafeContext.java,v 1.7 2008-10-27 09:40:16 msekoranja Exp $
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
import java.util.logging.*;
import java.util.concurrent.*;



final public class ThreadSafeContext extends JNIContext implements Runnable, Configurable {
	protected int _priority= Thread.NORM_PRIORITY;
	protected Thread _thread;
	
	public static final Version VERSION = new Version("JNI/ThreadSafeContext Channel Access", "Java w/ JNI using native C++ code",
													  JCALibrary.getInstance().getVersion(), JCALibrary.getInstance().getRevision(),
													  JCALibrary.getInstance().getModification(), 0);
	
	/**
	 * @see Context#getVersion()
	 */
	public Version getVersion() {
		return VERSION;
	}
	
	public ThreadSafeContext() {
		String cn=gov.aps.jca.jni.ThreadSafeContext.class.getName();
		JCALibrary jca=JCALibrary.getInstance();
		
		String logger= jca.getProperty( cn+".logger", null);
		setLogger(logger==null?getLogger():Logger.getLogger(logger));
		
	    if (Boolean.getBoolean("jca.use_env"))
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
			setPriority(jca.getPropertyAsInt( cn+".priority", getPriority()));
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
		
		int priority= getPriority();
		try {
			priority= conf.getChild("priority", false).getValueAsInteger();
		} catch(Exception ex) {
			priority= conf.getAttributeAsInteger("priority", priority);
		}
		setPriority(priority);
		
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
	
	
	
	public String paramString() {
		StringBuffer sb=new StringBuffer( getClass().getName() );
		sb.append( "[" );
		sb.append( "PREEMPTIVE_CALLBACK=" );
		sb.append( getPreemptiveCallback() );
		sb.append( ",ADDR_LIST=" );
		sb.append( getAddrList() );
		sb.append( ",AUTO_ADDR_LIST=" );
		sb.append( getAutoAddrList() );
		sb.append( ",CONNECTION_TIMEOUT=" );
		sb.append( getConnectionTimeout() );
		sb.append( ",BEACON_PERIOD=" );
		sb.append( getBeaconPeriod() );
		sb.append( ",REPEATER_PORT=" );
		sb.append( getRepeaterPort() );
		sb.append( ",SERVER_PORT=" );
		sb.append( getServerPort() );
		sb.append( ",MAX_ARRAY_BYTES=" );
		sb.append( getMaxArrayBytes() );
		sb.append( ",PRIORITY=" );
		sb.append( getPriority() );
		sb.append( "]" );
		return sb.toString();
	}
	
	public void printInfo( java.io.PrintStream out ) {
		super.printInfo( out );
		out.println( "PRIORITY : "+getPriority() );
	}
	
	
	
	protected void setPriority(int priority) throws IllegalStateException {
		assertState(!isInitialized(), "Context already initialized");
		_priority= priority;
	}
	
	public int getPriority() {
		return _priority;
	}
	
	protected long contextInitialize() throws CAException, IllegalStateException {
		assertState(!isInitialized(), "Context already initialized");
		_thread= new Thread(this);
		_thread.setDaemon(true);
		_thread.setPriority(getPriority());
		_thread.start();
		try {
			return ((Long)processRequest(new ContextCreateRequest())).longValue();
		} catch(JNIException ex) {
			throw new CAException("Failed to initialize context", ex);
		}
	}
	
	public void destroy() throws CAException, IllegalStateException{
		if(getCtxtID()==0) return;
		super.destroy();
		
		try {
			processRequest(new ContextDestroyRequest());
		} catch(JNIException ex) {
			throw new CAException("Failed to destroy context", ex);
		} finally {
			_thread=null;
			_keepRunning=false;
			REQUEST_QUEUE.clear();
			setCtxtID(0);
		}
	}
	
	
	void setMessageCallback(JNIContextMessageCallback callback) throws CAException, IllegalStateException{
		_initialize();
		assertState(isValid(), "Invalid context");
		try {
			processRequest(new MessageCallbackRequest(callback));
		} catch(JNIException ex) {
			throw new CAException("Failed to set context message handler", ex);
		}
	}
	
	void setExceptionCallback(JNIContextExceptionCallback callback) throws CAException, IllegalStateException {
		_initialize();
		assertState(isValid(), "Invalid context");
		try {
			processRequest(new ExceptionCallbackRequest(callback));
		} catch(JNIException ex) {
			throw new CAException("Failed to se context exception's handler", ex);
		}
	}
	
	public void pendIO(double timeout) throws gov.aps.jca.TimeoutException, CAException, IllegalStateException {
		if(getLogger()!=null) getLogger().log(Level.FINE, "pendIO("+timeout+")");
		_initialize();
		assertState(isValid(), "Invalid context");
		try {
			processRequest(new PendIORequest(timeout));
		} catch(JNIException ex) {
			if(ex.getStatus()==CAStatus.TIMEOUT) throw new gov.aps.jca.TimeoutException("pendIO timed out", ex);
			else throw new CAException("pendIO failed", ex);
		}
	}
	
	public boolean  testIO() throws CAException, IllegalStateException {
		if(getLogger()!=null) getLogger().log(Level.FINE, "testIO()");
		_initialize();
		assertState(isValid(), "Invalid context");
		try {
			return((Boolean)processRequest(new TestIORequest())).booleanValue();
		} catch(JNIException ex) {
			throw new CAException("testIO failed", ex);
		}
	}
	
	public void pendEvent(double time) throws CAException, IllegalStateException {
		if(getLogger()!=null) getLogger().log(Level.FINE, "pendEvent("+time+")");
		_initialize();
		assertState(isValid(), "Invalid context");
		try {
			processRequest(new PendEventRequest(time));
		} catch(JNIException ex) {
			throw new CAException("pendEvent failed", ex);
		}
	}
	
	public void poll() throws CAException, IllegalStateException {
		if(getLogger()!=null) getLogger().log(Level.FINE, "poll()");
		_initialize();
		assertState(isValid(), "Invalid context");
		try {
			processRequest(new PollRequest());
		} catch(JNIException ex) {
			throw new CAException("poll failed", ex);
		}
	}
	
	public void flushIO() throws CAException, IllegalStateException {
		if(getLogger()!=null) getLogger().log(Level.FINE, "flushIO()");
		_initialize();
		assertState(isValid(), "Invalid context");
		try {
			processRequest(new FlushIORequest());
		} catch(JNIException ex) {
			throw new CAException("flushIO failed", ex);
		}
	}
	
	public void attachCurrentThread() throws IllegalStateException {
		if(getLogger()!=null) getLogger().log(Level.FINE, "attachCurrentThread()", Thread.currentThread().getName());
		_initialize();
		assertState(isValid(), "Invalid context");
	}
	
	
	public Channel createChannel(String name, ConnectionListener l, short priority) throws CAException, IllegalStateException {
		if(getLogger()!=null) getLogger().log(Level.FINE, "createChannel("+name+","+l+","+priority+")");
		_initialize();
		if(name==null || (name=name.trim()).equals("")) throw new IllegalArgumentException("Channel's name is null or empty");
		if(priority<Channel.PRIORITY_MIN || priority>Channel.PRIORITY_MAX) throw new IllegalArgumentException("Priority out of bounds");
		assertState(isValid(), "Invalid context");
		try {
			JNIChannel ch= (JNIChannel)processRequest(new ChannelCreateRequest(name,l,priority));
			registerChannel(ch);
			return ch;
		} catch(JNIException ex) {
			throw new CAException("createChannel failed", ex);
		}
	}
	
	
	
	
	void ch_channelDestroy(JNIChannel ch) throws JNIException {
		assertState(isValid(), "Invalid context");
		processRequest(new ChannelDestroyRequest(ch));
	}
	
	void ch_setConnectionCallback(JNIChannel ch, JNIConnectionCallback callback) throws JNIException {
		assertState(isValid(), "Invalid context");
		processRequest(new ConnectionCallbackRequest(ch, callback));
	}
	
	void ch_setAccessRightsCallback(JNIChannel ch, JNIAccessRightsCallback callback) throws JNIException {
		assertState(isValid(), "Invalid context");
		processRequest(new AccessRightsRequest(ch,callback));
	}
	
	int ch_getFieldType(JNIChannel ch) {
		assertState(isValid(), "Invalid context");
		return((Integer)processRequestSafe(new GetFieldTypeRequest(ch))).intValue();
	}
	
	int ch_getElementCount(JNIChannel ch) {
		assertState(isValid(), "Invalid context");
		return((Integer)processRequestSafe(new GetElementCountRequest(ch))).intValue();
	}
	
	int ch_getState(JNIChannel ch) {
		assertState(isValid(), "Invalid context");
		return((Integer)processRequestSafe(new GetStateRequest(ch))).intValue();
	}
	
	String ch_getHostName(JNIChannel ch) {
		assertState(isValid(), "Invalid context");
		return(String)processRequestSafe(new GetHostNameRequest(ch));
	}
	
	boolean ch_getReadAccess(JNIChannel ch) {
		assertState(isValid(), "Invalid context");
		return((Boolean)processRequestSafe(new GetReadAccessRequest(ch))).booleanValue();
	}
	
	boolean ch_getWriteAccess(JNIChannel ch) {
		assertState(isValid(), "Invalid context");
		return((Boolean)processRequestSafe(new GetWriteAccessRequest(ch))).booleanValue();
	}
	
	void ch_arrayPut(DBRType type, int count, JNIChannel ch, Object value) throws JNIException {
		assertState(isValid(), "Invalid context");
		processRequest(new ArrayPutRequest(type,count,ch,value));
	}
	
	void ch_arrayPut(DBRType type, int count, JNIChannel ch, Object value, PutListener l) throws JNIException {
		assertState(isValid(), "Invalid context");
		processRequest(new ArrayPutCallbackRequest(type,count,ch,value,l));
	}
	
	
	void ch_arrayGet(DBR dbr, JNIChannel ch) throws JNIException {
		assertState(isValid(), "Invalid context");
		processRequest(new ArrayGetRequest(dbr,ch));
	}
	
	void ch_arrayGet(DBRType type, int count, JNIChannel ch, GetListener l) throws JNIException {
		assertState(isValid(), "Invalid context");
		processRequest(new ArrayGetCallbackRequest(type,count,ch,l));
	}
	
	
	JNIMonitor ch_addMonitor(DBRType type, int count, JNIChannel ch, MonitorListener l, int mask) throws JNIException {
		assertState(isValid(), "Invalid context");
		return(JNIMonitor)processRequest(new AddMonitorRequest(type,count,ch,l,mask));
	}
	
	void ch_clearMonitor(JNIMonitor monitor) throws JNIException {
		assertState(isValid(), "Invalid context");
		processRequest(new ClearMonitorRequest(monitor));
	}
	
	// CA THREAD
	
	final private LinkedBlockingQueue REQUEST_QUEUE = new LinkedBlockingQueue();
	private boolean _keepRunning= true;
	
	private void queueRequest( final Request request ) {
		try {
			if ( request != null ) {
				REQUEST_QUEUE.put( request );			  
			}
		}
		catch( InterruptedException exception ) {
			exception.printStackTrace();
		}
	}
	
	
	private Request nextRequest() {
		try {
			return (Request)REQUEST_QUEUE.take();
		}
		catch( InterruptedException exception ) {
			exception.printStackTrace();
			return null;
		}
	}
	
	/*
	private void pause(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException ie) {
		}
	}
	*/
	
	public void run() {
		while ( _keepRunning ) {
			final Request request = nextRequest();
			if ( request != null ) {
				synchronized( request ) {
					try {
						request.process();
					} 
					catch ( JNIException cae ) {
						request.setException( cae );
					}
					catch ( Throwable th ) {
						  // catch all exception not to break call this thread, report exception
						  new RuntimeException("Unexpected exception caught.", th).printStackTrace();
					}
					request.notifyAll();
				}
				Thread.yield();
			}
		}
	}
	
	protected Object processRequestSafe(Request r) {
		try {
			return processRequest(r);
		} catch (JNIException cae) {
		}
		return null;
	}
	
	protected Object processRequest(Request r) throws JNIException {
		synchronized(r) {
			queueRequest(r);
			try {
				r.wait();
			} catch (InterruptedException ie) {
			}
			r.fireException();
			return r.result();
		}
	}
	
	
	// INTERNAL SERVICES
	
	
	LinkedList _GetQueue= new LinkedList();
	
	class Get {
		DBR dbr;
		long dbrid;
		public Get(DBR dbr, long dbrid) {
			this.dbr= dbr;
			this.dbrid= dbrid;
		}
		public void update() {
			JNI.dbr_update(dbr, dbrid);
			JNI.dbr_destroy(dbrid);
		}
		public String toString() {
			return Get.class.getName()+"[ID: "+dbrid+" dbr="+dbr+"]";
		}
	}
	
	void clearGetQueue() {
		for (Iterator it= _GetQueue.iterator(); it.hasNext();) {
	    	try {
	    		( ( Get )it.next() ).update();
	    	} catch (Throwable th) {
	    		// catch all the exceptions, so that request will be processed
	    		th.printStackTrace();
	    	}
		}
		_GetQueue.clear();
	}
	
	
	
	JNIContext getJNIContext() {
		return this;
	}
	
	
	class Request {
		protected JNIException _cause;
		
		public void process() throws JNIException {
		}
		public Object result() {
			return null;
		}
		public void setException(JNIException cause) {
			_cause=cause;
		}
		public void fireException() throws JNIException {
			if (_cause!=null) throw _cause;
		}
	}
	
	
	class ContextCreateRequest extends Request {
		protected long _res = 0;
		public void process() throws JNIException {
			synchronized(JNI.lock()) {
				JNI.setenv("EPICS_CA_ADDR_LIST", _addr_list);
				JNI.setenv("EPICS_CA_AUTO_ADDR_LIST", (_auto_addr_list?"YES":"NO"));
				JNI.setenv("EPICS_CA_CONN_TMO", ""+_connection_timeout);
				JNI.setenv("EPICS_CA_BEACON_PERIOD", ""+_beacon_period);
				JNI.setenv("EPICS_CA_REPEATER_PORT", ""+_repeater_port);
				JNI.setenv("EPICS_CA_SERVER_PORT", ""+_server_port);
				JNI.setenv("EPICS_CA_MAX_ARRAY_BYTES", ""+_max_array_bytes);
				_res= JNI.ctxt_contextCreate(_preemptive_callback);
			}
		}
		public Object result() {
			return new Long(_res);
		}
	}
	
	class ContextDestroyRequest extends Request {
		public void process() throws JNIException {
			JNI.ctxt_contextDestroy();
		}
	}
	
	class MessageCallbackRequest extends Request {
		protected JNIContextMessageCallback _callback;
		public MessageCallbackRequest(JNIContextMessageCallback cb) {
			_callback=cb;
		}
		public void process() throws JNIException {
			JNI.ctxt_setMessageCallback(_callback);
		}
	}
	
	class ExceptionCallbackRequest extends Request {
		protected JNIContextExceptionCallback _callback;
		public ExceptionCallbackRequest(JNIContextExceptionCallback cb) {
			_callback=cb;
		}
		public void process() throws JNIException {
			JNI.ctxt_setExceptionCallback(_callback);
		}
	}
	
	class PendIORequest extends Request {
		protected double _timeout;
		public PendIORequest(double timeout) {
			_timeout= timeout;
		}
		public void process() throws JNIException {
			JNI.ctxt_pendIO(_timeout);
			clearGetQueue();
		}
	}
	
	class TestIORequest extends Request {
		protected boolean _res;
		public void process() throws JNIException {
			_res= JNI.ctxt_testIO();
		}
		public Object result() {
			return new Boolean(_res);
		}
	}
	
	class PendEventRequest extends Request {
		protected double _timeout;
		public PendEventRequest(double timeout) {
			_timeout= timeout;
		}
		public void process() throws JNIException {
			JNI.ctxt_pendEvent(_timeout);
			clearGetQueue();
		}
	}
	
	class PollRequest extends Request {
		public void process() throws JNIException {
			JNI.ctxt_poll();
		}
	}
	
	class FlushIORequest extends Request {
		public void process() throws JNIException {
			JNI.ctxt_flushIO();
		}
	}
	
	
	class ChannelCreateRequest extends Request {
		protected JNIChannel _res;
		public ChannelCreateRequest(String name, ConnectionListener l, short priority) {
			_res= new JNIChannel(getJNIContext(), name, l, priority);
		}
		public void process() throws JNIException {
			
			synchronized(JNI.lock()) {
				JNI.setenv("EPICS_CA_ADDR_LIST", _addr_list);
				JNI.setenv("EPICS_CA_AUTO_ADDR_LIST", (_auto_addr_list?"YES":"NO"));
				JNI.setenv("EPICS_CA_CONN_TMO", ""+_connection_timeout);
				JNI.setenv("EPICS_CA_BEACON_PERIOD", ""+_beacon_period);
				JNI.setenv("EPICS_CA_REPEATER_PORT", ""+_repeater_port);
				JNI.setenv("EPICS_CA_SERVER_PORT", ""+_server_port);
				JNI.setenv("EPICS_CA_MAX_ARRAY_BYTES", ""+_max_array_bytes);
				
				_res.setChannelID(JNI.ch_channelCreate(_res.getName(),_res.getConnectionCallback(),_res.getPriority()));
			}
			
		}
		public Object result() {
			return _res;
		}
	}
	
	class ChannelDestroyRequest extends Request {
		protected JNIChannel _ch;
		public ChannelDestroyRequest(JNIChannel ch) {
			_ch= ch;
		}
		public void process() throws JNIException {
			JNI.ch_channelDestroy(_ch.getChannelID());
		}
	}
	
	class ConnectionCallbackRequest extends Request {
		protected JNIChannel _ch;
		protected JNIConnectionCallback _callback;
		public ConnectionCallbackRequest(JNIChannel ch, JNIConnectionCallback cb) {
			_ch= ch;
			_callback=cb;
		}
		public void process() throws JNIException {
			JNI.ch_setConnectionCallback(_ch.getChannelID(), _callback);
		}
	}
	
	
	class AccessRightsRequest extends Request {
		protected JNIChannel _ch;
		protected JNIAccessRightsCallback _callback;
		public AccessRightsRequest(JNIChannel ch, JNIAccessRightsCallback cb) {
			_ch= ch;
			_callback=cb;
		}
		public void process() throws JNIException {
			JNI.ch_setAccessRightsCallback(_ch.getChannelID(), _callback);
		}
	}
	
	
	class GetFieldTypeRequest extends Request {
		protected int _res;
		protected JNIChannel _ch;
		public GetFieldTypeRequest(JNIChannel ch) { _ch= ch;}
		public void process() throws JNIException { _res= JNI.ch_getFieldType(_ch.getChannelID());}
		public Object result() { return new Integer(_res);}
	}
	
	class GetElementCountRequest extends Request {
		protected int _res;
		protected JNIChannel _ch;
		public GetElementCountRequest(JNIChannel ch) { _ch= ch;}
		public void process() throws JNIException { _res= JNI.ch_getElementCount(_ch.getChannelID());}
		public Object result() { return new Integer(_res);}
	}
	
	class GetStateRequest extends Request {
		protected int _res;
		protected JNIChannel _ch;
		public GetStateRequest(JNIChannel ch) { _ch= ch;}
		public void process() throws JNIException { _res= JNI.ch_getState(_ch.getChannelID());}
		public Object result() { return new Integer(_res);}
	}
	
	class GetHostNameRequest extends Request {
		protected String _res;
		protected JNIChannel _ch;
		public GetHostNameRequest(JNIChannel ch) { _ch= ch;}
		public void process() throws JNIException { _res= JNI.ch_getHostName(_ch.getChannelID());}
		public Object result() { return _res;}
	}
	
	class GetReadAccessRequest extends Request {
		protected boolean _res;
		protected JNIChannel _ch;
		public GetReadAccessRequest(JNIChannel ch) { _ch= ch;}
		public void process() throws JNIException { _res= JNI.ch_getReadAccess(_ch.getChannelID());}
		public Object result() { return new Boolean(_res);}
	}
	
	class GetWriteAccessRequest extends Request {
		protected boolean _res;
		protected JNIChannel _ch;
		public GetWriteAccessRequest(JNIChannel ch) { _ch= ch;}
		public void process() throws JNIException { _res= JNI.ch_getWriteAccess(_ch.getChannelID());}
		public Object result() { return new Boolean(_res);}
	}
	
	
	
	class ArrayPutRequest extends Request {
		protected DBRType _type;
		protected int _count;
		protected JNIChannel _ch;
		protected Object _value;
		public ArrayPutRequest(DBRType type, int count, JNIChannel ch, Object value) {
			_type=type;_count=count;_ch=ch;_value=value;
		}
		public void process() throws JNIException {
			long dbrid= JNI.dbr_create(_type.getValue(),_count);
			JNI.dbr_setValue(dbrid, _type.getValue(), _count, _value);
			try {
				JNI.ch_arrayPut(_type.getValue(),_count,_ch.getChannelID(),dbrid);
			} finally {
				JNI.dbr_destroy(dbrid);
			}
		}
	}
	
	class ArrayPutCallbackRequest extends Request {
		protected DBRType _type;
		protected int _count;
		protected JNIChannel _ch;
		protected Object _value;
		protected PutListener _l;
		public ArrayPutCallbackRequest(DBRType type, int count, JNIChannel ch, Object value, PutListener l) {
			_type=type;_count=count;_ch=ch;_value=value;_l=l;
		}
		public void process() throws JNIException {
			synchronized(JNI.lock()) {
				
				JNIPutCallback callback= new JNIPutCallback(_ch, _eventDispatcher, _l);
				long dbrid= JNI.dbr_create(_type.getValue(),_count);
				JNI.dbr_setValue(dbrid, _type.getValue(), _count, _value);
				try {
					JNI.ch_arrayPutCallback(_type.getValue(),_count,_ch.getChannelID(),dbrid, callback);
				} finally {
					JNI.dbr_destroy(dbrid);
				}
			}
		}
	}
	
	
	
	class ArrayGetRequest extends Request {
		protected DBR _dbr;
		protected JNIChannel _ch;
		public ArrayGetRequest(DBR dbr, JNIChannel ch) {
			_dbr= dbr;
			_ch=ch;
		}
		public void process() throws JNIException {
			synchronized(JNI.lock()) {
				
				int type= _dbr.getType().getValue();
				int count= _dbr.getCount();
				long dbrid= JNI.dbr_create(type,count);
				
				_GetQueue.addLast(new Get(_dbr,dbrid));
				JNI.ch_arrayGet(type,count,_ch.getChannelID(),dbrid);
			}
		}
	}
	
	
	class ArrayGetCallbackRequest extends Request {
		protected DBRType _type;
		protected int _count;
		protected JNIChannel _ch;
		protected GetListener _l;
		public ArrayGetCallbackRequest(DBRType type, int count, JNIChannel ch, GetListener l) {
			_type=type;_count=count;_ch=ch;_l=l;
		}
		public void process() throws JNIException {
			JNIGetCallback callback= new JNIGetCallback(_ch,_eventDispatcher, _l);
			JNI.ch_arrayGetCallback(_type.getValue(),_count,_ch.getChannelID(),callback);
		}
	}
	
	class AddMonitorRequest extends Request {
		protected DBRType _type;
		protected int _count;
		protected JNIChannel _ch;
		protected MonitorListener _l;
		protected int _mask;
		protected JNIMonitor _res;
		public AddMonitorRequest(DBRType type, int count, JNIChannel ch, MonitorListener l, int mask) {
			_type=type;
			_count=count;
			_ch=ch;
			_l=l;
			_mask=mask;
			_res= new JNIMonitor(getJNIContext(),type,count,ch,l,mask);
		}
		public void process() throws JNIException {
			JNIMonitorCallback callback= _res.getMonitorCallback();
			_res.setMonitorID(JNI.ch_addMonitor(_type.getValue(),_count,_ch.getChannelID(),callback,_mask));
		}
		public Object result() {
			return _res;
		}
	}
	
	
	class ClearMonitorRequest extends Request {
		protected JNIMonitor _m;
		public ClearMonitorRequest(JNIMonitor m) {
			_m=m;
		}
		public void process() throws JNIException {
			JNI.ch_clearMonitor(_m.getMonitorID());
		}
	}
	
	
}





