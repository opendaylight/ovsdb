/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.schema.openvswitch;
import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

/*
 * Reusing the existing Table definitions and some of columns are not defined here
 * TODO : Fill up the missing Columns and include Supported DB Version
 */

@TypedTable(name="Bridge", database="Open_vSwitch")
public interface Bridge extends TypedBaseTable {

    @TypedColumn(name="name", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, String> getNameColumn();

    @TypedColumn(name="name", method=MethodType.GETDATA)
    public String getName();

    @TypedColumn(name="name", method=MethodType.SETDATA)
    public void setName(String name);


    @TypedColumn(name="ports", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getPortsColumn();

    @TypedColumn(name="ports", method=MethodType.SETDATA)
    public void setPorts(Set<UUID> ports);


    @TypedColumn(name="mirrors", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getMirrorsColumn();

    @TypedColumn(name="mirrors", method=MethodType.SETDATA)
    public void setMirrors(Set<UUID> mirrors);


    @TypedColumn(name="controller", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getControllerColumn();

    @TypedColumn(name="controller", method=MethodType.SETDATA)
    public void setController(Set<UUID> controller);


    @TypedColumn(name="datapath_id", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getDatapathIdColumn();

    @TypedColumn(name="datapath_id", method=MethodType.SETDATA)
    public void setDatapathId(Set<String> datapathId);

    @TypedColumn(name="datapath_type", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, String> getDatapathTypeColumn();

    @TypedColumn(name="datapath_type", method=MethodType.SETDATA)
    public void setDatapathType(String datapathType);


    @TypedColumn(name="fail_mode", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getFailModeColumn();

    @TypedColumn(name="fail_mode", method=MethodType.SETDATA)
    public void setFailMode(Set<String> failMode);


    @TypedColumn(name="flood_vlans", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<Integer>> getFloodVlansColumn();

    @TypedColumn(name="flood_vlans", method=MethodType.SETDATA)
    public void setFloodVlans(Set<Integer> vlans);


    @TypedColumn(name="sflow", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getSflowColumn();

    @TypedColumn(name="sflow", method=MethodType.SETDATA)
    public void setSflow(Set<UUID> sflow);


    @TypedColumn(name="netflow", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getNetflowColumn();

    @TypedColumn(name="netflow", method=MethodType.SETDATA)
    public void setNetflow(Set<UUID> netflow);


    @TypedColumn(name="flow_tables", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<Integer, UUID>> getFlowTablesColumn();

    @TypedColumn(name="flow_tables", method=MethodType.SETDATA)
    public void setFlowTables(Map<Integer, UUID> flowTables);


    @TypedColumn(name="status", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getStatusColumn();

    @TypedColumn(name="status", method=MethodType.SETDATA)
    public void setStatus(Map<String, String> status);


    @TypedColumn(name="stp_enable", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Boolean> getStpEnableColumn();

    @TypedColumn(name="stp_enable", method=MethodType.SETDATA)
    public void setStpEnable(Boolean stp_enable);


    @TypedColumn(name="protocols", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getProtocolsColumn();

    @TypedColumn(name="protocols", method=MethodType.SETDATA)
    public void setProtocols(Set<String> protocols);


    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn();

    @TypedColumn(name="other_config", method=MethodType.SETDATA)
    public void setOtherConfig(Map<String, String> otherConfig);


    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();

    @TypedColumn(name="external_ids", method=MethodType.SETDATA)
    public void setExternalIds(Map<String, String> externalIds);


    @TypedColumn(name="ipfix", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getIpfixColumn();

    @TypedColumn(name="ipfix", method=MethodType.SETDATA)
    public void setIpfix(Set<UUID> ipfix);
}
