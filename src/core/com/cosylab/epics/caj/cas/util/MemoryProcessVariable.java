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

import com.cosylab.epics.caj.cas.handlers.AbstractCASResponseHandler;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Monitor;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ProcessVariableReadCallback;
import gov.aps.jca.cas.ProcessVariableWriteCallback;
import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.GR;
import gov.aps.jca.dbr.LABELS;
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;

/**
 * Memory (what you write is what you get) process variable implementation.
 * Implementation does not handle status, severity and alarms.
 */
public class MemoryProcessVariable extends ProcessVariable 
{
	/**
	 * PV type.
	 */
	protected DBRType type;
	
	/**
	 * PV value.
	 */
	protected Object value;
	
	/**
	 * PV count.
	 */
	protected int count;

	/**
	 * Timestamp of set value.
	 */
	protected TimeStamp timestamp;

	/**
	 * Units (for value).
	 */
	protected String units = GR.EMPTYUNIT;

	/**
	 * Upper display limit.
	 */
	protected Number upperDispLimit = GR.ZEROD;

	/**
	 * Lower display value.
	 */
	protected Number lowerDispLimit = GR.ZEROD;

	/**
	 * Upper alarm limit.
	 */
	protected Number upperAlarmLimit = GR.ZEROD;

	/**
	 * Upper warning limit.
	 */
	protected Number upperWarningLimit = GR.ZEROD;

	/**
	 * Lower warning limit.
	 */
	protected Number lowerWarningLimit = GR.ZEROD;

	/**
	 * Lower alarm limit.
	 */
	protected Number lowerAlarmLimit = GR.ZEROD;

	/**
	 * Upper control limit.
	 */
	protected Number upperCtrlLimit = GR.ZEROD;

	/**
	 * Lower control limit.
	 */
	protected Number lowerCtrlLimit = GR.ZEROD;

	/**
	 * Precision.
	 * Used for floating decimal value to string conversion.
	 */
	protected short precision = -1;

	/**
	 * Array of labels for enum type PV.
	 */
	protected String[] enumLabels = null;
	
	/**
	 * Constructor of memory process variable.
	 * @param name	name of the PV.
	 * @param eventCallback	event callback, where to report value changes if <code>interest</code> is <code>true</code>.
	 * @param type	type of the PV (of initial value).
	 * @param initialValue	initial value, array is expected.
	 */
	public MemoryProcessVariable(String name, ProcessVariableEventCallback eventCallback,
								 DBRType type, Object initialValue)
	{
		super(name, eventCallback);
		
		// check if initial value is an array
		if (!initialValue.getClass().isArray())
			throw new IllegalArgumentException("array expected as initialValue");
		
		this.type = type;
		this.value = initialValue;
	    this.count = java.lang.reflect.Array.getLength(this.value);
	    this.timestamp = new TimeStamp();
	}

	/**
	 * @see gov.aps.jca.cas.ProcessVariable#getType()
	 */
	public DBRType getType() {
		return type;
	}

	/**
	 * @see gov.aps.jca.cas.ProcessVariable#getDimensionSize(int)
	 */
	public int getDimensionSize(int dimension) {
		if (dimension == 0)
			return count;
		else
			return 0;
	}

    /**
	 * Get lower alarm limit.
	 * @return lower alarm limit, <code>non-null</code>.
	 */
	public Number getLowerAlarmLimit() {
		return lowerAlarmLimit;
	}

    /**
	 * Set lower alarm limit.
	 * @param lowerAlarmLimit lower alarm limit, <code>non-null</code>.
	 */
	public void setLowerAlarmLimit(Number lowerAlarmLimit) {
		this.lowerAlarmLimit = lowerAlarmLimit;
	}

	/**
	 * Get lower control limit.
	 * @return lower control limit, <code>non-null</code>.
	 */
	public Number getLowerCtrlLimit() {
		return lowerCtrlLimit;
	}

	/**
	 * Set lower control limit.
	 * @param lowerCtrlLimit lower control limit, <code>non-null</code>.
	 */
	public void setLowerCtrlLimit(Number lowerCtrlLimit) {
		this.lowerCtrlLimit = lowerCtrlLimit;
	}

	/**
	 * Get lower display limit.
	 * @return lower display limit, <code>non-null</code>.
	 */
	public Number getLowerDispLimit() {
		return lowerDispLimit;
	}

	/**
	 * Set lower display limit.
	 * @param lowerDispLimit lower display limit, <code>non-null</code>.
	 */
	public void setLowerDispLimit(Number lowerDispLimit) {
		this.lowerDispLimit = lowerDispLimit;
	}

	/**
	 * Get lower warning limit.
	 * @return lower warning limit, <code>non-null</code>.
	 */
	public Number getLowerWarningLimit() {
		return lowerWarningLimit;
	}

	/**
	 * Set lower warning limit.
	 * @param lowerWarningLimit lower warning limit, <code>non-null</code>.
	 */
	public void setLowerWarningLimit(Number lowerWarningLimit) {
		this.lowerWarningLimit = lowerWarningLimit;
	}

