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
import gov.aps.jca.Version;
import gov.aps.jca.event.*;


import java.io.*;

/**
 * The class representing a CA Server Context.
 * Context are created through the JCALibrary factory class.
 *
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id: ServerContext.java,v 1.5 2006-03-16 11:58:49 msekoranja Exp $
 * @see JCALibrary
 */
public abstract class ServerContext {
  
  /**
   * @return	version of the context implementation.
   */
  public abstract Version getVersion();

  /**
   * Set <code>Server</code> implemenation and initialzie server. This method is called immediately after instance construction (call of constructor).
   * @param server	<code>Server</code> implementation providing <code>ProcessVariable</code> access (existance test and attach).
   */
  public abstract void initialize(Server server) throws CAException, IllegalStateException;
 
  /**
   * Run server (process events).
   * @param	seconds	time in seconds the server will process events (method will block), if <code>0</code>
   * 				the method would block until <code>destory()</code> is called.
   * @throws IllegalStateException	if server is already destroyed.
   * @throws CAException
   */
  public abstract void run(int seconds) throws CAException, IllegalStateException;
  
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
   * Shutdown (stop executing run() method) of this Context.
   * After shutdown Context cannot be rerun again, destory() has to be called
   * to clear all used resources.
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public void shutdown() throws CAException, IllegalStateException;

  /**
   * Clear all resources attached to this Context
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  abstract public void destroy() throws CAException, IllegalStateException;

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
    out.println("CLASS   : "+getClass().getName());
    out.println("VERSION : "+getVersion());
  }

  /**
   * Dispose (destroy) server context.
   */
  public void dispose() {
    try {
      destroy();
    } catch(Throwable th) {
    	// noop
    }
  }
}


