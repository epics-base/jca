/**********************************************************************
 *
 *      Original Author: Eric Boucher
 *      Date:            05/05/2003
 *
 *      Experimental Physics and Industrial Control System (EPICS)
 *
 *      Copyright 1991, the University of Chicago Board of Governors.
 *
 *      This software was produced under  U.S. Government contract
 *      W-31-109-ENG-38 at Argonne National Laboratory.
 *
 *      Beamline Controls & Data Acquisition Group
 *      Experimental Facilities Division
 *      Advanced Photon Source
 *      Argonne National Laboratory
 *
 *
 * $Id: Channel.java,v 1.6 2007-01-21 15:54:36 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */


package gov.aps.jca;


import gov.aps.jca.dbr.*;
import gov.aps.jca.event.*;

import java.io.PrintStream;

/** The class representing a CA Channel.
 * <P>
 * A Channel is a link between a client (the application) and a CA process variable located on a CA server.
 * All operations between the client and the process variable are handled by objects of this class.
 * </P>
 * <P> The following code shows how to synchronously create a channel.
 * </P>
 * <PRE>
 *  try {
 *    JCALibrary jca= JCALibrary.getInstance();
 *    Context ctxt= jca.createContext(JCALibrary.JNI_THREAD_SAFE);
 *    Channel ch= ctxt.createChannel("my.Channel");
 *    ctxt.pendIO(1.0);
 *    ... // If we're here, then the channel has been found and connected.
 *    ...
 *    ch.destroy();
 *    ctxt.destroy();
 *  } catch(CAException caEx) {
 *    System.err.println("A Error occured: "+caEx);
 *  }
 * </PRE>
 *
 * <P> The following code shows how to asynchronously create a channel. </P>
 * <PRE>
 *  try {
 *    JCALibrary jca= JCALibrary.getInstance();
 *    Context ctxt= jca.createContext(JCALibrary.JNI_THREAD_SAFE);
 *    Channel ch= ctxt.createChannel("my.Channel", new ConnectionListener() {
 *      public void connectionChanged(ConnectionEvent ev) {
 *        System.out.println("Channel is connected: "+ev.isConnected());
 *      });
 *    ctxt.flushIO();
 *    ... // We have no clue on whether the channel has been found and connected.
 *    ... // until the connection callback is called.
 *    ... //
 *    ch.destroy();
 *    ctxt.destroy();
 *  } catch(CAException caEx) {
 *    System.err.println("A Error occured: "+caEx);
 *  }
 * </PRE>
 * @see Context
 */
abstract public class Channel {
  
  /** Enumeration class representing the Channel's connection state.  */
  static public class ConnectionState extends gov.aps.jca.ValuedEnum {
    static private java.util.Map _map= new java.util.HashMap();
    static final public ConnectionState NEVER_CONNECTED= new ConnectionState("NEVER_CONNECTED", 0);
    static final public ConnectionState DISCONNECTED   = new ConnectionState("DISCONNECTED", 1);
    static final public ConnectionState CONNECTED      = new ConnectionState("CONNECTED", 2);
    static final public ConnectionState CLOSED         = new ConnectionState("CLOSED", 3);

    protected ConnectionState(String name, int value) {
      super(name,value,_map);
    }
    static public ConnectionState forValue(int value) {
      ConnectionState c;
      for(java.util.Iterator it= _map.values().iterator(); it.hasNext(); ) {
        c= (ConnectionState) it.next();
        if(c.getValue()==value) return c;
      }
      return null;
    }
    static public ConnectionState forName(String name) {
      return (ConnectionState)_map.get(name);
    }

  }

  /** Channel has never been connected. */  
  static final public ConnectionState NEVER_CONNECTED= ConnectionState.NEVER_CONNECTED;
  /** Channel is disconnected. */  
  static final public ConnectionState DISCONNECTED   = ConnectionState.DISCONNECTED;
  /** Channel is connected. */  
  static final public ConnectionState CONNECTED      = ConnectionState.CONNECTED;
  /** Channel has been closed and destroyed. */  
  static final public ConnectionState CLOSED         = ConnectionState.CLOSED;

