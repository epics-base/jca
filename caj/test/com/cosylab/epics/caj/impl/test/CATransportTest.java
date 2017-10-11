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

package com.cosylab.epics.caj.impl.test;

import java.io.IOException;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;
import gov.aps.jca.TimeoutException;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.CAConnector;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.CATransport;
import com.cosylab.epics.caj.impl.ConnectionException;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.requests.AbstractCARequest;
import com.cosylab.epics.caj.impl.requests.EchoRequest;
import com.cosylab.epics.caj.impl.requests.EventsOffRequest;
import com.cosylab.epics.caj.impl.requests.EventsOnRequest;

import junit.framework.TestCase;

/**
 * Exception response test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CATransportTest extends TestCase {

	/**
	 * Context to be tested.
	 */
	private CAJContext context;
	
    /**
	 * Channel to be tested.
	 */
    private CAJChannel channel;
    
	/**
	 * Constructor for ExceptionResponseTest.
	 * @param methodName
	 */
	public CATransportTest(String methodName) {
		super(methodName);
	}

	/**
	 * CA transport test.
	 */
	public void testCATransport() throws CAException, TimeoutException, InterruptedException {
	    CATransport transport = (CATransport)channel.getTransport(); 
	    assertTrue(transport != null);
	    
	    // TODO test if still alive
	    channel.addMonitor(Monitor.VALUE);
	    
	    // ... will just echo
	    transport.timeout(0);
	    Thread.sleep(1500);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());

	    // make transport unresponsive, hopefully this will be fast enough
	    // echo response will make it responsive again
	    transport.timeout(0);
	    transport.timeout(0);
	    // TODO fix this - linux is too fast
	    //assertEquals(Channel.DISCONNECTED, channel.getConnectionState());
	    Thread.sleep(1500);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());
	    
	    // forcefully close transport
	    // reconnect (search) will be initiated
	    transport.close(true);
	    assertEquals(Channel.DISCONNECTED, channel.getConnectionState());
	    Thread.sleep(3000);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());

	    // transport instance was changed
	    CATransport newTransport = (CATransport)channel.getTransport();
	    assertTrue(transport != newTransport);
	    transport = newTransport;
	}

	/**
	 * CA transport test.
	 */
	public void testCAConnectorFailedToConnect() {
	    try
        {
            new CAConnector(context).connect(channel, null, null,
            		CAConstants.CA_MINOR_PROTOCOL_REVISION, CAConstants.CA_DEFAULT_PRIORITY);
            fail("impossible connection accepted");
        } catch (ConnectionException e) {
            // ok
            assertEquals(null, e.getAddress());
        }
	}
	
	/**
	 * CA transport test.
	 */
	public void testTransportClosed() throws IOException, InterruptedException {
	    CATransport transport = (CATransport)channel.getTransport(); 
	    assertTrue(transport != null);
	    
	    transport.close(true);
	    Thread.sleep(1000);
	    
	    /*
	    try {
	        transport.getRemoteAddress();
	        fail("returning remote address when closed");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	    */

	    try {
	        new EventsOnRequest(transport).submit();
	        fail("sending when closed");
	    } catch (IOException ioex) {
	        // ok
	    }

	    // try again...
	    try {
	        new EventsOffRequest(transport).submit();
	        fail("sending when closed");
	    } catch (IOException ioex) {
	        // ok
	    }
	    
	    // put sth in buffer
	    Request r = new EchoRequest(transport) {
			/**
			 * @see com.cosylab.epics.caj.impl.Request#getPriority()
			 */
			public byte getPriority() {
				return Request.DEFAULT_USER_PRIORITY;
			}
	    };
	    try {
	    	r.submit();
	        fail("sending when closed");
	    } catch (IllegalStateException ise) {
	        // ok
	    }
	    transport.flush();
	}

	/**
	 * Some write methods test.
	 */
	public void testWriteMethods() throws IOException {
	    CATransport transport = (CATransport)channel.getTransport(); 
	    assertTrue(transport != null);
	    
	    Request r = new AbstractCARequest(transport) { };
	    // do not complain test
	    transport.submit(r);
	    
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		context = new CAJContext();
	    channel = (CAJChannel)context.createChannel("record1");
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
		junit.textui.TestRunner.run(CATransportTest.class);
	}
	
}
