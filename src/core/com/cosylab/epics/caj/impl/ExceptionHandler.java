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

import java.nio.ByteBuffer;

/**
 * Interface defining exception response handler.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public interface ExceptionHandler {

	/**
	 * Exception response notification.
	 * @param errorCode	exception code.
	 * @param cid	channel ID, can me 0.
	 * @param errorMessage	received detailed message.
	 * @param originalHeaderBuffer	original request header buffer causing this exception.
	 */
	public void handleException(int errorCode, int cid, String errorMessage, ByteBuffer originalHeaderBuffer);

}
