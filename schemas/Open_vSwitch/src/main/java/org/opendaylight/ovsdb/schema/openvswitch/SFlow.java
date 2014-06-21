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

/**
 * This class is a typed interface to the SFlow Table
 */
@TypedTable(name="sFlow", database="Open_vSwitch", fromVersion="1.0.0")
public interface SFlow extends TypedBaseTable {
    @TypedColumn(name="targets", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<String>> getTargetsColumn() ;
    @TypedColumn(name="targets", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setTargets(Set<String> targets) ;

    @TypedColumn(name="agent", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<String>> getAgentColumn() ;
    @TypedColumn(name="agent", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setAgent(Set<String> agent) ;

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn() ;
    @TypedColumn(name="external_ids", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setExternalIds(Map<String, String> externalIds) ;

    @TypedColumn(name="header", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<Integer>> getHeaderColumn() ;
    @TypedColumn(name="header", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setHeader(Set<Integer> header) ;

    @TypedColumn(name="polling", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<Integer>> getPollingColumn() ;
    @TypedColumn(name="polling", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setPolling(Set<Integer> polling) ;

    @TypedColumn(name="sampling", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Set<Integer>> getSamplingColumn() ;
    @TypedColumn(name="sampling", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setSampling(Set<Integer> sampling) ;
}