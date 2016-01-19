/*
 * Copyright (c) 2015, 2016 NEC Corporation and others.  All rights reserved.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronFloatingIP;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.Floatingips;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.floatingips.Floatingip;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.floatingips.FloatingipBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Unit test for {@link NeutronFloatingIPInterface}
 */
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
     * Floatingip object reference for unit testing.
     */
    private Floatingip floatingip = new FloatingipBuilder().setUuid(new Uuid(UUID_VALUE)).build();

    /**
     * Test that checks if @{NeutronFloatingIPInterface#floatingIPExists} is called
     * and then checks that floating Ip exists or not.
     */
    @Test
    public void testFloatingIPExists() throws Exception {
        ProviderContext providerContext = mock(ProviderContext.class);
        DataBroker broker = mock(DataBroker.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        NeutronFloatingIPInterface neutronFloatingIPInterface = new NeutronFloatingIPInterface(providerContext);
        // First case: floatingIPExists() is expected to return true because the datastore contains a matching floating IP.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingip));
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        assertTrue("Should return true, when floatingIPExists success.", neutronFloatingIPInterface.floatingIPExists(UUID_VALUE));
        // Second case: the datastore has no matching floating IP, expect false
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
        ProviderContext providerContext = mock(ProviderContext.class);
        DataBroker broker = mock(DataBroker.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        NeutronFloatingIPInterface neutronFloatingIPInterface = new NeutronFloatingIPInterface(providerContext);
        // First case: getFloatingIP is expected to return a valid object.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingip));
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        NeutronFloatingIP neutronFloatingIPReceived = neutronFloatingIPInterface.getFloatingIP(UUID_VALUE);
        assertEquals("UUID mismatch", UUID_VALUE, neutronFloatingIPReceived.getID());
        // Second case: getFloatingIP returns null object.
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
        ProviderContext providerContext = mock(ProviderContext.class);
        DataBroker broker = mock(DataBroker.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        NeutronFloatingIPInterface neutronFloatingIPInterface = new NeutronFloatingIPInterface(providerContext);
        // First case: getAllFloatingIPs returns a list of valid objects.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        List<Floatingip> floatingipList = new ArrayList<>();
        floatingipList.add(floatingip);
        Floatingips floatingips = mock(Floatingips.class);
        NeutronFloatingIP neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(floatingips.getFloatingip()).thenReturn(floatingipList);
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingips));
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        List<NeutronFloatingIP> actualFloatingIps = neutronFloatingIPInterface.getAllFloatingIPs();
        assertEquals("There should be one floating IP", 1, actualFloatingIps.size());
        assertEquals("UUID mismatch", UUID_VALUE, actualFloatingIps.get(0).getID());
        // Second case: getAllFloatingIPs not returns a list of valid objects.
        CheckedFuture failingFuture = mock(CheckedFuture.class);
        when(failingFuture.checkedGet()).thenReturn(Optional.absent());
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(failingFuture);
        assertTrue("Non-empty list of floating IPs", neutronFloatingIPInterface.getAllFloatingIPs().isEmpty());
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#addFloatingIP} is called
     * and then verifies whether floating Ip already exists in datastore if not then
     * ensures floating ip addition by invoking MD-SAL add.
     */
    @Test
    public void testAddFloatingIP() throws Exception {
        ProviderContext providerContext = mock(ProviderContext.class);
        DataBroker broker = mock(DataBroker.class);
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        NeutronFloatingIP neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        NeutronFloatingIPInterface neutronFloatingIPInterface = new NeutronFloatingIPInterface(providerContext);
        when(neutronFloatingIP.getID()).thenReturn(UUID_VALUE);
        //First case: addFloatingIP returns false, the datastore has a matching floating IP.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingip));
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        assertFalse("Should return false, floating Ip already exists.", neutronFloatingIPInterface.addFloatingIP(neutronFloatingIP));
        //Second case: addFloatingIP returns true, the datastore has no matching floating IP, so invokes addMd() to write on datastore.
        CheckedFuture failingFuture = mock(CheckedFuture.class);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(failingFuture.checkedGet()).thenReturn(Optional.absent());
        when(writeTransaction.submit()).thenReturn(failingFuture);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(failingFuture);
        assertTrue("Should return true for addFloatingIP success.", neutronFloatingIPInterface.addFloatingIP(neutronFloatingIP));
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#removeFloatingIP} is called
     * and then verifies by reading floating ip from datastore and ensures floating ip
     * removal by invoking MD-SAL remove.
     */
    @Test
    public void testRemoveFloatingIP() throws Exception {
        ProviderContext providerContext = mock(ProviderContext.class);
        DataBroker broker = mock(DataBroker.class);
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        NeutronFloatingIP neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        NeutronFloatingIPInterface neutronFloatingIPInterface = new NeutronFloatingIPInterface(providerContext);
        when(neutronFloatingIP.getID()).thenReturn(UUID_VALUE);
        //First case: removeFloatingIP returns true by ensuring floating ip removal in datastore.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingip));
        when(writeTransaction.submit()).thenReturn(succeedingFuture);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        assertTrue("Should return true for removeFloatingIP success.", neutronFloatingIPInterface.removeFloatingIP(UUID_VALUE));
        // Second case: removeFloatingIP returns false for negative case.
        CheckedFuture failingFuture = mock(CheckedFuture.class);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(failingFuture.checkedGet()).thenReturn(Optional.absent());
        when(writeTransaction.submit()).thenReturn(failingFuture);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(failingFuture);
        assertFalse("Should return false for negative case.", neutronFloatingIPInterface.removeFloatingIP(UUID_VALUE));
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#updateFloatingIP} is called
     * and then verifies by reading floating ip from datastore and ensures floating ip
     * updation by invoking MD-SAL update.
     */
    @Test
    public void testUpdateFloatingIP() throws Exception {
        ProviderContext providerContext = mock(ProviderContext.class);
        DataBroker broker = mock(DataBroker.class);
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        NeutronFloatingIP neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        NeutronFloatingIPInterface neutronFloatingIPInterface = new NeutronFloatingIPInterface(providerContext);
        when(neutronFloatingIP.getID()).thenReturn(UUID_VALUE);
        //First case: updateFloatingIP returns true by ensuring floating ip updation in datastore.
        CheckedFuture succeedingFuture = mock(CheckedFuture.class);
        when(succeedingFuture.checkedGet()).thenReturn(Optional.of(floatingip));
        when(writeTransaction.submit()).thenReturn(succeedingFuture);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(readOnlyTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(succeedingFuture);
        assertTrue("Should return true for updateFloatingIP success.", neutronFloatingIPInterface.updateFloatingIP(UUID_VALUE, neutronFloatingIP));
        //Second case: updateFloatingIP returns false for negative case.
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
        ProviderContext providerContext = mock(ProviderContext.class);
        DataBroker broker = mock(DataBroker.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        NeutronFloatingIPInterface neutronFloatingIPInterface = new NeutronFloatingIPInterface(providerContext);
        NeutronFloatingIP neutronFloatingIP = new NeutronFloatingIP();
        neutronFloatingIP.setID(UUID_VALUE);
        neutronFloatingIP.setFloatingNetworkUUID(UUID_VALUE);
        neutronFloatingIP.setPortUUID(UUID_VALUE);
        neutronFloatingIP.setFixedIPAddress(FIXED_IP_ADDRESS);
        neutronFloatingIP.setFloatingIPAddress(FLOATING_IP_ADDRESS);
        neutronFloatingIP.setTenantUUID(UUID_VALUE);
        neutronFloatingIP.setRouterUUID(UUID_VALUE);
        neutronFloatingIP.setStatus(STATUS);
        Floatingip floatingipReceived = neutronFloatingIPInterface.toMd(neutronFloatingIP);
        assertEquals("UUID mismatch", UUID_VALUE, String.valueOf(floatingipReceived.getUuid().getValue()));
        assertEquals("FloatingNetworkId mismatch", UUID_VALUE, String.valueOf(floatingipReceived.getFloatingNetworkId().getValue()));
        assertEquals("Port ID mismatch", UUID_VALUE, String.valueOf(floatingipReceived.getPortId().getValue()));
        assertEquals("Fixed IP Address mismatch", FIXED_IP_ADDRESS, String.valueOf(floatingipReceived.getFixedIpAddress().getValue()));
        assertEquals("Floating IP Address mismatch", FLOATING_IP_ADDRESS, String.valueOf(floatingipReceived.getFloatingIpAddress().getValue()));
        assertEquals("Tenant Id mismatch", UUID_VALUE, String.valueOf(floatingipReceived.getTenantId().getValue()));
        assertEquals("Router Id mismatch", UUID_VALUE, String.valueOf(floatingipReceived.getRouterId().getValue()));
        assertEquals("Status mismatch", STATUS, String.valueOf(floatingipReceived.getStatus()));
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#fromMd} is called
     * and then checks that it gets values from Floating Ip.
     */
    @Test
    public void testFromMd() throws Exception {
        ProviderContext providerContext = mock(ProviderContext.class);
        DataBroker broker = mock(DataBroker.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        Floatingip actualfloatingip = new FloatingipBuilder()
                .setUuid(new Uuid(UUID_VALUE))
                .setFixedIpAddress(
                        new IpAddress(FIXED_IP_ADDRESS.toCharArray()))
                .setFloatingIpAddress(
                        new IpAddress(FLOATING_IP_ADDRESS.toCharArray()))
                .setFloatingNetworkId(new Uuid(UUID_VALUE))
                .setPortId(new Uuid(UUID_VALUE))
                .setRouterId(new Uuid(UUID_VALUE)).setStatus(STATUS)
                .setTenantId(new Uuid(UUID_VALUE)).build();
        NeutronFloatingIPInterface neutronFloatingIPInterface = new NeutronFloatingIPInterface(providerContext);
        NeutronFloatingIP neutronFloatingIPReceived = neutronFloatingIPInterface.fromMd(actualfloatingip);
        assertEquals("UUID mismatch", UUID_VALUE, neutronFloatingIPReceived.getID());
        assertEquals("FloatingNetworkId mismatch", UUID_VALUE, neutronFloatingIPReceived.getFloatingNetworkUUID());
        assertEquals("Port ID mismatch", UUID_VALUE, neutronFloatingIPReceived.getPortUUID());
        assertEquals("Fixed IP Address mismatch", FIXED_IP_ADDRESS, neutronFloatingIPReceived.getFixedIPAddress());
        assertEquals("Floating IP Address mismatch", FLOATING_IP_ADDRESS, neutronFloatingIPReceived.getFloatingIPAddress());
        assertEquals("Tenant Id mismatch", UUID_VALUE, neutronFloatingIPReceived.getTenantUUID());
        assertEquals("Router Id mismatch", UUID_VALUE, neutronFloatingIPReceived.getRouterUUID());
        assertEquals("Status mismatch", STATUS, neutronFloatingIPReceived.getStatus());
    }
}
