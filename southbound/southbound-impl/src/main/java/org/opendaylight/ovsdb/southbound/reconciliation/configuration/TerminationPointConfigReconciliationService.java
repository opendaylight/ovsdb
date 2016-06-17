/*
 * Copyright (c) 2016 , NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation.configuration;


import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactUtils;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Reconciliation task to reconcile termination point configurations in the config datastore and existing
 * bridge configuration in the switch. Termination Point configuration can be added to config DS in two ways:
 * 1) Embedded as part of bridge configuration - New bridge with associated termination points are to be created at the
 *    switch as a result.
 * 2) New termination point configurations for existing bridges at the switch - Only termination points for the
 *    associated bridge are to be created
 *
 * This service handles the reconciliation for 2). The reconciliation for 1) is handled by
 * BridgeConfigReconciliationTask.
 *
 * In the scenario 2, only the termination endpoint configuration exists in the config DS. The reconciliation task for
 * termination points will be triggered when the associated bridge created in the Operational DS via the
 * first configuration update from the switch.
 *
 * The list of bridges with termination points to be reconciled in a timeout cache. The service then listens for any
 * new bridge show up in the operational data store. If the new bridge existing in the timeout caches the trigger
 * termination point reconciliation is triggered and is removed from the cache.
 * Once cache is empty, either being removed explicitly or expired, the data store listener for reconciliation is
 * de-registered.
 *
 * Created by Vinh Nguyen (vinh.nguyen@hcl.com) on 6/28/16.
 */
public class TerminationPointConfigReconciliationService implements AutoCloseable  {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointConfigReconciliationService.class);
    private static final long CACHE_TIMEOUT_IN_SECONDS = 60;

    private LoadingCache<NodeKey, NodeConnectionInstance> bridgeNodeCache = null;
    private ReconciliationManager reconciliationManager;
    private ListenerRegistration<TpDataTreeChangeListener> registration = null;

    public TerminationPointConfigReconciliationService(final ReconciliationManager reconciliationManager) {
        this.reconciliationManager = reconciliationManager;
        bridgeNodeCache = CacheBuilder.newBuilder()
                .expireAfterWrite(CACHE_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .build(new CacheLoader<NodeKey, NodeConnectionInstance>() {
                    @Override
                    public NodeConnectionInstance load(NodeKey nodeKey) throws Exception {
                        // the termination points are explicitly added to the cache, retrieving bridges that are not in
                        // the cache results in NoSuchElementException
                        throw new NoSuchElementException();
                    }
                });
    }

    /**
     * The method reconciles Termination Point configurations in the config datastore and existing bridge
     * configuration at the switch.
     *
     * @param connectionInstance ConnectionInstance
     * @param bridgeNodes list of bridge nodes with termination points need to be reconciled
     */
    public void reconcileTerminationPoint(final OvsdbConnectionInstance connectionInstance,
                                          final List<Node> bridgeNodes) {
        LOG.debug("reconcileTerminationPoint for bridgeNodes {}", bridgeNodes);
        Preconditions.checkNotNull(bridgeNodes, "Bridge Node list must not be null");
        if (!bridgeNodes.isEmpty()) {
            for (Node node : bridgeNodes) {
                bridgeNodeCache.put(node.getKey(), new NodeConnectionInstance(node, connectionInstance));
            }
            registerDataTreeChangeListener();
        }
    }

    public void cancelReconciliation() {
        cleanupRegistration();
        for (NodeConnectionInstance nodeConnectionInstance : bridgeNodeCache.asMap().values()) {
            if (nodeConnectionInstance.getNodeIid() != null) {
                reconciliationManager.dequeue(new TerminationPointConfigReconciliationTask(
                        reconciliationManager,
                        nodeConnectionInstance.getNode(),
                        nodeConnectionInstance.getNodeIid(),
                        nodeConnectionInstance.getConnectionInstance()
                ));
            }
        }
        bridgeNodeCache.invalidateAll();
    }

    @Override
    public void close() {
    }

    private synchronized void registerDataTreeChangeListener() {
        if (registration == null) {
            TpDataTreeChangeListener tpDataTreeChangeListener = new TpDataTreeChangeListener();
            InstanceIdentifier<Node> path = SouthboundMapper.createTopologyInstanceIdentifier()
                    .child(Node.class);
            DataTreeIdentifier<Node> dataTreeIdentifier =
                    new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, path);

            registration = reconciliationManager.getDb().registerDataTreeChangeListener(dataTreeIdentifier,
                    tpDataTreeChangeListener);
        }
    }

    class TpDataTreeChangeListener implements ClusteredDataTreeChangeListener<Node> {
        @Override
        public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> changes) {
            bridgeNodeCache.cleanUp();
            if (!bridgeNodeCache.asMap().isEmpty()) {
                Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> nodes =
                        TransactUtils.extractCreated(changes, OvsdbBridgeAugmentation.class);
                for (Map.Entry<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> entry :
                        nodes.entrySet()) {
                    InstanceIdentifier<?> bridgeIid = entry.getKey();
                    NodeKey nodeKey = bridgeIid.firstKeyOf(Node.class);
                    try {
                        NodeConnectionInstance bridgeNode = bridgeNodeCache.get(nodeKey);
                        bridgeNode.setNodeIid(bridgeIid);
                        TerminationPointConfigReconciliationTask tpReconciliationTask =
                                new TerminationPointConfigReconciliationTask(reconciliationManager,
                                        bridgeNode.getNode(), bridgeIid, bridgeNode.getConnectionInstance());
                        reconciliationManager.enqueue(tpReconciliationTask);
                        bridgeNodeCache.invalidate(nodeKey);
                    } catch (UncheckedExecutionException ex) {
                        // Ignore NoSuchElementException which indicates bridge node is not in the list of
                        // pending reconciliation
                        if (!(ex.getCause() instanceof NoSuchElementException)) {
                            LOG.error("error getting Termination Point node from LoadingCache", ex);
                        }

                    } catch (ExecutionException ex) {
                        LOG.error("error getting Termination Point node from LoadingCache", ex);
                    }
                    if (bridgeNodeCache.asMap().isEmpty()) {
                        LOG.debug("De-registering for bridge creation event");
                        cleanupRegistration();
                    }
                }
            } else {
                LOG.debug("Cache expired - De-registering for bridge creation event");
                cleanupRegistration();
            }
        }
    }

    private void cleanupRegistration() {
        if (registration != null) {
            registration.close();
            registration = null;
        }
    }

    private class NodeConnectionInstance {
        Node node;
        InstanceIdentifier<?> nodeIid;
        OvsdbConnectionInstance connectionInstance;

        public NodeConnectionInstance(Node node, OvsdbConnectionInstance connectionInstance) {
            this.node = node;
            this.connectionInstance = connectionInstance;
        }

        public Node getNode() {
            return node;
        }

        public OvsdbConnectionInstance getConnectionInstance() {
            return connectionInstance;
        }

        public void setNodeIid(InstanceIdentifier<?> nodeIid) {
            this.nodeIid = nodeIid;
        }

        public InstanceIdentifier<?> getNodeIid() {
            return nodeIid;
        }
    }
}
