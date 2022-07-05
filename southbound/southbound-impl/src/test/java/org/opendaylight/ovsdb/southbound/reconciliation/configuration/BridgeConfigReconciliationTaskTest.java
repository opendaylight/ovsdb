/*
 * Copyright © 2016, 2017 NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation.configuration;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow10;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class BridgeConfigReconciliationTaskTest {
    private static final String BR_INT = "br-int";
    private static final String NODE_ID = "ovsdb://uuid/6ff3d0cf-4102-429d-b41c-f8027a0fd7f4";
    private BridgeConfigReconciliationTask configurationReconciliationTask;
    @Mock private OvsdbConnectionManager ovsdbConnectionManager;
    @Mock private OvsdbConnectionInstance ovsdbConnectionInstance;
    @Mock private DataBroker db;
    @Mock private ReconciliationManager reconciliationManager;
    @Mock private Topology topology;
    @Mock private InstanceIdentifier<Node> iid;
    @Mock private SouthboundProvider provider;

    @Before
    public void setUp() throws Exception {
        NodeKey nodeKey = new NodeKey(new NodeId(new Uri(NODE_ID)));

        iid = SouthboundMapper.createInstanceIdentifier(nodeKey.getNodeId());
        SouthboundProvider.setBridgesReconciliationInclusionList(Arrays.asList(BR_INT));
        Node brIntNode = createBridgeNode(NODE_ID + "/bridge/" + BR_INT);
        Optional<Node> nodeOptional = Optional.of(brIntNode);
        FluentFuture<Optional<Node>> readNodeFuture =
                FluentFutures.immediateFluentFuture(nodeOptional);
        when(reconciliationManager.getDb()).thenReturn(db);
        ReadTransaction tx = mock(ReadTransaction.class);
        Mockito.when(db.newReadOnlyTransaction()).thenReturn(tx);
        Mockito.when(tx.read(any(LogicalDatastoreType.class),any(InstanceIdentifier.class)))
                .thenReturn(readNodeFuture);

        when(topology.getNode()).thenReturn(Map.of(brIntNode.key(), brIntNode));

        configurationReconciliationTask =
                new BridgeConfigReconciliationTask(reconciliationManager, ovsdbConnectionManager, iid,
                        ovsdbConnectionInstance, mock(InstanceIdentifierCodec.class));
    }

    @Test
    public void testReconcileConfiguration() throws Exception {
        BridgeConfigReconciliationTask underTest = spy(configurationReconciliationTask);
        doNothing().when(underTest).reconcileBridgeConfigurations(any(Map.class));
        assertTrue(underTest.reconcileConfiguration(ovsdbConnectionManager));
        Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        for (Node bridgeNode : topology.getNode().values()) {
            changes.putAll(createExpectedConfigurationChanges(bridgeNode));
        }
        verify(underTest).reconcileBridgeConfigurations(changes);
    }

    private Node createBridgeNode(final String bridgeName) {
        ProtocolEntry protocolEntry = new ProtocolEntryBuilder()
                .setProtocol(OvsdbBridgeProtocolOpenflow10.VALUE)
                .build();
        ControllerEntry controllerEntry = new ControllerEntryBuilder().setTarget(new Uri("mock")).build();

        return new NodeBuilder()
                .setNodeId(new NodeId(new Uri(bridgeName)))
                .addAugmentation(new OvsdbBridgeAugmentationBuilder()
                    .setManagedBy(new OvsdbNodeRef(iid))
                    .setProtocolEntry(Collections.singletonMap(protocolEntry.key(), protocolEntry))
                    .setControllerEntry(Collections.singletonMap(controllerEntry.key(), controllerEntry))
                    .build())
                .build();
    }

    private static Map<InstanceIdentifier<?>, DataObject> createExpectedConfigurationChanges(final Node bridgeNode) {
        OvsdbBridgeAugmentation ovsdbBridge = bridgeNode.augmentation(OvsdbBridgeAugmentation.class);

        Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        final InstanceIdentifier<Node> bridgeNodeIid =
                SouthboundMapper.createInstanceIdentifier(bridgeNode.getNodeId());
        final InstanceIdentifier<OvsdbBridgeAugmentation> ovsdbBridgeIid =
                bridgeNodeIid.builder().augmentation(OvsdbBridgeAugmentation.class).build();
        changes.put(bridgeNodeIid, bridgeNode);
        changes.put(ovsdbBridgeIid, ovsdbBridge);
        for (ProtocolEntry protocolEntry : ovsdbBridge.getProtocolEntry().values()) {
            KeyedInstanceIdentifier<ProtocolEntry, ProtocolEntryKey> protocolIid =
                    ovsdbBridgeIid.child(ProtocolEntry.class, protocolEntry.key());
            changes.put(protocolIid, protocolEntry);
        }
        for (ControllerEntry controller : ovsdbBridge.getControllerEntry().values()) {
            KeyedInstanceIdentifier<ControllerEntry, ControllerEntryKey> controllerIid =
                    ovsdbBridgeIid.child(ControllerEntry.class, controller.key());
            changes.put(controllerIid, controller);
        }
        return changes;
    }
}
