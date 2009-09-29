/**********************************************************************
 *
 *      Original Author: Eric Boucher
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
 * $Id: CAException.java,v 1.1 2004-08-11 15:50:32 msekoranja Exp $
 */


package gov.aps.jca;


/**
 * Base class for all JCA Exception
 */
public class CAException extends Exception {

  public CAException() {
    super();
  }
  public CAException( String msg ) {
    super( msg );
  }
  public CAException( String msg, Throwable cause ) {
    super( msg, cause );
  }
  public CAException( Throwable cause ) {
    super( cause );
  }
}