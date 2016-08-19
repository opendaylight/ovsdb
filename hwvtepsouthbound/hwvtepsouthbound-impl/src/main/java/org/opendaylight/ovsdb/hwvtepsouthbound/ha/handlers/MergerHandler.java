/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.handlers;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MergerHandler implements MergerInterface {

    public static MergerHandler mergerHandler = new MergerHandler();
    public static MergerHandler getHandler() {
        return mergerHandler;
    }
    @Override
    public void mergeChildSwitchOperationalToHA(Node psNode, InstanceIdentifier<Node> haNodePath,
                                                InstanceIdentifier<Node> haPsPath, ReadWriteTransaction tx) throws Exception {
        SwitchNodeHandler.mergeChildSwitchOperationalToHA(psNode, haNodePath, haPsPath, tx);
    }

    @Override
    public void mergeChildGlobalOperationalToHA(Node srcNode, InstanceIdentifier<Node> dstNodePath,
                                                ReadWriteTransaction tx) throws Exception {
        GlobalNodeHandler.mergeChildGlobalOperationalToHA(srcNode, dstNodePath, tx);
    }


    @Override
    public void mergeHASwitchConfigToChild(Optional<Node> haPsConfigNodeOptional,
                                           InstanceIdentifier<Node> psNodPath,
                                           InstanceIdentifier<Node> nodePath,
                                           ReadWriteTransaction tx) throws Exception {
        SwitchNodeHandler.mergeHASwitchConfigToChild(haPsConfigNodeOptional, psNodPath,
                nodePath, tx);
    }

    @Override
    public void mergeHAGlobalConfigToChild(Node srcNode, InstanceIdentifier<Node> dstNodePath,
                                           ReadWriteTransaction tx) throws Exception {
        GlobalNodeHandler.mergeHAGlobalConfigToChild(srcNode, dstNodePath, tx);
    }
}
