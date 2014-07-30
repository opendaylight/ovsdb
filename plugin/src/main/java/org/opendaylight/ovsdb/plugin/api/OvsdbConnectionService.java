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

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.plugin.Connection;

public interface OvsdbConnectionService {
    public Connection getConnection(Node node);
    public List<Node> getNodes();
    public Node connect(String identifier, Map<ConnectionConstants, String> params);
}
