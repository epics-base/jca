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
 * $Id: CTRL.java,v 1.4 2006-08-30 10:58:12 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */



package gov.aps.jca.dbr;


public interface CTRL extends GR {
  /**   * Get upper control limit.   * @return upper control limit, <code>non-null</code>.   */  public Number getUpperCtrlLimit();
  public void   setUpperCtrlLimit(Number limit);

  /**   * Get lower control limit.   * @return lower control limit, <code>non-null</code>.   */  public Number getLowerCtrlLimit();
  public void   setLowerCtrlLimit(Number limit);
}

