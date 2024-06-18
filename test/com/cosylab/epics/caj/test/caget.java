package com.cosylab.epics.caj.test;

import gov.aps.jca.JCALibrary;
import gov.aps.jca.Context;
import com.cosylab.epics.caj.CAJContext;
import gov.aps.jca.Channel;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_Double;

/** Bad but simple 'caget' demo
 *  Better implementation would use listeners
 *  instead of fixed wait times,
 *  handle all data types,
 *  and properly close channel and context.
 * 
 *  Example invocation:
 *  java -cp target/classes:target/test-classes -DCAJ_DEBUG=true -Djca.use_env=true com.cosylab.epics.caj.test.caget PV_name
 */
public class caget
{
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1)
        {
            System.err.println("USAGE: caget PV_name");
            System.exit(1);
        }
        final String name = args[0];

        final JCALibrary jca = JCALibrary.getInstance();
        final Context context = new CAJContext();
        final Channel channel = context.createChannel(name);
        context.pendIO(2.0);
        
        DBR data = channel.get(DBRType.DOUBLE, 1);
        context.pendIO(2.0);

        if (data instanceof DBR_Double)
        {
            DBR_Double value = (DBR_Double) data;
            System.out.println(name + " = " + value.getDoubleValue()[0]);
        }
        else
            System.out.println(data);
        System.exit(0);
    }
}
