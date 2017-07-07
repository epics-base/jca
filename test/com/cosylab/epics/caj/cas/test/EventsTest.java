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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.util.DefaultServerImpl;
import com.cosylab.epics.caj.cas.util.examples.CounterProcessVariable;
import com.cosylab.epics.caj.impl.CATransport;
import com.cosylab.epics.caj.impl.requests.EventsOffRequest;
import com.cosylab.epics.caj.impl.requests.EventsOnRequest;

import junit.framework.TestCase;

/**
 * Get (encoder) and put test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class EventsTest extends TestCase {

    /**
     * Implementation of MonitorListener.
     */
    private class MonitorListenerImpl implements MonitorListener {
        /* (non-Javadoc)
         * @see gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.MonitorEvent)
         */
        
        public volatile CAStatus status;
        public volatile DBR response;
        public volatile int counter = 0;
        
        public synchronized void monitorChanged(MonitorEvent ev)
        {
        	counter++;
            status = ev.getStatus();
            response = ev.getDBR();
            this.notifyAll();
        }

        public synchronized void reset()
        {
        	counter = 0;
            status = null;
            response = null;
        }
    }

    /**
     * Implementation of MonitorListener.
     */
    private class QueuedMonitorListenerImpl implements MonitorListener {
        /* (non-Javadoc)
         * @see gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.MonitorEvent)
         */
  
    	public ArrayList queue = new ArrayList();
        
        public synchronized void monitorChanged(MonitorEvent ev)
        {
        	queue.add(ev);

        }
    }
    
    /**
	 * Context to be tested.
	 */
	private CAJServerContext context;
	
	/**
	 * Client cntext to be tested.
	 */
	private CAJContext clientContext;

	/**
	 * Constructor for EventTest.
	 * @param methodName
	 */
	public EventsTest(String methodName) {
		super(methodName);
	}

	/**
	 * Check if monitoring works, also events on/off.
	 * @throws Exception
	 */
	public void testMonitor() throws Exception
	{
		Channel channel = clientContext.createChannel("COUNTER");
		clientContext.pendIO(3.0);

	    Monitor monitor = null;
	    MonitorListenerImpl listener = new MonitorListenerImpl();

	    // 1 value difference
	    final int COUNT = 3;
	    double lastVal = Double.MIN_VALUE;
	    long lastTime = 0;
	    synchronized (listener)
	    {
	        monitor = channel.addMonitor(DBR_Double.TYPE, 1, Monitor.VALUE, listener);
	        clientContext.flushIO();
	        
	        for (int i = 0; i < COUNT; i++) {
		        listener.wait(3000);
		        
		        assertEquals(CAStatus.NORMAL, listener.status);
		        double val = ((double[])listener.response.getValue())[0];
		        if (lastVal != Double.MIN_VALUE)
		            assertEquals(1.0, val-lastVal, 0.0001);
		        lastVal = val;
		        lastTime = System.currentTimeMillis();
		        assertEquals(CAStatus.NORMAL, listener.status);
		        
		        listener.reset();
	        }
	    }

	    CATransport transport = ((CAJChannel)channel).getTransport();
	    
	    try {
	        new EventsOffRequest(transport).submit();
	    } catch (IOException ioex) {
	        fail("sending failed");
	    }

	    clientContext.pendEvent(1.0);
	    
	    listener.reset();
	    
	    clientContext.pendEvent(3.0);

	    // no events expected here
	    assertEquals(0, listener.counter);
	    
	    // do a little trick and enable events
	    try {
	        new EventsOnRequest(transport).submit();
	    } catch (IOException ioex) {
	        fail("sending failed");
	    }
	    
	    // calculate expected value (lower bound)
	    long currentTime = System.currentTimeMillis();
	    long diff = (currentTime - lastTime)/1000;
	    
	    // monitors keep coming test
	    lastVal = Double.MIN_VALUE;
	    synchronized (listener)
	    {
	        for (int i = 0; i < COUNT; i++) {
		        listener.wait(3000);
		        
		        assertEquals(CAStatus.NORMAL, listener.status);
		        double val = ((double[])listener.response.getValue())[0];
		        if (lastVal != Double.MIN_VALUE)
		            assertEquals(1.0, val-lastVal, 0.0001);
		        else
		        {
		        	// check if events only sent the latest one
		    	    assertTrue(val > (lastVal + diff - 1.0));
		        }
		        lastVal = val;
		        assertEquals(CAStatus.NORMAL, listener.status);
		        
		        listener.reset();
	        }
	    }
	    
	    monitor.clear();

	    clientContext.pendEvent(1.0);
	    
	    listener.reset();
	    
	    clientContext.pendEvent(3.0);

	    // no events expected here
	    assertEquals(0, listener.counter);
	
	}

	/**
	 * Check for value loss.
	 * @throws Exception
	 */
	public void testFastMonitor() throws Exception
	{
		Channel channel = clientContext.createChannel("FAST");
		clientContext.pendIO(3.0);

		QueuedMonitorListenerImpl listener = new QueuedMonitorListenerImpl();
        Monitor monitor = channel.addMonitor(DBR_Double.TYPE, 1, Monitor.VALUE, listener);
        clientContext.flushIO();

        // wait 2 seconds to get bunch of monitors
        clientContext.pendEvent(2.0);
        
        monitor.clear();
        
        clientContext.pendEvent(1.0);

        double lastVal = Double.MIN_VALUE;
        Iterator iter = listener.queue.iterator();
        while (iter.hasNext())
        {
        	MonitorEvent me = (MonitorEvent)iter.next();
        	double val = ((DBR_Double)me.getDBR()).getDoubleValue()[0];
        	if (lastVal != Double.MIN_VALUE)
	            assertEquals(1.0, val-lastVal, 0.0001);
        }
	}
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		
		context = new CAJServerContext();
		
		DefaultServerImpl server = new DefaultServerImpl();

		// COUNTER - 1s increment
		CounterProcessVariable cpv = new CounterProcessVariable("COUNTER", null, 0, Integer.MAX_VALUE, 1, 1000, -Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE, Integer.MAX_VALUE);
		server.registerProcessVaribale(cpv);
		
		// FAST - 10ms increment
		cpv = new CounterProcessVariable("FAST", null, 0, Integer.MAX_VALUE, 1, 10, -Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE, Integer.MAX_VALUE);
		server.registerProcessVaribale(cpv);

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
	
		clientContext = (CAJContext)JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
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
	}

	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(EventsTest.class);
	}
}
