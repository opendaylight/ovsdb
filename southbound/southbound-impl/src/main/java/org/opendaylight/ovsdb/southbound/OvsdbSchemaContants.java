/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

/**
 *
 * @author Anil Vishnoi (avishnoi@brocade.com)
 *
 */
public class OvsdbSchemaContants {
    public static final String databaseName = "Open_vSwitch";
    public enum OVSDBSCHEMATABLES {
        OPENVSWITCH("Open_vSwitch",null,null),
        BRIDGE("Bridge", "Open_vSwitch", "bridges"),
        PORT("Port", "Bridge", "ports"),
        INTERFACE("Interface", "Port", "interfaces"),
        SSL("SSL", "Open_vSwitch", "ssl"),
        IPFIX("IPFIX", "Bridge", "ipfix"),
        SFLOW("sFlow", "Bridge", "sflow"),
        FLOWTABLE("Flow_Table", "Bridge", "flow_tables"),
        QOS("QoS", "Port", "qos"),
        NETFLOW("NetFlow", "Bridge", "netflow"),
        MIRROR("Mirror", "Bridge", "mirrors"),
        MANAGER("Manager", "Open_vSwitch", "manager_options"),
        CONTROLLER("Controller", "Bridge", "controller"),
        FLOWSAMPLECOLLECTORSET("Flow_Sample_Collector_Set",null,null);

        private final String tableName;
        private final String parentTableName;
        private final String columnNameInParentTable;

        OVSDBSCHEMATABLES(
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
