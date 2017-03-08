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
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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

    private static Map<OvsdbClient, Map<OvsdbClient, SettableFuture>> pendingConnectionClients =
            new ConcurrentHashMap<>();

    private final ScheduledExecutorService executorService;
    private static final int ECHO_TIMEOUT = 10;

    public StalePassiveConnectionService(final ScheduledExecutorService executorService) {
        this.executorService = executorService;
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
        final Map<OvsdbClient, SettableFuture> clientFutureMap = new ConcurrentHashMap<>();
        pendingConnectionClients.put(newOvsdbClient, clientFutureMap);

        // scheduled task for ping response timeout. Connections that don't response to the
        // ping or haven't disconnected after the timeout will be closed
        final ScheduledFuture<?> echoTimeoutFuture =
                executorService.schedule(() -> {
                    for (OvsdbClient client : clientFutureMap.keySet()) {
                        Future<?> clientFuture = clientFutureMap.get(client);
                        if (!clientFuture.isDone() && !clientFuture.isCancelled()) {
                            clientFuture.cancel(true);
                        }
                        if (client.isActive()) {
                            client.disconnect();
                        }
                    }
                }, ECHO_TIMEOUT, TimeUnit.SECONDS);

        // for every connection create a SettableFuture, save it to 'clientFutureMap', and send a ping (echo).
        // The ping results in either:
        // 1. ping response returns - the connection is active
        // 2. the netty connection is closed due to IO exception -
        // The future is removed from the 'clientFutureMap' when the onSuccess event for each future arrives
        // If the map is empty we proceed with new connection process
        for (final OvsdbClient client : clientsFromSameNode) {
            SettableFuture clientFuture = SettableFuture.create();
            clientFutureMap.put(client, clientFuture);
            Futures.addCallback(clientFuture,
                    createStaleConnectionFutureCallback(client, newOvsdbClient, clientFutureMap, echoTimeoutFuture));
            Futures.addCallback(client.echo(),
                    createStaleConnectionFutureCallback(client, newOvsdbClient, clientFutureMap, echoTimeoutFuture));
        }
    }

    /**
     * Notify the service that the given client has disconnected.
     * @param disconnectedClient the client just disconnected
     */
    public void clientDisconnected(OvsdbClient disconnectedClient) {
        for (OvsdbClient pendingClient : pendingConnectionClients.keySet()) {
            // set the future result for pending connections that wait for this client to be disconnected
            if (pendingClient.getConnectionInfo().getRemoteAddress()
                    .equals(disconnectedClient.getConnectionInfo().getRemoteAddress())) {
                Map<OvsdbClient, SettableFuture> clientFutureMap = pendingConnectionClients.get(pendingClient);
                if (clientFutureMap.containsKey(disconnectedClient)) {
                    clientFutureMap.get(disconnectedClient).set(null);
                }
            }
        }
    }

    @Override
    public void close() {
    }

    private FutureCallback<List<String>> createStaleConnectionFutureCallback(
            final OvsdbClient cbForClient, final OvsdbClient newClient,
            final Map<OvsdbClient, SettableFuture> clientFutureMap, final ScheduledFuture<?> echoTimeoutFuture) {
        return new FutureCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> result) {
                // The future is removed from the 'clientFutureMap' when the onSuccess event for each future arrives
                // If the map is empty we proceed with new connection process
                clientFutureMap.remove(cbForClient);
                if (clientFutureMap.isEmpty()) {
                    if (!echoTimeoutFuture.isDone() && !echoTimeoutFuture.isCancelled()) {
                        echoTimeoutFuture.cancel(true);
                    }
                    OvsdbConnectionService.notifyListenerForPassiveConnection(newClient);
                    pendingConnectionClients.remove(newClient);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Error in checking stale connections)", throwable);
            }
        };
    }
}
