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

import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

import java.net.InetAddress;

public interface OvsdbInventoryListener {
    void nodeAdded(Node node, InetAddress address, int port);
    void nodeRemoved(Node node);
    void rowAdded(Node node, String tableName, String uuid, Row row);
    void rowUpdated(Node node, String tableName, String uuid, Row old, Row row);
    void rowRemoved(Node node, String tableName, String uuid, Row row, Object context);
}
