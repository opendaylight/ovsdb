/*
 * Copyright (c) 2015 Inocybe Technologies. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BridgeOperationalState.class, Optional.class, InstanceIdentifier.class, Node.class,
    OvsdbBridgeAugmentation.class })
public class BridgeOperationalStateTest {

    @Mock private BridgeOperationalState briOperationState;
    @Mock private InstanceIdentifier<ProtocolEntry> protocolEntry;
    @Mock private InstanceIdentifier<?> iid;
    @Mock private InstanceIdentifier<Node> iidNode;
    @Mock private Node nd;
    private Map<InstanceIdentifier<Node>, Node> operationalNodes;

    @Before
    public void setUp() throws Exception {
        briOperationState = mock(BridgeOperationalState.class, Mockito.CALLS_REAL_METHODS);
        iid = mock(InstanceIdentifier.class, Mockito.RETURNS_MOCKS);
        protocolEntry = mock(InstanceIdentifier.class, Mockito.RETURNS_MOCKS);
        iidNode = mock(InstanceIdentifier.class, Mockito.RETURNS_MOCKS);
        nd = mock(Node.class, Mockito.RETURNS_MOCKS);

        operationalNodes = new HashMap<>();
        operationalNodes.put(iidNode, nd);
        MemberModifier.field(BridgeOperationalState.class,"operationalNodes").set(briOperationState, operationalNodes);
        PowerMockito.suppress(MemberMatcher.methodsDeclaredIn(InstanceIdentifier.class));
    }

    @Test
    public void testGetBridgeNode() throws Exception {
        Optional<Node> optNodes = briOperationState.getBridgeNode(iid);
        verify(iid, times(1)).firstIdentifierOf(Node.class);
        assertNotNull(optNodes);
        assertTrue(optNodes.equals(Optional.absent()));
    }

    @Test
    public void testGetOvsdbBridgeAugmentation() throws Exception {
        Optional<OvsdbBridgeAugmentation> optOvsdbBri = briOperationState.getOvsdbBridgeAugmentation(iid);
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertNotNull(optOvsdbBri);
        assertTrue(optOvsdbBri.equals(Optional.absent()));

        suppress(MemberMatcher.method(BridgeOperationalState.class, "getBridgeNode", InstanceIdentifier.class));
        Node node = mock(Node.class);
        Optional<Node> optNode = Optional.of(node);
        when(briOperationState.getBridgeNode(any(InstanceIdentifier.class))).thenReturn(optNode);
        OvsdbBridgeAugmentation ovsdbBriAug = mock(OvsdbBridgeAugmentation.class);
        when(node.getAugmentation(OvsdbBridgeAugmentation.class)).thenReturn(ovsdbBriAug);
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

        suppress(MemberMatcher.method(BridgeOperationalState.class, "getBridgeNode", InstanceIdentifier.class));
        Node node = mock(Node.class);
        Optional<Node> optNode = Optional.of(node);
        when(briOperationState.getBridgeNode(any(InstanceIdentifier.class))).thenReturn(optNode);
        when(node.getTerminationPoint()).thenReturn(termPntList);
        TerminationPointKey termPntKey = mock(TerminationPointKey.class);
        when(termPnt.getKey()).thenReturn(termPntKey);

        final InstanceIdentifier<?> iid2 = PowerMockito.mock(InstanceIdentifier.class);
        //PowerMockito.suppress(MemberMatcher.method(InstanceIdentifier.class, "firstKeyOf", Class.class, Class.class));
        //PowerMockito.when(iid2.firstKeyOf(TerminationPoint.class, TerminationPointKey.class)).thenReturn(termPntKey);
        Optional<TerminationPoint> optTermPnt = briOperationState.getBridgeTerminationPoint(iid2);
        assertNotNull(optTermPnt);
        //assertTrue(optTermPnt.get() instanceof TerminationPoint);
    }

    @Test
    public void testGetOvsdbTerminationPointAugmentation() {
        Optional<OvsdbTerminationPointAugmentation> optOvsdbTermPoint = briOperationState
                .getOvsdbTerminationPointAugmentation(iid);
        assertNotNull(optOvsdbTermPoint);
        verify(briOperationState, times(1)).getBridgeTerminationPoint(any(InstanceIdentifier.class));
        verify(briOperationState, times(1)).getBridgeNode(any(InstanceIdentifier.class));
        assertTrue(optOvsdbTermPoint.equals(Optional.absent()));

        PowerMockito.suppress(MemberMatcher.method(BridgeOperationalState.class, "getBridgeTerminationPoint",
                InstanceIdentifier.class));
        TerminationPoint termPoint = mock(TerminationPoint.class);
        Optional<TerminationPoint> termPntOptional = Optional.of(termPoint);
        when(briOperationState.getBridgeTerminationPoint(any(InstanceIdentifier.class))).thenReturn(termPntOptional);
        OvsdbTerminationPointAugmentation ovsdbTermPntAug = mock(OvsdbTerminationPointAugmentation.class);
        when(termPoint.getAugmentation(OvsdbTerminationPointAugmentation.class)).thenReturn(ovsdbTermPntAug);
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
