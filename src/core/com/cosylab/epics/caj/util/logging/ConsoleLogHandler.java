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

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Implementation of Java Logging API handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ConsoleLogHandler extends Handler {

	/**
	 * Logging formatter.
	 */
	private Formatter formatter; 
	
	/**
	 * Default constructor.
	 */
	public ConsoleLogHandler() {
		this(new ConsoleLogFormatter());
	}

	/**
	 * Construct handler with using giver formatter.
	 * @param	formatter	console log formatter, non-<code>null</code>.
	 */
	public ConsoleLogHandler(Formatter formatter) {
		this.formatter = formatter;
	}

	/**
	 * @see java.util.logging.Handler#close()
	 */
	public void close() throws SecurityException {
		// noop
	}

	/**
	 * @see java.util.logging.Handler#flush()
	 */
	public void flush() {
		System.out.flush();
	}

	/**
	 * Prints the log record to the console using the current formatter, if the 
	 * log record is loggable.
	 * @param record the log record to publish
	 * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
	 */
	public void publish(LogRecord record) {
		if (isLoggable(record))
			System.out.print(formatter.format(record));
	}

}
