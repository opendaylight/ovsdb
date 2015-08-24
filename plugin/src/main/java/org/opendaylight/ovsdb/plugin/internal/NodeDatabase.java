/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.plugin.internal;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections.MapUtils;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;

import com.google.common.collect.Maps;

public class NodeDatabase {
    ConcurrentMap<String, TableDB> dbCache = Maps.newConcurrentMap();

    public ConcurrentMap<String, ConcurrentMap<String, Row>> getDatabase(String dbName) {
        TableDB tdb = dbCache.get(dbName);
        if (tdb == null) {
           return null;
        }
        return tdb.getTableCache();
    }

    public ConcurrentMap<String, Row> getTableCache(String dbName, String tableName) {
        ConcurrentMap<String, ConcurrentMap<String,Row>> tdbMap = getDatabase(dbName);
        if (tdbMap == null) {
           return null;
        }
        return tdbMap.get(tableName);
    }

    private void setDBCache(String dbName,  TableDB table) {
        dbCache.put(dbName, table);
    }

    public Row getRow (String dbName, String tableName, String uuid) {
        ConcurrentMap<String, Row> tdb = this.getTableCache(dbName, tableName);
        if (tdb == null) {
           return null;
        }
        return tdb.get(uuid);
    }

    public void updateRow(String dbName, String tableName, String uuid, Row row) {
        TableDB db = dbCache.get(dbName);
        if (db == null) {
            db = new TableDB();
            setDBCache(dbName, db);
        }
        db.updateRow(tableName, uuid, row);
    }

    public void removeRow(String dbName, String tableName, String uuid) {
        TableDB db = dbCache.get(dbName);
        if (db == null) {
           return;
        }
        db.removeRow(tableName, uuid);
    }

    public void printTableCache() {
        for (String dbName : dbCache.keySet()) {
            System.out.println("Database "+dbName);
            ConcurrentMap<String, ConcurrentMap<String,Row>> tableDB = this.getDatabase(dbName);
            if (tableDB == null) {
               continue;
            }
            for (String tableName : tableDB.keySet()) {
                ConcurrentMap<String, Row> tableRows = this.getTableCache(dbName, tableName);
                System.out.println("\tTable "+tableName);
                for (String uuid : tableRows.keySet()) {
                    Row row = tableRows.get(uuid);
                    Collection<Column> columns = row.getColumns();
                    System.out.print("\t\t"+uuid+ "==");
                    for (Column column : columns) {
                        if (column.getData() != null) {
                           System.out.print(column.getSchema().getName()+" : "+ column.getData()+" ");
                        }
                    }
                    System.out.println("");
                }
                System.out.println("-----------------------------------------------------------");
            }
        }
    }

    public class TableDB {
        ConcurrentMap<String, ConcurrentMap<String, Row>> cache = Maps.newConcurrentMap();

        public ConcurrentMap<String, ConcurrentMap<String, Row>> getTableCache() {
            return cache;
        }

        public ConcurrentMap<String, Row> getTableCache(String tableName) {
            return cache.get(tableName);
        }

        private void setTableCache(String tableName,  ConcurrentMap<String, Row> tableCache) {
            cache.put(tableName, tableCache);
        }

        public Row getRow (String tableName, String uuid) {
            Map<String, Row> tableCache = getTableCache(tableName);
            if (tableCache != null) {
                return tableCache.get(uuid);
            }
            return null;
        }

        public void updateRow(String tableName, String uuid, Row row) {
            ConcurrentMap<String, Row> tableCache = getTableCache(tableName);
            if (tableCache == null) {
                tableCache = Maps.newConcurrentMap();
                setTableCache(tableName, tableCache);
            }
            tableCache.put(uuid, row);
        }

        public void removeRow(String tableName, String uuid) {
            Map<String, Row> tableCache = getTableCache(tableName);
            if (tableCache != null) {
                tableCache.remove(uuid);
            }
        }

        public void printTableCache() {
            MapUtils.debugPrint(System.out, null, cache);
        }
    }
}
