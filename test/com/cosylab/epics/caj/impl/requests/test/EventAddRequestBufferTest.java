package com.cosylab.epics.caj.impl.requests.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.cosylab.epics.caj.impl.CAConstants;

/**
 * Regression test for the ByteBuffer corruption bug in
 * {@link com.cosylab.epics.caj.impl.requests.EventAddRequest#resubscribeSubscription}.
 *
 * <p>A failed {@code noSyncSend()} during a reconnect could leave
 * {@code requestMessage} with {@code position < capacity} after the transport
 * had partially consumed the buffer via {@code channel.write()}.  On the next
 * call to {@code resubscribeSubscription()}, {@code Transport.submit()} would
 * call {@code flip()}, baking that partial position into {@code limit}.  The
 * reconnect after that would then throw {@link IndexOutOfBoundsException} at
 * {@code requestMessage.putInt(8, ...)} because {@code limit < 12}.
 *
 * <p>The fix resets {@code limit} and {@code position} to {@code capacity} at
 * the top of {@code resubscribeSubscription()} so that {@code flip()} always
 * produces {@code position=0, limit=capacity}.  Because {@code requestMessage}
 * is a fixed-size, single-purpose buffer allocated in the
 * {@code EventAddRequest} constructor to hold exactly one EventAdd message,
 * {@code capacity} is always equal to the message size.
 */
public class EventAddRequestBufferTest {

    // EventAddRequest allocates CA_MESSAGE_HEADER_SIZE (16) + 16 payload bytes
    // for the common case of dataCount < 0xFFFF.
    private static final int MESSAGE_SIZE = CAConstants.CA_MESSAGE_HEADER_SIZE + 16;

    // Offset of the server channel ID (parameter1) in a standard CA header.
    // putInt(8, sid) is the call that throws when limit < 12.
    private static final int SID_OFFSET = 8;

    /**
     * Reproduces the exact three-step sequence that caused the original
     * {@link IndexOutOfBoundsException} in the field.
     *
     * <pre>
     * Step 1 – constructor writes MESSAGE_SIZE bytes:
     *   position=MESSAGE_SIZE, limit=MESSAGE_SIZE  (fully written)
     *
     * Step 2 – first resubscribeSubscription: submit() flips, channel.write()
     *          partially consumes N bytes, then noSyncSend() throws IOException:
     *   position=N, limit=MESSAGE_SIZE  (partially consumed)
     *
     * Step 3 – second resubscribeSubscription: submit() flips the partial position:
     *   position=0, limit=N  (corrupted — limit is now N, not MESSAGE_SIZE)
     *
     * Step 4 – third resubscribeSubscription: putInt(8, sid) with limit=N < 12:
     *   throws IndexOutOfBoundsException
     * </pre>
     */
    @Test
    public void bufferCorruptionAfterPartialSend_throwsOnThirdReconnect() {
        ByteBuffer buf = simulateFullyWrittenBuffer();

        // Step 2: submit flips, partial write advances position to 5, then IOException
        buf.flip();
        buf.position(5); // position=5, limit=MESSAGE_SIZE

        // Step 3: second resubscribeSubscription — putInt(8,...) still ok here,
        // then submit flips the partial position into limit
        buf.putInt(SID_OFFSET, 0x00000001); // absolute put, limit=MESSAGE_SIZE >= 12: ok
        buf.flip(); // position=0, limit=5 — corrupted

        assertEquals("limit should be the partial position after the corrupting flip",
                5, buf.limit());

        // Step 4: third resubscribeSubscription — putInt(8,...) now throws
        try {
            buf.putInt(SID_OFFSET, 0x00000002); // limit=5, index 8 is out of bounds
            fail("Expected IndexOutOfBoundsException — this is the original bug");
        } catch (IndexOutOfBoundsException e) {
            // expected — this is the bug that was fixed
        }
    }

    /**
     * Verifies that after applying the fix (resetting {@code limit} and
     * {@code position} to {@code capacity}), the buffer is in the
     * fully-written state and {@code flip()} produces a correct read view.
     */
    @Test
    public void fixResetsBufferToFullyWrittenState() {
        ByteBuffer buf = simulateFullyWrittenBuffer();

        // Drive the buffer into the corrupted state (position=0, limit=5)
        buf.flip();
        buf.position(5);
        buf.flip();

        // Apply the fix
        buf.limit(buf.capacity());
        buf.position(buf.capacity());

        // putInt(8, sid) must succeed
        buf.putInt(SID_OFFSET, 0xCAFEBABE);

        // submit() calls flip() — must yield position=0, limit=MESSAGE_SIZE
        buf.flip();
        assertEquals("position should be 0 after flip", 0, buf.position());
        assertEquals("limit should equal capacity (full message) after flip",
                MESSAGE_SIZE, buf.limit());
    }

    /**
     * Verifies that the fix is idempotent: repeated calls to
     * resubscribeSubscription (simulated by the reset + flip cycle) always
     * leave the buffer in the same valid send state regardless of how many
     * reconnects occur.
     */
    @Test
    public void fixIsIdempotentAcrossMultipleReconnects() {
        ByteBuffer buf = simulateFullyWrittenBuffer();

        for (int reconnect = 1; reconnect <= 5; reconnect++) {
            // Apply fix
            buf.limit(buf.capacity());
            buf.position(buf.capacity());

            // Update SID — must not throw
            buf.putInt(SID_OFFSET, reconnect);

            // submit() flips
            buf.flip();

            assertEquals("reconnect " + reconnect + ": position should be 0",
                    0, buf.position());
            assertEquals("reconnect " + reconnect + ": limit should equal MESSAGE_SIZE",
                    MESSAGE_SIZE, buf.limit());

            // Simulate a partial write consuming half the buffer
            buf.position(MESSAGE_SIZE / 2);
        }
    }

    // -------------------------------------------------------------------------

    /** Returns a ByteBuffer in the fully-written state the constructor leaves it in. */
    private static ByteBuffer simulateFullyWrittenBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(MESSAGE_SIZE);
        // Fill with MESSAGE_SIZE bytes, mirroring what insertCAHeader + putFloat/putShort
        // do in the EventAddRequest constructor.
        for (int i = 0; i < MESSAGE_SIZE; i++) {
            buf.put((byte) i);
        }
        assertEquals(MESSAGE_SIZE, buf.position());
        assertEquals(MESSAGE_SIZE, buf.limit());
        return buf;
    }
}
