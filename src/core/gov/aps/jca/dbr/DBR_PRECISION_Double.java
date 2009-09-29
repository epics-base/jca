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
 * $Id: DBR_PRECISION_Double.java,v 1.2 2006-08-29 13:35:08 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.dbr;

import gov.aps.jca.CAStatusException;

import java.io.*;

/**
 * This is an "artificial" DBR type to support TIME type with PRECISION. 
 * @author msekoranja
 *
 */
public class DBR_PRECISION_Double extends DBR_TIME_Double implements PRECISION {

	protected short _precision;

  public DBR_PRECISION_Double() {

    this( 1 );

  }

  public DBR_PRECISION_Double( int count ) {

    this( new double[count] );

  }

  public DBR_PRECISION_Double( double[] value ) {
    super( value );
  }

  public short getPrecision() {
    return _precision;
  }

  public void setPrecision( short prec ) {
    _precision=prec;
  }


  public void printInfo( PrintStream out ) {

    super.printInfo( out );

    out.println( "PRECISION: "+getPrecision() );

  }

	/**
	 * @see gov.aps.jca.dbr.DBR_Byte#convert(gov.aps.jca.dbr.DBRType)
	 */
	public DBR convert(DBRType convertType) throws CAStatusException {
		DBR dbr = super.convert(convertType);
	
		if (dbr.isPRECSION())
		{
			PRECISION prec = (PRECISION)dbr;
			prec.setPrecision(getPrecision());
		}

		return dbr;
	}

}
