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

package com.cosylab.epics.caj.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Interface defining request to be sent via <code>Transport</code>.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public interface Request {

	/**
	 * Highest priority, message is sent immediately.
	 */
	public static final short SEND_IMMEDIATELY_PRIORITY = 100; 

	/**
	 * Maximum user message priority.
	 */
	public static final short MAX_USER_PRIORITY = SEND_IMMEDIATELY_PRIORITY - 1; 

	/**
	 * Minimum user message priority.
	 */
	public static final short MIN_USER_PRIORITY = 0; 

	/**
	 * Default (recommended) user message priority.
	 */
	public static final short DEFAULT_USER_PRIORITY = (MAX_USER_PRIORITY + MIN_USER_PRIORITY) / 2; 

	/**
	 * Get request priority, higher view higher priority.
	 * @return request priority.
	 */
	public byte getPriority();
	
	/**
	 * Get request message to be sent.
	 * @return	request message to be sent.
	 */
	public ByteBuffer getRequestMessage();

	/**
	 * Submit this request message to the corresponding transport.
	 * @throws IOException
	 */
	public void submit() throws IOException;

}
