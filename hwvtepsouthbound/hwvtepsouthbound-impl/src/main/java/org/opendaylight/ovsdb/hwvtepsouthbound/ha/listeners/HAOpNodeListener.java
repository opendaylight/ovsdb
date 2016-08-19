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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
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

public class HAOpNodeListener extends ListenerBase implements DataTreeChangeListener<Node>, AutoCloseable {

    public static final Logger LOG = LoggerFactory.getLogger(HAOpNodeListener.class);

    public HAOpNodeListener(DataBroker db) {
        super(OPERATIONAL, db);
        LOG.info("Registering HwvtepDataChangeListener for operational nodes");
    }

    @Override
    void processConnectedNodes(Collection<DataTreeModification<Node>> changes,
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
                    LOG.error("new node connected nodeId {}", node.getNodeId().getValue());
                    haContext = new HAContext(node, null, true, tx);
                    HAStateHandler haStateHandler = HAStateHelper.getStateHandler(haContext);
                    haStateHandler.handle(haContext, tx);

                } else if (physicalSwitchAugmentation != null) {
                    InstanceIdentifier<?> globalIid = physicalSwitchAugmentation.getManagedBy().getValue();
                    globalNodeId = globalIid.firstKeyOf(Node.class).getNodeId().getValue();
                    if (processedNodes.containsKey(globalNodeId)) {
                        continue;
                    }

                    LOG.error("new node connected switch nodeId {} ", node.getNodeId().getValue());
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

    @Override
    void handleDeleted(InstanceIdentifier<Node> key, Node deleted,ReadWriteTransaction tx) throws Exception {
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

    @Override
    void handleConnected(InstanceIdentifier<Node> key, Node node,ReadWriteTransaction tx) throws Exception {
    }

    @Override
    void handleUpdated(InstanceIdentifier<Node> key, Node updated, Node original, ReadWriteTransaction tx) throws Exception {
        if (updated != null && original != null) {
            if (updated.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                boolean wasHAChild = HACache.isHAChildNode(key);
                HAConfigClusteredListener.updateHACache(key, updated, original, db, tx);
                boolean becameHAChild = HACache.isHAChildNode(key);

                if (wasHAChild) {
                    HAContext haContext = new HAContext(updated, null, true, tx);
                    InstanceIdentifier<Node> haPath = haContext.getHaNodePath();
                    if (haPath != null) {
                        GlobalNodeHandler.pushChildGlobalOperationalUpdateToHA(updated, original, haPath, tx);
                        D2ConnectedHandler.onChildGlobalOperationalUpdate(updated, original, haPath, haContext, tx);
                    }
                } else if (becameHAChild) {
                    HAContext haContext = new HAContext(updated, null, true, tx);
                    HAStateHandler haStateHandler = HAStateHelper.getStateHandler(haContext);
                    haStateHandler.handle(haContext, tx);
                }
            } else {
                HAContext haContext = new HAContext(null, updated, true, tx);
                InstanceIdentifier<Node> haPath = haContext.getHaNodePath();
                InstanceIdentifier<Node> haPsPath = haContext.getHaPsNodePath();
                if (haPath != null) {
                    SwitchNodeHandler.pushChildGlobalOperationalUpdateToHA(updated, original,
                            haPath, haPsPath, tx);
                }
            }
        }
    }

}
