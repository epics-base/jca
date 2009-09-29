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

import java.net.InetSocketAddress;

import gov.aps.jca.CAException;
import gov.aps.jca.configuration.ConfigurationException;
import gov.aps.jca.configuration.DefaultConfiguration;
import gov.aps.jca.event.DirectEventDispatcher;
import gov.aps.jca.event.QueuedEventDispatcher;

import com.cosylab.epics.caj.CAJContext;

import junit.framework.TestCase;

/**
 * CAJContext configuration test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJContextConfigurationTest extends TestCase {

    /**
	 * Context to be tested.
	 */
	private CAJContext context;
	
	/**
	 * Constructor for CAJContextConfigurationTest.
	 * @param methodName
	 */
	public CAJContextConfigurationTest(String methodName) {
		super(methodName);
	}

	/**
	 * Tests configuration via environment (JVM properties or file).
	 */
	public void testConfigViaEnvironment() {
        assertEquals("TestName", context.getLogger().getName());
	    assertEquals("192.168.0.12", context.getAddressList());
	    assertEquals(false, context.isAutoAddressList());
	    assertEquals(31.0, context.getConnectionTimeout(), 0.0);
	    assertEquals(16.0, context.getBeaconPeriod(), 0.0);
	    assertEquals(6065, context.getRepeaterPort());
	    assertEquals(6064, context.getServerPort());
	    assertEquals(16000, context.getMaxArrayBytes());
	    assertTrue(context.getEventDispatcher() instanceof TestEventDispatcher);
	}
	
	/**
	 * Tests configuration via environment (JVM properties or file).
	 */
	public void testConfigViaEnvironmentInvalidEventDispatcher() throws CAException {
	    context.destroy();
	    
        System.setProperty("com.cosylab.epics.caj.CAJContext.event_dispatcher", "invalid class name - expecting exception");

        context = new CAJContext();
        
	    // default
	    assertTrue(context.getEventDispatcher() instanceof DirectEventDispatcher);
	}

	/**
	 * Tests configuration via (empty) default configuration,
	 * environment configuration to be taken (JVM properties or file).
	 */
	public void testConfigViaDefaultConfiguration() throws ConfigurationException {
        context = new CAJContext();
        context.configure(new DefaultConfiguration("empty"));
        testConfigViaEnvironment();
	}

	/**
	 * Tests configuration via configuration, this overrides env. config.
	 */
	public void testConfigViaConfiguration() throws ConfigurationException {

		DefaultConfiguration conf = new DefaultConfiguration("CAJContext");

		// logger name cannot be set
		//conf.setAttribute("logger", "TestName2");
		conf.setAttribute("addr_list", "192.168.0.13");
		conf.setAttribute("auto_addr_list", "true");
		conf.setAttribute("connection_timeout", "32.0");
		conf.setAttribute("beacon_period", "17.0");
		conf.setAttribute("repeater_port", "6066");
		conf.setAttribute("server_port", "6065");
		conf.setAttribute("max_array_bytes", "16001");
		
		DefaultConfiguration edconf = new DefaultConfiguration("event_dispatcher");
		edconf.setAttribute("class", QueuedEventDispatcher.class.getName());
		conf.addChild(edconf);
        
        context.configure(conf);

        //assertEquals("TestName2", context.getLogger().getName());
	    assertEquals("192.168.0.13", context.getAddressList());
	    assertEquals(true, context.isAutoAddressList());
	    assertEquals(32.0, context.getConnectionTimeout(), 0.0);
	    assertEquals(17.0, context.getBeaconPeriod(), 0.0);
	    assertEquals(6066, context.getRepeaterPort());
	    assertEquals(6065, context.getServerPort());
	    assertEquals(16001, context.getMaxArrayBytes());
	    assertTrue(context.getEventDispatcher() instanceof QueuedEventDispatcher);
	    
	}

	/**
	 * Tests configuration via configuration, this overrides env. config.
	 */
	public void testConfigViaConfigurationInvalidEventDispatcher() throws ConfigurationException {

		DefaultConfiguration conf = new DefaultConfiguration("CAJContext");

		// no child
        context.configure(conf);
        // default
	    assertTrue(context.getEventDispatcher() instanceof DirectEventDispatcher);


	    DefaultConfiguration edconf = new DefaultConfiguration("event_dispatcher");
		conf.addChild(edconf);
	    // no class
        context.configure(conf);
        // default
	    assertTrue(context.getEventDispatcher() instanceof DirectEventDispatcher);
	    
	    // adding class attribute
		edconf.setAttribute("class", "invalid class name - expecting exception");
        context.configure(conf);
        // default
	    assertTrue(context.getEventDispatcher() instanceof DirectEventDispatcher);
	}

	/**
	 * Tests auto and user address list.
	 */
	public void testAutoAndUserAddressList() throws Exception {
	    tearDown();

	    System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", "192.168.0.12");
        System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "true");
        
    	context = new CAJContext();
        context.initialize();

    	final InetSocketAddress[] expectedAddreses =
    		new InetSocketAddress[] { 
    	        new InetSocketAddress("192.168.0.12", context.getServerPort()),
    	        new InetSocketAddress("255.255.255.255", context.getServerPort())
    	        };
    	
        assertEquals(2, context.getBroadcastTransport().getBroadcastAddresses().length);
        assertEquals(expectedAddreses[0], context.getBroadcastTransport().getBroadcastAddresses()[0]);
        assertEquals(expectedAddreses[1], context.getBroadcastTransport().getBroadcastAddresses()[1]);
	}

	/**
	 * Tests disabled auto address list, but no list specified.
	 */
	public void testNoAddressListGiven() throws Exception {
	    tearDown();

        System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");
        
        context = new CAJContext();
        context.initialize();
        
    	final InetSocketAddress[] expectedAddreses =
    		new InetSocketAddress[] { 
    	        new InetSocketAddress("255.255.255.255", context.getServerPort()),
    	        };

        assertEquals(1, context.getBroadcastTransport().getBroadcastAddresses().length);
        assertEquals(expectedAddreses[0], context.getBroadcastTransport().getBroadcastAddresses()[0]);
	}

	/**
	 * Tests disabled auto address list with changed server port, but no list specified .
	 */
	public void testNoAddressListGivenDifferentServerPort() throws Exception {
	    tearDown();

        System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");
        System.setProperty("com.cosylab.epics.caj.CAJContext.server_port", "7064");
        
        context = new CAJContext();
        context.initialize();
        
    	final InetSocketAddress[] expectedAddreses =
    		new InetSocketAddress[] { 
    	        new InetSocketAddress("255.255.255.255", 7064),
    	        };

        assertEquals(1, context.getBroadcastTransport().getBroadcastAddresses().length);
        assertEquals(expectedAddreses[0], context.getBroadcastTransport().getBroadcastAddresses()[0]);
	}

	/**
	 * Tests address list.
	 */
	public void testAddressList() throws Exception {
	    tearDown();

	    System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", "192.168.0.12 192.168.0.13:8064");
        System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");
        System.setProperty("com.cosylab.epics.caj.CAJContext.server_port", "7064");
    	 
        context = new CAJContext();
        context.initialize();

        final InetSocketAddress[] expectedAddreses =
    		new InetSocketAddress[] { 
    	        new InetSocketAddress("192.168.0.12", 7064),
    	        new InetSocketAddress("192.168.0.13", 8064),
    	        };

        assertEquals(2, context.getBroadcastTransport().getBroadcastAddresses().length);
        assertEquals(expectedAddreses[0], context.getBroadcastTransport().getBroadcastAddresses()[0]);
        assertEquals(expectedAddreses[1], context.getBroadcastTransport().getBroadcastAddresses()[1]);
	}

	/**
	 * Tests invalid address list.
	 */
	public void testInvalidAddressList() throws Exception {
	    tearDown();

	    System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", " ");
        System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");
    	 
        context = new CAJContext();
        context.initialize();

    	final InetSocketAddress[] expectedAddreses =
    		new InetSocketAddress[] { 
    	        new InetSocketAddress("255.255.255.255", context.getServerPort()),
    	        };

        assertEquals(1, context.getBroadcastTransport().getBroadcastAddresses().length);
        assertEquals(expectedAddreses[0], context.getBroadcastTransport().getBroadcastAddresses()[0]);
	}

	/**
	 * Setup JVM properties.
	 */
	private void setupProperties() {
	    // non-defaults are used
		System.setProperty("com.cosylab.epics.caj.CAJContext.logger", "TestName");
		System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", "192.168.0.12");
        System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");
        System.setProperty("com.cosylab.epics.caj.CAJContext.connection_timeout", "31.0");
        System.setProperty("com.cosylab.epics.caj.CAJContext.beacon_period", "16.0");
        System.setProperty("com.cosylab.epics.caj.CAJContext.repeater_port", "6065");
        System.setProperty("com.cosylab.epics.caj.CAJContext.server_port", "6064");
        System.setProperty("com.cosylab.epics.caj.CAJContext.max_array_bytes", "16000");
        System.setProperty("com.cosylab.epics.caj.CAJContext.event_dispatcher", TestEventDispatcher.class.getName());
	}
	
	/**
	 * Remove JVM properties.
	 */
	private void removeProperties() {
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.logger");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.addr_list");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.auto_addr_list");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.connection_timeout");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.beacon_period");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.repeater_port");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.server_port");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.max_array_bytes");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.event_dispatcher");
	}
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
	    setupProperties();
        context = new CAJContext();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		if (!context.isDestroyed())
			context.destroy();
		context = null;
		removeProperties();
	}

	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(CAJContextConfigurationTest.class);
	}
}
