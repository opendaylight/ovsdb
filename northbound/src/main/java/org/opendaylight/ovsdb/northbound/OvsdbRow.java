/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.northbound;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.annotation.XmlElement;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Deprecated
public class OvsdbRow {
    private static final String PARENTUUID = "parent_uuid";
    private static final String ROW = "row";

    @XmlElement(name=PARENTUUID)
    String parentUuid;

    String tableName;

    @XmlElement(name=ROW)
    Row<GenericTableSchema> row;

    public OvsdbRow() {
    }

    public OvsdbRow(String parentUuid, String tableName, Row<GenericTableSchema> row) {
        this.parentUuid = parentUuid;
        this.tableName = tableName;
        this.row = row;
    }

    public static OvsdbRow fromJsonNode(OvsdbClient client, String dbName, JsonNode json) {
        JsonNode parentUuidNode = json.get(PARENTUUID);
        String parentUuid = null;
        if (parentUuidNode != null) parentUuid = parentUuidNode.asText();

        JsonNode rowNode = json.get(ROW);
        if (rowNode == null) return null;
        for(Iterator<String> fieldNames = rowNode.fieldNames(); fieldNames.hasNext();) {
            String tableName = fieldNames.next();
            Row<GenericTableSchema> row = null;
            try {
                row = getRow(client, dbName, tableName, rowNode.get(tableName));
            } catch (InterruptedException | ExecutionException | IOException e) {
                e.printStackTrace();
                return null;
            }
            return new OvsdbRow(parentUuid, tableName, row);
        }
        return null;
    }

    public static Row<GenericTableSchema> getRow(OvsdbClient client, String dbName, String tableName, JsonNode rowJson) throws InterruptedException, ExecutionException, JsonParseException, IOException {
        DatabaseSchema dbSchema = client.getSchema(dbName).get();
        GenericTableSchema schema = dbSchema.table(tableName, GenericTableSchema.class);
        return schema.createRow((ObjectNode)rowJson);
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public String getTableName() {
        return tableName;
    }

    public Row<GenericTableSchema> getRow() {
        return row;
    }

    @Override
    public String toString() {
        return "OVSDBRow [parentUuid=" + parentUuid + ", tableName="
                + tableName + ", row=" + row + "]";
    }
}
