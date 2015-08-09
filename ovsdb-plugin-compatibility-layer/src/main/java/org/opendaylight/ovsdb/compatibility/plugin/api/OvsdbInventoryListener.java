/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.compatibility.plugin.api;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.notation.Row;

import java.net.InetAddress;

public interface OvsdbInventoryListener {
    void nodeAdded(Node node, InetAddress address, int port);
    void nodeRemoved(Node node);
    void rowAdded(Node node, String tableName, String uuid, Row row);
    void rowUpdated(Node node, String tableName, String uuid, Row old, Row row);
    void rowRemoved(Node node, String tableName, String uuid, Row row, Object context);
}
