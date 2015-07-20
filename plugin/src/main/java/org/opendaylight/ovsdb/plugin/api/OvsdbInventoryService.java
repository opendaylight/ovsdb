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
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

public interface OvsdbInventoryService {
    ConcurrentMap<String, ConcurrentMap<String, Row>> getCache(Node n, String databaseName);
    ConcurrentMap<String, Row> getTableCache(Node n, String databaseName, String tableName);
    Row getRow(Node n, String databaseName, String tableName, String uuid);
    void updateRow(Node n, String databaseName, String tableName, String uuid, Row row);
    void removeRow(Node n, String databaseName, String tableName, String uuid);
    void processTableUpdates(Node n, String databaseName, TableUpdates tableUpdates);
    void printCache(Node n);
    void notifyNodeAdded(Node n, InetAddress address, int port);
    void removeNode(Node n);
}