/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
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
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.schema.openvswitch.AutoAttach;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Autoattach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.AutoattachKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({OvsdbAutoAttachRemovedCommand.class, SouthboundMapper.class, SouthboundUtil.class})
@RunWith(PowerMockRunner.class)
public class OvsdbAutoAttachRemovedCommandTest {

    private Map<UUID, AutoAttach> removedAutoAttachRows = new HashMap<>();
    private OvsdbAutoAttachRemovedCommand ovsdbAutoAttachRemovedCommand;
    private ReadWriteTransaction transaction;
    private InstanceIdentifier<Autoattach> aaIid;

    private static final UUID AUTOATTACH_UUID = new UUID("798f35d8-f40a-449a-94d3-c860f5547f9a");
    private static final String CONNECTED_NODE_ID = "10.0.0.1";

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        ovsdbAutoAttachRemovedCommand = mock(OvsdbAutoAttachRemovedCommand.class, Mockito.CALLS_REAL_METHODS);

        AutoAttach autoAttach = mock(AutoAttach.class);
        removedAutoAttachRows.put(AUTOATTACH_UUID, autoAttach);
        MemberModifier.field(OvsdbAutoAttachRemovedCommand.class, "removedAutoAttachRows")
                .set(ovsdbAutoAttachRemovedCommand, removedAutoAttachRows);

        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbAutoAttachRemovedCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        AutoattachKey aaKey = new AutoattachKey(
                new Uri(SouthboundConstants.AUTOATTACH_URI_PREFIX + "://" + AUTOATTACH_UUID.toString()));
        aaIid = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(CONNECTED_NODE_ID)))
                .augmentation(OvsdbNodeAugmentation.class)
                .child(Autoattach.class, aaKey);
        InstanceIdentifier<Node> nodeIid = aaIid.firstIdentifierOf(Node.class);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(nodeIid);

        PowerMockito.mockStatic(SouthboundUtil.class);
        transaction = mock(ReadWriteTransaction.class);
        Node node = mock(Node.class);
        Optional<Node> ovsdbNode = Optional.of(node);
        PowerMockito.when(SouthboundUtil.readNode(transaction, nodeIid)).thenReturn(ovsdbNode);
        NodeId nodeId = mock(NodeId.class);
        when(node.getNodeId()).thenReturn(nodeId);

        PowerMockito.mockStatic(SouthboundMapper.class);
        when(ovsdbConnectionInstance.getNodeId()).thenReturn(nodeId);
        when(SouthboundMapper.createInstanceIdentifier(nodeId)).thenReturn(nodeIid);

        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);
        List<Autoattach> autoAttachList = new ArrayList<>();
        Autoattach aaEntry = mock(Autoattach.class);
        autoAttachList.add(aaEntry);
        when(aaEntry.getAutoattachUuid()).thenReturn(new Uuid(AUTOATTACH_UUID.toString()));
        when(ovsdbNodeAugmentation.getAutoattach()).thenReturn(autoAttachList);
        when(aaEntry.getKey()).thenReturn(aaKey);

        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testExecute() {
        ovsdbAutoAttachRemovedCommand.execute(transaction);
        verify(transaction).delete(eq(LogicalDatastoreType.OPERATIONAL), eq(aaIid));
    }
}
