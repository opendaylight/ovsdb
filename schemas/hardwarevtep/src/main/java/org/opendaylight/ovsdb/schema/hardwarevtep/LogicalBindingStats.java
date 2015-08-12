/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

@TypedTable(name="Logical_Binding_Stats", database="hardware_vtep", fromVersion="1.0.0")
public interface LogicalBindingStats extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="bytes_from_local", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Long> getBytesFromLocalColumn();

    @TypedColumn(name="bytes_from_local", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setBytesFromLocal(Long bytesFromLocal);

    @TypedColumn(name="packets_from_local", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Long> getPacketsFromLocalColumn();

    @TypedColumn(name="packets_from_local", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setPacketsFromLocal(Long packetsFromLocal);

    @TypedColumn(name="bytes_to_local", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Long> getBytesToLocalColumn();

    @TypedColumn(name="bytes_to_local", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setBytesToLocal(Long bytesToLocal);

    @TypedColumn(name="packets_to_local", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Long> getPacketsToLocalColumn();

    @TypedColumn(name="packets_to_local", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setPacketsToLocal(Long packetsToLocal);
}
