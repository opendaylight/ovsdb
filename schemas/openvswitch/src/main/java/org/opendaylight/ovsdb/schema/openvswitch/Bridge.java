/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Dave Tucker
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

/**
 * This class is a typed interface to the Bridge Table
 */
@TypedTable(name="Bridge", database="Open_vSwitch", fromVersion="1.0.0")
public interface Bridge extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="name", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getNameColumn();

    @TypedColumn(name="name", method=MethodType.GETDATA, fromVersion="1.0.0")
    String getName();

    @TypedColumn(name="name", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setName(String name);

    @TypedColumn(name="datapath_type", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getDatapathTypeColumn();

    @TypedColumn(name="datapath_type", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setDatapathType(String datapathType);

    @TypedColumn(name="datapath_id", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getDatapathIdColumn();

    @TypedColumn(name="datapath_id", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setDatapathId(Set<String> datapathId);

    @TypedColumn(name="stp_enable", method=MethodType.GETCOLUMN, fromVersion="6.2.0")
    Column<GenericTableSchema, Boolean> getStpEnableColumn();

    @TypedColumn(name="stp_enable", method=MethodType.SETDATA, fromVersion="6.2.0")
    void setStpEnable(Boolean stp_enable);

    @TypedColumn(name="ports", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getPortsColumn();

    @TypedColumn(name="ports", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setPorts(Set<UUID> ports);

    @TypedColumn(name="mirrors", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getMirrorsColumn();

    @TypedColumn(name="mirrors", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setMirrors(Set<UUID> mirrors);

    @TypedColumn(name="netflow", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getNetflowColumn();

    @TypedColumn(name="netflow", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setNetflow(Set<UUID> netflow);

    @TypedColumn(name="sflow", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getSflowColumn();

    @TypedColumn(name="sflow", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setSflow(Set<UUID> sflow);

    @TypedColumn(name="ipfix", method=MethodType.GETCOLUMN, fromVersion="7.1.0")
    Column<GenericTableSchema, Set<UUID>> getIpfixColumn();

    @TypedColumn(name="ipfix", method=MethodType.SETDATA, fromVersion="7.1.0")
    void setIpfix(Set<UUID> ipfix);

    @TypedColumn(name="controller", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getControllerColumn();

    @TypedColumn(name="controller", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setController(Set<UUID> controller);

    @TypedColumn(name="protocols", method=MethodType.GETCOLUMN, fromVersion="6.11.1")
    Column<GenericTableSchema, Set<String>> getProtocolsColumn();

    @TypedColumn(name="protocols", method=MethodType.SETDATA, fromVersion="6.11.1")
    void setProtocols(Set<String> protocols);

    @TypedColumn(name="fail_mode", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getFailModeColumn();

    @TypedColumn(name="fail_mode", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setFailMode(Set<String> failMode);

    @TypedColumn(name="status", method=MethodType.GETCOLUMN, fromVersion="6.2.0")
    Column<GenericTableSchema, Map<String, String>> getStatusColumn();

    @TypedColumn(name="status", method=MethodType.SETDATA, fromVersion="6.2.0")
    void setStatus(Map<String, String> status);

    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn();

    @TypedColumn(name="other_config", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setOtherConfig(Map<String, String> otherConfig);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();

    @TypedColumn(name="external_ids", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setExternalIds(Map<String, String> externalIds);

    @TypedColumn(name="flood_vlans", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getFloodVlansColumn();

    @TypedColumn(name="flood_vlans", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setFloodVlans(Set<Long> vlans);

    @TypedColumn(name="flow_tables", method=MethodType.GETCOLUMN, fromVersion="6.5.0")
    Column<GenericTableSchema, Map<Long, UUID>> getFlowTablesColumn();

    @TypedColumn(name="flow_tables", method=MethodType.SETDATA, fromVersion="6.5.0")
    void setFlowTables(Map<Long, UUID> flowTables);

}
