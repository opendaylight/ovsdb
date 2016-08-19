/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.listeners;

import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.PhysicalLocatorUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.RemoteMcastUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.RemoteUcastUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.VlanBindingsUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.ComparatorUtils.translateConfigUpdate;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LogicalSwitchesUtil.logicalSwitchTransformer;

public class HAConfigNodeListener extends ListenerBase {
    private static final Logger LOG = LoggerFactory.getLogger(HAConfigNodeListener.class);

    public HAConfigNodeListener(DataBroker db) {
        super(LogicalDatastoreType.CONFIGURATION, db);
    }

    InstanceIdentifier<Node> getManagedByNodeId(Node node) {
        if (node == null) {
            return null;
        }
        PhysicalSwitchAugmentation augmentation = node.getAugmentation(PhysicalSwitchAugmentation.class);
        if (augmentation != null && augmentation.getManagedBy() != null) {
            return (InstanceIdentifier<Node>) augmentation.getManagedBy().getValue();
        }
        return null;
    }

    @Override
    void handleConnected(InstanceIdentifier<Node> key, Node added, ReadWriteTransaction tx) throws Exception {
        List<String> haNewNodeIds = null;
        if (added != null) {
            if (added.getAugmentation(HwvtepGlobalAugmentation.class) == null) {
                haNewNodeIds = HAUtil.getNodeIdsForSwitchNode(db, added);
                handleHASwitchNodeUpdates(added, haNewNodeIds, tx);
            }
        }
    }

    @Override
    void handleUpdated(InstanceIdentifier<Node> key, Node haUpdated, Node haOriginal, ReadWriteTransaction tx) throws Exception {
        List<String> haNewNodeIds = null;
        if (haUpdated != null) {
            if (haUpdated.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                HAConfigClusteredListener.updateHACache(key, haUpdated, haOriginal, db, tx);
                haNewNodeIds = HAUtil.getNodeIdsFromOtherConfig(haUpdated);
                handleHAGlobalNodeUpdates(haUpdated, haOriginal, haNewNodeIds, tx);
            } else {
                haNewNodeIds = HAUtil.getNodeIdsForSwitchNode(db, haUpdated);
                handleHASwitchNodeUpdates(haUpdated, haNewNodeIds, tx);
                /*
                InstanceIdentifier<Node> globalNodeId = getManagedByNodeId(haUpdated);
                if (HACache.isHAParentNode(globalNodeId)) {
                    //TODO
                }
                */
            }
        }
    }

    private void handleHASwitchNodeUpdates(Node haUpdated, List<String> childNodeIds,
                                           ReadWriteTransaction tx) throws InterruptedException, ExecutionException, ReadFailedException {
        if (null == childNodeIds) {
            return;
        }
        for (String haChildNodeId : childNodeIds) {
            LOG.trace("copy switch config  from ha {} to {}",
                    haUpdated.getNodeId().getValue(), haChildNodeId);
            mergeHASwitchConfigToChildNode(haUpdated, haChildNodeId, tx);
        }
    }

    private void handleHAGlobalNodeUpdates(Node haUpdated, Node haOriginal, List<String> haNewNodeIds,
                                           ReadWriteTransaction tx) throws InterruptedException, ExecutionException, ReadFailedException {
        if (null == haNewNodeIds) {
            return;
        }
        for (String haChildNodeId : haNewNodeIds) {
            LOG.trace("copy global config from ha {} to {}",
                    haUpdated.getNodeId().getValue(), haChildNodeId);
            mergeHaGlobalConfigToChildNode(haOriginal, haUpdated, haChildNodeId, tx);
        }
    }

