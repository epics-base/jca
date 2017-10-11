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
import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.GR;

/**
 * Enum process variable implementation. 
 * Implementation does not handle status, severity and alarms.
 */
public abstract class NumericProcessVariable extends ProcessVariable
{
	/**
	 * Numeric PV constructor.
	 * @param name process variable name.
	 * @param eventCallback	event callback, can be <code>null</code>.
	 */
	public NumericProcessVariable(String name, ProcessVariableEventCallback eventCallback)
	{
		super(name, eventCallback);
	}

	/**
	 * Get units. Default implementation, returns empty string. 
	 * 
	 * @return get units, <code>non-null</code>.
	 */
	public String getUnits() {
		return GR.EMPTYUNIT;
	}

	/**
	 * Get upper display limit. Default implementation, returns zero.
	 * 
	 * @return upper display limit, <code>non-null</code>.
	 */
	public Number getUpperDispLimit() {
		return GR.ZEROD;
	}

	/**
	 * Get lower display limit. Default implementation, returns zero.
	 * 
	 * @return lower display limit, <code>non-null</code>.
	 */
	public Number getLowerDispLimit() {
		return GR.ZEROD;
	}

	/**
	 * Get upper alarm limit. Default implementation, returns zero.
	 * 
	 * @return upper alarm limit, <code>non-null</code>.
	 */
	public Number getUpperAlarmLimit() {
		return GR.ZEROD;
	}

	/**
	 * Get upper warning limit. Default implementation, returns zero.
	 * 
	 * @return upper warning limit, <code>non-null</code>.
	 */
	public Number getUpperWarningLimit() {
		return GR.ZEROD;
	}

	/**
	 * Get lower warning limit. Default implementation, returns zero.
	 * 
	 * @return lower warning limit, <code>non-null</code>.
	 */
	public Number getLowerWarningLimit() {
		return GR.ZEROD;
	}

	/**
	 * Get lower alarm limit. Default implementation, returns zero.
	 * 
	 * @return lower alarm limit, <code>non-null</code>.
	 */
	public Number getLowerAlarmLimit() {
		return GR.ZEROD;
	}

	/**
	 * Get upper control limit. Default implementation, returns zero.
	 * 
	 * @return upper control limit, <code>non-null</code>.
	 */
	public Number getUpperCtrlLimit() {
		return GR.ZEROD;
	}

	/**
	 * Get lower control limit. Default implementation, returns zero.
	 * 
	 * @return lower control limit, <code>non-null</code>.
	 */
	public Number getLowerCtrlLimit() {
		return GR.ZEROD;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.aps.jca.cas.ProcessVariable#read(gov.aps.jca.dbr.DBR,
	 *      gov.aps.jca.cas.ProcessVariableReadCallback)
	 */
	public CAStatus read(DBR value, ProcessVariableReadCallback asyncReadCallback) throws CAException {

		// fill
		fillInDBR(value);
		
		// read value
		return readValue(value, asyncReadCallback);
	}
	
	/**
	 * Set data (units, limits, ...) to DBR.
	 * GR and CTRL data is filled only if <code>isCTRLSupported</code> returns <code>true</code>.
	 * @param value DBR to fill-in.
	 */
	public void fillInDBR(DBR value)
	{
		if (value.isGR()) {
			// fill GR
			GR gr = (GR)value;
			gr.setUnits(getUnits());
			gr.setUpperDispLimit(getUpperDispLimit());
			gr.setLowerDispLimit(getLowerDispLimit());
			gr.setUpperAlarmLimit(getUpperAlarmLimit());
			gr.setUpperWarningLimit(getUpperWarningLimit());
			gr.setLowerWarningLimit(getLowerWarningLimit());
			gr.setLowerAlarmLimit(getLowerAlarmLimit());
		}

		if (value.isCTRL()) {
			// fill-up GR to CTRL
			CTRL ctrl = (CTRL)value;
			ctrl.setUpperCtrlLimit(getUpperCtrlLimit());
			ctrl.setLowerCtrlLimit(getLowerCtrlLimit());
		}
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
	 * 		value.setStatus(Status.<status>);
	 * 		value.setSeverity(Severity.<severity>);
	 * 		
	 * 		// set timestamp
	 * 		value.setTimestamp(timestamp);
	 * 		
	 * 		// END optional (to override defaults)
 	 * 		
	 * 		// set value to given DBR (example of copying value for DOUBLE type process variable)
	 * 		// given DBR has already allocated an array of elements client has requested
	 * 		// it contains maximum number of elements to fill
	 * 		double[] arrayValue = (DBR_Double)value.getDoubleValue();
	 * 		int elementCount = Math.min(<fromDoubleArray>.length, arrayValue.length);
	 * 		System.arraycopy(<fromDoubleArray>, 0, arrayValue, 0, elementCount);
	 * 
	 * 		// return read completion status
	 * 		return CAStatus.NORMAL;
	 * 	} 
	 * 
	 * </pre>
	 * @see gov.aps.jca.cas.ProcessVariable#read(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableReadCallback)
	 */
	protected abstract CAStatus readValue(DBR value,
			ProcessVariableReadCallback asyncReadCallback) throws CAException;
		
	
	/* (non-Javadoc)
	 * @see gov.aps.jca.cas.ProcessVariable#write(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableWriteCallback)
	 */
	public CAStatus write(DBR value, ProcessVariableWriteCallback asyncWriteCallback) throws CAException {
		return writeValue(value, asyncWriteCallback);
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
	 * 		// set value from given DBR here
	 * 		...
	 * 
	 * 	    // notify, set appropirate Monitor mask (VALUE, LOG, ALARM)
	 * 	    if (status == CAStatus.NORMAL && interest)
	 *      {
	 *			DBR monitorDBR = AbstractCASResponseHandler.createDBRforReading(this);
	 *			((DBR_Double)monitorDBR).getDoubleValue()[0] = this.value;
	 *			fillInDBR(monitorDBR);
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
	protected abstract CAStatus writeValue(DBR value,
			ProcessVariableWriteCallback asyncWriteCallback) throws CAException;

}