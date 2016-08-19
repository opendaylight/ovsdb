package org.opendaylight.ovsdb.hwvtepsouthbound.ha.listeners;

import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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

import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.ComparatorUtils.translateConfigUpdate;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LogicalSwitchesUtil.logicalSwitchTransformer;

//import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.*;
//import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.*;

public class HAConfigNodeListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HAConfigNodeListener.class);

    private final DataBroker db;
    private ListenerRegistration<HAConfigNodeListener> registration;

    public HAConfigNodeListener(DataBroker db) {
        LOG.info("Registering HAConfigNodeListener");
        this.db = db;
        registerListener(db);
    }

    private void registerListener(final DataBroker db) {
        final DataTreeIdentifier<Node> treeId =
                new DataTreeIdentifier<Node>(LogicalDatastoreType.CONFIGURATION, getWildcardPath());
        try {
            LOG.trace("Registering on path: {}", treeId);
            registration = db.registerDataTreeChangeListener(treeId, HAConfigNodeListener.this);
        } catch (final Exception e) {
            LOG.warn("HAConfigNodeListener registration failed", e);
        }
    }

    @Override
    public void close() throws Exception {
        if(registration != null) {
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
        HAUtil.setNextId();
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node haUpdated = HAUtil.getUpdated(mod);
            Node haOriginal = HAUtil.getOriginal(mod);
            List<String> haNewNodeIds = null;

            if (haUpdated != null) {
                if (haUpdated.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                    haNewNodeIds = HAUtil.getNodeIdsFromOtherConfig(haUpdated);
                    handleHAGlobalNodeUpdates(haUpdated, haOriginal, haNewNodeIds);
                } else {
                    haNewNodeIds = HAUtil.getNodeIdsForSwitchNode(db, haUpdated);
                    handleHASwitchNodeUpdates(haUpdated, haOriginal, haNewNodeIds);
                }
            }
        }
    }

    private void handleHASwitchNodeUpdates(Node haUpdated, Node haOriginal, List<String> childNodeIds) {
        if (null == childNodeIds) {
            return;
        }
        for (String haChildNodeId : childNodeIds) {
            LOG.trace("copy switch config  from ha {} to {} tid {}",
                    haUpdated.getNodeId().getValue(), haChildNodeId, HAUtil.getId());
            mergeHASwitchConfigToChildNode(haUpdated,haOriginal, haChildNodeId);
        }
    }

    private void handleHAGlobalNodeUpdates(Node haUpdated, Node haOriginal, List<String> haNewNodeIds) {
        if (null == haNewNodeIds) {
            return;
        }
        for (String haChildNodeId : haNewNodeIds) {
            LOG.trace("copy global config from ha {} to {} tid {}",
                    haUpdated.getNodeId().getValue(), haChildNodeId, HAUtil.getId());
            mergeHaGlobalConfigToChildNode(haOriginal, haUpdated, haChildNodeId);
        }
    }

    private void mergeHaGlobalConfigToChildNode(Node haOriginal, Node haUpdated, String haChildNodeId) {
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

        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, nodePath, haChildNodeBuilder.build(), true);
        writeTransaction.submit();
    }

    private void mergeHASwitchConfigToChildNode(Node haUpdated , Node haOriginal, String haChildNodeId) {
        final InstanceIdentifier<Node> nodePath = HAUtil.createInstanceIdentifier(haChildNodeId);
        NodeId nodeId = nodePath.firstKeyOf(Node.class).getNodeId();
        NodeBuilder haChildNodeBuilder = HAUtil.getHAChildNodeBuilder(db, nodePath, nodeId);

        WriteTransaction writeTransaction = db.newWriteOnlyTransaction();

        PhysicalSwitchAugmentation haPSNodeUpdated = haUpdated.getAugmentation(PhysicalSwitchAugmentation.class);
        PhysicalSwitchAugmentation haPSNodeOriginal = null;
        if (haOriginal != null) {
            haPSNodeOriginal = haOriginal.getAugmentation(PhysicalSwitchAugmentation.class);
        }
        haChildNodeBuilder.setTerminationPoint(translateConfigUpdate(
                haUpdated.getTerminationPoint(), new VlanBindingsUtil.VlanBindingsTransformer(nodePath)));

        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, nodePath, haChildNodeBuilder.build(), true);
        writeTransaction.submit();
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


    private InstanceIdentifier<Node> getWildcardPath() {
        InstanceIdentifier<Node> path = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class);
        return path;
    }
}
