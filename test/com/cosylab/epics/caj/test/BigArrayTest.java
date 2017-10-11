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
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBR_Byte;
import gov.aps.jca.dbr.DBR_TIME_Byte;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;

import com.cosylab.epics.caj.CAJChannel;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class BigArrayTest {

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
            
    		DBR dbr = channel.get();
    		context.pendIO(3.0);
    		//dbr.printInfo();
    		
    		DBR_Byte image = (DBR_Byte)dbr;
    		System.out.println(image.getByteValue().length);
for(int r = 0; r < 10; r++){
    		final int COUNT = 1000;
    		long t1 = System.currentTimeMillis();
    		for (int i = 0; i < COUNT; i++)
    		{
        		//channel.get();
                        // use preallocated DBR
    			((CAJChannel)channel).get(dbr, DBR_Byte.TYPE, dbr.getCount());
        		context.pendIO(3.0);
    		}
    		long t2 = System.currentTimeMillis();
    		System.out.println("Avg. freq.: " + (COUNT/((t2-t1)/1000.0)));
}
System.out.println();
    		
            
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
/*
	    // check command-line arguments
		if (args.length != 1) {
			System.out.println(
				"usage: java " + BigArrayTest.class.getName() + " <pvname>");
			System.exit(1);
		}
*/		
		// execute
		new BigArrayTest().execute("image");
	}
	
}
