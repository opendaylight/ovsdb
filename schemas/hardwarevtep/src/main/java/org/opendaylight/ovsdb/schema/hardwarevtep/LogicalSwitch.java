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
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

@TypedTable(name="Logical_Switch", database="hardware_vtep", fromVersion="1.0.0")
public interface LogicalSwitch extends TypedBaseTable {
    @TypedColumn(name="name", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<String> getNameColumn();

    @TypedColumn(name="name", method=MethodType.GETDATA, fromVersion="1.0.0")
    String getName();

    @TypedColumn(name="name", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setName(String name);


    @TypedColumn(name="description", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<String> getDescriptionColumn();

    @TypedColumn(name="description", method=MethodType.GETDATA, fromVersion="1.0.0")
    String getDescription();

    @TypedColumn(name="description", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setDescription(String description);


    @TypedColumn(name="tunnel_key", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<Set<Long>> getTunnelKeyColumn();

    @TypedColumn(name="tunnel_key", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setTunnelKey(Set<Long> tunnelKey);


    @TypedColumn(name="replication_mode", method=MethodType.GETCOLUMN, fromVersion="1.6.0")
    Column<Set<String>> getReplicationModeColumn();

    @TypedColumn(name="replication_mode", method=MethodType.SETDATA, fromVersion="1.6.0")
    void setReplicationMode(Set<String> replicationMode);


    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN, fromVersion="1.5.1")
    Column<Map<String, String>> getOtherConfigColumn();

    @TypedColumn(name="other_config", method=MethodType.SETDATA, fromVersion="1.5.1")
    void setOtherConfig(Map<String, String> otherConfig);
}