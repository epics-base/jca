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
import gov.aps.jca.Channel;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBR_Int;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.cas.CAJServerContext;

import junit.framework.TestCase;

/**
 * CAS server impl. test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ServerImplTest extends TestCase {

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
	public ServerImplTest(String methodName) {
		super(methodName);
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		context = new CAJServerContext();
		clientContext = (CAJContext)JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
	}

	/**
	 * Sync server, no simulated exceptions.
	 * @throws Exception
	 */
	public void testSyncOK() throws Exception
	{
		internalTest(false, 0, false);
	}
	
	/**
	 * Sync server, with simulated exceptions.
	 * @throws Exception
	 */
	public void testSyncException() throws Exception
	{
		internalTest(false, 0, true);
	}

	/**
	 * Async server, no simulated exceptions.
	 * @throws Exception
	 */
	public void testAsyncOK() throws Exception
	{
		internalTest(true, 1000, false);
	}
	
	/**
	 * Async server, with simulated exceptions.
	 * @throws Exception
	 */
	public void testAsyncException() throws Exception
	{
		internalTest(true, 1000, true);
	}

	/**
	 * Fast async server, no simulated exceptions.
	 * @throws Exception
	 */
	public void testFastAsyncOK() throws Exception
	{
		internalTest(true, 0, false);
	}
	
	/**
	 * Fast async server, with simulated exceptions.
	 * @throws Exception
	 */
	public void testFastAsyncException() throws Exception
	{
		internalTest(true, 0, true);
	}

	private void internalTest(boolean async, long asyncProcessTime, boolean simulateExceptions) throws Exception
	{
		serverImpl = new TestServerImpl(async, asyncProcessTime, simulateExceptions);
		initializeCAJServerContext(serverImpl);
		
		final double waitTime = asyncProcessTime/1000.0 + 3.0;
		
		// PV does not exist
		Channel channel = clientContext.createChannel("NONEXISTANT");
		try
		{
			clientContext.pendIO(waitTime);
			// should always fail
			fail();
		}
		catch (TimeoutException te) {
			// this is OK
		}
		channel.destroy();

	
		// PV exists
		channel = clientContext.createChannel("TEST");
		if (simulateExceptions)
		{
			try
			{
				clientContext.pendIO(waitTime);
				// should always fail
				fail();
			}
			catch (TimeoutException te) {
				// this is OK
			}
		}
		else
		{
			clientContext.pendIO(waitTime);
			// if it comes to here, it is OK 
		}
		channel.destroy();
	}

	/**
	 * Initialize.
	 * @param server
	 * @throws CAException
	 */
	protected void initializeCAJServerContext(TestServerImpl server) throws CAException
	{
		// simple in-memory PV
		server.createMemoryProcessVariable("TEST", DBR_Int.TYPE, new int[] { 0 });
		
		context.initialize(server);

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
		junit.textui.TestRunner.run(ServerImplTest.class);
	}
}
