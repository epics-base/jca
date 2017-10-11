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

package com.cosylab.epics.caj.impl.requests;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public abstract class AbstractCARequest implements Request {

	/**
	 * Empty (dummy) message.
	 */
	private static final ByteBuffer EMPTY_MESSAGE = ByteBuffer.allocate(0);

	/**
	 * Request message.
	 */
	protected ByteBuffer requestMessage = EMPTY_MESSAGE;

	/**
	 * Transport to be used when sending.
	 */
	protected Transport transport;
	
	/**
	 * @param transport	transport to be used when sending.
	 */
	public AbstractCARequest(Transport transport) {
		this.transport = transport;
	}

	/**
	 * Return default priority.
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.DEFAULT_USER_PRIORITY;
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getRequestMessage()
	 */
	public ByteBuffer getRequestMessage()
	{
		if (requestMessage == null)
			requestMessage = generateRequestMessage();
		return requestMessage;
	}
	
	/**
	 * Generates (new instance) new request message.
	 * Used if <code>requestMessage</code> not yet set - lazy message generation.
	 * @return generated request message.
	 * @see com.cosylab.epics.caj.impl.Request#getRequestMessage()
	 */
	public ByteBuffer generateRequestMessage()
	{
		// noop
		return null;
	}

	/**
	 * Submit request to the corresponding transport. 
	 * @see com.cosylab.epics.caj.impl.Request#submit()
	 */
	public void submit() throws IOException
	{
		transport.submit(this);
	}

	/**
	 * Inserts CA header to the buffer.
	 * @param transport	transport to be used when sending.
	 * @param buffer	buffer to be filled, can be <code>null</code>.
	 * @return	filled buffer, if given buffer size is less that header size,
	 * 			then new buffer is allocated and returned.
	 */
	public static ByteBuffer insertCAHeader(Transport transport, ByteBuffer buffer,
											short command, int payloadSize,
											short dataType, int dataCount,
											int parameter1, int parameter2)
	{ 
		boolean useExtendedHeader = payloadSize >= 0xFFFF || dataCount >= 0xFFFF;
		
		// check if supported by current transport protocol revision
		if (useExtendedHeader && transport != null && transport.getMinorRevision() < 9) 
			throw new IllegalArgumentException("Out of bounds.");

		int requiredSize = useExtendedHeader ? 
								CAConstants.CA_EXTENDED_MESSAGE_HEADER_SIZE : CAConstants.CA_MESSAGE_HEADER_SIZE;
			
		// allocate buffer, if necessary
		if (buffer == null || (buffer.limit() - buffer.position() < requiredSize))
			buffer = ByteBuffer.allocate(requiredSize);
			
		// standard header
		if (!useExtendedHeader)
		{
			buffer.putShort(command);
			// conversion int -> unsigned short is done right
			buffer.putShort((short)payloadSize);
			buffer.putShort(dataType);
			// conversion int -> unsigned short is done right
			buffer.putShort((short)dataCount);
			buffer.putInt(parameter1);
			buffer.putInt(parameter2);
		}
		// extended header 
		else
		{
			buffer.putShort(command);
			buffer.putShort((short)0xFFFF);
			buffer.putShort(dataType);
			buffer.putShort((short)0x0000);
			buffer.putInt(parameter1);
			buffer.putInt(parameter2);
			buffer.putInt(payloadSize);
			buffer.putInt(dataCount);
		}

		return buffer;
	}

	/**
	 * Fills buffer to become aligned. <code>ByteBuffer.position()</code> is aligned.
	 * @param align		alignment to be used
	 * @param buffer	buffer to be filled, non-<code>null</code>.
	 * @return	filled buffer, if given buffer size is less that aligned size,
	 * 			then new buffer is allocated and returned.
	 */
	public static ByteBuffer alignBuffer(int align, ByteBuffer buffer)
	{ 
		int alignedBufferSize = calculateAlignedSize(align, buffer.position());
		if (alignedBufferSize > buffer.capacity())
		{
			ByteBuffer nbuffer = ByteBuffer.allocate(alignedBufferSize);
			nbuffer.put(buffer);
			buffer = nbuffer;
		}

		// fill w/ zeros
		alignedBufferSize -= buffer.position();
		for (int i = 0; i < alignedBufferSize; i++)
			buffer.put((byte)0);
			
		return buffer;
	}

	/**
	 * Calculates aligned sice.
	 * @param align		alignment to be used
	 * @param nonAlignedSize	current non-aligned size
	 * @return aligned size.
	 */
	public static int calculateAlignedSize(int align, int nonAlignedSize)
	{
		return ((nonAlignedSize+align-1)/align)*align;
	}
}
