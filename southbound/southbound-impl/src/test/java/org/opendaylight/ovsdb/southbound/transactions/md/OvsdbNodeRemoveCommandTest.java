/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

@PrepareForTest({OvsdbNodeRemoveCommand.class})
@RunWith(PowerMockRunner.class)
public class OvsdbNodeRemoveCommandTest {
    private static final long ONE_CONNECTED_MANAGER = 1;
    private static final long ONE_ACTIVE_CONNECTION_IN_PASSIVE_MODE = 1;
    private OvsdbNodeRemoveCommand ovsdbNodeRemoveCommand;

    @Before
    public void setUp() throws Exception {
        ovsdbNodeRemoveCommand = PowerMockito.mock(OvsdbNodeRemoveCommand.class, Mockito.CALLS_REAL_METHODS);
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
        ReadWriteTransaction transaction= mock(ReadWriteTransaction.class);
        CheckedFuture<Optional<Node>, ReadFailedException> ovsdbNodeFuture = mock(CheckedFuture.class);
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbNodeRemoveCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(mock(InstanceIdentifier.class));
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(ovsdbNodeFuture);

        Optional<Node> ovsdbNodeOptional = mock(Optional.class);
        when(ovsdbNodeFuture.get()).thenReturn(ovsdbNodeOptional);
        when(ovsdbNodeOptional.isPresent()).thenReturn(true);
        Node ovsdbNode = mock(Node.class);
        when(ovsdbNodeOptional.get()).thenReturn(ovsdbNode);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        when(ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);

        PowerMockito.doReturn(true).when(ovsdbNodeRemoveCommand, "checkIfOnlyConnectedManager", any(OvsdbNodeAugmentation.class));

        List<ManagedNodeEntry> listManagedNodeEntry = new ArrayList<>();
        ManagedNodeEntry managedNode = mock(ManagedNodeEntry.class);
        listManagedNodeEntry.add(managedNode);
        when(ovsdbNodeAugmentation.getManagedNodeEntry()).thenReturn(listManagedNodeEntry);
        OvsdbBridgeRef ovsdbBridgeRef = mock(OvsdbBridgeRef.class);
        when(managedNode.getBridgeRef()).thenReturn(ovsdbBridgeRef);
        when(ovsdbBridgeRef.getValue()).thenReturn(mock(InstanceIdentifier.class));

        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));

        ovsdbNodeRemoveCommand.execute(transaction);
        verify(ovsdbNodeAugmentation, times(2)).getManagedNodeEntry();
        verify(transaction, times(2)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testCheckIfOnlyConnectedManager() throws Exception {
        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        ManagerEntry onlyConnectedManager= mock(ManagerEntry.class);
        ManagerEntry manager = mock(ManagerEntry.class);
        List<ManagerEntry> listManagerEntry = new ArrayList<>();
        listManagerEntry.add(manager);

        //case 1: connectedManager > ONE_CONNECTED_MANAGER
        ManagerEntry manager1 = mock(ManagerEntry.class);
        listManagerEntry.add(manager1);
        when(ovsdbNodeAugmentation.getManagerEntry()).thenReturn(listManagerEntry);
        when(manager.isConnected()).thenReturn(true, false, true);
        when(manager1.isConnected()).thenReturn(true, false, true);
        assertEquals(false, Whitebox.invokeMethod(ovsdbNodeRemoveCommand, "checkIfOnlyConnectedManager", ovsdbNodeAugmentation));

        //case 2: connectedManager == 0
        assertEquals(true, Whitebox.invokeMethod(ovsdbNodeRemoveCommand, "checkIfOnlyConnectedManager", ovsdbNodeAugmentation));

        //case 3: onlyConnectedManager.getNumberOfConnections().longValue() > ONE_ACTIVE_CONNECTION_IN_PASSIVE_MODE
        when(onlyConnectedManager.getNumberOfConnections()).thenReturn(ONE_CONNECTED_MANAGER + 1,
                ONE_ACTIVE_CONNECTION_IN_PASSIVE_MODE);
        assertEquals(false, Whitebox.invokeMethod(ovsdbNodeRemoveCommand, "checkIfOnlyConnectedManager", ovsdbNodeAugmentation));

        //case 4: when all the above don't apply
        listManagerEntry.remove(manager1);
        assertEquals(true, Whitebox.invokeMethod(ovsdbNodeRemoveCommand, "checkIfOnlyConnectedManager", ovsdbNodeAugmentation));
    }
}
