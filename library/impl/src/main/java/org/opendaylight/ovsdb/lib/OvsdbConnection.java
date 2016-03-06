/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib;

import java.net.InetAddress;
import java.util.Collection;
import javax.net.ssl.SSLContext;

/**
 * OvsDBConnection Interface provides OVSDB connection management APIs which includes
 * both Active and Passive connections.
 * From the Library perspective, Active OVSDB connections are those that are initiated from
 * the Controller towards the ovsdb-manager.
 * While Passive OVSDB connections are those that are initiated from the ovs towards
 * the controller.
 *
 * Applications that use OvsDBConnectionService can use the connect APIs to initiate Active
 * connections and can listen to the asynchronous Passive connections via registerConnectionListener
 * listener API.
 */

public interface OvsdbConnection {
    /**
     * connect API can be used by the applications to initiate Active connection from
     * the controller towards ovsdb-server
     * @param address IP Address of the remote server that hosts the ovsdb server.
     * @param port Layer 4 port on which the remote ovsdb server is listening on.
     * @return OvsDBClient The primary Client interface for the ovsdb connection.
     */
    OvsdbClient connect(final InetAddress address, final int port);

    /**
     * connect API can be used by the applications to initiate Active ssl
     * connection from the controller towards ovsdb-server
     * @param address IP Address of the remote server that hosts the ovsdb server.
     * @param port Layer 4 port on which the remote ovsdb server is listening on.
     * @param sslContext Netty sslContext for channel configuration
     * @return OvsDBClient The primary Client interface for the ovsdb connection.
     */
    OvsdbClient connectWithSsl(final InetAddress address, final int port,
                               final SSLContext sslContext);

    /**
     * Method to disconnect an existing connection.
     * @param client that represents the ovsdb connection.
     */
    void disconnect(OvsdbClient client);

    /**
     * Method to start ovsdb server for passive connection
     */
    boolean startOvsdbManager(final int ovsdbListenPort);

    /**
     * Method to start ovsdb server for passive connection with SSL
     */
    boolean startOvsdbManagerWithSsl(final int ovsdbListenPort,
                                     final SSLContext sslContext);

    /**
     * Method to register a Passive Connection Listener with the ConnectionService.
     * @param listener Passive Connection listener interested in Passive OVSDB connection requests.
     */
    void registerConnectionListener(OvsdbConnectionListener listener);

    /**
     * Method to unregister a Passive Connection Listener with the ConnectionService.
     * @param listener
     */
    void unregisterConnectionListener(OvsdbConnectionListener listener);

    /**
     * Returns a Collection of all the active OVSDB Connections.
     *
     * @return Collection of all the active OVSDB Connections
     */
    Collection<OvsdbClient> getConnections();
}
