/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.openstack.netvirt.SouthboundEvent.Type;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryListener.OvsdbType;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test fort {@link SouthboundHandler}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class SouthboundHandlerTest {

    @InjectMocks private SouthboundHandler southboundHandler;
    private SouthboundHandler southboundHandlerSpy;

    @Mock private ConfigurationService configurationService;
    @Mock private BridgeConfigurationManager bridgeConfigurationManager;
    @Mock private TenantNetworkManager tenantNetworkManager;
    @Mock private NetworkingProviderManager networkingProviderManager;
    @Mock private NeutronL3Adapter neutronL3Adapter;
    @Mock private NodeCacheManager nodeCacheManager;
    @Mock private OvsdbInventoryService ovsdbInventoryService;
    @Mock private Southbound southbound;

    @Before
    public void setUp() {
        southboundHandler.eventDispatcher = mock(EventDispatcher.class);
        southboundHandlerSpy = Mockito.spy(southboundHandler);
    }

    @Test
    public void testTriggerUpdates() {
        Node node = mock(Node.class);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(mock(OvsdbNodeAugmentation.class));

        List<Node> nodes = new ArrayList<Node>();
        nodes.add(mock(Node.class));
        when(southbound.readOvsdbTopologyNodes()).thenReturn(nodes);

        southboundHandlerSpy.triggerUpdates();
        verify(southboundHandlerSpy, times(1)).ovsdbUpdate(any(Node.class), any(DataObject.class), any(OvsdbType.class), any(Action.class));;
    }

    @Test
    public void testNotifyNode() {
        when(southbound.getBridge(any(Node.class))).thenReturn(mock(OvsdbBridgeAugmentation.class));

        NetworkingProvider networkingProvider = mock(NetworkingProvider.class);
        when(networkingProviderManager.getProvider(any(Node.class))).thenReturn(networkingProvider);

        southboundHandlerSpy.notifyNode(mock(Node.class), Action.ADD);

        verify(networkingProvider, times(1)).initializeOFFlowRules(any(Node.class));
    }

    @Test
    public void testProcessEvent() {
        SouthboundEvent ev = mock(SouthboundEvent.class);
        Node node = mock(Node.class);

        // NODE
        when(ev.getNode()).thenReturn(node);
        when(ev.getAugmentationData()).thenReturn(mock(OvsdbNodeAugmentation.class));
        when(ev.getType()).thenReturn(Type.NODE);

        when(ev.getAction()).thenReturn(Action.ADD);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeAdded(any(Node.class));
        verify(bridgeConfigurationManager, times(1)).prepareNode(any(Node.class));
        Mockito.reset(nodeCacheManager);

        when(ev.getAction()).thenReturn(Action.UPDATE);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeAdded(any(Node.class));
        Mockito.reset(nodeCacheManager);

        when(ev.getAction()).thenReturn(Action.DELETE);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeRemoved(any(Node.class));
        Mockito.reset(nodeCacheManager);

        Mockito.reset(ev);

        // BRIDGE
        when(ev.getNode()).thenReturn(node);
        when(ev.getAugmentationData()).thenReturn(mock(OvsdbBridgeAugmentation.class));
        when(ev.getType()).thenReturn(Type.BRIDGE);

        when(southbound.getDatapathId(any(OvsdbBridgeAugmentation.class))).thenReturn("45");

        when(ev.getAction()).thenReturn(Action.ADD);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeAdded(any(Node.class));
        Mockito.reset(nodeCacheManager);

        when(ev.getAction()).thenReturn(Action.UPDATE);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeAdded(any(Node.class));
        Mockito.reset(nodeCacheManager);

        Mockito.reset(nodeCacheManager);
        when(ev.getAction()).thenReturn(Action.DELETE);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeRemoved(any(Node.class));
        verify(southbound, times(1)).deleteBridge(any(Node.class));
        Mockito.reset(nodeCacheManager);

        Mockito.reset(ev);

        // PORT
        when(ev.getNode()).thenReturn(node);
        when(ev.getAugmentationData()).thenReturn(mock(OvsdbTerminationPointAugmentation.class));
        when(ev.getType()).thenReturn(Type.PORT);

        // TODO HERE
    }

}
