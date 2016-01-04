/**
 * Copyright (c) 2015 NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronFloatingIP;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronFloatingIPCRUD;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.Floatingips;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.floatingips.Floatingip;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.floatingips.FloatingipBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Unit test for {@link NeutronFloatingIPInterface}
 */
@PrepareForTest({NeutronFloatingIPInterface.class})
@RunWith(PowerMockRunner.class)
public class NeutronFloatingIPInterfaceTest {
    /**
     * UUID_VALUE used for testing different scenarios.
     */
    private static final String UUID_VALUE = "b9a13232-525e-4d8c-be21-cd65e3436034";
    /**
     * FIXED_IP_ADDRESS used for testing different scenarios.
     */
    private static final String FIXED_IP_ADDRESS = "10.0.0.3";
    /**
     * FLOATING_IP_ADDRESS used for testing different scenarios.
     */
    private static final String FLOATING_IP_ADDRESS = "172.24.4.228";
    /**
     * STATUS used for testing different scenarios.
     */
    private static final String STATUS = "ACTIVE";
    /**
     * Boolean values used for testing different scenarios.
     */
    private boolean actualResult, actualResultTwo, expectedResultTwo, expectedResult = true;
    /**
     * NeutronFloatingIPInterface object reference for unit testing.
     */
    private NeutronFloatingIPInterface neutronFloatingIPInterface;
    /**
     * ProviderContext object reference for unit testing.
     */
    private ProviderContext providerContext;
    /**
     * NeutronFloatingIP object reference for unit testing.
     */
    private NeutronFloatingIP neutronFloatingIP;
    /**
     * Floatingip object reference for unit testing.
     */
    private Floatingip floatingip;
    /**
     * Floatingips object reference for unit testing.
     */
    private Floatingips floatingips;
    /**
     * BundleContext object reference for unit testing.
     */
    private BundleContext bundleContext;
    /**
     * ServiceRegistration object reference for unit testing.
     */
    private ServiceRegistration serviceRegistration;
    /**
     * InstanceIdentifier object reference for unit testing.
     */
    private InstanceIdentifier instanceIdentifier;
    /**
     * Optional object reference for unit testing.
     */
    private Optional optional;
    /**
     * CheckedFuture object reference for unit testing.
     */
    private CheckedFuture checked;
    /**
     * DataBroker object reference for unit testing.
     */
    private DataBroker broker;
    /**
     * ReadOnlyTransaction object reference for unit testing.
     */
    private ReadOnlyTransaction readOnlyTransaction;
    /**
     * WriteTransaction object reference for unit testing.
     */
    private WriteTransaction writeTransaction;

