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
 * $Id: DBR_String.java,v 1.6 2006-08-30 19:02:39 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.dbr;

import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;

public class DBR_String extends DBR implements STRING {
  static public final DBRType TYPE= new DBRType("DBR_STRING", 0, DBR_String.class);
  
  public DBR_String() {

    this( 1 );

  }

  public DBR_String( int count ) {

    this( new String[count] );

  }

  public DBR_String( byte[] value ) {

    this( new String( value ) );

  }

  public DBR_String( String value ) {

    this( new String[] {value} );

  }

  public DBR_String( char[] value ) {

    this( new String( value ) );

  }

  public DBR_String( String[] value ) {
    super( value );
  }

  public DBRType getType() {
    return TYPE;
  }



  public String[] getStringValue() {
    return( String[] )getValue();
  }

	/**
	 * @see gov.aps.jca.dbr.DBR#convert(gov.aps.jca.dbr.DBRType)
	 */
	public DBR convert(DBRType convertType) throws CAStatusException {
		return convert(convertType, null);
	}

	/**
	 * @see gov.aps.jca.dbr.DBR#convert(gov.aps.jca.dbr.DBRType, Object)
	 */
	public DBR convert(DBRType convertType, Object params) throws CAStatusException {
		
		// no conversion needed
		if (convertType.isSTRING() && convertType.getValue() <= getType().getValue())
			return this;

		final String[] fromValue = getStringValue();
		DBR dbr = DBRFactory.create(convertType, _count);
	
		try
		{
			
			if (convertType.isSTRING())
			{
				String[] toValue = ((DBR_String)dbr).getStringValue();
				for (int i = 0; i < _count; i++)
					toValue[i] = String.valueOf(fromValue[i]);
			}
			else if (convertType.isSHORT())
			{
				short[] toValue = ((DBR_Short)dbr).getShortValue();
				for (int i = 0; i < _count; i++)
					toValue[i] = Short.parseShort(fromValue[i]);
			}
			else if (convertType.isFLOAT())
			{
				float[] toValue = ((DBR_Float)dbr).getFloatValue();
				for (int i = 0; i < _count; i++)
					toValue[i] = Float.parseFloat(fromValue[i]);
			}
			else if (convertType.isENUM())
			{
				short[] toValue = ((DBR_Enum)dbr).getEnumValue();

				String[] labels = (String[])params;
				
				// check if any
				if (labels == null)
					throw new CAStatusException(CAStatus.NOCONVERT, "no labels provided");
				
				for (int i = 0; i < _count; i++)
				{
					short pos = 0;
					for (; pos < (short)labels.length; pos++)
						if (labels[pos] != null && labels[pos].equals(fromValue[i]))
							break;
					
					// found
					if (pos < labels.length)
						toValue[i] = pos;
					else
						throw new CAStatusException(CAStatus.NOCONVERT, "'" + fromValue[i] + "' is not a valid choice");
						
				}

				/*
				for (int i = 0; i < _count; i++)
					toValue[i] = Short.parseShort(fromValue[i]);
				*/
			} 
			else if (convertType.isBYTE())
			{
				byte[] toValue = ((DBR_Byte)dbr).getByteValue();
				for (int i = 0; i < _count; i++)
					toValue[i] = Byte.parseByte(fromValue[i]);
			} 
			else if (convertType.isINT())
			{
				int[] toValue = ((DBR_Int)dbr).getIntValue();
				for (int i = 0; i < _count; i++)
					toValue[i] = Integer.parseInt(fromValue[i]);
			}
			else if (convertType.isDOUBLE())
			{
				double[] toValue = ((DBR_Double)dbr).getDoubleValue();
				for (int i = 0; i < _count; i++)
					toValue[i] = Double.parseDouble(fromValue[i]);
			}
			else
			{
				throw new CAStatusException(CAStatus.NOCONVERT, "converstion not supported");
			}
		} catch (NumberFormatException nfr) {
			throw new CAStatusException(CAStatus.NOCONVERT);
		}
		
		return dbr;
	}

}
