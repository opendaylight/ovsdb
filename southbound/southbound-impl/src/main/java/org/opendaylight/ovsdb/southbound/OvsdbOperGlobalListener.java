/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.utils.mdsal.utils.Scheduler;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbOperGlobalListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {
    public static final ConcurrentMap<InstanceIdentifier<Node>, Node> OPER_NODE_CACHE = new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbOperGlobalListener.class);

    private ListenerRegistration<OvsdbOperGlobalListener> registration;
    private DataBroker db;
    private final OvsdbConnectionManager ovsdbConnectionManager;
    private final TransactionInvoker txInvoker;
    private static final Map<InstanceIdentifier<Node>, ScheduledFuture> TIMEOUT_FTS = new ConcurrentHashMap<>();
    private static Map<InstanceIdentifier<Node>, List<OnlyOnceRunnable>> deletePendingJobs
        = CacheHelper.createCacheForDeleteJobs();


    OvsdbOperGlobalListener(DataBroker db, OvsdbConnectionManager ovsdbConnectionManager,
                            TransactionInvoker txInvoker) {
        LOG.info("Registering OvsdbOperGlobalListener");
        this.db = db;
        this.ovsdbConnectionManager = ovsdbConnectionManager;
        this.txInvoker = txInvoker;
        registerListener();
    }

    public void registerListener() {
        DataTreeIdentifier<Node> treeId =
            DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, getWildcardPath());
        registration = db.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    public void close() {
        if (registration != null) {
            registration.close();
            LOG.info("OVSDB Oper Node listener has been closed.");
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
        LOG.trace("onDataTreeChanged: ");
        changes.forEach((change) -> {
            try {
                InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                DataObjectModification<Node> mod = change.getRootNode();
                Node addNode = getCreated(mod);
                if (addNode != null) {
                    OPER_NODE_CACHE.put(key, addNode);
                    deletePendingJobs.put(key, new CopyOnWriteArrayList<>());
                    LOG.info("Node added to oper {}", SouthboundUtil.getOvsdbNodeId(key));
                }
                Node removedNode = getRemoved(mod);
                if (removedNode != null) {
                    Node cacheRemovedNode = OPER_NODE_CACHE.remove(key);
                    LOG.info("Node deleted from oper {}", SouthboundUtil.getOvsdbNodeId(key));
                    deletePendingJobs.remove(key);

                    OvsdbConnectionInstance connectionInstance = ovsdbConnectionManager.getConnectionInstance(key);
                    if (connectionInstance != null && connectionInstance.isActive()
                        && connectionInstance.getHasDeviceOwnership() != null
                        && connectionInstance.getHasDeviceOwnership()) {
                        LOG.error("Unexpected oper node delete received, Adding the node {} back to oper datastore",
                            SouthboundUtil.getOvsdbNodeId(key));
                        //put the node back in oper
                        txInvoker.invoke(transaction -> {
                            transaction.put(LogicalDatastoreType.OPERATIONAL, key, removedNode);
                        });
                        OPER_NODE_CACHE.put(key, cacheRemovedNode);
                        deletePendingJobs.put(key, new CopyOnWriteArrayList<>());
                    }
                }

                Node modifiedNode = getUpdated(mod);
                if (modifiedNode != null) {
                    OPER_NODE_CACHE.put(key, modifiedNode);
                }
            } catch (Exception e) {
                LOG.error("Failed to handle oper node ", e);
            }
        });
    }

    public static void runAfterTimeoutIfNodeNotCreated(InstanceIdentifier<Node> iid, Runnable job) {
        ScheduledFuture<?> ft = TIMEOUT_FTS.get(iid);
        if (ft != null) {
            ft.cancel(false);
        }
        ft = Scheduler.getScheduledExecutorService().schedule(() -> {
            TIMEOUT_FTS.remove(iid);
            if (!OPER_NODE_CACHE.containsKey(iid)) {
                job.run();
            }
        }, SouthboundConstants.EOS_TIMEOUT, TimeUnit.SECONDS);
        TIMEOUT_FTS.put(iid, ft);
    }

    public static void runAfterNodeDeleted(InstanceIdentifier<Node> iid, Runnable job) {
        CacheHelper.runAfterKeyIsRemoved(job, deletePendingJobs, iid);
    }

    private Node getCreated(DataObjectModification<Node> mod) {
        if ((mod.getModificationType() == DataObjectModification.ModificationType.WRITE)
                && (mod.getDataBefore() == null)) {
            return mod.getDataAfter();
        }
        return null;
    }

    private Node getRemoved(DataObjectModification<Node> mod) {
        if (mod.getModificationType() == DataObjectModification.ModificationType.DELETE) {
            return mod.getDataBefore();
        }
        return null;
    }

    private Node getUpdated(DataObjectModification<Node> mod) {
        Node node = null;
        switch (mod.getModificationType()) {
            case SUBTREE_MODIFIED:
                node = mod.getDataAfter();
                break;
            case WRITE:
                if (mod.getDataBefore() !=  null) {
                    node = mod.getDataAfter();
                }
                break;
            default:
                break;
        }
        return node;
    }

    private InstanceIdentifier<Node> getWildcardPath() {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class);
        return path;
    }

}
