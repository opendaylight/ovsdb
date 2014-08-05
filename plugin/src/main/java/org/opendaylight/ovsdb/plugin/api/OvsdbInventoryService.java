/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.plugin.api;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;

public interface OvsdbInventoryService extends IPluginInInventoryService {
    public ConcurrentMap<String, ConcurrentMap<String, Row>> getCache(Node n, String databaseName);
    public ConcurrentMap<String, Row> getTableCache(Node n, String databaseName, String tableName);
    public Row getRow (Node n, String databaseName, String tableName, String uuid);
    public void updateRow(Node n, String databaseName, String tableName, String uuid, Row row);
    public void removeRow(Node n, String databaseName, String tableName, String uuid);
    public void processTableUpdates(Node n, String databaseName,TableUpdates tableUpdates);
    public void printCache(Node n);
    public void addNode(Node n, Set<Property> props);
    public void notifyNodeAdded(Node n, InetAddress address, int port);
    public void removeNode(Node n);
    public void addNodeProperty(Node node, UpdateType type, Set<Property> props);
}