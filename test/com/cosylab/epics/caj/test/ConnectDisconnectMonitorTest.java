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
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connect/disconnect monitor test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ConnectDisconnectMonitorTest {

	/**
     * Implementation of monitor listener.
     */
    private static class MonitorListenerImpl implements MonitorListener {
        
    	public AtomicInteger count = new AtomicInteger();
    	
        /**
         * @see gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.MonitorEvent)
         */
        public void monitorChanged(MonitorEvent event)
        {
            // immediately print timestamp
            if (event.getStatus() == CAStatus.NORMAL)
            {
            	count.incrementAndGet();
            	System.out.println(((TIME)event.getDBR()).getTimeStamp().secPastEpoch());
            }
            else
                System.err.println("Monitor error: " + event.getStatus());
        }
    }
    
	/**
     * Implementation of connection listener.
     */
    private static class ConnectionListenerImpl implements ConnectionListener {

		public synchronized void connectionChanged(ConnectionEvent ev) {
			
			Channel channel = (Channel)ev.getSource();
			System.out.println(channel.getName() + " " + channel.getConnectionState().getName());
			
			this.notifyAll();
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
	public void execute(String channelName, String dummyChannelName) {

		try {
		    
		    // initialize context
		    initialize();

		    // another channel that holds TCP conneciton
		    if (dummyChannelName != null)
			/*Channel dummyChannel =*/ context.createChannel(dummyChannelName);
			context.pendIO(5.0);

		    while (true)
		    {
		
			    // Create the Channel to connect to the PV.
		    	ConnectionListener cl = new ConnectionListenerImpl();
				Channel channel = context.createChannel(channelName, cl);

				synchronized (cl) {
		    		while (channel.getConnectionState() != Channel.CONNECTED)
		    			cl.wait();
				}
	
	/*
				// If we're here, then everything went fine.
				// Display basic information about the channel.
				channel.printInfo();
	*/
	            // Create a monitor
				MonitorListenerImpl ml = new MonitorListenerImpl();
	            //Monitor monitor = 
	                channel.addMonitor(DBRType.TIME_DOUBLE, 1, Monitor.VALUE, ml);
	            context.flushIO();
	
	            // Sleep for 3 seconds (monitors will be printed out).
	            Thread.sleep(1500);
	            
	            // Clear the monitor
	            //monitor.clear();
	
	            if (ml.count.get() == 0)
	            {
	            	System.err.println("BUUUUGGGGG!!!!");
	            	System.exit(1);
	            }
	            
	            // Disconnect the channel.
	            channel.destroy();
	            
			}

			// Flush all pending requests...
            //context.flushIO();

			//System.out.println("Done.");

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
		if (args.length < 1) {
			System.out.println(
				"usage: java " + ConnectDisconnectMonitorTest.class.getName() + " <channel name> <dummy channel name>");
			System.exit(1);
		}
		
		// execute
		new ConnectDisconnectMonitorTest().execute(args[0], (args.length > 1) ? args[1] : null);
	}
	
}
