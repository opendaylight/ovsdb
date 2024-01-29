/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.utils.Scheduler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepOperGlobalListener implements DataTreeChangeListener<Node>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepOperGlobalListener.class);
    private static final Map<InstanceIdentifier<Node>, ConnectionInfo> NODE_CONNECTION_INFO = new ConcurrentHashMap<>();
    private static final Map<InstanceIdentifier<Node>, ScheduledFuture> TIMEOUT_FTS = new ConcurrentHashMap<>();

    private Registration registration;
    private final HwvtepConnectionManager hcm;
    private final DataBroker db;
    private static final Map<InstanceIdentifier<Node>, List<Callable<Void>>> NODE_DELET_WAITING_JOBS
            = new ConcurrentHashMap<>();
    private static final Map<InstanceIdentifier<Node>, Node> CONNECTED_NODES = new ConcurrentHashMap<>();


    HwvtepOperGlobalListener(final DataBroker db, HwvtepConnectionManager hcm) {
        LOG.info("Registering HwvtepOperGlobalListener");
        this.db = db;
        this.hcm = hcm;
        registerListener();
    }

    private void registerListener() {
        final DataTreeIdentifier<Node> treeId =
                        DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL, getWildcardPath());

        registration = db.registerTreeChangeListener(treeId, this);
    }

    @Override
    public void close() {
        if (registration != null) {
            registration.close();
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void onDataTreeChanged(final List<DataTreeModification<Node>> changes) {
        LOG.trace("onDataTreeChanged: ");
        try {
            connect(changes);
            updated(changes);
            disconnect(changes);
        } catch (Exception e) {
            LOG.error("Failed to handle dcn event ", e);
        }
    }

    public static void runAfterTimeoutIfNodeNotCreated(InstanceIdentifier<Node> iid, Runnable job) {
        ScheduledFuture<?> ft = TIMEOUT_FTS.get(iid);
        if (ft != null) {
            ft.cancel(false);
        }
        ft = Scheduler.getScheduledExecutorService().schedule(() -> {
            TIMEOUT_FTS.remove(iid);
            if (!NODE_CONNECTION_INFO.containsKey(iid)) {
                job.run();
            }
        }, HwvtepSouthboundConstants.EOS_TIMEOUT, TimeUnit.SECONDS);
        TIMEOUT_FTS.put(iid, ft);
    }

    public void runAfterNodeDeleted(InstanceIdentifier<Node> iid, Callable<Void> job) throws Exception {
        synchronized (HwvtepOperGlobalListener.class) {
            if (NODE_DELET_WAITING_JOBS.containsKey(iid)) {
                LOG.error("Node present in the cache {} adding to delete queue", iid);
                NODE_DELET_WAITING_JOBS.get(iid).add(job);
                //Also delete the node so that reconciliation kicks in
                deleteTheNodeOfOldConnection(iid, getNodeConnectionInfo(iid));
                HwvtepSouthboundUtil.schedule(() -> {
                    runPendingJobs(iid);
                }, HwvtepSouthboundConstants.HWVTEP_REGISTER_CALLBACKS_WAIT_TIMEOUT, TimeUnit.SECONDS);
            } else {
                LOG.info("Node not present in the cache {} running the job now", iid);
                job.call();
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private synchronized void runPendingJobs(InstanceIdentifier<Node> iid) {
        List<Callable<Void>> jobs = NODE_DELET_WAITING_JOBS.remove(iid);
        if (jobs != null && !jobs.isEmpty()) {
            jobs.forEach(job -> {
                try {
                    LOG.error("Node disconnected job found {} running it now ", iid);
                    job.call();
                } catch (Exception e) {
                    LOG.error("Failed to run callable ", e);
                }
            });
            jobs.clear();
        }
    }

    private static void connect(List<DataTreeModification<Node>> changes) {
        changes.forEach(change -> {
            InstanceIdentifier<Node> key = change.getRootPath().path();
            DataObjectModification<Node> mod = change.getRootNode();
            Node node = getCreated(mod);
            if (node == null) {
                return;
            }
            CONNECTED_NODES.put(key, node);
            ScheduledFuture ft = TIMEOUT_FTS.remove(key);
            if (ft != null) {
                ft.cancel(false);
            }
            HwvtepGlobalAugmentation globalAugmentation = node.augmentation(HwvtepGlobalAugmentation.class);
            if (globalAugmentation != null) {
                ConnectionInfo connectionInfo = globalAugmentation.getConnectionInfo();
                if (connectionInfo != null) {
                    NODE_CONNECTION_INFO.put(key, connectionInfo);
                }
            }
            if (node != null) {
                synchronized (HwvtepOperGlobalListener.class) {
                    NODE_DELET_WAITING_JOBS.putIfAbsent(key, new ArrayList<>());
                }
            }
        });
    }

    private static void updated(List<DataTreeModification<Node>> changes) {
        changes.forEach(change -> {
            InstanceIdentifier<Node> key = change.getRootPath().path();
            DataObjectModification<Node> mod = change.getRootNode();
            Node node = getUpdated(mod);
            if (node != null) {
                CONNECTED_NODES.put(key, node);
            }
        });
    }

    public static Node getNode(final InstanceIdentifier<Node> key) {
        return CONNECTED_NODES.get(key);
    }

    private void disconnect(List<DataTreeModification<Node>> changes) {
        changes.forEach(change -> {
            InstanceIdentifier<Node> key = change.getRootPath().path();
            DataObjectModification<Node> mod = change.getRootNode();
            Node node = getRemoved(mod);
            if (node != null) {
                CONNECTED_NODES.remove(key);
                NODE_CONNECTION_INFO.remove(key);
                synchronized (HwvtepOperGlobalListener.class) {
                    runPendingJobs(key);
                }
            }
        });
    }

    private static String getNodeId(InstanceIdentifier<Node> iid) {
        return iid.firstKeyOf(Node.class).getNodeId().getValue();
    }

    public void scheduleOldConnectionNodeDelete(InstanceIdentifier<Node> iid) {
        ConnectionInfo oldConnectionInfo = getNodeConnectionInfo(iid);
        HwvtepSouthboundUtil.schedule(() -> {
            deleteTheNodeOfOldConnection(iid, oldConnectionInfo);
        }, HwvtepSouthboundConstants.STALE_HWVTEP_CLEANUP_DELAY_SECS, TimeUnit.SECONDS);
    }

    private void deleteTheNodeOfOldConnection(InstanceIdentifier<Node> iid,
                                                    ConnectionInfo oldConnectionInfo) {
        if (oldConnectionInfo == null) {
            return;
        }
        ConnectionInfo latestConnectionInfo = getNodeConnectionInfo(iid);
        if (Objects.equals(latestConnectionInfo, oldConnectionInfo)) {
            //Still old connection node is not deleted
            LOG.debug("Delete Node {} from oper ", getNodeId(iid));
            hcm.cleanupOperationalNode(iid);
        }
    }

    private static ConnectionInfo getNodeConnectionInfo(InstanceIdentifier<Node> iid) {
        return NODE_CONNECTION_INFO.get(iid);
    }

    private static Node getCreated(final DataObjectModification<Node> mod) {
        if (mod.getModificationType() == ModificationType.WRITE && mod.getDataBefore() == null) {
            return mod.getDataAfter();
        }
        return null;
    }

    private static Node getRemoved(final DataObjectModification<Node> mod) {
        if (mod.getModificationType() == ModificationType.DELETE) {
            return mod.getDataBefore();
        }
        return null;
    }

    private static InstanceIdentifier<Node> getWildcardPath() {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class);
    }

    private static Node getUpdated(DataObjectModification<Node> mod) {
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
}
