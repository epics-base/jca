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

package com.cosylab.epics.caj.test;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import gov.aps.jca.CAException;

import com.cosylab.epics.caj.CAJConstants;
import com.cosylab.epics.caj.CAJContext;

import junit.framework.TestCase;

/**
 * CAJContext debug test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJContextDebugTest extends TestCase {

    /**
     * Test handler - only counts logs.
     */
    private class TestHandler extends Handler {

    	/**
    	 * Log counter.
    	 */
    	private int logs = 0; 
    	
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
    		// noop
    	}

    	/**
    	 * Prints the log record to the console using the current formatter, if the 
    	 * log record is loggable.
    	 * @param record the log record to publish
    	 * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
    	 */
    	public void publish(LogRecord record) {
    		if (isLoggable(record))
    		    logs++;
    	}
    	
        /**
         * @return Returns the logs.
         */
        public int getLogs() {
            return logs;
        }
    }
    
   	/**
	 * Constructor for CAJContextDebugTest.
	 * @param methodName
	 */
	public CAJContextDebugTest(String methodName) {
		super(methodName);
	}

	/**
	 * Tests CAJ_DEBUG property to turn ebug mode on.
	 */
	public void testDebug() throws CAException {
	    boolean exists = System.getProperties().containsKey(CAJConstants.CAJ_DEBUG);
	    try
	    {
		    if (!exists)
		        System.setProperty(CAJConstants.CAJ_DEBUG, "true");
		    
			CAJContext context = new CAJContext();
	
			TestHandler testHandler = new TestHandler();
			context.getLogger().addHandler(testHandler);
			
			context.initialize();
			assertTrue(context.isInitialized());
	
			// some logs are expected
			assertTrue(testHandler.getLogs() > 0);
			
			context.destroy();
			assertTrue(context.isDestroyed());
	    }
	    finally
	    {
	        if (!exists)
	            System.getProperties().remove(CAJConstants.CAJ_DEBUG);
	    }
	}
	
	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(CAJContextDebugTest.class);
	}
}
