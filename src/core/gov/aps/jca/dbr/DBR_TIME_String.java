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
 * $Id: DBR_TIME_String.java,v 1.3 2006-08-30 17:25:08 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.dbr;

import gov.aps.jca.CAStatusException;

import java.io.*;

public class DBR_TIME_String extends DBR_STS_String implements TIME {
  static public final DBRType TYPE= new DBRType("DBR_TIME_STRING", 14, DBR_TIME_String.class);
  
  protected TimeStamp _stamp;

  public DBR_TIME_String() {

    this( 1 );

  }

  public DBR_TIME_String( int count ) {
    this( new String[count] );
  }

  public DBR_TIME_String( String[] value ) {
    super( value );
  }

  public DBRType getType() {
    return TYPE;
  }


  public TimeStamp getTimeStamp() {
    return _stamp;
  }

  public void setTimeStamp( TimeStamp stamp ) {
    _stamp=stamp;
  }

  public void printInfo( PrintStream out ) {

    super.printInfo( out );

    if (!(this instanceof DBR_GR_String))
    	out.println( "TIME     : "+getTimeStamp().toMMDDYY() );

  }

	/**
	 * @see gov.aps.jca.dbr.DBR_Byte#convert(gov.aps.jca.dbr.DBRType)
	 */
	public DBR convert(DBRType convertType) throws CAStatusException {
		DBR dbr = super.convert(convertType);
	
		if (dbr.isTIME())
			((TIME)dbr).setTimeStamp(_stamp);
			
		return dbr;
	}

}
