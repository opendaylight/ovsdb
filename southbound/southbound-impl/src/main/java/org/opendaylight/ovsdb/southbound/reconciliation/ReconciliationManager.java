/*
 * Copyright © 2016, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation;

import static java.util.Objects.requireNonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactUtils;
import org.opendaylight.ovsdb.southbound.reconciliation.configuration.TerminationPointConfigReconciliationTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides the implementation of ovsdb southbound plugins
 * configuration reconciliation engine. This engine provide interfaces
 * to enqueue (one time retry)/ enqueueForRetry(periodic retry)/ dequeue
 * (remove from retry queue) reconciliation task. Reconciliation task can
 * be a connection reconciliation or configuration reconciliation of any
 * ovsdb managed resource like bridge, termination point etc. This engine
 * execute all the reconciliation task through a fixed size thread pool.
 * If submitted task need to be retry after a periodic interval they are
 * submitted to a single thread executor to periodically wake up and check
 * if task is ready for execution.
 * Ideally, addition of any type of reconciliation task should not require
 * any change in this reconciliation manager execution engine.
 *
 * <p>3-Node Cluster:
 * Reconciliation manager is agnostic of whether it's running in single
 * node cluster or 3-node cluster. It's a responsibility of the task
 * submitter to make sure that it submit the task for reconciliation only
 * if it's an owner of that device EXCEPT controller initiated Connection.
 * Reconciliation of controller initiated connection should be done by all
 * the 3-nodes in the cluster, because connection to individual controller
 * can be interrupted for various reason.
 */
