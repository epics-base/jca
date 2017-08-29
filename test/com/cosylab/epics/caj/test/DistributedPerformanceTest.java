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

import com.cosylab.epics.caj.CAJContext;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;

import junit.framework.TestCase;

/**
 * Get performance test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class DistributedPerformanceTest extends TestCase {

	private static final int SERVERS = 3;
	private static final int REPEAT = 10;
	
	private class GetListenerImpl implements GetListener
	{
	    private volatile int count = 0;
	    private int limit;
	    
	    public GetListenerImpl(int limit) {
	        this.limit = limit;
	    }
	    
		/**
		 * @see gov.aps.jca.event.GetListener#getCompleted(gov.aps.jca.event#GetEvent)
		 */
		public synchronized void getCompleted(GetEvent ev) {
		    count++;
		    if (count == limit)
		        this.notifyAll();
		}
	}

	/**
	 * Context to be tested.
	 */
	private Context context;
	
    /**
	 * Channel to be tested.
	 */
    private Channel[] channel = new Channel[3];
    private int[] channelCount = new int[3];
    private DBRType[] channelType = new DBRType[3];
    
	/**
	 * Constructor for CAJChannelGetTest.
	 * @param methodName
	 */
	public DistributedPerformanceTest(String methodName) {
		super(methodName);
	}

	/**
	 * Sync get test.
	 */
	private void internalSyncGetTest(int bulkSize)
		throws CAException, TimeoutException, InterruptedException {
	
		long sum = 0;
		long sumi = 0;
		for (int ii = 0; ii < REPEAT; ii++) {
	
		    long time1 = System.currentTimeMillis();
		    for (int i = 0; i < bulkSize; i++)
				for (int s = 0; s < SERVERS; s++) 
				    channel[s].get(channelType[s], channelCount[s]);
		    long time2 = System.currentTimeMillis();
		    context.pendIO(0.0);
		    long time3 = System.currentTimeMillis();
		    sumi += time2-time1;
		    sum += time3-time1;
	
		    System.out.println();
		    System.out.println(bulkSize + " sync get(s) results");
		    System.out.println("----------------------------------");
		    System.out.println("Time to issue requests [ms]: " + (time2-time1));
		    System.out.println("Time get all responses [ms]: " + (time3-time1));
		    System.out.println();
		}
	    System.out.println("----------------------------------");
	    System.out.println("Avg time to issue requests [ms]: " + sumi/(REPEAT*SERVERS));
	    System.out.println("Avg time get all responses [ms]: " + sum/(REPEAT*SERVERS));
		System.out.println("----------------------------------");
	    System.out.println("----------------------------------");
	    System.out.println("----------------------------------");
	    System.out.println();
	}
	
	/**
	 * ASync get test.
	 */
	private void internalAsyncGetTest(int bulkSize)
		throws CAException, TimeoutException, InterruptedException {
	    
		long sum = 0;
		long sumi = 0;
		for (int ii = 0; ii < REPEAT; ii++) {
		    GetListener listener = new GetListenerImpl(bulkSize*SERVERS);
		    
		    long time1 = System.currentTimeMillis();
		    for (int i = 0; i < bulkSize; i++)
				for (int s = 0; s < SERVERS; s++) 
				    channel[s].get(channelType[s], channelCount[s], listener);
		    long time2 = System.currentTimeMillis();
			synchronized (listener)
			{
				context.flushIO();
				listener.wait(100000);
			}
		    long time3 = System.currentTimeMillis();
		    sumi += time2-time1;
		    sum += time3-time1;
	
		    System.out.println();
		    System.out.println(bulkSize + " async get(s) results");
		    System.out.println("----------------------------------");
		    System.out.println("Time to issue requests [ms]: " + (time2-time1));
		    System.out.println("Time get all responses [ms]: " + (time3-time1));
		    System.out.println();
		}
	    System.out.println("----------------------------------");
	    System.out.println("Avg time to issue requests [ms]: " + sumi/(REPEAT*SERVERS));
	    System.out.println("Avg time get all responses [ms]: " + sum/(REPEAT*SERVERS));
		System.out.println("----------------------------------");
	    System.out.println("----------------------------------");
	    System.out.println("----------------------------------");
	    System.out.println();
	}
	
	/**
	 * Sync get test (flush called after each request).
	 */
	private void internalSyncGetTestNoBulk(int bulkSize)
		throws CAException, TimeoutException, InterruptedException {
	    
		long sum = 0;
		long sumi = 0;
		for (int ii = 0; ii < REPEAT; ii++) {
	
		    long time1 = System.currentTimeMillis();
		    for (int i = 0; i < bulkSize; i++) 
				for (int s = 0; s < SERVERS; s++) {
				    channel[s].get(channelType[s], channelCount[s]);
				    context.flushIO();
				}
		    
		    long time2 = System.currentTimeMillis();
		    context.pendIO(0.0);
		    long time3 = System.currentTimeMillis();
		    sumi += time2-time1;
		    sum += time3-time1;
	
		    System.out.println();
		    System.out.println(bulkSize + " (no bulk) sync get(s) results");
		    System.out.println("----------------------------------");
		    System.out.println("Time to issue requests [ms]: " + (time2-time1));
		    System.out.println("Time get all responses [ms]: " + (time3-time1));
		    System.out.println();
		}
	    System.out.println("----------------------------------");
	    System.out.println("Avg time to issue requests [ms]: " + sumi/(REPEAT*SERVERS));
	    System.out.println("Avg time get all responses [ms]: " + sum/(REPEAT*SERVERS));
		System.out.println("----------------------------------");
	    System.out.println("----------------------------------");
	    System.out.println("----------------------------------");
	    System.out.println();
	}
	
	/**
	 * ASync get test  (flush called after each request).
	 */
	private void internalAsyncGetTestNoBulk(int bulkSize)
		throws CAException, TimeoutException, InterruptedException {
	    
		long sum = 0;
		long sumi = 0;
		for (int ii = 0; ii < REPEAT; ii++) {
		    GetListener listener = new GetListenerImpl(bulkSize);
		    
		    long time1 = System.currentTimeMillis();
		    for (int i = 0; i < bulkSize; i++) 
				for (int s = 0; s < SERVERS; s++) {
				    channel[s].get(channelType[s], channelCount[s], listener);
				    context.flushIO();
				}
		    long time2 = System.currentTimeMillis();
			synchronized (listener)
			{
				context.flushIO();
				listener.wait(100000);
			}
		    long time3 = System.currentTimeMillis();
		    sumi += time2-time1;
		    sum += time3-time1;
	
		    System.out.println();
		    System.out.println(bulkSize + " (no bulk) async. get(s) results");
		    System.out.println("----------------------------------");
		    System.out.println("Time to issue requests [ms]: " + (time2-time1));
		    System.out.println("Time get all responses [ms]: " + (time3-time1));
		    System.out.println();
		}
	    System.out.println("----------------------------------");
	    System.out.println("Avg time to issue requests [ms]: " + sumi/(REPEAT*SERVERS));
	    System.out.println("Avg time get all responses [ms]: " + sum/(REPEAT*SERVERS));
		System.out.println("----------------------------------");
	    System.out.println("----------------------------------");
	    System.out.println("----------------------------------");
	    System.out.println();
	}

	public void testSyncGet() throws CAException, TimeoutException, InterruptedException 
	{
	    /*
	    internalSyncGetTest(10);
	    internalSyncGetTest(100);
	    internalSyncGetTest(250);
	    internalSyncGetTest(500);
	    internalSyncGetTest(1000);
	    internalSyncGetTest(2500);
	    internalSyncGetTest(5000);
	    */
	    internalSyncGetTest(10000);
	}
	
	public void testAsyncGet() throws CAException, TimeoutException, InterruptedException 
	{
	    /*
	    internalAsyncGetTest(10);
	    internalAsyncGetTest(100);
	    internalAsyncGetTest(250);
	    internalAsyncGetTest(500);
	    internalAsyncGetTest(1000);
	    internalAsyncGetTest(2500);
	    internalAsyncGetTest(5000);
	    */
	    internalAsyncGetTest(10000);
	}

	public void testSyncGetNoBulk() throws CAException, TimeoutException, InterruptedException 
	{
	    /*
	    internalSyncGetTestNoBulk(10);
	    internalSyncGetTestNoBulk(100);
	    internalSyncGetTestNoBulk(250);
	    internalSyncGetTestNoBulk(500);
	    internalSyncGetTestNoBulk(1000);
	    internalSyncGetTestNoBulk(2500);
	    internalSyncGetTestNoBulk(5000);
	    */
	    internalSyncGetTestNoBulk(10000);
	}
	
	public void testAsyncGetNoBulk() throws CAException, TimeoutException, InterruptedException 
	{
	    /*
	    internalAsyncGetTestNoBulk(10);
	    internalAsyncGetTestNoBulk(100);
	    internalAsyncGetTestNoBulk(250);
	    internalAsyncGetTestNoBulk(500);
	    internalAsyncGetTestNoBulk(1000);
	    internalAsyncGetTestNoBulk(2500);
	    internalAsyncGetTestNoBulk(5000);
	    */
	    internalAsyncGetTestNoBulk(10000);
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
	    //System.getProperties().put(CAJContext.CAJ_SINGLE_THREADED_MODEL, "");
        //System.setProperty("com.cosylab.epics.caj.CAJContext.event_dispatcher", "gov.aps.jca.event.QueuedEventDispatcher");
		context = new CAJContext();
	    //context = new ThreadSafeContext();
		for (int i = 1; i <= SERVERS; i++) {
		    channel[i-1] = context.createChannel("record" + String.valueOf(i));
		    context.pendIO(5.0);
		    assertEquals(Channel.CONNECTED, channel[i-1].getConnectionState());
		    channelCount[i-1] = channel[i-1].getElementCount();
		    channelType[i-1] = channel[i-1].getFieldType();
		}
		System.out.println();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		//if (!context.isDestroyed())
			context.destroy();
		context = null;
	}

	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(DistributedPerformanceTest.class);
	}
	
}
