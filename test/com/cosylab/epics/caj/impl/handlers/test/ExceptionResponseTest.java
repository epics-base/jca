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

package com.cosylab.epics.caj.impl.handlers.test;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;

import com.cosylab.epics.caj.CAJContext;

import junit.framework.TestCase;

/**
 * Exception response test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ExceptionResponseTest extends TestCase {

	private class GetListenerImpl implements GetListener
	{
	    public DBR value = null;
	    public CAStatus status = null;
	    
		/**
		 * @see gov.aps.jca.event.GetListener#getCompleted(gov.aps.jca.event#GetEvent)
		 */
		public synchronized void getCompleted(GetEvent ev) {
		    status = ev.getStatus();
		    value = ev.getDBR();
		    this.notifyAll();
		}
	}

	/**
	 * Context to be tested.
	 */
	private CAJContext context;
	
    /**
	 * Channel to be tested.
	 */
    private Channel channel;
    
	/**
	 * Constructor for ExceptionResponseTest.
	 * @param methodName
	 */
	public ExceptionResponseTest(String methodName) {
		super(methodName);
	}

	/**
	 * Exception test.
	 */
	public void testExceptionResponse() throws CAException, TimeoutException, InterruptedException {
		GetListenerImpl listener = new GetListenerImpl();
		channel.get(0xFFFF, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.GETFAIL, listener.status);
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		context = new CAJContext();
	    channel = context.createChannel("record1");
	    context.pendIO(5.0);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		if (!context.isDestroyed())
			context.destroy();
		context = null;
	}

	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(ExceptionResponseTest.class);
	}
	
}
