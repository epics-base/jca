/*
 * Copyright (c) 2006 by Cosylab
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

package gov.aps.jca.cas;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.dbr.DBR;

import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

/**
 * Server channel (client connection to process variable).
 * This (default) implementation grants all access rights.
 * 
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id: ServerChannel.java,v 1.8 2006-08-28 12:28:32 msekoranja Exp $
 */
public class ServerChannel {

	/**
	 * Process variable.
	 */
	protected ProcessVariable processVariable;

	/**
	 * Channel SID.
	 */
	protected int sid;

	/**
	 * Channel CID.
	 */
	protected int cid;

	/**
	 * Monitors.
	 */
	protected Map monitors;

	/**
	 * Destroy state.
	 */
	protected boolean destroyed = false;

	/**
	 * Create server channel for given process variable.
	 * @param processVariable	process variable.
	 * @param cid channel CID.
	 * @param sid channel SID.
	 * @param userName	client user name.
	 * @param hostName  client host name.
	 */
	public ServerChannel(ProcessVariable processVariable, 
						 int cid, int sid,
						 String userName, String hostName)
	{
		if (processVariable == null)
			throw new IllegalArgumentException("non null process variable expected.");

		this.cid = cid;
		this.sid = sid;
		this.processVariable = processVariable;

		this.monitors = new TreeMap();
		
		// register to the PV
		processVariable.registerChannel(this);
	}
	
	/**
	 * Get process variable.
	 * @return process variable.
	 */
	public ProcessVariable getProcessVariable()
	{
		return processVariable;
	}
	
	/**
	 * Get channel CID.
	 * @return channel CID.
	 */
	public int getCID() {
		return cid;
	}

	/**
	 * Get channel SID.
	 * @return channel SID.
	 */
	public int getSID() {
		return sid;
	}

	/**
	 * Read process variable value.
	 * Default implementation calls <code>ProcessVariable.read()</code> method.
	 * @see ProcessVaribale.read()
	 */
    public CAStatus read(DBR value, ProcessVariableReadCallback asyncReadCallback) throws CAException
    {
    	return processVariable.read(value, asyncReadCallback);
    }
	    
	/**
	 * Write process variable value.
	 * Default implementation calls <code>ProcessVariable.write()</code> method.
	 * @see ProcessVaribale.write()
	 */
	public CAStatus write (DBR value, ProcessVariableWriteCallback asyncWriteCallback) throws CAException
	{
		return processVariable.write(value, asyncWriteCallback);
	}

    /**
     * Check if read access is granted.
     * @return <code>true</code> (default) if read access is granted, <code>false</code> otherwise.
     */
    public boolean readAccess()
    {
    	return true;
    }
    
    /**
     * Check if write access is granted.
     * @return <code>true</code> (default) if write access is granted, <code>false</code> otherwise.
     */
    public boolean writeAccess()
    {
    	return true;
    }

    /**
     * Register monitor.
     * @param monitor monitor to be registered.
     */
    public void registerMonitor(ServerMonitor monitor)
    {
    	synchronized (monitors) {
			monitors.put(new Integer(monitor.getIOID()), monitor);
		}
    }
    
    /**
     * Unregister monitor.
     * @param monitor monitor to be unregistered.
     */
    public void unregisterMonitor(ServerMonitor monitor)
    {
    	synchronized (monitors) {
			monitors.remove(new Integer(monitor.getIOID()));
		}
    }

    /**
     * Get monitor by its IOID.
     * @param ioid	monitor IOID
     * @return monitor with given IOID, <code>null</code> if there is no monitor with such IOID.
     */
    public ServerMonitor getMonitor(int ioid)
    {
    	synchronized (monitors) {
			return (ServerMonitor)monitors.get(new Integer(ioid));
		}
    }

    /**
     * Destroy all registered monitors.
     */
    protected void destroyAllMonitors()
    {
    	ServerMonitor[] sms;
    	synchronized (monitors) {
    		
    		// resource allocation optimization
    		if (monitors.size() == 0)
    			return;

    		sms = new ServerMonitor[monitors.size()];
    		monitors.values().toArray(sms);
		}
    	
    	for (int i = 0; i < sms.length; i++)
    		sms[i].destroy();
    }

    /**
	 * Destory server channel.
	 * This method MUST BE called if this it is overriden.
	 */ 
	public synchronized void destroy()
	{ 
		if (destroyed)
			return;
		destroyed = true;
		
		// destroy all monitors
		destroyAllMonitors();
		
		// unregister from the PV
		processVariable.unregisterChannel(this);
	}

    /**
	 * Prints detailed information about the process variable to the standard output
	 * stream.
	 * 
	 * @throws java.lang.IllegalStateException
	 *             if the context has been destroyed.
	 */
	 public void printInfo() throws IllegalStateException
	 {
		 printInfo(System.out);
	 }
	
	 /**
 	  * Prints detailed information about the process variable to the specified output
	  * stream.
	  * 
	  * @param out
	  *            the output stream.
	  * @throws java.lang.IllegalStateException
	  *             if the context has been destroyed.
	  */
	  public void printInfo(PrintStream out) throws IllegalStateException
	  {
		  out.println("CLASS        : " + getClass().getName());
		  out.println("PV           : " + processVariable);
		  out.println("READ ACCESS  : " + readAccess());
		  out.println("WRITE ACCESS : " + writeAccess());
	  }


}


