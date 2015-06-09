/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.neutron.spi.INeutronLoadBalancerCRUD;
import org.opendaylight.neutron.spi.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronLoadBalancer;
import org.opendaylight.neutron.spi.NeutronLoadBalancerPool;
import org.opendaylight.neutron.spi.NeutronLoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration.LoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link LBaaSPoolMemberHandler}
 */
///* TODO SB_MIGRATION */ @Ignore
@PrepareForTest(NeutronCacheUtils.class)
@RunWith(PowerMockRunner.class)
public class LBaaSPoolHandlerTest {

    @InjectMocks LBaaSPoolHandler lBaaSPoolHandler;

    @Mock private INeutronLoadBalancerPoolCRUD neutronLBPoolCache;
    @Mock private INeutronLoadBalancerCRUD neutronLBCache;
    @Mock private LoadBalancerProvider loadBalancerProvider;
    @Mock private NodeCacheManager nodeCacheManager;

    @Mock private NeutronLoadBalancerPool neutronLBPool;

    @Before
    public void setUp() {
        when(neutronLBPool.getLoadBalancerPoolProtocol()).thenReturn(LoadBalancerConfiguration.PROTOCOL_HTTP);

        List<NeutronLoadBalancerPoolMember> members = new ArrayList();
        NeutronLoadBalancerPoolMember neutronLBPoolMember = mock(NeutronLoadBalancerPoolMember.class);
        when(neutronLBPoolMember.getPoolMemberAdminStateIsUp()).thenReturn(true);
        when(neutronLBPoolMember.getPoolMemberSubnetID()).thenReturn("subnetID");
        when(neutronLBPoolMember.getPoolMemberID()).thenReturn("pool_memberID");
        when(neutronLBPoolMember.getPoolMemberAddress()).thenReturn("pool_member_address");
        when(neutronLBPoolMember.getPoolMemberProtoPort()).thenReturn(1);
        members.add(neutronLBPoolMember);
        when(neutronLBPool.getLoadBalancerPoolMembers()).thenReturn(members);

        List<NeutronLoadBalancer> list_neutronLB = new ArrayList();
        NeutronLoadBalancer neutronLB = mock(NeutronLoadBalancer.class);
        when(neutronLB.getLoadBalancerName()).thenReturn("load_balancer_name");
        when(neutronLB.getLoadBalancerVipAddress()).thenReturn("vip_address");
        when(neutronLB.getLoadBalancerVipSubnetID()).thenReturn("subnetID");
        list_neutronLB.add(neutronLB);
        when(neutronLBCache.getAllNeutronLoadBalancers()).thenReturn(list_neutronLB);

        Map.Entry<String,String> providerInfo = mock(Entry.class);
        when(providerInfo.getKey()).thenReturn("key");
        when(providerInfo.getValue()).thenReturn("value");

        PowerMockito.mockStatic(NeutronCacheUtils.class);
        when(NeutronCacheUtils.getMacAddress(any(INeutronPortCRUD.class), anyString(), anyString())).thenReturn("mac_address");
        when(NeutronCacheUtils.getProviderInformation(any(INeutronNetworkCRUD.class), any(INeutronSubnetCRUD.class), anyString())).thenReturn(providerInfo);
    }

    /**
     * Test method {@link LBaaSPoolHandler#canCreateNeutronLoadBalancerPool(NeutronLoadBalancerPool)}
     */
    @Test
    public void testCanCreateNeutronLoadBalancerPoolMember() {
        when(neutronLBPool.getLoadBalancerPoolProtocol())
                                    .thenReturn(LoadBalancerConfiguration.PROTOCOL_HTTP) // to test HTTP_OK
                                    .thenReturn(null) // to test HTTP_BAD_REQUEST
                                    .thenReturn("dummy_proto"); // to test HTTP_NOT_ACCEPTABLE

        assertEquals("Error, canCreateNeutronLoadBalancerPool() didn't return the correct HTTP flag", HttpURLConnection.HTTP_OK, lBaaSPoolHandler.canCreateNeutronLoadBalancerPool(neutronLBPool));
        assertEquals("Error, canCreateNeutronLoadBalancerPool() didn't return the correct HTTP flag", HttpURLConnection.HTTP_BAD_REQUEST, lBaaSPoolHandler.canCreateNeutronLoadBalancerPool(neutronLBPool));
        assertEquals("Error, canCreateNeutronLoadBalancerPool() didn't return the correct HTTP flag", HttpURLConnection.HTTP_NOT_ACCEPTABLE, lBaaSPoolHandler.canCreateNeutronLoadBalancerPool(neutronLBPool));
    }

