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
 * $Id: DBR_STS_Enum.java,v 1.2 2006-08-16 15:21:51 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.dbr;

import gov.aps.jca.CAStatusException;

import java.io.*;

public class DBR_STS_Enum extends DBR_Enum implements STS {
  static public final DBRType TYPE= new DBRType("DBR_STS_ENUM",  10, DBR_STS_Enum.class);
  
  
  protected Status _status;

  protected Severity _severity;

  public DBR_STS_Enum() {

    this( 1 );

  }

  public DBR_STS_Enum( int count ) {

    this( new short[count] );

  }

  public DBR_STS_Enum( short[] value ) {
    super( value );
  }

  public DBRType getType() {
    return TYPE;
  }


  public Status getStatus() {
    return _status;
  }

  public void setStatus( int status ) {
    setStatus(Status.forValue(status));
  }

  public void setStatus(Status status) {
    if(status==null) throw new IllegalArgumentException("Illegal status: null");
    _status=status;
  }
  
  
  public Severity getSeverity() {
    return _severity;
  }

  public void setSeverity( int severity ) {
    setSeverity(Severity.forValue(severity));
  }

  public void setSeverity(Severity severity) {
    if(severity==null) throw new IllegalArgumentException("Illegal severity: null");
    _severity=severity;
  }
  
  public void printInfo( PrintStream out ) {

    super.printInfo( out );

    out.println( "STATUS   : "+getStatus().getName() );
    out.println( "SEVERITY : "+getSeverity().getName() );

  }

	/**
	 * @see gov.aps.jca.dbr.DBR_Byte#convert(gov.aps.jca.dbr.DBRType)
	 */
	public DBR convert(DBRType convertType) throws CAStatusException {
		DBR dbr = super.convert(convertType);
	
		if (dbr.isSTS()) 
		{
			STS sts = (STS)dbr;
			sts.setSeverity(_severity);
			sts.setStatus(_status);
		}	

		return dbr;
	}


}
