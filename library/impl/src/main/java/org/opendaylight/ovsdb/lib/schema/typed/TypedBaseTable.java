/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.schema.typed;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

public interface TypedBaseTable {
    @TypedColumn(name = "", method = MethodType.GETTABLESCHEMA)
    TableSchema getSchema();

    @TypedColumn(name = "", method = MethodType.GETROW)
    Row getRow();

    @TypedColumn(name = "_uuid", method = MethodType.GETDATA)
    UUID getUuid();

    @TypedColumn(name = "_uuid", method = MethodType.GETCOLUMN)
    Column<UUID> getUuidColumn();

    @TypedColumn(name = "_version", method = MethodType.GETDATA)
    UUID getVersion();

    @TypedColumn(name = "_version", method = MethodType.GETCOLUMN)
    Column<UUID> getVersionColumn();
}
