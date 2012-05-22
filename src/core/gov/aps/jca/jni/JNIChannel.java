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
 * $Id: JNIChannel.java,v 1.7 2007-01-21 15:54:36 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */


package gov.aps.jca.jni;

import java.util.*;

import gov.aps.jca.*;
import gov.aps.jca.dbr.*;
import gov.aps.jca.event.*;


public class JNIChannel extends Channel {

  protected JNIContext _jnicontext;
  protected long        _channelID=0;
  protected String     _name;
  protected short	   _priority;
  
  protected JNIChannel(JNIContext context, String name, ConnectionListener l, short priority) {
    _jnicontext= context;
    _name= name;
    _priority= priority;
    if(l!=null) {
      _cnxListenerList.add(l);
      _cnxCallback= new JNIConnectionCallback(this, getEventDispatcher(),_cnxListenerList);
    }

  }

  public EventDispatcher getEventDispatcher() {
    return _jnicontext.getEventDispatcher();
  }

  void setChannelID(long channelID) {
    _channelID= channelID;
  }

  final long getChannelID() {
    return _channelID;
  }

  final public boolean isValid() {
    return _channelID!=0;
  }
  
  final protected void assertState(boolean cond, String msg) throws IllegalStateException {
    if(!cond) throw new IllegalStateException(msg);
  }

  public void destroy() throws CAException, IllegalStateException {
    assertState(isValid(), "Channel already destroyed");
    try {
      _jnicontext.ch_channelDestroy(this);
    } catch(JNIException ex) {
      throw new CAException("Failed to destroy channel", ex);
    } finally {
    _jnicontext.unregisterChannel(this);

    _jnicontext=null;
    _name=null;
    _channelID=0;
    _cnxListenerList.clear();
    _accessListenerList.clear();
    }
  }


  public Context getContext() throws IllegalStateException {
    assertState(isValid(), "Invalid channel");
    return _jnicontext;
  }

  final short getPriority() {
	return _priority;
  }

  protected ArrayList _cnxListenerList= new ArrayList();
  protected JNIConnectionCallback _cnxCallback=null;

  JNIConnectionCallback getConnectionCallback() {
    return _cnxCallback;
  }

  public ConnectionListener[] getConnectionListeners() {
    return (ConnectionListener[]) _cnxListenerList.toArray(new ConnectionListener[0]);
  }

  public void addConnectionListener(ConnectionListener l) throws CAException, IllegalStateException {
    assertState(isValid(), "Invalid channel");
    
    if(l!=null) _cnxListenerList.add(l);

    if(_cnxListenerList.size()==1) {
      _cnxCallback= new JNIConnectionCallback(this, getEventDispatcher(), _cnxListenerList);
      try {
        _jnicontext.ch_setConnectionCallback(this, _cnxCallback);
      } catch(JNIException jniex) {
        throw new CAException("Failed to add ConnectionListener", jniex);
      }
    }
  }

  public void removeConnectionListener(ConnectionListener l) throws CAException, IllegalStateException {
    assertState(isValid(), "Invalid channel");
    if(l!=null) _cnxListenerList.remove(l);

    if(_cnxListenerList.size()==0) {
      _cnxCallback= null;
      try {
        _jnicontext.ch_setConnectionCallback(this, _cnxCallback);
      } catch(JNIException jniex) {
        throw new CAException("Failed to remove ConnectionListener", jniex);
      }
    }
  }


  protected ArrayList  _accessListenerList= new ArrayList();
  protected JNIAccessRightsCallback _accessEventCallback=null;

  JNIAccessRightsCallback getAccessRightsCallback() {
    return _accessEventCallback;
  }

  public AccessRightsListener[] getAccessRightsListeners() throws IllegalStateException {
    assertState(isValid(), "Invalid channel");
    return (AccessRightsListener[]) _accessListenerList.toArray(new AccessRightsListener[0]);
  }

  public void addAccessRightsListener(AccessRightsListener l) throws CAException, IllegalStateException {
    assertState(isValid(), "Invalid channel");
    if(l!=null) _accessListenerList.add(l);

    if(_accessListenerList.size()==1) {
      _accessEventCallback= new JNIAccessRightsCallback(this, getEventDispatcher(), _accessListenerList);
      try {
        _jnicontext.ch_setAccessRightsCallback(this, _accessEventCallback);
      } catch(JNIException jniex) {
        throw new CAException("Failed to add AccessRightsListener", jniex);
      }
    }
  }

  public void removeAccessRightsListener(AccessRightsListener l) throws CAException, IllegalStateException {
    assertState(isValid(), "Invalid channel");
    if(l!=null) _accessListenerList.remove(l);

    if(_accessListenerList.size()==0) {
      _accessEventCallback= null;
      try {
        _jnicontext.ch_setAccessRightsCallback(this, _accessEventCallback);
      } catch(JNIException jniex) {
        throw new CAException("Failed to add AccessRightsCallback", jniex);
      }

    }
  }



  /**
   * return the name of the process variable.
   *
   * @exception IllegalStateException is thrown if the process variable has been disposed.
   **/
  public String  getName() throws IllegalStateException {
//    assertState(isValid(),  "Invalid channel");
    return _name;
  }

  /**
   * return the pv's channel access type.
   *
   * @exception IllegalStateException is thrown if the process variable has been disposed.
   **/
  public DBRType getFieldType() throws IllegalStateException{
    assertState(isValid(), "Invalid channel");
    return DBRType.forValue(_jnicontext.ch_getFieldType(this));
  }

