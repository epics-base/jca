package com.cosylab.epics.caj.cas.test;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableAttachCallback;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCompletion;
import gov.aps.jca.cas.Server;

import java.net.InetSocketAddress;

/**
 * Dummy server implementation.
 */
class DummyServerImpl implements Server
{

	public ProcessVariable processVariableAttach(String aliasName, ProcessVariableEventCallback eventCallback,
												 ProcessVariableAttachCallback asyncCompletionCallback) throws CAStatusException, IllegalArgumentException, IllegalStateException {
		throw new CAStatusException(CAStatus.NOSUPPORT, "not supported");
	}

	public ProcessVariableExistanceCompletion processVariableExistanceTest(String aliasName, InetSocketAddress clientAddress, ProcessVariableExistanceCallback asyncCompletionCallback) throws CAException, IllegalArgumentException, IllegalStateException {
		return ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE;
	}
	
}