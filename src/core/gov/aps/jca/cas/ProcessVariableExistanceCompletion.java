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

import gov.aps.jca.Enum;

/**
 * Process variable existance completion enum class.
 * @author Matej Sekoranja (matej.sekoranja@cosylab.com)
 * @version $Id: ProcessVariableExistanceCompletion.java,v 1.1 2006-03-06 17:16:04 msekoranja Exp $
 */
public final class ProcessVariableExistanceCompletion extends Enum
{
	   
	/**
	 * Process variable exists.
	 */
	public static final ProcessVariableExistanceCompletion EXISTS_HERE = new ProcessVariableExistanceCompletion("EXISTS_HERE");

	/**
	 * Process variable does not exist.
	 */
	public static final ProcessVariableExistanceCompletion DOES_NOT_EXIST_HERE = new ProcessVariableExistanceCompletion("DOES_NOT_EXIST_HERE");

	/**
	 * Deffered result (asynchronous operation),
	 * <code>ProcessVariableExistanceCompletionCallback.processVariableExistanceTestCompleted()</code> callback method method should be called to return completion.
	 */
	public static final ProcessVariableExistanceCompletion ASYNC_COMPLETION = new ProcessVariableExistanceCompletion("ASYNC_COMPLETION");

	/**
	 * Creates a new PV existance completion.
	 * Contructor is <code>protected</code> to deny creation of unsupported types.
	 * @param name	name of the completion.
	 */
	protected ProcessVariableExistanceCompletion(String name) {
		super(name);
	}


}
