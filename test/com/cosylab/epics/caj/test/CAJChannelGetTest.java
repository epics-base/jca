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

import java.io.IOException;
import java.lang.reflect.Array;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRFactory;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_Byte;
import gov.aps.jca.dbr.DBR_CTRL_Byte;
import gov.aps.jca.dbr.DBR_CTRL_Double;
import gov.aps.jca.dbr.DBR_CTRL_Float;
import gov.aps.jca.dbr.DBR_CTRL_Int;
import gov.aps.jca.dbr.DBR_CTRL_Short;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.DBR_Enum;
import gov.aps.jca.dbr.DBR_Float;
import gov.aps.jca.dbr.DBR_GR_Byte;
import gov.aps.jca.dbr.DBR_GR_Double;
import gov.aps.jca.dbr.DBR_GR_Float;
import gov.aps.jca.dbr.DBR_GR_Int;
import gov.aps.jca.dbr.DBR_GR_Short;
import gov.aps.jca.dbr.DBR_Int;
import gov.aps.jca.dbr.DBR_LABELS_Enum;
import gov.aps.jca.dbr.DBR_PutAckS;
import gov.aps.jca.dbr.DBR_PutAckT;
import gov.aps.jca.dbr.DBR_STS_Byte;
import gov.aps.jca.dbr.DBR_STS_Double;
import gov.aps.jca.dbr.DBR_STS_Enum;
import gov.aps.jca.dbr.DBR_STS_Float;
import gov.aps.jca.dbr.DBR_STS_Int;
import gov.aps.jca.dbr.DBR_STS_Short;
import gov.aps.jca.dbr.DBR_STS_String;
import gov.aps.jca.dbr.DBR_Short;
import gov.aps.jca.dbr.DBR_String;
import gov.aps.jca.dbr.DBR_TIME_Byte;
import gov.aps.jca.dbr.DBR_TIME_Double;
import gov.aps.jca.dbr.DBR_TIME_Enum;
import gov.aps.jca.dbr.DBR_TIME_Float;
import gov.aps.jca.dbr.DBR_TIME_Int;
import gov.aps.jca.dbr.DBR_TIME_Short;
import gov.aps.jca.dbr.DBR_TIME_String;
import gov.aps.jca.dbr.GR;
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.DBRDecoder;

import junit.framework.TestCase;

