/*
 * Copyright © 2016, 2017 NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StalePassiveConnectionService provides functionalities to clean up stale passive connections
 * from the same node before new connection request arrives, especially for connection flapping scenarios.
 *
 * <p>When new connection arrives all connections from the same node are pinged. The pings cause
 * the stale netty connections to close due to IOException. Those have not been closed after a timeout
 * will be closed programmatically. New connection request handling is then proceeded after all
 * stale connections are cleaned up in the OvsdbConnectionService
 *
 * @author Vinh Nguyen (vinh.nguyen@hcl.com) on 6/10/16.
 */
public class StalePassiveConnectionService implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(StalePassiveConnectionService.class);

    private final Map<OvsdbClient, Set<OvsdbClient>> pendingClients = new ConcurrentHashMap<>();
    private final Function<OvsdbClient, Void> clientNotificationCallback;

    public StalePassiveConnectionService(Function<OvsdbClient, Void> clientNotificationCallback) {
        this.clientNotificationCallback = clientNotificationCallback;
    }

    public Map<OvsdbClient, Set<OvsdbClient>> getPendingClients() {
        return new HashMap<>(pendingClients);
    }

    /**
     * This method makes sure that all stale connections from the same node are properly cleaned up before processing
     * new connection request.
     *
     * @param newOvsdbClient the connecting OvsdbClient
     * @param clientsFromSameNode list of existing OvsdbClients from the same node as the new OvsdbClient
     */
    public void handleNewPassiveConnection(final OvsdbClient newOvsdbClient,
                                           final List<OvsdbClient> clientsFromSameNode) {
        LOG.info("Adding client to pending list {}", newOvsdbClient.getConnectionInfo());
        pendingClients.put(newOvsdbClient, new HashSet<>());
        /*
            if old client echo succeeds
               do not notify new client as it has to wait
            else
                if all old clients got disconnected/echo failed notify the new client
         */
        for (final OvsdbClient oldClient : clientsFromSameNode) {
            pendingClients.get(newOvsdbClient).add(oldClient);
            LOG.info("Echo testing client {}", oldClient.getConnectionInfo());
            Futures.addCallback(oldClient.echo(),
                new FutureCallback<List<String>>() {
                    @Override
                    public void onSuccess(List<String> result) {
                        //old client still active
                        LOG.info("Echo testing of old client {} succeeded", oldClient.getConnectionInfo());
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.info("Echo testing of old client {} failed, disconnect and notify clients",
                                oldClient.getConnectionInfo());
                        //disconnect the old client to cleanup, so that new connection can proceed
                        oldClient.disconnect();
                        onInactiveClient(oldClient);
                    }
                }, MoreExecutors.directExecutor());
        }
    }

    /**
     * Notify the service that the given client has disconnected.
     * @param disconnectedClient the client just disconnected
     */
    public synchronized void clientDisconnected(OvsdbClient disconnectedClient) {
        LOG.info("Client disconnected {}", disconnectedClient.getConnectionInfo());
        onInactiveClient(disconnectedClient);
    }

    public synchronized void onInactiveClient(OvsdbClient disconnectedClient) {
        pendingClients.remove(disconnectedClient);
        pendingClients.entrySet().stream().forEach(entry -> entry.getValue().remove(disconnectedClient));
        Optional<OvsdbClient> clientOptional = pendingClients.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(entry -> entry.getKey())
                .findFirst();
        if (clientOptional.isPresent()) {
            OvsdbClient client = clientOptional.orElseThrow();
            LOG.info("Sending notification for client {}", client.getConnectionInfo());
            pendingClients.remove(client);
            clientNotificationCallback.apply(client);
        }
    }

    @Override
    public void close() {
    }
}
