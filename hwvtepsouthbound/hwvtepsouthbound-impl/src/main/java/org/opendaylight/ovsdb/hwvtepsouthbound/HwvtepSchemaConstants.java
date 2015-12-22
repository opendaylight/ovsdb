/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

public class HwvtepSchemaConstants {
    public static final String HARDWARE_VTEP = "hardware_vtep";
    public enum HWVTEPSCHEMATABLES {
        GLOBAL("Global", null, null),
        MANAGER("Manager","Global","managers"),
        PHYSICALSWITCH("Physical_Switch","Global","switches"),
        PHYSICALPORT("Physical_Port","Physical_Switch","ports"),
        TUNNEL("Tunnel","Physical_Switch","tunnels"),
        LOGICALSWITCH("Logical_Switch","Physical_Port","vlan_bindings"),
        ACL("ACL","Physical_Port","acl_bindings"),
        LOGICALBINDINGSTATS("Logical_Binding_Stats","Physical_Port","vlan_stats"),
//        PHYSICALLOCATORLOCAL("Physical_Locator","Tunnel","local"),
//        PHYSICALLOCATORREMOTE("Physical_Locator","Tunnel","remote"),
        UCASTMACSLOCAL("Ucast_Macs_Local",null, null),
        UCASTMACSREMOTE("Ucast_Macs_Remote",null, null),
        MCASTMACSLOCAL("Mcast_Macs_Local",null, null),
        PHYSICALLOCATORSET("Physical_Locator_Set","Mcast_Macs_Local", "locator_set"),
        MCASTMACSREMOTE("Mcast_Macs_Remote",null, null),
        LOGICALROUTER("Logical_Router",null, null),
        ARPSOURCESLOCAL("Arp_Sources_Local",null, null),
        ARPSOURCESREMOTE("Arp_Sources_Remote",null, null),
        PHYSICALLOCATOR("Physical_Locator","Physical_Locator_Set", "locators"),
        ACLENTRY("Acl_Entry","ACL", "acl_entries");

        private final String tableName;
        private final String parentTableName;
        private final String columnNameInParentTable;

        HWVTEPSCHEMATABLES(
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
