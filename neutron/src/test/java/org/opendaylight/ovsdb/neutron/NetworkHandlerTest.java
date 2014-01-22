/*******************************************************************************
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Added unit tests for the NetworkHandler class.
 *******************************************************************************/

package org.opendaylight.ovsdb.neutron;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TenantNetworkManager.class)

public class NetworkHandlerTest {

    NetworkHandler testNetworkHandler = new NetworkHandler();

    @Test
    public void testCanCreateNetwork() throws Exception {
        NeutronNetwork mockNet = mock(NeutronNetwork.class);

        when(mockNet.isShared())
                .thenReturn(true)
                .thenReturn(false);

        assertEquals(HttpURLConnection.HTTP_NOT_ACCEPTABLE, testNetworkHandler.canCreateNetwork(mockNet));
        assertEquals(HttpURLConnection.HTTP_CREATED, testNetworkHandler.canCreateNetwork(mockNet));

    }

    @Test
    public void testCanUpdateNetwork() {
        NeutronNetwork delta = new NeutronNetwork();
        NeutronNetwork original = new NeutronNetwork();

        assertEquals(HttpURLConnection.HTTP_OK, testNetworkHandler.canUpdateNetwork(delta, original));
    }

    @Test
    public void testCanDeleteNetwork() throws Exception {
        NeutronNetwork network = new NeutronNetwork();

        assertEquals(HttpURLConnection.HTTP_OK, testNetworkHandler.canDeleteNetwork(network));
    }

    @Test
    public void testNeutronNetworkDeleted() throws Exception {
        String netId = "6cfdb7";

        NeutronNetwork mockNet = mock(NeutronNetwork.class);
        when(mockNet.isShared()).thenReturn(false);
        when(mockNet.getID()).thenReturn(netId);

        NetworkHandler spy = spy(testNetworkHandler);

        TenantNetworkManager tenantNetworkManager = mock(TenantNetworkManager.class);
        Whitebox.setInternalState(TenantNetworkManager.class, "tenantHelper", tenantNetworkManager);

        when(spy.canDeleteNetwork(mockNet))
                .thenReturn(HttpURLConnection.HTTP_BAD_REQUEST)
                .thenCallRealMethod();

        spy.neutronNetworkDeleted(mockNet);
        spy.neutronNetworkDeleted(mockNet);

        verify(tenantNetworkManager).networkDeleted(netId);

    }
}
