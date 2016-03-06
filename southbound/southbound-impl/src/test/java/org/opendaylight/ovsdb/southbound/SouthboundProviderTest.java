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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

import com.google.common.base.Optional;

public class SouthboundProviderTest extends AbstractDataBrokerTest {
    private EntityOwnershipService entityOwnershipService;

    @Before
    public void setUp() throws CandidateAlreadyRegisteredException {
        entityOwnershipService = mock(EntityOwnershipService.class);
        when(entityOwnershipService.registerListener(anyString(), any(EntityOwnershipListener.class))).thenReturn(
                mock(EntityOwnershipListenerRegistration.class));
        when(entityOwnershipService.registerCandidate(any(Entity.class))).thenReturn(mock(
                EntityOwnershipCandidateRegistration.class));
    }

    @Test
    public void testOnSessionInitiated() throws CandidateAlreadyRegisteredException {
        ProviderContext session = mock(ProviderContext.class);
        when(session.getSALService(DataBroker.class)).thenReturn(getDataBroker());

        // Indicate that this is the owner
        when(entityOwnershipService.getOwnershipState(any(Entity.class))).thenReturn(
                Optional.of(new EntityOwnershipState(true, true)));

        try (SouthboundProvider southboundProvider = new SouthboundProvider(entityOwnershipService,
                mock(OvsdbConnection.class))) {
            // Initiate the session
            southboundProvider.onSessionInitiated(session);

            // Verify that at least one listener was registered
            verify(entityOwnershipService, atLeastOnce()).registerListener(anyString(),
                    any(EntityOwnershipListener.class));

            // Verify that a candidate was registered
            verify(entityOwnershipService).registerCandidate(any(Entity.class));
        }
    }

    @Test
    public void testGetDb() {
        ProviderContext session = mock(ProviderContext.class);
        when(session.getSALService(DataBroker.class)).thenReturn(getDataBroker());
        when(entityOwnershipService.getOwnershipState(any(Entity.class))).thenReturn(
                Optional.of(new EntityOwnershipState(true, true)));
        try (SouthboundProvider southboundProvider = new SouthboundProvider(entityOwnershipService,
                mock(OvsdbConnection.class))) {
            southboundProvider.onSessionInitiated(session);

            assertEquals(getDataBroker(), SouthboundProvider.getDb());
        }
    }

    @Test
    public void testHandleOwnershipChange() throws ReadFailedException {
        // Start as slave
        ProviderContext session = mock(ProviderContext.class);
        when(session.getSALService(DataBroker.class)).thenReturn(getDataBroker());
        when(entityOwnershipService.getOwnershipState(any(Entity.class))).thenReturn(
                Optional.of(new EntityOwnershipState(false, true)));
        Entity entity = new Entity("ovsdb-southbound-provider", "ovsdb-southbound-provider");
        KeyedInstanceIdentifier<Topology, TopologyKey> topologyIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        try (SouthboundProvider southboundProvider = new SouthboundProvider(entityOwnershipService,
                mock(OvsdbConnection.class))) {
            southboundProvider.onSessionInitiated(session);

            // At this point the OVSDB topology must not be present in either tree
            assertFalse(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                    topologyIid).checkedGet().isPresent());
            assertFalse(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                    topologyIid).checkedGet().isPresent());

            // Become owner
            southboundProvider.handleOwnershipChange(new EntityOwnershipChange(entity, false, true, true));

            // Now the OVSDB topology must be present in both trees
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                    topologyIid).checkedGet().isPresent());
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                    topologyIid).checkedGet().isPresent());

            // Verify idempotency
            southboundProvider.handleOwnershipChange(new EntityOwnershipChange(entity, false, true, true));

            // The OVSDB topology must be present in both trees
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                    topologyIid).checkedGet().isPresent());
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                    topologyIid).checkedGet().isPresent());
        }
    }
}