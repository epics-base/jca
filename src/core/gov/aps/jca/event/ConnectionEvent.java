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
 * $Id: ConnectionEvent.java,v 1.2 2004-08-12 07:24:05 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.event;

import gov.aps.jca.*;

/**
 * An event which indicates a change in a Channel connection's state.
 *
 * @see Channel
 */

public class ConnectionEvent extends CAEvent {

  protected boolean _isConnected;

  public ConnectionEvent( Channel channel, boolean connected ) {
    super( channel );
    _isConnected=connected;
  }

  public boolean isConnected() {
    return _isConnected;
  }
}
