package org.opendaylight.ovsdb.plugin;

import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public interface InventoryServiceInternal {
    public Map<String, Map<String, Table<?>>> getCache(Node n);
    public Map<String, Table<?>> getTableCache(Node n, String tableName);
    public Table<?> getRow (Node n, String tableName, String uuid);
    public void updateRow(Node n, String tableName, String uuid, Table<?> row);
    public void removeRow(Node n, String tableName, String uuid);
    public void processTableUpdates(Node n, TableUpdates tableUpdates);
    public void updateDatabaseSchema(Node n, DatabaseSchema schema);
    public     DatabaseSchema getDatabaseSchema(Node n);
    public void printCache(Node n);

    public void addNodeProperty(Node n, Property prop);
}