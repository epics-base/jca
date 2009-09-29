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

import java.io.PrintStream;

/**
 * Server monitor.
 * 
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id: ServerMonitor.java,v 1.2 2006-08-24 08:13:19 msekoranja Exp $
 */
public class ServerMonitor {

	/**
	 * Channel.
	 */
	protected ServerChannel channel;

	/**
	 * Evenr ID.
	 */
	protected int ioid;

	/**
	 * Create server monitor for given channel.
	 * @param channel	channel.
	 * @param ioid event ID.
	 */
	public ServerMonitor(ServerChannel channel, int ioid)
	{
		if (channel == null)
			throw new IllegalArgumentException("non null channel expected.");

		this.ioid = ioid;
		this.channel = channel;

		// register to the channel
		channel.registerMonitor(this);
	}
	
	/**
	 * Get channel.
	 * @return channel.
	 */
	public ServerChannel getChannel()
	{
		return channel;
	}
	
	/**
	 * Get event ID.
	 * @return event ID.
	 */
	public int getIOID() {
		return ioid;
	}

    /**
	 * Destroy monitor.
	 * This method MUST BE called if this it is overriden.
	 */ 
	public void destroy()
	{ 
		// unregister from the channel
		channel.unregisterMonitor(this);
	}

    /**
	 * Prints detailed information about the monitor to the standard output
	 * stream.
	 * 
	 * @throws java.lang.IllegalStateException
	 *             if the context has been destroyed.
	 */
	 public void printInfo() throws IllegalStateException
	 {
		 printInfo(System.out);
	 }
	
	 /**
 	  * Prints detailed information about the monitor to the specified output
	  * stream.
	  * 
	  * @param out
	  *            the output stream.
	  * @throws java.lang.IllegalStateException
	  *             if the context has been destroyed.
	  */
	  public void printInfo(PrintStream out) throws IllegalStateException
	  {
		  out.println("CLASS        : " + getClass().getName());
		  out.println("CHANNEL      : " + channel);
		  out.println("IOID	        : " + ioid);
	  }


}


