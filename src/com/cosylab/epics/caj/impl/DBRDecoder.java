/*
 * Copyright (c) 2004 by Cosylab
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file "LICENSE-CAJ". If the license is not included visit Cosylab web site,
 * <http://www.cosylab.com>.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package com.cosylab.epics.caj.impl;

import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_Byte;
import gov.aps.jca.dbr.DBR_CTRL_Byte;
import gov.aps.jca.dbr.DBR_CTRL_Double;
import gov.aps.jca.dbr.DBR_CTRL_Enum;
import gov.aps.jca.dbr.DBR_CTRL_Float;
import gov.aps.jca.dbr.DBR_CTRL_Int;
import gov.aps.jca.dbr.DBR_CTRL_Short;
import gov.aps.jca.dbr.DBR_CTRL_String;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.DBR_Enum;
import gov.aps.jca.dbr.DBR_Float;
import gov.aps.jca.dbr.DBR_GR_Byte;
import gov.aps.jca.dbr.DBR_GR_Double;
import gov.aps.jca.dbr.DBR_GR_Float;
import gov.aps.jca.dbr.DBR_GR_Int;
import gov.aps.jca.dbr.DBR_GR_Short;
import gov.aps.jca.dbr.DBR_GR_String;
import gov.aps.jca.dbr.DBR_Int;
import gov.aps.jca.dbr.DBR_LABELS_Enum;
import gov.aps.jca.dbr.DBR_PutAckS;
import gov.aps.jca.dbr.DBR_PutAckT;
import gov.aps.jca.dbr.DBR_STSACK_String;
import gov.aps.jca.dbr.DBR_STS_Byte;
import gov.aps.jca.dbr.DBR_STS_Double;
import gov.aps.jca.dbr.DBR_STS_Enum;
import gov.aps.jca.dbr.DBR_STS_Float;
import gov.aps.jca.dbr.DBR_STS_Int;
import gov.aps.jca.dbr.DBR_STS_Short;
import gov.aps.jca.dbr.DBR_STS_String;
import gov.aps.jca.dbr.DBR_Short;
import gov.aps.jca.dbr.DBR_String;
import gov.aps.jca.dbr.DBR_TIME_Byte;
import gov.aps.jca.dbr.DBR_TIME_Double;
import gov.aps.jca.dbr.DBR_TIME_Enum;
import gov.aps.jca.dbr.DBR_TIME_Float;
import gov.aps.jca.dbr.DBR_TIME_Int;
import gov.aps.jca.dbr.DBR_TIME_Short;
import gov.aps.jca.dbr.DBR_TIME_String;
import gov.aps.jca.dbr.GR;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;

import java.nio.ByteBuffer;

/**
 * DBR decoder - deserializes data payload.
 * 
 * N  N  O  TTT EEE 
 * NN N O O  T  E_      due to exisiting JCA API (strict constructors, n/a setters), this class is very messy...
 * N NN O O  T  E       sth like DBRType.create(Object data) is missing...
 * N  N  O   T  EEE
 * 
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class DBRDecoder {

	/**
	 * Get (deserialize) data payload buffer to DBR structure.
	 * @param inDBR	DBR to be used (set), do not create a new instance of DBR if non-<code>null</code>.
	 * 				DBR type and count must match.
	 * @param dataTypeValue	data type, see <code>DBRType</code>.
	 * @param dataCount	data type.
	 * @param dataPayloadBuffer		data payload buffer received from server, non-<code>null</code>.
	 * @return DBR structure, <code>null</code> in case of error.
	 */
	// TODO optimize if (use fast lookup table)
	public static DBR getDBR(DBR inDBR, short dataTypeValue, int dataCount, ByteBuffer dataPayloadBuffer)
	{
		try
		{
			// check if valid
			DBRType dataType = DBRType.forValue(dataTypeValue);
			if (dataType == null)
				throw new IllegalArgumentException("Invalid data type.");
				
			// DBR consistency check
			if (inDBR != null)
			{
				if (inDBR.getType() != dataType || inDBR.getCount() != dataCount)
					throw new IllegalArgumentException("Encoded DBR and expected DBR are not consistant.");
			}
			
			// invalid type
			if (dataTypeValue < DBR_String.TYPE.getValue())
				return null;
			
			// normal (value only)
			else if (dataTypeValue < DBR_STS_String.TYPE.getValue())
				return getValueDBR(inDBR, dataType, dataCount, dataPayloadBuffer);
			
			// status
			else if (dataTypeValue < DBR_TIME_String.TYPE.getValue())
				return getStatusDBR(inDBR, dataType, dataCount, dataPayloadBuffer);
			
			// time
			else if (dataTypeValue < DBR_GR_Short.TYPE.getValue() - 1)
				return getTimeDBR(inDBR, dataType, dataCount, dataPayloadBuffer);
	
			// graphics (there is no string)
			else if (dataTypeValue < DBR_CTRL_Short.TYPE.getValue() - 1)
				return getGraphicsDBR(inDBR, dataType, dataCount, dataPayloadBuffer);
	
			// control
			else if (dataTypeValue < DBR_PutAckT.TYPE.getValue())
				return getControlDBR(inDBR, dataType, dataCount, dataPayloadBuffer);
				
			// ackT
			else if (dataType == DBR_PutAckT.TYPE)
				return getAckTDBR(inDBR, dataCount, dataPayloadBuffer);
	
			// ackS
			else if (dataType == DBR_PutAckS.TYPE)
				return getAckSDBR(inDBR, dataCount, dataPayloadBuffer);
	
			// stsAckString
			else if (dataType == DBR_STSACK_String.TYPE)
				return getStatusAckStringDBR(inDBR, dataCount, dataPayloadBuffer);
	
			// NOTE: DBR_CLASS_NAME not supported
			else // if (dataType > DBR_STSACK_String.TYPE.getValue())
				return null;
		}
		catch (Throwable th)
		{
			// TODO remove
			th.printStackTrace();
			return null;
		}
	}
	
	/**
	 * @param inDBR	DBR to be used (set), do not create a new instance of DBR if non-<code>null</code>.
	 * @param dataType
	 * @param dataCount
	 * @param dataPayloadBuffer
	 * @return decoded DBR.
	 */
	private static DBR getValueDBR(DBR inDBR, DBRType dataType, int dataCount, ByteBuffer dataPayloadBuffer)
	{
		DBR retVal = inDBR;
		
		if (dataType == DBR_String.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_String((String[])readValues(null, DBR_String.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(retVal, DBR_String.TYPE, dataCount, dataPayloadBuffer);	
		}
		else if (dataType == DBR_Short.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_Short((short[])readValues(null, DBR_Short.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(retVal, DBR_Short.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_Float.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_Float((float[])readValues(null, DBR_Float.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(retVal, DBR_Float.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_Enum.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_Enum((short[])readValues(null, DBR_Enum.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(retVal, DBR_Enum.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_Byte.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_Byte((byte[])readValues(null, DBR_Byte.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(retVal, DBR_Byte.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_Int.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_Int((int[])readValues(null, DBR_Int.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(retVal, DBR_Int.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_Double.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_Double((double[])readValues(null, DBR_Double.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(retVal, DBR_Double.TYPE, dataCount, dataPayloadBuffer);
		}

		return retVal;
	}

	/**
	 * No bound checking is done (payload is trusted to be OK, otherwise an exception will be thown and later handled.
	 * @param inDBR	DBR to be used (set), do not create a new instance of DBR if non-<code>null</code>.
	 * @param dataType	only plain types are aaccepted.
	 * @param dataCount
	 * @param dataPayloadBuffer
	 * @return decoded DBR.
	 */
	private static Object readValues(DBR inDBR, DBRType dataType, int dataCount, ByteBuffer dataPayloadBuffer)
	{
		if (dataType == DBR_String.TYPE)
		{
			String[] arr;
			if (inDBR != null)
				arr = (String[])inDBR.getValue();
			else
				arr = new String[dataCount];

			if (dataCount == 1) {
				byte[] data = dataPayloadBuffer.array();
				int start = dataPayloadBuffer.position();
				final int bufferEnd = dataPayloadBuffer.limit();
				int end = start;
				
				// find zero char (string terminator)
				while (data[end] != 0 && end < bufferEnd)
					end++;
				// NOTE: rest of the bytes are left in the buffer
	
				arr[0] = new String(data, start, end-start);
			}
			else 
			{
			    byte[] rawBuffer = new byte[CAConstants.MAX_STRING_SIZE];
				for (int i = 0; i < dataCount; i++)
				{
					dataPayloadBuffer.get(rawBuffer);
					arr[i] = extractString(rawBuffer);
				}
			}
			
			return arr;
		}
		else if (dataType == DBR_Short.TYPE)
		{
			short[] arr;
			if (inDBR != null)
				arr = (short[])inDBR.getValue();
			else
				arr = new short[dataCount];

			for (int i = 0; i < dataCount; i++)
				arr[i] = dataPayloadBuffer.getShort();
			return arr;
		}
		else if (dataType == DBR_Float.TYPE)
		{
			float[] arr;
			if (inDBR != null)
				arr = (float[])inDBR.getValue();
			else
				arr = new float[dataCount];

			for (int i = 0; i < dataCount; i++)
				arr[i] = dataPayloadBuffer.getFloat();
			return arr;
		}
		else if (dataType == DBR_Enum.TYPE)
		{
			// enum is short (16-bit integer)
			short[] arr;
			if (inDBR != null)
				arr = (short[])inDBR.getValue();
			else
				arr = new short[dataCount];

			for (int i = 0; i < dataCount; i++)
				arr[i] = dataPayloadBuffer.getShort();
			return arr;
		}
		else if (dataType == DBR_Byte.TYPE)
		{
			byte[] arr;
			if (inDBR != null)
				arr = (byte[])inDBR.getValue();
			else
				arr = new byte[dataCount];

			for (int i = 0; i < dataCount; i++)
				arr[i] = dataPayloadBuffer.get();
			return arr;
		}
		else if (dataType == DBR_Int.TYPE)
		{
			int[] arr;
			if (inDBR != null)
				arr = (int[])inDBR.getValue();
			else
				arr = new int[dataCount];

			for (int i = 0; i < dataCount; i++)
				arr[i] = dataPayloadBuffer.getInt();
			return arr;
		}
		else if (dataType == DBR_Double.TYPE)
		{
			double[] arr;
			if (inDBR != null)
				arr = (double[])inDBR.getValue();
			else
				arr = new double[dataCount];

			for (int i = 0; i < dataCount; i++)
				arr[i] = dataPayloadBuffer.getDouble();
			return arr;
		}
		else
			return null;
	}

	/**
	 * @param inDBR	DBR to be used (set), do not create a new instance of DBR if non-<code>null</code>.
	 * @param dataType
	 * @param dataCount
	 * @param dataPayloadBuffer
	 * @return decoded DBR.
	 */
	private static DBR getStatusDBR(DBR inDBR, DBRType dataType, int dataCount, ByteBuffer dataPayloadBuffer)
	{
		STS retVal = (STS)inDBR;
		
		int status = dataPayloadBuffer.getShort() & 0xFFFF;
		int severity = dataPayloadBuffer.getShort() & 0xFFFF;
		
		if (dataType == DBR_STS_String.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_STS_String((String[])readValues(null, DBR_String.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_String.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_GR_String.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_GR_String((String[])readValues(null, DBR_String.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_String.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_CTRL_String.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_CTRL_String((String[])readValues(null, DBR_String.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_String.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_STS_Short.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_STS_Short((short[])readValues(null, DBR_Short.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Short.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_STS_Float.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_STS_Float((float[])readValues(null, DBR_Float.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Float.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_STS_Enum.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_STS_Enum((short[])readValues(null, DBR_Enum.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Enum.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_STS_Byte.TYPE)
		{
			// RISC padding
			dataPayloadBuffer.get();
			if (retVal == null)
				retVal = new DBR_STS_Byte((byte[])readValues(null, DBR_Byte.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Byte.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_STS_Int.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_STS_Int((int[])readValues(null, DBR_Int.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Int.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_STS_Double.TYPE)
		{
			// RISC padding
			dataPayloadBuffer.getInt();
			if (retVal == null)
				retVal = new DBR_STS_Double((double[])readValues(null, DBR_Double.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Double.TYPE, dataCount, dataPayloadBuffer);
		}

		retVal.setStatus(status);
		retVal.setSeverity(severity);
		
		return (DBR)retVal;
	}

	/**
	 * @param inDBR	DBR to be used (set), do not create a new instance of DBR if non-<code>null</code>.
	 * @param dataType
	 * @param dataCount
	 * @param dataPayloadBuffer
	 * @return decoded DBR.
	 */
	private static DBR getTimeDBR(DBR inDBR, DBRType dataType, int dataCount, ByteBuffer dataPayloadBuffer)
	{
		TIME retVal = (TIME)inDBR;

		int status = dataPayloadBuffer.getShort() & 0xFFFF;
		int severity = dataPayloadBuffer.getShort() & 0xFFFF;

		// seconds since 0000 Jan 1, 1990
		long secPastEpoch = dataPayloadBuffer.getInt() & 0x00000000FFFFFFFFL;
		// nanoseconds within second
		long nsec = dataPayloadBuffer.getInt() & 0x00000000FFFFFFFFL;

		
		if (dataType == DBR_TIME_String.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_TIME_String((String[])readValues(null, DBR_String.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_String.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_TIME_Short.TYPE)
		{
			// RISC padding
			dataPayloadBuffer.getShort();
			if (retVal == null)
				retVal = new DBR_TIME_Short((short[])readValues(null, DBR_Short.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Short.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_TIME_Float.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_TIME_Float((float[])readValues(null, DBR_Float.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Float.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_TIME_Enum.TYPE)
		{
			// RISC padding
			dataPayloadBuffer.getShort();
			if (retVal == null)
				retVal = new DBR_TIME_Enum((short[])readValues(null, DBR_Enum.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Enum.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_TIME_Byte.TYPE)
		{
			// RISC padding
			dataPayloadBuffer.getShort();
			dataPayloadBuffer.get();
			if (retVal == null)
				retVal = new DBR_TIME_Byte((byte[])readValues(null, DBR_Byte.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Byte.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_TIME_Int.TYPE)
		{
			if (retVal == null)
				retVal = new DBR_TIME_Int((int[])readValues(null, DBR_Int.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Int.TYPE, dataCount, dataPayloadBuffer);
		}
		else if (dataType == DBR_TIME_Double.TYPE)
		{
			// RISC padding
			dataPayloadBuffer.getInt();
			if (retVal == null)
				retVal = new DBR_TIME_Double((double[])readValues(null, DBR_Double.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Double.TYPE, dataCount, dataPayloadBuffer);
		}

		retVal.setStatus(status);
		retVal.setSeverity(severity);
		retVal.setTimeStamp(new TimeStamp(secPastEpoch, nsec));
		
		return (DBR)retVal;
	}

	/**
	 * Create (extract) string (zero-terminated) from byte buffer.
	 * @param rawBuffer
	 * @return decoded DBR.
	 */
	private static String extractString(byte[] rawBuffer)
	{
		int len = 0;
		while (rawBuffer[len] != 0 && len < rawBuffer.length)
			len++;
		return new String(rawBuffer, 0, len);
	}

	/**
	 * @param inDBR	DBR to be used (set), do not create a new instance of DBR if non-<code>null</code>.
	 * @param dataType
	 * @param dataCount
	 * @param dataPayloadBuffer
	 * @return decoded DBR.
	 */
	private static DBR getGraphicsDBR(DBR inDBR, DBRType dataType, int dataCount, ByteBuffer dataPayloadBuffer)
	{
		// GR_String is actually a STS
		if (dataType == DBR_GR_String.TYPE)
			return getStatusDBR(inDBR, dataType, dataCount, dataPayloadBuffer);
		
		int status = dataPayloadBuffer.getShort() & 0xFFFF;
		int severity = dataPayloadBuffer.getShort() & 0xFFFF;

		// special case
		if (dataType == DBR_LABELS_Enum.TYPE || dataType == DBR_CTRL_Enum.TYPE)
		{
			final int MAX_ENUM_STRING_SIZE = 26;
			final int MAX_ENUM_STATES = 16;

			int count = dataPayloadBuffer.getShort() & 0xFFFF;
			
			byte[] rawBuffer = new byte[MAX_ENUM_STRING_SIZE];

			// read labels
			String[] labels = new String[count];
			for (int i = 0; i < count; i++)
			{
				dataPayloadBuffer.get(rawBuffer);
				labels[i] = extractString(rawBuffer);
			}
			
			// read rest
			int restEntries = MAX_ENUM_STATES - count; 
			for (int i = 0; i < restEntries; i++)
				dataPayloadBuffer.get(rawBuffer);

			DBR_LABELS_Enum le = (DBR_LABELS_Enum)inDBR;
			if (le == null)
			{
				if (dataType == DBR_LABELS_Enum.TYPE)
					le = new DBR_LABELS_Enum((short[])readValues(null, DBR_Enum.TYPE, dataCount, dataPayloadBuffer));
				else
					le = new DBR_CTRL_Enum((short[])readValues(null, DBR_Enum.TYPE, dataCount, dataPayloadBuffer));
			}
			else
				readValues(le, DBR_Enum.TYPE, dataCount, dataPayloadBuffer);
			
			le.setStatus(status);
			le.setSeverity(severity);
			le.setLabels(labels);
			
			return le;
		}

		GR retVal = (GR)inDBR;

		int precision = 0;
		if (dataType == DBR_GR_Float.TYPE || dataType == DBR_GR_Double.TYPE)
		{
			precision = dataPayloadBuffer.getShort() & 0xFFFF;
			// RISC padding
			dataPayloadBuffer.getShort();
		}

		// read units
		final int MAX_UNITS_SIZE = 8;
		byte[] rawUnits = new byte[MAX_UNITS_SIZE];
		dataPayloadBuffer.get(rawUnits);

		String units = extractString(rawUnits);			
		
		if (dataType == DBR_GR_Short.TYPE)
		{
			// GR values
			Short upperDisplay = new Short(dataPayloadBuffer.getShort());
			Short lowerDisplay = new Short(dataPayloadBuffer.getShort());
			Short upperAlarm   = new Short(dataPayloadBuffer.getShort());
			Short upperWarning = new Short(dataPayloadBuffer.getShort());
			Short lowerWarning = new Short(dataPayloadBuffer.getShort());
			Short lowerAlarm   = new Short(dataPayloadBuffer.getShort());

			if (retVal == null)
				retVal = new DBR_GR_Short((short[])readValues(null, DBR_Short.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Short.TYPE, dataCount, dataPayloadBuffer);
			
			retVal.setUpperDispLimit(upperDisplay);
			retVal.setUpperAlarmLimit(upperAlarm);
			retVal.setUpperWarningLimit(upperWarning);
			retVal.setLowerWarningLimit(lowerWarning);
			retVal.setLowerAlarmLimit(lowerAlarm);
			retVal.setLowerDispLimit(lowerDisplay);
		}
		else if (dataType == DBR_GR_Float.TYPE)
		{
			// GR values
			Float upperDisplay = new Float(dataPayloadBuffer.getFloat());
			Float lowerDisplay = new Float(dataPayloadBuffer.getFloat());
			Float upperAlarm   = new Float(dataPayloadBuffer.getFloat());
			Float upperWarning = new Float(dataPayloadBuffer.getFloat());
			Float lowerWarning = new Float(dataPayloadBuffer.getFloat());
			Float lowerAlarm   = new Float(dataPayloadBuffer.getFloat());

			DBR_GR_Float val = (DBR_GR_Float)inDBR;
			if (val == null) 
				val = new DBR_GR_Float((float[])readValues(null, DBR_Float.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(val, DBR_Float.TYPE, dataCount, dataPayloadBuffer);				
			val.setPrecision((short)precision);
			retVal = val;

			retVal.setUpperDispLimit(upperDisplay);
			retVal.setUpperAlarmLimit(upperAlarm);
			retVal.setUpperWarningLimit(upperWarning);
			retVal.setLowerWarningLimit(lowerWarning);
			retVal.setLowerAlarmLimit(lowerAlarm);
			retVal.setLowerDispLimit(lowerDisplay);
		}
		else if (dataType == DBR_GR_Byte.TYPE)
		{
			// GR values
			Byte upperDisplay = new Byte(dataPayloadBuffer.get());
			Byte lowerDisplay = new Byte(dataPayloadBuffer.get());
			Byte upperAlarm   = new Byte(dataPayloadBuffer.get());
			Byte upperWarning = new Byte(dataPayloadBuffer.get());
			Byte lowerWarning = new Byte(dataPayloadBuffer.get());
			Byte lowerAlarm   = new Byte(dataPayloadBuffer.get());

			// RISC padding
			dataPayloadBuffer.get();
			if (retVal == null)
				retVal = new DBR_GR_Byte((byte[])readValues(null, DBR_Byte.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Byte.TYPE, dataCount, dataPayloadBuffer);

			retVal.setUpperDispLimit(upperDisplay);
			retVal.setUpperAlarmLimit(upperAlarm);
			retVal.setUpperWarningLimit(upperWarning);
			retVal.setLowerWarningLimit(lowerWarning);
			retVal.setLowerAlarmLimit(lowerAlarm);
			retVal.setLowerDispLimit(lowerDisplay);
		}
		else if (dataType == DBR_GR_Int.TYPE)
		{
			// GR values
			Integer upperDisplay = new Integer(dataPayloadBuffer.getInt());
			Integer lowerDisplay = new Integer(dataPayloadBuffer.getInt());
			Integer upperAlarm   = new Integer(dataPayloadBuffer.getInt());
			Integer upperWarning = new Integer(dataPayloadBuffer.getInt());
			Integer lowerWarning = new Integer(dataPayloadBuffer.getInt());
			Integer lowerAlarm   = new Integer(dataPayloadBuffer.getInt());

			if (retVal == null)
				retVal = new DBR_GR_Int((int[])readValues(null, DBR_Int.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Int.TYPE, dataCount, dataPayloadBuffer);
				
			retVal.setUpperDispLimit(upperDisplay);
			retVal.setUpperAlarmLimit(upperAlarm);
			retVal.setUpperWarningLimit(upperWarning);
			retVal.setLowerWarningLimit(lowerWarning);
			retVal.setLowerAlarmLimit(lowerAlarm);
			retVal.setLowerDispLimit(lowerDisplay);
		}
		else if (dataType == DBR_GR_Double.TYPE)
		{
			// GR values
			Double upperDisplay = new Double(dataPayloadBuffer.getDouble());
			Double lowerDisplay = new Double(dataPayloadBuffer.getDouble());
			Double upperAlarm   = new Double(dataPayloadBuffer.getDouble());
			Double upperWarning = new Double(dataPayloadBuffer.getDouble());
			Double lowerWarning = new Double(dataPayloadBuffer.getDouble());
			Double lowerAlarm   = new Double(dataPayloadBuffer.getDouble());

			DBR_GR_Double val = (DBR_GR_Double)inDBR;
			if (val == null)
				val = new DBR_GR_Double((double[])readValues(null, DBR_Double.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(val, DBR_Double.TYPE, dataCount, dataPayloadBuffer);
			val.setPrecision((short)precision);
			retVal = val;

			retVal.setUpperDispLimit(upperDisplay);
			retVal.setUpperAlarmLimit(upperAlarm);
			retVal.setUpperWarningLimit(upperWarning);
			retVal.setLowerWarningLimit(lowerWarning);
			retVal.setLowerAlarmLimit(lowerAlarm);
			retVal.setLowerDispLimit(lowerDisplay);
		}

		retVal.setStatus(status);
		retVal.setSeverity(severity);
		retVal.setUnits(units);
		
		return (DBR)retVal;
	}

	/**
	 * @param inDBR	DBR to be used (set), do not create a new instance of DBR if non-<code>null</code>.
	 * @param dataType
	 * @param dataCount
	 * @param dataPayloadBuffer
	 * @return decoded DBR.
	 */
	private static DBR getControlDBR(DBR inDBR, DBRType dataType, int dataCount, ByteBuffer dataPayloadBuffer)
	{
		// CTRL_String is actually a STS
		if (dataType == DBR_CTRL_String.TYPE)
			return getStatusDBR(inDBR, dataType, dataCount, dataPayloadBuffer);
		// CTRL_Enum is actially LABEL (GRAPHICS)
		else if (dataType == DBR_CTRL_Enum.TYPE)
			return getGraphicsDBR(inDBR, dataType, dataCount, dataPayloadBuffer);
			
		int status = dataPayloadBuffer.getShort() & 0xFFFF;
		int severity = dataPayloadBuffer.getShort() & 0xFFFF;
		
		CTRL retVal = (CTRL)inDBR;

		int precision = 0;
		if (dataType == DBR_CTRL_Float.TYPE || dataType == DBR_CTRL_Double.TYPE)
		{
			precision = dataPayloadBuffer.getShort() & 0xFFFF;
			// RISC padding
			dataPayloadBuffer.getShort();
		}

		// read units
		final int MAX_UNITS_SIZE = 8;
		byte[] rawUnits = new byte[MAX_UNITS_SIZE];
		dataPayloadBuffer.get(rawUnits);

		String units = extractString(rawUnits);			
		
		if (dataType == DBR_CTRL_Short.TYPE)
		{
			// CTRL values
			Short upperDisplay = new Short(dataPayloadBuffer.getShort());
			Short lowerDisplay = new Short(dataPayloadBuffer.getShort());
			Short upperAlarm   = new Short(dataPayloadBuffer.getShort());
			Short upperWarning = new Short(dataPayloadBuffer.getShort());
			Short lowerWarning = new Short(dataPayloadBuffer.getShort());
			Short lowerAlarm   = new Short(dataPayloadBuffer.getShort());
			Short upperControl = new Short(dataPayloadBuffer.getShort());
			Short lowerControl = new Short(dataPayloadBuffer.getShort());

			if (retVal == null)
				retVal = new DBR_CTRL_Short((short[])readValues(null, DBR_Short.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Short.TYPE, dataCount, dataPayloadBuffer);
				
			retVal.setUpperDispLimit(upperDisplay);
			retVal.setUpperAlarmLimit(upperAlarm);
			retVal.setUpperWarningLimit(upperWarning);
			retVal.setLowerWarningLimit(lowerWarning);
			retVal.setLowerAlarmLimit(lowerAlarm);
			retVal.setLowerDispLimit(lowerDisplay);
			retVal.setUpperCtrlLimit(upperControl);
			retVal.setLowerCtrlLimit(lowerControl);
		}
		else if (dataType == DBR_CTRL_Float.TYPE)
		{
			// CTRL values
			Float upperDisplay = new Float(dataPayloadBuffer.getFloat());
			Float lowerDisplay = new Float(dataPayloadBuffer.getFloat());
			Float upperAlarm   = new Float(dataPayloadBuffer.getFloat());
			Float upperWarning = new Float(dataPayloadBuffer.getFloat());
			Float lowerWarning = new Float(dataPayloadBuffer.getFloat());
			Float lowerAlarm   = new Float(dataPayloadBuffer.getFloat());
			Float upperControl = new Float(dataPayloadBuffer.getFloat());
			Float lowerControl = new Float(dataPayloadBuffer.getFloat());

				
			DBR_CTRL_Float val = (DBR_CTRL_Float)inDBR;
			if (val == null) 
				val = new DBR_CTRL_Float((float[])readValues(null, DBR_Float.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(val, DBR_Float.TYPE, dataCount, dataPayloadBuffer);
			val.setPrecision((short)precision);
			retVal = val;

			retVal.setUpperDispLimit(upperDisplay);
			retVal.setUpperAlarmLimit(upperAlarm);
			retVal.setUpperWarningLimit(upperWarning);
			retVal.setLowerWarningLimit(lowerWarning);
			retVal.setLowerAlarmLimit(lowerAlarm);
			retVal.setLowerDispLimit(lowerDisplay);
			retVal.setUpperCtrlLimit(upperControl);
			retVal.setLowerCtrlLimit(lowerControl);
		}
		else if (dataType == DBR_CTRL_Byte.TYPE)
		{
			// CTRL values
			Byte upperDisplay = new Byte(dataPayloadBuffer.get());
			Byte lowerDisplay = new Byte(dataPayloadBuffer.get());
			Byte upperAlarm   = new Byte(dataPayloadBuffer.get());
			Byte upperWarning = new Byte(dataPayloadBuffer.get());
			Byte lowerWarning = new Byte(dataPayloadBuffer.get());
			Byte lowerAlarm   = new Byte(dataPayloadBuffer.get());
			Byte upperControl = new Byte(dataPayloadBuffer.get());
			Byte lowerControl = new Byte(dataPayloadBuffer.get());

			// RISC padding
			dataPayloadBuffer.get();
			if (retVal == null)
				retVal = new DBR_CTRL_Byte((byte[])readValues(null, DBR_Byte.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Byte.TYPE, dataCount, dataPayloadBuffer);				

			retVal.setUpperDispLimit(upperDisplay);
			retVal.setUpperAlarmLimit(upperAlarm);
			retVal.setUpperWarningLimit(upperWarning);
			retVal.setLowerWarningLimit(lowerWarning);
			retVal.setLowerAlarmLimit(lowerAlarm);
			retVal.setLowerDispLimit(lowerDisplay);
			retVal.setUpperCtrlLimit(upperControl);
			retVal.setLowerCtrlLimit(lowerControl);
		}
		else if (dataType == DBR_CTRL_Int.TYPE)
		{
			// CTRL values
			Integer upperDisplay = new Integer(dataPayloadBuffer.getInt());
			Integer lowerDisplay = new Integer(dataPayloadBuffer.getInt());
			Integer upperAlarm   = new Integer(dataPayloadBuffer.getInt());
			Integer upperWarning = new Integer(dataPayloadBuffer.getInt());
			Integer lowerWarning = new Integer(dataPayloadBuffer.getInt());
			Integer lowerAlarm   = new Integer(dataPayloadBuffer.getInt());
			Integer upperControl = new Integer(dataPayloadBuffer.getInt());
			Integer lowerControl = new Integer(dataPayloadBuffer.getInt());

			if (retVal == null)
				retVal = new DBR_CTRL_Int((int[])readValues(null, DBR_Int.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(inDBR, DBR_Int.TYPE, dataCount, dataPayloadBuffer);
			
			retVal.setUpperDispLimit(upperDisplay);
			retVal.setUpperAlarmLimit(upperAlarm);
			retVal.setUpperWarningLimit(upperWarning);
			retVal.setLowerWarningLimit(lowerWarning);
			retVal.setLowerAlarmLimit(lowerAlarm);
			retVal.setLowerDispLimit(lowerDisplay);
			retVal.setUpperCtrlLimit(upperControl);
			retVal.setLowerCtrlLimit(lowerControl);
		}
		else if (dataType == DBR_CTRL_Double.TYPE)
		{
			// CTRL values
			Double upperDisplay = new Double(dataPayloadBuffer.getDouble());
			Double lowerDisplay = new Double(dataPayloadBuffer.getDouble());
			Double upperAlarm   = new Double(dataPayloadBuffer.getDouble());
			Double upperWarning = new Double(dataPayloadBuffer.getDouble());
			Double lowerWarning = new Double(dataPayloadBuffer.getDouble());
			Double lowerAlarm   = new Double(dataPayloadBuffer.getDouble());
			Double upperControl = new Double(dataPayloadBuffer.getDouble());
			Double lowerControl = new Double(dataPayloadBuffer.getDouble());

			DBR_CTRL_Double val = (DBR_CTRL_Double)inDBR;
			 if (val == null)
				val = new DBR_CTRL_Double((double[])readValues(null, DBR_Double.TYPE, dataCount, dataPayloadBuffer));
			else
				readValues(val, DBR_Double.TYPE, dataCount, dataPayloadBuffer);
			val.setPrecision((short)precision);
			retVal = val;

			retVal.setUpperDispLimit(upperDisplay);
			retVal.setUpperAlarmLimit(upperAlarm);
			retVal.setUpperWarningLimit(upperWarning);
			retVal.setLowerWarningLimit(lowerWarning);
			retVal.setLowerAlarmLimit(lowerAlarm);
			retVal.setLowerDispLimit(lowerDisplay);
			retVal.setUpperCtrlLimit(upperControl);
			retVal.setLowerCtrlLimit(lowerControl);
		}

		retVal.setStatus(status);
		retVal.setSeverity(severity);
		retVal.setUnits(units);
		
		return (DBR)retVal;
	}

	/**
	 * @param inDBR	DBR to be used (set), do not create a new instance of DBR if non-<code>null</code>.
	 * @param dataCount
	 * @param dataPayloadBuffer
	 * @return decoded DBR.
	 */
	private static DBR getAckTDBR(DBR inDBR, int dataCount, ByteBuffer dataPayloadBuffer)
	{
		if (inDBR == null)
			return new DBR_PutAckT((short[])readValues(null, DBR_Short.TYPE, dataCount, dataPayloadBuffer));
		else
		{
			readValues(inDBR, DBR_Short.TYPE, dataCount, dataPayloadBuffer);
			return inDBR;
		}
	}

	/**
	 * @param inDBR	DBR to be used (set), do not create a new instance of DBR if non-<code>null</code>.
	 * @param dataCount
	 * @param dataPayloadBuffer
	 * @return decoded DBR.
	 */
	private static DBR getAckSDBR(DBR inDBR, int dataCount, ByteBuffer dataPayloadBuffer)
	{
		if (inDBR == null)
			return new DBR_PutAckS((short[])readValues(null, DBR_Short.TYPE, dataCount, dataPayloadBuffer));
		else
		{
			readValues(inDBR, DBR_Short.TYPE, dataCount, dataPayloadBuffer);
			return inDBR;
		}
	}

	/**
	 * @param inDBR	DBR to be used (set), do not create a new instance of DBR if non-<code>null</code>.
	 * @param dataCount
	 * @param dataPayloadBuffer
	 * @return decoded DBR.
	 */
	private static DBR getStatusAckStringDBR(DBR inDBR, int dataCount, ByteBuffer dataPayloadBuffer)
	{
		int status = dataPayloadBuffer.getShort() & 0xFFFF;
		int severity = dataPayloadBuffer.getShort() & 0xFFFF;

		int ackT = dataPayloadBuffer.getShort() & 0xFFFF;
		int ackS = dataPayloadBuffer.getShort() & 0xFFFF;

		DBR_STSACK_String retVal = (DBR_STSACK_String)inDBR;
		if (retVal == null) 
			retVal = new DBR_STSACK_String((String[])readValues(null, DBR_String.TYPE, dataCount, dataPayloadBuffer));
		else
			readValues(retVal, DBR_String.TYPE, dataCount, dataPayloadBuffer);
			
		retVal.setStatus(status);
		retVal.setSeverity(severity);
		retVal.setAckT(ackT);
		retVal.setAckS(ackS);
		
		return retVal;
	}
}
