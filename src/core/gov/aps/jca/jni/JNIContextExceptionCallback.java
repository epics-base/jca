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
 * $Id: JNIContextExceptionCallback.java,v 1.2 2006-11-03 11:01:47 msekoranja Exp $
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

class JNIContextExceptionCallback extends JNICallback {
  protected JNIContext _source;
  JNIContextExceptionCallback( JNIContext source, EventDispatcher dispatcher, List listeners ) {
    super( dispatcher, listeners );
    _source=source;
  }

  public void fire( long channelID, int type, int count, long dbrID, String status, String ctxtInfo, String file, int line ) {
    Channel ch=null;
    if( channelID!=0 ) {
      ch=_source.lookupChannel( channelID );
    }
    DBR dbr=null;
    if( dbrID!=0 ) {
      dbr=DBRFactory.create( type, count );
      JNI.dbr_update( dbr, dbrID );
    }

    String msg="Status: "+status+"\nInfo: "+ctxtInfo+"\nfile: "+file+" at line "+line;

    ContextExceptionEvent ev=new ContextExceptionEvent( _source, ch, DBRType.forValue(type), count, dbr, msg );

    dispatch( ev );
  }

}
