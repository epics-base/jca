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
 * $Id: MonitorEvent.java,v 1.1 2004-08-11 15:50:32 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.event;

import gov.aps.jca.*;
import gov.aps.jca.dbr.*;

/**
 * The listener interface for receiving MonitorEvents.
 *
 * @see gov.aps.jca.Channel
 */

public class MonitorEvent extends CAEvent {

  protected DBR _dbr;

  protected CAStatus _status;

  public MonitorEvent( Channel channel, DBR dbr, CAStatus status ) {
    super( channel );
    _dbr=dbr;
    _status=status;
  }

  public DBR getDBR() {
    return _dbr;
  }

  /**
   * Returns the status of the operation.
   */
  public CAStatus getStatus() {
    return _status;
  }

}
