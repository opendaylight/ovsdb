/*
 * Copyright (c) 2015, 2016 Inocybe and others.  All rights reserved.
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

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration.LoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancer;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerPool;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSubnet;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronLoadBalancerCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronNetworkCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronSubnetCRUD;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import org.osgi.framework.ServiceReference;

/**
 * Unit test for {@link LBaaSPoolMemberHandler}
 */
@RunWith(MockitoJUnitRunner.class)
public class LBaaSPoolMemberHandlerTest {

    @InjectMocks LBaaSPoolMemberHandler lBaaSPoolMemberHandler;

    @Mock private INeutronLoadBalancerPoolCRUD neutronLBPoolCache;
    @Mock private INeutronLoadBalancerCRUD neutronLBCache;
    @Mock private LoadBalancerProvider loadBalancerProvider;
    @Mock private NodeCacheManager nodeCacheManager;
    @Mock private INeutronPortCRUD neutronPortCache;
    @Mock private INeutronNetworkCRUD neutronNetworkCache;
    @Mock private INeutronSubnetCRUD neutronSubnetCache;

    private NeutronLoadBalancerPoolMember neutronLBMember;

    @Before
    public void setUp() {
        neutronLBMember = new NeutronLoadBalancerPoolMember();
        neutronLBMember.setID("pool_memberID");
        neutronLBMember.setPoolMemberAddress("pool_member_address");
        neutronLBMember.setPoolMemberSubnetID("pool_member_subnetID");
        neutronLBMember.setPoolMemberProtoPort(1);
        neutronLBMember.setPoolID("poolID");

        lBaaSPoolMemberHandler.setDependencies(neutronPortCache);
        final NeutronPort neutronPort = new NeutronPort();
        final Neutron_IPs neutronIP1 = new Neutron_IPs();
        neutronIP1.setSubnetUUID("pool_member_subnetID");
        neutronIP1.setIpAddress("pool_member_address");
        final Neutron_IPs neutronIP2 = new Neutron_IPs();
        neutronIP2.setSubnetUUID("pool_member_subnetID");
        neutronIP2.setIpAddress("vip_address");
        final List<Neutron_IPs> neutronIPs = new ArrayList<>();
        neutronIPs.add(neutronIP1);
        neutronIPs.add(neutronIP2);
        neutronPort.setFixedIPs(neutronIPs);
        neutronPort.setMacAddress("mac_address");
        when(neutronPortCache.getAllPorts()).thenReturn(Collections.singletonList(neutronPort));

        lBaaSPoolMemberHandler.setDependencies(neutronSubnetCache);
        final NeutronSubnet neutronSubnet = new NeutronSubnet();
        neutronSubnet.setID("pool_member_subnetID");
        neutronSubnet.setNetworkUUID("pool_member_networkUUID");
        when(neutronSubnetCache.getAllSubnets()).thenReturn(Collections.singletonList(neutronSubnet));

        lBaaSPoolMemberHandler.setDependencies(neutronNetworkCache);
        final NeutronNetwork neutronNetwork = new NeutronNetwork();
        neutronNetwork.setNetworkUUID("pool_member_networkUUID");
        neutronNetwork.setProviderNetworkType("key");
        neutronNetwork.setProviderSegmentationID("value");
        when(neutronNetworkCache.getAllNetworks()).thenReturn(Collections.singletonList(neutronNetwork));

        List<NeutronLoadBalancerPoolMember> members = new ArrayList<>();
        NeutronLoadBalancerPoolMember neutronLBPoolMember = new NeutronLoadBalancerPoolMember();
        neutronLBPoolMember.setPoolMemberAdminStateIsUp(true);
        neutronLBPoolMember.setPoolMemberSubnetID("pool_member_subnetID");
        neutronLBPoolMember.setID("pool_memberID1");
        neutronLBPoolMember.setPoolMemberProtoPort(1);
        members.add(neutronLBPoolMember);

        NeutronLoadBalancerPool neutronLBPool = new NeutronLoadBalancerPool();
        neutronLBPool.setLoadBalancerPoolProtocol(LoadBalancerConfiguration.PROTOCOL_TCP);
        neutronLBPool.setLoadBalancerPoolMembers(members);
        when(neutronLBPoolCache.getNeutronLoadBalancerPool(anyString())).thenReturn(neutronLBPool);

        NeutronLoadBalancer neutronLB = new NeutronLoadBalancer();
        neutronLB.setLoadBalancerName("load_balancer_name");
        neutronLB.setLoadBalancerVipAddress("vip_address");
        neutronLB.setLoadBalancerVipSubnetID("pool_member_subnetID");
        when(neutronLBCache.getAllNeutronLoadBalancers()).thenReturn(Collections.singletonList(neutronLB));
    }