    private void mergeHaGlobalConfigToChildNode(Node haOriginal, Node haUpdated, String haChildNodeId,
                                                ReadWriteTransaction tx) throws InterruptedException, ExecutionException, ReadFailedException {
        final InstanceIdentifier<Node> nodePath = HAUtil.createInstanceIdentifier(haChildNodeId);
        NodeId nodeId = nodePath.firstKeyOf(Node.class).getNodeId();
        NodeBuilder haChildNodeBuilder = HAUtil.getHAChildNodeBuilder(db, nodePath, nodeId);

        WriteTransaction writeTransaction = db.newWriteOnlyTransaction();
        HwvtepGlobalAugmentation haGlobalNodeUpdated = haUpdated.getAugmentation(HwvtepGlobalAugmentation.class);
        HwvtepGlobalAugmentation haGlobalNodeOriginal = null;
        if (haOriginal != null) {
            haGlobalNodeOriginal = haOriginal.getAugmentation(HwvtepGlobalAugmentation.class);
        }

        haChildNodeBuilder.setTerminationPoint(translateConfigUpdate(
                haUpdated.getTerminationPoint(), new PhysicalLocatorUtil.PhysicalLocatorTransformer()));

        mergeHaNodeGlobalConfigToChildNode(haGlobalNodeOriginal, haGlobalNodeUpdated, haChildNodeBuilder, nodeId);

        tx.put(LogicalDatastoreType.CONFIGURATION, nodePath, haChildNodeBuilder.build(), true);
    }

    private void mergeHASwitchConfigToChildNode(Node haUpdated , String haChildNodeId,
                                                ReadWriteTransaction tx) throws InterruptedException, ExecutionException, ReadFailedException {
        final InstanceIdentifier<Node> nodePath = HAUtil.createInstanceIdentifier(haChildNodeId);
        NodeId nodeId = nodePath.firstKeyOf(Node.class).getNodeId();
        NodeBuilder haChildNodeBuilder = HAUtil.getHAChildNodeBuilder(db, nodePath, nodeId);

        haChildNodeBuilder.setTerminationPoint(translateConfigUpdate(
                haUpdated.getTerminationPoint(), new VlanBindingsUtil.VlanBindingsTransformer(nodePath)));

        tx.put(LogicalDatastoreType.CONFIGURATION, nodePath, haChildNodeBuilder.build(), true);
    }

    private void mergeHaNodeGlobalConfigToChildNode(HwvtepGlobalAugmentation haGlobalNodeOriginal,
                HwvtepGlobalAugmentation haGlobalNodeUpdated, NodeBuilder haChildNodeBuilder, NodeId nodeId) {
        HwvtepGlobalAugmentationBuilder haChildNodeGlobalBuilder;
        HwvtepGlobalAugmentation haChildGlobalAugmentation =
                haChildNodeBuilder.getAugmentation(HwvtepGlobalAugmentation.class);
        if (haChildGlobalAugmentation != null) {
            haChildNodeGlobalBuilder = new HwvtepGlobalAugmentationBuilder(haChildGlobalAugmentation);
        } else {
            haChildNodeGlobalBuilder = new HwvtepGlobalAugmentationBuilder();
        }

        InstanceIdentifier<Node> childNodePath = HAUtil.createInstanceIdentifier(nodeId.getValue());
        haChildNodeGlobalBuilder.setLogicalSwitches(translateConfigUpdate(
                haGlobalNodeUpdated.getLogicalSwitches(), logicalSwitchTransformer));
        haChildNodeGlobalBuilder.setRemoteMcastMacs(translateConfigUpdate(
                haGlobalNodeUpdated.getRemoteMcastMacs(), new RemoteMcastUtil.RemoteMcastMacsTransformer(childNodePath)));
        haChildNodeGlobalBuilder.setRemoteUcastMacs(translateConfigUpdate(
                haGlobalNodeUpdated.getRemoteUcastMacs(), new RemoteUcastUtil.RemoteUcastMacsTransformer(childNodePath)));
        haChildNodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, haChildNodeGlobalBuilder.build());
    }

    @Override
    void handleDeleted(InstanceIdentifier<Node> key, Node deleted, ReadWriteTransaction tx) throws Exception {
        List<String> childNodeIds = null;
        if (deleted != null) {
            if (deleted.getAugmentation(PhysicalSwitchAugmentation.class) != null) {
                childNodeIds = HAUtil.getNodeIdsForSwitchNode(db, deleted);
            }
            if (deleted.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                childNodeIds = HAUtil.getNodeIdsFromOtherConfig(deleted);
            }
            if (null == childNodeIds) {
                return;
            }
            for (String haChildNodeId : childNodeIds) {
                LOG.error("TODO delete child global/switch config ");
            }
        }
    }

}
