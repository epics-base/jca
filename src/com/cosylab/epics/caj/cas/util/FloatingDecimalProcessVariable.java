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

import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.PRECISION;

/**
 * Abstract convenient floating decimal process variable implementation.
 * This implementation specializes <code>NumericProcessVariable</code> - adds precision support.
 * Implementation does not handle status, severity and alarms.
 */
public abstract class FloatingDecimalProcessVariable extends NumericProcessVariable 
{
	/**
	 * Floating decimal PV constructor.
	 * @param name process variable name.
	 * @param eventCallback	event callback, can be <code>null</code>.
	 */
	public FloatingDecimalProcessVariable(String name, ProcessVariableEventCallback eventCallback)
	{
		super(name, eventCallback);
	}

	/**
	 * Get precision (number of digits after decimal point).
	 * Use <code>-1</code> value as undefined precision.
	 * @return precision.
	 */
	public abstract short getPrecision();

	/**
	 * Set data (units, limits, ...) and precision to DBR.
	 * @param value DBR to fill-in.
	 */
	public void fillInDBR(DBR value)
	{
		super.fillInDBR(value);
		
		if (value.isPRECSION()) {
			// fill PRECISION
			PRECISION precision = (PRECISION)value;
			precision.setPrecision(getPrecision());
		}
	}
}