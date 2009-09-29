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
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.STRING;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

/**
 * Simple basic usage test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class BasicExample {

    /**
     * Implementation of get listener.
     */
	private class GetListenerImpl implements GetListener
	{
	    private DBR value = null;
	    private CAStatus status = null;
	    
		/**
		 * @see gov.aps.jca.event.GetListener#getCompleted(gov.aps.jca.event#GetEvent)
		 */
		public synchronized void getCompleted(GetEvent ev) {
		    status = ev.getStatus();
		    value = ev.getDBR();
		    
		    // notify retrival
		    this.notifyAll();
		}
		
        public CAStatus getStatus() {
            return status;
        }
        
        public DBR getValue() {
            return value;
        }
	}

	/**
     * Implementation of monitor listener.
     */
    private static class MonitorListenerImpl implements MonitorListener {
        
        /**
         * @see gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.MonitorEvent)
         */
        public void monitorChanged(MonitorEvent event)
        {
            // immediately print info
            if (event.getStatus() == CAStatus.NORMAL)
                event.getDBR().printInfo();
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
	 * @param channelName
	 */
	public void execute(String channelName) {

		try {
		    
		    // initialize context
		    initialize();

			// Create the Channel to connect to the PV.
			Channel channel = context.createChannel(channelName);

			// Send the request and wait 5.0 seconds for the channel to connect to the PV.
			context.pendIO(5.0);

			// If we're here, then everything went fine.
			// Display basic information about the channel.
			channel.printInfo();

            /********************************************************************/ 
            /***************************** sync get *****************************/ 
            /********************************************************************/

            System.out.println("\n------------------------------------------------\n");
            System.out.println("Sync get:");
            
    		// get request w/o callbacks will wait until flush/pendIO is called
            // (channel default 'type' and 'count' is used)
    		DBR dbr = channel.get();
    		context.pendIO(3.0);
    		dbr.printInfo();

    		System.out.println();

    		dbr = channel.get(DBRType.STRING, 1);
    		context.pendIO(3.0);
    		String[] value = ((STRING)dbr).getStringValue();
    		System.out.println("Read string value: " + value[0]);
    		
            /********************************************************************/ 
            /**************************** async get *****************************/ 
            /********************************************************************/
    		
            System.out.println("\n------------------------------------------------\n");
            System.out.println("Async get:");

            // get request w/ callbacks are always issued immediately
    		// not related to pendIO at all, but require pend_event (to be flushed also)
    		GetListenerImpl listener = new GetListenerImpl();
    		channel.get(listener);
    		synchronized (listener)
    		{
    			// flush & get event back
    			context.flushIO();
    			// wait for response...
    			listener.wait(3000);
    		}
    		
    		if (listener.getStatus() == CAStatus.NORMAL)
    		    listener.getValue().printInfo();
    		else
    		    System.err.println("Get error: " + listener.getStatus());


            /********************************************************************/ 
            /***************************** Monitors *****************************/ 
            /********************************************************************/ 
            
            System.out.println("\n------------------------------------------------\n");
            System.out.println("Monitors:");
            System.out.println();

            // Create a monitor
            Monitor monitor = 
                channel.addMonitor(Monitor.VALUE, new MonitorListenerImpl());
            context.flushIO();

            // Sleep for 10 seconds (monitors will be printed out).
            Thread.sleep(10000);
            
            // Clear the monitor
            monitor.clear();

            System.out.println("\n------------------------------------------------");
            
            /********************************************************************/ 
            /********************************************************************/ 
            /********************************************************************/ 

            // Disconnect the channel.
			channel.destroy();

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

	    // check command-line arguments
		if (args.length != 1) {
			System.out.println(
				"usage: java " + BasicExample.class.getName() + " <pvname>");
			System.exit(1);
		}
		
		// execute
		new BasicExample().execute(args[0]);
	}
	
}
