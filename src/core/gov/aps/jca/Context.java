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
 * $Id: Context.java,v 1.5 2006-10-26 12:56:39 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */


package gov.aps.jca;

import gov.aps.jca.event.*;


import java.io.*;

/**
 * The class representing a CA Context.
 * A Context controls all IO operations and the circuits thru which Channels will be created and connected.
 * Since CA 3.14 an application can create more than one context, thus ehancing IO control and Channels managment.
 * Context are created thru the JCALibrary factory class.
 *
 * @see JCALibrary
 */
public abstract class Context {
  
  /**
   * @return	version of the context implementation.
   */
  public abstract Version getVersion();
  
  public void initialize() throws CAException {
  }
  
  /**
   * @return an array containing all ContextMessageListeners attached to this context.
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public ContextMessageListener[] getContextMessageListeners() throws IllegalStateException;

  /**
   * Add a ContextMessageListener to this context.
   * @param l the listener to add
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public void addContextMessageListener(ContextMessageListener l) throws CAException, IllegalStateException;

  /**
   * Removes a ContextMessageListener from this context.
   * @param l the listener to remove
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public void removeContextMessageListener(ContextMessageListener l) throws CAException, IllegalStateException;



  /**
   * @return an array containing all ContextExceptionListeners attached to this context.
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public ContextExceptionListener[] getContextExceptionListeners() throws IllegalStateException;

  /**
   * Add a ContextExceptionListener to this context.
   * @param l the listener to add
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public void addContextExceptionListener(ContextExceptionListener l) throws CAException, IllegalStateException;

  /**
   * Removes a ContextExceptionListener from this context.
   * @param l the listener to remove
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public void removeContextExceptionListener(ContextExceptionListener l) throws CAException, IllegalStateException;



  /**
   * Clear all ressources attached to this Context
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public void destroy() throws CAException, IllegalStateException;

  /**
   * Create a new Channel using this context to access a process variable.
   *
   * @param name the process variable to connect to.
   * @return the new Channel.
   *
   * @throws CAException is thrown if a Channel Access error occured while creating the channel.
   * @throws java.lang.IllegalArgumentException is thrown if the channel's name is null or empty.
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  public Channel createChannel(String name) throws CAException, IllegalArgumentException, IllegalStateException {
    return createChannel(name, null, Channel.PRIORITY_DEFAULT);
  }

  /**
   * Create a new Channel using this context to access a process variable.
   *
   * @param name the process variable to connect to.
   * @param priority channel process priority
   * @return the new Channel.
   *
   * @throws CAException is thrown if a Channel Access error occured while creating the channel.
   * @throws java.lang.IllegalArgumentException is thrown if the channel's name is null or empty.
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  public Channel createChannel(String name, short priority) throws CAException, IllegalArgumentException, IllegalStateException {
    return createChannel(name, null, priority);
  }

  /**
   * Create a new Channel using this context to access a process variable with a ConnectionListener
   * to receive ConnectionEvent from the newly created Channel.
   *
   * @param name the process variable to connect to.
   * @param l the ConnectionListener.
   * @param priority channel process priority
   * @return the new Channel.
   * @throws CAException is thrown if a Channel Access error occured while creating the channel.
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  public Channel createChannel(String name, ConnectionListener l) throws CAException, IllegalStateException {
	    return createChannel(name, l, Channel.PRIORITY_DEFAULT);
  }

  /**
   * Create a new Channel using this context to access a process variable with a ConnectionListener
   * to receive ConnectionEvent from the newly created Channel.
   *
   * @param name the process variable to connect to.
   * @param l the ConnectionListener.
   * @return the new Channel.
   * @throws CAException is thrown if a Channel Access error occured while creating the channel.
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public Channel createChannel(String name, ConnectionListener l, short priority) throws CAException, IllegalStateException;

  /**
   * Get all channels of this context.
   * @return array of channels of this context.
   */
  abstract public Channel[] getChannels();
  
  /**
   * Process all pending IO operations.
   *
   * @param timeout the timeout limit.
   * @throws TimeoutException if all the IO couldn't be processed.
   * @throws CAException if a Channel Exception error occured while processing IOs.
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public void pendIO(double timeout) throws TimeoutException, CAException, IllegalStateException;

  /**
   * Tests if all synchronous (ie not specifying listeners) get and channel creation requests are completed.
   * It will only test request and channel created after the last pendIO() call.
   *
   * @return true if all pending IO request have been processed. False otherwise.
   *
   * @throws CAException if a Channel Access error occured while testing IOs
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public boolean testIO() throws CAException, IllegalStateException;

  /**
   * Process all pending events.
   *
   * @param time the time during which the Context will process events.
   * @throws CAException if a Channel Access error occured while pending events
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public void pendEvent(double time) throws CAException, IllegalStateException;

  /**
   * Flush the send buffer and process any outstanding CA background activity.
   *
   * @throws CAException if a Channel Access error occured while testing IOs
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public void poll() throws CAException, IllegalStateException;

  /**
   * Flush outstanding IO request to the servers.
   *
   * @throws CAException if a Channel Access error occured while flushing IOs
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public void flushIO() throws CAException, IllegalStateException;

  /**
   * Attach the calling thread to the list of threads allowed to access this context and all its channels.
   *
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   *                                         or if the context doesn't support multiple threads.
   */
  abstract public void attachCurrentThread() throws CAException, IllegalStateException;


  /**
   * Prints detailed information about the Context to the standard output stream.
   *
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  public void printInfo() throws IllegalStateException  {
    printInfo(System.out);
  }

  /**
   * Prints detailed information about the Context to the specified output stream.
   *
   * @param out the output stream.
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  public void printInfo(PrintStream out) throws IllegalStateException  {
    out.println("CLASS : "+getClass().getName());
  }


  public void dispose() {
    try {
      destroy();
    } catch(Throwable th) {
    }
  }
}


