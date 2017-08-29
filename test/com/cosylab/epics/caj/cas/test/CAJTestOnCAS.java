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

package com.cosylab.epics.caj.cas.test;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.cas.ProcessVariableReadCallback;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.DBR_Enum;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.util.DefaultServerImpl;
import com.cosylab.epics.caj.cas.util.MemoryProcessVariable;
import com.cosylab.epics.caj.cas.util.examples.CounterProcessVariable;

import junit.framework.TestCase;

/**
 * CAJ test on CAS - this is a manual test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJTestOnCAS extends TestCase {

	/**
	 * Context to be tested.
	 */
	private CAJServerContext context;
	
	/**
	 * Client cntext to be tested.
	 */
	private CAJContext clientContext;

	/**
	 * Server impl.
	 */
	private TestServerImpl serverImpl = null;
	
	/**
	 * Constructor for CAJContextStateTest.
	 * @param methodName
	 */
	public CAJTestOnCAS(String methodName) {
		super(methodName);
	}

	public void test()
	{
		// run CAJ test
		System.err.println("This is a manual 'test', run CAJ tests on this server...");
		//junit.textui.TestRunner.run(AllTests.class);
	}
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		context = new CAJServerContext();
		clientContext = (CAJContext)JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
		DefaultServerImpl server = new DefaultServerImpl();

		MemoryProcessVariable mpv = new MemoryProcessVariable("record1", null, DBR_Double.TYPE, new double[] { 1.23 })
		{
			public synchronized CAStatus read(DBR value, ProcessVariableReadCallback asyncReadCallback) throws CAException {
				// set no alarm status
				((STS)value).setSeverity(Severity.NO_ALARM);
				((STS)value).setStatus(Status.NO_ALARM);
				
				return super.read(value, asyncReadCallback);
			}
			
		};
		mpv.setUpperDispLimit(new Double(100));
		mpv.setLowerDispLimit(new Double(-100));
		
		mpv.setUpperAlarmLimit(new Double(5));
		mpv.setLowerAlarmLimit(new Double(-5));

		mpv.setUpperCtrlLimit(new Double(100));
		mpv.setLowerCtrlLimit(new Double(-100));

		mpv.setUpperWarningLimit(new Double(3));
		mpv.setLowerWarningLimit(new Double(-3));

		mpv.setUnits("units");
		mpv.setPrecision((short)2);
		server.registerProcessVaribale(mpv);
		
		
		// COUNTER - 1s increment
		CounterProcessVariable cpv = new CounterProcessVariable("record2", null, 0, Integer.MAX_VALUE, 1, 1000, -Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE, Integer.MAX_VALUE);
		server.registerProcessVaribale(cpv);
		
		// ENUM
		MemoryProcessVariable enumPV = new MemoryProcessVariable("enum", null, DBR_Enum.TYPE, new short[] { 0 }) 
		{
			/**
			 * Test labels.
			 */
			private final String[] LABELS =
			        new String[] {  "zeroString", "oneString", "twoString", "threeString", "fourString", "fiveString",
			            			"sixString", "sevenString", "8s", "9s", "10s", "11s", "12s", "13s", "14s", "15s" };

			/* (non-Javadoc)
			 * @see com.cosylab.epics.caj.cas.util.MemoryProcessVariable#getEnumLabels()
			 */
			public String[] getEnumLabels() {
				return LABELS;
			}
			
		};
		server.registerProcessVaribale(enumPV);
		
		context.initialize(server);

		context.run(0);
/*
		new Thread(new Runnable()
		{
			public void run() {
				try
				{
					context.run(0);
				} catch (Throwable th) {
					th.printStackTrace();
				}
			}
		}, this.getClass().getName()).start();
*/
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		if (clientContext != null && !clientContext.isDestroyed())
			clientContext.destroy();
		clientContext = null;

		if (context != null && !context.isDestroyed())
			context.destroy();
		context = null;

		if (serverImpl != null)
			serverImpl.destroy();
		serverImpl = null;
	}

	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(CAJTestOnCAS.class);
	}
}
