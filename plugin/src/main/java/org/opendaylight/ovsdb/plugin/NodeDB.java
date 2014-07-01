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
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections.MapUtils;
import org.opendaylight.ovsdb.lib.notation.Row;

import com.google.common.collect.Maps;

public class NodeDB {
    ConcurrentMap<String, ConcurrentMap<String, TableDB>> dbCache = Maps.newConcurrentMap();

    public ConcurrentMap<String, ConcurrentMap<String,Row>> getDatabase(String dbName) {
        ConcurrentMap<String, TableDB> tdbMap = dbCache.get(dbName);
        if (tdbMap == null) return null;
        ConcurrentMap<String, ConcurrentMap<String,Row>> retMap = Maps.newConcurrentMap();
        for (String tableName : tdbMap.keySet()) {
            TableDB tdb = tdbMap.get(tableName);
            retMap.put(tableName, tdb.getTableCache(tableName));
        }
        return retMap;
    }

    public ConcurrentMap<String, Row> getTableCache(String dbName, String tableName) {
        ConcurrentMap<String, ConcurrentMap<String,Row>> tdbMap = getDatabase(dbName);
        if (tdbMap == null) return null;
        return tdbMap.get(tableName);
    }

    private void setDBCache(String dbName,  ConcurrentMap<String, TableDB> table) {
        dbCache.put(dbName, table);
    }

    public Row getRow (String dbName, String tableName, String uuid) {
        ConcurrentMap<String, ConcurrentMap<String, Row>> db = getDatabase(dbName);
        if (db == null) return null;
        ConcurrentMap<String, Row> tdb = db.get(tableName);
        if (tdb == null) return null;
        return tdb.get(uuid);
    }

    public void updateRow(String dbName, String tableName, String uuid, Row row) {
        ConcurrentMap<String, TableDB> db = dbCache.get(dbName);
        if (db == null) {
            db = Maps.newConcurrentMap();
            setDBCache(dbName, db);
        }
        TableDB tdb = db.get(tableName);
        if (tdb == null) {
            tdb = new TableDB();
            db.put(tableName, tdb);
        }
        tdb.updateRow(tableName, uuid, row);
    }

    public void removeRow(String dbName, String tableName, String uuid) {
        ConcurrentMap<String, TableDB> db = dbCache.get(dbName);
        if (db == null) return;
        TableDB tdb = db.get(tableName);
        if (tdb == null) return;
        tdb.removeRow(tableName, uuid);
    }

    public void printTableCache() {
        MapUtils.debugPrint(System.out, null, dbCache);
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