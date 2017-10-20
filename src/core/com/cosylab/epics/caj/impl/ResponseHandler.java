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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Interface defining response handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public interface ResponseHandler {

	/**
	 * Handle response.
	 * @param responseFrom	remove address of the responder, <code>null</code> if unknown. 
	 * @param transport	response source transport.
	 * @param response	array of response messages to handle.
	 * 					First buffer in array has to contain whole (extended) message header.
	 */
	public void handleResponse(InetSocketAddress responseFrom, Transport transport, ByteBuffer[] response);

}
