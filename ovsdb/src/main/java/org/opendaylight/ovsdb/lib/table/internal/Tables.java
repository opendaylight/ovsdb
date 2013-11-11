package org.opendaylight.ovsdb.lib.table.internal;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.ovsdb.lib.table.*;

public class Tables {
    public static List<Table> tables = new ArrayList<Table>();
    static {
        tables.add(new Bridge());
        tables.add(new Port());
        tables.add(new Capability());
        tables.add(new Interface());
        tables.add(new Controller());
        tables.add(new Manager());
        tables.add(new Mirror());
        tables.add(new NetFlow());
        tables.add(new Open_vSwitch());
        tables.add(new Qos());
        tables.add(new Queue());
        tables.add(new SFlow());
        tables.add(new SSL());
        tables.add(new Flow_Sample_Collector_Set());
        tables.add(new Flow_Table());
        tables.add(new IPFIX());
    }
    public static List<Table> getTables() {
        return tables;
    }
}
