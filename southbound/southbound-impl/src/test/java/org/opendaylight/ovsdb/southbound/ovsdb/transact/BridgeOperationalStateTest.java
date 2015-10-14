/*
 * Copyright (c) 2015 Inocybe Technologies. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.google.common.base.Optional;

@PrepareForTest({BridgeOperationalState.class, Optional.class, InstanceIdentifier.class})
@RunWith(PowerMockRunner.class)
public class BridgeOperationalStateTest {

    @Mock private BridgeOperationalState briOperationState;
    @Mock private InstanceIdentifier<ProtocolEntry> protocolEntry;
    @Mock private InstanceIdentifier<?> iid;

    @Before
    public void setUp() throws Exception {
        briOperationState = mock(BridgeOperationalState.class, Mockito.RETURNS_MOCKS);
        iid = mock(InstanceIdentifier.class, Mockito.RETURNS_MOCKS);
        protocolEntry = mock(InstanceIdentifier.class, Mockito.RETURNS_MOCKS);
    }

    @Test
    public void testGetBridgeNode() {
        Optional<Node> optNodes = briOperationState.getBridgeNode(iid);
        assertNotNull(optNodes);
        assertTrue(optNodes instanceof Optional);
    }

    @Test
    public void testGetOvsdbBridgeAugmentation() {
        Optional<OvsdbBridgeAugmentation> optOvsdbBri = briOperationState.getOvsdbBridgeAugmentation(iid);
        assertNotNull(optOvsdbBri);
        assertTrue(optOvsdbBri.equals(Optional.absent()) || optOvsdbBri instanceof Optional);
    }

    @Test
    public void testGetBridgeTerminationPoint() {
        Optional<TerminationPoint> optTerm = briOperationState.getBridgeTerminationPoint(iid);
        assertNotNull(optTerm);
        assertTrue(optTerm.equals(Optional.absent()) || optTerm instanceof Optional);
    }

    @Test
    public void testGetControllerEntry() {
        Optional<ControllerEntry> optController= briOperationState.getControllerEntry(iid);
        assertNotNull(optController);
        assertTrue(optController.equals(Optional.absent()) || optController instanceof Optional);
    }

    @Test
    public void testGetOvsdbTerminationPointAugmentation() {
        Optional<OvsdbTerminationPointAugmentation> optOvsdbTermPoint = briOperationState.getOvsdbTerminationPointAugmentation(iid);
        assertNotNull(optOvsdbTermPoint);
        assertTrue(optOvsdbTermPoint.equals(Optional.absent()) || optOvsdbTermPoint instanceof Optional);
    }

    @Test
    public void testGetProtocolEntry() {
        Optional<ProtocolEntry> optProtocolEntry = briOperationState.getProtocolEntry(protocolEntry);
        assertNotNull(optProtocolEntry);
        assertTrue(optProtocolEntry.equals(Optional.absent()) || optProtocolEntry instanceof Optional);
    }

}
