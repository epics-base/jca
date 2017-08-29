package gov.aps.jca.test;

import gov.aps.jca.JCALibrary;

public class JCATestHelper {
	
	public static volatile String implementation = JCALibrary.CHANNEL_ACCESS_JAVA;
	
	public static void setJNIImplementation(String impl)
	{
		implementation = impl;
	}
	
	public static String getJNIImplementation()
	{
		return implementation;
	}
}
