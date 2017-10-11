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

package com.cosylab.epics.caj.util;

/**
 * Timer. Based on <code>EDU.oswego.cs.dl.util.concurrent</code>. Timer tasks
 * should complete quickly. If a timer task takes excessive time to complete, it
 * "hogs" the timer's task execution thread. This can, in turn, delay the
 * execution of subsequent tasks, which may "bunch up" and execute in rapid
 * succession when (and if) the offending task finally completes.
 * 
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class SearchTimer extends Thread {

	/**
	 * Tasks are maintained in a standard priority queue.
	 **/
	protected final Heap heap = new Heap(64);

	protected final RunLoop runLoop = new RunLoop();

	/**
	 * Protected constructor (singleton pattern).
	 */
	public SearchTimer() {
	}


	public static abstract class TimerTask implements Comparable {
		private long timeToRun; // The time to run command
		private long delay; // The delay

		// Cancellation does not immediately remove node, it just
		// sets up lazy deletion bit, so is thrown away when next
		// encountered in run loop
		private boolean cancelled = false;

		// Access to cancellation status and and run time needs sync
		// since they can be written and read in different threads

		synchronized void setCancelled() {
			cancelled = true;
		}

		synchronized boolean getCancelled() {
			return cancelled;
		}

		// rt = now + d
		synchronized void setTimeToRun(long d, long rt) {
			delay = d;
			timeToRun = rt;
		}

		public synchronized long getTimeToRun() {
			return timeToRun;
		}

		public synchronized long getDelay() {
			return delay;
		}

		public int compareTo(Object other) {
			long a = getTimeToRun();
			long b = ((TimerTask) (other)).getTimeToRun();
			return (a < b) ? -1 : ((a == b) ? 0 : 1);
		}

		/**
		 * Method invoked by timer at requested time.
		 * 
		 * @return delay (in ms) after which to reschedule,
		 * 		   not rescheduled if <= 0.
		 */
		public abstract long timeout();
	}

	/**
	 * Execute the given command after waiting for the given delay.
	 * 
	 * @param millisecondsToDelay
	 *            -- the number of milliseconds from now to run the command.
	 * @param command
	 *            -- the command to run after the delay.
	 **/
	public synchronized void executeAfterDelay(
			long millisecondsToDelay,
			TimerTask task) {
		long runtime = System.currentTimeMillis() + millisecondsToDelay;
		task.setTimeToRun(millisecondsToDelay, runtime);
		heap.insert(task);
		restart();
	}

	public synchronized void rescheduleAllAfterDelay(long millisecondsToDelay) {
		long timeToRun = System.currentTimeMillis() + millisecondsToDelay;

		synchronized (heap) {
			Object[] nodes = heap.getNodes();
			int count = heap.size();
			for (int i = 0; i < count; i++)
				((TimerTask) nodes[i]).setTimeToRun(millisecondsToDelay, timeToRun);
		}

		restart();
	}

	/**
	 * Cancel a scheduled task that has not yet been run. The task will be
	 * cancelled upon the <em>next</em> opportunity to run it. This has no
	 * effect if this is a one-shot task that has already executed. Also, if an
	 * execution is in progress, it will complete normally. (It may however be
	 * interrupted via getThread().interrupt()). But if it is a periodic task,
	 * future iterations are cancelled.
	 * 
	 * @param taskID
	 *            -- a task reference returned by one of the execute commands
	 * @exception ClassCastException
	 *                if the taskID argument is not of the type returned by an
	 *                execute command.
	 **/
	public static void cancel(Object taskID) {
		((TimerTask) taskID).setCancelled();
	}

	/** The thread used to process commands **/
	protected Thread thread;

	/*
	 * Return the thread being used to process commands, or null if there is no
	 * such thread. You can use this to invoke any special methods on the
	 * thread, for example, to interrupt it.
	 */
	public synchronized Thread getThread() {
		return thread;
	}

	/** set thread to null to indicate termination **/
	protected synchronized void clearThread() {
		thread = null;
	}

	/**
	 * Start (or restart) a thread to process commands, or wake up an existing
	 * thread if one is already running. This method can be invoked if the
	 * background thread crashed due to an unrecoverable exception in an
	 * executed command.
	 **/

	public synchronized void restart() {
		if (thread == null) {
			thread = new Thread(runLoop, this.getClass().getName());
			thread.start();
		} else
			notify();
	}

	/**
	 * Cancel all tasks and interrupt the background thread executing the
	 * current task, if any. A new background thread will be started if new
	 * execution requests are encountered. If the currently executing task does
	 * not respond to interrupts, the current thread may persist, even if a new
	 * thread is started via restart().
	 **/
	public synchronized void shutDown() {
		heap.clear();
		if (thread != null)
			thread.interrupt();
		thread = null;
	}

	/**
	 * Return the next task to execute, or null if thread is interrupted.
	 **/
	protected synchronized TimerTask nextTask(boolean blockAndExtract, long dt) {

		// Note: This code assumes that there is only one run loop thread

		try {
			while (!Thread.interrupted()) {

				// Using peek simplifies dealing with spurious wakeups

				TimerTask task = (TimerTask) (heap.peek());

				if (task == null) {
					if (!blockAndExtract)
						return null;
					wait();
				} else {
					long now = System.currentTimeMillis();
					long when = task.getTimeToRun();

					if ((when - dt) > now) { // false alarm wakeup
						if (!blockAndExtract)
							return null;
						wait(when - now);
					} else {
						if (!blockAndExtract)
							return task;
						
						task = (TimerTask) (heap.extract());

						if (!task.getCancelled()) { // Skip if cancelled by
							return task;
						}
					}
				}
			}
		} catch (InterruptedException ex) {
		} // fall through

		return null; // on interrupt
	}

	/**
	 * Check whether there is a task scheduled in next "dT" ms.
	 * @param dT
	 * @return
	 */
	public boolean hasNext(long dT)
	{
		return (nextTask(false, dT) != null);
	}
	
	/**
	 * The run loop is isolated in its own Runnable class just so that the main
	 * class need not implement Runnable, which would allow others to directly
	 * invoke run, which is not supported.
	 **/

	protected class RunLoop implements Runnable {
		public void run() {
			try {
				for (;;) {
					TimerTask task = nextTask(true, 0);
					if (task != null) {
						long millisecondsToDelay = task.timeout();
						if (millisecondsToDelay > 0) {
							long runtime = System.currentTimeMillis()
									+ millisecondsToDelay;
							task.setTimeToRun(millisecondsToDelay, runtime);
							heap.insert(task);
						}
					} else
						break;
				}
			} finally {
				clearThread();
			}
		}
	}

}
