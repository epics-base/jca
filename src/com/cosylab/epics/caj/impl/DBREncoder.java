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

import gov.aps.jca.dbr.ACK;
import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.GR;
import gov.aps.jca.dbr.LABELS;
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

/**
 * DBR encode - serializes DBR.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class DBREncoder {

	private static final int[] PAYLOAD_SIZE = new int[] { 
		0,	/* string max size			*/
		0,	/* short					*/
		0,	/* IEEE Float				*/
		0,	/* item number				*/
		0,	/* character				*/

		0,	/* int						*/
		0,	/* double					*/
		4,	/* string field	with status	*/
		4,	/* short field with status	*/
		4,	/* float field with status	*/

		4,	/* item number with status	*/
		5,	/* char field with status	*/
		4,	/* int field with status	*/
		8,	/* double field with status	*/
		
		12,	/* string field	with time	*/

		14,	/* short field with time	*/
		12,	/* float field with time	*/
		14,	/* item number with time	*/
		15,	/* char field with time		*/
		12,	/* int field with time		*/
		16,	/* double field with time	*/

		4,	/* graphic string info (sts string)	*/
		24,	/* graphic short info		*/
		40,	/* graphic float info		*/
		422,/* graphic item info		*/ 

		19,	/* graphic char info		*/
		36,	/* graphic int info			*/
		64,	/* graphic double info		*/

		4,	/* control string info (sts string)	*/
		28,	/* control short info		*/

		48,	/* control float info		*/
		422,/* control item info		*/
		21,	/* control char info		*/
		44,	/* control int info			*/
		80,	/* control double info		*/

		0,	/* put ackt					*/
		0,	/* put acks					*/
		8	/* string with status/ack 	*/
	};  
	
	/**
	 * NOTE: alignment (8) is done externally
	 * @param dataTypeValue
	 * @param dataCount
	 * @param value
	 * @return calculated (non-aligned) payload size.
	 */
	// TODO size could be extracted from DBR type obejct
	// TODO converting TYPE -> short and now short -> TYPE
	public static int calculatePayloadSize(short dataTypeValue, int dataCount, Object value)
	{
		DBRType dataType = DBRType.forValue(dataTypeValue);
		if (dataType == null)
			throw new IllegalArgumentException("Invalid data type value: " + dataTypeValue);

		int additionalPayload = PAYLOAD_SIZE[dataTypeValue];
		
		return calculateValuePayloadSize(dataCount, value, dataType) + additionalPayload;

	}

	/**
	 * Calculate palyoad value size.
	 * @param dataCount
	 * @param value
	 * @param dataType
	 * @return calculated value payload size.
	 */
	private static int calculateValuePayloadSize(int dataCount, Object value, DBRType dataType) 
	{
		if (dataType.isDOUBLE())
		{
			return dataCount * 8;
		}
		else if (dataType.isINT())
		{
			return dataCount * 4;
		}
		else if (dataType.isSTRING())
		{
			if (dataCount == 1)
			{
				String str = ((String[])value)[0];
				if (str != null)
					return str.length() + 1;
				else
					return 1;
			}
			else
			    return dataCount * CAConstants.MAX_STRING_SIZE;
		}
		else if (dataType.isSHORT())
		{
			return dataCount * 2;
		}
		else if (dataType.isFLOAT())
		{
			return dataCount * 4;
		}
		else if (dataType.isENUM())
		{
			return dataCount * 2;
		}
		else if (dataType.isBYTE())
		{
			return dataCount * 1;
		}
		else if (dataType == DBRType.PUT_ACKT ||
				 dataType == DBRType.PUT_ACKS)
		{
			return dataCount * 2;
		}
		else
			throw new IllegalArgumentException("Unsupported data type: " + dataType);
	}

	/**
	 * Insert value payload.
	 * @param dataTypeValue
	 * @param dataCount
	 * @param value
	 */
	public static void insertPayload(ByteBuffer payloadBuffer, short dataTypeValue, int dataCount, Object value)
	{
		DBRType dataType = DBRType.forValue(dataTypeValue);
		if (dataType == null)
			throw new IllegalArgumentException("Invalid data type value: " + dataTypeValue);

		final int count = Math.min(Array.getLength(value), dataCount);
		
		if (dataType.isDOUBLE())
		{
			double[] array = (double[])value;
			for (int i = 0; i < count; i++)
				payloadBuffer.putDouble(array[i]);
		}
		else if (dataType.isINT())
		{
			int[] array = (int[])value;
			for (int i = 0; i < count; i++)
				payloadBuffer.putInt(array[i]);
		}
		else if (dataType.isSTRING())
		{
			String[] array = (String[])value;
			
			if (count == 1) {
				if (array.length > 0 && array[0] != null)
					payloadBuffer.put(array[0].getBytes());
			    payloadBuffer.put((byte)0);
			}
			else 
			{
				for (int i = 0; i < count; i++)
				{
				    // limit string size, leave one byte for termination
				    int pos = payloadBuffer.position();
				    if (array[i] != null)
				    {
				    	int bytesToWrite = Math.min(array[i].length(), CAConstants.MAX_STRING_SIZE - 1); 
				    	payloadBuffer.put(array[i].getBytes(), 0, bytesToWrite);
				    }
				    payloadBuffer.put((byte)0);
					payloadBuffer.position(pos + CAConstants.MAX_STRING_SIZE);
				}
			}
		}
		else if (dataType.isSHORT())
		{
			short[] array = (short[])value;
			for (int i = 0; i < count; i++)
				payloadBuffer.putShort(array[i]);
		}
		else if (dataType.isFLOAT())
		{
			float[] array = (float[])value;
			for (int i = 0; i < count; i++)
				payloadBuffer.putFloat(array[i]);
		}
		else if (dataType.isENUM())
		{
			short[] array = (short[])value;
			for (int i = 0; i < count; i++)
				payloadBuffer.putShort(array[i]);
		}
		else if (dataType.isBYTE())
		{
			byte[] array = (byte[])value;
			for (int i = 0; i < count; i++)
				payloadBuffer.put(array[i]);
		}
		else if (dataType == DBRType.PUT_ACKT ||
				 dataType == DBRType.PUT_ACKS)
		{
			short[] array = (short[])value;
			for (int i = 0; i < count; i++)
				payloadBuffer.putShort(array[i]);
		}
		else
			throw new IllegalArgumentException("Unsupported data type: " + dataType);

	}

	/**
	 * Insert DBR payload.
	 * NOTE: non-null CTRL, GR values required.
	 * @param dataTypeValue
	 * @param dataCount
	 * @param value
	 */
	// TODO optimize!!!
	public static void insertPayload(ByteBuffer payloadBuffer, short dataTypeValue, int dataCount, DBR value)
	{
		DBRType dataType = DBRType.forValue(dataTypeValue);
		if (dataType == null)
			throw new IllegalArgumentException("Invalid data type value: " + dataTypeValue);

		if (dataType.isSTS())
		{
			STS sts = (STS)value;

			Status status = sts.getStatus(); 
			if (status == null)
				payloadBuffer.putShort((short)Status.UDF_ALARM.getValue());
			else
				payloadBuffer.putShort((short)status.getValue());
			
			Severity severity = sts.getSeverity();
			if (severity == null)
				payloadBuffer.putShort((short)Severity.INVALID_ALARM.getValue());
			else
				payloadBuffer.putShort((short)severity.getValue());
		}
		
		// special case DBR_STSACK_String
		if (dataType == DBRType.STSACK_STRING)
		{
			ACK ack = (ACK)value;

			payloadBuffer.putShort(ack.getAckT() ? (short)1 : (short)0);
			
			Severity severity = ack.getAckS();
			if (severity == null)
				payloadBuffer.putShort((short)Severity.INVALID_ALARM.getValue());
			else
				payloadBuffer.putShort((short)severity.getValue());
		}
		// special case DBR_LABELS_Enum
		else if (dataType.isLABELS())
		{
			LABELS labels = (LABELS)value;
			
			String[] labelsArray = labels.getLabels();
			final int count = (labelsArray != null) ? labelsArray.length : 0;

			payloadBuffer.putShort((short)count);
			
			final int MAX_ENUM_STRING_SIZE = 26;
			final int MAX_ENUM_STATES = 16;
			final byte[] EMPTY_LABEL = new byte[MAX_ENUM_STRING_SIZE];
			
			for (int i = 0; i < count; i++)
			{
				String label = labelsArray[i];
				if (label == null)
					payloadBuffer.put(EMPTY_LABEL);
				else
				{
					// check (and fix) length, watch for zero terminator
					int labelLen = label.length();
					if (labelLen >= MAX_ENUM_STRING_SIZE)
					{
						labelLen = MAX_ENUM_STATES - 1;
						label = label.substring(0, labelLen);
					}
					
					// insert label
					payloadBuffer.put(label.getBytes());
					for (int j = labelLen; j < MAX_ENUM_STRING_SIZE; j++)
						payloadBuffer.put((byte)0);
				}
			}
				
			// fill the rest
			final int labelsLeft = (MAX_ENUM_STATES - count);
			for (int i = 0; i < labelsLeft; i++)
				payloadBuffer.put(EMPTY_LABEL);
				
		}
		else
		{
		
			// RISC padding
			if (dataType == DBRType.STS_BYTE)
				payloadBuffer.put((byte)0);
			else if (dataType == DBRType.STS_DOUBLE)
				payloadBuffer.putInt(0);
			
			// GR is not actually a TIME, TODO clean up this mess
			if (dataType.isTIME() && !dataType.isGR() && 
					!(dataTypeValue == 21 || dataTypeValue == 28))     // not GR_STRING, CTRL_STRING
			{
				TIME time = (TIME)value;
				TimeStamp ts = time.getTimeStamp();
				
				// speical case if timestamp is null
				if (ts == null)
				{
					payloadBuffer.putInt(0);
					payloadBuffer.putInt(0);
				}
				else
				{
					payloadBuffer.putInt((int)ts.secPastEpoch());
					payloadBuffer.putInt((int)ts.nsec());
				}
			}
			
			// RISC padding
			if (dataType == DBRType.TIME_SHORT ||
				dataType == DBRType.TIME_ENUM)
				payloadBuffer.putShort((short)0);
			else if (dataType == DBRType.TIME_BYTE)
			{
				payloadBuffer.putShort((short)0);
				payloadBuffer.put((byte)0);
			}
			else if (dataType == DBRType.TIME_DOUBLE)
				payloadBuffer.putInt(0);
	
			if (dataType.isGR()) 
			{
				
				if (dataType.isPRECISION())
				{
					PRECISION precision = (PRECISION)value;
					payloadBuffer.putShort(precision.getPrecision());
					// RISC padding
					payloadBuffer.putShort((short)0);
				}
				
				GR gr = (GR)value;
	
				// write units
				String units = gr.getUnits();
				payloadBuffer.put(units.getBytes());
				
				final int MAX_UNITS_SIZE = 8;
				final int zeros = MAX_UNITS_SIZE - units.length();
				for (int i = zeros; i > 0; i--)
					payloadBuffer.put((byte)0);
				
				if (dataType.isSHORT())
				{
					payloadBuffer.putShort(gr.getUpperDispLimit().shortValue());
					payloadBuffer.putShort(gr.getLowerDispLimit().shortValue());
					payloadBuffer.putShort(gr.getUpperAlarmLimit().shortValue());
					payloadBuffer.putShort(gr.getUpperWarningLimit().shortValue());
					payloadBuffer.putShort(gr.getLowerWarningLimit().shortValue());
					payloadBuffer.putShort(gr.getLowerAlarmLimit().shortValue());
				}
				else if (dataType.isFLOAT())
				{
					payloadBuffer.putFloat(gr.getUpperDispLimit().floatValue());
					payloadBuffer.putFloat(gr.getLowerDispLimit().floatValue());
					payloadBuffer.putFloat(gr.getUpperAlarmLimit().floatValue());
					payloadBuffer.putFloat(gr.getUpperWarningLimit().floatValue());
					payloadBuffer.putFloat(gr.getLowerWarningLimit().floatValue());
					payloadBuffer.putFloat(gr.getLowerAlarmLimit().floatValue());
				}
				else if (dataType.isBYTE())
				{
					payloadBuffer.put(gr.getUpperDispLimit().byteValue());
					payloadBuffer.put(gr.getLowerDispLimit().byteValue());
					payloadBuffer.put(gr.getUpperAlarmLimit().byteValue());
					payloadBuffer.put(gr.getUpperWarningLimit().byteValue());
					payloadBuffer.put(gr.getLowerWarningLimit().byteValue());
					payloadBuffer.put(gr.getLowerAlarmLimit().byteValue());
				}
				else if (dataType.isINT())
				{
					payloadBuffer.putInt(gr.getUpperDispLimit().intValue());
					payloadBuffer.putInt(gr.getLowerDispLimit().intValue());
					payloadBuffer.putInt(gr.getUpperAlarmLimit().intValue());
					payloadBuffer.putInt(gr.getUpperWarningLimit().intValue());
					payloadBuffer.putInt(gr.getLowerWarningLimit().intValue());
					payloadBuffer.putInt(gr.getLowerAlarmLimit().intValue());
				}
				else if (dataType.isDOUBLE())
				{
					payloadBuffer.putDouble(gr.getUpperDispLimit().doubleValue());
					payloadBuffer.putDouble(gr.getLowerDispLimit().doubleValue());
					payloadBuffer.putDouble(gr.getUpperAlarmLimit().doubleValue());
					payloadBuffer.putDouble(gr.getUpperWarningLimit().doubleValue());
					payloadBuffer.putDouble(gr.getLowerWarningLimit().doubleValue());
					payloadBuffer.putDouble(gr.getLowerAlarmLimit().doubleValue());
				}
	
			}
			
			if (dataType.isCTRL()) 
			{
				CTRL ctrl = (CTRL)value;
	
				if (dataType.isSHORT())
				{
					payloadBuffer.putShort(ctrl.getUpperCtrlLimit().shortValue());
					payloadBuffer.putShort(ctrl.getLowerCtrlLimit().shortValue());
				}
				else if (dataType.isFLOAT())
				{
					payloadBuffer.putFloat(ctrl.getUpperCtrlLimit().floatValue());
					payloadBuffer.putFloat(ctrl.getLowerCtrlLimit().floatValue());
				}
				else if (dataType.isBYTE())
				{
					payloadBuffer.put(ctrl.getUpperCtrlLimit().byteValue());
					payloadBuffer.put(ctrl.getLowerCtrlLimit().byteValue());
				}
				else if (dataType.isINT())
				{
					payloadBuffer.putInt(ctrl.getUpperCtrlLimit().intValue());
					payloadBuffer.putInt(ctrl.getLowerCtrlLimit().intValue());
				}
				else if (dataType.isDOUBLE())
				{
					payloadBuffer.putDouble(ctrl.getUpperCtrlLimit().doubleValue());
					payloadBuffer.putDouble(ctrl.getLowerCtrlLimit().doubleValue());
				}
			}

		}
		
		// RISC padding
		if (dataType == DBRType.GR_BYTE ||
		    dataType == DBRType.CTRL_BYTE)
			payloadBuffer.put((byte)0);

		// write values
		insertPayload(payloadBuffer, dataTypeValue, dataCount, value.getValue());
	}

}
