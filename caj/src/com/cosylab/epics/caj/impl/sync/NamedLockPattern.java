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

package com.cosylab.epics.caj.impl.sync;

import java.util.HashMap;
import java.util.Map;

/**
 * Named lock implementation (I named it "Named-lock pattern").
 * <code>
 * // try to acquire named lock
 * boolean lockAcquired = namedLocker.acquireSynchronizationObject(namedObject, getLockTimeout());
 * if (lockAcquired) 
 * {
 * 		try
 *		{
 *		    // ... so sth here
 *		}
 *		finally
 *		{
 *			namedLocker.releaseSynchronizationObject(name);	
 *		}
 *	}
 *	else
 *	{
 *		// .. failed to obtain synchronization lock for component 'namedObject', possible deadlock
 *	}
 * </code>
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class NamedLockPattern {

	/**
	 * Map of (named) locks.
	 */
	private Map namedLocks;

	/**
	 * Default constructor. 
	 */
	public NamedLockPattern() {
		namedLocks = new HashMap();
	}

	/**
	 * Acquire synchronization lock for named object.
	 * @param	name	name of the object whose lock to acquire.
	 * @param	msec	the number of milleseconds to wait.
	 * 					An argument less than or equal to zero means not to wait at all.
	 * @return	<code>true</code> if acquired, <code>false</code> othwerwise.
	 */
	public boolean acquireSynchronizationObject(Object name, long msec)
	{
		ReferenceCountingLock lock;
		
		synchronized (namedLocks)
		{
			// get synchronization object
			lock = (ReferenceCountingLock)namedLocks.get(name);
		
			// none is found, create and return new one
			// increment references
			if (lock == null)
			{
				lock = new ReferenceCountingLock();
				namedLocks.put(name, lock);
			}
			else
				lock.increment();
		}
		
		boolean success = lock.acquire(msec);
		
		if (!success)
			releaseSynchronizationObject(name, false);
		
		return success;
	}

	/**
	 * Release synchronization lock for named object.
	 * @param	name	name of the object whose lock to release.
	 */
	public void releaseSynchronizationObject(Object name)
	{
		releaseSynchronizationObject(name, true);
	}

	/**
	 * Release synchronization lock for named object.
	 * @param	name	name of the object whose lock to release.
	 * @param	release	set to <code>false</code> if there is no need to call release
	 * 					on synchronization lock.
	 */
	public void releaseSynchronizationObject(Object name, boolean release)
	{
		synchronized (namedLocks)
		{
			// get synchronization object
			ReferenceCountingLock lock = (ReferenceCountingLock)namedLocks.get(name);
		
			// release lock
			if (lock != null)
			{
				// if there only one current lock exists
				// remove it from the map
				if (lock.decrement() <= 0)
					namedLocks.remove(name);
					
				// release the lock
				if (release)
					lock.release();
			}
		}
	}

}
