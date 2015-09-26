/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.northbound;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OvsdbRow {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbRow.class);
    private static final String PARENTUUID = "parent_uuid";
    private static final String PARENTTABLE = "parent_table";
    private static final String PARENTCOLUMN = "parent_column";
    private static final String ROW = "row";

    private String parentUuid;
    private String parentTable;
    private String parentColumn;
    private String tableName;
    private Row<GenericTableSchema> row;

    public OvsdbRow() {
    }

    public OvsdbRow(String parentUuid, String tableName, Row<GenericTableSchema> row) {
        this.parentUuid = parentUuid;
        this.tableName = tableName;
        this.row = row;
    }

    public OvsdbRow(String parentTable, String parentUuid, String parentColumn, String tableName, Row<GenericTableSchema> row) {
        this.parentTable = parentTable;
        this.parentColumn = parentColumn;
        this.parentUuid = parentUuid;
        this.tableName = tableName;
        this.row = row;
    }

    public static OvsdbRow fromJsonNode(OvsdbClient client, String dbName, JsonNode json) {
        JsonNode parentUuidNode = json.get(PARENTUUID);
        String parentUuid = null;
        if (parentUuidNode != null) {
	    parentUuid = parentUuidNode.asText();
        }
        JsonNode parentTableNode = json.get(PARENTTABLE);
        String parentTable = null;
        if (parentTableNode != null) {
            parentTable = parentTableNode.asText();
        }
        JsonNode parentColumnNode = json.get(PARENTCOLUMN);
        String parentColumn = null;
        if (parentColumnNode != null) {
            parentColumn = parentColumnNode.asText();
        }
        JsonNode rowNode = json.get(ROW);
        if (rowNode == null) {
            return null;
        }
        Iterator<String> fieldNames = rowNode.fieldNames();
        if (fieldNames.hasNext()) {
            String tableName = fieldNames.next();
            try {
                Row<GenericTableSchema> row = getRow(client, dbName, tableName, rowNode.get(tableName));
                return new OvsdbRow(parentTable, parentUuid, parentColumn, tableName, row);
            } catch (InterruptedException | ExecutionException | IOException e) {
                LOG.error("Error retrieving the row for {}", tableName, e);
                return null;
            }
        }
        return null;
    }

    public static Row<GenericTableSchema> getRow(OvsdbClient client, String dbName, String tableName, JsonNode rowJson) throws InterruptedException, ExecutionException, IOException {
        DatabaseSchema dbSchema = client.getSchema(dbName).get();
        GenericTableSchema schema = dbSchema.table(tableName, GenericTableSchema.class);
        return schema.createRow((ObjectNode)rowJson);
    }

    public String getParentTable() {
        return parentTable;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public String getParentColumn() {
        return parentColumn;
    }

    public String getTableName() {
        return tableName;
    }

    public Row<GenericTableSchema> getRow() {
        return row;
    }
}
