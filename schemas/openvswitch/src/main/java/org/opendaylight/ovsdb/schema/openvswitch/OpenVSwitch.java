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
 * This class is a typed interface to the Open_vSwitch table
 */
@TypedTable(name="Open_vSwitch", database="Open_vSwitch", fromVersion="1.0.0")
public interface OpenVSwitch extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="bridges", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getBridgesColumn();
    @TypedColumn(name="bridges", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setBridges(Set<UUID> bridges);

    @TypedColumn(name="managers", method=MethodType.GETCOLUMN, fromVersion="1.0.0", untilVersion="2.0.0")
    Column<GenericTableSchema, Set<UUID>> getManagersColumn();
    @TypedColumn(name="managers", method=MethodType.SETDATA, fromVersion="1.0.0", untilVersion="2.0.0")
    void setManagers(Set<UUID> managers);

    @TypedColumn(name="manager_options", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getManagerOptionsColumn();
    @TypedColumn(name="manager_options", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setManagerOptions(Set<UUID> managerOptions);

    @TypedColumn(name="ssl", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getSslColumn();
    @TypedColumn(name="ssl", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setSsl(Set<UUID> ssl);

    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN, fromVersion="5.1.0")
    Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn() ;
    @TypedColumn(name="other_config", method=MethodType.SETDATA, fromVersion="5.1.0")
    void setOtherConfig(Map<String, String> otherConfig);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn() ;
    @TypedColumn(name="external_ids", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setExternalIds(Map<String, String> externalIds);

    @TypedColumn(name="next_cfg", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Long> getNextConfigColumn();
    @TypedColumn(name="next_cfg", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setNextConfig(Long nextConfig);

    @TypedColumn(name="cur_cfg", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Long> getCurrentConfigColumn();
    @TypedColumn(name="cur_cfg", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setCurrentConfig(Long currentConfig);

    @TypedColumn(name="capabilities", method=MethodType.GETCOLUMN, fromVersion="1.0.0", untilVersion="6.7.0")
    Column<GenericTableSchema, Map<String, UUID>> getCapabilitiesColumn();
    @TypedColumn(name="capabilities", method=MethodType.SETDATA, fromVersion="1.0.0", untilVersion="6.7.0")
    void setCapabilities(Map<String, UUID> capabilities);

    @TypedColumn(name="statistics", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, Long>> getStatisticsColumn();
    @TypedColumn(name="statistics", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setStatistics(Map<String, Long> statistics);

    @TypedColumn(name="ovs_version", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getOvsVersionColumn();
    @TypedColumn(name="ovs_version", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setOvsVersion(Set<String> ovsVersion);

    @TypedColumn(name="db_version", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getDbVersionColumn();
    @TypedColumn(name="db_version", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setDbVersion(Set<String> dbVersion);

    @TypedColumn(name="system_type", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getSystemTypeColumn();
    @TypedColumn(name="system_type", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setSystemType(Set<String> systemType);

    @TypedColumn(name="system_version", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getSystemVersionColumn();
    @TypedColumn(name="system_version", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setSystemVersion(Set<String> systemVersion);

    @TypedColumn(name="datapath_types", method=MethodType.GETCOLUMN, fromVersion="7.12.1")
    Column<GenericTableSchema, Set<String>> getDatapathTypesColumn();
    @TypedColumn(name="datapath_types", method=MethodType.SETDATA, fromVersion="7.12.1")
    void setDatapathTypes(Set<String> datapath_types);

    @TypedColumn(name="iface_types", method=MethodType.GETCOLUMN, fromVersion="7.12.1")
    Column<GenericTableSchema, Set<String>> getIfaceTypesColumn();
    @TypedColumn(name="iface_types", method=MethodType.SETDATA, fromVersion="7.12.1")
    void setIfaceTypes(Set<String> iface_types);

}
