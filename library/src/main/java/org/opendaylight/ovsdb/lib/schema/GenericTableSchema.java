/*
 *
 *  * Copyright (C) 2014 EBay Software Foundation
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Ashwin Raveendran
 *
 */

package org.opendaylight.ovsdb.lib.schema;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.opendaylight.ovsdb.lib.error.BadSchemaException;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericTableSchema extends TableSchema<GenericTableSchema> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericTableSchema.class);

    public GenericTableSchema() {
    }

    public GenericTableSchema(String tableName) {
        super(tableName);
    }

    public GenericTableSchema(TableSchema tableSchema) {
        super(tableSchema.getName(), tableSchema.getColumnSchemas());
    }

    public GenericTableSchema fromJson(String tableName, JsonNode json) {

        if (!json.isObject() || !json.has("columns")) {
            throw new BadSchemaException("bad tableschema root, expected \"columns\" as child");
        }

        Map<String, ColumnSchema> columns = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iter = json.get("columns").fields(); iter.hasNext(); ) {
            Map.Entry<String, JsonNode> column = iter.next();
            LOG.trace("{}:{}", tableName, column.getKey());
            columns.put(column.getKey(), ColumnSchema.fromJson(column.getKey(), column.getValue()));
        }

        this.setName(tableName);
        this.setColumns(columns);
        return this;
    }
}
