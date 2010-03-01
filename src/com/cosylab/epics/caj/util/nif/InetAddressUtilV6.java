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

package com.cosylab.epics.caj.util.nif;

import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * <code>InetAddress</code> utility methods.
 * This class needs Java 6 or never. To compile on older version, simply remove this file.
 * CAJ will detect this and use defaults.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id: InetAddressUtil.java,v 1.4 2009/12/22 21:50:26 msekoranja Exp $
 */
public class InetAddressUtilV6 {

	/**
	 * Get broadcast addresses.
	 * @param port port to be added to get socket address.
	 * @return array of broadcast addresses with given port.
	 */
	public static InetSocketAddress[] getBroadcastAddresses(int port) {
		Enumeration nets;
		try {
			nets = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException se) {
			// fallback
			return new InetSocketAddress[] { new InetSocketAddress("255.255.255.255", port) };
		}

		ArrayList list = new ArrayList(10);

			while (nets.hasMoreElements())
			{
				NetworkInterface net = (NetworkInterface)nets.nextElement();
				//if (net.isUp())
				{
					List interfaceAddresses = net.getInterfaceAddresses();
					Iterator iter = interfaceAddresses.iterator();
					while (iter.hasNext())
					{
						InterfaceAddress addr = (InterfaceAddress)iter.next();
						if (addr.getBroadcast() != null)
						{
							InetSocketAddress isa = new InetSocketAddress(addr.getBroadcast(), port);
							if (!list.contains(isa))
								list.add(isa);
						}
					}
				}
			}
		
		InetSocketAddress[] retVal = new InetSocketAddress[list.size()];
		list.toArray(retVal);
		return retVal;
	}

}
