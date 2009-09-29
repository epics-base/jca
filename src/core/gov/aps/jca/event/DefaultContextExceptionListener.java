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

 * $Id: DefaultContextExceptionListener.java,v 1.3 2006-08-02 16:01:35 msekoranja Exp $

 *

 * Modification Log:

 * 01. 05/07/2003  erb  initial development

 *

 */





package gov.aps.jca.event;





/**

 * A default implementation for ContextExceptionListener.

 * It prints out the exception to the standard err stream.

 *

 * @see gov.aps.jca.Context

 */

public class DefaultContextExceptionListener implements ContextExceptionListener {

  public void contextException(ContextExceptionEvent ev) {
    System.err.println("Exception form context '" + ev.getSource() + "', message: " + ev.getMessage());
  }

  public void contextVirtualCircuitException(ContextVirtualCircuitExceptionEvent ev) {
  	// noop
  }
  
}

