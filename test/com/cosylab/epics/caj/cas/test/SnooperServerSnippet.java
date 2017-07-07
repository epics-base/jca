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

package com.cosylab.epics.caj.cas.test;


import gov.aps.jca.CAException;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableAttachCallback;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCompletion;
import gov.aps.jca.cas.Server;
import gov.aps.jca.cas.ServerContext;

import java.net.InetSocketAddress;

/**
 * Simple snooper server.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class SnooperServerSnippet {

	/**
	 * Implementation of the server.
	 * @author msekoranja
	 */
	class SnopperServerImpl implements Server
	{

		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.Server#processVariableAttach(java.lang.String, gov.aps.jca.cas.ProcessVariableEventCallback, gov.aps.jca.cas.ProcessVariableAttachCallback)
		 */
		public ProcessVariable processVariableAttach(String aliasName, ProcessVariableEventCallback eventCallback, ProcessVariableAttachCallback asyncCompletionCallback) throws CAStatusException, IllegalArgumentException, IllegalStateException {
			// since we do not host any PV, this will never be called
			throw new IllegalStateException("we do not host any PVs");
		}

		/* (non-Javadoc)
		 * @see gov.aps.jca.cas.Server#processVariableExistanceTest(java.lang.String, java.net.InetSocketAddress, gov.aps.jca.cas.ProcessVariableExistanceCallback)
		 */
		public ProcessVariableExistanceCompletion processVariableExistanceTest(String aliasName, InetSocketAddress clientAddress, ProcessVariableExistanceCallback asyncCompletionCallback) throws CAException, IllegalArgumentException, IllegalStateException {
			System.out.println("Request for '" + aliasName + "' from client " + clientAddress);
			return ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE;
		}
		
	}

	/**
     * JCA server context.
     */
    private ServerContext context = null;
    
    /**
     * Initialize JCA context.
     * @throws CAException	throws on any failure.
     */
    private void initialize() throws CAException {
        
		// Get the JCALibrary instance.
		JCALibrary jca = JCALibrary.getInstance();

		// Create server implmentation
		SnopperServerImpl server = new SnopperServerImpl();
		
		// Create a context with default configuration values.
		context = jca.createServerContext(JCALibrary.CHANNEL_ACCESS_SERVER_JAVA, server);

		// Display basic information about the context.
        System.out.println(context.getVersion().getVersionString());
        context.printInfo(); System.out.println();
    }
 
    /**
     * Destroy JCA server  context.
     */
    private void destroy() {
        
        try {

            // Destroy the context, check if never initialized.
            if (context != null)
                context.destroy();
            
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
    
	/**
	 * @param channelName
	 */
	public void execute() {

		try {
			
			// initialize context
			initialize();
		    
			System.out.println("Running server...");

			// run server 
			context.run(0);
			
			System.out.println("Done.");

		} catch (Throwable th) {
			th.printStackTrace();
		}
		finally {
		    // always finalize
		    destroy();
		}

	}
	
	
	/**
	 * Program entry point. 
	 * @param args	command-line arguments
	 */
	public static void main(String[] args) {
		// execute
		new SnooperServerSnippet().execute();
	}
	
}
