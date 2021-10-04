package org.epics.jca;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.cas.util.MemoryProcessVariable;
import gov.aps.jca.CAException;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.configuration.DefaultConfiguration;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_String;
import gov.aps.jca.event.MonitorEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class DynamicPortTest {
    private EmbeddedIoc ioc;
    private MonitorClient client;
    private String[] pvs;

    @Before
    public void setup() throws CAException {
        ioc = new EmbeddedIoc();
        client = new MonitorClient();

        pvs = new String[]{"channel1", "channel2"};

        for(String pv: pvs) {
            ioc.registerPv(new MemoryProcessVariable(pv, null, DBRType.STRING, new String[]{"Hello from " + pv + "!"}));
        }

        DefaultConfiguration config = new DefaultConfiguration("config");
        config.setAttribute("class", JCALibrary.CHANNEL_ACCESS_JAVA);
        config.setAttribute("auto_addr_list", "NO");
        config.setAttribute("addr_list", ioc.getAddress());

        long pollWaitMillis = 1000;
        long pendIOTimeoutMillis = 2000;

        ioc.start();
        client.start(config, pvs, pollWaitMillis, pendIOTimeoutMillis);
    }

    @After
    public void tearDown() throws CAException {
        client.stop();
        ioc.stop();
    }

    @Test
    public void basicTest() throws InterruptedException, CAException, TimeoutException {
        List<MonitorEvent> events = client.poll(); // Grabs most recent update, if any

        int actualCount = events.size();
        String actualC2Value = null;

        for(MonitorEvent event: events) {
            CAJChannel channel = (CAJChannel)event.getSource();

            if("channel2".equals(channel.getName())) {
                DBR_String dbr = (DBR_String)event.getDBR();
                String[] strArray = ((gov.aps.jca.dbr.STRING) dbr).getStringValue();
                actualC2Value = strArray[0];
            }
        }

        Assert.assertEquals(2, actualCount);
        Assert.assertEquals("Hello from channel2!", actualC2Value);
    }
}