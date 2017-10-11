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
import gov.aps.jca.TimeoutException;

import com.cosylab.epics.caj.cas.CAJServerContext;

import junit.framework.TestCase;

/**
 * CAJContext state transition test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJServerContextStateTest extends TestCase {

	/**
	 * Context to be tested.
	 */
	private CAJServerContext context;
	
	/**
	 * Constructor for CAJContextStateTest.
	 * @param methodName
	 */
	public CAJServerContextStateTest(String methodName) {
		super(methodName);
	}

	/**
	 * Normal initialization.
	 */
	public void testInitialize() throws CAException {
		assertTrue(!context.isInitialized());
		assertTrue(!context.isDestroyed());
		context.initialize(new DummyServerImpl());
		assertTrue(context.isInitialized());
		assertTrue(!context.isDestroyed());
	}
	
	/**
	 * Normal destruction.
	 */
	public void testDestruction() throws CAException {
		testInitialize();
		context.destroy();
		assertTrue(!context.isInitialized());
		assertTrue(context.isDestroyed());
	}

	/**
	 * Method call after destruction.
	 */
	public void testCallAfterDestruction() throws CAException, TimeoutException {
		testDestruction();
		
		try
		{
			context.addContextExceptionListener(null);
			fail("addContextExceptionListener() call allowed after destruction.");
		}
		catch (IllegalStateException ise)
		{
			// ok
		}
		
		try
		{
			context.addContextMessageListener(null);
			fail("addContextMessageListener() call allowed after destruction.");
		}
		catch (IllegalStateException ise)
		{
			// ok
		}

		try
		{
			context.initialize(new DummyServerImpl());
			fail("initialize(new DummyServerImpl()) call allowed after destruction.");
		}
		catch (IllegalStateException ise)
		{
			// ok
		}

		try
		{
			context.removeContextExceptionListener(null);
			fail("removeContextExceptionListener() call allowed after destruction.");
		}
		catch (IllegalStateException ise)
		{
			// ok
		}

		try
		{
			context.removeContextMessageListener(null);
			fail("removeContextMessageListener() call allowed after destruction.");
		}
		catch (IllegalStateException ise)
		{
			// ok
		}

		try
		{
			context.run(1);
			fail("run() call allowed after destruction.");
		}
		catch (IllegalStateException ise)
		{
			// ok
		}

		assertTrue(!context.isInitialized());
		assertTrue(context.isDestroyed());

	}

	/**
	 * Destruction w/o initialization.
	 */
	public void testDestructionWithoutInitialization() throws CAException {
		assertTrue(!context.isInitialized());
		assertTrue(!context.isDestroyed());
		context.destroy();
		assertTrue(!context.isInitialized());
		assertTrue(context.isDestroyed());
	}

	/**
	 * Multiple destruction.
	 */
	public void testMultipleDestruction() throws CAException {
		testDestruction();
		
		try
		{
			context.destroy();
			fail("Multiple destruction allowed.");
		}
		catch (IllegalStateException ise)
		{
			// ok
		}
		
		assertTrue(!context.isInitialized());
		assertTrue(context.isDestroyed());
	}


	/**
	 * Normal initialization.
	 */
	public void testMultipleInitialize() throws CAException {
		testInitialize();
		
		try
		{
			context.initialize(new DummyServerImpl());
			fail("Multiple initialization allowed.");
		}
		catch (IllegalStateException ise)
		{
			// ok
		}
		
		assertTrue(context.isInitialized());
		assertTrue(!context.isDestroyed());
	}

	/**
	 * Normal initialization.
	 */
	public void testMultipleRun() throws CAException {
		testInitialize();
		
		context.run(1);
		
		try
		{
			context.run(1);
			fail("Multiple run allowed.");
		}
		catch (IllegalStateException ise)
		{
			// ok
		}

		assertTrue(context.isInitialized());
		assertTrue(!context.isDestroyed());
	}

	/**
	 * Test (exception test) printInfo method.
	 */
	public void testPrintInfo() throws CAException {
		context.printInfo();
		context.initialize(new DummyServerImpl());
		context.printInfo();
		context.destroy();
		context.printInfo();
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		context = new CAJServerContext();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		if (!context.isDestroyed())
			context.destroy();
		context = null;
	}

	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(CAJServerContextStateTest.class);
	}
}
