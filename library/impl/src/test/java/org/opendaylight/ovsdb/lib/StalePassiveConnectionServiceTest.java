/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.lib.impl.StalePassiveConnectionService;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ StalePassiveConnectionService.class })
public class StalePassiveConnectionServiceTest {

    private Map<OvsdbClient, SettableFuture> clientFts = new HashMap<>();
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private StalePassiveConnectionService stalePassiveConnectionService
            = new StalePassiveConnectionService((client) -> {
                clientFts.get(client).set(null);
                return null;
            });

    @Test
    public void testFirstClientAlive() throws Exception {
        OvsdbClient firstClient = createClient("127.0.0.1", 8001);
        OvsdbClient secondClient = createClient("127.0.0.1", 8002);
        when(firstClient.echo()).thenAnswer((mock) -> {
            return Futures.scheduleAsync(() -> {
                return Futures.immediateFuture(null);
            }, 100, TimeUnit.MILLISECONDS, scheduledExecutorService);
        });
        stalePassiveConnectionService.handleNewPassiveConnection(secondClient, Lists.newArrayList(firstClient));
        //wait for first client echo succeeds
        firstClient.echo().get();

        assertFalse("Second client should not be processed while first client is there",
                clientFts.get(secondClient).isDone());
        stalePassiveConnectionService.clientDisconnected(firstClient);
        clientFts.get(secondClient).get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testFirstClientInActive() throws Exception {
        OvsdbClient firstClient = createClient("127.0.0.1", 8001);
        OvsdbClient secondClient = createClient("127.0.0.1", 8002);
        when(firstClient.echo()).thenAnswer((mock) -> {
            return Futures.scheduleAsync(() -> {
                return Futures.immediateFailedFuture(new RuntimeException("Echo failed"));
            }, 100, TimeUnit.MILLISECONDS, scheduledExecutorService);
        });
        stalePassiveConnectionService.handleNewPassiveConnection(secondClient, Lists.newArrayList(firstClient));
        clientFts.get(secondClient).get(5, TimeUnit.SECONDS);
    }

    private OvsdbClient createClient(String ip, int port) {
        OvsdbClient ovsdbClient = mock(OvsdbClient.class);
        when(ovsdbClient.getConnectionInfo()).thenReturn(new ConnectionInfo(ip, port));
        clientFts.put(ovsdbClient, SettableFuture.create());
        return ovsdbClient;
    }

    private static class ConnectionInfo extends OvsdbConnectionInfo {

        private String host;
        private int port;

        ConnectionInfo(String host, int port) {
            super(null, null);
            this.host = host;
            this.port = port;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ConnectionInfo other = (ConnectionInfo) obj;
            return Objects.equals(host, other.host) && port == other.port;
        }

        @Override
        public int hashCode() {
            return host.hashCode() + port;
        }
    }
}
