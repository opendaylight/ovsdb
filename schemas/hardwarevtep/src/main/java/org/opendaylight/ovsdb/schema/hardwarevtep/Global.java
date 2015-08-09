/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

@TypedTable(name="Global", database="hardware_vtep", fromVersion="1.0.0")
public interface Global extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="managers", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getManagersColumn();

    @TypedColumn(name="managers", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setManagers(Set<UUID> managers);

    @TypedColumn(name="switches", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<UUID>> getSwitchesColumn();

    @TypedColumn(name="switches", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setSwitches(Set<UUID> switches);
}
