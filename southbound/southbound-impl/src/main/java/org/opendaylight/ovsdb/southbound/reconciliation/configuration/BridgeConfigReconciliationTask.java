/*
 * Copyright (c) 2016, 2017 NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation.configuration;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.BridgeOperationalState;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.DataChangeEvent;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.DataChangesManagedByOvsdbNodeEvent;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactCommandAggregator;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
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

    public BridgeConfigReconciliationTask(ReconciliationManager reconciliationManager,
            OvsdbConnectionManager connectionManager, InstanceIdentifier<?> nodeIid,
            OvsdbConnectionInstance connectionInstance, InstanceIdentifierCodec instanceIdentifierCodec) {
        super(reconciliationManager, connectionManager, nodeIid, null);
        this.connectionInstance = connectionInstance;
        this.instanceIdentifierCodec = instanceIdentifierCodec;
    }


    @Override
    public boolean reconcileConfiguration(final OvsdbConnectionManager connectionManagerOfDevice) {

        String nodeIdVal = nodeIid.firstKeyOf(Node.class).getNodeId().getValue();
        List<String> bridgeReconcileIncludeList = getNodeIdForBridges(nodeIdVal,
            SouthboundProvider.getBridgesReconciliationInclusionList());
        List<String> bridgeReconcileExcludeList = getNodeIdForBridges(nodeIdVal,
            SouthboundProvider.getBridgesReconciliationExclusionList());

        LOG.trace("bridgeReconcileIncludeList : {}", bridgeReconcileIncludeList);
        LOG.trace("bridgeReconcileExcludeList : {}", bridgeReconcileExcludeList);
        // (1) Both "bridge-reconciliation-inclusion-list" and "bridge-reconciliation-exclusion-list" are empty.
        // it means it will keep the default behavior of reconciling on all bridges.
        // (2) Only "bridge-reconciliation-inclusion-list" has list of bridge.
        // than plugin will only reconcile specified bridges.
        // (3) Only "bridge-reconciliation-exclusion-list" has list of bridge.
        // than plugin will reconcile all the bridge, except excluding the specified bridges.
        // (4) Both bridge-reconciliation-inclusion-list and bridge-reconciliation-exclusion-list has bridges specified.
        // this is invalid scenario, so it should log the warning saying this is not valid configuration,
        // but plugin will give priority to "bridge-reconciliation-exclusion-list" and reconcile all the bridges
        // except the one specified in the exclusion-list.

        Boolean reconcileAllBridges = Boolean.FALSE;
        if ((bridgeReconcileIncludeList.isEmpty() && bridgeReconcileExcludeList.isEmpty())
            || (bridgeReconcileIncludeList.isEmpty() && !bridgeReconcileExcludeList.isEmpty())) {
            // Case 1 & 3
            reconcileAllBridges = Boolean.TRUE;
        } else if (!bridgeReconcileIncludeList.isEmpty() && !bridgeReconcileExcludeList.isEmpty()) {
            // Case 4
            LOG.warn(
                "Not a valid case of having both inclusion list : {} and exclusion list : {} for reconcile."
                    + "OvsDb Plugin will reconcile all the bridge excluding exclusion list bridges",
                bridgeReconcileIncludeList, bridgeReconcileExcludeList);
            reconcileAllBridges = Boolean.TRUE;
        }

        List<Node> bridgeNodeList = new ArrayList<>();

        if (reconcileAllBridges) {
            // case 1, 3 & 4
            LOG.trace("Reconciling all bridges with exclusion list {}", bridgeReconcileExcludeList);
            CheckedFuture<Optional<Topology>, ReadFailedException> readTopologyFuture;
            InstanceIdentifier<Topology> topologyInstanceIdentifier = SouthboundMapper
                .createTopologyInstanceIdentifier();
            try (ReadOnlyTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction()) {
                // find all bridges of the specific device in the config data store
                // TODO: this query is not efficient. It retrieves all the Nodes in the datastore, loop over them and
                // look for the bridges of specific device. It is mre efficient if MDSAL allows query nodes using
                // wildcard on node id (ie: ovsdb://uuid/<device uuid>/bridge/*) r attributes
                readTopologyFuture = tx.read(CONFIGURATION, topologyInstanceIdentifier);
            }
            Futures.addCallback(readTopologyFuture, new FutureCallback<Optional<Topology>>() {
                @Override
                public void onSuccess(@Nullable Optional<Topology> optionalTopology) {
                    if (optionalTopology != null && optionalTopology.isPresent()) {
                        @SuppressWarnings("unchecked")
                        Topology topology = optionalTopology.get();
                        if (topology.getNode() != null) {
                            for (Node node : topology.getNode()) {
                                String bridgeNodeIid = node.getNodeId().getValue();
                                LOG.trace("bridgeNodeIid : {}", bridgeNodeIid);
                                if (bridgeReconcileExcludeList.contains(bridgeNodeIid)) {
                                    LOG.trace(
                                        "Ignoring reconcilation on bridge:{} as its part of exclusion list",
                                        bridgeNodeIid);
                                    continue;
                                }
                                bridgeNodeList.add(node);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    LOG.warn("Read Config/DS for Topology failed! {}", nodeIid, throwable);
                }

            }, MoreExecutors.directExecutor());
        } else {
            // Case 3
            // Reconciling Specific set of bridges in order to avoid full Topology Read.
            CheckedFuture<Optional<Node>, ReadFailedException> readNodeFuture;
            LOG.trace("Reconcile Bridge from InclusionList {} only", bridgeReconcileIncludeList);
            for (String bridgeNodeIid : bridgeReconcileIncludeList) {
                try (ReadOnlyTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction()) {
                    InstanceIdentifier<Node> nodeInstanceIdentifier =
                        SouthboundMapper.createInstanceIdentifier(new NodeId(bridgeNodeIid));
                    readNodeFuture = tx.read(CONFIGURATION, nodeInstanceIdentifier);
                }
                Futures.addCallback(readNodeFuture, new FutureCallback<Optional<Node>>() {
                    @Override
                    public void onSuccess(@Nullable Optional<Node> optionalTopology) {
                        if (optionalTopology != null && optionalTopology.isPresent()) {
                            @SuppressWarnings("unchecked")

                            Node node = optionalTopology.get();
                            if (node != null) {
                                bridgeNodeList.add(node);
                            }
                        } else {
                            LOG.info("Reconciliation of bridge {} missing in network-topology config DataStore",
                                bridgeNodeIid);
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.warn("Read Config/DS for Topology failed! {}", bridgeNodeIid, throwable);
                    }
                }, MoreExecutors.directExecutor());
            }
        }

        final Map<InstanceIdentifier<?>, DataObject> brChanges = new HashMap<>();
        final List<Node> tpChanges = new ArrayList<>();
        for (Node node : bridgeNodeList) {
            InstanceIdentifier<Node> ndIid = (InstanceIdentifier<Node>) nodeIid;
            OvsdbBridgeAugmentation bridge = node.augmentation(OvsdbBridgeAugmentation.class);
            if (bridge != null && bridge.getManagedBy() != null
                && bridge.getManagedBy().getValue().equals(ndIid)) {
                brChanges.putAll(extractBridgeConfigurationChanges(node, bridge));
                tpChanges.add(node);
            } else if (node.key().getNodeId().getValue().startsWith(
                nodeIid.firstKeyOf(Node.class).getNodeId().getValue())) {
                //&& node.getTerminationPoint() != null && !node.getTerminationPoint().isEmpty()) {
                // Above check removed to handle delete reconciliation with ManagedBy
                // param not set in config DS
                tpChanges.add(node);
            } else {
                LOG.trace("Ignoring Reconcilation of Bridge: {}", node.key().getNodeId().getValue());

            }
        }

        if (!brChanges.isEmpty()) {
            reconcileBridgeConfigurations(brChanges);
        }
        if (!tpChanges.isEmpty()) {
            reconciliationManager.reconcileTerminationPoints(
                    connectionManagerOfDevice, connectionInstance, tpChanges);
        }
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
                            ovsdbBridgeIid.child(ProtocolEntry.class, protocol.key());
                    changes.put(protocolIid, protocol);
                } else {
                    throw new IllegalArgumentException("Unknown protocol " + protocol.getProtocol());
                }
            }
        }

        if (ovsdbBridge.getControllerEntry() != null) {
            for (ControllerEntry controller : ovsdbBridge.getControllerEntry()) {
                KeyedInstanceIdentifier<ControllerEntry, ControllerEntryKey> controllerIid =
                        ovsdbBridgeIid.child(ControllerEntry.class, controller.key());
                changes.put(controllerIid, controller);
            }
        }

        return changes;
    }

    @VisibleForTesting
    void reconcileBridgeConfigurations(final Map<InstanceIdentifier<?>, DataObject> changes) {
        DataChangeEvent changeEvents = new DataChangeEvent() {
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

    private List<String> getNodeIdForBridges(String nodeIdVal, List<String> bridgeList) {
        List<String> nodeIdBridgeList = new ArrayList<>();
        for (String bridge : bridgeList) {
            String bridgeNodeIid = new StringBuilder().append(nodeIdVal)
                .append(SouthboundConstants.URI_SEPERATOR).append(SouthboundConstants.BRIDGE_URI_PREFIX)
                .append(SouthboundConstants.URI_SEPERATOR).append(bridge).toString();
            nodeIdBridgeList.add(bridgeNodeIid);
        }
        return nodeIdBridgeList;
    }
}
