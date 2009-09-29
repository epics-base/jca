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
 * $Id: DBR_LABELS_Enum.java,v 1.2 2006-08-30 18:24:00 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.dbr;

import gov.aps.jca.CAStatusException;

import java.io.*;

public class DBR_LABELS_Enum extends DBR_STS_Enum implements LABELS {
  static public final DBRType TYPE= new DBRType("DBR_LABELS_ENUM", 24, DBR_LABELS_Enum.class);
  
  
  protected String[] _strs;

  public DBR_LABELS_Enum() {

    this( 1 );

  }

  public DBR_LABELS_Enum( int count ) {

    this( new short[count] );

  }

  public DBR_LABELS_Enum( short[] value ) {
    super( value );
  }

  public DBRType getType() {
    return TYPE;
  }



  public String[] getLabels() {
    return _strs;
  }

  public void setLabels( String[] strs ) {
    _strs=strs;
  }

  public void printInfo( PrintStream out ) {

    super.printInfo( out );

    out.print( "LABELS   : " );

    printValue( getLabels(), out );

    out.println();

  }

  	/**
	 * @see gov.aps.jca.dbr.DBR_Byte#convert(gov.aps.jca.dbr.DBRType)
	 */
	public DBR convert(DBRType convertType) throws CAStatusException {
		DBR dbr = super.convert(convertType);
	
		if (dbr.isLABELS()) 
		{
			LABELS labels = (LABELS)dbr;
			labels.setLabels(getLabels());
		}	

		return dbr;
	}

}
