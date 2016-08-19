package org.opendaylight.ovsdb.hwvtepsouthbound.ha.state;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.handlers.MergerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class D1ReConnectedHandler implements HAStateHandler {

    static final Logger LOG = LoggerFactory.getLogger(D1ReConnectedHandler.class);

    @Override
    public void handle(HAContext ctx, ReadWriteTransaction tx) {
        try {
            LOG.error("D1 reconnected tid {} haId {} nodeId {}", HAUtil.getId(),
                    ctx.getHaId(), ctx.nodeIdOptional.get().getValue());

            LOG.error("D1 reconnected tid {} copy operational from node {} haId {}",HAUtil.getId(),
                    ctx.nodeIdOptional.get().getValue(),
                    ctx.getHaId());
            LOG.error("D1 reconnected tid {} copy switch operational from node {} haId {}",HAUtil.getId(),
                    ctx.nodeIdOptional.get().getValue(),
                    ctx.getHaId());

            MergerHandler.getHandler().mergeChildGlobalOperationalToHA(ctx.node, ctx.haNodePath, tx);
            MergerHandler.getHandler().mergeChildSwitchOperationalToHA(ctx.psNode, ctx.haNodePath, ctx.haPsPath, tx);

            MergerHandler.getHandler().mergeHAGlobalConfigToChild(ctx.haGlobalConfigNodeOptional.get(),
                    ctx.nodePath, tx);
            D2ConnectedHandler.scheduleCopySwitchConfigToChild(ctx, tx);

        } catch (Exception e) {
            //LOG.error("Failed to handle ",e);
            throw new RuntimeException(e);
        }
    }
}
