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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.CABeaconHandler;
import com.cosylab.epics.caj.impl.Transport;
import com.cosylab.epics.caj.util.InetAddressUtil;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class BeaconResponse extends AbstractCAJResponseHandler {

	/**
	 * @param context
	 */
	public BeaconResponse(CAJContext context) {
		super(context, "Beacon");
	}

	/**
	 * @see com.cosylab.epics.caj.impl.handlers.AbstractCAJResponseHandler#internalHandleResponse(java.net.InetSocketAddress, com.cosylab.epics.caj.impl.Transport, java.nio.ByteBuffer[])
	 */
	protected void internalHandleResponse(
		InetSocketAddress responseFrom,
		Transport sourceTransport,
		ByteBuffer[] response) {
		// NOTE: sequental IDs are implemented from v4.10+
		
		long timestamp = System.currentTimeMillis();
		
		// old version servers do not supply port,
		// set default one
		if (dataCount == 0)
			dataCount = (short)context.getServerPort();
		
		InetAddress addr = InetAddressUtil.intToIPv4Address(parameter2);
		responseFrom = new InetSocketAddress(addr, dataCount);

		CABeaconHandler beaconHandler = context.getBeaconHandler(responseFrom);
		// currently we care only for servers used by this context  
		if (beaconHandler == null)
			return;

		// convert unsigned int to signed long
		long sequentalID =  parameter1 & 0x00000000FFFFFFFFL;

		// dataType contains minor protocol revision
		// notify beacon handler
		beaconHandler.beaconNotify(dataType, timestamp, sequentalID);
	}

}
