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

package com.cosylab.epics.caj.cas;

import java.util.ArrayList;

import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.dbr.DBR;

/**
 * Event callback dispatcher (fan-out).
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
// TODO optimize
public class ProcessVariableEventDispatcher implements ProcessVariableEventCallback {

	/**
	 * PV to dispatch for.
	 */
	protected ProcessVariable processVariable;

	/**
	 * Empty array (performance opt.)
	 */
	private static final ProcessVariableEventCallback[] EMPTY_LIST = new ProcessVariableEventCallback[0];

	/**
	 * List of listeners.
	 */
	protected ArrayList listeners = new ArrayList();
	protected ProcessVariableEventCallback[] cachedList = EMPTY_LIST;
	
	/**
	 * Constructor.
	 * @param processVariable PV to dispatch for, can be <code>null</code>.
	 */
	public ProcessVariableEventDispatcher(ProcessVariable processVariable)
	{
		this.processVariable = processVariable;
	}
	
	/**
	 * Get served PV.
	 * @return the processVariable
	 */
	public ProcessVariable getProcessVariable() {
		return processVariable;
	}

	/**
	 * Set served PV (allow PV to be set later to avoid chicken-egg problem).
	 * @param processVariable the processVariable to set
	 */
	public void setProcessVariable(ProcessVariable processVariable) {
		if (this.processVariable != null)
			throw new IllegalStateException("PV already set");
		
		this.processVariable = processVariable;
	}

	/**
	 * @see gov.aps.jca.cas.ProcessVariableEventCallback#postEvent(int, gov.aps.jca.dbr.DBR)
	 */
	public void postEvent(int select, DBR event) {
		
		// dispatch
		synchronized (listeners)
		{
			final int size = cachedList.length; 
			for (int i = 0; i < size; i++) {
				try {
					cachedList[i].postEvent(select, event);
				} catch (Throwable th) {
					// print exception trace, do nothing
					th.printStackTrace();
				}
			}
		}
	}

	/**
	 * Register new listener.
	 * @param listener
	 */
	public void registerEventListener(ProcessVariableEventCallback listener)
	{
		synchronized (listeners)
		{
			listeners.add(listener);
			ProcessVariableEventCallback[] ncl = new ProcessVariableEventCallback[listeners.size()];
			listeners.toArray(ncl);
			cachedList = ncl;
			
			// notify PV about our interest
			if (listeners.size() == 1 && processVariable != null)
				processVariable.interestRegister();
		}
	}
	
	/**
	 * Unregister new listener.
	 * @param listener
	 */
	public void unregisterEventListener(ProcessVariableEventCallback listener)
	{
		synchronized (listeners)
		{
			boolean removed = listeners.remove(listener);
			if (removed) {
				ProcessVariableEventCallback[] ncl = new ProcessVariableEventCallback[listeners.size()];
				listeners.toArray(ncl);
				cachedList = ncl;

				// notify PV we are not interested anymore
				if (listeners.size() == 0 && processVariable != null)
					processVariable.interestDelete();
			}
		}
	}

	/**
	 * @see gov.aps.jca.cas.CompletionCallback#canceled()
	 */
	public void canceled() {
		// noop
	}

}
