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
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;

import com.cosylab.epics.caj.CAJContext;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJContextPendPoolFlushTest extends TestCase {

	private class PutListenerImpl implements PutListener
	{
	    public CAStatus status = null;
		/**
		 * @see gov.aps.jca.event.PutListener#putCompleted(gov.aps.jca.event.PutEvent)
		 */
		public synchronized void putCompleted(PutEvent ev) {
		    status = ev.getStatus();
		    this.notifyAll();
		}
	}

	private class GetListenerImpl implements GetListener
	{
	    public DBR value = null;
	    public CAStatus status = null;
	    
		/**
		 * @see gov.aps.jca.event.GetListener#getCompleted(gov.aps.jca.event#GetEvent)
		 */
		public synchronized void getCompleted(GetEvent ev) {
		    status = ev.getStatus();
		    value = ev.getDBR();
		    this.notifyAll();
		}
	}

	/**
	 * Context to be tested.
	 */
	private CAJContext context;
	
	/**
	 * Constructor for CAJContextPendPoolFlushTest.
	 * @param methodName
	 */
	public CAJContextPendPoolFlushTest(String methodName) {
		super(methodName);
	}
	
	/**
	 * Simple pend IO timing.
	 */
	public void testPendIOTiming() throws CAException, TimeoutException, InterruptedException {
	    // should not block...
	    context.pendIO(-1.0);
	    assertTrue(true);
	    context.pendIO(0.0);
	    assertTrue(true);
	    
	    long time = System.currentTimeMillis();
	    context.pendIO(10.0);
	    time = System.currentTimeMillis() - time;
	    assertTrue(time < 3000);

	    time = System.currentTimeMillis();
	    context.createChannel("record1");
	    context.pendIO(10.0);
	    time = System.currentTimeMillis() - time;
	    assertTrue(time < 3000);
	}

	/**
	 * Simple pend event timing.
	 */
	public void testPendEventTiming() throws CAException, TimeoutException, InterruptedException {
	    long time = System.currentTimeMillis();
	    context.pendEvent(0.5);
	    time = System.currentTimeMillis() - time;
	    assertTrue(time >= 500);
	    
	    // should not block...
	    context.pendEvent(-1.0);
	    assertTrue(true);
	    
	    Thread thread = new Thread(
	            new Runnable() {
	                public void run()
	                {
	            	    try
                        {
                            // ... blocks forever
                            context.pendEvent(0.0);
                        } catch (Throwable th) {
                            fail("pendEvent(0.0) not accepted");
                        }
	                }
	            }, "pendEvent(0.0) test thread");
	    thread.start();
	    thread.join(1100);
	    // terminate
	    if (thread.isAlive())
	        thread.interrupt();
	    else
	        fail("pendEvent(0.0) does not block forever...");
	    
	}

	/**
	 * Simple get tests.
	 */
	public void testAll() throws CAException, TimeoutException, InterruptedException {
	    
		// connect requests are always issued immediately
		// w/o not related to pendIO at all; w/ callback yes...
		// (no pend_event needed)
		Channel channel = context.createChannel("record1");
					
		// wait until connected
		context.pendIO(5.0);
		assertEquals(Channel.CONNECTED, channel.getConnectionState());

		// set to test...
	    channel.put(3.21);
	    // put does not count...
	    assertEquals(true, context.testIO());
		context.pendIO(5.0);
	    assertEquals(true, context.testIO());
		
		// -----------------------------------------------------------

		// get request w/o callbacks will wait until flush/pendIO is called
		DBR dbr = channel.get();
		assertTrue(dbr != null);
	    assertEquals(false, context.testIO());
		context.pendIO(3.0);
	    assertEquals(true, context.testIO());
		assertEquals(3.21, ((double[])dbr.getValue())[0], 0.0);
		
		// get request w/o callbacks will wait until flush/pendIO is called
		dbr = channel.get();
		assertTrue(dbr != null);
		// ... means until complete (up to infinity)
	    assertEquals(false, context.testIO());
		context.pendIO(0.0);
	    assertEquals(true, context.testIO());
		assertEquals(3.21, ((double[])dbr.getValue())[0], 0.0);

		// -----------------------------------------------------------

		// get request w/o callbacks will wait until flush/pendIO is called
		dbr = channel.get();
		assertTrue(dbr != null);
		DBR dbr2 = channel.get();
		assertTrue(dbr2 != null);
		context.pendIO(3.0);
	    assertEquals(true, context.testIO());
		assertEquals(3.21, ((double[])dbr2.getValue())[0], 0.0);
		assertEquals(3.21, ((double[])dbr.getValue())[0], 0.0);
		
		// -----------------------------------------------------------
		
		// get request w/ callbacks are always issued immediately
		// not related to pendIO at all, but require pend_event (to be flushed also)
		GetListenerImpl listener = new GetListenerImpl();
		channel.get(listener);
	    assertEquals(true, context.testIO());
		// wait until flushed...
		Thread.sleep(500);
		assertEquals(null, listener.status);
		synchronized (listener)
		{
			// flush & get event back
			context.pendEvent(1.0);
		    assertEquals(true, context.testIO());
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
		assertEquals(3.21, ((double[])listener.value.getValue())[0], 0.0);
		
		// -----------------------------------------------------------

		// put request w/o callbacks will wait until flush is called
		channel.put(1.11);
		// ... still old (request in buffer)
		// but put doesn not count
	    assertEquals(true, context.testIO());
		context.flushIO();

		// ... read new value
		dbr = channel.get();
	    assertEquals(false, context.testIO());
		context.pendIO(1.0);
	    assertEquals(true, context.testIO());
		assertEquals(1.11, ((double[])dbr.getValue())[0], 0.0);

		// -----------------------------------------------------------
		
		// get request w/ callbacks are always issued immediately
		PutListenerImpl plistener = new PutListenerImpl();
		channel.put(3.33, plistener);
	    assertEquals(true, context.testIO());
		// wait until flushed...
		Thread.sleep(500);
		assertEquals(null, plistener.status);
		synchronized (plistener)
		{
			// flush & get event back
			context.pendEvent(0.000001);
		    assertEquals(true, context.testIO());
			plistener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, plistener.status);

		// ... read new value
		dbr = channel.get();
	    assertEquals(false, context.testIO());
		context.pendIO(1.0);
	    assertEquals(true, context.testIO());
		assertEquals(3.33, ((double[])dbr.getValue())[0], 0.0);
	
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		context = new CAJContext();
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
		junit.textui.TestRunner.run(CAJContextPendPoolFlushTest.class);
	}
	
}
