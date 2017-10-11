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

package com.cosylab.epics.caj.util.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * An implementation of <code>java.util.logging.Formatter</code>.
 * Produces single line log reports meant to go to the console.
 * @author	Matej Sekoranja (matej.sekoranjaATcosylab.com)
 */
public class ConsoleLogFormatter extends Formatter
{
	/**
	 * System property key to enable trace messages.
	 */
	public static String KEY_TRACE = "TRACE";
	/**
	 * Line separator string. 
	 */
	private boolean showTrace = System.getProperties().containsKey(KEY_TRACE);

	/**
	 * Line separator string. 
	 */
	private static String lineSeparator = System.getProperty("line.separator");

	/**
	 * ISO 8601 date formatter.
	 */
	private SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	/**
	 * Date object (used not to recreate it every time).
	 */
	private Date date = new Date();

	/**
	 * Format the given LogRecord.
	 * @param record the log record to be formatted.
	 * @return a formatted log record
	 */
	public String format(LogRecord record)
	{
		StringBuffer sb = new StringBuffer(128);
	
		synchronized (date)
		{
			date.setTime(record.getMillis());
			sb.append(timeFormatter.format(date));
		}
		
		/*
		if (record.getLoggerName() != null)
		{
			sb.append(' ');
			sb.append('[');
			sb.append(record.getLoggerName());
			sb.append(']');
		}
		*/

		sb.append(' ');
		sb.append(record.getMessage());
		sb.append(' ');

		//sb.append(record.getLevel().getLocalizedName());

		// trace
		if (showTrace)
		{
			// source
			sb.append('[');
			if (record.getSourceClassName() != null) 	
				sb.append(record.getSourceClassName());
		
			// method name
			if (record.getSourceMethodName() != null)
			{	
				sb.append('#');
				sb.append(record.getSourceMethodName());
			}
			sb.append(']');
		}
		
		sb.append(lineSeparator);


		// exceptions
		if (record.getThrown() != null)
		{
			/*
			try
			{
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {}
			*/
			record.getThrown().printStackTrace();
		}

		return new String(sb);
	}

}
