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
 * $Id: GR.java,v 1.4 2006-08-30 12:17:26 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */





package gov.aps.jca.dbr;

/*
 * NOTE: CA server needs GR to implement TIME.
 */
public interface GR extends /*STS*/ TIME {
  static final public String  EMPTYUNIT="";
  static final public Byte    ZEROB= new Byte((byte)0);
  static final public Short   ZEROS= new Short((short)0);
  static final public Integer ZEROI= new Integer((int)0);
  static final public Float   ZEROF= new Float(0.0);
  static final public Double  ZEROD= new Double(0.0);

  /**
   * Get units.
   * @return get units, <code>non-null</code>.
   */
  public String getUnits();
  public void   setUnits(String unit);
  /**   * Get upper display limit.   * @return upper display limit, <code>non-null</code>.   */
  public Number getUpperDispLimit();
  public void   setUpperDispLimit(Number limit);
  /**   * Get lower display limit.   * @return lower display limit, <code>non-null</code>.   */  public Number getLowerDispLimit();
  public void   setLowerDispLimit(Number limit);
  /**   * Get upper alarm limit.   * @return upper alarm limit, <code>non-null</code>.   */  public Number getUpperAlarmLimit();
  public void   setUpperAlarmLimit(Number limit);
  /**   * Get upper warning limit.   * @return upper warning limit, <code>non-null</code>.   */  public Number getUpperWarningLimit();
  public void   setUpperWarningLimit(Number limit);
  /**   * Get lower warning limit.   * @return lower warning limit, <code>non-null</code>.   */  public Number getLowerWarningLimit();
  public void   setLowerWarningLimit(Number limit);
  /**   * Get lower alarm limit.   * @return lower alarm limit, <code>non-null</code>.   */  public Number getLowerAlarmLimit();
  public void   setLowerAlarmLimit(Number limit);
}

