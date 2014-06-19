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
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;


/*
 * Reusing the existing Table definitions and many of columns are not defined here
 * TODO : Fill up the missing Columns and include Supported DB Version
 */

@TypedTable(name="SFlow", database="Open_vSwitch")
public interface SFlow extends TypedBaseTable {
    @TypedColumn(name="targets", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getTargetsColumn() ;
    @TypedColumn(name="targets", method=MethodType.SETDATA)
    public void setTargets(Set<String> targets) ;

    @TypedColumn(name="agent", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getAgentColumn() ;
    @TypedColumn(name="agent", method=MethodType.SETDATA)
    public void setAgent(Set<String> agent) ;

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn() ;
    @TypedColumn(name="external_ids", method=MethodType.SETDATA)
    public void setExternalIds(Map<String, String> externalIds) ;

    @TypedColumn(name="header", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<Integer>> getHeaderColumn() ;
    @TypedColumn(name="header", method=MethodType.SETDATA)
    public void setHeader(Set<Integer> header) ;

    @TypedColumn(name="polling", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<Integer>> getPollingColumn() ;
    @TypedColumn(name="polling", method=MethodType.SETDATA)
    public void setPolling(Set<Integer> polling) ;

    @TypedColumn(name="sampling", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<Integer>> getSamplingColumn() ;
    @TypedColumn(name="sampling", method=MethodType.SETDATA)
    public void setSampling(Set<Integer> sampling) ;
}
