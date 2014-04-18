/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class DatabaseSchema {

    public static Logger logger = LoggerFactory.getLogger(DatabaseSchema.class);

    public Map<String, TableSchema> tables;

    public DatabaseSchema(Map<String, TableSchema> tables) {
        this.tables = tables;
    }

    public Set<String> getTables() {
        return this.tables.keySet();
    }

    public boolean hasTable(String table) {
        return this.getTables().contains(table);
    }

    public TableSchema getTable(String table) {
        return this.tables.get(table);
    }

    public static DatabaseSchema fromJson(JsonNode json) {
        if (!json.isObject() || !json.has("tables")) {
            //todo specific types of exception
            throw new RuntimeException("bad databaseschema root, expected \"tables\" as child");
        }

        Map<String, TableSchema> tables = new HashMap<>();
        //Iterator<Map.Entry<String,JsonNode>> fields = json.fields();
        for (Iterator<Map.Entry<String, JsonNode>> iter = json.get("tables").fields(); iter.hasNext(); ) {
            Map.Entry<String, JsonNode> table = iter.next();
            logger.debug("Read schema for table[{}]:{}", table.getKey(), table.getValue());

            tables.put(table.getKey(), TableSchema.fromJson(table.getKey(), table.getValue()));
        }

        return new DatabaseSchema(tables);
    }

    public TransactionBuilder beginTransaction() {
        return new TransactionBuilder(this);
    }

    public <E extends TableSchema<E>> TableSchema<E> table(String tableName) {
        //todo : error handling
        return tables.get(tableName);
    }

    public <E extends TableSchema<E>> E table(String tableName, Class<E> clazz) {
        TableSchema<E> table = table(tableName);
        return table.as(clazz);
    }
}
