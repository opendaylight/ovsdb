/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Managers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.managers.ManagerOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.managers.ManagerOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.managers.ManagerOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class HAUtil {

    static Logger LOG = LoggerFactory.getLogger(HAUtil.class);

    public static HwvtepPhysicalLocatorRef buildLocatorRef(InstanceIdentifier<Node> nodeIid,String tepIp ) {
        InstanceIdentifier<TerminationPoint> tepId = buildTpId(nodeIid, tepIp);
        return new HwvtepPhysicalLocatorRef(tepId);
    }

    public static Uuid getUUid(String key) {
        return new Uuid(UUID.nameUUIDFromBytes(key.getBytes()).toString());
    }


    public static InstanceIdentifier<TerminationPoint> buildTpId(InstanceIdentifier<Node> nodeIid,String tepIp ) {
        String tpKeyStr = "vxlan_over_ipv4"+':'+tepIp;
        TerminationPointKey tpKey = new TerminationPointKey(new TpId(tpKeyStr));
        InstanceIdentifier<TerminationPoint> plIid = nodeIid.child(TerminationPoint.class, tpKey);
        return plIid;
    }

    public static String getTepIp(HwvtepPhysicalLocatorRef locatorRef) {
        InstanceIdentifier<TerminationPoint> tpId = (InstanceIdentifier<TerminationPoint>) locatorRef.getValue();
        return tpId.firstKeyOf(TerminationPoint.class).getTpId().getValue().substring("vxlan_over_ipv4:".length());
    }

    public static String getSwitchName(HwvtepLogicalSwitchRef logicalSwitchRef) {
        InstanceIdentifier<LogicalSwitches> id = (InstanceIdentifier<LogicalSwitches>) logicalSwitchRef.getValue();
        return id.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue();
    }

    public static String getNodeId(HwvtepPhysicalLocatorRef locatorRef) {
        InstanceIdentifier<TerminationPoint> tpId = (InstanceIdentifier<TerminationPoint>) locatorRef.getValue();
        return tpId.firstKeyOf(Node.class).getNodeId().getValue();
    }

    public static String getNodeId(HwvtepLogicalSwitchRef logicalSwitchRef) {
        InstanceIdentifier<LogicalSwitches> id = (InstanceIdentifier<LogicalSwitches>) logicalSwitchRef.getValue();
        return id.firstKeyOf(Node.class).getNodeId().getValue();
    }


    public static NodeId getNodeId(String haId) {
        String nodeString = HwvtepSouthboundConstants.HWVTEP_URI_PREFIX + "://" +
                HwvtepSouthboundConstants.UUID + "/" + UUID.nameUUIDFromBytes(haId.getBytes()).toString();
        NodeId nodeId = new NodeId(new Uri(nodeString));
        return nodeId;
    }

    public static InstanceIdentifier<Node> getInstanceIdentifier(String haUUidVal) {
        String nodeString = HwvtepSouthboundConstants.HWVTEP_URI_PREFIX + "://" +
                HwvtepSouthboundConstants.UUID + "/" + UUID.nameUUIDFromBytes(haUUidVal.getBytes()).toString();
        NodeId nodeId = new NodeId(new Uri(nodeString));
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(String nodeIdString) {
        NodeId nodeId = new NodeId(new Uri(nodeIdString));
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }

    public static ManagerOtherConfigs getOtherConfig(String key, String val) {
        ManagerOtherConfigsBuilder otherConfigsBuilder = new ManagerOtherConfigsBuilder();
        ManagerOtherConfigsKey z = new ManagerOtherConfigsKey(key);
        otherConfigsBuilder.setKey(z);
        otherConfigsBuilder.setOtherConfigKey(key);
        otherConfigsBuilder.setOtherConfigValue(val);
        return otherConfigsBuilder.build();
    }

    public static Optional<Node> readNode(DataBroker db, LogicalDatastoreType storeType, InstanceIdentifier<Node> nodeId)
            throws ReadFailedException, ExecutionException, InterruptedException {
        return db.newReadWriteTransaction().read(storeType, nodeId).get();
    }
    //ReadWriteTransaction

    public static Optional<Node> readNode(ReadWriteTransaction tx, LogicalDatastoreType storeType, InstanceIdentifier<Node> nodeId)
            throws ReadFailedException, ExecutionException, InterruptedException {
        return tx.read(storeType, nodeId).get();
    }

    public static String getHAId(DataBroker db, HwvtepGlobalAugmentation globalAugmentation) {
        String haId = "";
        boolean haEnabled = false;
        if (globalAugmentation.getManagers() != null && globalAugmentation.getManagers().size() > 0 &&
                globalAugmentation.getManagers().get(0).getManagerOtherConfigs() != null){
            for (ManagerOtherConfigs configs : globalAugmentation.getManagers().get(0).getManagerOtherConfigs()) {
                if (configs.getOtherConfigKey().equals("ha_enabled") && configs.getOtherConfigValue().equals("true")) {
                    haEnabled = true;
                }
                if (configs.getOtherConfigKey().equals("ha_id")) {
                    haId = configs.getOtherConfigValue();
                }
            }
        }
        return haId;
    }

    public static List<String> getNodeIdsForSwitchNode(DataBroker db, Node switchNode) {
        List<String> haChildrenNodeIds = null;
        String haPSNodeId = switchNode.getNodeId().getValue();
        String haGlobalNodeId = getGlobalNodeIdFromSwitchNodeId(haPSNodeId);
        final InstanceIdentifier<Node> nodePath = createInstanceIdentifier(haGlobalNodeId);
        try {
            Optional<Node> haGlobalNode = readNode(db, LogicalDatastoreType.CONFIGURATION, nodePath);
            if (haGlobalNode.isPresent()) {
                haChildrenNodeIds = getNodeIdsFromOtherConfig(haGlobalNode.get());
                if (haChildrenNodeIds != null) {
                    final String psId = haPSNodeId.substring(
                            haPSNodeId.indexOf(HwvtepSouthboundConstants.PSWITCH_URI_PREFIX) - 1);
                    haChildrenNodeIds = Lists.transform(haChildrenNodeIds, new Function<String, String>() {
                        public String apply(String globalId) {
                            return globalId + psId;
                        }
                    });
                }
            }
        } catch (Exception e) {
        }
        return haChildrenNodeIds;
    }

    public static String getGlobalNodeIdFromSwitchNodeId(String psNodeId) {
        return psNodeId.substring(0, psNodeId.indexOf(HwvtepSouthboundConstants.PSWITCH_URI_PREFIX) - 1);
    }

    public static List<String> getNodeIdsFromOtherConfig(Node haNode) {
        List<String> haChildrenNodeIds = null;
        HwvtepGlobalAugmentation globalAugmentation = haNode.getAugmentation(HwvtepGlobalAugmentation.class);
        if (globalAugmentation != null) {
            List<Managers> managers = globalAugmentation.getManagers();
            if (managers != null && managers.size() > 0) {
                for (ManagerOtherConfigs configs : managers.get(0).getManagerOtherConfigs()) {
                    if (configs.getOtherConfigKey().equals("ha_children")) {
                        haChildrenNodeIds = Arrays.asList(configs.getOtherConfigValue().split(","));
                        break;
                    }
                }
            }
        }
        return haChildrenNodeIds;
    }

    public static List<String> getSwitchNodeIdsFromHANode(DataBroker db, Node haNode) {
        List<String> haSwitchNodeIds = null;
        try {
            Optional<Node> haNodeOperational = readNode(db, LogicalDatastoreType.OPERATIONAL,
                    createInstanceIdentifier(haNode.getNodeId().getValue()));
            if (haNodeOperational.isPresent()) {
                HwvtepGlobalAugmentation globalAugmentation =
                        haNodeOperational.get().getAugmentation(HwvtepGlobalAugmentation.class);
                if (globalAugmentation != null) {
                    List<Switches> switches = globalAugmentation.getSwitches();
                    if (switches != null && switches.size() > 0) {
                        haSwitchNodeIds = new ArrayList<>();
                        for (Switches physicalSwitch : switches) {
                            InstanceIdentifier<?> value = physicalSwitch.getSwitchRef().getValue();
                            haSwitchNodeIds.add(value.firstKeyOf(Node.class).getNodeId().getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return haSwitchNodeIds;
    }


    public static HwvtepLogicalSwitchRef getHwvtepLogicalSwitchRef(NodeId nodeId, HwvtepNodeName hwvtepNodeName) {
        InstanceIdentifier<LogicalSwitches> logicalSwitchIdentifier =
                InstanceIdentifier.create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class, new NodeKey(nodeId))
                        .augmentation(HwvtepGlobalAugmentation.class)
                        .child(LogicalSwitches.class, new LogicalSwitchesKey(hwvtepNodeName));
        return new HwvtepLogicalSwitchRef(logicalSwitchIdentifier);
    }

    public static HwvtepLogicalSwitchRef getLogicalSwitchRefForHaChild(NodeId nodeId,
                InstanceIdentifier<?> logicalSwitchRefIdentifier) {
        HwvtepNodeName hwvtepNodeName =
                logicalSwitchRefIdentifier.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName();
        return getHwvtepLogicalSwitchRef(nodeId, hwvtepNodeName);
    }

    public static HwvtepLogicalSwitchRef getLogicalSwitchRef(HwvtepLogicalSwitchRef src, InstanceIdentifier<Node> nodePath) {
        InstanceIdentifier<LogicalSwitches> srcId = (InstanceIdentifier<LogicalSwitches>)src.getValue();
        HwvtepNodeName switchName = srcId.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName();
        InstanceIdentifier<LogicalSwitches> iid = nodePath.augmentation(HwvtepGlobalAugmentation.class).
                child(LogicalSwitches.class, new LogicalSwitchesKey(switchName));
        String srcVal = src.getValue().firstKeyOf(Node.class).getNodeId().getValue();
        String dstVal = iid.firstKeyOf(Node.class).getNodeId().getValue();
        if (srcVal.equals(dstVal)) {
            //LOG.error("Failed to change node id value  ");
        }
        HwvtepLogicalSwitchRef ref = new HwvtepLogicalSwitchRef(iid);
        return ref;
    }


    public static HwvtepPhysicalLocatorRef getLocatorRef(HwvtepPhysicalLocatorRef src, InstanceIdentifier<Node> nodePath) {
        InstanceIdentifier<TerminationPoint> tpPath2 = (InstanceIdentifier<TerminationPoint>)src.getValue();
        TpId tpId = tpPath2.firstKeyOf(TerminationPoint.class).getTpId();
        InstanceIdentifier<TerminationPoint> tpPath =
                nodePath.child(TerminationPoint.class, new TerminationPointKey(tpId));
        HwvtepPhysicalLocatorRef locatorRef = new HwvtepPhysicalLocatorRef(tpPath);

        String srcVal = src.getValue().firstKeyOf(Node.class).getNodeId().getValue();
        String dstVal = tpPath.firstKeyOf(Node.class).getNodeId().getValue();
        //LOG.error("getLocatorRef before {} ",srcVal);
        //LOG.error("getLocatorRef after {} ",dstVal);
        if (srcVal.equals(dstVal)) {
            LOG.error("Failed to change node id value  ");
        }
        return locatorRef;
    }

    public static NodeBuilder getHAChildNodeBuilder(DataBroker db, InstanceIdentifier<Node> nodePath,
                NodeId childNodeId) throws InterruptedException, ExecutionException, ReadFailedException {
        NodeBuilder haChildNodeBuilder = new NodeBuilder();
        Optional<Node> haChildOrigNode = readNode(db, LogicalDatastoreType.CONFIGURATION, nodePath);
        if (haChildOrigNode.isPresent()) {
            haChildNodeBuilder = new NodeBuilder(haChildOrigNode.get());
            return haChildNodeBuilder;
        }
        haChildNodeBuilder.setNodeId(childNodeId);
        return haChildNodeBuilder;
    }

    public static HwvtepPhysicalLocatorRef getHwvtepPhysicalLocatorRef(NodeId nodeId, TpId tpId) {
        InstanceIdentifier<TerminationPoint> terminationPointIdentifier =
                InstanceIdentifier.create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class, new NodeKey(nodeId))
                        .child(TerminationPoint.class, new TerminationPointKey(tpId));
        return new HwvtepPhysicalLocatorRef(terminationPointIdentifier);
    }

    public static Set<String> getUpdatedNodeIds(List<String> newNodeIds, List<String> origNodeIds) {
        Set<String> haUpdatedNodeIdsSet = new HashSet<String>(newNodeIds);
        Set<String> haOriginalNodeIdsSet = new HashSet<String>(origNodeIds);
        return Sets.difference(haUpdatedNodeIdsSet, haOriginalNodeIdsSet);
    }

    public static  List<NodeId> getChildNodeIds(Optional<Node> haGlobalConfigNodeOptional) {
        List<NodeId> childNodeIds = Lists.newArrayList();
        if (!haGlobalConfigNodeOptional.isPresent()) {
            return childNodeIds;
        }
        HwvtepGlobalAugmentation augmentation =
                haGlobalConfigNodeOptional.get().getAugmentation(HwvtepGlobalAugmentation.class);
        if (augmentation != null && augmentation.getManagers() != null
                && augmentation.getManagers().size() > 0) {
            Managers managers = augmentation.getManagers().get(0);
            for (ManagerOtherConfigs otherConfigs : managers.getManagerOtherConfigs()) {
                if (otherConfigs.getOtherConfigKey().equals("ha_children")) {
                    String nodeIdsVal = otherConfigs.getOtherConfigValue();
                    if (nodeIdsVal != null) {
                        String parts[] = nodeIdsVal.split(",");
                        for (String part : parts) {
                            childNodeIds.add(new NodeId(part));
                        }
                    }

                }
            }
        }
        return childNodeIds;
    }

    static InstanceIdentifier<Node> getHAPsPath(InstanceIdentifier<Node> haNodePath, Node childPsNode) {
        String psIdVal = childPsNode.getNodeId().getValue();
        String psName = psIdVal.substring(psIdVal.indexOf("physicalswitch") + "physicalswitch".length() + 1);

        NodeId haNodeId = haNodePath.firstKeyOf(Node.class).getNodeId();
        String haPsNodeIdVal = haNodeId.getValue() + "/physicalswitch/" + psName;

        InstanceIdentifier<Node> haPsPath = createInstanceIdentifier(haPsNodeIdVal);
        return haNodePath;
    }

    static InstanceIdentifier<Node> getGlobalPathFromPSPath(InstanceIdentifier<Node> psNodPath) {
        String psNodePathVal = psNodPath.firstKeyOf(Node.class).getNodeId().getValue();
        String nodePathVal = psNodePathVal.substring(0, psNodePathVal.indexOf("/physicalswitch"));
        InstanceIdentifier<Node> nodePath = HAUtil.createInstanceIdentifier(nodePathVal);
        return nodePath;
    }

    public static boolean isEmptyList(List list) {
        if (list == null || list.size() == 0) {
            return true;
        }
        return false;
    }

    public static void mergeManagedByNode(Node psNode,
                                          PhysicalSwitchAugmentationBuilder builder,
                                          InstanceIdentifier<Node> haNodePath,
                                          InstanceIdentifier<Node> haPsPath, NodeId haPSNodeId) {
        PhysicalSwitchAugmentation psAugmentation = psNode.getAugmentation(PhysicalSwitchAugmentation.class);
        builder.setManagedBy(new HwvtepGlobalRef(haNodePath));
        builder.setHwvtepNodeName(psAugmentation.getHwvtepNodeName());
        builder.setHwvtepNodeDescription(psAugmentation.getHwvtepNodeDescription());
        builder.setTunnelIps(psAugmentation.getTunnelIps());
        builder.setPhysicalSwitchUuid(getUUid(psAugmentation.getHwvtepNodeName().getValue()));
        //builder.setManagementIps(null);
    }

    public static Node getOriginal(DataObjectModification<Node> mod) {
        Node node = null;
        switch(mod.getModificationType()) {
            case SUBTREE_MODIFIED:
                node = mod.getDataBefore();
                break;
            case WRITE:
                if(mod.getDataBefore() !=  null) {
                    node = mod.getDataBefore();
                }
                break;
            case DELETE:
                node = mod.getDataBefore();
                break;
            default:
                break;
        }
        return node;
    }

    public static Node getUpdated(DataObjectModification<Node> mod) {
        Node node = null;
        switch(mod.getModificationType()) {
            case SUBTREE_MODIFIED:
                node = mod.getDataAfter();
                break;
            case WRITE:
                if(mod.getDataAfter() !=  null) {
                    node = mod.getDataAfter();
                }
                break;
            default:
                break;
        }
        return node;
    }


    public static Node getCreated(DataObjectModification<Node> mod) {
        if((mod.getModificationType() == DataObjectModification.ModificationType.WRITE)
                && (mod.getDataBefore() == null)){
            return mod.getDataAfter();
        }
        return null;
    }

    public static Node getRemoved(DataObjectModification<Node> mod) {
        if(mod.getModificationType() == DataObjectModification.ModificationType.DELETE){
            return mod.getDataBefore();
        }
        return null;
    }
}
