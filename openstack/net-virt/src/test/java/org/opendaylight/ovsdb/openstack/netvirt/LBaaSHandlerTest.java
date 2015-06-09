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
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
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
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration.LoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link LBaaSHandler}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NeutronCacheUtils.class, ServiceHelper.class})
public class LBaaSHandlerTest {

    @InjectMocks private LBaaSHandler lbaasHandler;
    private LBaaSHandler lbaasHandlerSpy;

    @Mock private INeutronLoadBalancerCRUD neutronLBCache;
    @Mock private INeutronLoadBalancerPoolCRUD neutronLBPoolCache;
    @Mock private LoadBalancerProvider loadBalancerProvider;
    @Mock private NodeCacheManager nodeCacheManager;
    @Mock private NeutronLoadBalancer neutronLB;

    @Before
    public void setUp(){
        NorthboundEvent ev = mock(NorthboundEvent.class);
        Map.Entry<String,String> providerInfo = mock(Entry.class);
        NeutronLoadBalancerPoolMember neutronLBPoolMember = mock(NeutronLoadBalancerPoolMember.class);
        NeutronLoadBalancerPool neutronLBPool = mock(NeutronLoadBalancerPool.class);
        List<NeutronLoadBalancerPoolMember> members = new ArrayList();
        List<NeutronLoadBalancerPool> list_neutronLBPool = new ArrayList();
        List<NeutronLoadBalancer> list_neutronLB = new ArrayList();
        List<Node> list_node = new ArrayList();

        lbaasHandlerSpy = Mockito.spy(lbaasHandler);

        neutronLB = mock(NeutronLoadBalancer.class);
        when(neutronLB.getLoadBalancerName()).thenReturn("load_balancer_name");
        when(neutronLB.getLoadBalancerVipAddress()).thenReturn("vip_address");
        when(neutronLB.getLoadBalancerVipSubnetID()).thenReturn("subnetID");

        when(ev.getLoadBalancer()).thenReturn(neutronLB);

        when(providerInfo.getKey()).thenReturn("key");
        when(providerInfo.getValue()).thenReturn("value");

        PowerMockito.mockStatic(NeutronCacheUtils.class);
        when(NeutronCacheUtils.getProviderInformation(any(INeutronNetworkCRUD.class), any(INeutronSubnetCRUD.class), anyString())).thenReturn(providerInfo);
        when(NeutronCacheUtils.getMacAddress(any(INeutronPortCRUD.class), anyString(), anyString())).thenReturn("mac_address");

        when(neutronLBPoolMember.getPoolMemberAdminStateIsUp()).thenReturn(true);
        when(neutronLBPoolMember.getPoolMemberSubnetID()).thenReturn("subnetID");
        when(neutronLBPoolMember.getPoolMemberID()).thenReturn("pool_memberID");
        when(neutronLBPoolMember.getPoolMemberAddress()).thenReturn("pool_member_address");
        when(neutronLBPoolMember.getPoolMemberProtoPort()).thenReturn(1);
        members.add(neutronLBPoolMember);

        when(neutronLBPool.getLoadBalancerPoolMembers()).thenReturn(members);
        when(neutronLBPool.getLoadBalancerPoolProtocol()).thenReturn(LoadBalancerConfiguration.PROTOCOL_TCP);
        list_neutronLBPool.add(neutronLBPool);
        when(neutronLBPoolCache.getAllNeutronLoadBalancerPools()).thenReturn(list_neutronLBPool);

        list_neutronLB.add(neutronLB);
        when(neutronLBCache.getAllNeutronLoadBalancers()).thenReturn(list_neutronLB );

        list_node.add(mock(Node.class));
        when(nodeCacheManager.getBridgeNodes()).thenReturn(list_node);
    }

    @Test
    public void testCanCreateNeutronLoadBalancer(){
        assertEquals("Error, canCreateNeutronLoadBalancer() did not return the correct value ", HttpURLConnection.HTTP_OK, lbaasHandler.canCreateNeutronLoadBalancer(any(NeutronLoadBalancer.class)));
    }

    @Test
    public void testCanUpdateNeutronLoadBalancer(){
        assertEquals("Error, canUpdateNeutronLoadBalancer() did not return the correct value ", HttpURLConnection.HTTP_OK, lbaasHandler.canUpdateNeutronLoadBalancer(any(NeutronLoadBalancer.class), any(NeutronLoadBalancer.class)));
    }

    @Test
    public void testCanDeleteNeutronLoadBalancer(){
        assertEquals("Error, canDeleteNeutronLoadBalancer() did not return the correct value ", HttpURLConnection.HTTP_OK, lbaasHandler.canDeleteNeutronLoadBalancer(any(NeutronLoadBalancer.class)));
    }

