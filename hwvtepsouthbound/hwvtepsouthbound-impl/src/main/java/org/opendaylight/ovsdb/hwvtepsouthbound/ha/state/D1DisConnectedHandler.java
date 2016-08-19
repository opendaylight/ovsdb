package org.opendaylight.ovsdb.hwvtepsouthbound.ha.state;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

public class D1DisConnectedHandler implements HAStateHandler {

    static final Logger LOG = LoggerFactory.getLogger(D1DisConnectedHandler.class);

    public static void deleteHANodeIfChildrenAreDeleted(
            String haId,
            InstanceIdentifier<Node> haNodePath,
            Optional<Node> haGlobalConfigNodeOptional,
            Optional<Node> haGlobalOperationalNodeOptinal,
            ReadWriteTransaction tx) throws Exception {

        Boolean allChildMissing = true;
        if (!haGlobalConfigNodeOptional.isPresent()) {
            return;
        }
        List<NodeId> nodeIds = HAUtil.getChildNodeIds(haGlobalConfigNodeOptional);
        for (NodeId nodeId : nodeIds) {
            InstanceIdentifier<Node> childPath = HAUtil.createInstanceIdentifier(nodeId.getValue());
            if (HAUtil.readNode(tx, OPERATIONAL, childPath).isPresent()) {
                allChildMissing = false;
                return;
            }
        }
        if (haGlobalOperationalNodeOptinal.isPresent()) {
            LOG.error("D1 disconnect tId {} deleted haId {}", HAUtil.getId(), haId);
            Node haGlobalOpNode = haGlobalOperationalNodeOptinal.get();
            HwvtepGlobalAugmentation augmentation = haGlobalOpNode.getAugmentation(HwvtepGlobalAugmentation.class);
            if (augmentation != null && augmentation.getSwitches() != null) {
                for (Switches switches : augmentation.getSwitches()) {
                    tx.delete(OPERATIONAL, switches.getSwitchRef().getValue());
                }
            }
            tx.delete(OPERATIONAL, haNodePath);
        }
    }

    @Override
    public void handle(HAContext ctx, ReadWriteTransaction tx) {
        try {
            LOG.error("D1 disconnect tId {} haId {} nodeId {}", HAUtil.getId(),
                    ctx.getHaId(), ctx.nodeIdOptional.get().getValue());

            deleteHANodeIfChildrenAreDeleted(ctx.getHaId(), ctx.haNodePath,
                    ctx.haGlobalConfigNodeOptional, ctx.haGlobalOperNodeOptional, tx);
        } catch (Exception e) {
            //LOG.error("Failed to handle ",e);
            throw new RuntimeException(e);
        }
    }
}
