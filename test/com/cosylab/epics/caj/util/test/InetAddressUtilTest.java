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

package com.cosylab.epics.caj.util.test;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import com.cosylab.epics.caj.util.InetAddressUtil;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class InetAddressUtilTest extends TestCase {

   	/**
	 * Constructor for InetAddressUtilTest.
	 * @param methodName
	 */
	public InetAddressUtilTest(String methodName) {
		super(methodName);
	}
	
	/**
	 * Test integer into an IPv4 INET address conversion.
	 */
	public void testIntToIPv4Address() throws UnknownHostException {
	    assertEquals(InetAddressUtil.intToIPv4Address(127*256*256*256+1), InetAddress.getByName("127.0.0.1"));
	    assertEquals(InetAddressUtil.intToIPv4Address(((192*256+168)*256+1)*256+254), InetAddress.getByName("192.168.1.254"));
	    assertEquals(InetAddressUtil.intToIPv4Address(0), InetAddress.getByName("0.0.0.0"));
	}

	/**
	 * Test IPv4 INET address to an integer address conversion.
	 */
	public void testIPv4AddressToInt() throws UnknownHostException {
	    assertEquals(InetAddressUtil.ipv4AddressToInt(InetAddress.getByName("127.0.0.1")), 127*256*256*256+1);
	    assertEquals(InetAddressUtil.ipv4AddressToInt(InetAddress.getByName("192.168.1.254")), ((192*256+168)*256+1)*256+254);
	    assertEquals(InetAddressUtil.ipv4AddressToInt(InetAddress.getByName("0.0.0.0")), 0);
	    
	    try {
	        InetAddressUtil.ipv4AddressToInt(Inet6Address.getByAddress(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}));
	        fail("IPv6 address accepted");
	    } catch (IllegalArgumentException iae) {
	        // ok
	    }
	}

	/**
	 * Test getSocketAddressList method.
	 */
	public void testGetSocketAddressList() {
	    assertEquals(0, InetAddressUtil.getSocketAddressList("", 0).length);

        final InetSocketAddress[] expectedAddreses =
    		new InetSocketAddress[] { 
    	        new InetSocketAddress("192.168.0.12", 7064),
    	        new InetSocketAddress("192.168.0.13", 8064),
    	        };

        InetSocketAddress[] list = InetAddressUtil.getSocketAddressList("192.168.0.12", 7064);
        assertEquals(1, list.length);
        assertEquals(expectedAddreses[0], list[0]);

        
        list = InetAddressUtil.getSocketAddressList("192.168.0.12 192.168.0.13:8064", 7064);
        assertEquals(2, list.length);
        assertEquals(expectedAddreses[0], list[0]);
        assertEquals(expectedAddreses[1], list[1]);

        final InetSocketAddress[] appendList  =
    		new InetSocketAddress[] { 
    	        new InetSocketAddress("255.255.255.255", 1000),
    	        new InetSocketAddress("10.0.0.255", 2000)
    	        };

        list = InetAddressUtil.getSocketAddressList("192.168.0.12 192.168.0.13:8064", 7064, appendList);
        assertEquals(3, list.length);
        assertEquals(expectedAddreses[0], list[0]);
        assertEquals(expectedAddreses[1], list[1]);
        assertEquals(appendList[0], list[2]);
        assertEquals(appendList[1], list[3]);
	}

}
