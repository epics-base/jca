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
 * $Id: JNIMonitorCallback.java,v 1.2 2006-11-03 11:01:47 msekoranja Exp $
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


class JNIMonitorCallback extends JNICallback {
  protected JNIChannel _source;

  JNIMonitorCallback(JNIChannel source, EventDispatcher dispatcher, List listeners) {
    super(dispatcher,listeners);
    _source=source;
  }

  public void fire(int type, int count, long dbrid, int status) {
	  try {
            // dbrid, the actual value, can be null if for example
            // the server's EPICS_CA_MAX_ARRAY_BYTES is too small
            // to send an array.
			CAStatus st= CAStatus.forValue(status);
			DBR dbr;
            if (dbrid == 0 || !st.isSuccessful())
                dbr = null;
            else
            {
                dbr = DBRFactory.create(type,count);
                JNI.dbr_update(dbr,dbrid);
            }
	    dispatch(new MonitorEvent(_source, dbr, st));
	  } catch (Throwable th) {
		  // catch all exception not to break call from C++, report exception
		  new RuntimeException("Unexpected exception caught.", th).printStackTrace();
	  }
  }
}



