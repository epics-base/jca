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

/**
 * CA constants.
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $id$
 */
public interface CAConstants {

	/**
	 * CA protocol major revision (implemented by this library).
	 */
	public static final short CA_MAJOR_PROTOCOL_REVISION = 4;
	
	/**
	 * CAC (client) protocol minor revision (implemented by this library).
	 */
	public static final short CA_MINOR_PROTOCOL_REVISION = 13;

	/**
	 * CAS (server) protocol minor revision (implemented by this library).
	 */
	public static final short CAS_MINOR_PROTOCOL_REVISION = 11;

	/**
	 * Unknown CA protocol minor revision.
	 */
	public static final short CA_UNKNOWN_MINOR_PROTOCOL_REVISION = 0;

	/**
	 * CA protocol port base.
	 */
	public static final int CA_PORT_BASE = 5056;

	/**
	 * Default CA server port.
	 */
	public static final int CA_SERVER_PORT = CA_PORT_BASE + 2 * CA_MAJOR_PROTOCOL_REVISION;

	/**
	 * Default CA repeater port.
	 */
	public static final int CA_REPEATER_PORT = CA_PORT_BASE + 2 * CA_MAJOR_PROTOCOL_REVISION + 1;

	/**
	 * CA protocol message header size.
	 */
	public static final short CA_MESSAGE_HEADER_SIZE = 16;
	
	/**
	 * CA protocol message extedned header size.
	 */
	public static final short CA_EXTENDED_MESSAGE_HEADER_SIZE = CA_MESSAGE_HEADER_SIZE + 8;

	/**
	 * UDP maximum send message size.
	 * MAX_UDP: 1500 (max of ethernet and 802.{2,3} MTU) - 20(IP) - 8(UDP) 
	 * (the MTU of Ethernet is currently independent of its speed varient)
	 */
	public static final int MAX_UDP_SEND = 1024;

	/**
	 * UDP maximum receive message size.
	 */
	public static final int MAX_UDP_RECV = 0xFFFF + 16;

	/**
	 * TCP maximum receive message size.
	 */
	public static final int MAX_TCP_RECV = 1024 * 16 + CA_EXTENDED_MESSAGE_HEADER_SIZE;

	/**
	 * Default priority (corresponds to POSIX SCHED_OTHER)
	 */
	public static final short CA_DEFAULT_PRIORITY = 0;
	
	/**
	 * Read access right mask.
	 */
	public static final int CA_PROTO_ACCESS_RIGHT_READ = 1 << 0;

	/**
	 * Write access right mask.
	 */
	public static final int CA_PROTO_ACCESS_RIGHT_WRITE = 1 << 1;

	/**
	 * Do not require response for CA search request.
	 */
	public static final short CA_SEARCH_DONTREPLY = 5;

	/**
	 * Require response (even if not found) for CA search request over TCP.
	 */
	public static final short CA_SEARCH_DOREPLY = 10;

	/**
	 * Echo (state-of-health message) reponse timeput in ms.
	 */
	public static final long CA_ECHO_TIMEOUT = 5000;

	/**
     * Max. (requested) string size.
     */
	public static final int MAX_STRING_SIZE = 40;
	
	/**
	 * Unreasonable channel name length.
	 */
	public static final int UNREASONABLE_CHANNEL_NAME_LENGTH = 500;
}