  /**
   * return the pv's element count.
   *
   * @exception IllegalStateException is thrown if the process variable has been disposed.
   **/
  public int getElementCount() throws IllegalStateException {
    assertState(isValid(), "Invalid channel");
    return _jnicontext.ch_getElementCount(this);
  }

  /**
   * return the pv's connection state.
   *
   * @exception IllegalStateException is thrown if the process variable has been disposed.
   **/
  public ConnectionState getConnectionState() throws IllegalStateException {
    assertState(isValid(), "Invalid channel");
    return ConnectionState.forValue(_jnicontext.ch_getState(this));
  }

  /**
   * return the pv's hostname.
   *
   * @exception IllegalStateException is thrown if the process variable has been disposed.
   **/
  public String getHostName() throws IllegalStateException {
    assertState(isValid(), "Invalid channel");
    return _jnicontext.ch_getHostName(this);
  }

  /**
   * return the pv's read access right.
   *
   * @exception IllegalStateException is thrown if the process variable has been disposed.
   **/
  public boolean getReadAccess() throws IllegalStateException {
    assertState(isValid(), "Invalid channel");
    return _jnicontext.ch_getReadAccess(this);
  }

  /**
   * return the pv's write access right.
   *
   * @exception IllegalStateException is thrown if the process variable has been disposed.
   **/
  public boolean getWriteAccess() throws IllegalStateException {
    assertState(isValid(), "Invalid channel");
    return _jnicontext.ch_getWriteAccess(this);
  }




  public void put(byte[] value) throws CAException, IllegalStateException {
    put(DBRType.BYTE,value.length, value);
  }
  public void put(byte[] value, PutListener l) throws CAException, IllegalStateException {
    put(DBRType.BYTE,value.length, value, l);
  }

  public void put(short[] value) throws CAException, IllegalStateException {
    put(DBRType.SHORT,value.length, value);
  }
  public void put(short[] value, PutListener l) throws CAException, IllegalStateException {
    put(DBRType.SHORT,value.length, value, l);
  }

  public void put(int[] value) throws CAException, IllegalStateException {
    put(DBRType.INT,value.length, value);
  }
  public void put(int[] value, PutListener l) throws CAException, IllegalStateException {
    put(DBRType.INT,value.length, value, l);
  }

  public void put(float[] value) throws CAException, IllegalStateException {
    put(DBRType.FLOAT,value.length, value);
  }
  public void put(float[] value, PutListener l) throws CAException, IllegalStateException {
    put(DBRType.FLOAT,value.length, value, l);
  }

  public void put(double[] value) throws CAException, IllegalStateException {
    put(DBRType.DOUBLE,value.length, value);
  }
  public void put(double[] value, PutListener l) throws CAException, IllegalStateException {
    put(DBRType.DOUBLE,value.length, value, l);
  }

  public void put(String[] value) throws CAException, IllegalStateException {
    put(DBRType.STRING,value.length, value);
  }
  public void put(String[] value, PutListener l) throws CAException, IllegalStateException {
    put(DBRType.STRING,value.length, value, l);
  }

  public void putACKS(Severity severity, PutListener l) throws CAException, IllegalStateException {
    put(DBRType.PUT_ACKS, 1, new short[] { (short)severity.getValue() }, l);
  }

  public void putACKS(Severity severity) throws CAException, IllegalStateException {
    put(DBRType.PUT_ACKS, 1, new short[] { (short)severity.getValue() });
  }

  public void putACKT(boolean value, PutListener l) throws CAException, IllegalStateException {
    put(DBRType.PUT_ACKT, 1, new short[] { value ? (short)1 : (short)0 }, l);
  }

  public void putACKT(boolean value) throws CAException, IllegalStateException {
    put(DBRType.PUT_ACKT, 1, new short[] { value ? (short)1 : (short)0 });
  }

  protected void put(DBRType type, int count, Object value) throws CAException, IllegalStateException {
    assertState(isValid(), "Invalid channel");
    try {
      _jnicontext.ch_arrayPut(type,count,this,value);
    } catch(JNIException jniex) {
      throw new CAException("Put failed", jniex);
    }
  }
  protected void put(DBRType type, int count, Object value, PutListener l) throws CAException, IllegalStateException {
    assertState(isValid(), "Invalid channel");
    try {
      _jnicontext.ch_arrayPut(type,count,this,value,l);
    } catch(JNIException jniex) {
      throw new CAException("Put failed", jniex);
    }
  }



  public DBR  get(DBRType type, int count) throws CAException {
    assertState(isValid(), "Invalid channel");
    DBR dbr= DBRFactory.create(type,count);
    if (dbr == null)
    	throw new CAException("Get failed: failed to create DBR.");
    try {
      _jnicontext.ch_arrayGet(dbr,this);
    } catch(JNIException jniex) {
      throw new CAException("Get failed", jniex);
    }
    return dbr;
  }

  public void get(DBRType type, int count, GetListener l) throws CAException {
    assertState(isValid(), "Invalid channel");
    try {
      _jnicontext.ch_arrayGet(type,count, this, l);
    } catch(JNIException jniex) {
      throw new CAException("Get failed", jniex);
    }

  }


  /**
   *
   * @param type
   * @param count
   * @param l
   * @param mask
   * @return newly created monitor.
   * @throws CAException
   */
  public Monitor addMonitor(DBRType type, int count, int mask, MonitorListener l) throws CAException {
    assertState(isValid(), "Invalid channel");
    checkMonitorSize(type, count, _jnicontext.getMaxArrayBytes());
    try {
      return _jnicontext.ch_addMonitor(type,count,this,l,mask);
    } catch(JNIException jniex) {
      throw new CAException("Add monitor failed", jniex);
    }
  }

}




