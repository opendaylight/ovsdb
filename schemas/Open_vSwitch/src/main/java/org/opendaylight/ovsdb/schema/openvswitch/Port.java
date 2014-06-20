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

import java.math.BigInteger;
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
@TypedTable(name="Port", database="Open_vSwitch")
public interface Port extends TypedBaseTable {

    @TypedColumn(name="name", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, String> getNameColumn();
    @TypedColumn(name="name", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setName(String name);

    @TypedColumn(name="interfaces", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<UUID>> getInterfacesColumn();
    @TypedColumn(name="interfaces", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setInterfaces(Set<UUID> interfaces);

    @TypedColumn(name="trunks", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<BigInteger>> getTrunksColumn();
    @TypedColumn(name="trunks", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setTrunks(Set<BigInteger> trunks);
      
    @TypedColumn(name="tag", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<BigInteger>> getTagColumn();
    @TypedColumn(name="tag", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setTag(Set<BigInteger> tag);

    @TypedColumn(name="vlan_mode", method=MethodType.GETCOLUMN, fromVersion="6.1.0")
    public Column<GenericTableSchema, Set<String>> getVlanModeColumn();
    @TypedColumn(name="vlan_mode", method=MethodType.SETDATA, fromVersion="6.1.0")
    public void setVlanMode(Set<String> vlanMode);

    @TypedColumn(name="qos", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<UUID>> getQosColumn();
    @TypedColumn(name="qos", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setQos(Set<UUID> qos);

    @TypedColumn(name="mac", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<String>> getMacColumn();
    @TypedColumn(name="mac", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setMac(Set<String> mac);

    @TypedColumn(name="bond_type", method=MethodType.GETCOLUMN, fromVersion="1.0.2", untilVersion="1.0.3")
    public Column<GenericTableSchema, Set<String>> getBondTypeColumn();
    @TypedColumn(name="bond_type", method=MethodType.SETDATA)
    public void setBondType(Set<String> bond_type);

    @TypedColumn(name="bond_mode", method=MethodType.GETCOLUMN, fromVersion="1.0.4")
    public Column<GenericTableSchema, Set<String>> getBondModeColumn();
    @TypedColumn(name="bond_mode", method=MethodType.SETDATA, fromVersion="1.0.4")
    public void setBondMode(Set<String> bond_mode);

    @TypedColumn(name="lacp", method=MethodType.GETCOLUMN, fromVersion="1.3.0")
    public Column<GenericTableSchema, Set<String>> getLacpColumn();
    @TypedColumn(name="lacp", method=MethodType.SETDATA, fromVersion="1.3.0")
    public void setLacp(Set<String> lacp);

    @TypedColumn(name="bond_updelay", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<BigInteger>> getBondUpDelayColumn();
    @TypedColumn(name="bond_updelay", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setBondUpDelay(Set<BigInteger> bondUpDelay);

    @TypedColumn(name="bond_downdelay", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<BigInteger>> getBondDownDelayColumn();
    @TypedColumn(name="bond_downdelay", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setBondDownDelay(Set<BigInteger> bondDownDelay);

    @TypedColumn(name="bond_fake_iface", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<Boolean>> getBondFakeInterfaceColumn();
    @TypedColumn(name="bond_fake_iface", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setBondFakeInterface(Set<Boolean> bondFakeInterface);

    @TypedColumn(name="fake_bridge", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<Boolean>> getFakeBridgeColumn();
    @TypedColumn(name="fake_bridge", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setFakeBridge(Set<Boolean> fakeBridge);

    @TypedColumn(name="status", method=MethodType.GETCOLUMN, fromVersion="6.2.0")
    public Column<GenericTableSchema, Map<String, String>> getStatusColumn();
    @TypedColumn(name="status", method=MethodType.SETDATA, fromVersion="6.2.0")
    public void setStatus(Map<String, String> status);

    @TypedColumn(name="statistics", method=MethodType.GETCOLUMN, fromVersion="6.3.0")
    public Column<GenericTableSchema, Map<String, BigInteger>> getStatisticsColumn();
    @TypedColumn(name="statistics", method=MethodType.SETDATA)
    public void setStatistics(Map<String, BigInteger> statistics);
    
    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn();
    @TypedColumn(name="other_config", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setOtherConfig(Map<String, String> otherConfig);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();
    @TypedColumn(name="external_ids", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setExternalIds(Map<String, String> externalIds);
}
