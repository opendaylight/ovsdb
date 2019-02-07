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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.getField;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class BridgeOperationalStateTest {
    private final Node nd = new NodeBuilder().setNodeId(new NodeId("foo")).build();
    private final InstanceIdentifier<?> iid = InstanceIdentifier.create(Topology.class);

    @Mock private BridgeOperationalState briOperationState;
    private InstanceIdentifier<ProtocolEntry> protocolEntry;
    private InstanceIdentifier<Node> iidNode;
    private Map<InstanceIdentifier<Node>, Node> operationalNodes;

    @Before
    public void setUp() throws Exception {
        iidNode = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("foo")))
                .child(Node.class, new NodeKey(nd.getNodeId()));
        protocolEntry = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class).child(Node.class)
                .augmentation(OvsdbBridgeAugmentation.class).child(ProtocolEntry.class);

        briOperationState = mock(BridgeOperationalState.class, Mockito.CALLS_REAL_METHODS);

        operationalNodes = new HashMap<>();
        operationalNodes.put(iidNode, nd);
        getField(BridgeOperationalState.class,"operationalNodes").set(briOperationState, operationalNodes);
    }

    @Test
    public void testGetBridgeNode() {
        Optional<Node> optNodes = briOperationState.getBridgeNode(iid);
        assertEquals(Optional.absent(), optNodes);
    }

    @Test
    public void testGetOvsdbBridgeAugmentation() throws Exception {
        Optional<OvsdbBridgeAugmentation> optOvsdbBri = briOperationState.getOvsdbBridgeAugmentation(iid);
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertNotNull(optOvsdbBri);
        assertTrue(optOvsdbBri.equals(Optional.absent()));

        Node node = mock(Node.class);
        Optional<Node> optNode = Optional.of(node);
        doReturn(optNode).when(briOperationState).getBridgeNode(any(InstanceIdentifier.class));
        OvsdbBridgeAugmentation ovsdbBriAug = mock(OvsdbBridgeAugmentation.class);
        doReturn(ovsdbBriAug).when(node).augmentation(OvsdbBridgeAugmentation.class);
        Optional<OvsdbBridgeAugmentation> ovsdbBriAugOptional = briOperationState.getOvsdbBridgeAugmentation(iid);
        assertNotNull(ovsdbBriAugOptional);
        assertTrue(ovsdbBriAugOptional.get() instanceof OvsdbBridgeAugmentation);
    }

    @Test
    public void testGetBridgeTerminationPoint() throws Exception {
        Optional<TerminationPoint> optTerm = briOperationState.getBridgeTerminationPoint(iid);
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertNotNull(optTerm);
        assertTrue(optTerm.equals(Optional.absent()));

        TerminationPoint termPnt = mock(TerminationPoint.class);
        List<TerminationPoint> termPntList = new ArrayList<>();
        termPntList.add(termPnt);

        Node node = mock(Node.class);
        Optional<Node> optNode = Optional.of(node);
        doReturn(optNode).when(briOperationState).getBridgeNode(any(InstanceIdentifier.class));
        when(node.getTerminationPoint()).thenReturn(termPntList);
        TerminationPointKey termPntKey = mock(TerminationPointKey.class);
        when(termPnt.key()).thenReturn(termPntKey);

        Optional<TerminationPoint> optTermPnt = briOperationState.getBridgeTerminationPoint(
            iidNode.child(TerminationPoint.class, termPntKey));
        assertTrue(optTermPnt.isPresent());
    }

    @Test
    public void testGetOvsdbTerminationPointAugmentation() {
        Optional<OvsdbTerminationPointAugmentation> optOvsdbTermPoint = briOperationState
                .getOvsdbTerminationPointAugmentation(iid);
        assertNotNull(optOvsdbTermPoint);
        verify(briOperationState, times(1)).getBridgeTerminationPoint(any(InstanceIdentifier.class));
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertTrue(optOvsdbTermPoint.equals(Optional.absent()));

        TerminationPoint termPoint = mock(TerminationPoint.class);
        Optional<TerminationPoint> termPntOptional = Optional.of(termPoint);
        doReturn(termPntOptional).when(briOperationState).getBridgeTerminationPoint(any(InstanceIdentifier.class));
        OvsdbTerminationPointAugmentation ovsdbTermPntAug = mock(OvsdbTerminationPointAugmentation.class);
        doReturn(ovsdbTermPntAug).when(termPoint).augmentation(OvsdbTerminationPointAugmentation.class);
        Optional<OvsdbTerminationPointAugmentation> ovsdbTermPointOpt = briOperationState
                .getOvsdbTerminationPointAugmentation(iid);
        assertNotNull(ovsdbTermPointOpt);
        assertTrue(ovsdbTermPointOpt.get() instanceof OvsdbTerminationPointAugmentation);
    }

    @Test
    public void testGetControllerEntry() {
        Optional<ControllerEntry> optController = briOperationState.getControllerEntry(iid);
        verify(briOperationState, times(1)).getOvsdbBridgeAugmentation(any(InstanceIdentifier.class));
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertNotNull(optController);
        assertTrue(optController.equals(Optional.absent()));
    }

    @Test
    public void testGetProtocolEntry() throws Exception {
        Optional<ProtocolEntry> optProtocolEntry = briOperationState.getProtocolEntry(protocolEntry);
        verify(briOperationState, times(1)).getOvsdbBridgeAugmentation(any(InstanceIdentifier.class));
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertNotNull(optProtocolEntry);
        assertTrue(optProtocolEntry.equals(Optional.absent()));
    }
}