  /** Minimal priority. */
  static final public short PRIORITY_MIN = 0;
  /** Maximal priority. */
  static final public short PRIORITY_MAX = 99;
  /** Default priority. */
  static final public short PRIORITY_DEFAULT = PRIORITY_MIN;
  /** DB links priority. */
  static final public short PRIORITY_LINKS_DB = PRIORITY_MAX;
  /** Archive priority. */
  static final public short PRIORITY_ARCHIVE = (PRIORITY_MAX + PRIORITY_MIN) / 2;
  /** OPI priority. */
  static final public short PRIORITY_OPI = PRIORITY_MIN;

  
  /** Returns the context which created this channel.
   * @return the context
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public Context getContext() throws IllegalStateException;

  /**
   * Clear the ressources used by this channel.
   * No further access should be made to a channel after it has been destroyed.
   *
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void destroy() throws CAException, IllegalStateException;

  /** Returns the ConnectionListeners registered with this channel.
   * @return an array containing the ConnectionListeners.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public ConnectionListener[] getConnectionListeners() throws IllegalStateException;

  /** Adds a ConnectionListener which will be notified of the connection state's changes of this Channel.
   * @param l the ConnectionListener to register.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void addConnectionListener(ConnectionListener l) throws CAException, IllegalStateException;

  /**
   * Removes a ConnectionListener .
   *
   * @param l the ConnectionListener to remove.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void removeConnectionListener(ConnectionListener l) throws CAException, IllegalStateException;


  /** Returns the AccessRightsListeners registered with this channel.
   * @return an array containing the AccessRightsListeners.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public AccessRightsListener[] getAccessRightsListeners() throws IllegalStateException;

  /**
   * Adds a AccessRightsListener which will be notified of the access rights's changes of this Channel.
   *
   * @param l the ConnectionListener to register.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void addAccessRightsListener(AccessRightsListener l) throws CAException, IllegalStateException;

  /**
   * Removes a AccessRightsListener which will be notified of the access rights's changes of this Channel.
   *
   * @param l the ConnectionListener to remove.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void removeAccessRightsListener(AccessRightsListener l) throws CAException, IllegalStateException;

  /** Returns the name of this channel.
   * @return the name of this Channel.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   **/
  abstract public String  getName() throws IllegalStateException;

  /**
   * Returns the DBR type of this Channel.
   *
   * @return the DBR type.
   * @see gov.aps.jca.dbr.DBR
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   **/
  abstract public DBRType getFieldType() throws IllegalStateException;

  /** Returns the element count of this channel.
   * @return the element count.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   **/
  abstract public int getElementCount() throws IllegalStateException;

  /** Returns the connection state of this channel.
   * @return the ConnectionState value.
   * @see ConnectionState
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   **/
  abstract public ConnectionState getConnectionState() throws IllegalStateException;

  /**
   * Returns the Channel's hostname.
   *
   * @return the hostname.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   **/
  abstract public String getHostName() throws IllegalStateException;

  /**
   * Returns whether read operations are allowed on this Channel.
   *
   * @return true is read operations are allowed, false otherwise.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   **/
  abstract public boolean getReadAccess() throws IllegalStateException;

  /**
   * Returns whether write operations are allowed on this Channel.
   *
   * @return true is write operations are allowed, false otherwise.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   **/
  abstract public boolean getWriteAccess() throws IllegalStateException;


