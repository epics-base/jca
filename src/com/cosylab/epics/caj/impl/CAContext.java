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

import com.cosylab.epics.caj.impl.reactor.Reactor;
import com.cosylab.epics.caj.impl.reactor.lf.LeaderFollowersThreadPool;
import com.cosylab.epics.caj.util.Timer;
import com.cosylab.epics.caj.util.logging.LoggerProvider;

/**
 * Interface defining <code>CAContext</code> (logging, reactor, thread-pool, etc.).
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public interface CAContext extends LoggerProvider {

	/**
	 * Get context reactor.
	 * @return context reactor.
	 */
	public Reactor getReactor();

    /**
     * Get LF thread pool.
     * @return LF thread pool, can be <code>null</code> if disabled.
     */
    public LeaderFollowersThreadPool getLeaderFollowersThreadPool();

	/**
	 * Get CA transport (virtual circuit) registry.
	 * @return CA transport (virtual circuit) registry.
	 */
	public CATransportRegistry getTransportRegistry();

    /**
     * Get cached byte allocator.
     * @return cached byte allocator.
     */
    public CachedByteBufferAllocator getCachedBufferAllocator();

	/**
	 * Get timer.
	 * @return timer.
	 */
	public Timer getTimer();

	/**
	 * Get CA server port.
	 * @return CA server port.
	 */
	public int getServerPort();

	/**
	 * Get CA broadcast (send) port.
	 * @return CA broadcast (send) port.
	 */
	public int getBroadcastPort();

	/**
	 * Broadcast transport.
	 * @return broadcast transport.
	 */
	public BroadcastTransport getBroadcastTransport();

	/**
	 * Invalidate last (UDP) received sequence.
	 */
	public void invalidateLastReceivedSequence();
}
