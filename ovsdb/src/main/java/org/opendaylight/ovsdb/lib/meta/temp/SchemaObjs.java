package org.opendaylight.ovsdb.lib.meta.temp;

import org.opendaylight.ovsdb.OpenVswitch;
import org.opendaylight.ovsdb.lib.meta.DatabaseSchema;
import org.opendaylight.ovsdb.lib.meta.ColumnSchema;
import org.opendaylight.ovsdb.lib.meta.TableSchema;

/**
 * @author araveendrann
 */
public class SchemaObjs {

    public static class Bridge extends TableSchema<Bridge> {
        public static String NAME = "Bridge";
        TableSchema target;

        ColumnSchema<Bridge, String> name;
        ColumnSchema<Bridge, Boolean> flood_vlans;


        public Bridge(TableSchema<Bridge> target) {
            this.target = target;
            name = target.column("name");
            flood_vlans = target.column("statistics");
        }

    }

    public static class Port extends TableSchema<Port> {
        public static String NAME = "Port";
        TableSchema target;

        ColumnSchema<Port, String> name;
        ColumnSchema<Port, String> statistics;

        public Port(TableSchema<Port> target) {
            this.target = target;
            name = target.column("name");
            statistics = target.column("statistics");
        }

    }

    public static void main(String[] args) {

        OpenVswitch ovs = new OpenVswitch(null, null);
        DatabaseSchema db = ovs.getSchema();
        Bridge bridge = db.table(Bridge.NAME, Bridge.class);
        Port port = db.table(Port.NAME, Port.class);

        db.beginTransaction()
                .add(bridge.insert()
                        .value(bridge.flood_vlans, true)
                        .value(bridge.name, "br-int"))
                .add(port.insert()
                        .value(port.statistics, "stats")
                        //.value(port.statistics, 2) ## will not type check as stats is a string
                        .value(port.name, "newport")
                )
                .execute();

        //todo untyped version

    }
}
