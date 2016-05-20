/*
 * Copyright (c) 2016 , NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation.configuration;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow10;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Vinh Nguyen (vinh.nguyen@hcl.com) on 3/21/16
 */
@PrepareForTest({BridgeConfigReconciliationTask.class, OvsdbConnectionManager.class, OvsdbConnectionInstance.class, InstanceIdentifier.class, Optional.class})
@RunWith(PowerMockRunner.class)
public class BridgeConfigReconciliationTaskTest {
    private final static String NODE_ID = "ovsdb://uuid/6ff3d0cf-4102-429d-b41c-f8027a0fd7f4";
    private final static String BR01 = "br01";
    private final static String BR02 = "br02";
//    @Mock
    private BridgeConfigReconciliationTask configurationReconciliationTask;
    @Mock private OvsdbConnectionManager ovsdbConnectionManager;
    @Mock private OvsdbConnectionInstance ovsdbConnectionInstance;
    @Mock private DataBroker db;
    @Mock private ReconciliationManager reconciliationManager;
    @Mock private Topology topology;
    @Mock private InstanceIdentifier<Node> iid;

    @Before
    public void setUp() throws Exception {
        NodeKey nodeKey = new NodeKey(new NodeId(new Uri(NODE_ID)));
        List<Node> bridgeNodes = new ArrayList<>();
        bridgeNodes.add(createBridgeNode(BR01));
        bridgeNodes.add(createBridgeNode(BR02));
        when(topology.getNode()).thenReturn(bridgeNodes);

        Optional<Topology> topologyOptional = mock(Optional.class);
        when(topologyOptional.isPresent()).thenReturn(true);
        when(topologyOptional.get()).thenReturn(topology);
        CheckedFuture<Optional<Topology>, ReadFailedException> readTopologyFuture = Futures.immediateCheckedFuture(topologyOptional);

        when(reconciliationManager.getDb()).thenReturn(db);
        ReadOnlyTransaction tx = mock(ReadOnlyTransaction.class);
        Mockito.when(db.newReadOnlyTransaction()).thenReturn(tx);
        Mockito.when(tx.read(any(LogicalDatastoreType.class),any(InstanceIdentifier.class)))
                .thenReturn(readTopologyFuture);

        when(topology.getNode()).thenReturn(bridgeNodes);
        when(ovsdbConnectionInstance.getNodeKey()).thenReturn(nodeKey);

        configurationReconciliationTask = new BridgeConfigReconciliationTask(
                reconciliationManager, ovsdbConnectionManager, iid, ovsdbConnectionInstance);
    }

    @Test
    public void testReconcileConfiguration() throws Exception {
        BridgeConfigReconciliationTask underTest = PowerMockito.spy(configurationReconciliationTask);
        PowerMockito.doNothing().when(underTest, "reconcileBridgeConfigurations", any(Map.class));
        assertEquals(true, underTest.reconcileConfiguration(ovsdbConnectionManager));
        Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        for (Node bridgeNode : topology.getNode()) {
            changes.putAll(createExpectedConfigurationChanges(bridgeNode));
        }
        PowerMockito.verifyPrivate(underTest).invoke("reconcileBridgeConfigurations", changes);
    }

    private Node createBridgeNode(final String bridgeName) {
        Node bridgeNode = mock(Node.class);
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = mock(OvsdbBridgeAugmentation.class);
        OvsdbNodeRef ovsdbNodeRef = mock(OvsdbNodeRef.class);

        when((InstanceIdentifier<Node>)ovsdbNodeRef.getValue()).thenReturn(iid);
        OvsdbBridgeName ovsdbBridgeName = new OvsdbBridgeName(bridgeName);
        when(bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class)).thenReturn(ovsdbBridgeAugmentation);
        when(ovsdbBridgeAugmentation.getBridgeName()).thenReturn(ovsdbBridgeName);
        ProtocolEntry protocolEntry = mock(ProtocolEntry.class);
        ProtocolEntryKey protocolEntryKey = mock(ProtocolEntryKey.class);
        Mockito.when(protocolEntry.getProtocol()).thenAnswer(new Answer<Class<? extends OvsdbBridgeProtocolBase>>() {
            public Class<? extends OvsdbBridgeProtocolBase> answer(
                    InvocationOnMock invocation) throws Throwable {
                return OvsdbBridgeProtocolOpenflow10.class;
            }
        });
        when(protocolEntry.getKey()).thenReturn(protocolEntryKey);
        when(ovsdbBridgeAugmentation.getProtocolEntry()).thenReturn(Arrays.asList(protocolEntry));

        ControllerEntry controllerEntry = mock(ControllerEntry.class);
        ControllerEntryKey controllerEntryKey = mock(ControllerEntryKey.class);
        when(controllerEntry.getKey()).thenReturn(controllerEntryKey);
        when(ovsdbBridgeAugmentation.getControllerEntry()).thenReturn(Arrays.asList(controllerEntry));

        when(ovsdbBridgeAugmentation.getManagedBy()).thenReturn(ovsdbNodeRef);

        return bridgeNode;
    }

    private Map<InstanceIdentifier<?>, DataObject> createExpectedConfigurationChanges(final Node bridgeNode) {
        OvsdbBridgeAugmentation ovsdbBridge = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);

        Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        final InstanceIdentifier<Node> bridgeNodeIid =
                SouthboundMapper.createInstanceIdentifier(ovsdbConnectionInstance, ovsdbBridge.getBridgeName().getValue());
        final InstanceIdentifier<OvsdbBridgeAugmentation> ovsdbBridgeIid =
                bridgeNodeIid.builder().augmentation(OvsdbBridgeAugmentation.class).build();
        changes.put(bridgeNodeIid, bridgeNode);
        changes.put(ovsdbBridgeIid, ovsdbBridge);
        for (ProtocolEntry protocolEntry : ovsdbBridge.getProtocolEntry()) {
            KeyedInstanceIdentifier<ProtocolEntry, ProtocolEntryKey> protocolIid =
                    ovsdbBridgeIid.child(ProtocolEntry.class, protocolEntry.getKey());
            changes.put(protocolIid, protocolEntry);
        }
        for (ControllerEntry controller : ovsdbBridge.getControllerEntry()) {
            KeyedInstanceIdentifier<ControllerEntry, ControllerEntryKey> controllerIid =
                    ovsdbBridgeIid.child(ControllerEntry.class, controller.getKey());
            changes.put(controllerIid, controller);
        }
        return changes;
    }
}
