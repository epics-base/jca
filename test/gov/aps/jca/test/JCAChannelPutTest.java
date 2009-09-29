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

package gov.aps.jca.test;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_STSACK_String;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;

import junit.framework.TestCase;

/**
 * Put tests, mainly tests DBREncoder.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class JCAChannelPutTest extends TestCase {

	private class PutListenerImpl implements PutListener
	{
	    public CAStatus status = null;
	    
		/**
		 * @see gov.aps.jca.event.PutListener#putCompleted(gov.aps.jca.event#PutEvent)
		 */
		public synchronized void putCompleted(PutEvent ev) {
		    status = ev.getStatus();
		    this.notifyAll();
		}
	}

	/**
	 * Context to be tested.
	 */
	private Context context;
	
    /**
	 * Channel to be tested.
	 */
    private Channel channel;
    
	/**
	 * Constructor for CAJChannelGetTest.
	 * @param methodName
	 */
	public JCAChannelPutTest(String methodName) {
		super(methodName);
	}

	/**
	 * Check set value.
	 * @param value
	 * @throws CAException
	 * @throws TimeoutException
	 */
	private void checkValue(double value) throws CAException, TimeoutException
	{
	    DBR dbr = channel.get();
	    context.pendIO(3.0);
	    assertEquals(value, ((double[])dbr.getValue())[0], 0.0001);
	}
	
	public void testBytesPut() throws CAException, TimeoutException, InterruptedException {
	    channel.put((byte)11);
	    context.flushIO();
	    checkValue(11);

	    channel.put(new byte[] { 12 });
	    context.flushIO();
	    checkValue(12);
	    
		PutListenerImpl listener = new PutListenerImpl();
		channel.put((byte)13, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(13);

		listener = new PutListenerImpl();
		channel.put(new byte[] { 14 }, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(14);
	}

	public void testDoublesPut() throws CAException, TimeoutException, InterruptedException {
	    channel.put((double)15.0);
	    context.flushIO();
	    checkValue(15.0);

	    channel.put(new double[] { 16.0 });
	    context.flushIO();
	    checkValue(16.0);
	    
		PutListenerImpl listener = new PutListenerImpl();
		channel.put((double)17.0, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(17.0);

		listener = new PutListenerImpl();
		channel.put(new double[] { 18.0 }, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(18.0);
	}

	public void testFloatsPut() throws CAException, TimeoutException, InterruptedException {
	    channel.put((float)19.0);
	    context.flushIO();
	    checkValue(19.0);

	    channel.put(new float[] { 19.0f });
	    context.flushIO();
	    checkValue(19.0);
	    
		PutListenerImpl listener = new PutListenerImpl();
		channel.put((float)20.0, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(20.0);

		listener = new PutListenerImpl();
		channel.put(new float[] { 21.0f }, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(21.0);
	}

	public void testIntsPut() throws CAException, TimeoutException, InterruptedException {
	    channel.put((int)22);
	    context.flushIO();
	    checkValue(22);

	    channel.put(new int[] { 23 });
	    context.flushIO();
	    checkValue(23);
	    
		PutListenerImpl listener = new PutListenerImpl();
		channel.put((int)24, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(24);

		listener = new PutListenerImpl();
		channel.put(new int[] { 25 }, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(25);
	}

	public void testShortsPut() throws CAException, TimeoutException, InterruptedException {
	    channel.put((short)26);
	    context.flushIO();
	    checkValue(26);

	    channel.put(new short[] { 27 });
	    context.flushIO();
	    checkValue(27);
	    
		PutListenerImpl listener = new PutListenerImpl();
		channel.put((short)28, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(28);

		listener = new PutListenerImpl();
		channel.put(new short[] { 29 }, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(29);
	}

	public void testStringsPut() throws CAException, TimeoutException, InterruptedException {
	    channel.put("30.1");
	    context.flushIO();
	    checkValue(30.1);

	    channel.put(new String[] { "31.2" });
	    context.flushIO();
	    checkValue(31.2);
	    
		PutListenerImpl listener = new PutListenerImpl();
		channel.put("33.3", listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(33.3);

		listener = new PutListenerImpl();
		channel.put(new String[] { "34.5" }, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(34.5);
	}

	public void testEnumsPut() throws CAException, TimeoutException, InterruptedException {
	    
	    channel.put(new short[] { 40 });
	    context.flushIO();
	    checkValue(40);
	    
		PutListenerImpl listener = new PutListenerImpl();
		channel.put(new short[] { 41 }, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkValue(41);
	}

	public void testACKTPut() throws CAException, TimeoutException, InterruptedException {
	    
	    channel.putACKT(true);
	    context.flushIO();

	    DBR_STSACK_String val = (DBR_STSACK_String)channel.get(DBRType.STSACK_STRING, 1);
	    context.pendIO(3.0);
	    assertEquals(true, val.getAckT());
	    
	    channel.putACKT(false);
	    context.flushIO();

	    val = (DBR_STSACK_String)channel.get(DBRType.STSACK_STRING, 1);
	    context.pendIO(3.0);
	    assertEquals(false, val.getAckT());

	    
		PutListenerImpl listener = new PutListenerImpl();
		channel.putACKT(true, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);

		val = (DBR_STSACK_String)channel.get(DBRType.STSACK_STRING, 1);
	    context.pendIO(3.0);
	    assertEquals(true, val.getAckT());
	}

	public void testACKSPut() throws CAException, TimeoutException, InterruptedException {
	    
	    channel.putACKS(Severity.MINOR_ALARM);
	    context.flushIO();

	    /*
	    DBR_STSACK_String val = (DBR_STSACK_String)channel.get(DBRType.STSACK_STRING, 1);
	    context.pendIO(3.0);
	    assertEquals(Severity.MINOR_ALARM, val.getAckS());
	    
	    channel.putACKS(Severity.MAJOR_ALARM);
	    context.flushIO();

	    val = (DBR_STSACK_String)channel.get(DBRType.STSACK_STRING, 1);
	    context.pendIO(3.0);
	    assertEquals(Severity.MAJOR_ALARM, val.getAckS());
		*/
	    
		PutListenerImpl listener = new PutListenerImpl();
	    channel.putACKS(Severity.MINOR_ALARM, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
		/*
		val = (DBR_STSACK_String)channel.get(DBRType.STSACK_STRING, 1);
	    context.pendIO(3.0);
	    assertEquals(Severity.MINOR_ALARM, val.getAckS());
	    */
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		context = JCALibrary.getInstance().createContext(JCATestHelper.getJNIImplementation());
	    channel = context.createChannel("record1");
	    context.pendIO(5.0);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		context.dispose();
		context = null;
	}

	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(JCAChannelPutTest.class);
	}
	
}
