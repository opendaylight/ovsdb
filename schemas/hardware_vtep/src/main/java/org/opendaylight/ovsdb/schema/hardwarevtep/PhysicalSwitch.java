/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.schema.hardwarevtep;

import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

@TypedTable(name="Physical_Switch", database="hardware_vtep", fromVersion="1.0.0")
public interface PhysicalSwitch extends TypedBaseTable {
    @TypedColumn(name="name", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, String> getNameColumn();

    @TypedColumn(name="name", method=MethodType.GETDATA, fromVersion="1.0.0")
    public String getName();

    @TypedColumn(name="name", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setName(String name);


    @TypedColumn(name="description", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, String> getDescriptionColumn();

    @TypedColumn(name="description", method=MethodType.GETDATA, fromVersion="1.0.0")
    public String getDescription();

    @TypedColumn(name="description", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setDescription(String description);


    @TypedColumn(name="ports", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<UUID>> getPortsColumn();

    @TypedColumn(name="ports", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setPorts(Set<UUID> ports);


    @TypedColumn(name="management_ips", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<String>> getManagementIpsColumn();

    @TypedColumn(name="management_ips", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setManagementIps(Set<String> managementIps);


    @TypedColumn(name="tunnel_ips", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<String>> getTunnelIpsColumn();

    @TypedColumn(name="tunnel_ips", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setTunnelIps(Set<String> tunnelIps);


    @TypedColumn(name="tunnels", method=MethodType.GETCOLUMN, fromVersion="1.3.0")
    public Column<GenericTableSchema, Set<UUID>> getTunnels();

    @TypedColumn(name="tunnels", method=MethodType.SETDATA, fromVersion="1.3.0")
    public void setTunnels(Set<UUID> tunnels);

    @TypedColumn(name="switch_fault_status", method=MethodType.GETCOLUMN, fromVersion="1.1.0")
    public Column<GenericTableSchema, Set<String>> getSwitchFaultStatusColumn();

    @TypedColumn(name="switch_fault_status", method=MethodType.SETDATA, fromVersion="1.1.0")
    public void setSwitchFaultStatus(Set<String> switchFaultStatus);
}
