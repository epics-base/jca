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

package com.cosylab.epics.caj.impl.reactor.lf;

import gov.aps.jca.JCALibrary;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class LeaderFollowersThreadPool {

	/**
	 * Default thread pool size.
	 */
	public static final int DEFAULT_THREADPOOL_SIZE = 5;
	
    /**
     * Shutdown status flag.
     */
    private volatile boolean shutdown = false;
    
    /**
     * Executor.
     */
    private ThreadPoolExecutor executor;

    /**
     * Constructor.
     */
    public LeaderFollowersThreadPool() {
        
        int threadPoolSize = DEFAULT_THREADPOOL_SIZE;
        String strVal = JCALibrary.getInstance().getProperty(this.getClass().getName()+".thread_pool_size");
        if (strVal != null)
        {
        	try	{
        	    // minimum are two threads (leader and one follower)
        		threadPoolSize = Math.max(2, Integer.parseInt(strVal));
        	}
        	catch (NumberFormatException nfe) { /* noop */ }
        }

        // TODO consider using LIFO ordering of threads (to maximize CPU cache affinity)
        // unbounded queue is OK, since its naturally limited (threadPoolSize + # of transports (used for flushing))
        executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize,
        								  Long.MAX_VALUE, TimeUnit.NANOSECONDS,
        								  new LinkedBlockingQueue());
        executor.prestartAllCoreThreads();
    }

    /**
     * Promote a new leader.
     * @param task task to execute by a new leader.
     */
    public void promoteLeader(Runnable task) 
    {
        //System.err.println("[promoteLeader] by " + Thread.currentThread().getName());
        execute(task);
    }
    
    /**
     * Execute given task.
     * @param task task to execute.
     */
    public void execute(Runnable task) 
    {
        try
        {
            executor.execute(task);
        } catch (Throwable th) { /* noop */ th.printStackTrace(); }
    }
    
    /**
     * Shutdown.
     */
    public synchronized void shutdown()
    {
    	if (shutdown)
    		return;
    	shutdown = true;

    	executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS))
            	executor.shutdownNow();
        } catch (InterruptedException ie) { /* noop */ } 
    }
    
}
