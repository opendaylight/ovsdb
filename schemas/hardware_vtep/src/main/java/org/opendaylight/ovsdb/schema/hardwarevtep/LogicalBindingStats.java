/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.schema.hardwarevtep;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

@TypedTable(name="Logical_Binding_Stats", database="hardware_vtep", fromVersion="1.0.0")
public interface LogicalBindingStats extends TypedBaseTable {

    @TypedColumn(name="bytes_from_local", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Integer> getBytesFromLocalColumn();

    @TypedColumn(name="bytes_from_local", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setBytesFromLocal(Integer bytesFromLocal);

    @TypedColumn(name="packets_from_local", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Integer> getPacketsFromLocalColumn();

    @TypedColumn(name="packets_from_local", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setPacketsFromLocal(Integer packetsFromLocal);

    @TypedColumn(name="bytes_to_local", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Integer> getBytesToLocalColumn();

    @TypedColumn(name="bytes_to_local", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setBytesToLocal(Integer bytesToLocal);

    @TypedColumn(name="packets_to_local", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    public Column<GenericTableSchema, Integer> getPacketsToLocalColumn();

    @TypedColumn(name="packets_to_local", method=MethodType.SETDATA, fromVersion="1.0.0")
    public void setPacketsToLocal(Integer packetsToLocal);
}
