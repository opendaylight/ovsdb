/*
 * Copyright (C) 2013 Red Hat, Inc.
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
 * Reusing the existing Table definitions and many of columns are not defined here
 * TODO : Fill up the missing Columns and include Supported DB Version
 */
@TypedTable(name="Mirror", database="Open_vSwitch")
public interface Mirror extends TypedBaseTable {

    @TypedColumn(name="name", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, String> getNameColumn();
    @TypedColumn(name="name", method=MethodType.SETDATA)
    public void setName(String name);

    @TypedColumn(name="select_src_port", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getSelectSrcPortColumn();
    @TypedColumn(name="select_src_port", method=MethodType.SETDATA)
    public void setSelectSrcPort(Set<UUID> selectSrcPort);

    @TypedColumn(name="select_dst_port", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getSelectDstPortColumn();
    @TypedColumn(name="select_dst_port", method=MethodType.SETDATA)
    public void setSelectDstPort(Set<UUID> selectDstPrt);

    @TypedColumn(name="select_vlan", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<Integer>> getSelectVlanColumn();
    @TypedColumn(name="select_vlan", method=MethodType.SETDATA)
    public void setSelectVlan(Set<Integer> selectVlan);

    @TypedColumn(name="output_port", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getOutputPortColumn();
    @TypedColumn(name="output_port", method=MethodType.SETDATA)
    public void setOutputPort(Set<UUID> outputPort);

    @TypedColumn(name="output_vlan", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<Integer>> getOutputVlanColumn();
    @TypedColumn(name="output_vlan", method=MethodType.SETDATA)
    public void setOutputVlan(Set<Integer> outputVlan);

    @TypedColumn(name="statistics", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, Integer>> getStatisticsColumn();
    @TypedColumn(name="statistics", method=MethodType.SETDATA)
    public void setStatistics(Map<String, Integer> statistics);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();
    @TypedColumn(name="external_ids", method=MethodType.SETDATA)
    public void setExternalIds(Map<String, String> externalIds);
}
