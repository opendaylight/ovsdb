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
@TypedTable(name="Flow_Table", database="Open_vSwitch")
public interface FlowTable extends TypedBaseTable {
    @TypedColumn(name="flow_limit", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Integer> getFlowLimitColumn() ;
    @TypedColumn(name="details", method=MethodType.SETDATA)
    public void setFlowLimit(Integer flowLimit) ;

    @TypedColumn(name="overflow_policy", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getOverflowPolicyColumn() ;
    @TypedColumn(name="overflow_policy", method=MethodType.SETDATA)
    public void setOverflowPolicy(Set<String> overflowPolicy) ;

    @TypedColumn(name="groups", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getGroupsColumn() ;
    @TypedColumn(name="groups", method=MethodType.SETDATA)
    public void setGroups(Set<String> groups) ;
}