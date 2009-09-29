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
 * $Id: DBR_STSACK_String.java,v 1.3 2007-01-21 15:54:36 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.dbr;

import gov.aps.jca.CAStatusException;

import java.io.*;

public class DBR_STSACK_String extends DBR_STS_String implements ACK {
  public static final DBRType TYPE= new DBRType("DBR_STSACK_STRING", 37, DBR_STSACK_String.class);
  
  
  protected boolean _ackt;

  protected Severity _acks;

  public DBR_STSACK_String() {

    this( 1 );

  }

  public DBR_STSACK_String( int count ) {

    this( new String[count] );

  }

  public DBR_STSACK_String( String[] value ) {
    super( value );
  }

  public DBRType getType() {
    return TYPE;
  }



  public Severity getAckS() {
    return _acks;
  }

  public void setAckS( Severity acks ) {
    _acks=acks;
  }

  public boolean getAckT() {
    return _ackt;
  }

  public void setAckT( boolean ackt ) {
    _ackt=ackt;
  }

  public void setAckS( int acks ) {
    setAckS(Severity.forValue(acks));
  }

  public void setAckT( int ackt ) {
    setAckT(ackt != 0);
  }

  public void printInfo( PrintStream out ) {

    super.printInfo( out );

    out.println( "ACKS     : "+getAckS() );

    out.println( "ACKT     : "+getAckT() );

  }


	/**
	 * @see gov.aps.jca.dbr.DBR_Byte#convert(gov.aps.jca.dbr.DBRType)
	 */
	public DBR convert(DBRType convertType) throws CAStatusException {
		DBR dbr = super.convert(convertType);
	
		// STS is handled by super
		
		if (dbr instanceof ACK) 
		{
			ACK ack = (ACK)dbr;
			ack.setAckS(_acks);
			ack.setAckT(_ackt);
		}	

		return dbr;
	}


}
