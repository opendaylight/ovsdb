/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.listeners;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.handlers.GlobalNodeHandler;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.handlers.SwitchNodeHandler;
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
import java.util.concurrent.ThreadFactory;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

public abstract  class ListenerBase implements DataTreeChangeListener<Node>, AutoCloseable {

    private ListenerRegistration<ListenerBase> registration;

    DataBroker db;

    private static DataBroker staticDb;

    public static final Logger LOG = LoggerFactory.getLogger(ListenerBase.class);

    static public ExecutorService executorService;

    static ThreadLocal<BindingTransactionChain> transactionChainThreadLocal = new ThreadLocal<>();

    static {
        ThreadFactory threadFact = new ThreadFactoryBuilder()
                .setNameFormat("ovsdb-ha-task-%d").build();
        executorService = Executors.newSingleThreadScheduledExecutor(threadFact);
    }

    public ListenerBase(LogicalDatastoreType datastoreType, DataBroker dataBroker) {
        db = dataBroker;
        staticDb = dataBroker;
        registerListener(datastoreType, db);
    }

    private void registerListener(LogicalDatastoreType datastoreType, final DataBroker db) {
        final DataTreeIdentifier<Node> treeId =
                new DataTreeIdentifier<Node>(datastoreType, getWildcardPath());
        try {
            LOG.trace("Registering on path: {}", treeId);
            registration = db.registerDataTreeChangeListener(treeId, ListenerBase.this);
        } catch (final Exception e) {
            LOG.error("ListenerBase registration failed", e);
        }
    }

    @Override
    public void close() throws Exception {
        if(registration != null) {
            registration.close();
        }
    }

    static public void setThreadPool(ExecutorService pool) {
        executorService = pool;
    }

    static TransactionChainListener chainListener = new TransactionChainListener() {
        @Override
        public void onTransactionChainFailed(TransactionChain<?, ?> transactionChain,
                                             AsyncTransaction<?, ?> asyncTransaction, Throwable throwable) {
            LOG.error("tx chain failed ");
            transactionChainThreadLocal.set(staticDb.createTransactionChain(chainListener));
        }

        @Override
        public void onTransactionChainSuccessful(TransactionChain<?, ?> transactionChain) {
        }
    };

    BindingTransactionChain getChain() {
        if (transactionChainThreadLocal.get() != null) {
            return transactionChainThreadLocal.get();
        }
        transactionChainThreadLocal.set(db.createTransactionChain(chainListener));
        return transactionChainThreadLocal.get();
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Node>> changes) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    BindingTransactionChain chain = getChain();
                    ReadWriteTransaction tx = chain.newReadWriteTransaction();
                    processConnectedNodes(changes, tx);
                    processUpdatedNodes(changes, tx);
                    processDisconnectedNodes(changes, tx);
                    tx.submit().get();
                } catch (Exception e) {
                    LOG.error("Failed to process node for HA", e);
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
                handleUpdated(key, updated, original, tx);
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
                handleDeleted(key, deleted, tx);
            }
        }
    }

    void processConnectedNodes(Collection<DataTreeModification<Node>> changes,
                                       ReadWriteTransaction tx) throws Exception {
        Map<String,Boolean> processedNodes = new HashMap<>();
        for (DataTreeModification<Node> change : changes) {
            InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            DataObjectModification<Node> mod = change.getRootNode();
            Node node = HAUtil.getCreated(mod);
            if (node != null) {
                handleConnected(key, node, tx);
            }
        }
    }

    abstract void handleDeleted(InstanceIdentifier<Node> key, Node deleted, ReadWriteTransaction tx) throws Exception;

    abstract void handleConnected(InstanceIdentifier<Node> key, Node added, ReadWriteTransaction tx) throws Exception;

    abstract void handleUpdated(InstanceIdentifier<Node> key, Node updated, Node original, ReadWriteTransaction tx) throws Exception;

    private InstanceIdentifier<Node> getWildcardPath() {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class);
        return path;
    }

}
