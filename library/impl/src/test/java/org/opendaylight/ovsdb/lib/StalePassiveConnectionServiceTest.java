/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opendaylight.ovsdb.lib.impl.StalePassiveConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class StalePassiveConnectionServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(StalePassiveConnectionService.class);
    private static final String NOTIFIED = "NOTIFIED";
    private Map<OvsdbClient, SettableFuture<String>> clientJobRunFutures = new HashMap<>();
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private StalePassiveConnectionService staleConnectionService
            = new StalePassiveConnectionService((client) -> {
                if (clientJobRunFutures.get(client) != null) {
                    clientJobRunFutures.get(client).set(NOTIFIED);
                }
                return null;
            });

    private OvsdbClient firstClient = createClient("127.0.0.1", 8001);
    private OvsdbClient secondClient = createClient("127.0.0.1", 8002);
    private OvsdbClient thirdClient = createClient("127.0.0.1", 8003);

    @Test
    public void testFirstClientAlive() throws Exception {
        staleConnectionService.handleNewPassiveConnection(secondClient, Lists.newArrayList(firstClient));
        clientShouldNotBeNotified(secondClient, "Second client should not be processed while first client is active");
        staleConnectionService.clientDisconnected(firstClient);
        clientShouldBeNotified(secondClient, "Second client should be notified after first client disconnect");
    }

    @Test
    public void testSecondClientDisconnect() throws Exception {
        staleConnectionService.handleNewPassiveConnection(secondClient, Lists.newArrayList(firstClient));
        clientShouldNotBeNotified(secondClient, "Second client should not be processed while first client is active");
        staleConnectionService.clientDisconnected(secondClient);
        clientShouldNotBeNotified(secondClient, "Second client should not be processed post its disconnect");
        clientShouldBeClearedFromPendingList(secondClient, "Second client should be cleared from pending state");
    }

    @Test
    public void testFirstClientInActive() throws Exception {
        firstClient.echo();
        when(firstClient.echo()).thenAnswer(delayedEchoFailedResponse(100, MILLISECONDS));
        staleConnectionService.handleNewPassiveConnection(secondClient, Lists.newArrayList(firstClient));
        clientShouldBeNotified(secondClient, "Second client should be notified after first client echo failed");
        clientShouldBeClearedFromPendingList(secondClient, "Second client should be cleared from pending state");
    }

    @Test
    public void testThreeClients() throws Exception {
        staleConnectionService.handleNewPassiveConnection(secondClient, Lists.newArrayList(firstClient));
        staleConnectionService.handleNewPassiveConnection(thirdClient, Lists.newArrayList(firstClient, secondClient));
        clientShouldNotBeNotified(thirdClient, "Third client should not be processed while first client is there");
        clientShouldNotBeNotified(secondClient, "Second client should not be processed while first client is there");

        //disconnect first client
        staleConnectionService.clientDisconnected(firstClient);
        //now second client should be processed
        clientShouldBeNotified(secondClient, "Second client should be notified after first client disconnected");
        clientShouldNotBeNotified(thirdClient, "Third client should not be processed while second client is active");

        //disconnect second client
        staleConnectionService.clientDisconnected(secondClient);
        //now third client should be processed
        clientShouldBeNotified(secondClient, "Third client should be notified after second client also disconnected");
    }

    @Test
    public void testDelayedFirstClientFailure() throws Exception {
        /*
        first client arrived
        second client arrived
        first client echo success
        keep second client on wait list

        third client arrived
        second client echo success, first client echo failed
        process second client //catch the moment first clients echo failed it is considered inactive

        second client disconnected
        process third client
         */
        staleConnectionService.handleNewPassiveConnection(secondClient, Lists.newArrayList(firstClient));

        when(firstClient.echo()).thenAnswer(delayedEchoFailedResponse(100, MILLISECONDS));
        staleConnectionService.handleNewPassiveConnection(thirdClient, Lists.newArrayList(firstClient, secondClient));
        //now second client should be processed
        clientShouldBeNotified(secondClient, "Second client should be processed post first client echo failed");
        clientShouldNotBeNotified(thirdClient, "Third client should not be processed while second client is active");
        //disconnect second client
        staleConnectionService.clientDisconnected(secondClient);
        //now third client should be processed
        clientShouldBeNotified(thirdClient, "Third client should be processed post second client disconnect also");
    }

    private void clientShouldBeClearedFromPendingList(OvsdbClient client, String msg) {
        assertTrue(msg, !staleConnectionService.getPendingClients().containsKey(client));
    }

    private void clientShouldBeNotified(OvsdbClient client, String msg)
            throws InterruptedException, ExecutionException, TimeoutException {
        assertEquals(msg, clientJobRunFutures.get(client).get(1, SECONDS), NOTIFIED);
        clientShouldBeClearedFromPendingList(client, "client should be cleared from pending state");
    }

    private void clientShouldNotBeNotified(OvsdbClient client, String msg)
            throws ExecutionException, InterruptedException {
        try {
            clientJobRunFutures.get(client).get(1, SECONDS);
            fail(msg);
        } catch (TimeoutException e) {
            LOG.trace("Expected exception");
        }
    }

    private Answer<Object> delayedEchoResponse(int delay, TimeUnit timeUnit) {
        return (mock) -> Futures.scheduleAsync(() -> Futures.immediateFuture(null),
                delay, timeUnit, scheduledExecutorService);
    }

    private Answer<Object> delayedEchoFailedResponse(int delay, TimeUnit timeUnit) {
        return (mock) -> Futures.scheduleAsync(() -> Futures.immediateFailedFuture(new RuntimeException("Echo failed")),
                delay, timeUnit, scheduledExecutorService);
    }

    private OvsdbClient createClient(String ip, int port) {
        OvsdbClient ovsdbClient = mock(OvsdbClient.class);
        clientJobRunFutures.put(ovsdbClient, SettableFuture.create());
        when(ovsdbClient.echo()).thenAnswer(delayedEchoResponse(100, MILLISECONDS));
        OvsdbConnectionInfo connectionInfo = mock(OvsdbConnectionInfo.class);
        when(connectionInfo.toString()).thenReturn("Host:" + ip + ",Port:" + port);
        when(ovsdbClient.getConnectionInfo()).thenReturn(connectionInfo);
        return ovsdbClient;
    }
}
