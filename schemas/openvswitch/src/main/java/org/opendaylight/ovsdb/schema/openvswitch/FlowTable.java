/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

/**
 * This class is a typed interface to the Flow_Table Table
 */
@TypedTable(name="Flow_Table", database="Open_vSwitch", fromVersion="6.5.0")
public interface FlowTable extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="flow_limit", method=MethodType.GETCOLUMN, fromVersion="6.5.0")
    Column<GenericTableSchema, Set<Long>> getFlowLimitColumn() ;

    @TypedColumn(name="flow_limit", method=MethodType.SETDATA, fromVersion="6.5.0")
    void setFlowLimit(Set<Long> flowLimit) ;

    @TypedColumn(name="overflow_policy", method=MethodType.GETCOLUMN, fromVersion="6.5.0")
    Column<GenericTableSchema, Set<String>> getOverflowPolicyColumn() ;

    @TypedColumn(name="overflow_policy", method=MethodType.SETDATA, fromVersion="6.5.0")
    void setOverflowPolicy(Set<String> overflowPolicy) ;

    @TypedColumn(name="groups", method=MethodType.GETCOLUMN, fromVersion="6.5.0")
    Column<GenericTableSchema, Set<String>> getGroupsColumn() ;

    @TypedColumn(name="groups", method=MethodType.SETDATA, fromVersion="6.5.0")
    void setGroups(Set<String> groups) ;

    @TypedColumn(name="name", method=MethodType.GETCOLUMN, fromVersion="6.5.0")
    Column<GenericTableSchema, Set<String>> getNameColumn();

    @TypedColumn(name="name", method=MethodType.SETDATA, fromVersion="6.5.0")
    void setName(Set<String> name);

    @TypedColumn(name="prefixes", method=MethodType.GETCOLUMN, fromVersion="7.4.0")
    Column<GenericTableSchema, Set<String>> getPrefixesColumn();

    @TypedColumn(name="prefixes", method=MethodType.SETDATA, fromVersion="7.4.0")
    void setPrefixes(Set<String> prefixes);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN, fromVersion="7.5.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();

    @TypedColumn(name="external_ids", method=MethodType.SETDATA, fromVersion="7.5.0")
    void setExternalIds(Map<String, String> externalIds);

}
