package org.opendaylight.ovsdb.hwvtepsouthbound.ha.state;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.handlers.MergerHandler;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Managers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ManagersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ManagersKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.managers.ManagerOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

public class D1ConnectedHandler implements HAStateHandler {

    static final Logger LOG = LoggerFactory.getLogger(D1ConnectedHandler.class);

    public static List<Managers> buildManagersForHANode(Optional<Node> haGlobalConfigNodeOptional,
                                                        Set<NodeId> nodeIds) {

        List<NodeId> childNodeIds = HAUtil.getChildNodeIds(haGlobalConfigNodeOptional);
        nodeIds.addAll(childNodeIds);

        ManagersBuilder builder1 = new ManagersBuilder();

        builder1.setKey(new ManagersKey(new Uri("test")));
        List<Managers> managers = Lists.newArrayList();
        List<ManagerOtherConfigs> otherConfigses = Lists.newArrayList();
        StringBuffer stringBuffer = new StringBuffer();
        for (NodeId nodeId : nodeIds) {
            stringBuffer.append(nodeId.getValue());
            stringBuffer.append(",");
        }

        String children = stringBuffer.substring(0, stringBuffer.toString().length() - 1);

        otherConfigses.add(HAUtil.getOtherConfig("ha_children", children ));
        builder1.setManagerOtherConfigs(otherConfigses);
        managers.add(builder1.build());
        return managers;
    }

    public static void buildGlobalConfigHANode(HAContext ctx,
                                               ReadWriteTransaction tx){

        NodeBuilder nodeBuilder = new NodeBuilder();
        HwvtepGlobalAugmentationBuilder hwvtepGlobalBuilder = new HwvtepGlobalAugmentationBuilder();

        if (ctx.globalConfigNodeOptional.isPresent()) {
            HwvtepGlobalAugmentation augmentation =
                    ctx.globalConfigNodeOptional.get().getAugmentation(HwvtepGlobalAugmentation.class);
            if (augmentation != null) {
                hwvtepGlobalBuilder = new HwvtepGlobalAugmentationBuilder(augmentation);
            }
        }
        NodeId nodeId = ctx.node.getNodeId();
        Set<NodeId> nodeIds = Sets.newHashSet();
        nodeIds.add(nodeId);

        hwvtepGlobalBuilder.setManagers(buildManagersForHANode(ctx.haGlobalConfigNodeOptional, nodeIds));

        nodeBuilder.setNodeId(ctx.haNodePath.firstKeyOf(Node.class).getNodeId());

        HwvtepGlobalAugmentation globalAugmentation = hwvtepGlobalBuilder.build();
        nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, globalAugmentation);
        Node haNode = nodeBuilder.build();
        tx.merge(CONFIGURATION, ctx.haNodePath, haNode, true);
    }

    @Override
    public void handle(HAContext ctx, ReadWriteTransaction tx) {
        try {
            LOG.error("D1 connected id {} haId {} nodeId {}",HAUtil.getId(),
                    ctx.getHaId(), ctx.nodeIdOptional.get().getValue());
            LOG.error("D1 connected tid {} build global config node managers for haId {}",HAUtil.getId(),
                    ctx.getHaId());
            LOG.error("D1 connected tid {} copy operational from node {} haId {}",HAUtil.getId(),
                    ctx.nodeIdOptional.get().getValue(),
                    ctx.getHaId());
            LOG.error("D1 connected tid {} copy switch operational from node {} haId {}",HAUtil.getId(),
                    ctx.nodeIdOptional.get().getValue(),
                    ctx.getHaId());

            buildGlobalConfigHANode(ctx, tx);
            MergerHandler.getHandler().mergeChildGlobalOperationalToHA(ctx.node, ctx.haNodePath, tx);
            MergerHandler.getHandler().mergeChildSwitchOperationalToHA(ctx.psNode, ctx.haNodePath, ctx.haPsPath, tx);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
