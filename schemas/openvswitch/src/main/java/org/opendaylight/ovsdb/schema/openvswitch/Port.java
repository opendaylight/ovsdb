/*
 * Copyright (C) 2013 Red Hat, Inc.
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
 * This class is a typed interface to the Port Table
 */
@TypedTable(name="Port", database="Open_vSwitch", fromVersion="1.0.0")
public interface Port extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="name", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getNameColumn();
    @TypedColumn(name="name", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setName(String name);
    @TypedColumn(name="name", method=MethodType.GETDATA, fromVersion="1.0.0")
    String getName();

    @TypedColumn(name="interfaces", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getInterfacesColumn();
    @TypedColumn(name="interfaces", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setInterfaces(Set<UUID> interfaces);

    @TypedColumn(name="trunks", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getTrunksColumn();
    @TypedColumn(name="trunks", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setTrunks(Set<Long> trunks);

    @TypedColumn(name="tag", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getTagColumn();

    @TypedColumn(name="tag", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setTag(Set<Long> tag);

    @TypedColumn(name="vlan_mode", method=MethodType.GETCOLUMN, fromVersion="6.1.0")
    Column<GenericTableSchema, Set<String>> getVlanModeColumn();
    @TypedColumn(name="vlan_mode", method=MethodType.SETDATA, fromVersion="6.1.0")
    void setVlanMode(Set<String> vlanMode);

    @TypedColumn(name="qos", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getQosColumn();
    @TypedColumn(name="qos", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setQos(Set<UUID> qos);

    @TypedColumn(name="mac", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getMacColumn();
    @TypedColumn(name="mac", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setMac(Set<String> mac);

    @TypedColumn(name="bond_type", method=MethodType.GETCOLUMN, fromVersion="1.0.2", untilVersion="1.0.3")
    Column<GenericTableSchema, Set<String>> getBondTypeColumn();
    @TypedColumn(name="bond_type", method=MethodType.SETDATA)
    void setBondType(Set<String> bond_type);

    @TypedColumn(name="bond_mode", method=MethodType.GETCOLUMN, fromVersion="1.0.4")
    Column<GenericTableSchema, Set<String>> getBondModeColumn();
    @TypedColumn(name="bond_mode", method=MethodType.SETDATA, fromVersion="1.0.4")
    void setBondMode(Set<String> bond_mode);

    @TypedColumn(name="lacp", method=MethodType.GETCOLUMN, fromVersion="1.3.0")
    Column<GenericTableSchema, Set<String>> getLacpColumn();
    @TypedColumn(name="lacp", method=MethodType.SETDATA, fromVersion="1.3.0")
    void setLacp(Set<String> lacp);

    @TypedColumn(name="bond_updelay", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getBondUpDelayColumn();
    @TypedColumn(name="bond_updelay", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setBondUpDelay(Set<Long> bondUpDelay);

    @TypedColumn(name="bond_downdelay", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getBondDownDelayColumn();
    @TypedColumn(name="bond_downdelay", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setBondDownDelay(Set<Long> bondDownDelay);

    @TypedColumn(name="bond_fake_iface", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Boolean>> getBondFakeInterfaceColumn();
    @TypedColumn(name="bond_fake_iface", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setBondFakeInterface(Set<Boolean> bondFakeInterface);

    @TypedColumn(name="fake_bridge", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Boolean>> getFakeBridgeColumn();
    @TypedColumn(name="fake_bridge", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setFakeBridge(Set<Boolean> fakeBridge);

    @TypedColumn(name="status", method=MethodType.GETCOLUMN, fromVersion="6.2.0")
    Column<GenericTableSchema, Map<String, String>> getStatusColumn();
    @TypedColumn(name="status", method=MethodType.SETDATA, fromVersion="6.2.0")
    void setStatus(Map<String, String> status);

    @TypedColumn(name="statistics", method=MethodType.GETCOLUMN, fromVersion="6.3.0")
    Column<GenericTableSchema, Map<String, Long>> getStatisticsColumn();
    @TypedColumn(name="statistics", method=MethodType.SETDATA)
    void setStatistics(Map<String, Long> statistics);

    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn();
    @TypedColumn(name="other_config", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setOtherConfig(Map<String, String> otherConfig);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();
    @TypedColumn(name="external_ids", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setExternalIds(Map<String, String> externalIds);
}
