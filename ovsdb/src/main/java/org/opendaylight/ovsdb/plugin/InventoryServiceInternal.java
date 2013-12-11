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
import java.util.Set;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public interface InventoryServiceInternal extends IPluginInInventoryService {
    public Map<String, Map<String, Table<?>>> getCache(Node n);
    public Map<String, Table<?>> getTableCache(Node n, String tableName);
    public Table<?> getRow (Node n, String tableName, String uuid);
    public void updateRow(Node n, String tableName, String uuid, Table<?> row);
    public void removeRow(Node n, String tableName, String uuid);
    public void processTableUpdates(Node n, TableUpdates tableUpdates);
    public void updateDatabaseSchema(Node n, DatabaseSchema schema);
    public DatabaseSchema getDatabaseSchema(Node n);
    public void printCache(Node n);

    public void addNode(Node n, Set<Property> props);
    public void notifyNodeAdded(Node n);
    public void removeNode(Node n);
    public void addNodeProperty(Node node, UpdateType type, Set<Property> props);
}