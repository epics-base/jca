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
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;

import java.io.PrintStream;

/**
 * Process variable (PV).
 * 
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id: ProcessVariable.java,v 1.12 2007-12-04 12:57:40 msekoranja Exp $
 */
public abstract class ProcessVariable {

	/**
	 * Process variable name.
	 */
	protected String name;

	/**
	 * Process variable event callback.
	 */
	protected ProcessVariableEventCallback eventCallback;

	/**
	 * Interess flag.
	 * 
	 * @see interestRegister()
	 */
	protected volatile boolean interest = false;

	/**
	 * Channel count (number of clients).
	 */
	protected int channelCount = 0;

	/**
	 * Alarm Acknowledge Severity.
	 */
	protected volatile Severity ackS = Severity.NO_ALARM;
	
	/**
	 * Alarm Acknowledge Transient.
	 */
	protected volatile boolean ackT = true;
	
	/**
	 * Create process variable with given name.
	 * 
	 * @param name process variable name.
	 * @param eventCallback	event callback, can be <code>null</code>.
	 */
	public ProcessVariable(String name, ProcessVariableEventCallback eventCallback)
	{
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("non empty name expected.");

		/*
		if (eventCallback == null)
			throw new IllegalArgumentException("non null event callback expected.");
		*/
		
		this.name = name;
		this.eventCallback = eventCallback;
	}
	
	/**
	 * Get process variable name. If there are several aliases for the same PV
	 * this routine should return the canonical (base) name for the PV.
	 * 
	 * @return process variable name.
	 */
	public String getName()
	{
		return name;
	}
    
    /**
	 * The best type for clients to use when accessing the value of the PV. Only
	 * basic types e.g. BYTE, INTEGER and not STS_BYTE have to be returned.
	 * 
	 * @return best type for clients.
	 */
	public abstract DBRType getType();
    
	/**
	 * Returns the maximum bounding box for all present and future data stored
	 * within the PV.
	 * 
	 * The method <code>getDimensionCount()</code> returns the maximum number
	 * of dimensions in the hypercube (0=scalar, 1=array, 2=plane, 3=cube ...).
	 * 
	 * The default (base) "getDimensionCount()" returns zero (scalar).
	 * 
	 * Clients will see that the PV's data is scalar if these routines are not
	 * supplied in the derived class.
	 * 
	 * If the "dimension" argument to getDimensionSize() is set to zero then the
	 * bound on the first dimension is being fetched. If the "dimension"
	 * argument to getDimensionSize() is set to one then the bound on the second
	 * dimension are being fetched...
	 * 
	 * @return number of dimensions presenting the data.
	 */
    public int getMaxDimension ()
    {
    	return 0;
    }

    /**
	 * The method <code>getDimensionSize(dimension)</code> returns the maximum
	 * number of elements in a particular dimension of the hypercube as follows:
	 * 
	 * <pre>
	 *   scalar - getDimensionCount() returns 0
	 *   
	 *   array -  getDimensionCount() returns 1
	 *            getDimensionSize(0) supplies number of elements in array
	 *            
	 *   plane -  getDimensionCount() returns 2
	 *            getDimensionSize(0) supplies number of elements in X dimension
	 *            getDimensionSize(1) supplies number of elements in Y dimension
	 *           
	 *   cube -   getDimensionCount() returns 3
	 *            getDimensionSize(0) supplies number of elements in X dimension
	 *            getDimensionSize(1) supplies number of elements in Y dimension
	 *            getDimensionSize(2) supplies number of elements in Z dimension
	 *            
	 *   ...
	 * </pre>
	 * 
	 * The default (base) "getDimensionSize()" returns one (scalar bounds) for
	 * all dimensions.
	 * 
	 * @param dimension
	 *            dimension for which to fecth its size.
	 * @return requested dimenstion size.
	 */
    public int getDimensionSize (int dimension)
    {
    	return 1;
    }
   
    /**
     * Get enum string labels.
     * If process variable is type of <code>ENUM</code> it should
     * override this method and return labels.
     * @return enum string labels.
     */
    public String[] getEnumLabels() {
    	return null;
    }
    
	/**
	 * Get acknowledged alarm severity.
	 * @return acknowledged alarm severity.
	 */
	public Severity getAckS() {
		return ackS;
	}

	/**
	 * Set acknowledged alarm severity.
	 * @param ackS acknowledged alarm severity to set.
	 */
	public void setAckS(Severity ackS) {
		this.ackS = ackS;
	}

	/**
	 * Get acknowledged alarm transient flag.
	 * @return acknowledged alarm transient flag
	 */
	public boolean isAckT() {
		return ackT;
	}

	/**
	 * Set acknowledged alarm transient flag.
	 * @param ackT acknowledged alarm transient flag to set
	 */
	public void setAckT(boolean ackT) {
		this.ackT = ackT;
	}

