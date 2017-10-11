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

package com.cosylab.epics.caj.impl.test;

import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.configuration.DefaultConfiguration;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class TestExample {

	private class PutListenerImpl implements PutListener
	{
			/**
		 * @see gov.aps.jca.event.PutListener#putCompleted(gov.aps.jca.event.PutEvent)
		 */
		public void putCompleted(PutEvent ev) {
			if (ev.getStatus() == CAStatus.NORMAL)
				System.out.println("Put done OK.");
			else
				System.out.println("Failed to put: " + ev.getStatus());
		}
	}

	private class GetListenerImpl implements GetListener
	{
		/**
		 * @see gov.aps.jca.event.GettListener#gettCompleted(gov.aps.jca.event#GetEvent)
		 */
		public void getCompleted(GetEvent ev) {
			if (ev.getStatus() == CAStatus.NORMAL)
			{
				System.out.println("Get done OK.");
				ev.getDBR().printInfo();
			}
			else
				System.out.println("Failed to get: " + ev.getStatus());
		}
	}

	public static void main(String[] args) throws Throwable {
		
		TestExample cct = new TestExample();
		
		try {
			// Get the JCALibrary instance.
			JCALibrary jca = JCALibrary.getInstance();

			// Create the configuration object for our context.
			DefaultConfiguration conf = new DefaultConfiguration("CAJContext");

			// Define the context class.
			conf.setAttribute("class", JCALibrary.CHANNEL_ACCESS_JAVA);
			//conf.setAttribute("class", JCALibrary.JNI_THREAD_SAFE);

			// Create a single threaded context with default configuration values.
			Context ctxt = jca.createContext(conf);

			// Display basic information about the context.
			ctxt.printInfo();

			System.out.println("=====================================");

			// connect requests are always issued immediately
			// w/o not related to pendIO at all; w/ callback yes...
			// (no pend_event needed)
			
			
			Channel channel = ctxt.createChannel("record1");
			ctxt.createChannel("record2");
			ctxt.createChannel("record3");
			ctxt.createChannel("stringTest");
			//Channel channel = ctxt.createChannel("stringTest");
						
			// wait until connected
			ctxt.pendIO(5.0);
			channel.printInfo();
			
			if (channel.getConnectionState() != Channel.CONNECTED)
			{
				ctxt.destroy();
				return;
			}

			System.out.println("=====================================");
			// get request w/o callbacks will wait until flush/pendIO is called
			DBR dbr = channel.get();
			ctxt.pendIO(0.0); 
			dbr.printInfo();

			System.out.println("=====================================");

			// get request w/ callbacks are always issued immediately
			// not related to pendIO at all, but require pend_event (to be flushed also)
			channel.get(cct.new GetListenerImpl());
			// flush & get event back
			ctxt.pendEvent(1.0);
			
			System.out.println("=====================================");

			// put request w/o callbacks will wait until flush is called
			channel.put(1.0);
			//channel.put("ej miki");
			ctxt.flushIO();
			System.out.println("OK");
						
			System.out.println("=====================================");
			
			// get request w/o callbacks will wait until flush/pendIO is called
			dbr = channel.get();
			ctxt.pendIO(0.0);
			dbr.printInfo();
			
			System.out.println("=====================================");

			// get request w/ callbacks are always issued immediately
			channel.put(321.0, cct.new PutListenerImpl());
			//channel.put("piki miki", cct.new PutListenerImpl());
			// flush & get event back
			ctxt.pendEvent(1.0);
			
			System.out.println("=====================================");

			// get request w/o callbacks will wait until flush/pendIO is called
			dbr = channel.get();
			ctxt.pendIO(0.0);
			dbr.printInfo();
			
			System.out.println("=====================================");


			Monitor monitor = channel.addMonitor(0x05, new MonitorListener(){
				/**
				 * @see gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.MonitorEvent)
				 */
				public void monitorChanged(MonitorEvent ev) {
					if (ev.getDBR() != null)
						ev.getDBR().printInfo();
				}
			});
			ctxt.flushIO();
			ctxt.pendEvent(10.0);
			
			monitor.clear();
			ctxt.flushIO();
			ctxt.pendEvent(3.0);

			// Destroy the context.
			ctxt.destroy();

		} catch (Throwable th) {
			th.printStackTrace();
		}

	}
}
