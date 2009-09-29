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
 * $Id: AccessRightsListener.java,v 1.1 2004-08-11 15:50:32 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.event;


/**
 * The listener interface for receiving AccessRightsEvents.
 *
 * @see gov.aps.jca.Channel
 */
public interface AccessRightsListener extends java.util.EventListener {

  /**
   * This method is called whenever the access rights of the Channel(s), with which the listener is registered, changed.
   *
   * @param ev the AccessRightsEvent representing the change.
   */
  public void accessRightsChanged(AccessRightsEvent ev);

}





