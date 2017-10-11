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

package com.cosylab.epics.caj.impl.handlers;

import java.nio.ByteBuffer;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.ExceptionHandler;
import com.cosylab.epics.caj.impl.ResponseRequest;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class ResponseRequestExceptionHandler implements ExceptionHandler {

	/**
	 * Context instance.
	 */
	protected CAJContext context;

	/**
	 * @param context
	 */
	public ResponseRequestExceptionHandler(CAJContext context) {
		this.context = context;
	}

	/**
	 * @see com.cosylab.epics.caj.impl.ExceptionHandler#handleException(int, int, java.lang.String, java.nio.ByteBuffer)
	 */
	public void handleException(
		int errorCode,
		int cid,
		String errorMessage,
		ByteBuffer originalHeaderBuffer) {

		int ioid = originalHeaderBuffer.getInt(12);
		ResponseRequest rr = (ResponseRequest)context.getResponseRequest(ioid);
		if (rr == null)
			return;
			
		rr.exception(errorCode, errorMessage);					

	}

}
