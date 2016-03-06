/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
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
 * This class is a typed interface to the SFlow Table
 */
@TypedTable(name="sFlow", database="Open_vSwitch", fromVersion="1.0.0")
public interface SFlow extends TypedBaseTable<GenericTableSchema> {
    @TypedColumn(name="targets", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getTargetsColumn() ;
    @TypedColumn(name="targets", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setTargets(Set<String> targets) ;

    @TypedColumn(name="agent", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getAgentColumn() ;
    @TypedColumn(name="agent", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setAgent(Set<String> agent) ;

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn() ;
    @TypedColumn(name="external_ids", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setExternalIds(Map<String, String> externalIds) ;

    @TypedColumn(name="header", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getHeaderColumn() ;
    @TypedColumn(name="header", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setHeader(Set<Long> header) ;

    @TypedColumn(name="polling", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getPollingColumn() ;
    @TypedColumn(name="polling", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setPolling(Set<Long> polling) ;

    @TypedColumn(name="sampling", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getSamplingColumn() ;
    @TypedColumn(name="sampling", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setSampling(Set<Long> sampling) ;
}