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

/**
 * CAJ constants.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public interface CAJConstants {

    /**
     * String value of the JVM property key to turn on debugging. 
     */
    public static final String CAJ_DEBUG = "CAJ_DEBUG";

    /**
     * String value of the JVM property key to strip hostname returned by InetAddress.getLocalHost().getHostName(). 
     */
    public static final String CAJ_STRIP_HOSTNAME = "CAJ_STRIP_HOSTNAME";

}
