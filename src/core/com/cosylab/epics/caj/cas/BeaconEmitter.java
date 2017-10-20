/*
 * Copyright (c) 2006 by Cosylab
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

package com.cosylab.epics.caj.cas;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.cosylab.epics.caj.cas.requests.BeaconRequest;
import com.cosylab.epics.caj.impl.BroadcastTransport;
import com.cosylab.epics.caj.impl.CAContext;
import com.cosylab.epics.caj.util.Timer;
import com.cosylab.epics.caj.util.Timer.TimerRunnable;

/**
 * Beacon emitter.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class BeaconEmitter implements TimerRunnable {

	/**
	 * Maximal CA beacon period (in seconds).
	 */
	protected static final float EPICS_CA_MAX_BEACON_PERIOD = 15.0f;

	/**
	 * Minimal (initial) CA beacon period (in seconds).
	 */
	protected static final float EPICS_CA_MIN_BEACON_PERIOD = 1.0e-3f;
	
	/**
	 * Timer.
	 */
	protected Timer timer;
	
	/**
	 * Logger.
	 */
	protected Logger logger;

	/**
	 * Broadcast (UDP) transport.
	 */
	protected BroadcastTransport transport;

	/**
	 * Beacon sequence ID.
	 */
	protected int beaconSequenceID = 0;

	/**
	 * Beacon period (in ms).
	 */
	protected int beaconPeriod = (int)(EPICS_CA_MIN_BEACON_PERIOD * 1000);

	/**
	 * Maximal beacon period (in ms).
	 */
	protected int maxBeaconPeriod;

	/**
	 * Timer task node.
	 */
	private Object taskID;

	
	/**
	 * Constructor.
	 * @param transport	transport to be used to send beacons.
	 * @param context CA context.
	 * @param beaconPeriod	configured (max) beacon period.
	 */
	public BeaconEmitter(BroadcastTransport transport, CAContext context, float beaconPeriod)
	{
		this.transport = transport;
		this.timer = context.getTimer();
		this.logger = context.getLogger();
		maxBeaconPeriod = (int)(Math.max(beaconPeriod, EPICS_CA_MIN_BEACON_PERIOD)*1000);
	}

	/**
	 * Start emitting.
	 */
	protected void start()
	{
		reschedule();
	}

	/**
	 * Reschedule timer.
	 */
	protected void reschedule()
	{
		taskID = timer.executeAfterDelay(beaconPeriod, this);
	}
		
	/**
	 * @see com.cosylab.epics.caj.util.Timer.TimerRunnable#timeout(long)
	 */
	public void timeout(long timeToRun)
	{

		// send beacon
		try
		{
			new BeaconRequest(transport, beaconSequenceID).submit();
		}
		catch (Throwable th) {
			logger.log(Level.WARNING, "Failed to send beacon.", th);
		}
			
		// increment beacon sequence ID
		beaconSequenceID++;
		
		// recalculate beacon period - double it, but dont exceed maxBeaconPeriod
		if (beaconPeriod < maxBeaconPeriod)
			beaconPeriod = Math.min(beaconPeriod * 2, maxBeaconPeriod);
		
		reschedule();
	}
	
	/**
	 * Destroy emitter.
	 */
	public void destroy()
	{
		if (taskID != null)
			Timer.cancel(taskID);
	}

}
