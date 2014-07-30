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

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.notation.Row;

public interface OvsdbInventoryListener {
    public void nodeAdded(Node node);
    public void nodeRemoved(Node node);
    public void rowAdded(Node node, String tableName, String uuid, Row row);
    public void rowUpdated(Node node, String tableName, String uuid, Row old, Row row);
    public void rowRemoved(Node node, String tableName, String uuid, Row row, Object context);
}
