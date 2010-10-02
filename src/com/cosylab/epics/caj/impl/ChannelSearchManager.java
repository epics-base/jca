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

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.requests.VersionRequest;
import com.cosylab.epics.caj.util.ArrayFIFO;
import com.cosylab.epics.caj.util.Timer;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ChannelSearchManager {
	
	/**
	 * Max search tries per frame.
	 */
	private static final int MAX_FRAMES_PER_TRY = 64;

	private class SearchTimer implements Timer.TimerRunnable
	{
		/**
		 * Number of search attempts in one frame.
		 */
		volatile int searchAttempts = 0; 

		/**
		 * Number of search responses in one frame.
		 */
		volatile int searchRespones = 0; 
		
		/**
		 * Number of frames per search try.
		 */
		double framesPerTry = 1;
		
		/**
		 * Number of frames until congestion threshold is reached.
		 */
	    double framesPerTryCongestThresh = Double.MAX_VALUE;

	    /**
	     * Start sequence number (first frame number within a search try). 
	     */
	    volatile int startSequenceNumber = 0;
	    
	    /**
	     * End sequence number (last frame number within a search try). 
	     */
	    volatile int endSequenceNumber = 0;
	    
	    /**
	     * This timer index.
	     */
	    final int timerIndex;
	    
	    /**
	     * Flag indicating whether boost is allowed. 
	     */
	    final boolean allowBoost;
	    
	    /**
	     * Flag indicating whether slow-down is allowed (for last timer). 
	     */
	    final boolean allowSlowdown;

	    /**
		 * Ordered (as inserted) list of channels with search request pending.
		 */
	    ArrayFIFO requestPendingChannels = new ArrayFIFO();

		/**
		 * Ordered (as inserted) list of channels with search request pending.
		 */
	    ArrayFIFO responsePendingChannels = new ArrayFIFO();

		/**
		 * Timer ID.
		 * (sync on requestPendingChannels)
		 */
		Object timerTaskID = null;

		/**
		 * Cancel this instance.
		 */
		volatile boolean canceled = false;
		
	    /**
	     * Time of last response check.
	     */
	    long timeAtResponseCheck = 0;

	    /**
		 * Constructor;
		 * @param timerIndex this timer instance index.
		 * @param allowBoost is boost allowed flag.
		 */
		public SearchTimer(int timerIndex, boolean allowBoost, boolean allowSlowdown) {
			this.timerIndex = timerIndex;
			this.allowBoost = allowBoost;
			this.allowSlowdown = allowSlowdown;
		}
		
		/**
		 * Shutdown this instance.
		 */
		public synchronized void shutdown()
		{
			if (canceled)
				return;
			canceled = true;

			synchronized (requestPendingChannels)
			{
				if (timerTaskID != null)
				{
					Timer.cancel(timerTaskID);
					timerTaskID = null;
				}
			}
			
			requestPendingChannels.clear();
			responsePendingChannels.clear();
		}
		
		/**
		 * Install channel.
		 * @param channel channel to be registered.
		 */
		public synchronized void installChannel(CAJChannel channel)
		{
			if (canceled)
				return;

			synchronized (requestPendingChannels)
			{
				boolean startImmediately = requestPendingChannels.isEmpty();
				channel.addAndSetListOwnership(requestPendingChannels, timerIndex);

				// start searching
				if (startImmediately) {
					if (timerTaskID != null)
						Timer.cancel(timerTaskID);
					if (timeAtResponseCheck == 0)
						timeAtResponseCheck = System.currentTimeMillis();
					// start with some initial delay (to collect all installed requests)
					timerTaskID = context.getTimer().executeAfterDelay(10, this);
				}
			}
		}

		/**
		 * Uninstall channel.
		 * @param channel channel to be unregistered.
		 *
		public void uninstallChannel(CAJChannel channel)
		{
			unregisterChannel(channel);
		}*/

		/**
		 * Move channels to other <code>SearchTimer</code>.
		 * @param destination where to move channels.
		 */
		public void moveChannels(SearchTimer destination)
		{
			// do not sync this, not necessary and might cause deadlock
			CAJChannel channel;
			while ((channel = (CAJChannel)responsePendingChannels.pop()) != null) {
				if (searchAttempts > 0)
					searchAttempts--;
				destination.installChannel(channel);
			}

			// bulk move
			synchronized (requestPendingChannels) {
				while (!requestPendingChannels.isEmpty())
					destination.installChannel((CAJChannel)requestPendingChannels.pop());
			}
		}

		/**
		 * Called when timer expired.
		 * @param timeoutTime expiration time.
		 */
		public void timeout(long timeoutTime)
		{

			if (canceled)
				return;

			synchronized (requestPendingChannels) {
				timerTaskID = null;
			}	

			// if there was some success (no congestion)
			// boost search period (if necessary) for channels not recently searched
			if (allowBoost && searchRespones > 0)
			{
				synchronized (requestPendingChannels) {
					while (!requestPendingChannels.isEmpty()) {
						CAJChannel channel = (CAJChannel)requestPendingChannels.peek();
						// boost needed check
						//final int boostIndex = searchRespones >= searchAttempts * SUCCESS_RATE ? Math.min(Math.max(0, timerIndex - 1), beaconAnomalyTimerIndex) : beaconAnomalyTimerIndex;
						final int boostIndex = beaconAnomalyTimerIndex;
						if (channel.getOwnerIndex() > boostIndex)
						{
							requestPendingChannels.pop();
							channel.unsetListOwnership();
							boostSearching(channel, boostIndex);
						}
					}
				}
			}

			CAJChannel channel;
			
			// should we check results (installChannel trigger timer immediately)
			long now = System.currentTimeMillis();
			if (now - timeAtResponseCheck >= period())
			{
				timeAtResponseCheck = now;
				
				// notify about timeout (move it to other timer) 
				while ((channel = (CAJChannel)responsePendingChannels.pop()) != null) {
					if (allowSlowdown) {
						channel.unsetListOwnership();
						searchResponseTimeout(channel, timerIndex);
					}
					else {
						channel.addAndSetListOwnership(requestPendingChannels, timerIndex);
					}
				}
		
				// check search results
				if (searchAttempts > 0)
				{
		            // increase UDP frames per try if we have a good score
					if (searchRespones >= searchAttempts * SUCCESS_RATE)
					{
						// increase frames per try
		                // a congestion avoidance threshold similar to TCP is now used
						if (framesPerTry < MAX_FRAMES_PER_TRY)
						{
							if (framesPerTry < framesPerTryCongestThresh)
								framesPerTry = Math.min(2*framesPerTry, framesPerTryCongestThresh);
							else
								framesPerTry += 1.0/framesPerTry;
						}
					}
					else
					{
						// decrease frames per try, fallback
						framesPerTryCongestThresh = framesPerTry / 2.0;
						framesPerTry = 1;
					}
				
				}
			}
			
			startSequenceNumber = getSequenceNumber() + 1;
			
			searchAttempts = 0;
			searchRespones = 0;
			
			int framesSent = 0;
			int triesInFrame = 0;
			
			while ((channel = (CAJChannel)requestPendingChannels.pop()) != null) {
				channel.unsetListOwnership();
				
				boolean requestSent = true;
				boolean allowNewFrame = (framesSent+1) < framesPerTry;
				boolean frameWasSent;
				try
				{
					frameWasSent = generateSearchRequestMessage(channel, allowNewFrame);
				} catch (Throwable th) {
					// make sure that channel is owned
					// and add it to response list not to cause dead-loops
					channel.addAndSetListOwnership(responsePendingChannels, timerIndex);
					// do not report any error
					break;
				}
				if (frameWasSent) {
					framesSent++;
					triesInFrame = 0;
					if (!allowNewFrame) { 
						channel.addAndSetListOwnership(requestPendingChannels, timerIndex);
						requestSent = false;
					}
					else
						triesInFrame++;
				}
				else
					triesInFrame++;

				if (requestSent) {
					channel.addAndSetListOwnership(responsePendingChannels, timerIndex);
					if (searchAttempts < Integer.MAX_VALUE)
						searchAttempts++;
				}
				
				// limit
				if (triesInFrame == 0 && !allowNewFrame)
					break;
			}

		    // flush out the search request buffer
			if (triesInFrame > 0) {
				try
				{
					flushSendBuffer();
					framesSent++;
				} catch (Throwable th) {
					// noop
				}
			}
			
			endSequenceNumber = getSequenceNumber();
			
			// reschedule
			synchronized (requestPendingChannels)
			{
				if (!canceled && timerTaskID == null) {
					boolean someWorkToDo = (!requestPendingChannels.isEmpty() || !responsePendingChannels.isEmpty());
					if (someWorkToDo)
						timerTaskID = context.getTimer().executeAfterDelay(period(), this);
				}
			}

		}
		
		/**
		 * Search response received notification.
		 * @param responseSequenceNumber sequence number of search frame which contained search request.
		 * @param isSequenceNumberValid valid flag of <code>responseSequenceNumber</code>.
		 * @param responseTime time of search response.
		 */
		public void searchResponse(int responseSequenceNumber, boolean isSequenceNumberValid, long responseTime)
		{
			if (canceled)
				return;
		
			boolean validResponse = true;
			if (isSequenceNumberValid)
				validResponse = startSequenceNumber <= sequenceNumber && sequenceNumber <= endSequenceNumber;
			
			// update RTTE
			if (validResponse)
			{
				final long dt = responseTime - getTimeAtLastSend();
				updateRTTE(dt);
				
				if (searchRespones < Integer.MAX_VALUE)
				{
					searchRespones++;
					
					// all found, send new search requests immediately if neccessary
					if (searchRespones == searchAttempts)
					{
						if (requestPendingChannels.size() > 0) 
						{
							if (timerTaskID != null)
								Timer.cancel(timerTaskID);
							timerTaskID = context.getTimer().executeAfterDelay(0, this);
						}
					}
				}
			}
		}
		
		
		/**
		 * Calculate search time period.
		 * @return search time period.
		 */
		public final long period()
		{
			return (long) ((1 << timerIndex) * getRTTE());
		}

	}
	
	

	/**
	 * Minimal RTT (ms).
	 */
	private static final long MIN_RTT = 32;

	/**
	 * Maximal RTT (ms).
	 */
	private static final long MAX_RTT = 2 * MIN_RTT;

	/**
	 * Rate to be considered as OK.
	 */
	private static final double SUCCESS_RATE = 0.9;

	/**
	 * Context.
	 */
	private CAJContext context;

	/**
	 * Canceled flag.
	 */
	private volatile boolean canceled = false;

	/**
	 * Round-trip time (RTT) mean.
	 */
	private volatile double rttmean = MIN_RTT;

	/**
	 * Search timers.
	 * Each timer with a greater index has longer (doubled) search period.
	 */
	private SearchTimer[] timers;
	
	/**
	 * Index of a timer to be used when beacon anomaly is detected.
	 */
	private int beaconAnomalyTimerIndex;

	/**
	 * Search (datagram) sequence number.
	 */
	private volatile int sequenceNumber = 0;
	
	/**
	 * Max search period (in ms).
	 */
	private static final long MAX_SEARCH_PERIOD = 5 * 60000;  
	
	/**
	 * Max search period (in ms) - lower limit.
	 */
	private static final long MAX_SEARCH_PERIOD_LOWER_LIMIT = 60000;  

	/**
	 * Beacon anomaly search period (in ms).
	 */
	private static final long BEACON_ANOMALY_SEARCH_PERIOD = 5000;  
	
	/**
	 * Max number of timers.
	 */
	private static final int MAX_TIMERS = 18;  

	/**
	 * Send byte buffer (frame)
	 */
	private ByteBuffer sendBuffer;
	
    /**
     * Time of last frame send.
     */
    private volatile long timeAtLastSend;

    /**
     * Set of registered channels.
     */
    private Set channels = new HashSet();
    
    /**
	 * Constructor.
	 * @param context
	 */
	public ChannelSearchManager(CAJContext context)
	{
		this.context = context;

		// create and initialize send buffer
		sendBuffer = ByteBuffer.allocateDirect(CAConstants.MAX_UDP_SEND);
		initializeSendBuffer();

		// TODO should be configurable
		long maxPeriod = MAX_SEARCH_PERIOD;
		
		maxPeriod = Math.min(maxPeriod, MAX_SEARCH_PERIOD_LOWER_LIMIT);
		
		// calculate number of timers to reach maxPeriod (each timer period is doubled)
		double powerOfTwo = Math.log(maxPeriod / (double)MIN_RTT) / Math.log(2);
		int numberOfTimers = (int)(powerOfTwo + 1);
		numberOfTimers = Math.min(numberOfTimers, MAX_TIMERS);

		// calculate beacon anomaly timer index
		powerOfTwo = Math.log(BEACON_ANOMALY_SEARCH_PERIOD  / (double)MIN_RTT) / Math.log(2);
		beaconAnomalyTimerIndex = (int)(powerOfTwo + 1);
		beaconAnomalyTimerIndex = Math.min(beaconAnomalyTimerIndex, numberOfTimers - 1);
		
		// create timers
		timers = new SearchTimer[numberOfTimers];
		for (int i = 0; i < numberOfTimers; i++)
			timers[i] = new SearchTimer(i, i > beaconAnomalyTimerIndex, i != (numberOfTimers-1));
	}
	
	/**
	 * Cancel.
	 */
	public synchronized void cancel()
	{
		if (canceled)
			return;
		canceled = true;

		if (timers != null)
			for (int i = 0; i < timers.length; i++)
				timers[i].shutdown();
	}

	/**
	 * Initialize send buffer.
	 */
	private void initializeSendBuffer()
	{
		sendBuffer.clear();
		
		// put version message
		sequenceNumber++;
		VersionRequest.generateVersionRequestMessage(context.getBroadcastTransport(), sendBuffer, (short)0, sequenceNumber, true);
		
	}

	/**
	 * Flush send buffer.
	 */
	private synchronized void flushSendBuffer()
	{
		timeAtLastSend = System.currentTimeMillis();
		context.getBroadcastTransport().send(sendBuffer);
		initializeSendBuffer();
	}
	
	/**
	 * Generate (put on send buffer) search request 
	 * @param channel 
	 * @param allowNewFrame flag indicating if new search request message is allowed to be put in new frame.
	 * @return <code>true</code> if new frame was sent.
	 */
	private synchronized boolean generateSearchRequestMessage(CAJChannel channel, boolean allowNewFrame)
	{
		boolean success = channel.generateSearchRequestMessage(context.getBroadcastTransport(), sendBuffer);
		// buffer full, flush
		if (!success)
		{
			flushSendBuffer();
			if (allowNewFrame)
				channel.generateSearchRequestMessage(context.getBroadcastTransport(), sendBuffer);
			return true;
		}
		
		return false;
	}

	/**
	 * Get number of registered channels.
	 * @return number of registered channels.
	 */
	public int registeredChannelCount() {
		synchronized (channels) {
			return channels.size();
		}
	}

	/**
	 * Register channel.
	 * @param channel
	 */
	public void registerChannel(CAJChannel channel)
	{
		if (canceled)
			return;

		synchronized (channels)
		{
			if (channels.contains(channel))
				return;
			channels.add(channel);

			timers[0].installChannel(channel);
		}
	}


	/**
	 * Unregister channel.
	 * @param channel
	 */
	public void unregisterChannel(CAJChannel channel)
	{
		synchronized (channels)
		{
			if (!channels.contains(channel))
				return;
			channels.remove(channel);
				
			channel.removeAndUnsetListOwnership();
		}
	}
	
	/**
	 * Search response received notification.
	 * @param channel found channel.
	 * @param responseSequenceNumber sequence number of search frame which contained search request.
	 * @param isSequenceNumberValid valid flag of <code>responseSequenceNumber</code>.
	 * @param responseTime time of search response.
	 */
	public void searchResponse(CAJChannel channel, int responseSequenceNumber, boolean isSequenceNumberValid, long responseTime)
	{
		int timerIndex = channel.getOwnerIndex();
		unregisterChannel(channel);
		
		timers[timerIndex].searchResponse(responseSequenceNumber, isSequenceNumberValid, responseTime);
	}
	
	/**
	 * Notifty about search failure (response timeout).
	 * @param channel channel whose search failed.
	 * @param timerIndex index of timer which tries to search.
	 */
	private void searchResponseTimeout(CAJChannel channel, int timerIndex)
	{
		int newTimerIndex = Math.min(++timerIndex, timers.length - 1);
		timers[newTimerIndex].installChannel(channel);
	}

	/**
	 * Beacon anomaly detected.
	 * Boost searching of all channels.
	 */
	public void beaconAnomalyNotify()
	{
//System.out.println("[*] beaconAnomaly");
		
		for (int i = beaconAnomalyTimerIndex + 1; i < timers.length; i++)
			timers[i].moveChannels(timers[beaconAnomalyTimerIndex]);
	}
	
	/**
	 * Boost searching of a channel.
	 * @param channel channel to boost searching.
	 * @param timerIndex to what timer-index to boost
	 */
	private void boostSearching(CAJChannel channel, int timerIndex)
	{
		timers[timerIndex].installChannel(channel);
	}
	
	/**
	 * Update (recalculate) round-trip estimate.
	 * @param rtt new sample of round-trip value.
	 */
	private final void updateRTTE(long rtt)
	{
		final double error = rtt - rttmean;
		rttmean += error / 4.0;
//System.out.println("rtt:" + rtt + ", rttmean:" + rttmean);
	}
	
	/**
	 * Get round-trip estimate (in ms).
	 * @return round-trip estimate (in ms).
	 */
	private final double getRTTE() {
		return Math.min(Math.max(rttmean, MIN_RTT), MAX_RTT);
	}
	
	/**
	 * Get search (UDP) frame sequence number.
	 * @return search (UDP) frame sequence number.
	 */
	private final int getSequenceNumber()
	{
		return sequenceNumber;
	}
	
	/**
	 * Get time at last send (when sendBuffer was flushed).
	 * @return time at last send.
	 */
	private final long getTimeAtLastSend()
	{
		return timeAtLastSend;
	}
}
