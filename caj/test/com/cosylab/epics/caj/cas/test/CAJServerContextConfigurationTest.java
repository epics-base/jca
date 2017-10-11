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

import java.net.InetSocketAddress;

import gov.aps.jca.configuration.ConfigurationException;
import gov.aps.jca.configuration.DefaultConfiguration;

import com.cosylab.epics.caj.cas.CAJServerContext;

import junit.framework.TestCase;

/**
 * CAJContext configuration test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJServerContextConfigurationTest extends TestCase {

	/**
	 * Context to be tested.
	 */
	private CAJServerContext context;
	
	/**
	 * Constructor for CAJContextConfigurationTest.
	 * @param methodName
	 */
	public CAJServerContextConfigurationTest(String methodName) {
		super(methodName);
	}

	/**
	 * Tests configuration via environment (JVM properties or file).
	 */
	public void testConfigViaEnvironment() {
        assertEquals("CASTestName", context.getLogger().getName());
	    assertEquals("192.168.0.112", context.getBeaconAddressList());
	    assertEquals(false, context.isAutoBeaconAddressList());
	    assertEquals(17.0, context.getBeaconPeriod(), 0.0);
	    assertEquals(7065, context.getBeaconPort());
	    assertEquals(7064, context.getServerPort());
	    assertEquals(17000, context.getMaxArrayBytes());
	    assertEquals("192.168.0.13", context.getIgnoreAddressList());
	}
	
	/**
	 * Tests configuration via CAJ context environment (JVM properties or file).
	 */
	private void configViaCAJEnvironmentTest() {
	    assertEquals("192.168.0.12", context.getBeaconAddressList());
	    assertEquals(false, context.isAutoBeaconAddressList());
	    assertEquals(16.0, context.getBeaconPeriod(), 0.0);
	    assertEquals(6065, context.getBeaconPort());
	    assertEquals(6064, context.getServerPort());
	    assertEquals(16000, context.getMaxArrayBytes());
	}

	/**
	 * Tests if configuration is reading CAJContext config as default.
	 */
	public void testConfigViaCAJEnvironment() throws ConfigurationException {
		removeProperties();
		setupCAJProperties();
        context = new CAJServerContext();
        configViaCAJEnvironmentTest();
	}

	/**
	 * Tests if configuration CAJContext config as default config is overriden by CASServerConfig.
	 */
	public void testConfigViaCASandCAJEnvironment() throws ConfigurationException {
		setupCAJProperties();
        context = new CAJServerContext();
        testConfigViaEnvironment();
	}

	/**
	 * Tests configuration via (empty) default configuration,
	 * environment configuration to be taken (JVM properties or file).
	 */
	public void testConfigViaDefaultConfiguration() throws ConfigurationException {
        context = new CAJServerContext();
        context.configure(new DefaultConfiguration("empty"));
        testConfigViaEnvironment();
	}

	/**
	 * Tests configuration via configuration, this overrides env. config.
	 */
	public void testConfigViaConfiguration() throws ConfigurationException {

		DefaultConfiguration conf = new DefaultConfiguration("CAJServerContext");

		// logger name cannot be set
		//conf.setAttribute("logger", "TestName2");
		conf.setAttribute("beacon_addr_list", "192.168.0.113");
		conf.setAttribute("auto_beacon_addr_list", "true");
		conf.setAttribute("beacon_period", "18.0");
		conf.setAttribute("beacon_port", "7066");
		conf.setAttribute("server_port", "7065");
		conf.setAttribute("max_array_bytes", "17001");
		conf.setAttribute("ignore_addr_list", "192.168.0.213");
		
        context.configure(conf);

        //assertEquals("TestName2", context.getLogger().getName());
	    assertEquals("192.168.0.113", context.getBeaconAddressList());
	    assertEquals(true, context.isAutoBeaconAddressList());
	    assertEquals(18.0, context.getBeaconPeriod(), 0.0);
	    assertEquals(7066, context.getBeaconPort());
	    assertEquals(7065, context.getServerPort());
	    assertEquals(17001, context.getMaxArrayBytes());
	    assertEquals("192.168.0.213", context.getIgnoreAddressList());
	    
	}

	/**
	 * Tests address list which is disabled.
	 */
	public void testAutoAndUserAddressList() throws Exception {
	    tearDown();

	    System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.beacon_addr_list", "192.168.0.12");
        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.auto_beacon_addr_list", "true");
        
    	context = new CAJServerContext();
        context.initialize(new DummyServerImpl());
        
    	final InetSocketAddress[] expectedAddreses =
    		new InetSocketAddress[] { 
    	        new InetSocketAddress("192.168.0.12", context.getBeaconPort()),
    	        new InetSocketAddress("255.255.255.255", context.getBeaconPort())
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

        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.auto_beacon_addr_list", "false");
        
        context = new CAJServerContext();
        context.initialize(new DummyServerImpl());
        
    	final InetSocketAddress[] expectedAddreses =
    		new InetSocketAddress[] { 
    	        new InetSocketAddress("255.255.255.255", context.getBeaconPort()),
    	        };

        assertEquals(1, context.getBroadcastTransport().getBroadcastAddresses().length);
        assertEquals(expectedAddreses[0], context.getBroadcastTransport().getBroadcastAddresses()[0]);
	}

	/**
	 * Tests disabled auto address list with changed server port, but no list specified .
	 */
	public void testNoAddressListGivenDifferentServerPort() throws Exception {
	    tearDown();

        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.auto_beacon_addr_list", "false");
        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.beacon_port", "7064");
        
        context = new CAJServerContext();
        context.initialize(new DummyServerImpl());
                
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

	    System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.beacon_addr_list", "192.168.0.12 192.168.0.13:8064");
        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.auto_beacon_addr_list", "false");
        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.beacon_port", "7064");
    	 
        context = new CAJServerContext();
        context.initialize(new DummyServerImpl());
        
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
	 * Tests address list.
	 */
	public void testIgnoreAddressList() throws Exception {
	    tearDown();

	    System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.ignore_addr_list", "192.168.0.12 192.168.0.13");
    	 
        context = new CAJServerContext();
        context.initialize(new DummyServerImpl());
        
        final InetSocketAddress[] expectedAddreses =
    		new InetSocketAddress[] { 
    	        new InetSocketAddress("192.168.0.12", 0),
    	        new InetSocketAddress("192.168.0.13", 0),
    	        };

        assertEquals(2, context.getBroadcastTransport().getIgnoredAddresses().length);
        assertEquals(expectedAddreses[0], context.getBroadcastTransport().getIgnoredAddresses()[0]);
        assertEquals(expectedAddreses[1], context.getBroadcastTransport().getIgnoredAddresses()[1]);
	}

	/**
	 * Tests invalid address list.
	 */
	public void testInvalidAddressList() throws Exception {
	    tearDown();

	    System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.beacon_addr_list", " ");
        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.auto_beacon_addr_list", "false");
    	 
        context = new CAJServerContext();
        context.initialize(new DummyServerImpl());
        
    	final InetSocketAddress[] expectedAddreses =
    		new InetSocketAddress[] { 
    	        new InetSocketAddress("255.255.255.255", context.getBeaconPort()),
    	        };

        assertEquals(1, context.getBroadcastTransport().getBroadcastAddresses().length);
        assertEquals(expectedAddreses[0], context.getBroadcastTransport().getBroadcastAddresses()[0]);
	}

	/**
	 * Setup JVM properties.
	 */
	private void setupProperties() {
	    // non-defaults are used
		System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.logger", "CASTestName");
		System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.beacon_addr_list", "192.168.0.112");
        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.auto_beacon_addr_list", "false");
        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.beacon_period", "17.0");
        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.beacon_port", "7065");
        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.server_port", "7064");
        System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.max_array_bytes", "17000");
		System.setProperty("com.cosylab.epics.caj.cas.CAJServerContext.ignore_addr_list", "192.168.0.13");
	}
	
	/**
	 * Remove JVM properties.
	 */
	private void removeProperties() {
		System.getProperties().remove("com.cosylab.epics.caj.cas.CAJServerContext.logger");
		System.getProperties().remove("com.cosylab.epics.caj.cas.CAJServerContext.beacon_addr_list");
		System.getProperties().remove("com.cosylab.epics.caj.cas.CAJServerContext.auto_beacon_addr_list");
		System.getProperties().remove("com.cosylab.epics.caj.cas.CAJServerContext.beacon_period");
		System.getProperties().remove("com.cosylab.epics.caj.cas.CAJServerContext.beacon_port");
		System.getProperties().remove("com.cosylab.epics.caj.cas.CAJServerContext.server_port");
		System.getProperties().remove("com.cosylab.epics.caj.cas.CAJServerContext.max_array_bytes");
		System.getProperties().remove("com.cosylab.epics.caj.cas.CAJServerContext.ignore_addr_list");
	}
	
	/**
	 * Setup JVM CAJ properties.
	 */
	private void setupCAJProperties() {
	    // non-defaults are used
		System.setProperty("com.cosylab.epics.caj.CAJContext.logger", "TestName");
		System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", "192.168.0.12");
        System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");
        System.setProperty("com.cosylab.epics.caj.CAJContext.connection_timeout", "31.0");
        System.setProperty("com.cosylab.epics.caj.CAJContext.beacon_period", "16.0");
        System.setProperty("com.cosylab.epics.caj.CAJContext.repeater_port", "6065");
        System.setProperty("com.cosylab.epics.caj.CAJContext.server_port", "6064");
        System.setProperty("com.cosylab.epics.caj.CAJContext.max_array_bytes", "16000");
	}
	
	/**
	 * Remove JVM CAJ properties.
	 */
	private void removeCAJProperties() {
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.logger");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.addr_list");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.auto_addr_list");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.connection_timeout");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.beacon_period");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.repeater_port");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.server_port");
		System.getProperties().remove("com.cosylab.epics.caj.CAJContext.max_array_bytes");
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
	    setupProperties();
        context = new CAJServerContext();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		if (!context.isDestroyed())
			context.destroy();
		context = null;
		removeProperties();
		removeCAJProperties();
	}

	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(CAJServerContextConfigurationTest.class);
	}
}
