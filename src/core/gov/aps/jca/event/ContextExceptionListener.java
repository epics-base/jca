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
 * $Id: ContextExceptionListener.java,v 1.2 2006-05-24 14:09:01 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.event;

/**
 * The listener interface for receiving ContextExceptionEvents.
 *
 * @see gov.aps.jca.Context
 */
public interface ContextExceptionListener extends java.util.EventListener {

  public void contextException(ContextExceptionEvent ev);
  public void contextVirtualCircuitException(ContextVirtualCircuitExceptionEvent ev);

}





