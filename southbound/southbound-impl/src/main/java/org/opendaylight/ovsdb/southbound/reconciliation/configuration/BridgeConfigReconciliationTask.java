/*
 * Copyright (c) 2016 , NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation.configuration;


import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.BridgeOperationalState;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.DataChangesManagedByOvsdbNodeEvent;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactCommandAggregator;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration Reconciliation task to reconcile existing bridge configurations in the config datastore and the
 * switch when the latter is up and connected to the controller.
 * Created by Vinh Nguyen (vinh.nguyen@hcl.com) on 3/21/16.
 */
public class BridgeConfigReconciliationTask extends ReconciliationTask {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeConfigReconciliationTask.class);
    private final OvsdbConnectionInstance connectionInstance;
    private final InstanceIdentifierCodec instanceIdentifierCodec;

    public BridgeConfigReconciliationTask(ReconciliationManager reconciliationManager, OvsdbConnectionManager
            connectionManager, InstanceIdentifier<?> nodeIid, OvsdbConnectionInstance connectionInstance,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        super(reconciliationManager, connectionManager, nodeIid, null);
        this.connectionInstance = connectionInstance;
        this.instanceIdentifierCodec = instanceIdentifierCodec;
    }

    @Override
    public boolean reconcileConfiguration(final OvsdbConnectionManager connectionManagerOfDevice) {
        InstanceIdentifier<Topology> topologyInstanceIdentifier = SouthboundMapper.createTopologyInstanceIdentifier();
        ReadOnlyTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction();

        // find all bridges of the specific device in the config data store
        // TODO: this query is not efficient. It retrieves all the Nodes in the datastore, loop over them and look for
        // the bridges of specific device. It is mre efficient if MDSAL allows query nodes using wildcard on node id
        // (ie: ovsdb://uuid/<device uuid>/bridge/*) r attributes
        CheckedFuture<Optional<Topology>, ReadFailedException> readTopologyFuture =
                tx.read(CONFIGURATION, topologyInstanceIdentifier);
        Futures.addCallback(readTopologyFuture, new FutureCallback<Optional<Topology>>() {
            @Override
            public void onSuccess(@Nullable Optional<Topology> optionalTopology) {
                if (optionalTopology != null && optionalTopology.isPresent()) {
                    @SuppressWarnings("unchecked")
                    InstanceIdentifier<Node> ndIid = (InstanceIdentifier<Node>) nodeIid;
                    Topology topology = optionalTopology.get();
                    if (topology.getNode() != null) {
                        final Map<InstanceIdentifier<?>, DataObject> brChanges = new HashMap<>();
                        final List<Node> tpChanges = new ArrayList<>();
                        for (Node node : topology.getNode()) {
                            LOG.debug("Reconcile Configuration for node {}", node.getNodeId());
                            OvsdbBridgeAugmentation bridge = node.getAugmentation(OvsdbBridgeAugmentation.class);
                            if (bridge != null && bridge.getManagedBy() != null
                                    && bridge.getManagedBy().getValue().equals(ndIid)) {
                                brChanges.putAll(extractBridgeConfigurationChanges(node, bridge));
                                tpChanges.add(node);
                            } else if (node.getKey().getNodeId().getValue().startsWith(
                                    nodeIid.firstKeyOf(Node.class).getNodeId().getValue())
                                    && node.getTerminationPoint() != null && !node.getTerminationPoint().isEmpty() ) {
                                tpChanges.add(node);
                            }
                        }
                        if (!brChanges.isEmpty()) {
                            reconcileBridgeConfigurations(brChanges);
                        }
                        if (!tpChanges.isEmpty()) {
                            reconciliationManager.reconcileTerminationPoints(
                                    connectionManagerOfDevice, connectionInstance, tpChanges);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.warn("Read Config/DS for Topology failed! {}", nodeIid, throwable);
            }

        });

        return true;
    }

    private Map<InstanceIdentifier<?>, DataObject> extractBridgeConfigurationChanges(
            final Node bridgeNode, final OvsdbBridgeAugmentation ovsdbBridge) {
        Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        final InstanceIdentifier<Node> bridgeNodeIid =
                SouthboundMapper.createInstanceIdentifier(bridgeNode.getNodeId());
        final InstanceIdentifier<OvsdbBridgeAugmentation> ovsdbBridgeIid =
                bridgeNodeIid.builder().augmentation(OvsdbBridgeAugmentation.class).build();
        changes.put(bridgeNodeIid, bridgeNode);
        changes.put(ovsdbBridgeIid, ovsdbBridge);

        if (ovsdbBridge.getProtocolEntry() != null) {
            for (ProtocolEntry protocol : ovsdbBridge.getProtocolEntry()) {
                if (SouthboundConstants.OVSDB_PROTOCOL_MAP.get(protocol.getProtocol()) != null) {
                    KeyedInstanceIdentifier<ProtocolEntry, ProtocolEntryKey> protocolIid =
                            ovsdbBridgeIid.child(ProtocolEntry.class, protocol.getKey());
                    changes.put(protocolIid, protocol);
                } else {
                    throw new IllegalArgumentException("Unknown protocol " + protocol.getProtocol());
                }
            }
        }

        if (ovsdbBridge.getControllerEntry() != null) {
            for (ControllerEntry controller : ovsdbBridge.getControllerEntry()) {
                KeyedInstanceIdentifier<ControllerEntry, ControllerEntryKey> controllerIid =
                        ovsdbBridgeIid.child(ControllerEntry.class, controller.getKey());
                changes.put(controllerIid, controller);
            }
        }

        return changes;
    }

    private void reconcileBridgeConfigurations(final Map<InstanceIdentifier<?>, DataObject> changes) {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvents = new AsyncDataChangeEvent() {
            @Override
            public Map<InstanceIdentifier<?>, DataObject> getCreatedData() {
                return changes;
            }

            @Override
            public Map<InstanceIdentifier<?>, DataObject> getUpdatedData() {
                return Collections.emptyMap();
            }

            @Override
            public Map<InstanceIdentifier<?>, DataObject> getOriginalData() {
                return Collections.emptyMap();
            }

            @Override
            public Set<InstanceIdentifier<?>> getRemovedPaths() {
                return Collections.emptySet();
            }

            @Override
            public DataObject getOriginalSubtree() {
                return null;
            }

            @Override
            public DataObject getUpdatedSubtree() {
                return null;
            }
        };

        connectionInstance.transact(new TransactCommandAggregator(),
                new BridgeOperationalState(reconciliationManager.getDb(), changeEvents),
                new DataChangesManagedByOvsdbNodeEvent(
                        reconciliationManager.getDb(),
                        connectionInstance.getInstanceIdentifier(),
                        changeEvents), instanceIdentifierCodec);
    }

    @Override
    public void doRetry(boolean wasPreviousAttemptSuccessful) {
    }

    @Override
    public void checkReadinessAndProcess() {
    }

    @Override
    public long retryDelayInMills() {
        return 0;
    }
}
