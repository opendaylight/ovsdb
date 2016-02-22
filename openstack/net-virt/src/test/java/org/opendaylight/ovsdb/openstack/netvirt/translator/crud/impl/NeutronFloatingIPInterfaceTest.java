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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronFloatingIP;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.floatingips.Floatingip;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.floatingips.attributes.floatingips.FloatingipBuilder;

/**
 * Unit test for {@link NeutronFloatingIPInterface}
 */
public class NeutronFloatingIPInterfaceTest extends AbstractDataBrokerTest {
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

    private NeutronFloatingIPInterface getTestInterface(DataBroker broker) {
        ProviderContext providerContext = mock(ProviderContext.class);
        when(providerContext.getSALService(DataBroker.class)).thenReturn(broker);
        return new NeutronFloatingIPInterface(providerContext);
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#floatingIPExists} is called
     * and then checks that floating Ip exists or not.
     */
    @Test
    public void testFloatingIPExists() throws TransactionCommitFailedException {
        // floatingIPExists() returns true if the underlying data broker contains the node, false otherwise
        DataBroker broker = getDataBroker();
        NeutronFloatingIPInterface testInterface = getTestInterface(broker);

        // First case: the underlying data broker returns nothing (we haven't inserted the IP yet)
        assertFalse(testInterface.floatingIPExists(UUID_VALUE));

        // Add an IP
        addTestFloatingIP(broker, testInterface);

        // Second case: the underlying data broker returns something
        assertTrue(testInterface.floatingIPExists(UUID_VALUE));
    }

    private void addTestFloatingIP(DataBroker broker, NeutronFloatingIPInterface testInterface)
            throws TransactionCommitFailedException {
        WriteTransaction writeTransaction = broker.newWriteOnlyTransaction();
        Floatingip floatingip = new FloatingipBuilder().setUuid(new Uuid(UUID_VALUE)).build();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION,
                testInterface.createInstanceIdentifier(floatingip), floatingip);
        writeTransaction.submit().checkedGet();
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#getFloatingIP} is called
     * and then checks that it gets floating Ip or not.
     */
    @Test
    public void testGetFloatingIP() throws TransactionCommitFailedException {
        // getFloatingIP() returns the floating IP if the underlying data broker contains the node, null otherwise
        DataBroker broker = getDataBroker();
        NeutronFloatingIPInterface testInterface = getTestInterface(broker);

        // First case: the underlying data broker returns nothing (we haven't inserted the IP yet)
        assertNull(testInterface.getFloatingIP(UUID_VALUE));

        // Add an IP
        addTestFloatingIP(broker, testInterface);

        // Second case: the underlying data broker returns something
        final NeutronFloatingIP returnedFloatingIp = testInterface.getFloatingIP(UUID_VALUE);
        assertNotNull(returnedFloatingIp);
        assertEquals("UUID mismatch", UUID_VALUE, returnedFloatingIp.getID());
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#getAllFloatingIPs} is called
     * and then checks that it gets all floating Ips in a list or not.
     */
    @Test
    public void testGetAllFloatingIPs() throws TransactionCommitFailedException {
        // getAllFloatingIPs() returns all the floating IPs in the underlying data broker
        DataBroker broker = getDataBroker();
        NeutronFloatingIPInterface testInterface = getTestInterface(broker);

        // First case: the underlying data broker returns nothing (we haven't inserted the IP yet)
        assertTrue("Non-empty list of floating IPs", testInterface.getAllFloatingIPs().isEmpty());

        // Add an IP
        addTestFloatingIP(broker, testInterface);

        // Second case: the underlying data broker returns something
        final List<NeutronFloatingIP> allFloatingIPs = testInterface.getAllFloatingIPs();
        assertFalse("Empty list of floating IPs", allFloatingIPs.isEmpty());
        assertEquals("Incorrect number of floating IPs", 1, allFloatingIPs.size());
        assertEquals("UUID mismatch", UUID_VALUE, allFloatingIPs.get(0).getID());
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#addFloatingIP} is called
     * and then verifies whether floating Ip already exists in datastore if not then
     * ensures floating ip addition by invoking MD-SAL add.
     */
    @Test
    public void testAddFloatingIP() throws TransactionCommitFailedException {
        // addFloatingIP() adds the given floating IP if it isn't already in the data store
        DataBroker broker = getDataBroker();
        NeutronFloatingIPInterface testInterface = getTestInterface(broker);

        // First case: addFloatingIP() adds the floating IP
        NeutronFloatingIP insertedFloatingIp = new NeutronFloatingIP();
        insertedFloatingIp.setID(UUID_VALUE);
        assertTrue("Floating IP already present", testInterface.addFloatingIP(insertedFloatingIp));

        // TODO Retrieve the floating IP directly and make sure it's correct

        // Second case: the floating IP is already present
        assertFalse("Floating IP missing", testInterface.addFloatingIP(insertedFloatingIp));
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#removeFloatingIP} is called
     * and then verifies by reading floating ip from datastore and ensures floating ip
     * removal by invoking MD-SAL remove.
     */
    @Test
    public void testRemoveFloatingIP() throws TransactionCommitFailedException {
        // removeFloatingIP() removes the given floating IP if it's present in the data store
        DataBroker broker = getDataBroker();
        NeutronFloatingIPInterface testInterface = getTestInterface(broker);

        // First case: the floating IP isn't present
        assertFalse("Floating IP present", testInterface.removeFloatingIP(UUID_VALUE));

        // Add an IP
        addTestFloatingIP(broker, testInterface);

        // Second case: the floating IP is present
        assertTrue("Floating IP absent", testInterface.removeFloatingIP(UUID_VALUE));

        // TODO Attempt to retrieve the floating IP and make sure it's absent
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#updateFloatingIP} is called
     * and then verifies by reading floating ip from datastore and ensures floating ip
     * updation by invoking MD-SAL update.
     */
    @Test
    public void testUpdateFloatingIP() throws TransactionCommitFailedException {
        // updateFloatingIP() updates the given floating IP only if it's already in the data store
        DataBroker broker = getDataBroker();
        NeutronFloatingIPInterface testInterface = getTestInterface(broker);

        // First case: the floating IP isn't present
        NeutronFloatingIP testFloatingIp = new NeutronFloatingIP();
        testFloatingIp.setID(UUID_VALUE);
        assertFalse("Floating IP present", testInterface.updateFloatingIP(UUID_VALUE, testFloatingIp));

        // Add an IP
        addTestFloatingIP(broker, testInterface);

        // Second case: the floating IP is present
        assertTrue("Floating IP absent", testInterface.updateFloatingIP(UUID_VALUE, testFloatingIp));

        // TODO Change some attributes and make sure they're updated
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#toMd} is called
     * and then checks that it sets vales into floating Ip.
     */
    @Test
    public void testToMd() throws Exception {
        DataBroker broker = getDataBroker();
        NeutronFloatingIPInterface testInterface = getTestInterface(broker);
        NeutronFloatingIP neutronFloatingIP = new NeutronFloatingIP();
        neutronFloatingIP.setID(UUID_VALUE);
        neutronFloatingIP.setFloatingNetworkUUID(UUID_VALUE);
        neutronFloatingIP.setPortUUID(UUID_VALUE);
        neutronFloatingIP.setFixedIPAddress(FIXED_IP_ADDRESS);
        neutronFloatingIP.setFloatingIPAddress(FLOATING_IP_ADDRESS);
        neutronFloatingIP.setTenantUUID(UUID_VALUE);
        neutronFloatingIP.setRouterUUID(UUID_VALUE);
        neutronFloatingIP.setStatus(STATUS);
        Floatingip floatingipReceived = testInterface.toMd(neutronFloatingIP);
        assertEquals("UUID mismatch", UUID_VALUE, floatingipReceived.getUuid().getValue());
        assertEquals("FloatingNetworkId mismatch", UUID_VALUE, floatingipReceived.getFloatingNetworkId().getValue());
        assertEquals("Port ID mismatch", UUID_VALUE, floatingipReceived.getPortId().getValue());
        assertEquals("Fixed IP Address mismatch", FIXED_IP_ADDRESS, String.valueOf(floatingipReceived.getFixedIpAddress().getValue()));
        assertEquals("Floating IP Address mismatch", FLOATING_IP_ADDRESS, String.valueOf(floatingipReceived.getFloatingIpAddress().getValue()));
        assertEquals("Tenant Id mismatch", UUID_VALUE, floatingipReceived.getTenantId().getValue());
        assertEquals("Router Id mismatch", UUID_VALUE, floatingipReceived.getRouterId().getValue());
        assertEquals("Status mismatch", STATUS, floatingipReceived.getStatus());
    }

    /**
     * Test that checks if @{NeutronFloatingIPInterface#fromMd} is called
     * and then checks that it gets values from Floating Ip.
     */
    @Test
    public void testFromMd() throws Exception {
        DataBroker broker = getDataBroker();
        NeutronFloatingIPInterface testInterface = getTestInterface(broker);
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
        NeutronFloatingIP neutronFloatingIPReceived = testInterface.fromMd(actualfloatingip);
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
