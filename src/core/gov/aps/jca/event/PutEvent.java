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
 * $Id: PutEvent.java,v 1.1 2004-08-11 15:50:32 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.event;

import gov.aps.jca.*;
import gov.aps.jca.dbr.*;

/**
 * The listener interface for receiving PutEvents.
 *
 * @see gov.aps.jca.Channel
 */

public class PutEvent extends CAEvent {

  protected DBRType _type;

  protected int _count;

  protected CAStatus _status;

  public PutEvent( Channel channel, DBRType type, int count, CAStatus status ) {
    super( channel );
    _type=type;
    _count=count;
    _status=status;
  }

  /**
   * Returns the type of the put request. One of DBR_XXX.
   */
  public DBRType getType() {
    return _type;
  }

  /**
   * Returns the number of elements actualy written.
   */
  public int getCount() {
    return _count;
  }

  /**
   * Returns the status of the operation.
   */
  public CAStatus getStatus() {
    return _status;
  }
}