    /**
     * Test method {@link LBaaSHandler#processEvent(AbstractEvent)}
     */
    @Test
    public void testProcessEvent(){
        NorthboundEvent ev = mock(NorthboundEvent.class);
        when(ev.getLoadBalancer()).thenReturn(neutronLB);

        when(ev.getAction()).thenReturn(Action.ADD);
        lbaasHandlerSpy.processEvent(ev);
        verify(lbaasHandlerSpy, times(1)).extractLBConfiguration(any(NeutronLoadBalancer.class));
        verify(loadBalancerProvider, times(1)).programLoadBalancerRules(any(Node.class), any(LoadBalancerConfiguration.class), same(Action.ADD));

        when(ev.getAction()).thenReturn(Action.DELETE);
        lbaasHandlerSpy.processEvent(ev);
        verify(lbaasHandlerSpy, times(2)).extractLBConfiguration(any(NeutronLoadBalancer.class)); // 1 + 1 above
        verify(loadBalancerProvider, times(1)).programLoadBalancerRules(any(Node.class), any(LoadBalancerConfiguration.class), same(Action.DELETE));

        when(ev.getAction()).thenReturn(Action.UPDATE);
        lbaasHandlerSpy.processEvent(ev);
        verify(lbaasHandlerSpy, times(4)).extractLBConfiguration(any(NeutronLoadBalancer.class)); // 2 + 2 above
        verify(loadBalancerProvider, times(2)).programLoadBalancerRules(any(Node.class), any(LoadBalancerConfiguration.class), same(Action.DELETE)); // 1 + 1 above
        verify(loadBalancerProvider, times(2)).programLoadBalancerRules(any(Node.class), any(LoadBalancerConfiguration.class), same(Action.ADD)); // 1 + 1 above
    }

    /**
     * Test method {@link LBaaSHandler#extractLBConfiguration(NeutronLoadBalancer)}
     */
    @Test
    public void testExtractLBConfiguration(){
        LoadBalancerConfiguration lbConfig = lbaasHandler.extractLBConfiguration(neutronLB);

        verify(neutronLBPoolCache, times(1)).getAllNeutronLoadBalancerPools();

        // make sure the load balancer configuration was correctly populated
        assertEquals("Error, did not return the correct value",  "key", lbConfig.getProviderNetworkType());
        assertEquals("Error, did not return the correct value",  "value", lbConfig.getProviderSegmentationId());
        assertEquals("Error, did not return the correct value",  "mac_address", lbConfig.getVmac());

        // make sure the load balancer pool member was correctly populated
        LoadBalancerPoolMember member = lbConfig.getMembers().get("pool_memberID");
        assertEquals("Error, did not return the correct value",  "pool_member_address", member.getIP());
        assertEquals("Error, did not return the correct value",  "mac_address", member.getMAC());
        assertEquals("Error, did not return the correct value",  LoadBalancerConfiguration.PROTOCOL_TCP, member.getProtocol());
        assertTrue("Error, did not return the correct value",  1 ==  member.getPort());
    }

    /**
     * Test method {@link LBaaSHandler#notifyNode(Node, Action)}
     */
    @Test
    public void testNotifyNode() {
        lbaasHandlerSpy.notifyNode(mock(Node.class), Action.ADD);

        verify(lbaasHandlerSpy, times(1)).extractLBConfiguration(any(NeutronLoadBalancer.class));
        verify(neutronLBCache, times(1)).getAllNeutronLoadBalancers();
        verify(neutronLBPoolCache, times(1)).getAllNeutronLoadBalancerPools();
        verify(loadBalancerProvider, times(1)).programLoadBalancerRules(any(Node.class), any(LoadBalancerConfiguration.class), any(Action.class));
    }

    @Test
    public void testSetDependencies() throws Exception {
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        LoadBalancerProvider loadBalancerProvider = mock(LoadBalancerProvider.class);
        NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(EventDispatcher.class, lbaasHandler)).thenReturn(eventDispatcher);
        PowerMockito.when(ServiceHelper.getGlobalInstance(LoadBalancerProvider.class, lbaasHandler)).thenReturn(loadBalancerProvider);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NodeCacheManager.class, lbaasHandler)).thenReturn(nodeCacheManager);

        lbaasHandler.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", lbaasHandler.eventDispatcher, eventDispatcher);
        assertEquals("Error, did not return the correct object", getField("loadBalancerProvider"), loadBalancerProvider);
        assertEquals("Error, did not return the correct object", getField("nodeCacheManager"), nodeCacheManager);
    }

    @Test
    public void testSetDependenciesObject() throws Exception{
        INeutronNetworkCRUD iNeutronNetworkCRUD = mock(INeutronNetworkCRUD.class);
        lbaasHandler.setDependencies(iNeutronNetworkCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronNetworkCache"), iNeutronNetworkCRUD);

        INeutronPortCRUD iNeutronPortCRUD = mock(INeutronPortCRUD.class);
        lbaasHandler.setDependencies(iNeutronPortCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronPortCache"), iNeutronPortCRUD);

        INeutronSubnetCRUD iNeutronSubnetCRUD = mock(INeutronSubnetCRUD.class);
        lbaasHandler.setDependencies(iNeutronSubnetCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronSubnetCache"), iNeutronSubnetCRUD);

        INeutronLoadBalancerCRUD iNeutronLoadBalancerCRUD = mock(INeutronLoadBalancerCRUD.class);
        lbaasHandler.setDependencies(iNeutronLoadBalancerCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronLBCache"), iNeutronLoadBalancerCRUD);

        INeutronLoadBalancerPoolCRUD iNeutronLoadBalancerPoolCRUD = mock(INeutronLoadBalancerPoolCRUD.class);
        lbaasHandler.setDependencies(iNeutronLoadBalancerPoolCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronLBPoolCache"), iNeutronLoadBalancerPoolCRUD);

        LoadBalancerProvider loadBalancerProvider = mock(LoadBalancerProvider.class);
        lbaasHandler.setDependencies(loadBalancerProvider);
        assertEquals("Error, did not return the correct object", getField("loadBalancerProvider"), loadBalancerProvider);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = LBaaSHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(lbaasHandler);
    }
}
