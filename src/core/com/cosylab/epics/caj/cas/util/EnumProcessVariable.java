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

package com.cosylab.epics.caj.cas.util;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ProcessVariableReadCallback;
import gov.aps.jca.cas.ProcessVariableWriteCallback;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_Enum;
import gov.aps.jca.dbr.DBR_TIME_LABELS_Enum;

/**
 * Abstract convenient enumeration process variable implementation. 
 */
public abstract class EnumProcessVariable extends ProcessVariable 
{
	/**
	 * Enum PV constructor.
	 * @param name process variable name.
	 * @param eventCallback	event callback, can be <code>null</code>.
	 */
	public EnumProcessVariable(String name, ProcessVariableEventCallback eventCallback)
	{
		super(name, eventCallback);
	}

	/**
	 * Return <code>DBRType.ENUM</code> type as native type.
	 * @see gov.aps.jca.cas.ProcessVariable#getType()
	 */
	public DBRType getType() {
		return DBRType.ENUM;
	}

	/**
	 * Casts <code>DBR</code> to <code>DBR_TIME_LABELS_Enum</code>,
	 * sets labels returned by <code>getEnumLabels()</code> and
	 * delegates operation of reading a value to <code>readValue</code> method.
	 * @see gov.aps.jca.cas.ProcessVariable#read(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableReadCallback)
	 */
	public CAStatus read(DBR value, ProcessVariableReadCallback asyncReadCallback) throws CAException {

		// for ENUM type input DBR is always DBR_TIME_LABELS_Enum
		DBR_TIME_LABELS_Enum enumValue = (DBR_TIME_LABELS_Enum)value;
		
		// set labels
		enumValue.setLabels(getEnumLabels());

		// read value
		return readValue(enumValue, asyncReadCallback);
	}

	/**
	 * Read value.
	 * Reference implementation:
	 * <pre>
	 * 	{
	 * 
	 * 		// for async. completion, return <code>null</code>,
	 * 		// set value (and status) to <code>enumValue</code> and
	 * 		// report completion using <code>asyncReadCallback<code> callback.
	 * 		// return null;
	 * 	
	 * 		// BEGIN optional (to override defaults) 
	 *
	 * 		// set status and severity
	 * 		enumValue.setStatus(Status.<status>);
	 * 		enumValue.setSeverity(Severity.<severity>);
	 * 		
	 * 		// set timestamp
	 * 		enumValue.setTimestamp(timestamp);
	 * 		
	 * 		// END optional (to override defaults)
 	 * 		
	 * 		// set value to given DBR (example of copying value)
	 * 		// given DBR has already allocated an array of elements client has requested
	 * 		// it contains maximum number of elements to fill
	 * 		short[] arrayValue = value.getEnumValue();
	 * 		int elementCount = Math.min(<fromShortArray>.length, arrayValue.length);
	 * 		System.arraycopy(<fromShortArray>, 0, arrayValue, 0, elementCount);
	 * 
	 * 		// return read completion status
	 * 		return CAStatus.NORMAL;
	 * 	} 
	 * 
	 * </pre>
	 * @see gov.aps.jca.cas.ProcessVariable#read(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableReadCallback)
	 */
	protected abstract CAStatus readValue(DBR_TIME_LABELS_Enum value,
			ProcessVariableReadCallback asyncReadCallback) throws CAException;
		
	
	/**
	 * Casts <code>DBR</code> to <code>DBR_Enum</code> and delegates operation of writing a value to <code>writeValue</code> method.
	 * @see gov.aps.jca.cas.ProcessVariable#write(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableWriteCallback)
	 */
	public CAStatus write(DBR value, ProcessVariableWriteCallback asyncWriteCallback) throws CAException {
		
		// for ENUM type input DBR is always DBR_Enum
		DBR_Enum enumValue = (DBR_Enum)value;
		
		return writeValue(enumValue, asyncWriteCallback);
	}
	
	/**
	 * Write value.
	 * Reference implementation:
	 * <pre>
	 * 	{
	 * 
	 * 		// for async. completion, return <code>null</code>,
	 * 		// set value (and status) from <code>enumValue</code>,
	 * 		// notify if there is an interest and
	 * 		// report completion using <code>asyncWriteCallback<code> callback.
	 * 		// return null;
	 * 
	 * 		// set value from given DBR here (scalar example)
	 *      this.value = value.getEnumValue()[0];
	 * 
	 * 	    // notify, set appropirate Monitor mask (VALUE, LOG, ALARM)
	 * 	    if (status == CAStatus.NORMAL && interest)
	 *      {
	 *			DBR monitorDBR = AbstractCASResponseHandler.createDBRforReading(this);
	 *			((DBR_Enum)monitorDBR).getEnumValue()[0] = this.value;
	 *
	 *			// set labels
	 *			((LABELS)monitorDBR).setLabels(getEnumLabels());
	 *
	 *			// set status, severity and time here (see readValue method example)
	 *			fillInStatusAndTime((TIME)monitorDBR);
	 *		
	 * 	    	eventCallback.postEvent(Monitor.VALUE|Monitor.LOG, value);
	 * 		}
	 * 
	 * 		// return read completion status
	 * 		return CAStatus.NORMAL;
	 * }
	 * </pre>
	 * @see gov.aps.jca.cas.ProcessVariable#write(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableWriteCallback)
	 */
	protected abstract CAStatus writeValue(DBR_Enum value,
			ProcessVariableWriteCallback asyncWriteCallback) throws CAException;

}