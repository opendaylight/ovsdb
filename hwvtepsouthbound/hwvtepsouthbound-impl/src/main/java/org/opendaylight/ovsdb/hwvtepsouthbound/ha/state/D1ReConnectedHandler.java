/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
            LOG.error("D1 reconnected haId {} nodeId {}",
                    ctx.getHaId(), ctx.nodeIdOptional.get().getValue());

            LOG.error("D1 reconnected copy operational from node {} haId {}",
                    ctx.nodeIdOptional.get().getValue(),
                    ctx.getHaId());
            LOG.error("D1 reconnected copy switch operational from node {} haId {}",
                    ctx.nodeIdOptional.get().getValue(),
                    ctx.getHaId());

            D1ConnectedHandler.buildGlobalConfigHANode(ctx, tx);
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
