/*
 * Copyright (c) 2015 Hewlett-Packard Enterprise and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSecurityGroupCRUD;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test fort {@link SecurityGroupCacheManagerImpl}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class SecurityGroupCacheManagerImplTest {

    @InjectMocks private SecurityGroupCacheManagerImpl securityGroupCacheManagerImpl;
    @Mock private INeutronPortCRUD neutronPortCache;
    @Mock private INeutronSecurityGroupCRUD securityGroupCache;
    @Mock NeutronPort neutronPort_Vm1;
    @Mock NeutronPort neutronPort_Vm2;
    @Mock NeutronPort neutronPort_Vm3;
    @Mock NeutronPort neutronPort_Vm4;
    @Mock NeutronPort neutronPort_Vm5;
    @Mock SecurityServicesManager securityServicesManager;
    @Mock NeutronSecurityGroup neutronSecurityGroup_1;
    @Mock NeutronSecurityGroup neutronSecurityGroup_2;
    @Mock NeutronSecurityGroup neutronSecurityGroup_3;
    @Mock NeutronSecurityRule neutronSecurityRule_1;
    @Mock NeutronSecurityRule neutronSecurityRule_2;
    @Mock NeutronSecurityRule neutronSecurityRule_3;
    @Mock Neutron_IPs neutron_ip_1;
    @Mock Neutron_IPs neutron_ip_2;
    @Mock Neutron_IPs neutron_ip_3;
    @Mock Neutron_IPs neutron_ip_4;
    @Mock Neutron_IPs neutron_ip_5;

    private static final String NEUTRON_PORT_ID_VM_1 = "neutronID_VM_1";
    private static final String NEUTRON_PORT_ID_VM_2 = "neutronID_VM_2";
    private static final String NEUTRON_PORT_ID_VM_3 = "neutronID_VM_3";
    private static final String NEUTRON_PORT_ID_VM_4 = "neutronID_VM_4";
    private static final String NEUTRON_PORT_ID_VM_5 = "neutronID_VM_5";
    private static final String SECURITY_GROUP_ID_1 = "securityGroupId_1";
    private static final String SECURITY_GROUP_ID_2 = "securityGroupId_2";
    private static final String SECURITY_GROUP_ID_3 = "securityGroupId_3";
    private static final List<Neutron_IPs> neutron_IPs_1 = new ArrayList<>();
    private static final List<Neutron_IPs> neutron_IPs_2 = new ArrayList<>();
    private static final List<Neutron_IPs> neutron_IPs_3 = new ArrayList<>();
    private static final List<Neutron_IPs> neutron_IPs_4 = new ArrayList<>();
    private static final List<Neutron_IPs> neutron_IPs_5 = new ArrayList<>();

    @Before
    public void setUp() throws Exception {

        List<NeutronSecurityGroup> securityGroups_Vm_1 = new ArrayList<>();
        securityGroups_Vm_1.add(neutronSecurityGroup_1);
        List<NeutronSecurityGroup> securityGroups_Vm_2 = new ArrayList<>();
        securityGroups_Vm_2.add(neutronSecurityGroup_2);
        List<NeutronSecurityGroup> securityGroups_Vm_3 = new ArrayList<>();
        securityGroups_Vm_3.add(neutronSecurityGroup_3);
        List<NeutronSecurityRule> securityRule_1 = new ArrayList<>();
        securityRule_1.add(neutronSecurityRule_1);
        List<NeutronSecurityRule> securityRule_2 = new ArrayList<>();
        securityRule_2.add(neutronSecurityRule_2);
        List<NeutronSecurityRule> securityRule_3 = new ArrayList<>();
        securityRule_3.add(neutronSecurityRule_3);

        neutron_IPs_1.add(neutron_ip_1);
        neutron_IPs_2.add(neutron_ip_2);
        neutron_IPs_3.add(neutron_ip_3);
        neutron_IPs_4.add(neutron_ip_4);
        neutron_IPs_5.add(neutron_ip_5);

        when(neutronPort_Vm1.getID()).thenReturn(NEUTRON_PORT_ID_VM_1);
        when(neutronPort_Vm2.getID()).thenReturn(NEUTRON_PORT_ID_VM_2);
        when(neutronPort_Vm3.getID()).thenReturn(NEUTRON_PORT_ID_VM_3);
        when(neutronPort_Vm4.getID()).thenReturn(NEUTRON_PORT_ID_VM_4);
        when(neutronPort_Vm5.getID()).thenReturn(NEUTRON_PORT_ID_VM_5);
        when(neutronPort_Vm1.getSecurityGroups()).thenReturn(securityGroups_Vm_1);
        when(neutronPort_Vm2.getSecurityGroups()).thenReturn(securityGroups_Vm_1);
        when(neutronPort_Vm3.getSecurityGroups()).thenReturn(securityGroups_Vm_3);
        when(neutronPort_Vm4.getSecurityGroups()).thenReturn(securityGroups_Vm_1);
        when(neutronPort_Vm5.getSecurityGroups()).thenReturn(securityGroups_Vm_3);
        when(neutronSecurityGroup_1.getSecurityRules()).thenReturn(securityRule_1);
        when(neutronSecurityGroup_2.getSecurityRules()).thenReturn(securityRule_2);
        when(neutronSecurityGroup_3.getSecurityRules()).thenReturn(securityRule_3);
        when(neutronSecurityGroup_1.getSecurityGroupUUID()).thenReturn(SECURITY_GROUP_ID_1);
        when(neutronSecurityGroup_2.getSecurityGroupUUID()).thenReturn(SECURITY_GROUP_ID_2);
        when(neutronSecurityGroup_3.getSecurityGroupUUID()).thenReturn(SECURITY_GROUP_ID_3);
        when(neutronSecurityRule_1.getSecurityRemoteGroupID()).thenReturn(SECURITY_GROUP_ID_1);
        when(neutronSecurityRule_3.getSecurityRemoteGroupID()).thenReturn(SECURITY_GROUP_ID_2);
        when(neutronPort_Vm1.getFixedIPs()).thenReturn(neutron_IPs_1);
        when(neutronPort_Vm2.getFixedIPs()).thenReturn(neutron_IPs_2);
        when(neutronPort_Vm3.getFixedIPs()).thenReturn(neutron_IPs_3);
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_VM_1))).thenReturn(neutronPort_Vm1);
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_VM_2))).thenReturn(neutronPort_Vm2);
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_VM_3))).thenReturn(neutronPort_Vm3);
    }

    /**
     * Remote Cache is empty a new port is added.
     */
    @Test
    public void testPortAddedWithNoRemoteSGInCache() {
        securityGroupCacheManagerImpl.portAdded(SECURITY_GROUP_ID_1, NEUTRON_PORT_ID_VM_1);
        verify(securityServicesManager, times(0)).syncSecurityRule(any(NeutronPort.class), any(NeutronSecurityRule.class), any(Neutron_IPs.class),anyBoolean());
    }

    /**
     * Remote Cache is empty a new port is removed.
     */
    @Test
    public void testPortRemovedWithNoRemoteSGInCache() {
        securityGroupCacheManagerImpl.addToCache(SECURITY_GROUP_ID_1, NEUTRON_PORT_ID_VM_1);
        securityGroupCacheManagerImpl.portRemoved(SECURITY_GROUP_ID_1, NEUTRON_PORT_ID_VM_1);
        verify(securityServicesManager, times(0)).syncSecurityRule(any(NeutronPort.class), any(NeutronSecurityRule.class), any(Neutron_IPs.class),anyBoolean());
    }

    /**
     * neutronSecurityGroup_1 has a rule which has neutronSecurityGroup_1 as remote SG.
     * A port with neutronSecurityGroup_1 is present in cache and new one is added.
     */
    @Test
    public void testPortAddedWithSelfInCache() {
        securityGroupCacheManagerImpl.addToCache(SECURITY_GROUP_ID_1, NEUTRON_PORT_ID_VM_1);
        securityGroupCacheManagerImpl.portAdded(SECURITY_GROUP_ID_1, NEUTRON_PORT_ID_VM_1);
        securityGroupCacheManagerImpl.addToCache(SECURITY_GROUP_ID_1, NEUTRON_PORT_ID_VM_2);
        securityGroupCacheManagerImpl.portAdded(SECURITY_GROUP_ID_1, NEUTRON_PORT_ID_VM_2);
        verify(securityServicesManager, times(1)).syncSecurityRule(eq(neutronPort_Vm1), eq(neutronSecurityRule_1), eq(neutron_ip_2),eq(true));
    }

    /**
     * neutronSecurityGroup_1 has a rule which has neutronSecurityGroup_1 as remote SG.
     * Two port with neutronSecurityGroup_1 is present in cache and  one of them is removed.
     */
    @Test
    public void testPortRemovedWithSelfInCache() {
        securityGroupCacheManagerImpl.addToCache(SECURITY_GROUP_ID_1, NEUTRON_PORT_ID_VM_1);
        securityGroupCacheManagerImpl.addToCache(SECURITY_GROUP_ID_1, NEUTRON_PORT_ID_VM_2);
        securityGroupCacheManagerImpl.portRemoved(SECURITY_GROUP_ID_1, NEUTRON_PORT_ID_VM_2);
        verify(securityServicesManager, times(1)).syncSecurityRule(eq(neutronPort_Vm1), eq(neutronSecurityRule_1), eq(neutron_ip_2),eq(false));
    }

    /**
     * neutronSecurityGroup_3 has a rule which has neutronSecurityGroup_2 as remote SG.
     * A port with neutronSecurityGroup_3 is present in cache. A new port is added with
     * neutronSecurityGroup_2 as security group.
     */
    @Test
    public void testPortAddedWithCidrInCache() {
        securityGroupCacheManagerImpl.addToCache(SECURITY_GROUP_ID_2, NEUTRON_PORT_ID_VM_3);
        securityGroupCacheManagerImpl.portAdded(SECURITY_GROUP_ID_2, NEUTRON_PORT_ID_VM_2);
        verify(securityServicesManager, times(1)).syncSecurityRule(eq(neutronPort_Vm3), eq(neutronSecurityRule_3), eq(neutron_ip_2),eq(true));
    }

    /**
     * neutronSecurityGroup_3 has a rule which has neutronSecurityGroup_2 as remote SG.
     * A port with neutronSecurityGroup_3 is present in cache. A  port with
     * neutronSecurityGroup_2 as security group is removed..
     */
    @Test
    public void testPortRemovedWithCidrInCache() {
        securityGroupCacheManagerImpl.addToCache(SECURITY_GROUP_ID_2, NEUTRON_PORT_ID_VM_3);
        securityGroupCacheManagerImpl.portRemoved(SECURITY_GROUP_ID_2, NEUTRON_PORT_ID_VM_2);
        verify(securityServicesManager, times(1)).syncSecurityRule(eq(neutronPort_Vm3), eq(neutronSecurityRule_3), eq(neutron_ip_2),eq(false));
    }

    /**
     *  A port is removed from the cache.
     */
    @Test
    public void testPortRemovedFromCache() {
        securityGroupCacheManagerImpl.addToCache(SECURITY_GROUP_ID_2, NEUTRON_PORT_ID_VM_3);
        securityGroupCacheManagerImpl.removeFromCache(SECURITY_GROUP_ID_2, NEUTRON_PORT_ID_VM_3);
        securityGroupCacheManagerImpl.portRemoved(SECURITY_GROUP_ID_2, NEUTRON_PORT_ID_VM_2);
        verify(securityServicesManager, times(0)).syncSecurityRule(any(NeutronPort.class), any(NeutronSecurityRule.class), any(Neutron_IPs.class),anyBoolean());
    }
}