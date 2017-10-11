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

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.Channel;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.cas.ProcessVariableReadCallback;
import gov.aps.jca.cas.ProcessVariableWriteCallback;
import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_Byte;
import gov.aps.jca.dbr.DBR_CTRL_Byte;
import gov.aps.jca.dbr.DBR_CTRL_Double;
import gov.aps.jca.dbr.DBR_CTRL_Enum;
import gov.aps.jca.dbr.DBR_CTRL_Float;
import gov.aps.jca.dbr.DBR_CTRL_Int;
import gov.aps.jca.dbr.DBR_CTRL_Short;
import gov.aps.jca.dbr.DBR_CTRL_String;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.DBR_Enum;
import gov.aps.jca.dbr.DBR_Float;
import gov.aps.jca.dbr.DBR_GR_Byte;
import gov.aps.jca.dbr.DBR_GR_Double;
import gov.aps.jca.dbr.DBR_GR_Float;
import gov.aps.jca.dbr.DBR_GR_Int;
import gov.aps.jca.dbr.DBR_GR_Short;
import gov.aps.jca.dbr.DBR_GR_String;
import gov.aps.jca.dbr.DBR_Int;
import gov.aps.jca.dbr.DBR_LABELS_Enum;
import gov.aps.jca.dbr.DBR_STSACK_String;
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
import gov.aps.jca.dbr.LABELS;
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.util.DefaultServerImpl;
import com.cosylab.epics.caj.cas.util.MemoryProcessVariable;

import junit.framework.TestCase;

