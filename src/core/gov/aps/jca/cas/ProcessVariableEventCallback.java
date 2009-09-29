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

import gov.aps.jca.dbr.DBR;

/**
 * Process variable event callback.
 * @author Matej Sekoranja (matej.sekoranja@cosylab.com)
 * @version $Id: ProcessVariableEventCallback.java,v 1.2 2006-03-10 13:54:25 msekoranja Exp $
 */
public interface ProcessVariableEventCallback extends CompletionCallback
{
	   
	/**
	 * Notify about process variable event.
	 * @param select <code>Monitor.[mask]</code> event type.
	 * @param event
	 *            event of type DBR_TIME_<type> where type is of
	 *            <code>ProcessVariable.getType()</code>.
	 */
	public void postEvent(int select, DBR event);

}
