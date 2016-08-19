package org.opendaylight.ovsdb.hwvtepsouthbound.ha.handlers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.listeners.HAOpNodeListener;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.TunnelUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.VlanBindingsUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.SwitchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.SwitchesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.ComparatorUtils.translateOperationalAdd;

public class SwitchNodeHandler {


    static Logger LOG = LoggerFactory.getLogger(HAOpNodeListener.class);

    public static void mergeChildSwitchOperationalToHA(
            Node psNode,
            InstanceIdentifier<Node> haNodePath,
            InstanceIdentifier<Node> haPsPath,
            ReadWriteTransaction tx) throws Exception {
        mergePsNodes(psNode, haNodePath, haPsPath, OPERATIONAL, tx);
        updateHaGlobalOperationalNodeWithSwitchesInfo(psNode, haNodePath, haPsPath, tx);

    }

    public static void updateHaGlobalOperationalNodeWithSwitchesInfo(Node psNode,
                                                                     InstanceIdentifier<Node> haNodePath,
                                                                     InstanceIdentifier<Node> haPsPath,
                                                                     ReadWriteTransaction tx)
            throws InterruptedException, ExecutionException, ReadFailedException {

        Optional<Node> nodeOptional = tx.read(OPERATIONAL, haNodePath).checkedGet();
        HwvtepGlobalAugmentation globalAugmentation = null;
        HwvtepGlobalAugmentationBuilder globalAugmentationBuilder = null;
        List<Switches> switches  = Lists.newArrayList();

        if (nodeOptional.isPresent()) {
            Node haNode = nodeOptional.get();
            globalAugmentation = haNode.getAugmentation(HwvtepGlobalAugmentation.class);
            globalAugmentationBuilder = new HwvtepGlobalAugmentationBuilder(globalAugmentation);
            if (globalAugmentation != null && globalAugmentation.getSwitches() != null) {
                switches.addAll(globalAugmentation.getSwitches());
            }
        } else {
            globalAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        }
        SwitchesBuilder switchesBuilder = new SwitchesBuilder();
        switchesBuilder.setKey(new SwitchesKey(new HwvtepPhysicalSwitchRef(haPsPath)));
        switchesBuilder.setSwitchRef(new HwvtepPhysicalSwitchRef(haPsPath));
        switches.add(switchesBuilder.build());

        globalAugmentationBuilder.setSwitches(switches);
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(haNodePath.firstKeyOf(Node.class).getNodeId());

        HwvtepGlobalAugmentation q = globalAugmentationBuilder.build();
        nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, globalAugmentationBuilder.build());
        tx.merge(OPERATIONAL, haNodePath, nodeBuilder.build());
        //LOG.error("updated global node switches {}", q.getSwitches().size());
    }

    public static void mergeHASwitchConfigToChild(Optional<Node> haPsConfigNodeOptional,
                                                  InstanceIdentifier<Node> psNodPath,
                                                  InstanceIdentifier<Node> nodePath,
                                                  ReadWriteTransaction tx)
            throws InterruptedException, ExecutionException, ReadFailedException {
        if (haPsConfigNodeOptional.isPresent()) {
            Node haPsConfigNode = haPsConfigNodeOptional.get();

            Optional<Node> psConfigNodeOptional = tx.read(CONFIGURATION, psNodPath).checkedGet();
            if (!psConfigNodeOptional.isPresent()) {
                //LOG.error("ps config node not present copy from ha config ps config node");
                mergePsNodes(haPsConfigNode, nodePath, psNodPath, CONFIGURATION, tx);
            } else {
                //LOG.error("PS config node already present ");
            }
        } else {
        }

    }

    public static void mergePsNodes(Node srcNode,
                                    InstanceIdentifier<Node> dstNodePath,
                                    InstanceIdentifier<Node> dstPsNodePath,
                                    LogicalDatastoreType datastoreType,
                                    ReadWriteTransaction tx) {
        //LOG.error("mergePsNodes copy the operational ps node to HA");

        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId dstNodeId =  dstNodePath.firstKeyOf(Node.class).getNodeId();
        NodeId dstPsNodeId = dstPsNodePath.firstKeyOf(Node.class).getNodeId();
        nodeBuilder.setNodeId(dstPsNodePath.firstKeyOf(Node.class).getNodeId());

        PhysicalSwitchAugmentation physicalSwitchAugmentation =
                srcNode.getAugmentation(PhysicalSwitchAugmentation.class);
        PhysicalSwitchAugmentationBuilder physicalSwitchAugmentationBuilder =
                new PhysicalSwitchAugmentationBuilder();

        //no connection info
        //hwvtepGlobalBuilder.setConnectionInfo(globalAugmentation.getConnectionInfo());

        //TODO check switches

        //haGlobalBuilder.setSwitches(globalAugmentation.getSwitches());

        HwvtepPhysicalPortAugmentationBuilder physicalPortAugmentationBuilder =
                new HwvtepPhysicalPortAugmentationBuilder();

        HAUtil.mergeManagedByNode(srcNode, physicalSwitchAugmentationBuilder, dstNodePath, dstPsNodePath, dstPsNodeId);

        TunnelUtil.mergeTunnels(srcNode, nodeBuilder, physicalSwitchAugmentationBuilder,
                physicalPortAugmentationBuilder, dstNodePath, dstPsNodePath, dstNodeId, dstPsNodeId);


        nodeBuilder.setTerminationPoint(translateOperationalAdd(nodeBuilder.getTerminationPoint(),
                srcNode.getTerminationPoint(),
                new VlanBindingsUtil.VlanBindingsTransformer(dstNodePath), new VlanBindingsUtil.VlanBindingsComparator()));


        nodeBuilder.addAugmentation(PhysicalSwitchAugmentation.class, physicalSwitchAugmentationBuilder.build());
        Node dstNode = nodeBuilder.build();
        tx.merge(datastoreType, dstPsNodePath, dstNode, true);
    }

}
