/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.lib;

import java.net.InetAddress;

/**
 * OvsDBConnection Interface provides OVSDB connection management APIs which includes
 * both Active and Passive connections.
 * From the Library perspective, Active OVSDB connections are those that are initiated from
 * the Controller towards the ovsdb-manager.
 * While Passive OVSDB connections are those that are initiated from the ovs towards
 * the controller.
 *
 * Applications that use OvsDBConnectionService can use the connect APIs to initiate Active
 * connections and can listen to the asynchronous Passive connections via registerForPassiveConnection
 * listener API.
 */

public interface OvsDBConnection {
    /**
     * connect API can be used by the applications to initiate Active connection from
     * the controller towards ovsdb-server
     * @param address IP Address of the remote server that hosts the ovsdb server.
     * @param port Layer 4 port on which the remote ovsdb server is listening on.
     * @return OvsDBClient The primary Client interface for the ovsdb connection.
     */
    public OvsDBClient connect(InetAddress address, int port);

    /**
     * Method to disconnect an existing connection.
     * @param client that represents the ovsdb connection.
     */
    public void disconnect(OvsDBClient client);

    /**
     * Method to register a Passive Connection Listener with the ConnectionService.
     * @param listener Passive Connection listener interested in Passive OVSDB connection requests.
     */
    public void registerForPassiveConnection(OvsDBConnectionListener listener);
}