	/**
  	 * Get units (of value).
	 * @return get units, <code>non-null</code>.
	 */
	public String getUnits() {
		return units;
	}

	/**
  	 * Set units (of value).
	 * @param units get units, <code>non-null</code>.
	 */
	public void setUnits(String units) {
		this.units = units;
	}

	/**
	 * Get upper alarm limit.
	 * @return upper alarm limit, <code>non-null</code>.
	 */
	public Number getUpperAlarmLimit() {
		return upperAlarmLimit;
	}

	/**
	 * Set upper alarm limit.
	 * @param upperAlarmLimit upper alarm limit, <code>non-null</code>.
	 */
	public void setUpperAlarmLimit(Number upperAlarmLimit) {
		this.upperAlarmLimit = upperAlarmLimit;
	}

	/**
	 * Get upper control limit.
	 * @return upper control limit, <code>non-null</code>.
	 */
	public Number getUpperCtrlLimit() {
		return upperCtrlLimit;
	}

	/**
	 * Set upper control limit.
	 * @param upperCtrlLimit upper control limit, <code>non-null</code>.
	 */
	public void setUpperCtrlLimit(Number upperCtrlLimit) {
		this.upperCtrlLimit = upperCtrlLimit;
	}

	/**
	 * Get upper display limit.
	 * @return upper display limit, <code>non-null</code>.
	 */
	public Number getUpperDispLimit() {
		return upperDispLimit;
	}

	/**
	 * Set upper display limit.
	 * @param upperDispLimit upper display limit, <code>non-null</code>.
	 */
	public void setUpperDispLimit(Number upperDispLimit) {
		this.upperDispLimit = upperDispLimit;
	}

	/**
	 * Get upper warning limit.
	 * @return upper warning limit, <code>non-null</code>.
	 */
	public Number getUpperWarningLimit() {
		return upperWarningLimit;
	}

	/**
	 * Set upper warning limit.
	 * @param upperWarningLimit upper warning limit, <code>non-null</code>.
	 */
	public void setUpperWarningLimit(Number upperWarningLimit) {
		this.upperWarningLimit = upperWarningLimit;
	}

	/**
	 * Get precision (number of digits after decimal point). Use <code>-1</code>
	 * value as undefined precision.
	 * 
	 * @return precision.
	 */
	public short getPrecision() {
		return precision;
	}

	/**
	 * Set precision.
	 * @param precision precision to set (number of digits after decimal point).
	 * @see #getPrecision()
	 */
	public void setPrecision(short precision) {
		this.precision = precision;
	}

	/**
	 * @see gov.aps.jca.cas.ProcessVariable#getEnumLabels()
	 */
	public String[] getEnumLabels() {
		return enumLabels;
	}

	/**
	 * Set labens.
	 * @param labels the labels to set
	 * @see gov.aps.jca.cas.ProcessVariable#getEnumLabels()
	 */
	public void setEnumLabels(String[] labels) {
		this.enumLabels = labels;
	}

	/**
	 * Read value.
	 * Fills-in DBR, sets timestamp (time when value was written) and copies value from local memory (array).
	 * No status and severity (alarms) are set - to be done in <code>fillInDBR</code> method (extending it).
	 * @see gov.aps.jca.cas.ProcessVariable#read(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableReadCallback)
	 */
	public synchronized CAStatus read(DBR value, ProcessVariableReadCallback asyncReadCallback) throws CAException {

		// fill
		fillInDBR(value);

		// given DBR is always at least TIME
		((TIME)value).setTimeStamp(timestamp);
		
		int minCount = Math.min(count, value.getCount());
		System.arraycopy(this.value, 0, value.getValue(), 0, minCount);
		return CAStatus.NORMAL;
	}

	/**
	 * Set data (units, limits, ...) to DBR.
	 * Data is obtained from this class accessors.
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

		if (value.isPRECSION()) {
			// fill PRECISION
			PRECISION precision = (PRECISION)value;
			precision.setPrecision(getPrecision());
		}

		if (value.isLABELS()) {
			// fill LABELS
			LABELS labels = (LABELS)value;
			labels.setLabels(getEnumLabels());
		}
	}

	/**
	 * Write value.
	 * Copies given value to local memory (array).
	 * If event interest flag <code>interest</code> is set, an event is fired to <code>eventCallback</code>.
	 * @see gov.aps.jca.cas.ProcessVariable#write(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableWriteCallback)
	 */
	public synchronized CAStatus write(DBR value, ProcessVariableWriteCallback asyncWriteCallback) throws CAException {
		this.value = value.getValue();
	    this.count = java.lang.reflect.Array.getLength(this.value);
	    this.timestamp = new TimeStamp();
	    
	    // notify
	    if (interest)
	    {
	    	DBR monitorDBR = AbstractCASResponseHandler.createDBRforReading(this);
			fillInDBR(monitorDBR);
			((TIME)monitorDBR).setTimeStamp(timestamp);
			System.arraycopy(this.value, 0, monitorDBR.getValue(), 0, count);
	    	eventCallback.postEvent(Monitor.VALUE|Monitor.LOG, monitorDBR);
	    }
	    
		return CAStatus.NORMAL;
	}
	
}