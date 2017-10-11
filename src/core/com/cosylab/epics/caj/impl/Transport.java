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
import java.net.InetSocketAddress;

import com.cosylab.epics.caj.impl.CAContext;

/**
 * Interface defining transport (connection).
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public interface Transport {

	/**
	 * Get remote address.
	 * @return remote address.
	 */
	public InetSocketAddress getRemoteAddress();

	/**
	 * Get context transport is living in.
	 * @return context transport is living in.
	 */
	public CAContext getContext();
	
	/**
	 * Transport protocol minor revision.
	 * @return protocol minor revision.
	 */
	public short getMinorRevision();
	
	/**
	 * Transport priority.
	 * @return protocol priority.
	 */
	public short getPriority();

	/**
	 * Enqueue (if supported or if necessary send immediately) request message.
	 * Message is sent immediately if its priority equals <code>SEND_IMMEDIATELY_PRIORITY</code>.
	 * @param requestMessage	request message to enqueue.
	 */
	public void submit(Request requestMessage) throws IOException;
	
	/**
	 * Enqueue (if necessary submit) request message.
	 * @return <code>true</code> on success, <code>false</code> on failure.
	 */
	public boolean flush();

}
