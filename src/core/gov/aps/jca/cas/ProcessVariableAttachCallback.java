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

/**
 * Asynchronous operation completion callback for process variable attach.
 * @author Matej Sekoranja (matej.sekoranja@cosylab.com)
 * @version $Id: ProcessVariableAttachCallback.java,v 1.2 2006-03-10 13:54:25 msekoranja Exp $
 */
public interface ProcessVariableAttachCallback extends CompletionCallback
{
	   
	/**
	 * Notify about process variable existance.
	 * @param completion process variable existance status.
	 */
	public void processVariableAttachCompleted(ProcessVariable processVariable);

}
