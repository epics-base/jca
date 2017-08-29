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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.CAConstants;

import junit.framework.TestCase;

/**
 * CAJContext channel (creation and deswctruction) test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJContextChannelTest extends TestCase {

	/**
	 * Context to be tested.
	 */
	private CAJContext context;
	
	/**
	 * Constructor for ChannelTest.
	 * @param methodName
	 */
	public CAJContextChannelTest(String methodName) {
		super(methodName);
	}

	/**
	 * Invalid name.
	 */
	public void testInvalidName() throws CAException {

	    try {
	        context.createChannel(null);
	        fail("null name accepted");
	    } catch (IllegalArgumentException iae) {
	        // ok
	    }

	    try {
	        context.createChannel("");
	        fail("empty name accepted");
	    } catch (IllegalArgumentException iae) {
	        // ok
	    }

	    try {
	        StringBuffer longName = new StringBuffer(CAConstants.MAX_UDP_SEND);
	        for (int i = 0; i < CAConstants.MAX_UDP_SEND; i++)
	            longName.append('a');
	        context.createChannel(longName.toString());
	        fail("too long name accepted");
	    } catch (CAException cae) {
	        // ok
	    }

	}
	
	/**
	 * Test on the same instance - concurrent creation/destruction test.
	 * @throws IllegalStateException
	 */
	private void concurrentOnTheSameInstance(final String channelName, boolean invalid, final boolean async)
		throws CAException, TimeoutException {
	    
	    final int THREADS = 3;
	    final ArrayList list = new ArrayList(THREADS);
	    final AtomicInteger connected = new AtomicInteger(0);
	    final AtomicInteger disconnected = new AtomicInteger(0);
	    
	    class ConnectionListenerImpl implements ConnectionListener {
	        public void connectionChanged(ConnectionEvent ce)
	        {
	            if (ce.isConnected())
	                connected.incrementAndGet();
	            else
	                disconnected.incrementAndGet();
	            
	            synchronized (this) {
	                this.notify();
	            }
	        }
	    }
	    
	    Runnable createTask = new Runnable() {
	        public void run() {
	            try {
	                ConnectionListener listener = null;
	                if (async)
	                    listener = new ConnectionListenerImpl();
	                synchronized (this) {
	                    // noop - this is only barrier
	                }
                    Channel channel = context.createChannel(channelName, listener);
                    synchronized (list) {
                        list.add(channel);
                    }
                } catch (Throwable th) {
                   th.printStackTrace();
                   fail("unexpected exception: " + th);
                }
	        }
	    };
	    
	    Thread[] threads = new Thread[THREADS];
	    synchronized(createTask) {
		    for (int i = 0; i < THREADS; i++) {
		        threads[i] = new Thread(createTask);
		        threads[i].start();
		    }
	    }
	    // barrier released - go go go...
	    
	    for (int i = 0; i < THREADS; i++) {
	        try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
	    }

	    // check all done
	    assertEquals(THREADS, list.size());
	    
	    // check if all the same
	    int pos = list.size()-1 ;
	    final Channel channel = (Channel)list.remove(pos--);
	    while (pos >= 0)
	        assertTrue(channel == list.remove(pos--));

	    if (!invalid)
	    {
	        if (async) {
	            try {
	                while (connected.get() < THREADS)
	                {
			            synchronized (this) {
			                int lastVal = connected.get(); 
			                this.wait(3000);
			                boolean timeout = (connected.get() == lastVal);
			                if (timeout)
			                    break;
			            }
	                }
	            } catch (InterruptedException ie) {}
                assertEquals(THREADS, connected.get());
	        }
	        else
	            context.pendIO(5.0);
	    }
	    assertEquals(invalid ? 1 : 0, context.getChannelSearchManager().registeredChannelCount());

	    
	    
	    
	    Runnable destroyTask = new Runnable() {
	        public void run() {
	            try {
	                synchronized (this) {
	                    // noop - this is only barrier
	                }
                    channel.destroy();
                } catch (IllegalStateException ise) {
                    // ok
                } catch (Throwable th) {
                   th.printStackTrace();
                   fail("unexpected exception: " + th);
                }
	        }
	    };
	    
	    synchronized(destroyTask) {
		    for (int i = 0; i < THREADS; i++) {
		        threads[i] = new Thread(destroyTask);
		        threads[i].start();
		    }
	    }
	    // barrier released - go go go...
	    
	    for (int i = 0; i < THREADS; i++) {
	        try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
	    }
	    
	    assertEquals(Channel.CLOSED, channel.getConnectionState());
	    
	    if (!invalid)
	    {
	        if (async) {
                assertEquals(THREADS, disconnected.get());
	        }
	    }

	    assertEquals(0, context.getChannelSearchManager().registeredChannelCount());
	}

		
	/**
	 * Test on the same instance on non-existant channel - sync.
	 */
	public void testConcurrentNonExistantSync() throws CAException, TimeoutException {
	    concurrentOnTheSameInstance("invalid", true, false);
	}

	/**
	 * Test on the same instance on non-existant channel - async.
	 */
	public void testConcurrentNonExistantAsync() throws CAException, TimeoutException {
	    concurrentOnTheSameInstance("invalid", true, true);
	}

	/**
	 * Test on the same instance - sync.
	 */
	public void testConcurrentSync() throws CAException, TimeoutException {
	    concurrentOnTheSameInstance("record1", false, false);
	}

	/**
	 * Test on the same instance - async.
	 */
	public void testConcurrentAsync() throws CAException, TimeoutException {
	    concurrentOnTheSameInstance("record1", false, true);
	}

	/**
	 * Test non-existant channel.
	 */
	public void testNonExistant() throws CAException, TimeoutException {
	    Channel channel = context.createChannel("invalid");
	    channel.printInfo();
	    assertEquals(1, context.getChannelSearchManager().registeredChannelCount());
	    try {
	    	context.pendIO(3.0);
	    	fail();
	    } catch (TimeoutException te) {
	    	// noop this is OK
	    }
	    assertEquals(1, context.getChannelSearchManager().registeredChannelCount());
	    channel.destroy();
	    assertEquals(Channel.CLOSED, channel.getConnectionState());
	    assertEquals(0, context.getChannelSearchManager().registeredChannelCount());
	}

	/**
	 * Test simple ordinary usage of channel.
	 */
	public void testSimple() throws CAException, TimeoutException {
	    Channel channel = context.createChannel("record1");
	    assertEquals(1, context.getChannelSearchManager().registeredChannelCount());
	    context.pendIO(5.0);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());
	    assertEquals(0, context.getChannelSearchManager().registeredChannelCount());
	    assertEquals(1, context.getTransportRegistry().numberOfActiveTransports());
	    channel.printInfo();
	    channel.destroy();
	    channel.printInfo();
	    assertEquals(0, context.getChannelSearchManager().registeredChannelCount());
	    assertEquals(0, context.getTransportRegistry().numberOfActiveTransports());
	}

	/**
	 * Test simple ordinary usage of channel.
	 */
	public void testReferenceCouting() throws CAException, TimeoutException {
	    
		// classic
		Channel channel = context.createChannel("record1");
	    context.pendIO(5.0);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());
	    channel.destroy();
	    assertEquals(Channel.CLOSED, channel.getConnectionState());
	    
		// duplicate create
		channel = context.createChannel("record1");
		Channel channel2 = context.createChannel("record1");
	    context.pendIO(5.0);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());
	    assertEquals(Channel.CONNECTED, channel2.getConnectionState());
	    channel.destroy();
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());
	    assertEquals(Channel.CONNECTED, channel2.getConnectionState());
	    channel2.destroy();
	    assertEquals(Channel.CLOSED, channel.getConnectionState());
	    assertEquals(Channel.CLOSED, channel2.getConnectionState());
	}

	/**
	 * Test simple multiple connect.
	 */
	public void testSimpleMultipleConnect() throws CAException, TimeoutException {
	    Channel channel1 = context.createChannel("record1");
	    Channel channel2 = context.createChannel("record2");
	    Channel channel3 = context.createChannel("enum");
	    context.pendIO(5.0);
	    assertEquals(Channel.CONNECTED, channel1.getConnectionState());
	    assertEquals(Channel.CONNECTED, channel2.getConnectionState());
	    assertEquals(Channel.CONNECTED, channel3.getConnectionState());
	    assertEquals(0, context.getChannelSearchManager().registeredChannelCount());
	    assertEquals(1, context.getTransportRegistry().numberOfActiveTransports());
	    channel2.destroy();
	    channel3.destroy();
	    channel1.destroy();
	    assertEquals(0, context.getChannelSearchManager().registeredChannelCount());
	    assertEquals(0, context.getTransportRegistry().numberOfActiveTransports());
	}

	/**
	 * Test "forget to destroy" channel case.
	 */
	public void testForgetToDestroy() throws CAException, TimeoutException {
	    Channel channel = context.createChannel("record1");
	    context.pendIO(5.0);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());
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
		junit.textui.TestRunner.run(CAJContextChannelTest.class);
	}
}
