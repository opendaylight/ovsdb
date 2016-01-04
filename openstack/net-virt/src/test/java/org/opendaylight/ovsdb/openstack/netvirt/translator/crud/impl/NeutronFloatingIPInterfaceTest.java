/**
 * Copyright (c) 2015 NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

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
     * Optional object reference for unit testing.
     */
    private Optional optional;
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
     * Test that checks if @{NeutronFloatingIPInterface#floatingIPExists} is called
     * and then checks that floating Ip exists or not.
     */
    @Test
    public void testFloatingIPExists() throws Exception {
        providerContext = mock(ProviderContext.class);
        broker = mock(DataBroker.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        neutronFloatingIPInterface = spy(new NeutronFloatingIPInterface(providerContext));
        //First case: floatingIPExists returns true.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        floatingip = new FloatingipBuilder().setUuid(new Uuid(UUID_VALUE)).build();
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingip));
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        assertTrue("Should return true, when floatingIPExists success.", neutronFloatingIPInterface.floatingIPExists(UUID_VALUE));
        //second case: floatingIPExists returns false.
        CheckedFuture failingFuture = mock(CheckedFuture.class);
        when(failingFuture.checkedGet()).thenReturn(Optional.absent());
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(failingFuture);
        assertFalse("Should return false for negative case.", neutronFloatingIPInterface.floatingIPExists(UUID_VALUE));
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#getFloatingIP} is called
     * and then checks that it gets floating Ip or not.
     */
    @Test
    public void testGetFloatingIP() throws Exception {
        providerContext = mock(ProviderContext.class);
        broker = mock(DataBroker.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        neutronFloatingIPInterface = spy(new NeutronFloatingIPInterface(providerContext));
        //First case: getFloatingIP returns valid object.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        floatingip = new FloatingipBuilder().setUuid(new Uuid(UUID_VALUE)).build();
        neutronFloatingIP = new NeutronFloatingIP();
        neutronFloatingIP.setID(String.valueOf(floatingip.getUuid().getValue()));
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingip));
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        NeutronFloatingIP neutronFloatingIPReceived = neutronFloatingIPInterface.getFloatingIP(UUID_VALUE);
        assertEquals("Both value should be equal, when getFloatingIP success.", neutronFloatingIPReceived.getID(), neutronFloatingIP.getID());
        //second case: getFloatingIP returns null object.
        CheckedFuture failingFuture = mock(CheckedFuture.class);
        when(failingFuture.checkedGet()).thenReturn(Optional.absent());
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(failingFuture);
        assertNull("Should be null for negative case.", neutronFloatingIPInterface.getFloatingIP(UUID_VALUE));
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#getAllFloatingIPs} is called
     * and then checks that it gets all floating Ips in a list or not.
     */
    @Test
    public void testGetAllFloatingIPs() throws Exception {
        providerContext = mock(ProviderContext.class);
        broker = mock(DataBroker.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        neutronFloatingIPInterface = spy(new NeutronFloatingIPInterface(providerContext));
        //First case: getAllFloatingIPs returns a list of valid objects.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        floatingip = new FloatingipBuilder().setUuid(new Uuid(UUID_VALUE)).build();
        List<Floatingip> floatingipList = new ArrayList<Floatingip>();
        floatingipList.add(floatingip);
        floatingips = mock(Floatingips.class);
        neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(floatingips.getFloatingip()).thenReturn(floatingipList);
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingips));
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        assertTrue("Should return true when getAllFloatingIPs success.", neutronFloatingIPInterface.getAllFloatingIPs().iterator().next() instanceof NeutronFloatingIP);
        //second case: getAllFloatingIPs not returns a list of valid objects.
        CheckedFuture failingFuture = mock(CheckedFuture.class);
        when(failingFuture.checkedGet()).thenReturn(Optional.absent());
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(failingFuture);
        assertFalse("Should return false for negative case.", neutronFloatingIPInterface.getAllFloatingIPs().contains(neutronFloatingIP));
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#addFloatingIP} is called
     * and then checks that it adds floating IP.
     */
    @Test
    public void testAddFloatingIP() throws Exception {
        providerContext = mock(ProviderContext.class);
        broker = mock(DataBroker.class);
        writeTransaction = mock(WriteTransaction.class);
        neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        neutronFloatingIPInterface = spy(new NeutronFloatingIPInterface(providerContext));
        when(neutronFloatingIP.getID()).thenReturn(UUID_VALUE);
        //First case: addFloatingIP returns false.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        floatingip = new FloatingipBuilder().setUuid(new Uuid(UUID_VALUE)).build();
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingip));
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        assertFalse("Should return false, floating Ip already exists.", neutronFloatingIPInterface.addFloatingIP(neutronFloatingIP));
        //second case: addFloatingIP returns true.
        CheckedFuture failingFuture = mock(CheckedFuture.class);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(failingFuture.checkedGet()).thenReturn(Optional.absent());
        when(writeTransaction.submit()).thenReturn(failingFuture);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(failingFuture);
        assertTrue("Should return true for addFloatingIP success.", neutronFloatingIPInterface.addFloatingIP(neutronFloatingIP));
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#removeFloatingIP} is called
     * and then checks that it removes floating Ip.
     */
    @Test
    public void testRemoveFloatingIP() throws Exception {
        providerContext = mock(ProviderContext.class);
        broker = mock(DataBroker.class);
        writeTransaction = mock(WriteTransaction.class);
        neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        neutronFloatingIPInterface = spy(new NeutronFloatingIPInterface(providerContext));
        when(neutronFloatingIP.getID()).thenReturn(UUID_VALUE);
        //First case: removeFloatingIP returns true.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        floatingip = new FloatingipBuilder().setUuid(new Uuid(UUID_VALUE)).build();
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingip));
        when(writeTransaction.submit()).thenReturn(succeedingFuture);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        assertTrue("Should return true for removeFloatingIP success.", neutronFloatingIPInterface.removeFloatingIP(UUID_VALUE));
        //second case: removeFloatingIP returns false.
        CheckedFuture failingFuture = mock(CheckedFuture.class);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(failingFuture.checkedGet()).thenReturn(Optional.absent());
        when(writeTransaction.submit()).thenReturn(failingFuture);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(failingFuture);
        assertFalse("Should return false for negative case.", neutronFloatingIPInterface.removeFloatingIP(UUID_VALUE));
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#updateFloatingIP} is called
     * and then checks that it updates floating Ip.
     */
    @Test
    public void testUpdateFloatingIP() throws Exception {
        providerContext = mock(ProviderContext.class);
        broker = mock(DataBroker.class);
        writeTransaction = mock(WriteTransaction.class);
        neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        neutronFloatingIPInterface = spy(new NeutronFloatingIPInterface(providerContext));
        when(neutronFloatingIP.getID()).thenReturn(UUID_VALUE);
        //First case: updateFloatingIP returns true.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        floatingip = new FloatingipBuilder().setUuid(new Uuid(UUID_VALUE)).build();
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingip));
        when(writeTransaction.submit()).thenReturn(succeedingFuture);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        assertTrue("Should return true for updateFloatingIP success.", neutronFloatingIPInterface.updateFloatingIP(UUID_VALUE, neutronFloatingIP));
        //second case: updateFloatingIP returns false.
        CheckedFuture failingFuture = mock(CheckedFuture.class);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(failingFuture.checkedGet()).thenReturn(Optional.absent());
        when(writeTransaction.submit()).thenReturn(failingFuture);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(failingFuture);
        assertFalse("Should return false for negative case.", neutronFloatingIPInterface.updateFloatingIP(UUID_VALUE, neutronFloatingIP));
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#toMd} is called
     * and then checks that it sets vales into floating Ip.
     */
    @Test
    public void testToMd() throws Exception {
        providerContext = mock(ProviderContext.class);
        broker = mock(DataBroker.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        neutronFloatingIPInterface = spy(new NeutronFloatingIPInterface(providerContext));
        floatingip = new FloatingipBuilder().setUuid(new Uuid(UUID_VALUE)).build();
        neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(neutronFloatingIP.getID()).thenReturn(UUID_VALUE);
        when(neutronFloatingIP.getFixedIPAddress()).thenReturn(FIXED_IP_ADDRESS);
        when(neutronFloatingIP.getFloatingIPAddress()).thenReturn(FLOATING_IP_ADDRESS);
        when(neutronFloatingIP.getFloatingNetworkUUID()).thenReturn(UUID_VALUE);
        when(neutronFloatingIP.getPortUUID()).thenReturn(UUID_VALUE);
        when(neutronFloatingIP.getRouterUUID()).thenReturn(UUID_VALUE);
        when(neutronFloatingIP.getStatus()).thenReturn(STATUS);
        when(neutronFloatingIP.getTenantUUID()).thenReturn(UUID_VALUE);
        Floatingip floatingipReceived = neutronFloatingIPInterface.toMd(neutronFloatingIP);
        assertEquals("Should be equal", floatingipReceived.getUuid(),floatingip.getUuid());
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#fromMd} is called
     * and then checks that it gets values from Floating Ip.
     */
    @Test
    public void testFromMd() throws Exception {
        providerContext = mock(ProviderContext.class);
        broker = mock(DataBroker.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        floatingip = mock(Floatingip.class);
        neutronFloatingIP = new NeutronFloatingIP();
        neutronFloatingIP.setID(UUID_VALUE);
        neutronFloatingIPInterface = spy(new NeutronFloatingIPInterface(providerContext));
        when(floatingip.getUuid()).thenReturn(new Uuid(UUID_VALUE));
        when(floatingip.getFixedIpAddress()).thenReturn(new IpAddress((FIXED_IP_ADDRESS).toCharArray()));
        when(floatingip.getFloatingIpAddress()).thenReturn(new IpAddress((FLOATING_IP_ADDRESS).toCharArray()));
        when(floatingip.getFloatingNetworkId()).thenReturn(new Uuid(UUID_VALUE));
        when(floatingip.getPortId()).thenReturn(new Uuid(UUID_VALUE));
        when(floatingip.getRouterId()).thenReturn(new Uuid(UUID_VALUE));
        when(floatingip.getStatus()).thenReturn(STATUS);
        when(floatingip.getTenantId()).thenReturn(new Uuid(UUID_VALUE));
        NeutronFloatingIP neutronFloatingIPReceived = neutronFloatingIPInterface.fromMd(floatingip);
        assertEquals("Should be equal.", neutronFloatingIP.getID(), neutronFloatingIPReceived.getID());
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#registerNewInterface} is called
     * and then checks that it register service or not.
     */
    @Test
    public void testRegisterNewInterface() throws Exception {
        providerContext = mock(ProviderContext.class);
        broker = mock(DataBroker.class);
        bundleContext = mock(BundleContext.class);
        serviceRegistration = mock(ServiceRegistration.class);
        mockStatic(NeutronFloatingIPInterface.class);
        List<ServiceRegistration<?>> serviceRegistrationList = new ArrayList<>();
        serviceRegistrationList.add(serviceRegistration);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        neutronFloatingIPInterface = spy(new NeutronFloatingIPInterface(providerContext));
        when(bundleContext.registerService(INeutronFloatingIPCRUD.class, neutronFloatingIPInterface, null)).thenReturn(serviceRegistration);
        NeutronFloatingIPInterface.registerNewInterface(bundleContext, providerContext, serviceRegistrationList);
        verifyStatic();
    }
}