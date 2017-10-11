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

package com.cosylab.epics.caj;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.NumberFormat;
import java.text.ParseException;

import gov.aps.jca.CAException;
import gov.aps.jca.JCALibrary;

/**
 * Temporary code that runs JNI CA Repeater, port to java to be done. 
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class JNIRepeater {

    /**
     * System JVM property key to disable JNI repeater. 
     */
    public static final String JNI_DISABLE_REPEATER = "JNI_DISABLE_REPEATER";
    
	static public void repeaterInit() throws CAException {

	    if (System.getProperties().containsKey(JNI_DISABLE_REPEATER))
	        return;
	    
		PrivilegedAction action = new PrivilegedAction() {
			
			public Object run() {
				try {

					String targetArch = JNIRepeater.getTargetArch();
					JCALibrary jca = JCALibrary.getInstance();

					// read JNI native config
					File caRepeaterPath =
						new File(jca.getProperty("gov.aps.jca.jni.epics." + targetArch + ".caRepeater.path", ""));
						
					try {
						String caRepeater = "caRepeater";
						if (caRepeaterPath.exists()) {
							caRepeater = (new File(caRepeaterPath, "caRepeater")).getAbsolutePath();
						}
						Runtime.getRuntime().exec(caRepeater);
					} catch (java.io.IOException ex) {
						Runtime.getRuntime().exec("caRepeater");
					}

				} catch (Throwable ex2) {
					// noop
				}

				return null;
			}
		};

		Object res = AccessController.doPrivileged(action);
		if (res != null)
			throw new CAException("Unable to init JNI CA Repeater", (Throwable) res);
	}


	/**
	 * Get standard "system-arch" string.
	 * @return standard "system-arch" string.
	 */
	static public String getTargetArch() {

		String osname = System.getProperty("os.name", "");

		float osversion = 0;
		try {
			osversion = NumberFormat.getInstance().parse(System.getProperty("os.version", "")).floatValue();
		} catch (ParseException pe) {
			// noop
		}
		
		String osarch = System.getProperty("os.arch", "");

		if (osarch.equals("i386")
			|| osarch.equals("i486")
			|| osarch.equals("i586")) {
			osarch = "x86";
		}
		
		if (osname.equals("SunOS")) {
			if (osversion >= 5) {
				if (osarch.equals("sparc")) {
					return "solaris-sparc";
				} else if (osarch.equals("x86")) {
					return "solaris-x86";
				}
			}
		} else if (osname.equals("Linux")) {
			if (osarch.equals("x86")) {
				return "linux-x86";
			}
		} else if ( osname.equals("Mac OS X") ) {
			return "darwin-ppc";
		}
		else if (osname.startsWith("Win")) {
			return "win32-x86";
		}
		return "unknown";
	}
	
}
