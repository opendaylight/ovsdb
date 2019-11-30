/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.opendaylight.ovsdb.lib.error.BadSchemaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericTableSchema extends TableSchema<GenericTableSchema> {
    private static final Logger LOG = LoggerFactory.getLogger(GenericTableSchema.class);

    public GenericTableSchema(final String name) {
        super(name);
    }

    public GenericTableSchema(final String name, final Map<String, ColumnSchema> columns) {
        super(name, columns);
    }

    public GenericTableSchema(final TableSchema tableSchema) {
        super(tableSchema.getName(), tableSchema.getColumnSchemas());
    }

    public static GenericTableSchema fromJson(final String tableName, final JsonNode json) {
        if (!json.isObject() || !json.has("columns")) {
            throw new BadSchemaException("bad tableschema root, expected \"columns\" as child");
        }

        Map<String, ColumnSchema> columns = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iter = json.get("columns").fields(); iter.hasNext(); ) {
            Map.Entry<String, JsonNode> column = iter.next();
            LOG.trace("fromJson() table/column = {}:{}", tableName, column.getKey());
            columns.put(column.getKey(), ColumnSchema.fromJson(column.getKey(), column.getValue()));
        }

        return new GenericTableSchema(tableName, columns);
    }

    @Override
    public GenericTableSchema withInternallyGeneratedColumns() {
        if (haveInternallyGeneratedColumns()) {
            return this;
        }

        final Map<String, ColumnSchema> columns = new HashMap<>(getColumnSchemas());
        columns.put(UUID_COLUMN_SCHMEMA.getName(), UUID_COLUMN_SCHMEMA);
        columns.put(VERSION_COLUMN_SCHMEMA.getName(), VERSION_COLUMN_SCHMEMA);
        return new GenericTableSchema(getName(), columns);
    }
}
