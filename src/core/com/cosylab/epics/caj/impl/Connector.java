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

/**
 * Interface defining socket connector (Connector-Transport pattern). 
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public interface Connector {
	
	/**
	 * Connect.
	 * @param client	client requesting connection (transport).
	 * @param address			address of the server.
	 * @param responseHandler	reponse handler.
	 * @param transportRevision	transport revision to be used.
	 * @param priority process priority.
	 * @return transport instance.
	 * @throws ConnectionException
	 */
	public Transport connect(TransportClient client, ResponseHandler responseHandler, 
							 InetSocketAddress address, short transportRevision, short priority) throws ConnectionException;
	
}
