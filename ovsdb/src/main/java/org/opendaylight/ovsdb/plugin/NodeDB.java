package org.opendaylight.ovsdb.plugin;

import java.util.Map;

import com.google.common.collect.Maps;

import org.apache.commons.collections.MapUtils;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class NodeDB <T extends Table<?>>{
    Map<String, Map<String, T>> cache = Maps.newHashMap();

    public Map<String, Map<String, T>> getTableCache() {
        return cache;
    }

    public Map<String, T> getTableCache(String tableName) {
        return cache.get(tableName);
    }

    private void setTableCache(String tableName,  Map<String, T> tableCache) {
        cache.put(tableName, tableCache);
    }

    public T getRow (String tableName, String uuid) {
        Map<String, T> tableCache = getTableCache(tableName);
        if (tableCache != null) {
            return tableCache.get(uuid);
        }
        return null;
    }

    public void updateRow(String tableName, String uuid, T row) {
        Map<String, T> tableCache = getTableCache(tableName);
        if (tableCache == null) {
            tableCache = Maps.newHashMap();
            setTableCache(tableName, tableCache);
        }
        tableCache.put(uuid, row);
    }

    public void removeRow(String tableName, String uuid) {
        Map<String, T> tableCache = getTableCache(tableName);
        if (tableCache != null) {
            tableCache.remove(uuid);
        }
    }

    public void printTableCache() {
        MapUtils.debugPrint(System.out, null, cache);
    }
}
