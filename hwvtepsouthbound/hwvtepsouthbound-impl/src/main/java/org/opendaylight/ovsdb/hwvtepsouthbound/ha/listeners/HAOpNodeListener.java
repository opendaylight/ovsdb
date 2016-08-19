/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.listeners;

import com.google.common.base.Strings;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.handlers.GlobalNodeHandler;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.state.D2ConnectedHandler;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.state.HAContext;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.state.HAStateHandler;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.state.HAStateHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

public class HAOpNodeListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {

    private ListenerRegistration<HAOpNodeListener> registration;
    private static DataBroker db;
    public static final Logger LOG = LoggerFactory.getLogger(HAOpNodeListener.class);

    public HAOpNodeListener(DataBroker db) {
        LOG.info("Registering HwvtepDataChangeListener for operational nodes");
        this.db = db;
        registerListener(db);
    }

    private void registerListener(final DataBroker db) {
        final DataTreeIdentifier<Node> treeId =
                new DataTreeIdentifier<Node>(OPERATIONAL, getWildcardPath());
        try {
            LOG.trace("Registering on path: {}", treeId);
            registration = db.registerDataTreeChangeListener(treeId, HAOpNodeListener.this);
        } catch (final Exception e) {
            LOG.warn("HwvtepDataChangeListener registration failed", e);
        }
    }

    @Override
    public void close() throws Exception {
        if(registration != null) {
            registration.close();
        }
    }
    static TransactionChainListener chainListener = new TransactionChainListener() {
        @Override
        public void onTransactionChainFailed(TransactionChain<?, ?> transactionChain,
                                             AsyncTransaction<?, ?> asyncTransaction, Throwable throwable) {
            LOG.error("tx chain failed ");
            transactionChainThreadLocal.set(db.createTransactionChain(chainListener));
        }

        @Override
        public void onTransactionChainSuccessful(TransactionChain<?, ?> transactionChain) {
        }
    };
    public ExecutorService service = Executors.newFixedThreadPool(1);
    public void setThreadPool(ExecutorService pool) {
        this.service = pool;
    }
    static ThreadLocal<BindingTransactionChain> transactionChainThreadLocal = new ThreadLocal<>();

    BindingTransactionChain getChain() {
        if (transactionChainThreadLocal.get() != null) {
            return transactionChainThreadLocal.get();
        }
        transactionChainThreadLocal.set(db.createTransactionChain(chainListener));
        return transactionChainThreadLocal.get();
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Node>> changes) {
        service.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    HAUtil.setNextId();
                    BindingTransactionChain chain = getChain();
                    ReadWriteTransaction transaction = chain.newReadWriteTransaction();
                    processConnectedNodes(changes, transaction);
                    processUpdatedNodes(changes, transaction);
                    processDisconnectedNodes(changes,transaction);
                    transaction.submit().get();
                } catch (Exception e) {
                    LOG.error("Failed to process node ",e);
                    transactionChainThreadLocal.set(db.createTransactionChain(chainListener));
                }
            }
        });
    }

    private void processUpdatedNodes(Collection<DataTreeModification<Node>> changes,
                                     ReadWriteTransaction tx) throws Exception {
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node updated = HAUtil.getUpdated(mod);
            Node original = HAUtil.getOriginal(mod);
            if (updated != null && original != null) {
                if (updated.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                    HAContext haContext = new HAContext(updated, null, true, tx);
                    InstanceIdentifier<Node> haPath = haContext.getHaNodePath();
                    if (haPath != null) {
                        GlobalNodeHandler.pushChildGlobalOperationalUpdateToHA(updated, original, haPath, tx);
                        D2ConnectedHandler.onChildGlobalOperationalUpdate(updated, original, haPath, haContext, tx);
                    }
                }
            }
        }
    }

    private void processDisconnectedNodes(Collection<DataTreeModification<Node>> changes,
                                          ReadWriteTransaction tx) throws Exception {

        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node deleted = HAUtil.getRemoved(mod);
            if (deleted != null) {
                HwvtepGlobalAugmentation hgDeleted = deleted.getAugmentation(HwvtepGlobalAugmentation.class);
                if (deleted.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                    HAContext haContext = new HAContext(deleted, null, false, tx);
                    HAStateHandler haStateHandler = HAStateHelper.getStateHandler(haContext);
                    haStateHandler.handle(haContext, tx);
                } else {
                    HAContext haContext = new HAContext(null, deleted, false, tx);
                    HAStateHandler haStateHandler = HAStateHelper.getStateHandler(haContext);
                    haStateHandler.handle(haContext, tx);
                }
            }
        }
    }

    private void processConnectedNodes(Collection<DataTreeModification<Node>> changes,
                                       ReadWriteTransaction tx) throws Exception {
        Map<String,Boolean> processedNodes = new HashMap<>();
        for (DataTreeModification<Node> change : changes) {
            InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            DataObjectModification<Node> mod = change.getRootNode();
            Node node = HAUtil.getCreated(mod);
            if (node != null) {
                HAContext haContext = null;
                String globalNodeId = null;
                PhysicalSwitchAugmentation physicalSwitchAugmentation =
                        node.getAugmentation(PhysicalSwitchAugmentation.class);

                if (node.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                    globalNodeId = node.getNodeId().getValue();
                    if (processedNodes.containsKey(globalNodeId)) {
                        continue;
                    }
                    LOG.error("new node connected tid {} nodeId {}", HAUtil.getId(), node.getNodeId().getValue());
                    haContext = new HAContext(node, null, true, tx);
                    HAStateHandler haStateHandler = HAStateHelper.getStateHandler(haContext);
                    haStateHandler.handle(haContext, tx);

                } else if (physicalSwitchAugmentation != null) {
                    InstanceIdentifier<?> globalIid = physicalSwitchAugmentation.getManagedBy().getValue();
                    globalNodeId = globalIid.firstKeyOf(Node.class).getNodeId().getValue();
                    if (processedNodes.containsKey(globalNodeId)) {
                        continue;
                    }

                    LOG.error("new node connected switch node tid {} nodeId {} ", HAUtil.getId(), node.getNodeId().getValue());
                    haContext = new HAContext(null, node, true, tx);
                    HAStateHandler haStateHandler = HAStateHelper.getStateHandler(haContext);
                    haStateHandler.handle(haContext, tx);
                }
                if (!Strings.isNullOrEmpty(haContext.getHaId())) {
                    processedNodes.put(globalNodeId, Boolean.TRUE);
                }
            }
        }
    }

    private InstanceIdentifier<Node> getWildcardPath() {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class);
        return path;
    }

}
