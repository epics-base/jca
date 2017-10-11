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

package com.cosylab.epics.caj.util.test;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class AllTests {

	public static Test suite()
    {
        TestSuite suite = new TestSuite(
                "Test for com.cosylab.epics.caj.util.test");
        //$JUnit-BEGIN$
        suite.addTestSuite(InetAddressUtilTest.class);
        suite.addTestSuite(HexDumpTest.class);
        //$JUnit-END$
        return suite;
    }
}
