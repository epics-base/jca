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
 * $Id: JNIConnectionCallback.java,v 1.2 2004-08-12 07:24:05 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */


package gov.aps.jca.jni;

import gov.aps.jca.event.*;
import java.util.*;

class JNIConnectionCallback extends JNICallback {
  protected JNIChannel _source;

  JNIConnectionCallback(JNIChannel source, EventDispatcher dispatcher, List listeners) {
    super(dispatcher,listeners);
    _source= source;
  }

  public void fire(boolean isConnected) {
	  try
	  {
		  dispatch(new ConnectionEvent(_source,isConnected));
	  } catch (Throwable th) {
		  // catch all exception not to break call from C++, report exception
		  new RuntimeException("Unexpected exception caught.", th).printStackTrace();
	  }
  }
}



