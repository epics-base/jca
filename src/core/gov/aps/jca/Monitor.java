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
 * $Id: Monitor.java,v 1.1 2004-08-11 15:50:32 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca;

import gov.aps.jca.event.*;
import gov.aps.jca.dbr.*;


/**
 * The class representing a Monitor.
 *
 */
abstract public class Monitor {
  static final public int VALUE=1;
  static final public int LOG  =2;
  static final public int ALARM=4;
  static final public int PROPERTY=8;


  abstract public Context  getContext();
  abstract public Channel  getChannel();
  abstract public DBRType getType();
  abstract public int      getCount();
  abstract public int      getMask();
           public boolean  isMonitoringValue() { return (getMask() & VALUE)!=0; }
           public boolean  isMonitoringLog()   { return (getMask() & LOG)!=0; }
           public boolean  isMonitoringAlarm() { return (getMask() & ALARM)!=0; }
           public boolean  isMonitoringProperty() { return (getMask() & PROPERTY)!=0; }
  /*
   * @deprecated
   */
  abstract public MonitorListener getMonitorListener();

  abstract public void clear() throws CAException;

  abstract public void addMonitorListener(MonitorListener l);
  abstract public void removeMonitorListener(MonitorListener l);
  abstract public MonitorListener[] getMonitorListeners();
}


