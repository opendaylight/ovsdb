/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.class)
public class OvsdbNodeRemoveCommandTest {
    private static final long ONE_CONNECTED_MANAGER = 1;
    private static final long ONE_ACTIVE_CONNECTION_IN_PASSIVE_MODE = 1;

    private OvsdbNodeRemoveCommand ovsdbNodeRemoveCommand;

    @Before
    public void setUp() throws Exception {
        ovsdbNodeRemoveCommand = mock(OvsdbNodeRemoveCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testOvsdbControllerRemovedCommand() {
        OvsdbConnectionInstance key = mock(OvsdbConnectionInstance.class);
        TableUpdates updates = mock(TableUpdates.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        OvsdbNodeRemoveCommand ovsdbNodeRemoveCommand1 = new OvsdbNodeRemoveCommand(key, updates, dbSchema);
        assertEquals(key, Whitebox.getInternalState(ovsdbNodeRemoveCommand1, "key"));
        assertEquals(updates, Whitebox.getInternalState(ovsdbNodeRemoveCommand1, "updates"));
        assertEquals(dbSchema, Whitebox.getInternalState(ovsdbNodeRemoveCommand1, "dbSchema"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        FluentFuture<Optional<Node>> ovsdbNodeFuture = mock(FluentFuture.class);
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbNodeRemoveCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(
            InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId("testConnection"))));
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(ovsdbNodeFuture);

        Node ovsdbNode = mock(Node.class);
        Optional<Node> ovsdbNodeOptional = Optional.of(ovsdbNode);
        when(ovsdbNodeFuture.get()).thenReturn(ovsdbNodeOptional);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        when(ovsdbNode.augmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);

        doReturn(true).when(ovsdbNodeRemoveCommand).checkIfOnlyConnectedManager(any(OvsdbNodeAugmentation.class));

        ManagedNodeEntry managedNode = new ManagedNodeEntryBuilder()
                .setBridgeRef(new OvsdbBridgeRef(InstanceIdentifier.create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                    .child(Node.class, new NodeKey(new NodeId("testBridge")))))
                .build();

        when(ovsdbNodeAugmentation.getManagedNodeEntry()).thenReturn(Map.of(managedNode.key(), managedNode));

        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));

        ovsdbNodeRemoveCommand.execute(transaction);
        verify(ovsdbNodeAugmentation).getManagedNodeEntry();
        verify(transaction, times(2)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testCheckIfOnlyConnectedManager() throws Exception {
        ManagerEntry manager = mock(ManagerEntry.class);
        ManagerEntry manager1 = mock(ManagerEntry.class);
        doReturn(new ManagerEntryKey(new Uri("manager"))).when(manager).key();
        doReturn(new ManagerEntryKey(new Uri("manager1"))).when(manager1).key();

        when(manager.getConnected()).thenReturn(true, false, true);
        when(manager1.getConnected()).thenReturn(true, false, true);
        when(manager.getNumberOfConnections()).thenReturn(Uint32.ZERO);

        Map<ManagerEntryKey, ManagerEntry> listManagerEntry = new HashMap<>();
        listManagerEntry.put(manager.key(), manager);
        listManagerEntry.put(manager1.key(), manager1);

        OvsdbNodeAugmentation ovsdbNodeAugmentation = new OvsdbNodeAugmentationBuilder()
                .setManagerEntry(listManagerEntry)
                .build();

        //case 1: connectedManager > ONE_CONNECTED_MANAGER
        assertEquals(false,
                Whitebox.invokeMethod(ovsdbNodeRemoveCommand, "checkIfOnlyConnectedManager", ovsdbNodeAugmentation));

        // case 2: connectedManager == 0
        assertEquals(true,
                Whitebox.invokeMethod(ovsdbNodeRemoveCommand, "checkIfOnlyConnectedManager", ovsdbNodeAugmentation));

        // case 3: onlyConnectedManager.getNumberOfConnections().longValue() > ONE_ACTIVE_CONNECTION_IN_PASSIVE_MODE
        ManagerEntry onlyConnectedManager = mock(ManagerEntry.class);
        assertEquals(false,
                Whitebox.invokeMethod(ovsdbNodeRemoveCommand, "checkIfOnlyConnectedManager", ovsdbNodeAugmentation));

        // case 4: when all the above don't apply
        listManagerEntry.remove(manager1.key());
        assertEquals(true,
                Whitebox.invokeMethod(ovsdbNodeRemoveCommand, "checkIfOnlyConnectedManager", ovsdbNodeAugmentation));
    }
}
