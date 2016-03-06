/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

import java.util.Map;
import java.util.Set;

/**
 * This class is a typed interface to the Mirror Table
 */
@TypedTable (name="Mirror", database="Open_vSwitch", fromVersion="1.0.0")
public interface Mirror extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="name", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getNameColumn();

    @TypedColumn(name="name", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setName(Set<String> name);

    @TypedColumn(name="name", method=MethodType.GETDATA, fromVersion="1.0.0")
    Set<String> getName();

    @TypedColumn(name="select_src_port", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getSelectSrcPortColumn();

    @TypedColumn(name="select_src_port", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setSelectSrcPort(Set<UUID> selectSrcPort);

    @TypedColumn(name="select_dst_port", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getSelectDstPortColumn();

    @TypedColumn(name="select_dst_port", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setSelectDstPort(Set<UUID> selectDstPrt);

    @TypedColumn(name="select_vlan", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getSelectVlanColumn();

    @TypedColumn(name="select_vlan", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setSelectVlan(Set<Long> selectVlan);

    @TypedColumn(name="output_port", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getOutputPortColumn();

    @TypedColumn(name="output_port", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setOutputPort(Set<UUID> outputPort);

    @TypedColumn (name="output_vlan", method= MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getOutputVlanColumn();

    @TypedColumn (name="output_vlan", method= MethodType.SETDATA, fromVersion="1.0.0")
    void setOutputVlan(Set<Long> outputVlan);

    @TypedColumn (name="statistics", method= MethodType.GETCOLUMN, fromVersion="6.4.0")
    Column<GenericTableSchema, Map<String, Long>> getStatisticsColumn();

    @TypedColumn (name="statistics", method= MethodType.SETDATA, fromVersion="6.4.0")
    void setStatistics(Map<String, Long> statistics);

    @TypedColumn (name="external_ids", method= MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();

    @TypedColumn (name="external_ids", method= MethodType.SETDATA, fromVersion="1.0.0")
    void setExternalIds(Map<String, String> externalIds);

    @TypedColumn (name="select_all", method= MethodType.GETCOLUMN, fromVersion="6.2.0")
    Column<GenericTableSchema, Boolean> getSelectAllColumn();

    @TypedColumn (name="select_all", method= MethodType.SETDATA, fromVersion="6.2.0")
    void setSelectAll(Boolean selectAll);
}