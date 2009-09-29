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
 * $Id: ContextMessageEvent.java,v 1.1 2004-08-11 15:50:32 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.event;

import gov.aps.jca.*;

/**
 * An event which indicates an asynchronous message from a Context.
 *
 * @see gov.aps.jca.Context
 */

public class ContextMessageEvent extends CAEvent {

  String _msg;

  public ContextMessageEvent( Context ctxt, String msg ) {

    super( ctxt );

    _msg=msg;

  }

  /**
   * Returns the String containing the message from the Context.
   */

  public String getMessage() {

    return _msg;

  }

}
