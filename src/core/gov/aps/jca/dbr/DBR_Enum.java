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
 * $Id: DBR_Enum.java,v 1.5 2006-08-30 18:24:39 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.dbr;

import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;

public class DBR_Enum extends DBR implements ENUM {
  static public final DBRType TYPE= new DBRType("DBR_ENUM", 3, DBR_Enum.class);
  
  public DBR_Enum() {

    this( 1 );

  }

  public DBR_Enum( int count ) {

    this( new short[count] );

  }

  public DBR_Enum( short[] value ) {
    super( value );
  }

  public DBRType getType() {
    return TYPE;
  }


  public short[] getEnumValue() {
    return( short[] )getValue();
  }

	/**
	 * @see gov.aps.jca.dbr.DBR#convert(gov.aps.jca.dbr.DBRType)
	 */
	public DBR convert(DBRType convertType) throws CAStatusException {
		
		// no conversion needed
		if (convertType.isENUM() && convertType.getValue() <= getType().getValue())
			return this;

		final short[] fromValue = getEnumValue();
		DBR dbr = DBRFactory.create(convertType, _count);
		
		if (convertType.isSTRING())
		{
			String[] toValue = ((DBR_String)dbr).getStringValue();

			String[] labels = null;
			if (isLABELS())
				labels = ((LABELS)this).getLabels();
			
			final String EMPTY = "";
			
			// set empty labels as default
			if (labels == null)
			{
				for (int i = 0; i < _count; i++)
					toValue[i] = EMPTY;
			}
			else
			{
				for (int i = 0; i < _count; i++)
				{
					if (fromValue[i] < labels.length)
						toValue[i] = labels[fromValue[i]];
					
					// do not allow nulls
					if (toValue[i] == null)
						toValue[i] = EMPTY;
				}
			}
		}
		else if (convertType.isSHORT())
		{
			short[] toValue = ((DBR_Short)dbr).getShortValue();
			for (int i = 0; i < _count; i++)
				toValue[i] = (short)fromValue[i];
		}
		else if (convertType.isFLOAT())
		{
			float[] toValue = ((DBR_Float)dbr).getFloatValue();
			for (int i = 0; i < _count; i++)
				toValue[i] = (float)fromValue[i];
		} 
		else if (convertType.isENUM())
		{
			short[] toValue = ((DBR_Enum)dbr).getEnumValue();
			for (int i = 0; i < _count; i++)
				toValue[i] = (short)fromValue[i];
		} 
		else if (convertType.isBYTE())
		{
			byte[] toValue = ((DBR_Byte)dbr).getByteValue();
			for (int i = 0; i < _count; i++)
				toValue[i] = (byte)fromValue[i];
		} 
		else if (convertType.isINT())
		{
			int[] toValue = ((DBR_Int)dbr).getIntValue();
			for (int i = 0; i < _count; i++)
				toValue[i] = (int)fromValue[i];
		}
		else if (convertType.isDOUBLE())
		{
			double[] toValue = ((DBR_Double)dbr).getDoubleValue();
			for (int i = 0; i < _count; i++)
				toValue[i] = (double)fromValue[i];
		}
		else
		{
			throw new CAStatusException(CAStatus.NOCONVERT, "converstion not supported");
		}
		
		return dbr;
	}
  
}