/**
 * Get tests, mainly tests DBRs and DBRDecoder.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CAJChannelGetTest extends TestCase {

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
	 * Constructor for CAJChannelGetTest.
	 * @param methodName
	 */
	public CAJChannelGetTest(String methodName) {
		super(methodName);
	}

	/**
	 * Simple get tests.
	 */
	public void testGet() throws CAException, TimeoutException, InterruptedException {
	    DBR dbr = channel.get();
	    context.pendIO(3.0);
	    assertEquals(channel.getFieldType(), dbr.getType());
	    assertEquals(channel.getElementCount(), dbr.getCount());
	    double[] retVal = (double[])dbr.getValue();
	    assertEquals(dbr.getCount(), retVal.length);
	    assertEquals(12.34, retVal[0], 0.0);
	    
		GetListenerImpl listener = new GetListenerImpl();
		channel.get(listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
		dbr = listener.value;
	    assertEquals(channel.getFieldType(), dbr.getType());
	    assertEquals(channel.getElementCount(), dbr.getCount());
	    retVal = (double[])dbr.getValue();
	    assertEquals(dbr.getCount(), retVal.length);
	    assertEquals(12.34, retVal[0], 0.0);
	}

	/**
	 * Simple get w/ count tests.
	 */
	public void testGetCount() throws CAException, TimeoutException, InterruptedException {
	    final int COUNT = 3;
	    
	    DBR dbr = channel.get(COUNT);
	    context.pendIO(3.0);
	    assertEquals(channel.getFieldType(), dbr.getType());
	    assertEquals(COUNT, dbr.getCount());
	    double[] retVal = (double[])dbr.getValue();
	    assertEquals(COUNT, retVal.length);
	    assertEquals(12.34, retVal[0], 0.0);
	    for (int i = 2; i < COUNT; i++)
		    assertEquals(0.0, retVal[i], 0.0);
	    
		GetListenerImpl listener = new GetListenerImpl();
		channel.get(COUNT, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
		dbr = listener.value;
	    assertEquals(channel.getFieldType(), dbr.getType());
	    assertEquals(COUNT, dbr.getCount());
	    retVal = (double[])dbr.getValue();
	    assertEquals(dbr.getCount(), retVal.length);
	    assertEquals(12.34, retVal[0], 0.0);
	    for (int i = 2; i < COUNT; i++)
		    assertEquals(0.0, retVal[i], 0.0);
	}


	private void checkValue(Object value, int count)
	{
	    if (count <= 0)
	        return;
	    
	    if (value instanceof byte[]) {
	        byte[] val = (byte[])value;
	        assertEquals(12, val[0]);
	        for (int i = 2; i < count; i++)
		        assertEquals(0, val[0]);
	    }
		else if (value instanceof double[]) {
	        double[] val = (double[])value;
	        assertEquals(12.34, val[0], 0.0);
	        for (int i = 2; i < count; i++)
		        assertEquals(0, val[0], 0.0);
	    }
	    else if (value instanceof float[]) {
	        float[] val = (float[])value;
	        assertEquals(12.34, val[0], 0.0001);
	        for (int i = 2; i < count; i++)
		        assertEquals(0, val[0], 0.0);
	    }
	    else if (value instanceof int[]) {
	        int[] val = (int[])value;
	        assertEquals(12, val[0]);
	        for (int i = 2; i < count; i++)
		        assertEquals(0, val[0]);
	    }
	    else if (value instanceof short[]) {
	        short[] val = (short[])value;
	        assertEquals(12, val[0]);
	        for (int i = 2; i < count; i++)
		        assertEquals(0, val[0]);
	    }
	    else if (value instanceof String[]) {
	        String[] val = (String[])value;
	        assertEquals("12.34", val[0]);
	        for (int i = 2; i < count; i++)
		        assertEquals("0", val[0]);
	    }
	}

	private void internalGetTest(DBRType type, int count)
		throws CAException, TimeoutException, InterruptedException {
	    
	    DBR dbr = channel.get(type, count);
	    context.pendIO(3.0);
	    checkDBR(type, count, dbr);

	    GetListenerImpl listener = new GetListenerImpl();
		channel.get(type, count, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
	    checkDBR(type, count, listener.value);
	}
	
	/**
     * @param type
     * @param count
     * @param dbr
     */
    private void checkDBR(DBRType type, int count, DBR dbr)
    {
        assertEquals(type, dbr.getType());
	    assertEquals(count, dbr.getCount());
	    assertEquals(count, Array.getLength(dbr.getValue()));

	    // TODO tmp check
	    if (type.getValue() == DBR_PutAckS.TYPE.getValue() ||
	        type.getValue() == DBR_PutAckT.TYPE.getValue()) {
	        // what to do here...
	    }
	    else
	        checkValue(dbr.getValue(), count);
	    
	    if (dbr instanceof STS) {
	        STS sts = (STS)dbr;
	        assertEquals(Status.NO_ALARM, sts.getStatus());
	        assertEquals(Severity.NO_ALARM, sts.getSeverity());
	    }
	    if (dbr instanceof TIME && !dbr.isGR()) {
	        TIME time = (TIME)dbr;
	        // 5 sec
	        long diff = Math.abs(new TimeStamp().secPastEpoch() - time.getTimeStamp().secPastEpoch());
	        assertTrue(diff < 100);
	    }
	    if (dbr instanceof GR) {
	        GR gr = (GR)dbr;
	        assertEquals(100, gr.getUpperDispLimit().doubleValue(), 0.0001);
	        assertEquals(5, gr.getUpperAlarmLimit().doubleValue(), 0.0001);
	        assertEquals(3, gr.getUpperWarningLimit().doubleValue(), 0.0001);
	        assertEquals(-3, gr.getLowerWarningLimit().doubleValue(), 0.0001);
	        assertEquals(-5, gr.getLowerAlarmLimit().doubleValue(), 0.0001);
	        assertEquals(-100, gr.getLowerDispLimit().doubleValue(), 0.0001);
	        assertEquals("units", gr.getUnits());
	    }
	    if (dbr instanceof CTRL) {
	        CTRL ctrl = (CTRL)dbr;
	        assertEquals(100, ctrl.getUpperCtrlLimit().doubleValue(), 0.0001);
	        assertEquals(-100, ctrl.getLowerCtrlLimit().doubleValue(), 0.0001);
	    }
	    if (dbr instanceof PRECISION) {
	        PRECISION prec = (PRECISION)dbr;
	        assertEquals(2, prec.getPrecision());
	    }
    }

    /**
	 * Helper method.
	 */
	public void testGets()
		throws CAException, TimeoutException, InterruptedException, IOException {
		
	    internalGetTest(DBR_Byte.TYPE, 1);
	    internalGetTest(DBR_Double.TYPE, 1);
	    internalGetTest(DBR_Enum.TYPE, 1);
	    internalGetTest(DBR_Float.TYPE, 1);
	    internalGetTest(DBR_Int.TYPE, 1);
	    internalGetTest(DBR_Short.TYPE, 1);
	    internalGetTest(DBR_String.TYPE, 1);

	    internalGetTest(DBR_STS_Byte.TYPE, 1);
	    internalGetTest(DBR_STS_Double.TYPE, 1);
	    internalGetTest(DBR_STS_Enum.TYPE, 1);
	    internalGetTest(DBR_STS_Float.TYPE, 1);
	    internalGetTest(DBR_STS_Int.TYPE, 1);
	    internalGetTest(DBR_STS_Short.TYPE, 1);
	    internalGetTest(DBR_STS_String.TYPE, 1);

	    internalGetTest(DBR_TIME_Byte.TYPE, 1);
	    internalGetTest(DBR_TIME_Double.TYPE, 1);
	    internalGetTest(DBR_TIME_Enum.TYPE, 1);
	    internalGetTest(DBR_TIME_Float.TYPE, 1);
	    internalGetTest(DBR_TIME_Int.TYPE, 1);
	    internalGetTest(DBR_TIME_Short.TYPE, 1);
	    internalGetTest(DBR_TIME_String.TYPE, 1);

	    internalGetTest(DBR_GR_Byte.TYPE, 1);
	    internalGetTest(DBR_GR_Double.TYPE, 1);
	    internalGetTest(DBR_GR_Float.TYPE, 1);
	    internalGetTest(DBR_GR_Int.TYPE, 1);
	    internalGetTest(DBR_GR_Short.TYPE, 1);

	    internalGetTest(DBR_CTRL_Byte.TYPE, 1);
	    internalGetTest(DBR_CTRL_Double.TYPE, 1);
	    internalGetTest(DBR_CTRL_Float.TYPE, 1);
	    internalGetTest(DBR_CTRL_Int.TYPE, 1);
	    internalGetTest(DBR_CTRL_Short.TYPE, 1);

	    // type code 24 (GR)
	    internalGetTest(DBR_LABELS_Enum.TYPE, 1);
	    
	    // there two would fail - there is not get op. for PUT types
		//internalGetTest(DBR_PutAckT.TYPE, 1);
	    //internalGetTest(DBR_PutAckS.TYPE, 1);
	    
	    // this test is done in CAJChannelPutTest
		// internalGetTest(DBR_STSACK_String.TYPE, 1);
	    
	    /*
	    // TODO type code 31 (CTRL) - there is not predefined type
	    // also not possible to retrieve value... see at com.cosylab.epics.caj.impl.DBRDecoder.getDBR(DBRDecoder.java:81)
	    //internalGetTest(DBR_LABELS_Enum.TYPE, 1);
	    CAJChannel cchannel = (CAJChannel)channel;

	    GetListenerImpl listener = new GetListenerImpl();
		new ReadNotifyRequest(cchannel, listener, null, cchannel.getTransport(), cchannel.getServerChannelID(),
		        			  31, 1).submit();
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
		
		// dirty (copy from checkDBR method)
		final int count = 1;
		final DBR dbr = listener.value;
	    assertEquals(count, dbr.getCount());
	    assertEquals(count, Array.getLength(dbr.getValue()));

	    checkValue(dbr.getValue(), count);
	    if (dbr instanceof STS) {
	        STS sts = (STS)dbr;
	        assertEquals(Status.NO_ALARM, sts.getStatus());
	        assertEquals(Severity.NO_ALARM, sts.getSeverity());
	    }
	    if (dbr instanceof TIME && !dbr.isGR()) {
	        TIME time = (TIME)dbr;
	        // 5 sec
	        long diff = Math.abs(new TimeStamp().secPastEpoch() - time.getTimeStamp().secPastEpoch());
	        assertTrue(diff < 100);
	    }
	    if (dbr instanceof GR) {
	        GR gr = (GR)dbr;
	        assertEquals(100, gr.getUpperDispLimit().doubleValue(), 0.0001);
	        assertEquals(5, gr.getUpperAlarmLimit().doubleValue(), 0.0001);
	        assertEquals(3, gr.getUpperWarningLimit().doubleValue(), 0.0001);
	        assertEquals(-3, gr.getLowerWarningLimit().doubleValue(), 0.0001);
	        assertEquals(-5, gr.getLowerAlarmLimit().doubleValue(), 0.0001);
	        assertEquals(-100, gr.getLowerDispLimit().doubleValue(), 0.0001);
	        assertEquals("units", gr.getUnits());
	    }
	    if (dbr instanceof CTRL) {
	        CTRL ctrl = (CTRL)dbr;
	        assertEquals(100, ctrl.getUpperCtrlLimit().doubleValue(), 0.0001);
	        assertEquals(-100, ctrl.getLowerCtrlLimit().doubleValue(), 0.0001);
	    }
	    if (dbr instanceof PRECISION) {
	        PRECISION prec = (PRECISION)dbr;
	        assertEquals(2, prec.getPrecision());
	    }*/
	}

    /**
	 * Helper method.
	 */
	public void testEnum()
		throws CAException, TimeoutException, InterruptedException {
		
	    channel.destroy();

	    channel = context.createChannel("enum");
	    context.pendIO(5.0);
	    
	    final DBRType type = DBR_LABELS_Enum.TYPE;
	    final int count = 1;
	    final String[] expectedResult =
	        new String[] {  "zeroString", "oneString", "twoString", "threeString", "fourString", "fiveString",
	            			"sixString", "sevenString", "8s", "9s", "10s", "11s", "12s", "13s", "14s", "15s" };
	    
	    DBR dbr = channel.get(type, count);
	    context.pendIO(3.0);
	    assertTrue(dbr instanceof DBR_LABELS_Enum);

	    DBR_LABELS_Enum label = (DBR_LABELS_Enum)dbr;
	    assertEquals(expectedResult.length, label.getLabels().length);
	    for (int i = 0; i < expectedResult.length; i++)
		    assertEquals(expectedResult[i], label.getLabels()[i]);
	
	    GetListenerImpl listener = new GetListenerImpl();
		channel.get(type, count, listener);
		synchronized (listener)
		{
			context.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
		dbr = listener.value;
	    assertTrue(dbr instanceof DBR_LABELS_Enum);
	    label = (DBR_LABELS_Enum)dbr;
	    assertEquals(expectedResult.length, label.getLabels().length);
	    for (int i = 0; i < expectedResult.length; i++)
		    assertEquals(expectedResult[i], label.getLabels()[i]);
	}

    /**
	 * Helper method.
	 */
	// tmp
	public void testDBRDecoderInvalidTypes() {
	    try {
	        assertEquals(null, DBRDecoder.getDBR(null, Short.MAX_VALUE, 0, null));
	        //fail("Invalid type code accepted.");
	    } catch (IllegalArgumentException iae) {
	        // ok
	    }

	    // inconsistant type
	    try {
	        DBR dbr = DBRFactory.create(DBR_Double.TYPE, 1);
	        assertEquals(null, DBRDecoder.getDBR(dbr, (short)DBR_String.TYPE.getValue(), 1, null));
	        //fail("Inconsitant DBR type accepted.");
	    } catch (IllegalArgumentException iae) {
	        // ok
	    }

	    // inconsistant count
	    try {
	        DBR dbr = DBRFactory.create(DBR_Double.TYPE, 1);
	        assertEquals(null, DBRDecoder.getDBR(dbr, (short)DBR_Double.TYPE.getValue(), 3, null));
	        //fail("Inconsitant DBR element count accepted.");
	    } catch (IllegalArgumentException iae) {
	        // ok
	    }
	}
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		context = new CAJContext();
	    channel = context.createChannel("record1");
	    context.pendIO(5.0);
	    assertEquals(Channel.CONNECTED, channel.getConnectionState());
	    channel.put(12.34);
	    context.pendIO(5.0);
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
		junit.textui.TestRunner.run(CAJChannelGetTest.class);
	}
	
}
