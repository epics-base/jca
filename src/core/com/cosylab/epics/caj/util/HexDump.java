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

package com.cosylab.epics.caj.util;

/**
 * Utility for dumping binary data.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public class HexDump {

	/**
	 * Output a buffer in hex format.
	 * @param name	name (desctription) of the message.
	 * @param bs	buffer to dump 
	 */
	public static synchronized void hexDump(String name, byte bs[])
	{
		hexDump(name, bs, 0, bs.length);
	}

	/**
	 * Output a buffer in hex format.
	 * @param name	name (desctription) of the message.
	 * @param bs	buffer to dump
	 * @param len	first bytes (length) to dump. 
	 */
	public static synchronized void hexDump(String name,
											byte bs[],
											int len)
	{
		hexDump( name, bs, 0, len );
	}
	/**
	 * Output a buffer in hex format.
	 * @param name	name (desctription) of the message.
	 * @param bs	buffer to dump
	 * @param start dump message using given offset. 
	 * @param len	first bytes (length) to dump. 
	 */
	public static synchronized void hexDump(String name,
											byte bs[],
											int start, 
											int len)
	{
			StringBuffer out = new StringBuffer("Hexdump ["+name+"] size = "+len);
			StringBuffer chars = new StringBuffer();
	
			for( int i = start; i < (start + len); i++ )
			{
				if(((i-start) % 16 ) == 0)
				{
					out.append(chars);
					out.append("\n");
					chars = new StringBuffer();                    
				}
	
				chars.append( toAscii( bs[i] ));
	
				out.append( toHex( bs[i] ));
	
				if( (i % 4) == 3 ) 
				{ 
					chars.append( ' ' );
					out.append( ' ' ); 
				}
			}
	
			if( len % 16 != 0 )
			{
				int pad = 0;
				int delta_bytes = 16 - (len % 16);
	
				//rest of line (no of bytes)
				//each byte takes two chars plus one ws
				pad = delta_bytes * 3;
	
				//additional whitespaces after four bytes
				pad += (delta_bytes / 4);
	
				for( int i = 0; i < pad; i++ )
				{
					chars.insert( 0, ' ' );
				}
			}
	                
			out.append( chars );
			System.out.println( out );
	}
	
	/**
	 * byte to hexchar mapping.
	 */
	private static final char[] lookup = 
		new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' }; 

	/**
	 * Get hex representation of byte.
	 * @param b
	 * @return string hex representation of byte.
	 */
	private static final String toHex( byte b )
	{
		StringBuffer sb = new StringBuffer();
	
		int upper = (b >> 4) & 0x0F;
		sb.append( lookup[upper] );
	
		int lower = b & 0x0F;
		sb.append( lookup[lower] );
	    
		sb.append( ' ' );
	
		return sb.toString();
	}

	/**
	 * Get ascii representation of byte, dot if non-readable.
	 * @param b
	 * @return ascii representation of byte, dot if non-readable.
	 */
	private static final char toAscii(byte b)
	{
		if( b > (byte) 31 && 
			b < (byte) 127) 
		{
			return (char) b; 
		}
		else 
		{
			return '.';
		}
	}
}
