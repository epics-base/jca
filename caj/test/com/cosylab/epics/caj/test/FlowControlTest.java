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

package com.cosylab.epics.caj.test;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

/**
 * Simple basic usage test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class FlowControlTest {

	/**
     * Implementation of monitor listener.
     */
    private static class MonitorListenerImpl implements MonitorListener {
        
        /**
         * @see gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.MonitorEvent)
         */
        public void monitorChanged(MonitorEvent event)
        {
        	try {
				Thread.sleep(5);
			} catch (InterruptedException e) { /* noop */ }
            // immediately print info
            if (event.getStatus() == CAStatus.NORMAL)
            {
            	//System.out.println(((Channel)event.getSource()).getName());
                //event.getDBR().printInfo();
            	//System.out.println();
            }
            else
                System.err.println("Monitor error: " + event.getStatus());
        }
    }
    
    /**
     * JCA context.
     */
    private Context context = null;
    
    /**
     * Initialize JCA context.
     * @throws CAException	throws on any failure.
     */
    private void initialize() throws CAException {
        
		// Get the JCALibrary instance.
		JCALibrary jca = JCALibrary.getInstance();

		// Create a context with default configuration values.
		context = jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA);

		// Display basic information about the context.
        System.out.println(context.getVersion().getVersionString());
        context.printInfo(); System.out.println();
    }

    /**
     * Destroy JCA context.
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
	 */
	public void execute() {

		try {
		    
		    // initialize context
		    initialize();

		    final int PHASE1_COUNT = 150;
		    final int PHASE2_COUNT = 199;
		    
			// Create the Channel to connect to the PV.
			Channel[] channels = new Channel[PHASE2_COUNT];
			for (int i = 0; i < PHASE1_COUNT; i++)
				channels[i] = context.createChannel("record" + i);

			// Send the request and wait 5.0 seconds for the channel to connect to the PV.
			try
			{
				context.pendIO(5.0);
			} catch(TimeoutException te) {
				for (int i = 0; i < PHASE1_COUNT; i++)
					if (channels[i].getConnectionState() != Channel.CONNECTED)
						System.err.println("Failed to connect to record" + i);
                throw te;				
			}
			
			System.out.println("All connected.");
			

            /********************************************************************/ 
            /***************************** Monitors *****************************/ 
            /********************************************************************/ 
            
            System.out.println("\n------------------------------------------------\n");
            System.out.println("Monitors...");
            System.out.println();

            // Create a monitors
			for (int i = 0; i < PHASE1_COUNT; i++)
				channels[i].addMonitor(channels[i].getFieldType(), 4, Monitor.VALUE, new MonitorListenerImpl());
				//channels[i].addMonitor(Monitor.VALUE, new MonitorListenerImpl());
            context.flushIO();

            // Sleep for 15 seconds (monitors will be printed out).
            Thread.sleep(15000);

            System.out.println("Going phase 2..");
            
            // go go go phase2
			for (int i = PHASE1_COUNT; i < PHASE2_COUNT; i++)
				channels[i] = context.createChannel("record" + i);

			// Send the request and wait 10.0 seconds for the channel to connect to the PV.
			try
			{
				context.pendIO(0.0);
			} catch(TimeoutException te) {
				for (int i = PHASE1_COUNT; i < PHASE2_COUNT; i++)
					if (channels[i].getConnectionState() != Channel.CONNECTED)
						System.err.println("Failed to connect to record" + i);
                throw te;				
			}
			
			System.out.println("Phase2: All connected.");

			// do some work for a few seconds
			Thread.sleep(5000);

			System.out.println("\n------------------------------------------------");
            
            /********************************************************************/ 
            /********************************************************************/ 
            /********************************************************************/ 

			// Flush all pending requests...
            context.flushIO();

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
		new FlowControlTest().execute();
	}
	
}
