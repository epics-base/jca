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

import gov.aps.jca.CAException;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.CARepeater;
import com.cosylab.epics.caj.impl.CAConstants;

import junit.framework.TestCase;

/**
 * CAJContext repeater test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJContextRepeaterTest extends TestCase {

   	/**
	 * Constructor for CAJContextRepeaterTest.
	 * @param methodName
	 */
	public CAJContextRepeaterTest(String methodName) {
		super(methodName);
	}

	/**
	 * Tests repeater starting/registration.
	 */
	public void testRepeater() throws CAException {
	    boolean exists = System.getProperties().containsKey(CARepeater.CA_DISABLE_REPEATER);
	    try
	    {
		    if (!exists)
		        System.setProperty(CARepeater.CA_DISABLE_REPEATER, "true");
		    
			CAJContext context = new CAJContext();
			context.initialize();
			
			// already running... issue warning and do not fail
			try {
	            Thread.sleep(300);
	        } catch (InterruptedException e) { }
			if (context.isRegistrationConfirmed()) {
			    System.err.println("CA repeater already running, repeater test skipped...");
			    return;
			}
			
			assertEquals(false, context.isRegistrationConfirmed());
			try {
	            Thread.sleep(1000);
	        } catch (InterruptedException e) { }
			assertEquals(false, context.isRegistrationConfirmed());
	
			// no repeater case
			context.destroy();
			assertTrue(context.isDestroyed());
	
	
			// test repeater
			if (!exists)
			{
			
			    context = new CAJContext();
			    context.initialize();
			    assertEquals(false, context.isRegistrationConfirmed());
	
		        System.getProperties().remove(CARepeater.CA_DISABLE_REPEATER);
		        // start here (later)...
		        try {
		        	CARepeater.startRepeater(CAConstants.CA_REPEATER_PORT);
		        } catch (Throwable th) {
		        	throw new CAException("Failed to start repeater.", th);
		        }
		        // not nice...
				try {
		            Thread.sleep(2000);
		        } catch (InterruptedException e) { }
				assertEquals(true, context.isRegistrationConfirmed());
				
				context.destroy();
			}
	    }
		finally
		{
		    if (!exists)
		        System.getProperties().remove(CARepeater.CA_DISABLE_REPEATER);
		}
	}
	
	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(CAJContextRepeaterTest.class);
	}
}
