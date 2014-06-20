/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

public interface TypedBaseTable {
    @TypedColumn(name="", method=MethodType.GETTABLESCHEMA)
    GenericTableSchema getSchema();

    @TypedColumn(name="_uuid", method=MethodType.GETDATA)
    public UUID getUuid();

    @TypedColumn(name="_uuid", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, UUID> getUuidColumn();

    @TypedColumn(name="_version", method=MethodType.GETDATA)
    public UUID getVersion();

    @TypedColumn(name="_version", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, UUID> getVersionColumn();
}
