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

package com.cosylab.epics.caj.cas.handlers;

import gov.aps.jca.CAStatus;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRFactory;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_PRECISION_Double;
import gov.aps.jca.dbr.DBR_PRECISION_Float;
import gov.aps.jca.dbr.DBR_TIME_LABELS_Enum;
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.requests.ExceptionRequest;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.impl.handlers.AbstractCAResponseHandler;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public abstract class AbstractCASResponseHandler extends AbstractCAResponseHandler {

	/**
	 * Context instance.
	 */
	protected CAJServerContext context;

	/**
	 * @param context
	 * @param description
	 */
	public AbstractCASResponseHandler(CAJServerContext context, String description) {
		super(description);
		this.context = context;
	}

	/**
	 * Extract string from payload.
	 * @param buffer buffer to use.
	 * @param start buffer offset to use, if negative current buffer position will be used
	 * @param maxSize maximum payload size reserved for string.
	 * @param setToEnd set to the end of string flag (after zero char)
	 * @return extracted string.
	 */
	public static final String extractString(ByteBuffer buffer, int start, int maxSize, boolean setToEnd)
	{
		if (start < 0)
			start = buffer.position();
		final int bufferEnd = start + maxSize;

		byte[] data = buffer.array();
		int end = start;
		
		// find zero char (string terminator)
		while (data[end] != 0 && end < bufferEnd)
			end++;

		// set to the end
		if (setToEnd && end != bufferEnd)
			buffer.position(end + 1); // skip 0 character
		
		return new String(data, start, end-start);
	}

	/**
	 * Send error (exception) response.
	 * @param transport	transport to use.
	 * @param cid channel ID.
	 * @param errorStatus error status.
	 * @param previousHeader header of the request messages causing this error.
	 * @param message string message, can be <code>null</code>.
	 */
	protected void sendException(Transport transport, int cid, 
			CAStatus errorStatus, ByteBuffer previousHeader, String message)
	{
		try {
			// prepare for reading
			previousHeader.flip();

			new ExceptionRequest(transport,	cid, errorStatus, previousHeader, message).submit();
		} catch (Throwable th) {
			context.getLogger().log(Level.WARNING, "Failed to send exception response for CID: " + cid, th);
		}
	}

	
	
	// type value codes
	private static final int DBR_TIME_OFFSET = 14;
	private static final int DBR_GR_OFFSET = 21;
	private static final int DBR_CTRL_OFFSET = 28;

	/**
	 * Create appropriate DBR structure for reading (of PV type).
	 * Returned DBR structure will be the most complete one (CTRL usually)., PV array length.
	 * @param processVariable	process variable to be read.
	 * @return requested DBR.
	 */
	public static final DBR createDBRforReading(ProcessVariable processVariable)
	{
		return createDBRforReading(processVariable, processVariable.getDimensionSize(0));
	}
	
	/**
	 * Create appropriate DBR structure for reading (of PV type).
	 * Returned DBR structure will be the most complete one (CTRL usually).
	 * @param processVariable	process variable to be read.
	 * @param dataCount	requested data count (returned DBR will have this count).
	 * @return requested DBR.
	 */
	public static final DBR createDBRforReading(ProcessVariable processVariable, int dataCount)
	{
		return createDBRforReading(processVariable, (short)DBR_CTRL_OFFSET, dataCount);
	}
	
		/**
	 * Create appropriate DBR structure for reading (of PV type).
	 * @param processVariable	process variable to be read.
	 * @param dataType 	data type requested from client (not necessary equals to PV type)
	 * @param dataCount	requested data count (returned DBR will have this count).
	 * @return requested DBR.
	 */
	public static final DBR createDBRforReading(ProcessVariable processVariable, short dataType, int dataCount)
	{

		// get native type (plain type) and convert it to TIME type (or GR, CTRL if requested)
		int offset = DBR_TIME_OFFSET;
		if (dataType >= DBR_GR_OFFSET)
			offset = (dataType >= DBR_CTRL_OFFSET) ? DBR_CTRL_OFFSET : DBR_GR_OFFSET;

		// calculate new type
		DBRType type = processVariable.getType();
		int typeValue = type.getValue() + offset;
		
		DBR value;
		
		// special case for TIME_Double, TIME_Float
		// use an "artificial" TIME type with PRECISION
		// they are needed for string formatting
		if (typeValue == 20)
			value = new DBR_PRECISION_Double(dataCount);
		else if (typeValue == 16)
			value = new DBR_PRECISION_Float(dataCount);
		// special case to get the enum table, TIME_Enum -> LABELS_Enum
		// use an "artificial" DBR_TIME_LABELS type to support timestamping 
		else if (typeValue == 17 || typeValue == 24 || typeValue == 31 )
			value = new DBR_TIME_LABELS_Enum(dataCount);
		else
		{
			type = DBRType.forValue(typeValue);
	
			// create native DBR_{TIME, GR, CTRL}_<type> and set default values
			// use required count
			value = DBRFactory.create(type, dataCount);
		}
		
		// set undefined status
		STS dbrSts = (STS)value;
		dbrSts.setSeverity(Severity.INVALID_ALARM);
		dbrSts.setStatus(Status.UDF_ALARM);
		
		// initialize precision 
		if (value.isPRECSION())
			((PRECISION)value).setPrecision((short)-1);

		return value;
	}

}
