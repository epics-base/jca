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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * All CAJ tests.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class AllTests {

	/**
	 * Assembles and returns a test suite containing all known tests.
	 * @return A non-null test suite.
	 */
	public static Test suite() {

		TestSuite suite = new TestSuite();

		// repeater should not be running...
		suite.addTestSuite(CAJContextRepeaterTest.class);

		suite.addTestSuite(CAJContextStateTest.class);
		suite.addTestSuite(CAJContextConfigurationTest.class);
		suite.addTestSuite(CAJContextListenersTest.class);
		suite.addTestSuite(CAJContextChannelTest.class);
		suite.addTestSuite(CAJChannelTest.class);
		suite.addTestSuite(CAJMonitorTest.class);
		suite.addTestSuite(CAJContextPendPoolFlushTest.class);
		suite.addTestSuite(CAJChannelGetTest.class);
		suite.addTestSuite(CAJChannelPutTest.class);
		
		suite.addTestSuite(CAJContextDebugTest.class);

		return suite;
	}

	/**
	 * Runs the test suite.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(AllTests.class);
	}
}





