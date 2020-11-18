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

public class MultipleServersTest {
    private EmbeddedIoc ioc1;
    private EmbeddedIoc ioc2;
    private MonitorClient client;
    private String[] pvs;

    @Before
    public void setup() throws CAException {
        ioc1 = new EmbeddedIoc(5064);
        ioc2 = new EmbeddedIoc(5064);
        client = new MonitorClient();

        pvs = new String[]{"channel1", "channel2"};

        ioc1.registerPv(new MemoryProcessVariable("channel1", null, DBRType.STRING, new String[]{"Hello from channel1!"}));
        ioc2.registerPv(new MemoryProcessVariable("channel2", null, DBRType.STRING, new String[]{"Hello from channel2!"}));

        DefaultConfiguration config = new DefaultConfiguration("config");
        config.setAttribute("class", JCALibrary.CHANNEL_ACCESS_JAVA);
        config.setAttribute("auto_addr_list", "NO");
        config.setAttribute("addr_list", ioc1.getAddress() + " " + ioc2.getAddress());

        long pollWaitMillis = 1000;
        long pendIOTimeoutMillis = 2000;

        ioc1.start();
        ioc2.start();
        client.start(config, pvs, pollWaitMillis, pendIOTimeoutMillis);
    }

    @After
    public void tearDown() throws CAException {
        client.stop();
        ioc1.stop();
        ioc2.stop();
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
