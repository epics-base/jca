package com.cosylab.epics.caj.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.util.DefaultServerImpl;
import com.cosylab.epics.caj.cas.util.examples.CounterProcessVariable;
import com.cosylab.epics.caj.impl.CATransport;

import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

/**
 * Checks that a monitor delivers each update once after its channel's
 * transport becomes unresponsive and then responsive again.
 *
 * <p>An unresponsive transport is not closed: the server keeps the channel and
 * its subscriptions. When the transport responds again, the channel must not
 * send another EVENT_ADD for its subscriptions, or the server holds two of
 * each and sends every update twice.
 *
 * <p>The test runs a CA server in the same process, with a PV that counts up
 * every 100 ms, and calls the channel's
 * {@link CAJChannel#transportUnresponsive()} and
 * {@link CAJChannel#transportResponsive(com.cosylab.epics.caj.impl.Transport)},
 * as {@link CATransport} does when its echo times out and when the server
 * answers again.
 */
public class ResponsiveTransportMonitorTest {

    private static final String PV_NAME = "COUNTER";

    private CAJServerContext serverContext;
    private CAJContext clientContext;

    /**
     * Collects the values of normal monitor events.
     */
    private static class ValueListener implements MonitorListener {

        private final List<Double> values = new ArrayList<Double>();

        public synchronized void monitorChanged(MonitorEvent event) {
            if (event.getStatus() == CAStatus.NORMAL)
                values.add(((DBR_Double) event.getDBR()).getDoubleValue()[0]);
        }

        public synchronized List<Double> takeValues() {
            List<Double> taken = new ArrayList<Double>(values);
            values.clear();
            return taken;
        }
    }

    @Before
    public void setUp() throws Exception {
        serverContext = new CAJServerContext();
        DefaultServerImpl server = new DefaultServerImpl();
        server.registerProcessVariable(new CounterProcessVariable(PV_NAME, null,
                0, Integer.MAX_VALUE, 1, 100,
                -Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE, Integer.MAX_VALUE));
        serverContext.initialize(server);
        new Thread(new Runnable() {
            public void run() {
                try {
                    serverContext.run(0);
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        }, getClass().getName()).start();

        clientContext = (CAJContext) JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
    }

    @After
    public void tearDown() throws Exception {
        if (clientContext != null && !clientContext.isDestroyed())
            clientContext.destroy();
        if (serverContext != null && !serverContext.isDestroyed())
            serverContext.destroy();
    }

    @Test
    public void monitorDeliversEachUpdateOnceAfterTransportRecovers() throws Exception {
        CAJChannel channel = (CAJChannel) clientContext.createChannel(PV_NAME);
        clientContext.pendIO(5.0);
        assertEquals(Channel.CONNECTED, channel.getConnectionState());

        ValueListener listener = new ValueListener();
        Monitor monitor = channel.addMonitor(DBR_Double.TYPE, 1, Monitor.VALUE, listener);
        clientContext.flushIO();
        clientContext.pendEvent(1.5);
        assertNoRepeatedValues("before the transport became unresponsive", listener.takeValues());

        CATransport transport = channel.getTransport();
        assertNotNull(transport);
        channel.transportUnresponsive();
        assertEquals(Channel.DISCONNECTED, channel.getConnectionState());
        channel.transportResponsive(transport);
        assertEquals(Channel.CONNECTED, channel.getConnectionState());

        // let updates sent before the transport recovered arrive, then count
        clientContext.pendEvent(0.5);
        listener.takeValues();
        clientContext.pendEvent(1.5);
        assertNoRepeatedValues("after the transport responded again", listener.takeValues());

        monitor.clear();
    }

    /**
     * The counter only counts up, so a value seen twice in a row is a
     * duplicate update.
     */
    private static void assertNoRepeatedValues(String when, List<Double> values) {
        assertTrue("no updates " + when, values.size() >= 5);
        int repeated = 0;
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i).doubleValue() == values.get(i - 1).doubleValue())
                repeated++;
        }
        assertEquals("repeated updates " + when + " in " + values, 0, repeated);
    }
}