    /**
     * This method creates the required objects to perform unit testing.
     */
    @Before
    public void setUp() throws Exception {
        providerContext = mock(ProviderContext.class);
        neutronFloatingIP = mock(NeutronFloatingIP.class);
        floatingip = mock(Floatingip.class);
        floatingips = mock(Floatingips.class);
        bundleContext = mock(BundleContext.class);
        serviceRegistration = mock(ServiceRegistration.class);
        broker = mock(DataBroker.class);
        instanceIdentifier = mock(InstanceIdentifier.class);
        readOnlyTransaction = mock(ReadOnlyTransaction.class);
        writeTransaction = mock(WriteTransaction.class);
        checked = mock(CheckedFuture.class);
        optional = mock(Optional.class);
        when(floatingip.getUuid()).thenReturn(new Uuid(UUID_VALUE));
        when(optional.orNull()).thenReturn(floatingip);
        when(checked.checkedGet()).thenReturn(optional);
        when(writeTransaction.submit()).thenReturn(checked);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        neutronFloatingIPInterface = spy(new NeutronFloatingIPInterface(providerContext));
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#floatingIPExists} is called
     * and then checks that floating Ip  exists or not.
     */
    @Test
    public void testFloatingIPExists() throws Exception {
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(checked, null);
        actualResult = neutronFloatingIPInterface.floatingIPExists(UUID_VALUE);
        assertEquals("Should return true, when floatingIPExists success.", expectedResult, actualResult);
        actualResultTwo = neutronFloatingIPInterface.floatingIPExists(UUID_VALUE);
        expectedResultTwo = false;
        assertEquals("Should return fasle for negative case.", expectedResultTwo, actualResultTwo);
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#getFloatingIP} is called
     * and then checks that it gets floating Ip or not.
     */
    @Test
    public void testGetFloatingIP() throws Exception {
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(checked, null);
        Object outputOne = neutronFloatingIPInterface.getFloatingIP(UUID_VALUE);
        assertTrue("Should return true, when getFloatingIP success.", outputOne instanceof NeutronFloatingIP);
        Object outputTwo = neutronFloatingIPInterface.getFloatingIP(UUID_VALUE);
        assertFalse("Should return false for negative case.", outputTwo instanceof NeutronFloatingIP);
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#getAllFloatingIPs} is called
     * and then checks that it gets all floating Ips in a list or not.
     */
    @Test
    public void testGetAllFloatingIPs() throws Exception {
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(checked, null);
        when(optional.orNull()).thenReturn(floatingips, null);
        Object output = neutronFloatingIPInterface.getAllFloatingIPs();
        assertTrue("Should return true, when getAllFloatingIPs success.", output instanceof List);
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#addFloatingIP} is called
     * and then checks that it adds floating IP.
     */
    @Test
    public void testAddFloatingIP() throws Exception {
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(checked, null);
        when(neutronFloatingIP.getID()).thenReturn(UUID_VALUE);
        actualResultTwo = neutronFloatingIPInterface.addFloatingIP(neutronFloatingIP);
        actualResult = neutronFloatingIPInterface.addFloatingIP(neutronFloatingIP);
        assertEquals("Should return false for negative case.", expectedResultTwo, actualResultTwo);
        assertEquals("Should return true, when addFloatingIP success.", expectedResult, actualResult);
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#removeFloatingIP} is called
     * and then checks that it removes floating Ip.
     */
    @Test
    public void testRemoveFloatingIP() throws Exception {
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(checked, null);
        actualResult = neutronFloatingIPInterface.removeFloatingIP(UUID_VALUE);
        assertEquals("Should return true, when removeFloatingIP success.", expectedResult, actualResult);
        actualResultTwo = neutronFloatingIPInterface.removeFloatingIP(UUID_VALUE);
        assertEquals("Should return false for negative case.", expectedResultTwo, actualResultTwo);
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#updateFloatingIP} is called
     * and then checks that it updates floating Ip.
     */
    @Test
    public void testUpdateFloatingIP() throws Exception {
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(checked, null);
        actualResult = neutronFloatingIPInterface.updateFloatingIP(UUID_VALUE, neutronFloatingIP);
        assertEquals("Should return true, when updateFloatingIP success.", expectedResult, actualResult);
        actualResultTwo = neutronFloatingIPInterface.updateFloatingIP(UUID_VALUE, neutronFloatingIP);
        assertEquals("Should return false for negative case.", expectedResultTwo, actualResultTwo);
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#toMd} is called
     * and then checks that it sets vales into floating Ip .
     */
    @Test
    public void testToMd() throws Exception {
        when(neutronFloatingIP.getID()).thenReturn(UUID_VALUE);
        when(neutronFloatingIP.getFixedIPAddress()).thenReturn(FIXED_IP_ADDRESS);
        when(neutronFloatingIP.getFloatingIPAddress()).thenReturn(FLOATING_IP_ADDRESS);
        when(neutronFloatingIP.getFloatingNetworkUUID()).thenReturn(UUID_VALUE);
        when(neutronFloatingIP.getPortUUID()).thenReturn(UUID_VALUE);
        when(neutronFloatingIP.getRouterUUID()).thenReturn(UUID_VALUE);
        when(neutronFloatingIP.getStatus()).thenReturn(STATUS);
        when(neutronFloatingIP.getTenantUUID()).thenReturn(UUID_VALUE);
        Object output = neutronFloatingIPInterface.toMd(neutronFloatingIP);
        assertTrue("Should return true, when toMd success.", output instanceof Floatingip);
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#fromMd} is called
     * and then checks that it gets values from Floating Ip .
     */
    @Test
    public void testFromMd() throws Exception {
        when(floatingip.getUuid()).thenReturn(new Uuid(UUID_VALUE));
        when(floatingip.getFixedIpAddress()).thenReturn(new IpAddress((FIXED_IP_ADDRESS).toCharArray()));
        when(floatingip.getFloatingIpAddress()).thenReturn(new IpAddress((FLOATING_IP_ADDRESS).toCharArray()));
        when(floatingip.getFloatingNetworkId()).thenReturn(new Uuid(UUID_VALUE));
        when(floatingip.getPortId()).thenReturn(new Uuid(UUID_VALUE));
        when(floatingip.getRouterId()).thenReturn(new Uuid(UUID_VALUE));
        when(floatingip.getStatus()).thenReturn(STATUS);
        when(floatingip.getTenantId()).thenReturn(new Uuid(UUID_VALUE));
        Object output = neutronFloatingIPInterface.fromMd(floatingip);  
        assertTrue("Should return true, when fromMd success.", output instanceof NeutronFloatingIP);
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#registerNewInterface} is called
     * and then checks that it register service or not.
     */
    @Test
    public void testRegisterNewInterface() throws Exception {
        mockStatic(NeutronFloatingIPInterface.class);
        List<ServiceRegistration<?>> serviceRegistrationList = new ArrayList<>();
        serviceRegistrationList.add(serviceRegistration);
        when(bundleContext.registerService(INeutronFloatingIPCRUD.class, neutronFloatingIPInterface, null)).thenReturn(serviceRegistration);
        NeutronFloatingIPInterface.registerNewInterface(bundleContext, providerContext, serviceRegistrationList);
        verifyStatic();
    }
}