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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Optional;

@PrepareForTest({OvsdbControllerUpdateCommand.class, SouthboundMapper.class, SouthboundUtil.class, InstanceIdentifier.class})
@RunWith(PowerMockRunner.class)
public class OvsdbControllerUpdateCommandTest {
    private Map<UUID, Controller> updatedControllerRows;
    private Map<UUID, Bridge> updatedBridgeRows;
    private OvsdbControllerUpdateCommand ovsdbControllerUpdateCommand;

    private static final String BRIDGE_NAME = "br-int";
    private static final String NODE_ID = "OF|00:00:00:0c:29:70:45:9b";

    @Before
    public void setUp() {
        ovsdbControllerUpdateCommand = PowerMockito.mock(OvsdbControllerUpdateCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testOvsdbControllerUpdateCommand() {
        OvsdbConnectionInstance key = mock(OvsdbConnectionInstance.class);
        TableUpdates updates = mock(TableUpdates.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        OvsdbControllerUpdateCommand ovsdbControllerUpdateCommand1 = new OvsdbControllerUpdateCommand(key, updates, dbSchema);
        assertEquals(key, Whitebox.getInternalState(ovsdbControllerUpdateCommand1, "key"));
        assertEquals(updates, Whitebox.getInternalState(ovsdbControllerUpdateCommand1, "updates"));
        assertEquals(dbSchema, Whitebox.getInternalState(ovsdbControllerUpdateCommand1, "dbSchema"));
    }

    @Test
    public void testExecute() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        updatedControllerRows = new HashMap<UUID, Controller>();
        updatedControllerRows.put(mock(UUID.class), mock(Controller.class));
        MemberModifier.field(OvsdbControllerUpdateCommand.class, "updatedControllerRows").set(ovsdbControllerUpdateCommand, updatedControllerRows);
        MemberModifier.suppress(MemberMatcher.method(OvsdbControllerUpdateCommand.class, "updateController", ReadWriteTransaction.class, Map.class, Map.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbControllerUpdateCommand.class, "updateController", ReadWriteTransaction.class, Map.class));

        //updatedBridgeRows null case
        ovsdbControllerUpdateCommand.execute(transaction);
        PowerMockito.verifyPrivate(ovsdbControllerUpdateCommand).invoke("updateController", any(ReadWriteTransaction.class), any(Map.class));

        //updatedBridgeRows not null case
        updatedBridgeRows = new HashMap<UUID, Bridge>();
        updatedBridgeRows.put(mock(UUID.class), mock(Bridge.class));
        MemberModifier.field(OvsdbControllerUpdateCommand.class, "updatedBridgeRows").set(ovsdbControllerUpdateCommand, updatedBridgeRows);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateController1() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        Map<UUID, Controller> updatedControllerRows = new HashMap<>();
        Map<UUID, Bridge> updatedBridgeRows = new HashMap<>();
        Bridge bridge = mock(Bridge.class);
        updatedBridgeRows.put(mock(UUID.class), bridge);

        PowerMockito.mockStatic(SouthboundMapper.class);
        List<ControllerEntry> controllerEntries = new ArrayList<ControllerEntry>();
        controllerEntries.add(mock(ControllerEntry.class));
        when(SouthboundMapper.createControllerEntries(any(Bridge.class), any(Map.class))).thenReturn(controllerEntries);
        Column<GenericTableSchema, String> column = mock(Column.class);
        when(bridge.getNameColumn()).thenReturn(column);
        when(column.getData()).thenReturn(BRIDGE_NAME);

        //suppress call to getControllerEntryIid()
        MemberModifier.suppress(MemberMatcher.method(OvsdbControllerUpdateCommand.class, "getControllerEntryIid", ControllerEntry.class, String.class));
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(ControllerEntry.class));
        Whitebox.invokeMethod(ovsdbControllerUpdateCommand, "updateController", transaction, updatedControllerRows, updatedBridgeRows);
        verify(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(ControllerEntry.class));
        verify(bridge).getNameColumn();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateController2() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        Map<UUID, Controller> updatedControllerRows = new HashMap<>();
        Map<InstanceIdentifier<Node>, Node> bridgeNodes = new HashMap<>();
        Node node = mock(Node.class);
        InstanceIdentifier<Node> bridgeIid = mock(InstanceIdentifier.class);
        bridgeNodes.put(bridgeIid, node);
        PowerMockito.doReturn(bridgeNodes).when(ovsdbControllerUpdateCommand, "getBridgeNodes", any(ReadWriteTransaction.class));

        Whitebox.invokeMethod(ovsdbControllerUpdateCommand, "updateController", transaction, updatedControllerRows);
        PowerMockito.verifyPrivate(ovsdbControllerUpdateCommand).invoke("getBridgeNodes", any(ReadWriteTransaction.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetBridgeNodes() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);

        Map<InstanceIdentifier<Node>, Node> testBridgeNodes = new HashMap<>();
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbControllerUpdateCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        InstanceIdentifier<Node> connectionIId = mock(InstanceIdentifier.class);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(connectionIId);
        PowerMockito.mockStatic(SouthboundUtil.class);
        Optional<Node> ovsdbNode = mock(Optional.class);
        when(SouthboundUtil.readNode(transaction, connectionIId)).thenReturn(ovsdbNode);
        when(ovsdbNode.isPresent()).thenReturn(true);
        Node node = mock(Node.class);
        when(ovsdbNode.get()).thenReturn(node);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);

        List<ManagedNodeEntry> managedNodeEntries = new ArrayList<>();
        ManagedNodeEntry managedNodeEntry = mock(ManagedNodeEntry.class);
        managedNodeEntries.add(managedNodeEntry);
        when(ovsdbNodeAugmentation.getManagedNodeEntry()).thenReturn(managedNodeEntries);
        InstanceIdentifier<Node> bridgeIid = mock(InstanceIdentifier.class);
        OvsdbBridgeRef ovsdbBridgeRef = mock(OvsdbBridgeRef.class);
        when(managedNodeEntry.getBridgeRef()).thenReturn(ovsdbBridgeRef);
        when((InstanceIdentifier<Node>) ovsdbBridgeRef.getValue()).thenReturn(bridgeIid);
        Optional<Node> bridgeNode = mock(Optional.class);
        when(SouthboundUtil.readNode(transaction, bridgeIid)).thenReturn(bridgeNode);
        when(bridgeNode.isPresent()).thenReturn(true);
        when(bridgeNode.get()).thenReturn(node);

        testBridgeNodes.put(bridgeIid, node);

        //verify if getBridgeNodes() returns expected value
        assertEquals(testBridgeNodes, Whitebox.invokeMethod(ovsdbControllerUpdateCommand, "getBridgeNodes", transaction));
    }

    @Test
    public void testGetControllerEntryIid() throws Exception {
        ControllerEntry controllerEntry = mock(ControllerEntry.class);
        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class);
        when(ovsdbControllerUpdateCommand.getOvsdbConnectionInstance()).thenReturn(client);
        NodeKey nodeKey = mock(NodeKey.class);
        when(client.getNodeKey()).thenReturn(nodeKey);
        NodeId nodeId = mock(NodeId.class);
        when(nodeKey.getNodeId()).thenReturn(nodeId);
        when(nodeId.getValue()).thenReturn(NODE_ID);
        PowerMockito.whenNew(Uri.class).withAnyArguments().thenReturn(mock(Uri.class));
        PowerMockito.whenNew(NodeId.class).withAnyArguments().thenReturn(nodeId);
        PowerMockito.whenNew(NodeKey.class).withAnyArguments().thenReturn(nodeKey);
        PowerMockito.whenNew(TopologyKey.class).withAnyArguments().thenReturn(mock(TopologyKey.class));
        //PowerMockito.suppress(MemberMatcher.methodsDeclaredIn(InstanceIdentifier.class));
        when(controllerEntry.getKey()).thenReturn(mock(ControllerEntryKey.class));
        assertEquals(KeyedInstanceIdentifier.class, (Whitebox.invokeMethod(ovsdbControllerUpdateCommand, "getControllerEntryIid", controllerEntry, BRIDGE_NAME).getClass()));
    }
}
