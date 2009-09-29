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
 * $Id: DBR_GR_Double.java,v 1.4 2006-08-30 17:25:08 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.dbr;

import gov.aps.jca.CAStatusException;

import java.io.*;

//NOTE: for CAS DBR_TIME_<type> is used instead of DBR_STS_<type>
public class DBR_GR_Double extends DBR_TIME_Double implements GR, PRECISION {
  static public final DBRType TYPE= new DBRType("DBR_GR_DOUBLE", 27, DBR_GR_Double.class);
  
  
  protected String _unit=EMPTYUNIT;

  protected Double _udl=ZEROD;

  protected Double _ldl=ZEROD;

  protected Double _ual=ZEROD;

  protected Double _uwl=ZEROD;

  protected Double _lwl=ZEROD;

  protected Double _lal=ZEROD;

  protected short _precision;

  public DBR_GR_Double() {

    this( 1 );

  }

  public DBR_GR_Double( int count ) {

    this( new double[count] );

  }

  public DBR_GR_Double( double[] value ) {
    super( value );
  }

  public DBRType getType() {
    return TYPE;
  }



  public short getPrecision() {
    return _precision;
  }

  public void setPrecision( short prec ) {
    _precision=prec;
  }

  public String getUnits() {
    return _unit;
  }

  public void setUnits( String unit ) {
    _unit=unit;
  }

  public Number getUpperDispLimit() {
    return _udl;
  }

  public void setUpperDispLimit( Number limit ) {
    _udl=new Double( limit.doubleValue() );
  }

  public Number getLowerDispLimit() {
    return _ldl;
  }

  public void setLowerDispLimit( Number limit ) {
    _ldl=new Double( limit.doubleValue() );
  }

  public Number getUpperAlarmLimit() {
    return _ual;
  }

  public void setUpperAlarmLimit( Number limit ) {
    _ual=new Double( limit.doubleValue() );
  }

  public Number getUpperWarningLimit() {
    return _uwl;
  }

  public void setUpperWarningLimit( Number limit ) {
    _uwl=new Double( limit.doubleValue() );
  }

  public Number getLowerWarningLimit() {
    return _lwl;
  }

  public void setLowerWarningLimit( Number limit ) {
    _lwl=new Double( limit.doubleValue() );
  }

  public Number getLowerAlarmLimit() {
    return _lal;
  }

  public void setLowerAlarmLimit( Number limit ) {
    _lal=new Double( limit.doubleValue() );
  }

  public void printInfo( PrintStream out ) {

    super.printInfo( out );

    out.println( "PRECISION: "+getPrecision() );

    out.println( "UNITS    : "+getUnits() );

    out.println( "UDL      : "+getUpperDispLimit() );

    out.println( "LDL      : "+getLowerDispLimit() );

    out.println( "UAL      : "+getUpperAlarmLimit() );

    out.println( "UWL      : "+getUpperWarningLimit() );

    out.println( "LWL      : "+getLowerWarningLimit() );

    out.println( "LAL      : "+getLowerAlarmLimit() );

  }

	/**
	 * @see gov.aps.jca.dbr.DBR_Byte#convert(gov.aps.jca.dbr.DBRType)
	 */
	public DBR convert(DBRType convertType) throws CAStatusException {
		DBR dbr = super.convert(convertType);
	
		if (dbr.isGR()) 
		{
			GR gr = (GR)dbr;
			
			gr.setUnits(getUnits());

			// no conversion actually needed, all is Number
			gr.setUpperDispLimit(getUpperDispLimit());
			gr.setLowerDispLimit(getLowerDispLimit());
			gr.setUpperAlarmLimit(getUpperAlarmLimit());
			gr.setUpperWarningLimit(getUpperWarningLimit());
			gr.setLowerWarningLimit(getLowerWarningLimit());
			gr.setLowerAlarmLimit(getLowerAlarmLimit());
		}	

		if (dbr.isPRECSION())
		{
			PRECISION prec = (PRECISION)dbr;
			prec.setPrecision(getPrecision());
		}

		return dbr;
	}


	/**
	 * NOTE: this method will always return <code>null</code> timestamp if
	 * called from CA client. <code>GR</code> DBR type does not carry
	 * timestamp. This method is used only by CA Server side.
	 */
	public TimeStamp getTimeStamp() {
		return super.getTimeStamp();
	}

	/**
	 * Set timestamp. To be used only by CA Server side.
	 */
	public void setTimeStamp(TimeStamp stamp) {
		super.setTimeStamp(stamp);
	}

}
