/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.plugin;

import java.util.Map;

import com.google.common.collect.Maps;

import org.apache.commons.collections.MapUtils;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class NodeDB {
    private DatabaseSchema schema;
    Map<String, Map<String, Table<?>>> cache = Maps.newHashMap();

    public DatabaseSchema getSchema() {
        return schema;
    }

    public void setSchema(DatabaseSchema schema) {
        this.schema = schema;
    }

    public Map<String, Map<String, Table<?>>> getTableCache() {
        return cache;
    }

    public Map<String, Table<?>> getTableCache(String tableName) {
        return cache.get(tableName);
    }

    private void setTableCache(String tableName,  Map<String, Table<?>> tableCache) {
        cache.put(tableName, tableCache);
    }

    public Table<?> getRow (String tableName, String uuid) {
        Map<String, Table<?>> tableCache = getTableCache(tableName);
        if (tableCache != null) {
            return tableCache.get(uuid);
        }
        return null;
    }

    public void updateRow(String tableName, String uuid, Table<?> row) {
        Map<String, Table<?>> tableCache = getTableCache(tableName);
        if (tableCache == null) {
            tableCache = Maps.newHashMap();
            setTableCache(tableName, tableCache);
        }
        tableCache.put(uuid, row);
    }

    public void removeRow(String tableName, String uuid) {
        Map<String, Table<?>> tableCache = getTableCache(tableName);
        if (tableCache != null) {
            tableCache.remove(uuid);
        }
    }

    public void printTableCache() {
        MapUtils.debugPrint(System.out, null, schema.getTables());
        MapUtils.debugPrint(System.out, null, cache);
    }
}
