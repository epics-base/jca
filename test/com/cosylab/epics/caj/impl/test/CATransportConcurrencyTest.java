package com.cosylab.epics.caj.impl.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.util.DefaultServerImpl;
import com.cosylab.epics.caj.cas.util.MemoryProcessVariable;
import com.cosylab.epics.caj.impl.CATransport;
import com.cosylab.epics.caj.impl.CachedByteBufferAllocator;

import gov.aps.jca.Channel;
import gov.aps.jca.configuration.DefaultConfiguration;
import gov.aps.jca.dbr.DBR_Int;

public class CATransportConcurrencyTest {

    private CAJServerContext serverContext;
    private CAJContext context;
    private CAJChannel channel;
    private ExecutorService executor;
    private Future<?> serverTask;
    private int[] value;

    @Before
    public void setUp() throws Exception {
        value = new int[CachedByteBufferAllocator.bufferSize / Integer.BYTES + 1];
        for (int i = 0; i < value.length; i++)
            value[i] = i;

        DefaultServerImpl server = new DefaultServerImpl();
        server.registerProcessVariable(new MemoryProcessVariable(
                "transport-concurrency-array", null, DBR_Int.TYPE, new int[value.length]));

        DefaultConfiguration serverConfig = new DefaultConfiguration("server");
        serverConfig.setAttribute("auto_beacon_addr_list", "false");
        serverConfig.setAttribute("beacon_addr_list", "");
        serverConfig.setAttribute("max_array_bytes", Integer.toString(value.length * Integer.BYTES + 1024));
        serverContext = new CAJServerContext();
        serverContext.configure(serverConfig);
        serverContext.setTcpServerPort(0);
        serverContext.setUdpServerPort(0);
        serverContext.initialize(server);

        executor = Executors.newFixedThreadPool(2);
        serverTask = executor.submit(() -> {
            serverContext.run(0);
            return null;
        });

        DefaultConfiguration clientConfig = new DefaultConfiguration("client");
        clientConfig.setAttribute("auto_addr_list", "false");
        clientConfig.setAttribute("addr_list", "127.0.0.1:" + serverContext.getUdpServerPort());
        clientConfig.setAttribute("max_array_bytes", Integer.toString(value.length * Integer.BYTES + 1024));
        context = new CAJContext();
        context.configure(clientConfig);
        channel = (CAJChannel) context.createChannel("transport-concurrency-array");
        context.pendIO(5.0);
        assertEquals(Channel.CONNECTED, channel.getConnectionState());
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (context != null)
                context.destroy();
        } finally {
            try {
                if (serverContext != null)
                    serverContext.destroy();
                if (serverTask != null)
                    serverTask.get(5, TimeUnit.SECONDS);
            } finally {
                if (executor != null) {
                    executor.shutdownNow();
                    assertTrue("Test threads did not terminate", executor.awaitTermination(5, TimeUnit.SECONDS));
                }
            }
        }
    }

    @Test
    public void largeArrayWriteAndFlushDoNotRequireTransportMonitor() throws Exception {
        CATransport transport = channel.getTransport();
        channel.put(new int[] { -1 });

        Future<?> write = null;
        try {
            // A concurrent flush used to hold this monitor while waiting for sendQueue.
            // Force the opposite path without creating an unbreakable deadlock on failure.
            synchronized (transport) {
                write = executor.submit(() -> {
                    channel.put(value);
                    context.flushIO();
                    return null;
                });
                write.get(5, TimeUnit.SECONDS);
            }
        } finally {
            // Release the monitor before joining, including when the old code times out.
            if (write != null)
                write.get(5, TimeUnit.SECONDS);
        }

        DBR_Int actual = (DBR_Int) channel.get(DBR_Int.TYPE, value.length);
        context.pendIO(5.0);
        assertArrayEquals(value, actual.getIntValue());
    }
}
