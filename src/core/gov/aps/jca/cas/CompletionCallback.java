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
 * Asynchronous operation completion callback root interface.
 * @author Matej Sekoranja (matej.sekoranja@cosylab.com)
 * @version $Id: CompletionCallback.java,v 1.1 2006-03-06 17:16:04 msekoranja Exp $
 */
interface CompletionCallback
{
	   
	/**
	 * Notify about asynchronous operation cancellation.
	 */
	public void canceled();

}
