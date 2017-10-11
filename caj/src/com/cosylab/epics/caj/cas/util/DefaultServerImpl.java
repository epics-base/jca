package com.cosylab.epics.caj.cas.util;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableAttachCallback;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCompletion;
import gov.aps.jca.cas.Server;
import gov.aps.jca.dbr.DBRType;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Default server implementation.
 * Implementation keeps a hash-map of all registered PVs.
 * When <code>processVariableExistanceTest</code> is called existance test is made on the hash-map.
 * <code>processVariableAttach</code> metod simply retrieves PV from the hash-map.
 */
public class DefaultServerImpl implements Server
{
	/**
	 * Map of PVs.
	 */
	protected Map pvs = new HashMap();
	
	/**
	 * Convenience method to create and register new in-memory process variables.
	 * @param name	process variable name.
	 * @param type	process variable native type.
	 * @param initialValue	process variable initial value (array expected). 
	 * @return created and registered process variable.
	 */
	public MemoryProcessVariable createMemoryProcessVariable(String name, DBRType type, Object initialValue)
	{
		MemoryProcessVariable memoryProcessVariable = new MemoryProcessVariable(name, null, type, initialValue);
		registerProcessVaribale(name, memoryProcessVariable);
		return memoryProcessVariable;
	}
	
	/**
	 * Register process variable.
	 * Note: if process variable with <code>aliasName</code> is already registered this will override it.
	 * @param aliasName process variable name.
	 * @param pv process variable instance.
	 */
	public void registerProcessVaribale(String aliasName, ProcessVariable pv)
	{
		synchronized (pvs)
		{
			pvs.put(aliasName, pv);
		}
	}
	
	/**
	 * Register process variable.
	 * Note: if process variable with the same name is already registered this will override it.
	 * @param pv process variable instance.
	 */
	public void registerProcessVaribale(ProcessVariable pv)
	{
		registerProcessVaribale(pv.getName(), pv);
	}

	/**
	 * Unregister process variable.
	 * @param aliasName	process variable name
	 * @return unregistered process variable, <code>null</code> if non unregistered.
	 */
	public ProcessVariable unregisterProcessVaribale(String aliasName)
	{
		synchronized (pvs)
		{
			return (ProcessVariable)pvs.remove(aliasName);
		}
	}

	/**
	 * @see gov.aps.jca.cas.Server#processVariableAttach(java.lang.String, gov.aps.jca.cas.ProcessVariableEventCallback, gov.aps.jca.cas.ProcessVariableAttachCallback)
	 */
	public ProcessVariable processVariableAttach(String aliasName, ProcessVariableEventCallback eventCallback,
												 ProcessVariableAttachCallback asyncCompletionCallback)
		throws CAStatusException, IllegalArgumentException, IllegalStateException
	{
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
				throw new CAStatusException(CAStatus.NOSUPPORT, "PV does not exist");
		}
	}

	/**
	 * @see gov.aps.jca.cas.Server#processVariableExistanceTest(java.lang.String, java.net.InetSocketAddress, gov.aps.jca.cas.ProcessVariableExistanceCallback)
	 */
	public ProcessVariableExistanceCompletion processVariableExistanceTest(String aliasName, InetSocketAddress clientAddress,
																		   ProcessVariableExistanceCallback asyncCompletionCallback)
		throws CAException, IllegalArgumentException, IllegalStateException
	{
		synchronized (pvs)
		{
			return pvs.containsKey(aliasName) ?
					ProcessVariableExistanceCompletion.EXISTS_HERE :
					ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE;
		}
	}
	
}