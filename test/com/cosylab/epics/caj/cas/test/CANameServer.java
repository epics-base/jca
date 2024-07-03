package com.cosylab.epics.caj.cas.test;

import java.net.InetSocketAddress;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableAttachCallback;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCompletion;
import gov.aps.jca.cas.Server;
import gov.aps.jca.cas.ServerContext;

/** Example for a Channel Access name server
 * 
 *  A name server is a CA server that listens to PV searches.
 *  It does not, however, host any PVs.
 *  Instead, it has a list of _other_ CA servers like IOCs
 *  which host the PVs, and the CA name server then provides
 *  the _other_ CA server's address.
 * 
 *  The main purpose is reduction of CA search traffic.
 *  Instead of having clients broadcast their requests,
 *  they can directly contact the CA name server.
 * 
 *  Example usage:
 * 
 *  In one terminal, run an IOC database with a record named "ramp"
 *  under a non-default UDP and TCP port:
 *      export EPICS_CA_SERVER_PORT=9876
 *      softIoc -d test/resources/ramp.db 
 * 
 *  In another terminal, check that
 *      caget ramp
 *  will NOT be able to connect because it searches by default
 *  via UDP port 5064, while the IOC runs on 9876 (UDP and TCP).
 * 
 *  Now run this CANameServer on the same host,
 *      java -cp target/classes:target/test-classes -DCAJ_DEBUG=true com.cosylab.epics.caj.cas.test.CANameServer
 *  and try `caget ramp` again.
 *  The client will reach the name server via UDP 5064.
 *  The name server replies with 127.0.0.1, port 9876
 *  to the search request and client can then reach the IOC.
 * 
 *  The CANameServer also supports searches via TCP:
 *      export EPICS_CA_NAME_SERVERS=127.0.0.1
 *      export EPICS_CA_AUTO_ADDR_LIST=no
 *      caget ramp
 */
public class CANameServer
{
    public static void main(String[] args) throws Exception
    {   // Log as much as possible
        final Logger logger = Logger.getLogger("");
        logger.setLevel(Level.ALL);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(Level.ALL);
        
        // Example for a name server database using some hardcoded values.
        final Map<String, InetSocketAddress> names = new HashMap<>();
        names.put("ramp", new InetSocketAddress("127.0.0.1", 9876));
        names.put("Fred", new InetSocketAddress("1.2.3.4", 4242));
        names.put("Jane", new InetSocketAddress("5.6.7.8", 5757));
                
        JCALibrary jca = JCALibrary.getInstance();        
        Server server = new Server()
        {
            @Override
            public ProcessVariableExistanceCompletion processVariableExistanceTest(String name,
                                                                                   InetSocketAddress client,
                                                                                   ProcessVariableExistanceCallback callback)
                    throws CAException, IllegalArgumentException, IllegalStateException
            {
                System.out.println("Client " + client + " searches for '" + name + "'");
                final InetSocketAddress addr = names.get(name);
                if (addr != null)
                    return ProcessVariableExistanceCompletion.EXISTS_ELSEWHERE(addr);
                else
                    return ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE;
            }

            @Override
            public ProcessVariable processVariableAttach(String name,
                                                         ProcessVariableEventCallback event,
                                                         ProcessVariableAttachCallback attach)
                    throws CAStatusException, IllegalArgumentException, IllegalStateException
            {
                throw new CAStatusException(CAStatus.NOSUPPORT, "not supported");
            }
        };
        
        final ServerContext context = jca.createServerContext(JCALibrary.CHANNEL_ACCESS_SERVER_JAVA, server);
        context.run(0);
    }
}