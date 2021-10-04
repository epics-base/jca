package org.epics.jca;

import com.cosylab.epics.caj.cas.CAJServerContext;
import com.cosylab.epics.caj.cas.util.DefaultServerImpl;
import gov.aps.jca.CAException;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.cas.ProcessVariable;

public class EmbeddedIoc {
    private JCALibrary jca;
    private DefaultServerImpl server;
    private CAJServerContext context;

    public EmbeddedIoc() throws CAException {
        this(0); // 0 means dynamically assign
    }

    public EmbeddedIoc(int udpPort) throws CAException {
        jca = JCALibrary.getInstance();

        server = new DefaultServerImpl();
        context = new CAJServerContext();
        context.setUdpServerPort(udpPort);
        context.initialize(server);
    }

    /**
     * The address to the server's UDP pv name search socket as one would use in a client addr_list.
     *
     * @return The server's pv name search UDP socket address
     */
    public String getAddress() {
        return "localhost:" + context.getUdpServerPort();
    }

    public void registerPv(ProcessVariable pv) {
        server.registerProcessVariable(pv);
    }

    public ProcessVariable unregisterPv(String name) {
        return server.unregisterProcessVariable(name);
    }

    public void start() {
        new Thread(() -> {
            try {
                context.run(0);
            } catch (CAException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void stop() throws CAException {
        context.destroy();
    }
}