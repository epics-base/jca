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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.event.AccessRightsEvent;
import gov.aps.jca.event.AccessRightsListener;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;

import com.cosylab.epics.caj.CAJContext;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJChannelTest extends TestCase {

    /**
     * Dummy implementation of ConnectionListener.
     */
    private class ConnectionListenerImpl implements ConnectionListener {
        
        /**
         * List of all received notifications.
         */
        private Vector notifications = new Vector();
        
        /**
         * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
         */
        public synchronized void connectionChanged(ConnectionEvent ev)
        {
            notifications.add(ev);
            this.notifyAll();
        }
        
        /**
         * Get list of all received notifications.
         * @return list of all received notifications.
         */
        public Vector getNotifications()
        {
            return notifications;
        }
    }

    /**
     * Dummy implementation of AccessRightsListener.
     */
    private class AccessRightsListenerImpl implements AccessRightsListener {
        
        /**
         * @see gov.aps.jca.event.AccessRightsListener#accessRightsChanged(gov.aps.jca.event.AccessRightsEvent)
         */
        public void accessRightsChanged(AccessRightsEvent ev)
        {
        }
    }
    
    /**
	 * Context to be tested.
	 */
	private CAJContext context;
	
    /**
	 * Channel to be tested.
	 */
    private Channel channel;
    
	/**
	 * Constructor for CAJChannelTest.
	 * @param methodName
	 */
	public CAJChannelTest(String methodName) {
		super(methodName);
	}
	
	/**
	 * Register connection listener.
	 */
	public void testConnectionListeners() throws CAException {
	    
	    try {
	        channel.addConnectionListener(null);
	        fail("null listener accepted");
	    } catch (IllegalArgumentException iae) {
	        // this is ok
		}
	    assertEquals(0, channel.getConnectionListeners().length);

	    ConnectionListener cl1 = new ConnectionListenerImpl();
	    channel.addConnectionListener(cl1);
	    assertEquals(1, channel.getConnectionListeners().length);
	    
	    ConnectionListener cl2 = new ConnectionListenerImpl();
	    channel.addConnectionListener(cl2);
	    assertEquals(2, channel.getConnectionListeners().length);
	    
	    // duplicate registration
	    channel.addConnectionListener(cl2);
	    assertEquals(2, channel.getConnectionListeners().length);

	    ConnectionListener[] listeners = channel.getConnectionListeners();
	    assertTrue(listeners[0] == cl1);
	    assertTrue(listeners[1] == cl2);
	    

	    try {
	        channel.removeConnectionListener(null);
	        fail("null listener removed");
	    } catch (IllegalArgumentException iae) {
	        // this is ok
		}
	    assertEquals(2, channel.getConnectionListeners().length);
	    
	    channel.removeConnectionListener(cl1);
	    listeners = channel.getConnectionListeners();
	    assertEquals(1, listeners.length);
	    assertTrue(listeners[0] == cl2);

	    // removing twice (non registered) does not raise any error
	    channel.removeConnectionListener(cl1);
	    listeners = channel.getConnectionListeners();
	    assertEquals(1, listeners.length);
	    assertTrue(listeners[0] == cl2);

	    channel.removeConnectionListener(cl2);
	    assertEquals(0, channel.getConnectionListeners().length);
	}

	/**
	 * Register connection listener notifications.
	 */
	public void testConnectionListenerNotifications() throws CAException, TimeoutException, InterruptedException {
	    channel.destroy();

	    ConnectionListenerImpl cl1 = new ConnectionListenerImpl();
        channel = context.createChannel("record1", cl1);

        synchronized(cl1) {
            if (cl1.getNotifications().size() == 0)
                cl1.wait(3000);
        }
        
	    assertEquals(1, cl1.getNotifications().size());
	    assertEquals(true, ((ConnectionEvent)cl1.getNotifications().firstElement()).isConnected());
	    
	    channel.destroy();

	    synchronized(cl1) {
            if (cl1.getNotifications().size() == 1)
                cl1.wait(3000);
        }

	    assertEquals(2, cl1.getNotifications().size());
	    assertEquals(false, ((ConnectionEvent)cl1.getNotifications().lastElement()).isConnected());
	}
    
	/**
	 * Test simple ordinary usage of channel.
	 */
	public void testConnectionListenerNotificationsWhenAlreadyConnected() throws CAException, TimeoutException, InterruptedException {
	    
		// classic
		Channel channel = context.createChannel("record1");
	    context.pendIO(5.0);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());

	    // duplicate create
	    ConnectionListenerImpl cl2 = new ConnectionListenerImpl();
		Channel channel2 = context.createChannel("record1", cl2);
		// since we are resuing channels it should be already connected
		assertEquals(Channel.CONNECTED, channel2.getConnectionState());
		// and connection fired
        synchronized(cl2) {
            if (cl2.getNotifications().size() == 0)
                cl2.wait(3000);
        }
	    assertEquals(1, cl2.getNotifications().size());
	    assertEquals(true, ((ConnectionEvent)cl2.getNotifications().firstElement()).isConnected());
	}


	/**
	 * Register AccessRights listener.
	 */
	public void testAccessRightsListeners() throws CAException, TimeoutException {
	    
	    try {
	        channel.addAccessRightsListener(null);
	        fail("null listener accepted");
	    } catch (IllegalArgumentException iae) {
	        // this is ok
		}
	    assertEquals(0, channel.getAccessRightsListeners().length);

	    AccessRightsListener cl1 = new AccessRightsListenerImpl();
	    channel.addAccessRightsListener(cl1);
	    assertEquals(1, channel.getAccessRightsListeners().length);
	    
	    AccessRightsListener cl2 = new AccessRightsListenerImpl();
	    channel.addAccessRightsListener(cl2);
	    assertEquals(2, channel.getAccessRightsListeners().length);
	    
	    // duplicate registration
	    channel.addAccessRightsListener(cl2);
	    assertEquals(2, channel.getAccessRightsListeners().length);

	    AccessRightsListener[] listeners = channel.getAccessRightsListeners();
	    assertTrue(listeners[0] == cl1);
	    assertTrue(listeners[1] == cl2);
	    

	    try {
	        channel.removeAccessRightsListener(null);
	        fail("null listener removed");
	    } catch (IllegalArgumentException iae) {
	        // this is ok
		}
	    assertEquals(2, channel.getAccessRightsListeners().length);
	    
	    channel.removeAccessRightsListener(cl1);
	    listeners = channel.getAccessRightsListeners();
	    assertEquals(1, listeners.length);
	    assertTrue(listeners[0] == cl2);

	    // removing twice (non registered) does not raise any error
	    channel.removeAccessRightsListener(cl1);
	    listeners = channel.getAccessRightsListeners();
	    assertEquals(1, listeners.length);
	    assertTrue(listeners[0] == cl2);

	    channel.removeAccessRightsListener(cl2);
	    assertEquals(0, channel.getAccessRightsListeners().length);
	}
	
	/**
	 * Simple properties (accessors) test.
	 */
	public void testProperties() throws CAException {
	    assertEquals("record1", channel.getName());
	    assertTrue(context == channel.getContext());
	    assertEquals(1, channel.getElementCount());
	    assertEquals(DBRType.DOUBLE, channel.getFieldType());
	    assertEquals(true, channel.getReadAccess());
	    assertEquals(true, channel.getWriteAccess());
	    try {
	        assertEquals(InetAddress.getLocalHost().getCanonicalHostName(), channel.getHostName());
	    } catch (UnknownHostException uhe) {
	        // noop (failed to determine)
	    }
	}

	/**
	 * Simple monitor test - just to check, if they are working...
	 */
	public void testMonitor() throws CAException {
	    Monitor monitor = channel.addMonitor(Monitor.VALUE);
	    monitor.clear();

	    monitor = channel.addMonitor(Monitor.VALUE);
	    // forget to clear
	}
	
	/**
	 * Multiple destroy test.
	 */
	public void testMultipleDestroy() throws CAException {
	    channel.destroy();
	    assertEquals(Channel.CLOSED, channel.getConnectionState());
	    
	    try {
	        channel.destroy();
	        fail("destroy allowed to be called on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	}

	/**
	 * Test states.
	 */
	public void testStates() throws CAException {
	    channel.destroy();
	    assertEquals(Channel.CLOSED, channel.getConnectionState());
	    
	    try {
		    channel.getFieldType();
	        fail("getFieldType() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	    
	    try {
		    channel.getElementCount();
	        fail("getElementCount() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	    
	    ConnectionListener cl = new ConnectionListenerImpl();
	    try {
		    channel.addConnectionListener(cl);
	        fail("addConnectionListener() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	    
	    try {
		    channel.removeConnectionListener(cl);
	        fail("removeConnectionListener() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }

	    AccessRightsListener al = new AccessRightsListenerImpl();
	    try {
		    channel.addAccessRightsListener(al);
	        fail("addAccessRightsListener() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }

	    try {
		    channel.removeAccessRightsListener(al);
	        fail("removeAccessRightsListener() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }

	    try {
		    channel.getHostName();
	        fail("getHostName() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }

	    try {
		    channel.getReadAccess();
	        fail("getReadAccess() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	    
	    try {
		    channel.getWriteAccess();
	        fail("getWriteAccess() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }

	    try {
		    channel.put(12.08);
	        fail("put() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	    
	    try {
		    channel.get();
	        fail("get() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }

	    try {
		    channel.addMonitor(Monitor.VALUE);
	        fail("addMonitor() on destroyed channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }

	    
	    
	    channel = context.createChannel("invalid");
	    assertEquals(Channel.NEVER_CONNECTED, channel.getConnectionState());
	    
	    try {
		    channel.getFieldType();
	        fail("getFieldType() on never connected channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	    
	    try {
		    channel.getElementCount();
	        fail("getElementCount() on never connected channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	    
	    try {
		    channel.addConnectionListener(cl);
	    } catch (IllegalStateException ise) {
	        fail("addConnectionListener() failed on never connected channel");
	    }
	    
	    try {
		    channel.removeConnectionListener(cl);
	    } catch (IllegalStateException ise) {
	        fail("removeConnectionListener() failed on never connected channel");
	    }

	    try {
		    channel.addAccessRightsListener(al);
	    } catch (IllegalStateException ise) {
	        fail("addAccessRightsListener() failed on never connected channel");
	    }

	    try {
		    channel.removeAccessRightsListener(al);
	    } catch (IllegalStateException ise) {
	        fail("removeAccessRightsListener() failed on never connected channel");
	    }

	    try {
		    channel.getHostName();
	        fail("getHostName() on never connected channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }

	    try {
		    channel.getReadAccess();
	        fail("getReadAccess() on never connected channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	    
	    try {
		    channel.getWriteAccess();
	        fail("getWriteAccess() on never connected channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }

	    try {
		    channel.put(12.08);
	        fail("put() on never connected channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	    
	    try {
		    channel.get();
	        fail("get() on never connected channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }

	    try {
		    channel.addMonitor(Monitor.VALUE);
	        fail("addMonitor() on never connected channel");
	    } catch (IllegalStateException ise) {
	        // ok
	    }

	    try {
	        channel.destroy();
	    } catch (IllegalStateException ise) {
	        fail("destroy failed on never conncted channel");
	    }
	    assertEquals(Channel.CLOSED, channel.getConnectionState());
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		context = new CAJContext();
	    channel = context.createChannel("record1");
	    context.pendIO(5.0);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());
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
		junit.textui.TestRunner.run(CAJChannelTest.class);
	}
	
}
