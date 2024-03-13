/*
 * Copyright (c) 2015 Inocybe Technologies. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.ovsdb.southbound.OvsdbOperGlobalListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class BridgeOperationalStateTest {
    private final Node nd = new NodeBuilder().setNodeId(new NodeId("foo")).build();
    private final InstanceIdentifier<Node> nodeIid = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class).child(Node.class);
    private final Node brNode = new NodeBuilder().setNodeId(new NodeId("bar")).build();

    @Mock
    private BridgeOperationalState briOperationState;
    @Mock
    private DataBroker db;
    @Mock
    private ReadTransaction mockReadTx;
    private InstanceIdentifier<ProtocolEntry> protocolEntry;
    private InstanceIdentifier<Node> iidNode;

    @Before
    public void setUp() throws Exception {
        iidNode = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("foo")))
                .child(Node.class, new NodeKey(nd.getNodeId()));
        protocolEntry = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class)
                .augmentation(OvsdbBridgeAugmentation.class).child(ProtocolEntry.class);
        doReturn(mockReadTx).when(db).newReadOnlyTransaction();
        OvsdbOperGlobalListener.OPER_NODE_CACHE.put(nodeIid, brNode);

        briOperationState = spy(new BridgeOperationalState(db));
    }

    @Test
    public void testGetBridgeNode() {
        assertEquals(Optional.of(brNode), briOperationState.getBridgeNode(nodeIid));
    }

    @Test
    public void testGetOvsdbBridgeAugmentation() throws Exception {
        Optional<OvsdbBridgeAugmentation> optOvsdbBri = briOperationState.getOvsdbBridgeAugmentation(nodeIid);
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertNotNull(optOvsdbBri);
        assertTrue(optOvsdbBri.equals(Optional.empty()));

        Node node = mock(Node.class);
        Optional<Node> optNode = Optional.of(node);
        doReturn(optNode).when(briOperationState).getBridgeNode(any(InstanceIdentifier.class));
        OvsdbBridgeAugmentation ovsdbBriAug = mock(OvsdbBridgeAugmentation.class);
        doReturn(ovsdbBriAug).when(node).augmentation(OvsdbBridgeAugmentation.class);
        Optional<OvsdbBridgeAugmentation> ovsdbBriAugOptional = briOperationState.getOvsdbBridgeAugmentation(
            InstanceIdentifier.create(NetworkTopology.class));
        assertNotNull(ovsdbBriAugOptional);
        assertTrue(ovsdbBriAugOptional.orElseThrow() instanceof OvsdbBridgeAugmentation);
    }

    @Test
    public void testGetBridgeTerminationPoint() throws Exception {
        Optional<TerminationPoint> optTerm = briOperationState.getBridgeTerminationPoint(nodeIid);
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertNotNull(optTerm);
        assertTrue(optTerm.equals(Optional.empty()));

        TerminationPoint termPnt = new TerminationPointBuilder().setTpId(new TpId("mockTp")).build();

        Node node = mock(Node.class);
        Optional<Node> optNode = Optional.of(node);
        doReturn(optNode).when(briOperationState).getBridgeNode(any(InstanceIdentifier.class));
        when(node.nonnullTerminationPoint()).thenCallRealMethod();
        when(node.getTerminationPoint()).thenReturn(Map.of(termPnt.key(), termPnt));

        Optional<TerminationPoint> optTermPnt = briOperationState.getBridgeTerminationPoint(
            iidNode.child(TerminationPoint.class, termPnt.key()));
        assertTrue(optTermPnt.isPresent());
    }

    @Test
    public void testGetOvsdbTerminationPointAugmentation() {
        Optional<OvsdbTerminationPointAugmentation> optOvsdbTermPoint = briOperationState
                .getOvsdbTerminationPointAugmentation(nodeIid);
        assertNotNull(optOvsdbTermPoint);
        verify(briOperationState, times(1)).getBridgeTerminationPoint(any(InstanceIdentifier.class));
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertTrue(optOvsdbTermPoint.equals(Optional.empty()));

        TerminationPoint termPoint = mock(TerminationPoint.class);
        Optional<TerminationPoint> termPntOptional = Optional.of(termPoint);
        doReturn(termPntOptional).when(briOperationState).getBridgeTerminationPoint(any(InstanceIdentifier.class));
        OvsdbTerminationPointAugmentation ovsdbTermPntAug = mock(OvsdbTerminationPointAugmentation.class);
        doReturn(ovsdbTermPntAug).when(termPoint).augmentation(OvsdbTerminationPointAugmentation.class);
        Optional<OvsdbTerminationPointAugmentation> ovsdbTermPointOpt = briOperationState
                .getOvsdbTerminationPointAugmentation(InstanceIdentifier.create(NetworkTopology.class));
        assertNotNull(ovsdbTermPointOpt);
        assertTrue(ovsdbTermPointOpt.orElseThrow() instanceof OvsdbTerminationPointAugmentation);
    }

    @Test
    public void testGetControllerEntry() {
        Optional<ControllerEntry> optController = briOperationState.getControllerEntry(nodeIid);
        verify(briOperationState, times(1)).getOvsdbBridgeAugmentation(any(InstanceIdentifier.class));
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertNotNull(optController);
        assertTrue(optController.equals(Optional.empty()));
    }

    @Test
    public void testGetProtocolEntry() throws Exception {
        Optional<ProtocolEntry> optProtocolEntry = briOperationState.getProtocolEntry(protocolEntry);
        verify(briOperationState, times(1)).getOvsdbBridgeAugmentation(any(InstanceIdentifier.class));
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertNotNull(optProtocolEntry);
        assertTrue(optProtocolEntry.equals(Optional.empty()));
    }
}
