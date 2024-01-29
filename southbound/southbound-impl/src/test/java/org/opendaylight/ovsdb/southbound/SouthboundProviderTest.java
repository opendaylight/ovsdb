/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.infrautils.ready.testutils.TestSystemReadyMonitor.Behaviour.IMMEDIATE;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceRegistration;
import org.opendaylight.infrautils.ready.testutils.TestSystemReadyMonitor;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipChange;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipListener;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipChangeState;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class SouthboundProviderTest extends AbstractConcurrentDataBrokerTest {
    private EntityOwnershipService entityOwnershipService;
    private DiagStatusService diagStatusService;

    public SouthboundProviderTest() {
        super(true);
    }

    @Before
    public void setUp() throws CandidateAlreadyRegisteredException {
        entityOwnershipService = mock(EntityOwnershipService.class);
        when(entityOwnershipService.registerListener(anyString(), any(EntityOwnershipListener.class))).thenReturn(
                mock(Registration.class));
        when(entityOwnershipService.registerCandidate(any(Entity.class))).thenReturn(mock(Registration.class));

        diagStatusService = Mockito.mock(DiagStatusService.class);
        when(diagStatusService.register(any())).thenReturn(Mockito.mock(ServiceRegistration.class));
    }

    @Test
    public void testInit() throws CandidateAlreadyRegisteredException {
        // Indicate that this is the owner
        when(entityOwnershipService.getOwnershipState(any(Entity.class))).thenReturn(
                java.util.Optional.of(EntityOwnershipState.from(true, true)));

        try (SouthboundProvider southboundProvider = new SouthboundProvider(
                getDataBroker(),
                entityOwnershipService,
                Mockito.mock(OvsdbConnection.class),
                Mockito.mock(DOMSchemaService.class),
                Mockito.mock(BindingNormalizedNodeSerializer.class),
                new TestSystemReadyMonitor(IMMEDIATE),
                diagStatusService)) {

            // Verify that at least one listener was registered
            verify(entityOwnershipService, atLeastOnce()).registerListener(
                    anyString(), any(EntityOwnershipListener.class));

            // Verify that a candidate was registered
            verify(entityOwnershipService).registerCandidate(any(Entity.class));
        }
    }

    @Test
    public void testInitWithClose() throws CandidateAlreadyRegisteredException {
        // Indicate that this is the owner
        when(entityOwnershipService.getOwnershipState(any(Entity.class))).thenReturn(
                java.util.Optional.of(EntityOwnershipState.from(true, true)));

        try (SouthboundProvider southboundProvider = new SouthboundProvider(
                getDataBroker(),
                entityOwnershipService,
                Mockito.mock(OvsdbConnection.class),
                Mockito.mock(DOMSchemaService.class),
                Mockito.mock(BindingNormalizedNodeSerializer.class),
                new TestSystemReadyMonitor(IMMEDIATE),
                diagStatusService)) {

            // Verify that at least one listener was registered
            verify(entityOwnershipService, atLeastOnce()).registerListener(
                    anyString(), any(EntityOwnershipListener.class));

            // Verify that a candidate was registered
            verify(entityOwnershipService).registerCandidate(any(Entity.class));

            //Close the session
            southboundProvider.close();
        }
    }

    @Test
    public void testGetDb() {
        when(entityOwnershipService.getOwnershipState(any(Entity.class))).thenReturn(
            java.util.Optional.of(EntityOwnershipState.from(true, true)));

        try (SouthboundProvider southboundProvider = new SouthboundProvider(
                getDataBroker(),
                entityOwnershipService,
                Mockito.mock(OvsdbConnection.class),
                Mockito.mock(DOMSchemaService.class),
                Mockito.mock(BindingNormalizedNodeSerializer.class),
                new TestSystemReadyMonitor(IMMEDIATE),
                diagStatusService)) {

            assertEquals(getDataBroker(), SouthboundProvider.getDb());
        }
    }

    @Test
    public void testHandleOwnershipChange() throws ExecutionException, InterruptedException {
        when(entityOwnershipService.getOwnershipState(any(Entity.class))).thenReturn(
            java.util.Optional.of(EntityOwnershipState.from(false, true)));
        Entity entity = new Entity("ovsdb-southbound-provider", "ovsdb-southbound-provider");
        KeyedInstanceIdentifier<Topology, TopologyKey> topologyIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        try (SouthboundProvider southboundProvider = new SouthboundProvider(
                getDataBroker(),
                entityOwnershipService,
                Mockito.mock(OvsdbConnection.class),
                Mockito.mock(DOMSchemaService.class),
                Mockito.mock(BindingNormalizedNodeSerializer.class),
                new TestSystemReadyMonitor(IMMEDIATE),
                diagStatusService)) {

            // At this point the OVSDB topology must not be present in either tree
            assertFalse(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                    topologyIid).get().isPresent());
            assertFalse(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                    topologyIid).get().isPresent());

            // Become owner
            southboundProvider.handleOwnershipChange(new EntityOwnershipChange(entity,
                    EntityOwnershipChangeState.from(false, true, true)));

            // Up to 3 seconds for DTCL to settle
            final Stopwatch sw = Stopwatch.createStarted();
            while (!southboundProvider.isRegistered() && sw.elapsed(TimeUnit.SECONDS) < 3) {
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
            assertTrue(southboundProvider.isRegistered());

            // Now the OVSDB topology must be present in both trees
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                    topologyIid).get().isPresent());
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                    topologyIid).get().isPresent());

            // Verify idempotency
            southboundProvider.handleOwnershipChange(new EntityOwnershipChange(entity,
                    EntityOwnershipChangeState.from(false, true, true)));

            // The OVSDB topology must be present in both trees
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                    topologyIid).get().isPresent());
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                    topologyIid).get().isPresent());
        }
    }
}
