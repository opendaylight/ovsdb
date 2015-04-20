/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.osgi.framework.ServiceReference;

/**
 * Unit test for {@link ProviderNetworkManagerImpl}
 */
/* TODO SB_MIGRATION */ @Ignore
@RunWith(MockitoJUnitRunner.class)
public class ProviderNetworkManagerImplTest {

    @InjectMocks private ProviderNetworkManagerImpl providerNetworkManagerImpl;
    @Spy private HashMap<Node, NetworkingProvider> nodeToProviderMapping;

    /**
     * Test method {@link ProviderNetworkManagerImpl#getProvider(Node)}
     */
    @Test
    public void testGetProvider(){
        // TODO test the method with no networkingProvider in the map
        // Could not be done as ProviderEntry is a private inner class of ProviderNetworkManagerImpl
//        assertNotNull("Error, did not return the networkingProvider of the specified node", providerNetworkManagerImpl.getProvider(any(Node.class));

        Node node = mock(Node.class);
        NetworkingProvider networkingProvider = mock(NetworkingProvider.class);
        nodeToProviderMapping.put(node, networkingProvider);
        assertEquals("Error, did not return the networkingProvider of the specified node", networkingProvider, providerNetworkManagerImpl.getProvider(node));
    }

    /**
     * Test methods {@link ProviderNetworkManagerImpl#providerRemoved(ServiceReference)}
     * and {@link ProviderNetworkManagerImpl#providerAdded(ServiceReference, NetworkingProvider)}
     */
    @Test
    public void testProviderAddedAndRemoved() throws Exception {
        Field field = ProviderNetworkManagerImpl.class.getDeclaredField("providers");
        field.setAccessible(true);
        HashMap map = (HashMap) field.get(providerNetworkManagerImpl);

        ServiceReference ref = mock(ServiceReference.class);
        when(ref.getProperty(org.osgi.framework.Constants.SERVICE_ID)).thenReturn(Long.valueOf(1));

        providerNetworkManagerImpl.providerAdded(ref, mock(NetworkingProvider.class));

        assertEquals("Error, providerAdded() did not add the provider", 1, map.size());

        providerNetworkManagerImpl.providerRemoved(ref);

        assertEquals("Error, providerRemoved() did not remove the provider", 0, map.size());
    }
}
