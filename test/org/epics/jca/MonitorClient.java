package org.epics.jca;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import gov.aps.jca.CAException;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.configuration.Configuration;
import gov.aps.jca.event.MonitorEvent;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MonitorClient {
    private Map<String, MonitorEvent> latest = new ConcurrentHashMap<>();

    private static final JCALibrary JCA_LIBRARY = JCALibrary.getInstance();
    private Configuration config = null;
    private CAJContext context;
    private String[] pvs;
    private long pollWaitMillis;
    private float pendIOTimeoutSeconds;

    public void start(Configuration config, String[] pvs, long pollWaitMillis, long pendIOTimeoutMillis) {
        this.config = config;
        this.pvs = pvs;
        this.pollWaitMillis = pollWaitMillis;
        this.pendIOTimeoutSeconds = pendIOTimeoutMillis / 1000.0f;
    }

    public List<MonitorEvent> poll() throws CAException, TimeoutException, InterruptedException {
        if(context == null) {
            createContext();
        }

        synchronized (this) {
            wait(pollWaitMillis); // Max update frequency; too fast is taxing and unnecessary work; too slow means delayed monitor updates
        }

        List<MonitorEvent> events = null; // Return null if no updates

        if(!latest.isEmpty()) {
            events = new ArrayList<>(latest.values());
            latest.clear();
        }

        return events;
    }

    public void createContext() throws CAException, TimeoutException {
        context = (CAJContext) JCA_LIBRARY.createContext(config);

        List<CAJChannel> channels = new ArrayList<>();

        for(String pv: pvs) {
            CAJChannel channel = (CAJChannel) context.createChannel(pv);
            channels.add(channel);
        }

        context.pendIO(pendIOTimeoutSeconds);

        for(CAJChannel channel: channels) {
            channel.addMonitor(Monitor.VALUE, ev -> latest.put(channel.getName(), ev));
        }

        context.pendIO(pendIOTimeoutSeconds);
    }

    public void stop() throws CAException {
        context.destroy();
    }
}
