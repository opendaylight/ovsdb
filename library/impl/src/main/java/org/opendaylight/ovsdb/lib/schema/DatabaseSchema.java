/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.opendaylight.ovsdb.lib.notation.Version;

/**
 * Represents an ovsdb database schema, which is comprised of a set of tables.
 */
public interface DatabaseSchema {
    Set<String> getTables();

    boolean hasTable(String table);

    <E extends TableSchema<E>> E table(String tableName, Class<E> clazz);

    String getName();

    Version getVersion();

    DatabaseSchema withInternallyGeneratedColumns();

    static DatabaseSchema fromJson(final String dbName, final JsonNode json) {
        return DatabaseSchemaImpl.fromJson(dbName, json);
    }
}
