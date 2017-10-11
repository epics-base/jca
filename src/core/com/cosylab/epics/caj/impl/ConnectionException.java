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
 * Connection exception.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ConnectionException extends Exception {

	static final long serialVersionUID = 2533297215333289327L;

	/**
	 * Failed connection address.
	 */
	private InetSocketAddress address;

	/**
	 * @param message
	 * @param cause
	 */
	public ConnectionException(String message, InetSocketAddress address, Throwable cause) {
		super(message, cause);
		this.address = address;
	}

    /**
     * Get connection addresss.
     * @return connection address.
     */
    public InetSocketAddress getAddress() {
        return address;
    }
}
