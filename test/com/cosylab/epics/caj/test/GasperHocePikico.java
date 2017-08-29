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
import gov.aps.jca.event.AccessRightsEvent;
import gov.aps.jca.event.AccessRightsListener;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

import com.cosylab.epics.caj.CAJContext;

/**
 * Simple basic usage test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class GasperHocePikico {

	private class ConnectionListenerImpl implements ConnectionListener
	{
		public synchronized void connectionChanged(ConnectionEvent event) {
			System.out.println(((Channel)event.getSource()).getName() + ": isConnected = " + event.isConnected());
			if (event.isConnected())
				this.notifyAll();
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
        	try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
            // immediately print info
            if (event.getStatus() == CAStatus.NORMAL)
                 System.out.print("."); //event.getDBR().printInfo();
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

		context.initialize();
		
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
			//context.createChannel("msekoranjaHost:aiExample1", new ConnectionListenerImpl());
			//context.createChannel("msekoranjaHost:aiExample2", new ConnectionListenerImpl());
		    ConnectionListenerImpl cl = new ConnectionListenerImpl();
			Channel channel = context.createChannel(channelName, cl);

			channel.addAccessRightsListener(new AccessRightsListener() {
				public void accessRightsChanged(AccessRightsEvent event) {
					Channel ch = (Channel)event.getSource();
					System.out.println("[" + ch.getName() + "] ACL: " + 
							(event.getReadAccess() ? 'r' : '-') +
							(event.getWriteAccess() ? 'w' : '-'));
				}
			});
			
            System.out.println();

            synchronized (cl)
            {
            	while (channel.getConnectionState() != Channel.CONNECTED)
            		cl.wait();
            	
            	System.out.print("Creating monitor...");
                channel.addMonitor(Monitor.VALUE, new MonitorListenerImpl());
	            context.flushIO();
            	System.out.println(" done. ");
            }
            
            String[] unames = new String[] { "goodUser", "badUser" };
            int i = 0;
            while (true)
            {
            	try {
        			Thread.sleep(3000);
            	} catch (InterruptedException ie ) { break; }
            	
		        String userName = unames[(i++) % 2];
		        System.out.println("\nSetting user name to: " + userName);
		        ((CAJContext)context).modifyUserName(userName);
            }
            
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
				"usage: java " + GasperHocePikico.class.getName() + " <pvname>");
			System.exit(1);
		}
		
		// execute
		new GasperHocePikico().execute(args[0]);
	}
	
}
