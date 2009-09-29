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


import com.cosylab.epics.caj.cas.util.DefaultServerImpl;
import com.cosylab.epics.caj.cas.util.MemoryProcessVariable;
import com.cosylab.epics.caj.cas.util.examples.CounterProcessVariable;

import gov.aps.jca.CAException;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.cas.ServerContext;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.DBR_Enum;
import gov.aps.jca.dbr.DBR_Int;

/**
 * Simple basic usage test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class BasicExample {

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
		DefaultServerImpl server = new DefaultServerImpl();
		
		// Create a context with default configuration values.
		context = jca.createServerContext(JCALibrary.CHANNEL_ACCESS_SERVER_JAVA, server);

		// Display basic information about the context.
        System.out.println(context.getVersion().getVersionString());
        context.printInfo(); System.out.println();

        // register process variables
		registerProcessVariables(server); 
    }

    /**
     * Register process variables.
     * @param server
     */
	private void registerProcessVariables(DefaultServerImpl server) {
		
		// simple in-memory PV
		server.createMemoryProcessVariable("TEST1", DBR_Int.TYPE, new int[] { 1, 2, 3 });

		// PV supporting all GR/CTRL info
		MemoryProcessVariable mpv = new MemoryProcessVariable("TEST", null, DBR_Double.TYPE, new double[] { 12.08 });
		
		mpv.setUpperDispLimit(new Double(10));
		mpv.setLowerDispLimit(new Double(-10));
		
		mpv.setUpperAlarmLimit(new Double(9));
		mpv.setLowerAlarmLimit(new Double(-9));

		mpv.setUpperCtrlLimit(new Double(8));
		mpv.setLowerCtrlLimit(new Double(-8));

		mpv.setUpperWarningLimit(new Double(7));
		mpv.setLowerWarningLimit(new Double(-7));

		mpv.setUnits("units");
		mpv.setPrecision((short)3);

		server.registerProcessVaribale(mpv);

		// enum in-memory PV
		MemoryProcessVariable enumPV = new MemoryProcessVariable("ENUM", null, DBR_Enum.TYPE, new short[] { 0 }) 
		{
			private final String[] labels =
				{ "zero", "one", "two", "three", "four", "five", "six", "seven" }; 
			/* (non-Javadoc)
			 * @see com.cosylab.epics.caj.cas.util.MemoryProcessVariable#getEnumLabels()
			 */
			public String[] getEnumLabels() {
				return labels;
			}
			
		};
		server.registerProcessVaribale(enumPV);

		// counter PV
		CounterProcessVariable counter = new CounterProcessVariable("COUNTER", null, -10, 10, 1, 100, -7, 7, -9, 9);
		server.registerProcessVaribale(counter);
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
		new BasicExample().execute();
	}
	
}
