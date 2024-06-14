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

import java.net.InetSocketAddress;

/**
 * Process variable existance completion class.
 * @author Matej Sekoranja (matej.sekoranja@cosylab.com)
 */
public final class ProcessVariableExistanceCompletion
{
	private final String name;
	private final InetSocketAddress addr;

	/**
	 * Process variable exists.
	 */
	public static final ProcessVariableExistanceCompletion EXISTS_HERE = new ProcessVariableExistanceCompletion("EXISTS_HERE", null);

	/**
	 * Process variable exists at a different address, not this CA server.
	 */
	public static ProcessVariableExistanceCompletion EXISTS_ELSEWHERE(final InetSocketAddress addr)
	{
		return new ProcessVariableExistanceCompletion("EXISTS_ELSEWHERE", addr);
	}

	/** @return Does the PV exist elsewhere? */
	public boolean doesExistElsewhere()
	{
		return addr != null;
	}

	/** @return Other address where PV does exist or null */
	public InetSocketAddress getOtherAddress()
	{
		return addr;
	}

	/**
	 * Process variable does not exist.
	 */
	public static final ProcessVariableExistanceCompletion DOES_NOT_EXIST_HERE = new ProcessVariableExistanceCompletion("DOES_NOT_EXIST_HERE", null);

	/**
	 * Deffered result (asynchronous operation),
	 * <code>ProcessVariableExistanceCompletionCallback.processVariableExistanceTestCompleted()</code> callback method method should be called to return completion.
	 */
	public static final ProcessVariableExistanceCompletion ASYNC_COMPLETION = new ProcessVariableExistanceCompletion("ASYNC_COMPLETION", null);

	/**
	 * Creates a new PV existance completion.
	 * Contructor is <code>protected</code> to deny creation of unsupported types.
	 * @param name	name of the completion.
	 * @param addr  Address if exists elsewhere, otherwise null
	 */
	protected ProcessVariableExistanceCompletion(final String name, final InetSocketAddress addr) {
		this.name = name;
		this.addr = addr;
	}

	@Override
	public String toString()
	{
		if (doesExistElsewhere())
			return name + "@" + addr;
		return name;
	}

}
