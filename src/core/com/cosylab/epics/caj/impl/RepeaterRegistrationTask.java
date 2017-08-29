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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.requests.RepeaterRegisterRequest;
import com.cosylab.epics.caj.util.Timer;

/**
 * Repeater registration tasks.
 * Periodically issues reppeater registration requests until registration is not confirmed.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class RepeaterRegistrationTask implements Timer.TimerRunnable {

	private CAJContext context;
	private RepeaterRegisterRequest registrationRequest; 
	private InetSocketAddress repeaterAddress;
	private Object timerID;
	
	/**
	 * @param context
	 * @param repeaterAddress
	 * @throws UnknownHostException
	 */
	public RepeaterRegistrationTask(CAJContext context, 
									InetSocketAddress repeaterAddress) throws UnknownHostException
	{
		this.context = context;
		this.repeaterAddress = repeaterAddress;
		 
		this.registrationRequest = new RepeaterRegisterRequest(context.getBroadcastTransport());
	}

	/**
	 * @see com.cosylab.epics.caj.util.Timer.TimerRunnable#timeout(long)
	 */
	public void timeout(long timeToRun) {
		// repeater confirmed registration, cancel timer
		if (context.isRegistrationConfirmed())
			Timer.cancel(timerID);
		else
			registrationRequest();
	}

	/**
	 * Schedules registration requests every <code>period</code> ms.
	 * @param period	period in ms.
	 */
	public void runInBackground(int period)
	{
		timerID = context.getTimer().executePeriodically(period, this, 0);
	}

	/**
	 * Issue registration request to repeater.
	 */
	public void registrationRequest()
	{
		context.getLogger().log(Level.FINE, "Repeater registation request sent to " + repeaterAddress + ".");
		try
		{
			context.getBroadcastTransport().send(registrationRequest, repeaterAddress);
		}
		catch (IOException ioex)
		{
			context.getLogger().log(Level.FINE, "Failed to send registation request.", ioex);
		}
	}

}
