/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

@TypedTable(name="Manager", database="hardware_vtep", fromVersion="1.0.0")
public interface Manager extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="target", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getTargetColumn();

    @TypedColumn(name="target", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setTarget(String target);

    @TypedColumn(name="max_backoff", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getMaxBackoffColumn();

    @TypedColumn(name="max_backoff", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setMaxBackoff(Set<Long> maxBackoff);

    @TypedColumn(name="inactivity_probe", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getInactivityProbeColumn();

    @TypedColumn(name="inactivity_probe", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setInactivityProbe(Set<Long> inactivityProbe);

    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn();

    @TypedColumn(name="other_config", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setOtherConfig(Map<String, String> otherConfig);

    @TypedColumn(name="is_connected", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Boolean> getIsConnectedColumn();

    @TypedColumn(name="is_connected", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setIsConnectedColumn(Boolean isConnected);

    @TypedColumn(name="status", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getStatusColumn();

    @TypedColumn(name="status", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setStatus(Map<String, String> status);
}