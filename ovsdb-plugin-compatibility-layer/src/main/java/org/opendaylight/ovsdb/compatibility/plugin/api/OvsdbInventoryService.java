/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.compatibility.plugin.api;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;

public interface OvsdbInventoryService{
    ConcurrentMap<String, ConcurrentMap<String, Row>> getCache(Node n, String databaseName);
    ConcurrentMap<String, Row> getTableCache(Node n, String databaseName, String tableName);
    Row getRow(Node n, String databaseName, String tableName, String uuid);
    void updateRow(Node n, String databaseName, String tableName, String uuid, Row row);
    void removeRow(Node n, String databaseName, String tableName, String uuid);
    void processTableUpdates(Node n, String databaseName, TableUpdates tableUpdates);
    void printCache(Node n);
    void addNode(Node n, Set<Property> props);
    void notifyNodeAdded(Node n, InetAddress address, int port);
    void removeNode(Node n);
    void addNodeProperty(Node node, UpdateType type, Set<Property> props);
}