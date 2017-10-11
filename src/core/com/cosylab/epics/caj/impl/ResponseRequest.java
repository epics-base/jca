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

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public interface ResponseRequest {

	/**
	 * Get I/O ID.
	 * @return ioid
	 */
	public int getIOID();

	/**
	 * Timeout notification.
	 */
	public void timeout();

	/**
	 * Cancel response request (allways to be called to complete/destroy).
	 */
	public void cancel();
	
	/**
	 * Exception response notification.
	 * @param errorCode	exception code.
	 * @param errorMessage	received detailed message.
	 */
	public void exception(int errorCode, String errorMessage);
	
}