public class ReconciliationManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationManager.class);

    private static final int NO_OF_RECONCILER = 10;
    private static final int RECON_TASK_QUEUE_SIZE = 5000;
    private static final long BRIDGE_CACHE_TIMEOUT_IN_SECONDS = 30;

    private final DataBroker db;
    private final InstanceIdentifierCodec instanceIdentifierCodec;
    private final ExecutorService reconcilers;
    private final ScheduledExecutorService taskTriager;

    // Timeout cache contains the list of bridges to be reconciled for termination points
    private LoadingCache<NodeKey, NodeConnectionMetadata> bridgeNodeCache = null;

    // Listens for new bridge creations in the operational DS
    private Registration bridgeCreatedDataTreeChangeRegistration = null;

    private final ReconciliationTaskManager reconTaskManager = new ReconciliationTaskManager();
    private final List<String> reconcileBridgeInclusionList;
    private final List<String> reconcileBridgeExclusionList;

    public ReconciliationManager(final DataBroker db, final InstanceIdentifierCodec instanceIdentifierCodec,
            final List<String> reconcileBridgeInclusionList, final List<String> reconcileBridgeExclusionList) {
        this.db = db;
        this.instanceIdentifierCodec = instanceIdentifierCodec;
        this.reconcileBridgeInclusionList = List.copyOf(reconcileBridgeInclusionList);
        this.reconcileBridgeExclusionList = List.copyOf(reconcileBridgeExclusionList);

        reconcilers = SpecialExecutors.newBoundedCachedThreadPool(NO_OF_RECONCILER, RECON_TASK_QUEUE_SIZE,
                "ovsdb-reconciler", getClass());

        ThreadFactory threadFact = new ThreadFactoryBuilder()
                .setNameFormat("ovsdb-recon-task-triager-%d").build();
        taskTriager = Executors.newSingleThreadScheduledExecutor(threadFact);

        bridgeNodeCache = buildBridgeNodeCache();
    }

    public List<String> getBridgesReconciliationInclusionList() {
        return reconcileBridgeInclusionList;
    }

    public List<String> getBridgesReconciliationExclusionList() {
        return reconcileBridgeExclusionList;
    }

    public boolean isEnqueued(final ReconciliationTask task) {
        return reconTaskManager.isTaskQueued(task);
    }

    public void enqueue(final ReconciliationTask task) {
        LOG.trace("Reconciliation task submitted for execution {}",task);
        reconTaskManager.cacheTask(task, reconcilers.submit(task));
    }

    public void enqueueForRetry(final ReconciliationTask task) {
        LOG.trace("Reconciliation task re-queued for re-execution {}",task);
        reconTaskManager.cacheTask(task, taskTriager.schedule(
                task::checkReadinessAndProcess, task.retryDelayInMills(), TimeUnit.MILLISECONDS
            )
        );
    }

    public void dequeue(final ReconciliationTask task) {
        reconTaskManager.cancelTask(task);
    }

    public DataBroker getDb() {
        return db;
    }

    @Override
    public void close() throws Exception {
        if (this.reconcilers != null) {
            this.reconcilers.shutdownNow();
        }

        if (this.taskTriager != null) {
            this.taskTriager.shutdownNow();
        }
    }

    /**
     * This method reconciles Termination Point configurations for the given list of bridge nodes.
     *
     * @param connectionManager OvsdbConnectionManager object
     * @param connectionInstance OvsdbConnectionInstance object
     * @param bridgeNodes list of bridge nodes be reconciled for termination points
     */
    public void reconcileTerminationPoints(final OvsdbConnectionManager connectionManager,
                                           final OvsdbConnectionInstance connectionInstance,
                                           final List<Node> bridgeNodes) {
        LOG.debug("Reconcile Termination Point Configuration for Bridges {}", bridgeNodes);
        requireNonNull(bridgeNodes, "Bridge Node list must not be null");
        if (!bridgeNodes.isEmpty()) {
            for (Node node : bridgeNodes) {
                bridgeNodeCache.put(node.key(),
                        new NodeConnectionMetadata(node, connectionManager, connectionInstance));
            }
            registerBridgeCreatedDataTreeChangeListener();
        }
    }

    public void cancelTerminationPointReconciliation() {
        cleanupBridgeCreatedDataTreeChangeRegistration();
        for (NodeConnectionMetadata nodeConnectionMetadata : bridgeNodeCache.asMap().values()) {
            if (nodeConnectionMetadata.getNodeIid() != null) {
                dequeue(new TerminationPointConfigReconciliationTask(
                        this,
                        nodeConnectionMetadata.getConnectionManager(),
                        nodeConnectionMetadata.getNode(),
                        nodeConnectionMetadata.getNodeIid(),
                        nodeConnectionMetadata.getConnectionInstance(),
                        nodeConnectionMetadata.getOperTerminationPoints(),
                        instanceIdentifierCodec
                ));
            }
        }
        bridgeNodeCache.invalidateAll();
    }

    private synchronized void registerBridgeCreatedDataTreeChangeListener() {
        if (bridgeCreatedDataTreeChangeRegistration == null) {
            BridgeCreatedDataTreeChangeListener bridgeCreatedDataTreeChangeListener =
                    new BridgeCreatedDataTreeChangeListener();
            InstanceIdentifier<Node> path = SouthboundMapper.createTopologyInstanceIdentifier()
                    .child(Node.class);
            DataTreeIdentifier<Node> dataTreeIdentifier = DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL, path);

            bridgeCreatedDataTreeChangeRegistration = db.registerTreeChangeListener(dataTreeIdentifier,
                    bridgeCreatedDataTreeChangeListener);
        }
    }

    private static LoadingCache<NodeKey, NodeConnectionMetadata> buildBridgeNodeCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(BRIDGE_CACHE_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .build(new CacheLoader<NodeKey, NodeConnectionMetadata>() {
                    @Override
                    public NodeConnectionMetadata load(NodeKey nodeKey) throws Exception {
                        // the termination points are explicitly added to the cache, retrieving bridges that are not in
                        // the cache results in NoSuchElementException
                        throw new NoSuchElementException();
                    }
                });
    }

    /**
     * This class listens for bridge creations in the operational data store.
     * If the newly created bridge is in the 'bridgeNodeCache', termination point reconciliation for the bridge
     * is triggered and the bridge entry is removed from the cache.
     * Once cache is empty, either being removed explicitly or expired, the the listener de-registered.
     */
    class BridgeCreatedDataTreeChangeListener implements DataTreeChangeListener<Node> {
        @Override
        public void onDataTreeChanged(List<DataTreeModification<Node>> changes) {
            bridgeNodeCache.cleanUp();
            if (!bridgeNodeCache.asMap().isEmpty()) {
                Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> nodes =
                        TransactUtils.extractCreated(changes, OvsdbBridgeAugmentation.class);
                Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
                            terminationPointsAug =
                        TransactUtils.extractCreated(changes, OvsdbTerminationPointAugmentation.class);
                for (Map.Entry<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> entry :
                        nodes.entrySet()) {
                    InstanceIdentifier<?> bridgeIid = entry.getKey();
                    NodeKey nodeKey = bridgeIid.firstKeyOf(Node.class);
                    try {
                        NodeConnectionMetadata bridgeNodeMetaData = bridgeNodeCache.get(nodeKey);
                        bridgeNodeMetaData.setNodeIid(bridgeIid);
                        bridgeNodeMetaData.setOperTerminationPoints(
                                filterTerminationPointsForBridge(nodeKey, terminationPointsAug));
                        TerminationPointConfigReconciliationTask tpReconciliationTask =
                                new TerminationPointConfigReconciliationTask(ReconciliationManager.this,
                                        bridgeNodeMetaData.getConnectionManager(),
                                        bridgeNodeMetaData.getNode(),
                                        bridgeIid,
                                        bridgeNodeMetaData.getConnectionInstance(),
                                        bridgeNodeMetaData.getOperTerminationPoints(),
                                        instanceIdentifierCodec);
                        enqueue(tpReconciliationTask);
                        bridgeNodeCache.invalidate(nodeKey);
                    } catch (UncheckedExecutionException ex) {
                        // Ignore NoSuchElementException which indicates bridge node is not in the list of
                        // pending reconciliation
                        if (!(ex.getCause() instanceof NoSuchElementException)) {
                            LOG.error("Error getting Termination Point node from LoadingCache", ex);
                        }

                    } catch (ExecutionException ex) {
                        LOG.error("Error getting Termination Point node from LoadingCache", ex);
                    }
                    if (bridgeNodeCache.asMap().isEmpty()) {
                        LOG.debug("De-registering for bridge creation event");
                        cleanupBridgeCreatedDataTreeChangeRegistration();
                    }
                }
            } else {
                LOG.debug("Cache expired - De-registering for bridge creation event");
                cleanupBridgeCreatedDataTreeChangeRegistration();
            }
        }
    }

    private static Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
        filterTerminationPointsForBridge(NodeKey nodeKey,
            Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
            terminationPoints) {

        Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
                filteredTerminationPoints = new HashMap<>();
        for (Map.Entry<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation> entry :
                terminationPoints.entrySet()) {
            InstanceIdentifier<?> bridgeIid = entry.getKey();
            NodeKey terminationPointNodeKey = bridgeIid.firstKeyOf(Node.class);
            if (terminationPointNodeKey.getNodeId().equals(nodeKey.getNodeId())) {
                LOG.trace("TP Match found: {} {} ", terminationPointNodeKey.getNodeId(), nodeKey.getNodeId());
                filteredTerminationPoints.put(entry.getKey(), entry.getValue());
            } else {
                LOG.trace("TP No Match found : {} {} ", terminationPointNodeKey.getNodeId(), nodeKey.getNodeId());
            }
        }
        return filteredTerminationPoints;

    }

    private void cleanupBridgeCreatedDataTreeChangeRegistration() {
        if (bridgeCreatedDataTreeChangeRegistration != null) {
            bridgeCreatedDataTreeChangeRegistration.close();
            bridgeCreatedDataTreeChangeRegistration = null;
        }
    }

    private static class NodeConnectionMetadata {
        private final Node node;
        private InstanceIdentifier<?> nodeIid;
        private final OvsdbConnectionManager connectionManager;
        private final OvsdbConnectionInstance connectionInstance;
        private Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
            operTerminationPoints;

        public Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
            getOperTerminationPoints() {
            return operTerminationPoints;
        }

        public void setOperTerminationPoints(
            Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
                operTerminationPoints) {
            this.operTerminationPoints = operTerminationPoints;
        }

        NodeConnectionMetadata(Node node,
                               OvsdbConnectionManager connectionManager,
                               OvsdbConnectionInstance connectionInstance) {
            this.node = node;
            this.connectionManager = connectionManager;
            this.connectionInstance = connectionInstance;
        }

        public Node getNode() {
            return node;
        }

        public OvsdbConnectionManager getConnectionManager() {
            return connectionManager;
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
