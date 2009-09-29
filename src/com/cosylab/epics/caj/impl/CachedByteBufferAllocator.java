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

package com.cosylab.epics.caj.impl;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import gov.aps.jca.JCALibrary;

/**
 * A buffer allocator which allocates and caches NIO (direct) byte buffers.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class CachedByteBufferAllocator {

    /**
     * This variable stores the size of the NIO byte buffer that should be allocated
     * according to properties.
     */
    // TODO determine (if possible)...
    public static int bufferSize = 16000;
    
    static {
        String strSize = JCALibrary.getInstance().getProperty(CachedByteBufferAllocator.class.getName() + ".buffer_size");
        if (strSize != null)
        {
	        try 
	        {
	            bufferSize = Integer.parseInt(strSize);
	        } catch (NumberFormatException nfe) { /* noop */ }
        }
    }

    /**
     * Cache for the byte buffers.
     */    
    private LinkedList cache = new LinkedList();

    /**
     * Get a byte buffer.
     * @return a byte buffer.
     */    
    public ByteBuffer get()
    {
        ByteBuffer buffer = null;
        
        synchronized (cache)
        {
            if (cache.size() > 0)
            {
                buffer = (ByteBuffer)cache.removeFirst();
                buffer.clear();
            }
        }

        if (buffer == null) {
            buffer = ByteBuffer.allocate(bufferSize); 
        } 

        return buffer;
    }

    /**
     * Return buffer to a cache.
     * @param buffer buffer to be returned in the cache.
     */    
    public void put(ByteBuffer buffer)
    {
        synchronized (cache)
        {
            // LIFO to maximize CPU cache affinity
            cache.addFirst(buffer);
        }
    }

}
