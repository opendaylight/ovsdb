/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

public class OvsdbSchemaContants {

    public static final String DATABASE_NAME = "Open_vSwitch";
    private static final String BRIDGE_NAME = "Bridge";

    public enum OvsdbSchemaTables {
        OPENVSWITCH(DATABASE_NAME,null,null),
        BRIDGE(BRIDGE_NAME, DATABASE_NAME, "bridges"),
        PORT("Port", BRIDGE_NAME, "ports"),
        INTERFACE("Interface", "Port", "interfaces"),
        SSL("SSL", DATABASE_NAME, "ssl"),
        IPFIX("IPFIX", BRIDGE_NAME, "ipfix"),
        SFLOW("sFlow", BRIDGE_NAME, "sflow"),
        FLOWTABLE("Flow_Table", BRIDGE_NAME, "flow_tables"),
        QOS("QoS", "Port", "qos"),
        NETFLOW("NetFlow", BRIDGE_NAME, "netflow"),
        MIRROR("Mirror", BRIDGE_NAME, "mirrors"),
        MANAGER("Manager", DATABASE_NAME, "manager_options"),
        CONTROLLER("Controller", BRIDGE_NAME, "controller"),
        FLOWSAMPLECOLLECTORSET("Flow_Sample_Collector_Set",null,null),
        AUTOATTACH("AutoAttach", BRIDGE_NAME, "auto_attach");

        private final String tableName;
        private final String parentTableName;
        private final String columnNameInParentTable;

        OvsdbSchemaTables(
                final String tableName, final String parentTableName,
                final String columnNameInParentTable) {
            this.tableName = tableName;
            this.parentTableName = parentTableName;
            this.columnNameInParentTable = columnNameInParentTable;
        }

        public String getTableName() {
            return this.tableName;
        }

        public String getParentTableName() {
            return this.parentTableName;
        }

        public String getColumnNameInParentTable() {
            return this.columnNameInParentTable;
        }
    }

}
