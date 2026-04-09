package com.cosylab.epics.caj.impl.requests.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.CAJMonitor;
import com.cosylab.epics.caj.impl.CATransport;
import com.cosylab.epics.caj.impl.requests.AbstractCARequest;
import com.cosylab.epics.caj.impl.requests.EventAddRequest;

import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;

/**
 * Demonstrates the ByteBuffer corruption bug in
 * {@link EventAddRequest#resubscribeSubscription} using a live IOC.
 *
 * <p>Requires a running IOC with a PV named {@value #PV_NAME}.
 *
 * <p><b>Bug summary:</b> A failed {@code noSyncSend()} during a reconnect
 * leaves {@code requestMessage} with {@code position < capacity} after the
 * transport partially consumed the buffer.  The next
 * {@code resubscribeSubscription()} call passes the buffer to
 * {@code Transport.submit()}, which calls {@code flip()}, baking that partial
 * position into {@code limit}.  The following reconnect then throws
 * {@link IndexOutOfBoundsException} at {@code requestMessage.putInt(8, ...)}
 * because {@code limit < 12}.  This exception is caught and logged at SEVERE
 * in {@code CAJChannel.resubscribeSubscriptions()}, silently leaving the
 * monitor dead.
 *
 * <p>This test demonstrates the exact three-step sequence using reflection to
 * place {@code requestMessage} into the corrupted state, then forces a
 * reconnect via {@link CATransport#close(boolean)}.  With the fix in place the
 * channel reconnects cleanly; without the fix
 * {@code resubscribeSubscription()} throws and the monitor is silently lost.
 */
@Ignore("Requires a running IOC with PV '" + EventAddRequestReconnectIT.PV_NAME + "'")
public class EventAddRequestReconnectIT {

    static final String PV_NAME = "eventAddRequesttest_1";

    private CAJContext context;
    private CAJChannel channel;

    @Before
    public void setUp() throws Exception {
        context = new CAJContext();
        channel = (CAJChannel) context.createChannel(PV_NAME);
        context.pendIO(5.0);
        assertEquals(Channel.CONNECTED, channel.getConnectionState());
    }

    @After
    public void tearDown() throws Exception {
        if (context != null && !context.isDestroyed())
            context.destroy();
        context = null;
    }

    /**
     * Reproduces the three-step buffer corruption sequence, then verifies
     * that the fix allows a clean reconnect and the monitor survives.
     *
     * <p>Without the fix the reconnect in step 3 would log a SEVERE
     * {@link IndexOutOfBoundsException} and the monitor would silently stop
     * delivering updates.
     */
    @Test
    public void monitorSurvivesReconnectAfterPartialSendFailure() throws Exception {
        // --- Step 1: create a monitor (allocates EventAddRequest internally) ---
        CAJMonitor cajMonitor = (CAJMonitor) channel.addMonitor(Monitor.VALUE);
        context.flushIO();
        assertNotNull(cajMonitor);

        // --- Step 2: reach inside and corrupt requestMessage ---
        // Simulate the state left by a failed noSyncSend():
        //   Transport.submit() called flip() (position=0, limit=capacity),
        //   channel.write() advanced position to 5, then threw IOException.
        //   Result: position=5, limit=capacity.
        //
        // Then the NEXT resubscribeSubscription called flip() on that state,
        // leaving position=0, limit=5 — the buffer that triggers the bug.

        Field earField = CAJMonitor.class.getDeclaredField("eventAddRequest");
        earField.setAccessible(true);
        EventAddRequest ear = (EventAddRequest) earField.get(cajMonitor);
        assertNotNull("eventAddRequest must be set after addMonitor()", ear);

        // requestMessage is declared in AbstractCARequest
        Field bufField = AbstractCARequest.class.getDeclaredField("requestMessage");
        bufField.setAccessible(true);
        ByteBuffer requestMessage = (ByteBuffer) bufField.get(ear);
        assertNotNull(requestMessage);

        int capacity = requestMessage.capacity();

        // Place the buffer into the corrupted state (position=0, limit=5).
        // This is the state that caused the third reconnect to throw.
        requestMessage.limit(capacity);   // ensure limit is sane before we start
        requestMessage.position(capacity);
        requestMessage.flip();            // position=0, limit=capacity — as after submit()
        requestMessage.position(5);       // partial write, then IOException
        requestMessage.flip();            // position=0, limit=5 — the corrupted state

        assertEquals("pre-condition: limit should be the partial position",
                5, requestMessage.limit());

        // --- Step 3: force a reconnect ---
        // CAJChannel.resubscribeSubscriptions() will call
        // ear.resubscribeSubscription(newTransport), which calls
        // requestMessage.putInt(8, sid).  With limit=5 and no fix, this throws.
        // With the fix, limit and position are reset to capacity first.
        CATransport transport = (CATransport) channel.getTransport();
        transport.close(true);   // forces disconnect and search/reconnect

        assertEquals(Channel.DISCONNECTED, channel.getConnectionState());

        Thread.sleep(3000);

        // With the fix: channel reconnects and monitor is active.
        // Without the fix: channel reconnects but the SEVERE log shows:
        //   java.lang.IndexOutOfBoundsException
        //     at EventAddRequest.resubscribeSubscription(EventAddRequest.java:...)
        //   and the monitor silently stops delivering updates.
        assertEquals("channel should reconnect after forced close",
                Channel.CONNECTED, channel.getConnectionState());

        // Verify the buffer is in the fully-consumed state after the send:
        //   fix resets to position=capacity, limit=capacity
        //   flip()              → position=0,        limit=capacity
        //   sendBuffer.put(msg) → position=capacity, limit=capacity  (all bytes consumed)
        // position=capacity, limit=capacity is the "ready for next fix-and-flip" state.
        ByteBuffer buf = (ByteBuffer) bufField.get(ear);
        assertEquals("after reconnect: buffer limit should equal full message capacity",
                capacity, buf.limit());
        assertEquals("after reconnect: buffer position should equal capacity (fully consumed)",
                capacity, buf.position());
    }
}
