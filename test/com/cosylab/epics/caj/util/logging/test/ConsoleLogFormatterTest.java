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

package com.cosylab.epics.caj.util.logging.test;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.cosylab.epics.caj.util.logging.ConsoleLogFormatter;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ConsoleLogFormatterTest extends TestCase {

    /**
     * @param methodName
     */
    public ConsoleLogFormatterTest(String methodName) {
        super(methodName);
    }
    
    /*
     * Class under test for String format(LogRecord)
     */
    public void testFormatLogRecord()
    {
        Formatter formatter = new ConsoleLogFormatter();
        LogRecord msg = new LogRecord(Level.INFO, "This is a message.");
        System.out.println(formatter.format(msg));

        msg = new LogRecord(Level.INFO, "This is an exception message.");
        msg.setThrown(new IllegalArgumentException("example of exception"));
        msg.setSourceClassName(this.getClass().getName());
        msg.setSourceMethodName("testFormatLogRecord");

        System.out.println(formatter.format(msg));
    }

    /*
     * Class under test for String format(LogRecord)
     */
    public void testFormatLogRecordVerbose()
    {
	    boolean exists = System.getProperties().containsKey(ConsoleLogFormatter.KEY_TRACE);
	    if (!exists)
	        System.setProperty(ConsoleLogFormatter.KEY_TRACE, "true");
	    
        testFormatLogRecord();

        if (!exists)
	        System.getProperties().remove(ConsoleLogFormatter.KEY_TRACE);
    }

}
