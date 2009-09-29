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
 * Timer.
 * Based on <code>EDU.oswego.cs.dl.util.concurrent</code> (it was not approriate for CAJ usage).
 * Timer tasks should complete quickly. If a timer task takes excessive time to complete,
 * it "hogs" the timer's task execution thread. This can, in turn, delay the execution of
 * subsequent tasks, which may "bunch up" and execute in rapid succession when (and if)
 * the offending task finally completes.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class Timer extends Thread  {

  /**
   * Protected constructor (singleton pattern). 
   */
  public Timer() {
	runLoop_ = new RunLoop();
  }

  /**
   * Tasks are maintained in a standard priority queue.
   **/
  protected final Heap heap_ = new Heap(64);

  /**
   * Timer runnable interface.
   */
  public interface TimerRunnable 
  {
  	/**
  	 * Method invoked by timer at requested time.
	 * @param timeToRun time in ms to run.
  	 */
  	public void timeout(long timeToRun);  
  }

  private static class TaskNode implements Comparable {
	final TimerRunnable command;   // The command to run
	final long period;        // The cycle period, or -1 if not periodic
	private long timeToRun_;  // The time to run command

	// Cancellation does not immediately remove node, it just
	// sets up lazy deletion bit, so is thrown away when next 
	// encountered in run loop

	private boolean cancelled_ = false;

	// Access to cancellation status and and run time needs sync 
	// since they can be written and read in different threads

	synchronized void setCancelled() { cancelled_ = true; }
	synchronized boolean getCancelled() { return cancelled_; }

	synchronized void setTimeToRun(long w) { timeToRun_ = w; }
	synchronized long getTimeToRun() { return timeToRun_; }
    
    
	public int compareTo(Object other) {
	  long a = getTimeToRun();
	  long b = ((TaskNode)(other)).getTimeToRun();
	  return (a < b)? -1 : ((a == b)? 0 : 1);
	}

	TaskNode(long w, TimerRunnable c, long p) {
	  timeToRun_ = w; command = c; period = p;
	}

	TaskNode(long w, TimerRunnable c) { this(w, c, -1); }
  }

  /*
   * Execute the given command at the given time.
   * @param date -- the absolute time to run the command, expressed
   * as a java.util.Date.
   * @param command -- the command to run at the given time.
   * @return taskID -- an opaque reference that can be used to cancel execution request
   **
  public Object executeAt(Date date, TimerRunnable command) {
	TaskNode task = new TaskNode(date.getTime(), command); 
	heap_.insert(task);
	restart();
	return task;
  }*/

  /** 
   * Excecute the given command after waiting for the given delay.
   * @param millisecondsToDelay -- the number of milliseconds
   * from now to run the command.
   * @param command -- the command to run after the delay.
   * @return taskID -- an opaque reference that can be used to cancel execution request
   **/
  public Object executeAfterDelay(long millisecondsToDelay, TimerRunnable command) {
	long runtime = System.currentTimeMillis() + millisecondsToDelay;
	TaskNode task = new TaskNode(runtime, command);
	heap_.insert(task);
	restart();
	return task;
  }

  /**
   * Execute the given command every <code>period</code> milliseconds AT FIXED RATE.
   * If <code>startNow</code> is true, execution begins immediately,
   * otherwise, it begins after the first <code>period</code> delay.
   * @param period -- the period, in milliseconds. Periods are
   *  measured from start-of-task to the next start-of-task. It is
   * generally a bad idea to use a period that is shorter than 
   * the expected task duration.
   * @param command -- the command to run at each cycle
   * @param firstTime -- time when task should start with execution, 0 means immediately.
   * @exception IllegalArgumentException if period less than or equal to zero.
   * @return taskID -- an opaque reference that can be used to cancel execution request
   **/
  // msekoran: added firstTime support
  public Object executePeriodically(long period,
									TimerRunnable command, 
									long firstTime) {

	if (period <= 0) throw new IllegalArgumentException();

	if (firstTime == 0)
		firstTime = System.currentTimeMillis();

	TaskNode task = new TaskNode(firstTime, command, period); 
	heap_.insert(task);
	restart();
	return task;
  }

  /** 
   * Cancel a scheduled task that has not yet been run. 
   * The task will be cancelled
   * upon the <em>next</em> opportunity to run it. This has no effect if
   * this is a one-shot task that has already executed.
   * Also, if an execution is in progress, it will complete normally.
   * (It may however be interrupted via getThread().interrupt()).
   * But if it is a periodic task, future iterations are cancelled. 
   * @param taskID -- a task reference returned by one of
   * the execute commands
   * @exception ClassCastException if the taskID argument is not 
   * of the type returned by an execute command.
   **/
  public static void cancel(Object taskID) {
	((TaskNode)taskID).setCancelled();
  }
   

  /** The thread used to process commands **/
  protected Thread thread_;

  
  /*
   * Return the thread being used to process commands, or
   * null if there is no such thread. You can use this
   * to invoke any special methods on the thread, for
   * example, to interrupt it.
   *
  public synchronized Thread getThread() { 
	return thread_;
  }*/

  /** set thread_ to null to indicate termination **/
  protected synchronized void clearThread() {
	thread_ = null;
  }

  /**
   * Start (or restart) a thread to process commands, or wake
   * up an existing thread if one is already running. This
   * method can be invoked if the background thread crashed
   * due to an unrecoverable exception in an executed command.
   **/

  public synchronized void restart() {
	if (thread_ == null) {
	  thread_ = new Thread(runLoop_, this.getClass().getName());
	  thread_.start();
	}
	else
	  notify();
  }


  /**
   * Cancel all tasks and interrupt the background thread executing
   * the current task, if any.
   * A new background thread will be started if new execution
   * requests are encountered. If the currently executing task
   * does not repsond to interrupts, the current thread may persist, even
   * if a new thread is started via restart().
   **/
  public synchronized void shutDown() {
	heap_.clear();
	if (thread_ != null) 
	  thread_.interrupt();
	thread_ = null;
  }

  /**
   * Return the next task to execute, or null if thread is interrupted.
   **/
  protected synchronized TaskNode nextTask() {

	// Note: This code assumes that there is only one run loop thread

	try {
	  while (!Thread.interrupted()) {

		// Using peek simplifies dealing with spurious wakeups

		TaskNode task = (TaskNode)(heap_.peek());

		if (task == null) {
		  wait();
		}
		else  {
		  long now = System.currentTimeMillis();
		  long when = task.getTimeToRun();

		  if (when > now) { // false alarm wakeup
			wait(when - now);
		  }
		  else {
			task = (TaskNode)(heap_.extract());

			if (!task.getCancelled()) { // Skip if cancelled by

			  if (task.period > 0) {  // If periodic, requeue
			  	// msekoran: using fixed rate scheduling 
				task.setTimeToRun(when + task.period);
				//task.setTimeToRun(now + task.period);
				heap_.insert(task);
			  }
              
			  return task;
			}
		  }
		}
	  }
	}
	catch (InterruptedException ex) {  } // fall through

	return null; // on interrupt
  }

  /**
   * The runloop is isolated in its own Runnable class
   * just so that the main 
   * class need not implement Runnable,  which would
   * allow others to directly invoke run, which is not supported.
   **/

  protected class RunLoop implements Runnable {
	public void run() {
	  try {
		for (;;) {
		  TaskNode task = nextTask();
		  if (task != null)
		    // msekoran added timeToRun parameter
			task.command.timeout(task.getTimeToRun() - task.period);
			//task.command.run();
		  else
			break;
		}
	  }
	  finally {
		clearThread();
	  }
	}
  }

  protected final RunLoop runLoop_;

}

