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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;

public interface IConnectionServiceInternal {
    public Connection getConnection(Node node);
    public List<Node> getNodes();
    public int getSupportedOpenflowVersion();
    public Node connect(String identifier, Map<ConnectionConstants, String> params);
    public Boolean setOFController(Node node, String bridgeUUID) throws InterruptedException, ExecutionException;
}
