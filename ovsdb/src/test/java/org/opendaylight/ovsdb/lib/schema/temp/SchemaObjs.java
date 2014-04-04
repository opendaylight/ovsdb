/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.schema.temp;

import org.opendaylight.ovsdb.lib.OvsDBClient;
import org.opendaylight.ovsdb.lib.OvsDBClientImpl;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import java.util.concurrent.ExecutionException;


public class SchemaObjs {

    public static class Bridge extends TableSchema<Bridge> {
        public static String NAME = "Bridge";
        TableSchema target;

        ColumnSchema<Bridge, String> name;
        ColumnSchema<Bridge, Boolean> flood_vlans;


        public Bridge(TableSchema<Bridge> target) {
            this.target = target;
            name = target.column("name", String.class);
            flood_vlans = target.column("statistics", Boolean.class);
        }

    }

    public static class Port extends TableSchema<Port> {
        public static String NAME = "Port";
        TableSchema target;

        ColumnSchema<Port, String> name;
        ColumnSchema<Port, String> statistics;

        public Port(TableSchema<Port> target) {
            this.target = target;
            name = target.column("name", String.class);
            statistics = target.column("statistics", String.class);
        }

    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        OvsDBClientImpl ovs = new OvsDBClientImpl(null, null);
        DatabaseSchema db = ovs.getSchema(OvsDBClient.OPEN_VSWITCH_SCHEMA, true).get();
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