	/**
	 * Called by the server libary each time that it wishes to subscribe for PV
	 * change notification from the server tool via <code>ProcessVariableEventCallback.postEvent()</code> method.
	 */
	public void interestRegister() {
		interest = true;
	}
	
	/**
	 * Called by the server library each time that it wishes to remove its
	 * subscription for PV value change events from the server tool via
	 * <code>ProcessVariableEventCallback.postEvent()</code> method.
	 */
	public void interestDelete() {
		interest = false;
	}
	
	/**
	 * Read process variable value. The request is allowed to complete
	 * asynchronously.
	 * <p>
	 * The incoming DBR value is always (at least) of type DBR_TIME_[type] where type
	 * is of <code>getType()</code>. If DBR_GR_<type> or DBR_CTRL_<type> is requested
	 * then <code>value</code> is instance of requested type.
	 * </p>
	 * <p>
	 * Special cases: <br/>
	 * <code>DBR_TIME_Enum</code> type is "upgraded" to an "artificial" <code>TIME</code> type <code>DBR_TIME_LABELS_Enum</code>.
	 * This enables PV to set labels and time. <br/>
	 * 
	 * <code>DBR_TIME_Double</code> and <code>DBR_TIME_Float</code> are "upgraded" to an "artificial" <code>TIME</code>
	 * type which also implements <code>PRECISION</code> interface, <code>DBR_PRECISION_Double</code> and <code>DBR_PRECISION_Float</code>. 
	 * This enables PV to set precision (-1, by default, means no precision set).
	 * </p>
	 * 
	 * @param value
	 *            value of type (at least) DBR_TIME_<type> where type is of
	 *            <code>getType()</code>.
     * @param asyncReadCallback if asynchronous completion is required method should return 
     * 								<code>null</code> and call
     * 								<code>ProcessVariableReadCallback.processVariableReadCompleted()</code> method.
	 * @return CA status, <code>null</code> if request will complete
	 *         asynchronously by calling method.
	 */
    public abstract CAStatus read(DBR value, ProcessVariableReadCallback asyncReadCallback) throws CAException;
	    
	/**
	 * Write process variable value. The request is allowed to complete
	 * asynchronously.
	 * 
	 * The incoming DBR is always of type <code>getType()</code>.
	 * 
	 * @param value
	 *            value of type <code>getType()</code>.
     * @param asyncWriteCallback if asynchronous completion is required method should return 
     * 								<code>null</code> and call
     * 								<code>ProcessVariableWriteCallback.processVariableWriteCompleted()</code> method.
	 * @return CA status, <code>null</code> if request will complete
	 *         asynchronously by calling method.
	 */
	public abstract CAStatus write (DBR value, ProcessVariableWriteCallback asyncWriteCallback) throws CAException;
	
	/**
	 * This method is called each time that a PV is attached to by a client.
	 * This method can be overriden to specialize access to PV (e.g. for
	 * particular client, setting access rights, ...).
	 * 
	 * @param cid channel CID.
	 * @param sid channel SID.
	 * @param userName	client user name.
	 * @param hostName  client host name.
	 * @return <code>ServerChannel</code> instance.
	 */
	public ServerChannel createChannel (int cid, int sid,
										String userName, String hostName) throws CAException
	{
		return new ServerChannel(this, cid, sid, userName, hostName);
	}
	
	/**
	 * Register channel to this process variable.
	 * @param channel	channel to register.
	 */
	public synchronized void registerChannel(ServerChannel channel) 
	{
		channelCount++;
		// TODO revival?
	}
	
	/**
	 * Unregister channel from this process variable.
	 * @param channel	channel to unregister.
	 */
	public synchronized void unregisterChannel(ServerChannel channel) 
	{
		channelCount--;
		if (channelCount == 0)
			destroy();
	}

	/**
	 * Destory process variable.
	 * 
	 * This method is called: <br/>
	 * - each time that a PV transitions from a situation where clients
	 * are attached to a situation where no clients are attached.<br/>
	 * - once for all PVs that exist when the server is deleted
	 */ 
	// TODO multiple calls of destroy
	public void destroy() { /* noop */ }

    /**
	 * Prints detailed information about the Context to the standard output
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
 	  * Prints detailed information about the Context to the specified output
	  * stream.
	  * 
	  * @param out
	  *            the output stream.
	  * @throws java.lang.IllegalStateException
	  *             if the context has been destroyed.
	  */
	  public void printInfo(PrintStream out) throws IllegalStateException
	  {
		  out.println("NAME  : " + getName());
	  }

	/**
	 * Get event callback.
	 * @return the eventCallback
	 */
	public ProcessVariableEventCallback getEventCallback() {
		return eventCallback;
	}

	/**
	 * Set event callback.
	 * @param eventCallback the eventCallback to set
	 */
	public void setEventCallback(ProcessVariableEventCallback eventCallback) {
		this.eventCallback = eventCallback;
	}

}


