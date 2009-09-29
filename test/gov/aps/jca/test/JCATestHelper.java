package gov.aps.jca.test;

import gov.aps.jca.JCALibrary;

public class JCATestHelper {
	
	public static volatile String implementation = JCALibrary.JNI_THREAD_SAFE;
	
	public static void setJNIImplementation(String impl)
	{
		implementation = impl;
	}
	
	public static String getJNIImplementation()
	{
		return implementation;
	}
}
