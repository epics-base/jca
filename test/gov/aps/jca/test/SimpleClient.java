package gov.aps.jca.test;

import com.cosylab.epics.caj.CAJContext;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBR;

public class SimpleClient {

    private static CAJContext context;
    private static Channel channel;

    public static void main(String[] args) throws IllegalStateException, TimeoutException {
        // TODO Auto-generated method stub

        try {
            context = (CAJContext) JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);

            context.printInfo();
            
            context.initialize();
            System.out.println(context.getAddressList());
            channel = context.createChannel("XF:31IDA-OP{Tbl-Ax:X1}Mtr");
            context.pendIO(3.0);
            DBR value = channel.get();
            System.out.println(value);
            channel.destroy();
            
        } catch (CAException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
