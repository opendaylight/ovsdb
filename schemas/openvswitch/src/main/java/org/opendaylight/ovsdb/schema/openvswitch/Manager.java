/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

import java.util.Map;
import java.util.Set;

/**
 * This class is a typed interface to the Manager Table
 */
@TypedTable (name="Manager", database="Open_vSwitch", fromVersion = "1.0.0")
public interface Manager extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn (name="target", method= MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Set<String>> getTargetColumn();

    @TypedColumn (name="target", method= MethodType.SETDATA, fromVersion = "1.0.0")
    void setTarget(Set<String> target) ;

    @TypedColumn (name = "is_connected", method = MethodType.GETCOLUMN, fromVersion = "1.1.0")
    Column<GenericTableSchema, Boolean> getIsConnectedColumn();

    @TypedColumn (name = "is_connected", method = MethodType.SETDATA, fromVersion = "1.1.0")
    void setIsConnected(Boolean isConnected);

    @TypedColumn (name = "other_config", method = MethodType.GETCOLUMN, fromVersion = "6.8.0")
    Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn();

    @TypedColumn (name = "other_config", method = MethodType.SETDATA, fromVersion = "6.8.0")
    void setOtherConfig(Map<String, String> otherConfig);

    @TypedColumn (name = "external_ids", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();

    @TypedColumn (name = "external_ids", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setExternalIds(Map<String, String> externalIds);

    @TypedColumn (name = "max_backoff", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Set<Long>> getMaxBackoffColumn();

    @TypedColumn (name = "max_backoff", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setMaxBackoff(Set<Long> maxBackoff);

    @TypedColumn (name = "status", method = MethodType.GETCOLUMN, fromVersion = "1.1.0")
    Column<GenericTableSchema, Map<String, String>> getStatusColumn();

    @TypedColumn (name = "status", method = MethodType.SETDATA, fromVersion = "1.1.0")
    void setStatus(Map<String, String> status);

    @TypedColumn (name = "inactivity_probe", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Set<Long>> getInactivityProbeColumn();

    @TypedColumn (name = "inactivity_probe", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setInactivityProbe(Set<Long> inactivityProbe);

    @TypedColumn (name = "connection_mode", method = MethodType.GETCOLUMN, fromVersion = "1.0.0")
    Column<GenericTableSchema, Set<String>> getConnectionModeColumn();

    @TypedColumn (name = "connection_mode", method = MethodType.SETDATA, fromVersion = "1.0.0")
    void setConnectionMode(Set<String> connectionMode);
}