    /**
     * Test method {@link LBaaSPoolMemberHandler#canCreateNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember)}
     */
    @Test
    public void testCanCreateNeutronLoadBalancerPoolMember() {
        // HTTP_BAD_REQUEST
        neutronLBMember.setPoolID(null);
        assertEquals("Error, canCreateNeutronLoadBalancerPoolMember() didn't return the correct HTTP flag", HttpURLConnection.HTTP_BAD_REQUEST, lBaaSPoolMemberHandler.canCreateNeutronLoadBalancerPoolMember(neutronLBMember));

        // HTTP_OK
        neutronLBMember.setPoolID("poolID");
        assertEquals("Error, canCreateNeutronLoadBalancerPoolMember() didn't return the correct HTTP flag", HttpURLConnection.HTTP_OK, lBaaSPoolMemberHandler.canCreateNeutronLoadBalancerPoolMember(neutronLBMember));

        // HTTP_NOT_ACCEPTABLE
        when(neutronNetworkCache.getAllNetworks()).thenReturn(Collections.<NeutronNetwork>emptyList());
        assertEquals("Error, canCreateNeutronLoadBalancerPoolMember() didn't return the correct HTTP flag", HttpURLConnection.HTTP_NOT_ACCEPTABLE, lBaaSPoolMemberHandler.canCreateNeutronLoadBalancerPoolMember(neutronLBMember));
    }

