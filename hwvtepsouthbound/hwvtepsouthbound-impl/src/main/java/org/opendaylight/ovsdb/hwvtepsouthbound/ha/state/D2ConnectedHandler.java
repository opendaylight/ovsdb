package org.opendaylight.ovsdb.hwvtepsouthbound.ha.state;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.handlers.MergerHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class D2ConnectedHandler implements HAStateHandler {

    static final Logger LOG = LoggerFactory.getLogger(D2ConnectedHandler.class);

    ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    @Override
    public void handle(final HAContext ctx, final ReadWriteTransaction tx) {
        try {
            LOG.error("D2 connected tid {} haId {} nodeId {}", HAUtil.getId(),
                    ctx.getHaId(), ctx.nodeIdOptional.get().getValue());

            LOG.error("D2 connected tid {} build global config node managers for haId {}",HAUtil.getId(),
                    ctx.getHaId());
            LOG.error("D2 connected tid {} copy operational from node {} haId {}",HAUtil.getId(),
                    ctx.nodeIdOptional.get().getValue(),
                    ctx.getHaId());
            LOG.error("D2 connected tid {} copy switch operational from node {} haId {}",HAUtil.getId(),
                    ctx.nodeIdOptional.get().getValue(),
                    ctx.getHaId());

            LOG.error("D2 connected tid {} copy config from haId {} to {}",HAUtil.getId(),
                    ctx.getHaId(),
                    ctx.nodeIdOptional.get().getValue());

            D1ConnectedHandler.buildGlobalConfigHANode(ctx, tx);
            MergerHandler.getHandler().mergeHAGlobalConfigToChild(ctx.haGlobalConfigNodeOptional.get(),
                    ctx.nodePath, tx);
            scheduleCopySwitchConfigToChild(ctx, tx);

            MergerHandler.getHandler().mergeChildGlobalOperationalToHA(ctx.node, ctx.haNodePath, tx);
            MergerHandler.getHandler().mergeChildSwitchOperationalToHA(ctx.psNode, ctx.haNodePath, ctx.haPsPath, tx);
        } catch (Exception e) {
            //LOG.error("Failed to handle ",e);
            throw new RuntimeException(e);
        }
    }

    public static void scheduleCopySwitchConfigToChild(HAContext ctx, ReadWriteTransaction tx) throws Exception {
        Optional<Node> childConfigNode = HAUtil.readNode(tx, LogicalDatastoreType.CONFIGURATION, ctx.nodePath);
        Optional<Node> childOpNode = HAUtil.readNode(tx, LogicalDatastoreType.CONFIGURATION, ctx.nodePath);
        Set<String> configLogicalSwitches = new HashSet<>(getLogicalSwitches(childConfigNode));
        Set<String> opLogicalSwitches = new HashSet<>(getLogicalSwitches(childConfigNode));
        Set<String> logicalSwitchesToBeAdded = Sets.difference(configLogicalSwitches, opLogicalSwitches);
        if (logicalSwitchesToBeAdded.size() > 0) {
            LOG.error("D2 connected schedule copy of switch config from haId {} to {}",
                    ctx.getHaId(),
                    ctx.nodeIdOptional.get().getValue());

            logicalSwitchWaitingNodes.put(ctx.node.getNodeId().getValue(), logicalSwitchesToBeAdded);
        }
    }
    private static List<String> getLogicalSwitches(Optional<Node> node) {
        if (node != null && node.isPresent()) {
            HwvtepGlobalAugmentation augmentation = node.get().getAugmentation(HwvtepGlobalAugmentation.class);
            if (augmentation != null && augmentation.getLogicalSwitches() != null) {
                return Lists.transform(augmentation.getLogicalSwitches(), new Function<LogicalSwitches, String>() {
                    @Nullable
                    @Override
                    public String apply(LogicalSwitches logicalSwitches) {
                        return logicalSwitches.getHwvtepNodeName().getValue();
                    }
                });
            }
        }
        return Lists.newArrayList();
    }

    static ConcurrentHashMap<String,Set<String>> logicalSwitchWaitingNodes = new ConcurrentHashMap<>();

    public static void onChildGlobalOperationalUpdate(Node updated, Node original,
                                                      InstanceIdentifier<Node> haPath,
                                                      HAContext ctx, ReadWriteTransaction tx) throws Exception {
        Set<String> logicalSwitches =  logicalSwitchWaitingNodes.get(updated.getNodeId().getValue());
        if (logicalSwitches != null) {
            LOG.error("D2 connected copy switch config from haId {} to {}",
                    ctx.getHaId(),
                    ctx.nodeIdOptional.get().getValue());
            MergerHandler.getHandler().mergeHASwitchConfigToChild(ctx.haSwitchConfigNodeOptional,
                    ctx.haNodePath, ctx.haPsPath, tx);
        }
    }
}
