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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private static Map<OvsdbClient, Map<OvsdbClient, SettableFuture<List<String>>>> pendingConnectionClients =
            new ConcurrentHashMap<>();

    Function<OvsdbClient, Void> callback;

    public StalePassiveConnectionService(Function<OvsdbClient, Void> callback) {
        this.callback = callback;
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
        final Map<OvsdbClient, SettableFuture<List<String>>> oldClients = new ConcurrentHashMap<>();
        pendingConnectionClients.put(newOvsdbClient, oldClients);

        /*
            if old client echo succeeds
               do not notify new client as it has to wait
            else
                if all old clients got disconnected/echo failed notify the new client
         */
        for (final OvsdbClient oldClient : clientsFromSameNode) {
            oldClients.put(oldClient, SettableFuture.create());
            Futures.addCallback(oldClient.echo(),
                new FutureCallback<List<String>>() {
                    @Override
                    public void onSuccess(List<String> result) {
                        //old client active
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        //old client inactive
                        oldClients.remove(oldClient);
                        if (oldClients.isEmpty()) {
                            if (pendingConnectionClients.remove(newOvsdbClient) != null) {
                                //new client still active notify it
                                callback.apply(newOvsdbClient);
                            }
                        }
                    }
                });
        }
    }

    /**
     * Notify the service that the given client has disconnected.
     * @param disconnectedClient the client just disconnected
     */
    public synchronized void clientDisconnected(OvsdbClient disconnectedClient) {
        /*
            if new or pending client got disconnected remove it from pending list
            if old client got disconnected update its future
         */
        Iterator<Entry<OvsdbClient, Map<OvsdbClient, SettableFuture<List<String>>>>> pendingClientsIterator
                = pendingConnectionClients.entrySet().iterator();
        while (pendingClientsIterator.hasNext()) {
            Entry<OvsdbClient, Map<OvsdbClient, SettableFuture<List<String>>>> pendingClientEntry
                    = pendingClientsIterator.next();
            OvsdbClient pendingClient = pendingClientEntry.getKey();

            //compare ip address and port aswell
            if (pendingClient.getConnectionInfo().equals(disconnectedClient.getConnectionInfo())) {
                //new client disconnected
                pendingClientsIterator.remove();
                continue;
            }
            Iterator<Entry<OvsdbClient, SettableFuture<List<String>>>> oldClientsIterator
                    = pendingClientEntry.getValue().entrySet().iterator();
            while (oldClientsIterator.hasNext()) {
                Entry<OvsdbClient, SettableFuture<List<String>>> oldClientEntry = oldClientsIterator.next();
                if (oldClientEntry.getKey().getConnectionInfo().equals(disconnectedClient.getConnectionInfo())) {
                    //old client disconnected
                    oldClientEntry.getValue().cancel(true);
                    oldClientsIterator.remove();
                    continue;
                }
            }
            if (pendingClientEntry.getValue().isEmpty()) {
                pendingClientsIterator.remove();
                callback.apply(pendingClient);
            }
        }
    }

    @Override
    public void close() {
    }
}