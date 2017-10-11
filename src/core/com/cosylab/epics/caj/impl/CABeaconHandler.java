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

import com.cosylab.epics.caj.CAJContext;

/**
 * CA beacon handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CABeaconHandler  {
	
	/**
	 * Context instance.
	 */
	private CAJContext context;

	/**
	 * Remote address.
	 */
	private InetSocketAddress responseFrom;

	/**
	 * Average period.
	 */
	private long averagePeriod = Long.MIN_VALUE;
	
	/**
	 * Period stabilization flag.
	 * If beacon monitoring began when server is being (re)started,
	 * beacon period increases by factor 2. This case is handled by this flag.
	 */
	private boolean periodStabilized = false;

	/**
	 * Last beacon sequence ID.
	 */
	private long lastBeaconSequenceID;
	
	/**
	 * Last beacon timestamp.
	 */
	private long lastBeaconTimeStamp = Long.MIN_VALUE;

	/**
	 * Transport initialization timestamp.
	 */
	private final long initializationTimeStamp = System.currentTimeMillis();

	/**
	 * Constructor.
	 * @param context context ot handle.
	 * @param responseFrom server to handle.
	 */
	public CABeaconHandler(CAJContext context, InetSocketAddress responseFrom)
	{
		this.context = context;
		this.responseFrom = responseFrom;
	}
	
	/**
	 * Update beacon period and do analitical checks (server restared, routing problems, etc.)
	 * @param remoteTransportRevision
	 * @param timestamp
	 * @param sequentalID
	 */
	public void beaconNotify(short remoteTransportRevision, long timestamp, long sequentalID)
	{
		boolean networkChanged = updateBeaconPeriod(remoteTransportRevision, timestamp, sequentalID);
		if (networkChanged)
			changedTransport();
	}

	/**
	 * Update beacon period.
	 * @param remoteTransportRevision
	 * @param timestamp
	 * @param sequentalID
	 * @return	network change (server restarted) detected.
	 */
	private synchronized boolean updateBeaconPeriod(short remoteTransportRevision, long timestamp, long sequentalID) {
		
		// first beacon notification check
		if (lastBeaconTimeStamp == Long.MIN_VALUE)
		{
			// new server up...
			context.beaconAnomalyNotify();
			
			if (remoteTransportRevision >= 10)
			{
				lastBeaconSequenceID = sequentalID;
			}
			
			lastBeaconTimeStamp = timestamp;
			beaconArrivalNotify();
			return false;
		}

		// v4.10+ support beacon sequental IDs and additional checks are possible:
		// - detect beacon duplications due to redundant routes
		// - detect lost beacons due to input queue overrun or damage
		if (remoteTransportRevision >= 10)
		{
			long beaconSeqAdvance;
			if (sequentalID >= lastBeaconSequenceID)
				beaconSeqAdvance = sequentalID - lastBeaconSequenceID;
			else
				beaconSeqAdvance = (0x00000000FFFFFFFFL - lastBeaconSequenceID) + sequentalID;

			lastBeaconSequenceID = sequentalID;
			
			// throw out sequence numbers just prior to, or the same as, the last one received 
			// (this situation is probably caused by a temporary duplicate route )
			if (beaconSeqAdvance == 0 || beaconSeqAdvance > 0x00000000FFFFFFFFL - 256)
				return false;

			// throw out sequence numbers that jump forward by only a few numbers 
			// (this situation is probably caused by a duplicate route 
			//  or a beacon due to input queue overun)
			if (beaconSeqAdvance > 1 && beaconSeqAdvance < 4)
				return false;
		}

		boolean networkChange = false;
		long currentPeriod = timestamp - lastBeaconTimeStamp;

		// second beacon, period can be calculated now
		if (averagePeriod < 0)
		{
			averagePeriod = currentPeriod;

			beaconArrivalNotify();
		}
		else
		{
			// is this a server seen because of a restored network segment?
			if (currentPeriod >= (averagePeriod * 1.25))
			{
				if (currentPeriod >= (averagePeriod * 3.25))
				{
					context.beaconAnomalyNotify();

					// trigger network change on any 3 contiguous missing beacons 
					networkChange = true;
				}
				else if (!periodStabilized)
				{
					// boost current period
					averagePeriod = currentPeriod;
					beaconArrivalNotify();
				}
				else
				{
					// something might be wrong...
					context.beaconAnomalyNotify();
				}
			}
			// is this a server seen because of reboot
			// (beacons come at a higher rate just after the)
			else if (currentPeriod <= (averagePeriod * 0.8))
			{
				// server restarted...
				context.beaconAnomalyNotify();
				
				networkChange = true;
			}
			// all OK
			else
			{
				periodStabilized = true;
				beaconArrivalNotify();
			}

			if (networkChange) {
				// reset 
				periodStabilized = false;
				averagePeriod = -1;
			}
			else {
				// update a running average period
				averagePeriod = (long)(currentPeriod * 0.125 + averagePeriod * 0.875);
			}
		}

		lastBeaconTimeStamp = timestamp;
		
		return networkChange;
	}

	/**
	 * Notify transport about beacon arrival.
	 */
	private void beaconArrivalNotify()
	{
		Transport[] transports = context.getTransportRegistry().get(responseFrom);
		if (transports == null)
			return;

		// notify all
		for (int i = 0; i < transports.length; i++)
			((CATransport)transports[i]).beaconArrivalNotify();
	}


	/**
	 * Changed transport (server restared) notify. 
	 */
	private void changedTransport()
	{
		Transport[] transports = context.getTransportRegistry().get(responseFrom);
		if (transports == null)
			return;

		// notify all
		for (int i = 0; i < transports.length; i++)
			((CATransport)transports[i]).changedTransport();
	}
}
