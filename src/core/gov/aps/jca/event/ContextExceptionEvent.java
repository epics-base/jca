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
 * $Id: ContextExceptionEvent.java,v 1.1 2004-08-11 15:50:32 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.event;

import gov.aps.jca.*;
import gov.aps.jca.dbr.*;

/**
 * An event which indicates an asynchronous exception in a Context.
 *
 * Asynchronous context exceptions occur when a server notify the Context of a remote problem.
 *
 * @see gov.aps.jca.Context
 */

public class ContextExceptionEvent extends CAEvent {

  Channel _channel;
  DBRType _dbrType;
  int _count;
  DBR _dbr;
  String _msg;

  public ContextExceptionEvent( Context ctxt, Channel ch, DBRType dbrType, int count,
                                DBR dbr, String msg ) {

    super( ctxt );

    _channel=ch;
    _dbrType=dbrType;
    _count=count;
    _dbr=dbr;
    _msg= msg;
  }

  /**
   * The Channel associated with the exception if any.
   * @return the Channel or null if none.
   */

  public Channel getChannel() {
    return _channel;
  }

  /**
   * Returns the type of the request when the failure occured.
   */

  public DBRType getDBRType() {
    return _dbrType;
  }

  /**
   * Returns the element count of the request when the failure occured.
   */

  public int getCount() {
    return _count;
  }

  /**
   * Returns the DBR object of the request when the failure occured.
   * Only valid for GET operations.
   * @return the DBR object or null if none.
   */

  public DBR getDBR() {
    return _dbr;
  }

  public String getMessage() {
    return _msg;
  }

}