    /**
     * Test method {@link LBaaSPoolMemberHandler#canUpdateNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember, NeutronLoadBalancerPoolMember)}
     */
    @Test
    public void testCanUpdateNeutronLoadBalancerPoolMember() {
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_NOT_IMPLEMENTED, lBaaSPoolMemberHandler.canUpdateNeutronLoadBalancerPoolMember(null, null));
    }

    /**
     * Test method {@link LBaaSPoolMemberHandler#canDeleteNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember)}
     */
    @Test
    public void testCanDeleteNeutronLoadBalancerPoolMember() {
        // HTTP_BAD_REQUEST
        neutronLBMember.setPoolID(null);
        assertEquals("Error, canDeleteNeutronLoadBalancerPoolMember() didn't return the correct HTTP flag", HttpURLConnection.HTTP_BAD_REQUEST, lBaaSPoolMemberHandler.canDeleteNeutronLoadBalancerPoolMember(neutronLBMember));

        // HTTP_OK
        neutronLBMember.setPoolID("poolID");
        assertEquals("Error, canDeleteNeutronLoadBalancerPoolMember() didn't return the correct HTTP flag", HttpURLConnection.HTTP_OK, lBaaSPoolMemberHandler.canDeleteNeutronLoadBalancerPoolMember(neutronLBMember));

        // HTTP_NOT_ACCEPTABLE
        when(neutronNetworkCache.getAllNetworks()).thenReturn(Collections.<NeutronNetwork>emptyList());
        assertEquals("Error, canDeleteNeutronLoadBalancerPoolMember() didn't return the correct HTTP flag", HttpURLConnection.HTTP_NOT_ACCEPTABLE, lBaaSPoolMemberHandler.canDeleteNeutronLoadBalancerPoolMember(neutronLBMember));
    }

    /**
     * Test method {@link LBaaSPoolMemberHandler#processEvent(AbstractEvent)}
     */
    @Test
    public void testProcessEvent(){
        LBaaSPoolMemberHandler lbaasPoolMemberHandlerSpy = Mockito.spy(lBaaSPoolMemberHandler);

        NorthboundEvent ev = mock(NorthboundEvent.class);
        when(ev.getLoadBalancerPoolMember()).thenReturn(neutronLBMember);

        List<Node> list_node = new ArrayList<>();
        list_node .add(mock(Node.class));
        when(nodeCacheManager.getBridgeNodes()).thenReturn(list_node);

        when(ev.getAction()).thenReturn(Action.ADD);
        lbaasPoolMemberHandlerSpy.processEvent(ev);
        verify(lbaasPoolMemberHandlerSpy, times(1)).extractLBConfiguration(any(NeutronLoadBalancerPoolMember.class));

        when(ev.getAction()).thenReturn(Action.DELETE);
        lbaasPoolMemberHandlerSpy.processEvent(ev);
        verify(lbaasPoolMemberHandlerSpy, times(2)).extractLBConfiguration(any(NeutronLoadBalancerPoolMember.class)); // 1 + 1 above

        when(ev.getAction()).thenReturn(Action.UPDATE);
        lbaasPoolMemberHandlerSpy.processEvent(ev);
        verify(lbaasPoolMemberHandlerSpy, times(2)).extractLBConfiguration(any(NeutronLoadBalancerPoolMember.class)); // same as before as nothing as been done
    }

    /**
     * Test method {@link LBaaSPoolMemberHandler#extractLBConfiguration(NeutronLoadBalancerPoolMember)}
     */
    @Test
    public void testExtractLBConfiguration() {
        LoadBalancerConfiguration lbConfig = lBaaSPoolMemberHandler.extractLBConfiguration(neutronLBMember);

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

    @Test
    public void testSetDependencies() throws Exception {
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        LoadBalancerProvider loadBalancerProvider = mock(LoadBalancerProvider.class);
        NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);

        ServiceHelper.overrideGlobalInstance(EventDispatcher.class, eventDispatcher);
        ServiceHelper.overrideGlobalInstance(LoadBalancerProvider.class, loadBalancerProvider);
        ServiceHelper.overrideGlobalInstance(NodeCacheManager.class, nodeCacheManager);

        lBaaSPoolMemberHandler.setDependencies(mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", lBaaSPoolMemberHandler.eventDispatcher, eventDispatcher);
        assertEquals("Error, did not return the correct object", getField("loadBalancerProvider"), loadBalancerProvider);
        assertEquals("Error, did not return the correct object", getField("nodeCacheManager"), nodeCacheManager);
    }

    @Test
    public void testSetDependenciesObject() throws Exception{
        INeutronNetworkCRUD iNeutronNetworkCRUD = mock(INeutronNetworkCRUD.class);
        lBaaSPoolMemberHandler.setDependencies(iNeutronNetworkCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronNetworkCache"), iNeutronNetworkCRUD);

        INeutronPortCRUD iNeutronPortCRUD = mock(INeutronPortCRUD.class);
        lBaaSPoolMemberHandler.setDependencies(iNeutronPortCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronPortCache"), iNeutronPortCRUD);

        INeutronSubnetCRUD iNeutronSubnetCRUD = mock(INeutronSubnetCRUD.class);
        lBaaSPoolMemberHandler.setDependencies(iNeutronSubnetCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronSubnetCache"), iNeutronSubnetCRUD);

        INeutronLoadBalancerCRUD iNeutronLoadBalancerCRUD = mock(INeutronLoadBalancerCRUD.class);
        lBaaSPoolMemberHandler.setDependencies(iNeutronLoadBalancerCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronLBCache"), iNeutronLoadBalancerCRUD);

        INeutronLoadBalancerPoolCRUD iNeutronLoadBalancerPoolCRUD = mock(INeutronLoadBalancerPoolCRUD.class);
        lBaaSPoolMemberHandler.setDependencies(iNeutronLoadBalancerPoolCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronLBPoolCache"), iNeutronLoadBalancerPoolCRUD);

        LoadBalancerProvider loadBalancerProvider = mock(LoadBalancerProvider.class);
        lBaaSPoolMemberHandler.setDependencies(loadBalancerProvider);
        assertEquals("Error, did not return the correct object", getField("loadBalancerProvider"), loadBalancerProvider);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = LBaaSPoolMemberHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(lBaaSPoolMemberHandler);
    }
}
