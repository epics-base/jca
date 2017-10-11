package com.cosylab.epics.caj.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.cosylab.epics.caj.CAJContext;
import com.cosylab.epics.caj.impl.requests.VersionRequest;
import com.cosylab.epics.caj.util.Timer;
import com.cosylab.epics.caj.util.Timer.TimerRunnable;

public class CAJNameClient implements TransportClient, TimerRunnable, Request {
	Logger log = Logger.getLogger(CAJNameClient.class.getName());
	
	final CAJContext context;
	final InetSocketAddress addr;
	Transport transport;
	Object reconnect;
	public final ByteBuffer sendBuffer = ByteBuffer.allocateDirect(CAConstants.MAX_UDP_SEND);

	public CAJNameClient(CAJContext ctxt, InetSocketAddress ep) {
		context = ctxt;
		addr = ep;
		initBuffer();
	}

	public void initBuffer() {
		sendBuffer.clear();
	}
	
	public void connect() {
		// initially give minor version=0, the allow client to replace when Version message is received
		Transport trn = context.getTransport(this, addr, (short)0, (short)0);
		if(trn!=null) {
			transport = trn;
			log.fine("Connected to name server "+addr.toString());
		} else {
			transportClosed(); // retry later
		}
	}

	public void cancel() {
		Object recon = reconnect;
		if(recon!=null)
			Timer.cancel(recon);
	}

	// from TimerRunnable	
	
	@Override
	public void timeout(long timeToRun) {
		reconnect = null;
		connect();
	}

	// from TransportClient
	
	@Override
	public void transportUnresponsive() {}

	@Override
	public void transportResponsive(Transport transport) {}

	@Override
	public void transportChanged() {}

	@Override
	public void transportClosed() {
		transport = null;
		//TODO: exponential backoff on retry
		reconnect = context.getTimer().executeAfterDelay(10000, this);
		log.fine("Lost connection to name server "+addr.toString());
	}

	// from Request
	
	@Override
	public byte getPriority() {
		// search requests have low priority
		return Request.MIN_USER_PRIORITY;
	}

	@Override
	public ByteBuffer getRequestMessage() {
		return sendBuffer;
	}

	@Override
	public void submit() throws IOException {
		Transport trn = transport;
		if(trn!=null) {
			trn.submit(this);
		}
	}
}
