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
 * $Id: JNIAccessRightsCallback.java,v 1.2 2004-08-12 07:24:05 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.jni;

import gov.aps.jca.event.*;
import java.util.*;

class JNIAccessRightsCallback extends JNICallback {
  JNIChannel _source;
  JNIAccessRightsCallback( JNIChannel source, EventDispatcher dispatcher, List listeners ) {
    super( dispatcher, listeners );
    _source=source;
  }

  public void fire( boolean read, boolean write ) {
	  try {
		  dispatch( new AccessRightsEvent( _source, read, write ) );
	  } catch (Throwable th) {
		  // catch all exception not to break call from C++, report exception
		  new RuntimeException("Unexpected exception caught.", th).printStackTrace();
	  }
  }
}
