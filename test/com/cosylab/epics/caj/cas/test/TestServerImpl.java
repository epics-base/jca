package com.cosylab.epics.caj.cas.test;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableAttachCallback;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCompletion;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.cosylab.epics.caj.cas.util.DefaultServerImpl;


/**
 * Test async. completion server implementation.
 */
public class TestServerImpl extends DefaultServerImpl
{

	private class AttachRequest implements Runnable
	{
		private String aliasName;
		private ProcessVariableEventCallback eventCallback;
		private ProcessVariableAttachCallback asyncCompletionCallback;

		/**
		 * @param aliasName
		 * @param eventCallback
		 * @param asyncCompletionCallback
		 */
		public AttachRequest(String aliasName, ProcessVariableEventCallback eventCallback, ProcessVariableAttachCallback asyncCompletionCallback) {
			this.aliasName = aliasName;
			this.eventCallback = eventCallback;
			this.asyncCompletionCallback = asyncCompletionCallback;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			try
			{
				ProcessVariable pv = internalPVAttach(aliasName, eventCallback, asyncCompletionCallback);
				asyncCompletionCallback.processVariableAttachCompleted(pv);
			} catch (Throwable th) {
				// we failed, cancel
				asyncCompletionCallback.canceled();
			}
		}		
	
	}
	
	private class ExistanceCheckRequest implements Runnable
	{
		protected String aliasName;
		protected InetSocketAddress clientAddress;
		protected ProcessVariableExistanceCallback asyncCompletionCallback;

		/**
		 * @param aliasName
		 * @param clientAddress
		 * @param asyncCompletionCallback
		 */
		public ExistanceCheckRequest(String aliasName, InetSocketAddress clientAddress, ProcessVariableExistanceCallback asyncCompletionCallback) {
			this.aliasName = aliasName;
			this.clientAddress = clientAddress;
			this.asyncCompletionCallback = asyncCompletionCallback;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			try
			{
				ProcessVariableExistanceCompletion pvec = internalPVExistanceTest(aliasName, clientAddress, asyncCompletionCallback);
				asyncCompletionCallback.processVariableExistanceTestCompleted(pvec);
			} catch (Throwable th) {
				// we failed, cancel
				asyncCompletionCallback.canceled();
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
	 * @param async	use async completion
	 * @param asyncProcessTimeMS	delay time when processing async is ms
	 * @param simulateException	simulate exception
	 */
	public TestServerImpl(boolean async, long asyncProcessTimeMS, boolean simulateException)
	{
		if (async)
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
	 * @see gov.aps.jca.cas.Server#processVariableAttach(java.lang.String, gov.aps.jca.cas.ProcessVariableEventCallback, gov.aps.jca.cas.ProcessVariableAttachCallback)
	 */
	public ProcessVariable processVariableAttach(String aliasName, ProcessVariableEventCallback eventCallback,
												 ProcessVariableAttachCallback asyncCompletionCallback)
		throws CAStatusException, IllegalArgumentException, IllegalStateException
	{
		if (executor != null)
		{
			// queue
			executor.schedule(new AttachRequest(aliasName, eventCallback, asyncCompletionCallback),
							  asyncProcessTimeMS, TimeUnit.MILLISECONDS);
			
			// indicate async completion
			return null;
		}
		else
			return internalPVAttach(aliasName, eventCallback, asyncCompletionCallback);
	}

	/**
	 * Internal implementation of the PV attach method.
	 */
	private ProcessVariable internalPVAttach(String aliasName,ProcessVariableEventCallback eventCallback,
			 								 ProcessVariableAttachCallback asyncCompletionCallback)
		throws CAStatusException, IllegalArgumentException, IllegalStateException
	{
		if (simulateException)
			throw new CAStatusException(CAStatus.DEFUNCT, "Simulated exception.");

		synchronized (pvs)
		{
			ProcessVariable pv = (ProcessVariable)pvs.get(aliasName);
			if (pv != null)
			{
				// set PV if not yet set
				if (pv.getEventCallback() == null)
					pv.setEventCallback(eventCallback);
				
				return pv;
			}
			else
				return null;
		}
	}

	/* (non-Javadoc)
	 * @see gov.aps.jca.cas.Server#processVariableExistanceTest(java.lang.String, java.net.InetSocketAddress, gov.aps.jca.cas.ProcessVariableExistanceCallback)
	 */
	public ProcessVariableExistanceCompletion processVariableExistanceTest(String aliasName, InetSocketAddress clientAddress,
																		   ProcessVariableExistanceCallback asyncCompletionCallback)
		throws CAException, IllegalArgumentException, IllegalStateException
	{
		if (executor != null)
		{
			// queue
			executor.schedule(new ExistanceCheckRequest(aliasName, clientAddress,asyncCompletionCallback),
							  asyncProcessTimeMS, TimeUnit.MILLISECONDS);
			
			// indicate async completion
			return ProcessVariableExistanceCompletion.ASYNC_COMPLETION;
		}
		else
			return internalPVExistanceTest(aliasName, clientAddress, asyncCompletionCallback);
	}
	
	/**
	 * Internal implementation of the PV existance method.
	 */ 
	public ProcessVariableExistanceCompletion internalPVExistanceTest(String aliasName, InetSocketAddress clientAddress,
																	  ProcessVariableExistanceCallback asyncCompletionCallback)
		throws CAException, IllegalArgumentException, IllegalStateException
	{
		if (simulateException)
			throw new CAStatusException(CAStatus.DEFUNCT, "Simulated exception.");

		synchronized (pvs)
		{
			return pvs.containsKey(aliasName) ?
					ProcessVariableExistanceCompletion.EXISTS_HERE :
					ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE;
		}
	}
}