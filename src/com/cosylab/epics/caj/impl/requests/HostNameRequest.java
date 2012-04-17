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

package com.cosylab.epics.caj.impl.requests;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.CAJConstants;
import com.cosylab.epics.caj.impl.CAConstants;
import com.cosylab.epics.caj.impl.Request;
import com.cosylab.epics.caj.impl.Transport;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class HostNameRequest extends AbstractCARequest {

	private static String hostName = null;
	private static final String HOSTNAME_KEY = "HOSTNAME";
	
	private static synchronized String getHostName() 
	{
		if (hostName == null)
		{
			// default fallback
			hostName = "localhost";
			
			try {
				InetAddress localAddress = InetAddress.getLocalHost();
				hostName = localAddress.getHostName();
			} catch (Throwable uhe) {	// not only UnknownHostException
				// try with environment variable
				try {
					String envHN = System.getenv(HOSTNAME_KEY);
					if (envHN != null)
						hostName = envHN;
				} catch (Throwable th) {
					// in case not supported by JVM/OS
				}
				
				// and system property (overrides env. var.)
				hostName = System.getProperty(HOSTNAME_KEY, hostName);
			}
			
			if (System.getProperties().contains(CAJConstants.CAJ_STRIP_HOSTNAME))
			{
				int dotPos = hostName.indexOf('.');
				if (dotPos > 0)
					hostName = hostName.substring(0, dotPos);
			}
		}
		
		return hostName;
	}
	
	/**
	 * @param transport
	 */
	public HostNameRequest(Transport transport) {
		super(transport);
		
		// compatibility check
		if (transport.getMinorRevision() < 1)
			return;

		String hostName = getHostName();
		
		int alignedMessageSize = calculateAlignedSize(8, CAConstants.CA_MESSAGE_HEADER_SIZE + hostName.length() + 1);
		requestMessage = ByteBuffer.allocate(alignedMessageSize);
		requestMessage = insertCAHeader(transport, requestMessage,
										(short)21, (short)(alignedMessageSize - CAConstants.CA_MESSAGE_HEADER_SIZE),
										(short)0, (short)0,
										0, 0);
		// append zero-terminated string and align message
		requestMessage.put(hostName.getBytes());
		requestMessage.put((byte)0);
		requestMessage = alignBuffer(8, requestMessage);
	}

	/**
	 * @see com.cosylab.epics.caj.impl.Request#getPriority()
	 */
	public byte getPriority() {
		return Request.SEND_IMMEDIATELY_PRIORITY;
	}

}
