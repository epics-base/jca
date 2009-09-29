/*
 * Copyright (c) 2006 by Cosylab
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

package gov.aps.jca.cas;

import gov.aps.jca.CAStatus;

/**
 * Process variable write callback.
 * @author Matej Sekoranja (matej.sekoranja@cosylab.com)
 * @version $Id: ProcessVariableWriteCallback.java,v 1.3 2006-08-29 09:51:19 msekoranja Exp $
 */
public interface ProcessVariableWriteCallback extends CompletionCallback
{
	   
	/**
	 * Notify about write completion.
	 * 
	 * @param status CA status.
	 */
	public void processVariableWriteCompleted(CAStatus status);

}
