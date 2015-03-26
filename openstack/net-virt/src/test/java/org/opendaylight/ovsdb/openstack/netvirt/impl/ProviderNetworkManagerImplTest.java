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

import java.util.HashMap;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.osgi.framework.ServiceReference;

/**
 * Unit test for class ProviderNetworkManagerImpl
 *
 * TODO In order to have deeper tests, private inner class
 *  ProviderEntry of ProviderNetworkManager should be public.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProviderNetworkManagerImplTest {

    @InjectMocks private ProviderNetworkManagerImpl providerNetworkManagerImpl;
    @Spy private HashMap<Node, NetworkingProvider> nodeToProviderMapping;

    private Random r = new Random();

    /**
     * Test method {@link ProviderNetworkManagerImpl#getProvider(Node)}
     */
    @Test
    public void testGetProvider(){
        Node node = mock(Node.class);
        NetworkingProvider networkingProvider = mock(NetworkingProvider.class);

        // populate nodeToProviderMapping
        nodeToProviderMapping.put(node, networkingProvider);

        // test when nodeToProviderMapping is populate
        assertEquals("Error, did not return the networkingProvider of the specified node", networkingProvider, providerNetworkManagerImpl.getProvider(node));
    }

    /**
     * Test method {@link ProviderNetworkManagerImpl#providerAdded(ServiceReference, NetworkingProvider)}
     */
    @Test
    public void testProviderAdded(){
        ServiceReference ref = mock(ServiceReference.class);

        // configure ref
        when(ref.getProperty(org.osgi.framework.Constants.SERVICE_ID)).thenReturn(r.nextLong());
        when(ref.getProperty(Constants.SOUTHBOUND_PROTOCOL_PROPERTY)).thenReturn("proto");
        when(ref.getProperty(Constants.OPENFLOW_VERSION_PROPERTY)).thenReturn("of");
        when(ref.getProperty(Constants.PROVIDER_TYPE_PROPERTY)).thenReturn("provider");

        // nothing much to test
        providerNetworkManagerImpl.providerAdded(ref, mock(NetworkingProvider.class));
    }

    /**
     * Test method {@link ProviderNetworkManagerImpl#providerRemoved(ServiceReference)}
     */
    @Test
    public void testProviderRemoved(){
        ServiceReference ref = mock(ServiceReference.class);

        // configure ref
        when(ref.getProperty(org.osgi.framework.Constants.SERVICE_ID)).thenReturn(r.nextLong());

        // nothing much to test
        providerNetworkManagerImpl.providerRemoved(ref);
    }
}
