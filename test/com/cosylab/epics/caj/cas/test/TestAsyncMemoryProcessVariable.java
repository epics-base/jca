/**
 * 
 */
package com.cosylab.epics.caj.cas.test;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ProcessVariableReadCallback;
import gov.aps.jca.cas.ProcessVariableWriteCallback;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;

import com.cosylab.epics.caj.cas.util.MemoryProcessVariable;

/**
 * @author msekoranja
 *
 */
public class TestAsyncMemoryProcessVariable extends MemoryProcessVariable {

	private class ReadRequest implements Runnable
	{
		private DBR value;
		private ProcessVariableReadCallback asyncReadCallback;
		
		/**
		 * @param value
		 * @param asyncReadCallback
		 */
		public ReadRequest(DBR value, ProcessVariableReadCallback asyncReadCallback) {
			this.value = value;
			this.asyncReadCallback = asyncReadCallback;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			try
			{
				CAStatus status = read(value, null);
				asyncReadCallback.processVariableReadCompleted(status);
			} catch (Throwable th) {
				// we failed, cancel
				asyncReadCallback.canceled();
			}
		}		
		
	}
	
	private class WriteRequest implements Runnable
	{
		private DBR value;
		private ProcessVariableWriteCallback asyncWriteCallback;
		
		/**
		 * @param value
		 * @param asyncWriteCallback
		 */
		public WriteRequest(DBR value, ProcessVariableWriteCallback asyncWriteCallback) {
			this.value = value;
			this.asyncWriteCallback = asyncWriteCallback;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			try
			{
				CAStatus status = write(value, null);
				asyncWriteCallback.processVariableWriteCompleted(status);
			} catch (Throwable th) {
				// we failed, cancel
				asyncWriteCallback.canceled();
			}
		}		
		
	}

	/**
	 * Scheduled executor.
	 */
	private ScheduledThreadPoolExecutor executor = null;
	private long asyncProcessTimeMS;
	private boolean simulateException;

	/**
	 * @param name
	 * @param eventCallback
	 * @param type
	 * @param initialValue
	 */
	public TestAsyncMemoryProcessVariable(String name,
			ProcessVariableEventCallback eventCallback, DBRType type,
			Object initialValue, long asyncProcessTimeMS, boolean simulateException) {
		super(name, eventCallback, type, initialValue);

		executor = new ScheduledThreadPoolExecutor(1);
		
		this.asyncProcessTimeMS = asyncProcessTimeMS;
		this.simulateException = simulateException;
	}
	
	/**
	 * Destroy server.
	 */
	public void destroy()
	{
		if (executor != null)
			executor.shutdownNow();
	}

	/* (non-Javadoc)
	 * @see com.cosylab.epics.caj.cas.util.MemoryProcessVariable#read(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableReadCallback)
	 */
	public synchronized CAStatus read(DBR value, ProcessVariableReadCallback asyncReadCallback) throws CAException {

		// sync
		if (asyncReadCallback == null) {
			if (simulateException)
				throw new CAException("Simulated exception.");
			
			return super.read(value, asyncReadCallback);
		}
		
		// queue
		executor.schedule(new ReadRequest(value, asyncReadCallback),
						  asyncProcessTimeMS, TimeUnit.MILLISECONDS);

		// indicate async completion
		return null;
	}

	/* (non-Javadoc)
	 * @see com.cosylab.epics.caj.cas.util.MemoryProcessVariable#write(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableWriteCallback)
	 */
	public synchronized CAStatus write(DBR value, ProcessVariableWriteCallback asyncWriteCallback) throws CAException {
		
		// sync
		if (asyncWriteCallback == null)
		{
			if (simulateException)
				throw new CAException("Simulated exception.");
			
			return super.write(value, asyncWriteCallback);
		}
		
		// queue
		executor.schedule(new WriteRequest(value, asyncWriteCallback),
						  asyncProcessTimeMS, TimeUnit.MILLISECONDS);

		// indicate async completion
		return null;
	}

}