  /**
   * Synchronously writes a value to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(byte value) throws CAException, IllegalStateException {
    put(new byte[] { value });
  }

  /**
   * Asynchronously writes a value to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(byte value, PutListener l) throws CAException, IllegalStateException  {
    put(new byte[] { value }, l);
  }

  /**
   * Synchronously writes a value to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(short value) throws CAException, IllegalStateException  {
    put(new short[] { value });
  }

  /**
   * Asynchronously writes a value to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(short value, PutListener l) throws CAException, IllegalStateException  {
    put(new short[] { value }, l);
  }

  /**
   * Synchronously writes a value to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(int value) throws CAException, IllegalStateException  {
    put(new int[] { value });
  }

  /**
   * Asynchronously writes a value to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(int value, PutListener l) throws CAException, IllegalStateException  {
    put(new int[] { value }, l);
  }

  /**
   * Synchronously writes a value to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(float value) throws CAException, IllegalStateException  {
    put(new float[] { value });
  }

  /**
   * Asynchronously writes a value to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(float value, PutListener l) throws CAException, IllegalStateException  {
    put(new float[] { value }, l);
  }

  /**
   * Synchronously writes a value to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(double value) throws CAException, IllegalStateException  {
    put(new double[] { value });
  }

  /**
   * Asynchronously writes a value to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(double value, PutListener l) throws CAException, IllegalStateException  {
    put(new double[] { value }, l);
  }

  /**
   * Synchronously writes a value to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(String value) throws CAException, IllegalStateException  {
    put(new String[] { value });
  }

  /**
   * Asynchronously writes a value to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void put(String value, PutListener l) throws CAException, IllegalStateException  {
    put(new String[] { value }, l);
  }




  /**
   * Synchronously writes an array to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(byte[] value) throws CAException, IllegalStateException ;
  /**
   * Asynchronously writes an array to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(byte[] value, PutListener l) throws CAException, IllegalStateException ;

  /**
   * Synchronously writes an array to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(short[] value) throws CAException, IllegalStateException ;
  /**
   * Asynchronously writes an array to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(short[] value, PutListener l) throws CAException, IllegalStateException ;

  /**
   * Synchronously writes an array to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(int[] value) throws CAException, IllegalStateException ;
  /**
   * Asynchronously writes an array to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(int[] value, PutListener l) throws CAException, IllegalStateException ;

  /**
   * Synchronously writes an array to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(float[] value) throws CAException, IllegalStateException ;
  /**
   * Asynchronously writes an array to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(float[] value, PutListener l) throws CAException, IllegalStateException ;

  /**
   * Synchronously writes an array to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(double[] value) throws CAException, IllegalStateException ;
  /**
   * Asynchronously writes an array to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(double[] value, PutListener l) throws CAException, IllegalStateException ;

  /**
   * Synchronously writes an array to this Channel.
   *
   * @param value the value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(String[] value) throws CAException, IllegalStateException ;

  /**
   * Asynchronously writes an array to this Channel.
   *
   * @param value the value.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void put(String[] value, PutListener l) throws CAException, IllegalStateException ;

  /**
   * Synchronously writes an Alarm Acknowledge Transient value to this Channel.
   *
   * @param value the value, <code>true</code> equals YES and <code>false</code> equals NO.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void putACKT(boolean value) throws CAException, IllegalStateException ;

  /**
   * Asynchronously writes an Alarm Acknowledge Transient value to this Channel.
   *
   * @param value the value, <code>true</code> equals YES and <code>false</code> equals NO.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void putACKT(boolean value, PutListener l) throws CAException, IllegalStateException ;

  /**
   * Synchronously writes an Alarm Acknowledge Severity value to this Channel.
   *
   * @param severity to acknowledge.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void putACKS(Severity severity) throws CAException, IllegalStateException ;

  /**
   * Asynchronously writes an Alarm Acknowledge Severity value to this Channel.
   *
   * @param severity to acknowledge.
   * @param l the PutListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void putACKS(Severity severity, PutListener l) throws CAException, IllegalStateException ;

  /**
   * Synchronously reads this Channel's value using the native DBR type and count.
   *
   * @return the channel's value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public DBR get() throws CAException, IllegalStateException  {
    return get(getFieldType(), getElementCount());
  }

  /**
   * Asynchronously reads this Channel's value using the native DBR type and count.
   *
   * @param l the GetListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void get(GetListener l) throws CAException, IllegalStateException  {
    get(getFieldType(), getElementCount(), l);
  }

  /**
   * Synchronously Reads a specified number elements from this Channel value using the native DBR type.
   *
   * @param count the number of element to read.
   * @return the channel's value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public DBR get(int count) throws CAException, IllegalStateException  {
    return get(getFieldType(), count);
  }

  /**
   * Asynchronously Reads a specified number from this Channel's value using the native DBR type.
   *
   * @param count the number of element to read.
   * @param l the GetListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void get(int count, GetListener l) throws CAException, IllegalStateException  {
    get(getFieldType(), count, l);
  }

  /**
   * Synchronously Reads a specified number of elements of a specified type from this Channel.
   *
   * @param type the DBR type to read.
   * @param count the number of element to read.
   * @return the channel's value.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public DBR  get(DBRType type, int count) throws CAException, IllegalStateException ;
  /**
   * Asynchronously reads a specified number of elements of a specified type from this Channel
   *
   * @param type the DBR type.
   * @param count the number of element to read.
   * @param l the GetListener to notify when the request has been completed.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  abstract public void get(DBRType type, int count, GetListener l) throws CAException, IllegalStateException ;


  /**
   * Add a monitor to this channel using the channel's native DBR type and count.
   *
   * @param mask the mask value indicating when the listener need to be notified.
   * @return the Monitor object representing this monitor.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public Monitor addMonitor(int mask) throws CAException, IllegalStateException  {
    return addMonitor(getFieldType(), getElementCount(), mask);
  }

  /**
   * Add a monitor to this channel using the channel's native DBR type and count.
   *
   * @param mask the mask value indicating when the listener need to be notified.
   * @param l a MonitorListener to be notified when the value/log/alarm changed.
   * @return the Monitor object representing this monitor.
   *
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   *
   * @see Monitor
   */
  public Monitor addMonitor(int mask, MonitorListener l) throws CAException, IllegalStateException  {
    return addMonitor(getFieldType(), getElementCount(), mask, l);
  }

