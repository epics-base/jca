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
import gov.aps.jca.CAStatusException;

import java.net.InetSocketAddress;

/**
 * CA Server interface.
 * User has to implement this interface to create <code>ServerContext</code>.
 * ServerContext are created through the JCALibrary factory class.
 *
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id: Server.java,v 1.3 2006-08-22 15:11:38 msekoranja Exp $
 * @see JCALibrary
 */
public interface Server {
  
  /**
   * This function is called by the server library when it needs to
   * determine if a named process variable (PV) exists (or could be created) in the
   * server tool.
   * The request is allowed to complete asynchronously.
   * 
   * The server tool is encouraged to accept multiple PV name aliases for the same PV here.
   *
   * NOTE: return <code>ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE<code> if too many simultaneous
   * asynchronous operations are pending against the server.
   * The client library will retry the request at some time in the future.
   * 
   * @param aliasName the process variable alias name.
   * @param clientAddress the client which requested test.
   * @param	asyncCompletionCallback if asynchronous completion is required method should return 
   * 								<code>ProcessVariableExistanceCompletion.ASYNC_COMPLETION</code> and call
   * 								<code>ProcessVariableExistanceCompletionCallback.processVariableExistanceTestCompleted()</code> method.
   * @return process variable existance status.
   * @throws java.lang.IllegalArgumentException is thrown if the process variable name is invalid.
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  public ProcessVariableExistanceCompletion processVariableExistanceTest(String aliasName, InetSocketAddress clientAddress, 
																		  ProcessVariableExistanceCallback asyncCompletionCallback)
  	throws CAException, IllegalArgumentException, IllegalStateException;

  /**
   * This function is called <b>every time</b> that a client attaches to the PV.
   * The name supplied here will be either a canonical PV name or an alias PV name.
   * The request is allowed to complete asynchronously.
   * 
   * It is a responsibility of the server tool to detect attempts by
   * the server library to attach to an existing PV. If the PV does not
   * exist then the server tool should create it. Otherwise, the server
   * tool typically will return a pointer to the preexisting PV.
   * 
   * The server tool is encouraged to accept multiple PV name aliases
   * for the same PV here.
   *  
   * In most situations the server tool should avoid PV duplication
   * by returning a pointer to an existing PV if the PV alias name
   * matches a preexisting PV's name or any of its aliases.
   * In certain specialized rare situations the server tool may choose
   * to create client private process variables that are not shared between
   * clients. In this situation there might be several process variables
   * with the same name. One for each client. For example, a server tool
   * might be written that provides access to archival storage of the
   * historic values of a set of process variables. Each client would
   * specify its date of interest by writing into a client private process
   * variable. Next the client would determine the current value of its
   * subset of named process variables at its privately specified date by
   * attaching to additional client private process variables.
   *
   * @param aliasName the process variable alias name.
   * @param eventCallback the process variable event callback (where PV reports value changes if <code>interest<code> is <code>true<code>).
   * @param	asyncCompletionCallback if asynchronous completion is required method should return 
   * 								<code>null</code> and call
   * 								<code>ProcessVariableExistanceCompletionCallback.processVariableAttachCompleted()</code> method.
   * @return requested <code>ProcessVariable</code> instance, <code>null</code> if operation is to be done asynchronously or
   * 		 exception is thrown in case of an error (non-existant PV, etc.).
   *
   * @throws CAStatusException is thrown if a Channel Access error occured while creating the channel.
   * @throws java.lang.IllegalArgumentException is thrown if the channel's name is null or empty.
   * @throws java.lang.IllegalStateException if the context has been destroyed.
   */
  public ProcessVariable processVariableAttach(String aliasName, ProcessVariableEventCallback eventCallback,
		  										ProcessVariableAttachCallback asyncCompletionCallback)
  	throws CAStatusException, IllegalArgumentException, IllegalStateException;

}


