/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.plugin.api;

import java.util.Map;

import com.google.common.collect.Maps;

public final class OvsVswitchdSchemaConstants {
    public static String DATABASE_NAME = "Open_vSwitch";

    private static final String OVSDB_AUTOCONFIGURECONTROLLER = "ovsdb.autoconfigurecontroller";
    private static final boolean defaultAutoConfigureController = true;
    private static boolean autoConfigureController = defaultAutoConfigureController;

    private static Map<String, String[]> columnToMutate = Maps.newHashMap();
    public static String[] getParentColumnToMutate(String childTabletoInsert) {
        return columnToMutate.get(childTabletoInsert);
    }
    private static void addParentColumnToMutate(String childTable, String parentTable, String columnName) {
        String[] parentColumn = {parentTable, columnName};
        columnToMutate.put(childTable, parentColumn);
    }

    static {
        addParentColumnToMutate("Bridge", "Open_vSwitch", "bridges");
        addParentColumnToMutate("Port", "Bridge", "ports");
        addParentColumnToMutate("Interface", "Port", "interfaces");
        addParentColumnToMutate("SSL", "Open_vSwitch", "ssl");
        addParentColumnToMutate("IPFIX", "Bridge", "ipfix");
        addParentColumnToMutate("sFlow", "Bridge", "sflow");
        addParentColumnToMutate("Flow_Table", "Bridge", "flow_tables");
        addParentColumnToMutate("QoS", "Port", "qos");
        addParentColumnToMutate("NetFlow", "Bridge", "netflow");
        addParentColumnToMutate("Mirror", "Bridge", "mirrors");
        addParentColumnToMutate("Manager", "Open_vSwitch", "manager_options");
        addParentColumnToMutate("Controller", "Bridge", "controller");
        // Keep the default value if the property is not set
        if (System.getProperty(OVSDB_AUTOCONFIGURECONTROLLER) != null)
            autoConfigureController = Boolean.getBoolean(OVSDB_AUTOCONFIGURECONTROLLER);
    }

    public static void setAutoConfigureController(boolean autoConfigure) {
        autoConfigureController = autoConfigure;
    }

    public static boolean shouldConfigureController (String databaseName, String tableName) {
        if (autoConfigureController && databaseName.equals(DATABASE_NAME) && tableName.equals("Bridge")) return true;
        return false;
    }

    public enum PortType {
        VLAN("vlan"),
        TUNNEL("Tunnel"),
        BONDING("Bonding"),
        PATCH("patch"),
        INTERNAL("internal");

        private PortType(String name) {
            this.name = name;
        }

        private String name;

        @Override
        public String toString() {
            return name;
        }
    }

}
