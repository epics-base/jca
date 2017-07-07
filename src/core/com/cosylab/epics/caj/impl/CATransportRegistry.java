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

package com.cosylab.epics.caj.impl;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.cosylab.epics.caj.util.IntHashMap;

/**
 * Class to cache CA transports (connections to other hosts).
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CATransportRegistry {

	/**
	 * Map caching transports.
	 */
	private Map transports;
	
	/**
	 * Array of all transports.
	 */
	private ArrayList allTransports;

	/**
	 * Constructor.
	 */
	public CATransportRegistry() {
		transports = new HashMap();
		allTransports = new ArrayList();
	}

	/**
	 * Cache new (address, transport) pair.
	 * @param address	address of the host computer.
	 * @param transport	tranport to the host computer.
	 */
	public void put(InetSocketAddress address, Transport transport)
	{
		synchronized (transports) {
			IntHashMap priorities = (IntHashMap)transports.get(address);
			if (priorities == null) {
				priorities = new IntHashMap();
				transports.put(address, priorities);
			}
			priorities.put(transport.getPriority(), transport);
			allTransports.add(transport);
		}
	}

	/**
	 * Lookup for a transport for given address.
	 * @param address	address of the host computer.
	 * @param priority  priority of the transport.
	 * @return curressponing transport, <code>null</code> if none found.
	 */
	public Transport get(InetSocketAddress address, short priority)
	{
		synchronized (transports) {
			IntHashMap priorities = (IntHashMap)transports.get(address);
			if (priorities != null)
				return (Transport)priorities.get(priority);
			else
				return null;
		}
	}

	/**
	 * Lookup for a transport for given address (all priorities).
	 * @param address	address of the host computer.
	 * @return array of curressponing transports, <code>null</code> if none found.
	 */
	public Transport[] get(InetSocketAddress address)
	{
		synchronized (transports) {
			IntHashMap priorities = (IntHashMap)transports.get(address);
			if (priorities != null)
			{
				// TODO optimize
				Transport[] ts = new Transport[priorities.size()];
				priorities.toArray(ts);
				return ts;
			}
			else
				return null;
		}
	}

	/**
	 * Remove (address, transport) pair from cache.
	 * @param address	address of the host computer.
	 * @param priority  priority of the transport to be removed.
	 * @return removed transport, <code>null</code> if none found.
	 */
	public Transport remove(InetSocketAddress address, short priority)
	{
		synchronized (transports) {
			IntHashMap priorities = (IntHashMap)transports.get(address);
			if (priorities != null) {
				Transport transport = (Transport)priorities.remove(priority);
				if (priorities.size() == 0)
					transports.remove(address);
				if (transport != null)
					allTransports.remove(transport);
				return transport;
			}
			else
				return null;
		}
	}

	/**
	 * Clear cache.
	 */
	public void clear()
	{
		synchronized (transports) {
			transports.clear();
			allTransports.clear();
		}
	}
	
	/**
	 * Get number of active (cached) transports.
	 * @return number of active (cached) transports.
	 */
	public int numberOfActiveTransports()
	{
		synchronized (transports) {
			return allTransports.size();
		}
	}

	/**
	 * Get array of all active (cached) transports.
	 * @return array of all active (cached) transports.
	 */
	public Transport[] toArray()
	{
		synchronized (transports) {
			return (Transport[]) allTransports.toArray(new Transport[transports.size()]);
		}
	}
}
