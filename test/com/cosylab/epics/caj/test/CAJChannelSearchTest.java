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
import gov.aps.jca.JCALibrary;
import gov.aps.jca.cas.ProcessVariableReadCallback;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.test.TestServerImpl;
import com.cosylab.epics.caj.cas.util.DefaultServerImpl;
import com.cosylab.epics.caj.cas.util.MemoryProcessVariable;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJChannelSearchTest extends TestCase {
    
    private class ConnectionListenerImpl implements ConnectionListener {
    	
    	final AtomicInteger connectedCount = new AtomicInteger(0);
    	
        /**
         * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
         */
        public synchronized void connectionChanged(ConnectionEvent ev)
        {
        	if (ev.isConnected())
        		connectedCount.incrementAndGet();
        	else
        		connectedCount.decrementAndGet();
            this.notifyAll();
        }
        
    }

    final int COUNT = 100000;
	final String CHANNEL_NAME_PREFIX = "channel";

	/**
	 * Context to be tested.
	 */
	private CAJServerContext context;
	
	/**
	 * Client cntext to be tested.
	 */
	private CAJContext clientContext;

	/**
	 * Server impl.
	 */
	private TestServerImpl serverImpl = null;
	
	/**
	 * Constructor for CAJChannelSearchTest.
	 * @param methodName
	 */
	public CAJChannelSearchTest(String methodName) {
		super(methodName);
	}
	
	public void testSearch() throws CAException {
	    
		System.out.println("Started issuing connect requests...");
		ConnectionListenerImpl cl = new ConnectionListenerImpl();
		for (int i = 0; i < COUNT; i++)
		{
			clientContext.createChannel(CHANNEL_NAME_PREFIX + String.valueOf(i), cl);
		}
		System.out.println("Connection events issued.");
		
		while (cl.connectedCount.get() < COUNT)
		{
			clientContext.pendEvent(1.0);
			System.out.println(cl.connectedCount.get());
		}
	}


	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		context = new CAJServerContext();
		clientContext = (CAJContext)JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
		DefaultServerImpl server = new DefaultServerImpl();

		for (int i = 0; i < COUNT; i++)
		{
			MemoryProcessVariable mpv = new MemoryProcessVariable(CHANNEL_NAME_PREFIX + String.valueOf(i), null, DBR_Double.TYPE, new double[] { 1.23 })
			{
				public synchronized CAStatus read(DBR value, ProcessVariableReadCallback asyncReadCallback) throws CAException {
					// set no alarm status
					((STS)value).setSeverity(Severity.NO_ALARM);
					((STS)value).setStatus(Status.NO_ALARM);
					
					return super.read(value, asyncReadCallback);
				}
				
			};
			server.registerProcessVaribale(mpv);
		}
		
		context.initialize(server);

		new Thread(new Runnable()
		{
			public void run() {
				try
				{
					context.run(0);
				} catch (Throwable th) {
					th.printStackTrace();
				}
			}
		}, this.getClass().getName()).start();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		if (clientContext != null && !clientContext.isDestroyed())
			clientContext.destroy();
		clientContext = null;

		if (context != null && !context.isDestroyed())
			context.destroy();
		context = null;

		if (serverImpl != null)
			serverImpl.destroy();
		serverImpl = null;
	}

	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(CAJChannelSearchTest.class);
	}
	
}
