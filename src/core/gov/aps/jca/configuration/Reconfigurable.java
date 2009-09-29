/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package gov.aps.jca.configuration;

/**
 * Extends <code>Configurable</code> to allow reconfiguration at runtime.
 *
 * @author <a href="mailto:fede@apache.org">Federico Barbieri</a>
 * @author <a href="mailto:pier@apache.org">Pierpaolo Fumagalli</a>
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="mailto:peter at apache.org">Peter Donald</a>
 * @version 1.0
 */
public interface Reconfigurable
    extends Configurable
{
    /**
     * Describe <code>reconfigure</code> method here.
     *
     * @param configuration a <code>Configuration</code> value
     * @throws ConfigurationException if an error occurs
     */
    void reconfigure( Configuration configuration )
        throws ConfigurationException;
}

