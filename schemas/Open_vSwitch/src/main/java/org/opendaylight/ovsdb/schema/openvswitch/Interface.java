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

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;


/*
 * Reusing the existing Table definitions and many of columns are not defined here
 * TODO : Fill up the missing Columns and include Supported DB Version
 */
@TypedTable(name="Interface", database="Open_vSwitch")
public interface Interface extends TypedBaseTable {

    @TypedColumn(name="name", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, String> getNameColumn();
    @TypedColumn(name="name", method=MethodType.SETDATA)
    public void setName(String name);

    @TypedColumn(name="options", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getOptionsColumn();
    @TypedColumn(name="options", method=MethodType.SETDATA)
    public void setOptions(Map<String, String> options);

    @TypedColumn(name="type", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, String> getTypeColumn();
    @TypedColumn(name="type", method=MethodType.SETDATA)
    public void setType(String type);

    @TypedColumn(name="ofport", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<BigInteger>> getOfPortColumn();
    @TypedColumn(name="ofport", method=MethodType.SETDATA)
    public void setOfPort(Set<BigInteger> ofport);

    @TypedColumn(name="mac", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getMacColumn();
    @TypedColumn(name="mac", method=MethodType.SETDATA)
    public void setMac(Set<String> mac);

    @TypedColumn(name="statistics", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, Integer>> getStatisticsColumn();
    @TypedColumn(name="statistics", method=MethodType.SETDATA)
    public void setStatistics(Map<String, Integer> statistics);

    @TypedColumn(name="status", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getStatusColumn();
    @TypedColumn(name="status", method=MethodType.SETDATA)
    public void setStatus(Map<String, String> status);

    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn();
    @TypedColumn(name="other_config", method=MethodType.SETDATA)
    public void setOtherConfig(Map<String, String> otherConfig);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();
    @TypedColumn(name="external_ids", method=MethodType.SETDATA)
    public void setExternalIds(Map<String, String> externalIds);
}
