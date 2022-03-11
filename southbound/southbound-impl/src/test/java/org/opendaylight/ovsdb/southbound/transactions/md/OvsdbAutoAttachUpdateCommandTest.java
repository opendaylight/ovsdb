/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.AutoAttach;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
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

@PrepareForTest({OvsdbAutoAttachUpdateCommand.class, SouthboundUtil.class})
@RunWith(PowerMockRunner.class)
public class OvsdbAutoAttachUpdateCommandTest {

    private static final String AUTOATTACH_SYSTEM_INFO = "AutoAttach Test";
    private static final Map<Long, Long> AUTOATTACH_MAPPINGS = ImmutableMap.of(100L, 200L);
    private static final UUID AUTOATTACH_UUID = new UUID("798f35d8-f40a-449a-94d3-c860f5547f9a");
    private static final String CONNECTED_NODE_ID = "10.0.0.2";
//    private static final Map<String, String> AUTOATTACH_EXTERNAL_IDS =
//            ImmutableMap.of("opendaylight-autoattach-id", "autoattach://798f35d8-f40a-449a-94d3-c860f5547f9a");
    private Map<UUID, AutoAttach> updatedAutoAttachRows = new HashMap<>();
    private Map<UUID, AutoAttach> oldAutoAttachRows = new HashMap<>();
    private OvsdbAutoAttachUpdateCommand ovsdbAutoAttachUpdateCommand;
    private ReadWriteTransaction transaction;
    private InstanceIdentifier<Autoattach> aaIid;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        ovsdbAutoAttachUpdateCommand = mock(OvsdbAutoAttachUpdateCommand.class, Mockito.CALLS_REAL_METHODS);

        AutoAttach autoAttach = mock(AutoAttach.class);
        updatedAutoAttachRows.put(AUTOATTACH_UUID, autoAttach);
        oldAutoAttachRows.put(AUTOATTACH_UUID, autoAttach);

        Column<GenericTableSchema, String> aaColumn = mock(Column.class);
        when(autoAttach.getSystemNameColumn()).thenReturn(aaColumn);
        when(autoAttach.getSystemDescriptionColumn()).thenReturn(aaColumn);
        when(aaColumn.getData()).thenReturn(AUTOATTACH_SYSTEM_INFO);
        Column<GenericTableSchema, Map<Long, Long>> aaMappingColumn = mock(Column.class);
        when(autoAttach.getMappingsColumn()).thenReturn(aaMappingColumn);
        when(aaMappingColumn.getData()).thenReturn(AUTOATTACH_MAPPINGS);

        // FIXME: To be uncommented when Open vSwitch supports external_ids column
//        Column<GenericTableSchema, Map<String, String>> aaExternalIdsColumn = mock(Column.class);
//        when(autoAttach.getExternalIdsColumn()).thenReturn(aaExternalIdsColumn);
//        when(aaExternalIdsColumn.getData()).thenReturn(AUTOATTACH_EXTERNAL_IDS);
        when(autoAttach.getUuid()).thenReturn(AUTOATTACH_UUID);

        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbAutoAttachUpdateCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        aaIid = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(CONNECTED_NODE_ID))).augmentation(OvsdbNodeAugmentation.class)
                .child(Autoattach.class, new AutoattachKey(
                        new Uri(SouthboundConstants.AUTOATTACH_URI_PREFIX + "://" + AUTOATTACH_UUID.toString())));
        InstanceIdentifier<Node> connectionIid = aaIid.firstIdentifierOf(Node.class);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(connectionIid);
        transaction = mock(ReadWriteTransaction.class);
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(Autoattach.class));
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));

        PowerMockito.mockStatic(SouthboundUtil.class);
        Node node = mock(Node.class);
        Optional<Node> ovsdbNode = Optional.of(node);
        PowerMockito.when(SouthboundUtil.readNode(transaction, connectionIid)).thenReturn(ovsdbNode);
        NodeId nodeId = mock(NodeId.class);
        when(node.getNodeId()).thenReturn(nodeId);

        MemberModifier.field(OvsdbAutoAttachUpdateCommand.class, "updatedAutoAttachRows")
                .set(ovsdbAutoAttachUpdateCommand, updatedAutoAttachRows);
        MemberModifier.field(OvsdbAutoAttachUpdateCommand.class, "oldAutoAttachRows").set(ovsdbAutoAttachUpdateCommand,
                oldAutoAttachRows);
    }

    @Test
    public void testExecute() {
        ovsdbAutoAttachUpdateCommand.execute(transaction);
        verify(transaction).put(eq(LogicalDatastoreType.OPERATIONAL), eq(aaIid), any(Autoattach.class));
    }
}
