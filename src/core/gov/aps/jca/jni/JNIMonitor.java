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
 * $Id: JNIMonitor.java,v 1.3 2006-12-19 15:59:22 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.jni;

import gov.aps.jca.*;
import gov.aps.jca.event.*;
import gov.aps.jca.dbr.*;
import java.util.*;


public class JNIMonitor extends Monitor {
  protected JNIContext _jnicontext;
  protected DBRType _type;
  protected int _count;
  protected JNIChannel _channel;
//  protected MonitorListener _listener;
  protected ArrayList _monitorListenerList= new ArrayList();
  protected int _mask;
  protected long _monitorID;
  protected JNIMonitorCallback _callback;

  public JNIMonitor(JNIContext context, DBRType type, int count, JNIChannel ch, MonitorListener l, int mask) {
    _jnicontext= context;
    _type= type;
    _count=count;
    _channel= ch;
    if(l!=null) _monitorListenerList.add(l);
    _mask=mask;
    _callback= new JNIMonitorCallback(ch, context.getEventDispatcher(), _monitorListenerList);
  }

  JNIMonitorCallback getMonitorCallback() {
    return _callback;
  }

  public Context getContext() {
    return _jnicontext;
  }

  public Channel  getChannel() {
    assertValidity();
    return _channel;
  }
  public JNIChannel  getJNIChannel() {
    assertValidity();
    return _channel;
  }

  public DBRType getType() {
    assertValidity();
    return _type;
  }
  public int getCount() {
    assertValidity();
    return _count;
  }
  public int getMask() {
    assertValidity();
    return _mask;
  }

  /**
   *
   * @return the first MonitorListener added.
   * @deprecated
   */
  public MonitorListener getMonitorListener() {
    assertValidity();
    return (MonitorListener)_monitorListenerList.get(0);
  }

  public void addMonitorListener(MonitorListener l) {
    if(l==null) return;
    _monitorListenerList.add(l);
  }

  public void removeMonitorListener(MonitorListener l) {
    if(l==null) return;
    _monitorListenerList.remove(l);
  }

  public MonitorListener[] getMonitorListeners() {
    return (MonitorListener[])_monitorListenerList.toArray(new MonitorListener[0]);
  }

  public long getMonitorID() {
    return _monitorID;
  }

  protected void setMonitorID(long mID) {
    if(_monitorID!=0) throw new IllegalStateException("MonitorID has already been set");
    _monitorID= mID;
  }

  public void clear() throws CAException {
    assertValidity();
    try {
      _jnicontext.ch_clearMonitor(this);
    } catch(JNIException jnie) {
      throw new CAException("Unable to clear monitor", jnie);
    }
  }

  protected void assertValidity() {
    if(_monitorID==0) throw new IllegalStateException("Invalid monitor");
  }
}