    /**
     * Test method {@link LBaaSPoolHandler#canUpdateNeutronLoadBalancerPool(NeutronLoadBalancerPool, NeutronLoadBalancerPool)}
     */
    public void testCanUpdateNeutronLoadBalancerPool() {
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_NOT_IMPLEMENTED, lBaaSPoolHandler.canUpdateNeutronLoadBalancerPool(any(NeutronLoadBalancerPool.class), any(NeutronLoadBalancerPool.class)));
    }

    /**
     * Test method {@link LBaaSPoolHandler#canDeleteNeutronLoadBalancerPool(NeutronLoadBalancerPool)}
     */
    @Test
    public void testCanDeleteNeutronLoadBalancerPool() {
        when(neutronLBPool.getLoadBalancerPoolProtocol())
                                        .thenReturn(LoadBalancerConfiguration.PROTOCOL_HTTP) // to test HTTP_OK
                                        .thenReturn(null) // to test HTTP_BAD_REQUEST
                                        .thenReturn("dummy_proto"); // to test HTTP_NOT_ACCEPTABLE

        assertEquals("Error, canDeleteNeutronLoadBalancerPool() didn't return the correct HTTP flag", HttpURLConnection.HTTP_OK, lBaaSPoolHandler.canDeleteNeutronLoadBalancerPool(neutronLBPool));
        assertEquals("Error, canDeleteNeutronLoadBalancerPool() didn't return the correct HTTP flag", HttpURLConnection.HTTP_BAD_REQUEST, lBaaSPoolHandler.canDeleteNeutronLoadBalancerPool(neutronLBPool));
        assertEquals("Error, canDeleteNeutronLoadBalancerPool() didn't return the correct HTTP flag", HttpURLConnection.HTTP_NOT_ACCEPTABLE, lBaaSPoolHandler.canDeleteNeutronLoadBalancerPool(neutronLBPool));
    }

    /**
     * Test method {@link LBaaSPoolHandler#processEvent(AbstractEvent)}
     */
    @Test
    public void testProcessEvent() {
        LBaaSPoolHandler lbaasPoolHandlerSpy = Mockito.spy(lBaaSPoolHandler);

        NorthboundEvent ev = mock(NorthboundEvent.class);
        when(ev.getLoadBalancerPool()).thenReturn(neutronLBPool);

        List<Node> list_node = new ArrayList();
        list_node .add(mock(Node.class));
        when(nodeCacheManager.getBridgeNodes()).thenReturn(list_node);

        when(ev.getAction()).thenReturn(Action.ADD);
        lbaasPoolHandlerSpy.processEvent(ev);
        verify(lbaasPoolHandlerSpy, times(1)).extractLBConfiguration(any(NeutronLoadBalancerPool.class));

        when(ev.getAction()).thenReturn(Action.DELETE);
        lbaasPoolHandlerSpy.processEvent(ev);
        verify(lbaasPoolHandlerSpy, times(2)).extractLBConfiguration(any(NeutronLoadBalancerPool.class)); // 1 + 1 above

        when(ev.getAction()).thenReturn(Action.UPDATE);
        lbaasPoolHandlerSpy.processEvent(ev);
        verify(lbaasPoolHandlerSpy, times(2)).extractLBConfiguration(any(NeutronLoadBalancerPool.class)); // same as before as nothing as been done
    }

    /**
     * Test method {@link LBaaSPoolHandler#extractLBConfiguration(NeutronLoadBalancerPool)}
     */
    @Test
    public void testExtractLBConfiguration() {
        List<LoadBalancerConfiguration> list_lbConfig = lBaaSPoolHandler.extractLBConfiguration(neutronLBPool);
        LoadBalancerConfiguration lbConfig = list_lbConfig.get(0);

        verify(neutronLBCache, times(1)).getAllNeutronLoadBalancers();

        // make sure the load balancer configuration was correctly populated
        assertEquals("Error, did not return the correct value",  "key", lbConfig.getProviderNetworkType());
        assertEquals("Error, did not return the correct value",  "value", lbConfig.getProviderSegmentationId());
        assertEquals("Error, did not return the correct value",  "mac_address", lbConfig.getVmac());

        // make sure the load balancer pool member was correctly populated
        LoadBalancerPoolMember member = lbConfig.getMembers().get("pool_memberID");
        assertEquals("Error, did not return the correct value",  "pool_member_address", member.getIP());
        assertEquals("Error, did not return the correct value",  "mac_address", member.getMAC());
        assertEquals("Error, did not return the correct value",  LoadBalancerConfiguration.PROTOCOL_HTTP, member.getProtocol());
        assertTrue("Error, did not return the correct value",  1 ==  member.getPort());
    }
}