  /**
   * Add a monitor to this channel.
   *
   * @param type the monitor's type.
   * @param count the monitor's element count.
   * @param mask the mask value indicating when the listener need to be notified.
   * @return the Monitor object representing this monitor.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public Monitor addMonitor(DBRType type, int count, int mask) throws CAException, IllegalStateException  {
    return addMonitor(type,count,mask,null);
  }

  /**
   * Adds a monitor to this Channel.
   *
   * @param type the monitor's type.
   * @param count the monitor's element count.
   * @param mask the mask value indicating when the listener need to be notified.
   * @param l a MonitorListener to be notified when the value/log/alarm changed.
   *
   * @return the Monitor object representing this monitor.
   * @throws CAException if a Channel Exception occured while performing this operation.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   *
   * @see Monitor
   */
  abstract public Monitor addMonitor(DBRType type, int count, int mask, MonitorListener l) throws CAException, IllegalStateException ;

  /**
   * Checks whether the size of this monitor is greater than maxArrayBytes,
   * and throws an exception if it is.
   * 
   * @param type type for the monitor
   * @param count size of the monitor
   * @param maxArrayBytes maximum size of the array
   */
  protected void checkMonitorSize(DBRType type, int count, int maxArrayBytes) {
      int bytesPerElement = 0;
      if (type.isBYTE()) {
          bytesPerElement = 1;
      } else if (type.isSHORT() || type.isENUM() || (type == DBRType.PUT_ACKT) || (type == DBRType.PUT_ACKS)) {
          bytesPerElement = 2;
      } else if (type.isINT() || type.isFLOAT()) {
          bytesPerElement = 4;
      } else if (type.isDOUBLE()) {
          bytesPerElement = 8;
      } else if (type.isSTRING()) {
          bytesPerElement = 40; // MAX_STRING_SIZE
      } else {
          throw new IllegalArgumentException("Unsupported data type: " + type);
      }

      if (bytesPerElement * count > maxArrayBytes)
          throw new IllegalArgumentException("Size of the monitor exceeds maxArrayBytes (" + count + " * " + bytesPerElement + " > " + maxArrayBytes + ")");
  }
  
  /**
   * Prints details information about this Channel to the standard output stream.
   * @throws java.lang.IllegalStateException if the channel is in no state to perform this operation (ie destroyed, etc...)
   */
  public void printInfo() throws IllegalStateException {
    printInfo(System.out);
  }

  /**
   * Prints details information about this Channel to the specified output stream.
   *
   * @param out the output stream.
   */
  public void printInfo(PrintStream out) throws IllegalStateException {
    out.println("CHANNEL  : "+getName());
    out.println("TYPE     : "+getFieldType());
    out.println("COUNT    : "+getElementCount());
    out.println("STATE    : "+getConnectionState());
    out.println("HOSTNAME : "+getHostName());
    out.println("READ     : "+getReadAccess());
    out.println("WRITE    : "+getWriteAccess());
  }


  public void dispose() {
    try {
      destroy();
    } catch(Throwable th) {
    }
  }
}