/**
 * Get (encoder) and put test.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class GetAndPutTest extends TestCase {

	private final double VALUE = 12.08; 
	private final String STR_VALUE = "12.080"; // precision is 3

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
     * Implementation of MonitorListener.
     */
    private class MonitorListenerImpl implements MonitorListener {
        /* (non-Javadoc)
         * @see gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.MonitorEvent)
         */
        
        public volatile CAStatus status;
        public volatile DBR response;
        
        public synchronized void monitorChanged(MonitorEvent ev)
        {
            status = ev.getStatus();
            response = ev.getDBR();
            this.notifyAll();
        }

        public synchronized void reset()
        {
            status = null;
            response = null;
        }
    }

    /**
	 * Context to be tested.
	 */
	private CAJServerContext context;
	
	/**
	 * Client cntext to be tested.
	 */
	private CAJContext clientContext;

	/**
	 * Test labels.
	 */
	private final String[] LABELS =
	{ "zero", "one", "two", "three", "four", "five", "six", "seven" }; 

	/**
	 * Constructor for CAJContextStateTest.
	 * @param methodName
	 */
	public GetAndPutTest(String methodName) {
		super(methodName);
	}

	private final static void checkStatus(STS status)
	{
		assertEquals(Status.UDF_ALARM, status.getStatus());
		assertEquals(Severity.INVALID_ALARM, status.getSeverity());
	}
	
	private final static void checkTime(TIME timeDBR)
	{
		checkStatus(timeDBR);
		TimeStamp time = timeDBR.getTimeStamp(); 
		time.sub(new TimeStamp());
		assertTrue(time.secPastEpoch() < 3);
	}

	private final static void checkGR(GR gr)
	{
		checkStatus(gr);

		assertEquals(new Double(10).doubleValue(), gr.getUpperDispLimit().doubleValue(), 0.0);
		assertEquals(new Double(-10).doubleValue(), gr.getLowerDispLimit().doubleValue(), 0.0);
		
		assertEquals(new Double(9).doubleValue(), gr.getUpperAlarmLimit().doubleValue(), 0.0);
		assertEquals(new Double(7).doubleValue(), gr.getUpperWarningLimit().doubleValue(), 0.0);
		assertEquals(new Double(-7).doubleValue(), gr.getLowerWarningLimit().doubleValue(), 0.0);
		assertEquals(new Double(-9).doubleValue(), gr.getLowerAlarmLimit().doubleValue(), 0.0);

		assertEquals("units", gr.getUnits());
		
		if (gr instanceof PRECISION)
		{
			PRECISION prec = (PRECISION)gr;
			assertEquals(3, prec.getPrecision());
		}
	}

	private void testEnumValue(Channel channel, short value) throws Exception
	{
		DBR dbr = channel.get();
		clientContext.pendIO(3.0);
		assertEquals(value, ((DBR_Enum)dbr).getEnumValue()[0]);
		
		// test string (enum -> string conversion)
		dbr = channel.get(DBR_String.TYPE, channel.getElementCount());
		clientContext.pendIO(3.0);
		assertEquals(LABELS[value], ((DBR_String)dbr).getStringValue()[0]);
	}

	public void testEnum() throws Exception
	{
		Channel channel = clientContext.createChannel("ENUM");
		clientContext.pendIO(3.0);

		// test all labels
		DBR dbr = channel.get(DBR_LABELS_Enum.TYPE, channel.getElementCount());
		clientContext.pendIO(3.0);
		checkLABELS((LABELS)dbr, LABELS);
		
		testEnumValue(channel, (short)0);

		// test string (enum -> string conversion) array
		dbr = channel.get(DBR_String.TYPE, channel.getElementCount());
		clientContext.pendIO(3.0);
		String[] vals = ((DBR_String)dbr).getStringValue();
		assertEquals(channel.getElementCount(), vals.length);
		for (int i = 0; i < vals.length; i++)
			assertEquals(LABELS[i], vals[i]);

		PutListenerImpl listener = new PutListenerImpl();
		channel.put(new short[] { 1, 1, 1 }, listener);
		synchronized (listener)
		{
			clientContext.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
		testEnumValue(channel, (short)1);
		
		// change value, string -> enum
		listener = new PutListenerImpl();
		synchronized (listener)
		{
			channel.put(new String[] { "two", "two", "two" }, listener);
			clientContext.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
		testEnumValue(channel, (short)2);

		// change value - invalid string, string -> enum
		listener = new PutListenerImpl();
		synchronized (listener)
		{
			channel.put(new String[] { "invalid", "two", "two" }, listener);
			clientContext.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NOCONVERT, listener.status);
		testEnumValue(channel, (short)2);

		/*
		// BADCOUNT
		listener = new PutListenerImpl();
		synchronized (listener)
		{
			channel.put(new String[] { "one", "two", "three", "four" }, listener);
			clientContext.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.BADCOUNT, listener.status);

		// this checks encoder, BADCOUNT 
		GetListenerImpl listener2 = new GetListenerImpl();
		channel.get(DBR_String.TYPE, 100, listener2);
		synchronized (listener2)
		{
			clientContext.flushIO();
			listener2.wait(3000);
		}
		assertEquals(CAStatus.BADCOUNT, listener2.status);
		*/
	}
	
	private final static void checkLABELS(LABELS labels, String[] result)
	{
		checkStatus((STS)labels);

		String[] enums = labels.getLabels();
		// null check
		if (enums == result)
			return;
		
		// length test
		assertEquals(enums.length, result.length);
		
		// value check
		for (int i = 0; i < enums.length; i++)
			assertEquals(result[i], enums[i]);
	}

	private final static void checkCTRL(CTRL ctrl)
	{
		checkGR(ctrl);

		assertEquals(new Double(8).doubleValue(), ctrl.getUpperCtrlLimit().doubleValue(), 0.0);
		assertEquals(new Double(-8).doubleValue(), ctrl.getLowerCtrlLimit().doubleValue(), 0.0);
	}

	public void testAllTypes() throws Exception
	{
		allTypesTest("TEST");
	}
	
	public void testAsyncAllTypes() throws Exception
	{
		allTypesTest("ASYNC");
	}

	private void allTypesTest(String name) throws Exception
	{
		Channel channel = clientContext.createChannel(name);
		clientContext.pendIO(3.0);
		channel.printInfo();
		channel.put(VALUE);

		int nativeCount = channel.getElementCount();

		//
		// basic
		//
		
		DBR dbr = channel.get(DBR_String.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals(STR_VALUE, ((DBR_String)dbr).getStringValue()[0]);
		
		dbr = channel.get(DBR_Short.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((short)VALUE, ((DBR_Short)dbr).getShortValue()[0]);

		dbr = channel.get(DBR_Float.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((float)VALUE, ((DBR_Float)dbr).getFloatValue()[0], 0.0);

		dbr = channel.get(DBR_Enum.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((short)VALUE, ((DBR_Enum)dbr).getEnumValue()[0]);

		dbr = channel.get(DBR_Byte.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((byte)VALUE, ((DBR_Byte)dbr).getByteValue()[0]);

		dbr = channel.get(DBR_Int.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((int)VALUE, ((DBR_Int)dbr).getIntValue()[0]);

		dbr = channel.get(DBR_Double.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((double)VALUE, ((DBR_Double)dbr).getDoubleValue()[0], 0.0);

		
		
		//
		// STS
		//
		
		dbr = channel.get(DBR_STS_String.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals(STR_VALUE, ((DBR_String)dbr).getStringValue()[0]);
		checkStatus((STS)dbr);
		
		dbr = channel.get(DBR_STS_Short.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((short)VALUE, ((DBR_Short)dbr).getShortValue()[0]);
		checkStatus((STS)dbr);

		dbr = channel.get(DBR_STS_Float.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((float)VALUE, ((DBR_Float)dbr).getFloatValue()[0], 0.0);
		checkStatus((STS)dbr);

		dbr = channel.get(DBR_STS_Enum.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((short)VALUE, ((DBR_Enum)dbr).getEnumValue()[0]);
		checkStatus((STS)dbr);

		dbr = channel.get(DBR_STS_Byte.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((byte)VALUE, ((DBR_Byte)dbr).getByteValue()[0]);
		checkStatus((STS)dbr);

		dbr = channel.get(DBR_STS_Int.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((int)VALUE, ((DBR_Int)dbr).getIntValue()[0]);
		checkStatus((STS)dbr);

		dbr = channel.get(DBR_STS_Double.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((double)VALUE, ((DBR_Double)dbr).getDoubleValue()[0], 0.0);
		checkStatus((STS)dbr);
		

		//
		// TIME
		//
		
		dbr = channel.get(DBR_TIME_String.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals(STR_VALUE, ((DBR_String)dbr).getStringValue()[0]);
		checkTime((TIME)dbr);
		
		dbr = channel.get(DBR_TIME_Short.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((short)VALUE, ((DBR_Short)dbr).getShortValue()[0]);
		checkTime((TIME)dbr);

		dbr = channel.get(DBR_TIME_Float.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((float)VALUE, ((DBR_Float)dbr).getFloatValue()[0], 0.0);
		checkTime((TIME)dbr);

		dbr = channel.get(DBR_TIME_Enum.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((short)VALUE, ((DBR_Enum)dbr).getEnumValue()[0]);
		checkTime((TIME)dbr);

		dbr = channel.get(DBR_TIME_Byte.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((byte)VALUE, ((DBR_Byte)dbr).getByteValue()[0]);
		checkTime((TIME)dbr);

		dbr = channel.get(DBR_TIME_Int.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((int)VALUE, ((DBR_Int)dbr).getIntValue()[0]);
		checkTime((TIME)dbr);

		dbr = channel.get(DBR_TIME_Double.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((double)VALUE, ((DBR_Double)dbr).getDoubleValue()[0], 0.0);
		checkTime((TIME)dbr);

		
		//
		// GR
		//
		
		dbr = channel.get(DBR_GR_String.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals(STR_VALUE, ((DBR_String)dbr).getStringValue()[0]);
		checkStatus((STS)dbr);

		dbr = channel.get(DBR_GR_Short.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((short)VALUE, ((DBR_Short)dbr).getShortValue()[0]);
		checkGR((GR)dbr);

		dbr = channel.get(DBR_GR_Float.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((float)VALUE, ((DBR_Float)dbr).getFloatValue()[0], 0.0);
		checkGR((GR)dbr);

		dbr = channel.get(DBR_LABELS_Enum.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((short)VALUE, ((DBR_Enum)dbr).getEnumValue()[0]);
		checkLABELS((LABELS)dbr, new String[0]);

		dbr = channel.get(DBR_GR_Byte.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((byte)VALUE, ((DBR_Byte)dbr).getByteValue()[0]);
		checkGR((GR)dbr);

		dbr = channel.get(DBR_GR_Int.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((int)VALUE, ((DBR_Int)dbr).getIntValue()[0]);
		checkGR((GR)dbr);

		dbr = channel.get(DBR_GR_Double.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((double)VALUE, ((DBR_Double)dbr).getDoubleValue()[0], 0.0);
		checkGR((GR)dbr);

		
		//
		// CTRL
		//
		
		dbr = channel.get(DBR_CTRL_String.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals(STR_VALUE, ((DBR_String)dbr).getStringValue()[0]);
		checkStatus((STS)dbr);

		dbr = channel.get(DBR_CTRL_Short.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((short)VALUE, ((DBR_Short)dbr).getShortValue()[0]);
		checkCTRL((CTRL)dbr);

		dbr = channel.get(DBR_CTRL_Float.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((float)VALUE, ((DBR_Float)dbr).getFloatValue()[0], 0.0);
		checkCTRL((CTRL)dbr);

		dbr = channel.get(DBR_CTRL_Enum.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((short)VALUE, ((DBR_Enum)dbr).getEnumValue()[0]);
		checkLABELS((LABELS)dbr, new String[0]);

		dbr = channel.get(DBR_CTRL_Byte.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((byte)VALUE, ((DBR_Byte)dbr).getByteValue()[0]);
		checkCTRL((CTRL)dbr);

		dbr = channel.get(DBR_CTRL_Int.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((int)VALUE, ((DBR_Int)dbr).getIntValue()[0]);
		checkCTRL((CTRL)dbr);

		dbr = channel.get(DBR_CTRL_Double.TYPE, nativeCount);
		clientContext.pendIO(3.0);
		assertEquals((double)VALUE, ((DBR_Double)dbr).getDoubleValue()[0], 0.0);
		checkCTRL((CTRL)dbr);
	}

	private void readWriteFailurePVTest(String name) throws Exception
	{
		Channel channel = clientContext.createChannel(name);
		clientContext.pendIO(3.0);

		// read notify test
		GetListenerImpl listener = new GetListenerImpl();
		channel.get(listener);
		synchronized (listener)
		{
			clientContext.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.DEFUNCT, listener.status);

		// write test
		channel.put(111.11);
		clientContext.pendEvent(3.0);
		// no actual test here...
		
		// write notify test
		PutListenerImpl listener2 = new PutListenerImpl();
		channel.put(12.34, listener2);
		synchronized (listener2)
		{
			clientContext.flushIO();
			listener2.wait(3000);
		}
		assertEquals(CAStatus.DEFUNCT, listener2.status);
	}
	
	public void testErrorReadWrite() throws Exception
	{
		readWriteFailurePVTest("ERROR");
	}
	
	public void testExceptionReadWrite() throws Exception
	{
		readWriteFailurePVTest("EXCEPTION");
	}

	public void testCount() throws Exception
	{
		// less
		countTest("ARRAY", 1);
		// more
		countTest("ARRAY", 5);
	}
	private void countTest(String name, int count) throws Exception
	{
		Channel channel = clientContext.createChannel(name);
		clientContext.pendIO(3.0);

		// set value
		double[] dbl = new double[count];
		for (int i = 0; i < count; i++)
			dbl[i] = i + 0.123;
		
		// put test
		channel.put(dbl);
		clientContext.pendEvent(3.0);
		
		// read test
		DBR dbr = channel.get(DBRType.DOUBLE, count);
		clientContext.pendIO(3.0);
		double[] result = ((DBR_Double)dbr).getDoubleValue();
		for (int i = 0; i < count; i++)
			assertEquals(dbl[i], result[i], 0.0);
			
		// new value
		double[] olddbl = dbl;
		for (int i = 0; i < count; i++)
			dbl[i] = i + 0.321;
		
		// write notify test
		PutListenerImpl listener2 = new PutListenerImpl();
		channel.put(dbl, listener2);
		synchronized (listener2)
		{
			clientContext.flushIO();
			listener2.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener2.status);
		
		// read notify test
		GetListenerImpl listener = new GetListenerImpl();
		channel.get(DBRType.DOUBLE, count, listener);
		synchronized (listener)
		{
			clientContext.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);
		result = ((DBR_Double)listener.value).getDoubleValue();
		for (int i = 0; i < count; i++)
			assertEquals(dbl[i], result[i], 0.0);

		// monitor test
		Monitor monitor = null;
	    MonitorListenerImpl listener3 = new MonitorListenerImpl();
		
	    synchronized (listener3)
	    {
	        monitor = channel.addMonitor(DBRType.DOUBLE, count, Monitor.VALUE, listener3);
	        clientContext.flushIO();
	        
	        listener3.wait(3000);
	    }
	    
		assertEquals(CAStatus.NORMAL, listener3.status);
		result = ((DBR_Double)listener3.response).getDoubleValue();
		for (int i = 0; i < count; i++)
			assertEquals(dbl[i], result[i], 0.0);
        listener3.reset();

        
        dbl = olddbl;
	    synchronized (listener3)
	    {
	    	channel.put(dbl);
	        clientContext.flushIO();
	        
	        listener3.wait(3000);
	    }

		assertEquals(CAStatus.NORMAL, listener3.status);
		result = ((DBR_Double)listener3.response).getDoubleValue();
		for (int i = 0; i < count; i++)
			assertEquals(dbl[i], result[i], 0.0);
        listener3.reset();

        monitor.clear();
	}

	public void testACKTS() throws Exception
	{
		boolean ACKT = false;
		Severity ACKS = Severity.MAJOR_ALARM;
			
		Channel channel = clientContext.createChannel("TEST");
		clientContext.pendIO(3.0);
		
		//
		// sync
		//
		
		// put ackt
		channel.putACKT(ACKT);
		clientContext.flushIO();
		
		// put acks
		channel.putACKS(ACKS);
		clientContext.flushIO();
		
		// get STSACK_STRING
		DBR_STSACK_String dbr = (DBR_STSACK_String)channel.get(DBR_STSACK_String.TYPE, 1);
		clientContext.pendIO(3.0);

		checkStatus(dbr);

		assertEquals(1, dbr.getStringValue().length);
		assertEquals("12.080", dbr.getStringValue()[0]);
		
		assertEquals(ACKS, dbr.getAckS());
		assertEquals(ACKT, dbr.getAckT());

		//
		// async
		//

		ACKT = true;
		ACKS = Severity.NO_ALARM;
		
		// put ackt
		PutListenerImpl listener = new PutListenerImpl();
		channel.putACKT(ACKT, listener);
		synchronized (listener)
		{
			clientContext.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);

		// put acks
		listener = new PutListenerImpl();
		channel.putACKS(ACKS, listener);
		synchronized (listener)
		{
			clientContext.flushIO();
			listener.wait(3000);
		}
		assertEquals(CAStatus.NORMAL, listener.status);

		
		// get STSACK_STRING
		dbr = (DBR_STSACK_String)channel.get(DBR_STSACK_String.TYPE, 1);
		clientContext.pendIO(3.0);

		checkStatus(dbr);

		assertEquals(1, dbr.getStringValue().length);
		assertEquals("12.080", dbr.getStringValue()[0]);
		
		assertEquals(ACKS, dbr.getAckS());
		assertEquals(ACKT, dbr.getAckT());
	}

	private void readWritePVTest(String name, boolean expectError) throws Exception
	{
		Channel channel = clientContext.createChannel(name);
		clientContext.pendIO(3.0);

		// read notify test
		GetListenerImpl listener = new GetListenerImpl();
		channel.get(listener);
		synchronized (listener)
		{
			clientContext.flushIO();
			listener.wait(3000);
		}
		if (expectError)
			assertEquals(CAStatus.GETFAIL, listener.status);
		else
		{
			assertEquals(CAStatus.NORMAL, listener.status);
			assertEquals((double)VALUE, ((DBR_Double)listener.getValue()).getDoubleValue()[0], 0.0);
		}

		final double NEW_VALUE = 123.43;
		
		// write test
		channel.put(NEW_VALUE);
		clientContext.pendEvent(3.0);
		if (expectError)
		{
			// no actual test here...
		}
		else
		{
			DBR dbr = channel.get();
			clientContext.pendIO(3.0);
			assertEquals((double)NEW_VALUE, ((DBR_Double)dbr).getDoubleValue()[0], 0.0);
		}
		
		final double NEW_VALUE2 = 123.43;

		// write notify test
		PutListenerImpl listener2 = new PutListenerImpl();
		channel.put(NEW_VALUE2, listener2);
		synchronized (listener2)
		{
			clientContext.flushIO();
			listener2.wait(3000);
		}
		if (expectError)
		{
			assertEquals(CAStatus.PUTFAIL, listener2.status);
			// no actual test here...
		}
		else
		{
			assertEquals(CAStatus.NORMAL, listener2.status);
			DBR dbr = channel.get();
			clientContext.pendIO(3.0);
			assertEquals((double)NEW_VALUE2, ((DBR_Double)dbr).getDoubleValue()[0], 0.0);
		}
	}

	public void testAsyncReadWrite() throws Exception
	{
		readWritePVTest("ASYNC", false);
	}

	public void testAsyncReadWriteException() throws Exception
	{
		readWritePVTest("ASYNCEX", true);
	}

	public void testAsyncFastReadWrite() throws Exception
	{
		readWritePVTest("FASTASYNC", false);
	}

	public void testAsyncFastReadWriteException() throws Exception
	{
		readWritePVTest("FASTASYNCEX", true);
	}

	private void setCTRLData(MemoryProcessVariable mpv) {
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
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		
		context = new CAJServerContext();
		
		DefaultServerImpl server = new DefaultServerImpl();

		// TEST double PV
		MemoryProcessVariable mpv = new MemoryProcessVariable("TEST", null, DBR_Double.TYPE, new double[] { VALUE });
		setCTRLData(mpv);
		server.registerProcessVaribale(mpv);
		
		// ENUM
		MemoryProcessVariable enumPV = new MemoryProcessVariable("ENUM", null, DBR_Enum.TYPE, new short[] { 0, 1, 2 }) 
		{
			/* (non-Javadoc)
			 * @see com.cosylab.epics.caj.cas.util.MemoryProcessVariable#getEnumLabels()
			 */
			public String[] getEnumLabels() {
				return LABELS;
			}
			
		};
		server.registerProcessVaribale(enumPV);

		// read/write error PV
		MemoryProcessVariable errorPV = new MemoryProcessVariable("ERROR", null, DBR_Double.TYPE, new double[] { 0.0 } ) {

			/* (non-Javadoc)
			 * @see com.cosylab.epics.caj.cas.util.MemoryProcessVariable#read(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableReadCallback)
			 */
			public synchronized CAStatus read(DBR value, ProcessVariableReadCallback asyncReadCallback) throws CAException {
				return CAStatus.DEFUNCT;
			}

			/* (non-Javadoc)
			 * @see com.cosylab.epics.caj.cas.util.MemoryProcessVariable#write(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableWriteCallback)
			 */
			public synchronized CAStatus write(DBR value, ProcessVariableWriteCallback asyncWriteCallback) throws CAException {
				return CAStatus.DEFUNCT;
			}
			
		};
		server.registerProcessVaribale(errorPV);
		
		// read/write exception PV
		MemoryProcessVariable exceptionPV = new MemoryProcessVariable("EXCEPTION", null, DBR_Double.TYPE, new double[] { 0.0 } ) {

			/* (non-Javadoc)
			 * @see com.cosylab.epics.caj.cas.util.MemoryProcessVariable#read(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableReadCallback)
			 */
			public synchronized CAStatus read(DBR value, ProcessVariableReadCallback asyncReadCallback) throws CAException {
				throw new CAStatusException(CAStatus.DEFUNCT, "Simulated exception.");
			}

			/* (non-Javadoc)
			 * @see com.cosylab.epics.caj.cas.util.MemoryProcessVariable#write(gov.aps.jca.dbr.DBR, gov.aps.jca.cas.ProcessVariableWriteCallback)
			 */
			public synchronized CAStatus write(DBR value, ProcessVariableWriteCallback asyncWriteCallback) throws CAException {
				throw new CAStatusException(CAStatus.DEFUNCT, "Simulated exception.");
			}
			
		};
		server.registerProcessVaribale(exceptionPV);

		
		// async (no exceptions)
		MemoryProcessVariable ampv = new TestAsyncMemoryProcessVariable("ASYNC", null, DBR_Double.TYPE, new double[] { VALUE },
				500, false);
		setCTRLData(ampv);
		server.registerProcessVaribale(ampv);
		
		// async (exceptions)
		ampv = new TestAsyncMemoryProcessVariable("ASYNCEX", null, DBR_Double.TYPE, new double[] { VALUE },
				1000, true);
		server.registerProcessVaribale(ampv);

		// fast async (no exceptions)
		ampv = new TestAsyncMemoryProcessVariable("FASTASYNC", null, DBR_Double.TYPE, new double[] { VALUE },
				0, false);
		server.registerProcessVaribale(ampv);

		// fast async (exceptions)
		ampv = new TestAsyncMemoryProcessVariable("FASTASYNCEX", null, DBR_Double.TYPE, new double[] { VALUE },
				0, true);
		server.registerProcessVaribale(ampv);

		server.createMemoryProcessVariable("ARRAY", DBRType.DOUBLE, new double[] { 1, 2, 3 } );
		
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
	
		clientContext = (CAJContext)JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
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
	}

	/**
	 * Java main entry point.
	 * @param args	arguments.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(GetAndPutTest.class);
	}
}
