/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalUpdateCommand.class);
    private Map<UUID, Global> updatedHwvtepRows =
                    TyperUtils.extractRowsUpdated(Global.class, getUpdates(),getDbSchema());
    private Map<UUID, Global> oldHwvtepRows =
                    TyperUtils.extractRowsUpdated(Global.class, getUpdates(),getDbSchema());

    public GlobalUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
                super(key, updates, dbSchema);
            }

    @Override
    public void execute(ReadWriteTransaction transaction) {

                for (Entry<UUID, Global> entry : updatedHwvtepRows.entrySet()) {
                    Global hwvtepGlobal = entry.getValue();
                    Global oldEntry = oldHwvtepRows.get(entry.getKey());
                    final InstanceIdentifier<Node> nodePath = getInstanceIdentifier(hwvtepGlobal);
                    LOG.trace("Processing hardware_vtep update for nodePath: {}", nodePath);

                    HwvtepGlobalAugmentationBuilder hwvtepGlobalBuilder = new HwvtepGlobalAugmentationBuilder();
                    hwvtepGlobalBuilder.setConnectionInfo(getConnectionInfo());
                    NodeBuilder nodeBuilder = new NodeBuilder();
                    nodeBuilder.setNodeId(getNodeId(hwvtepGlobal));
                    nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, hwvtepGlobalBuilder.build());
                    transaction.merge(LogicalDatastoreType.OPERATIONAL, nodePath, nodeBuilder.build());

                }
    }

    private InstanceIdentifier<Node> getInstanceIdentifier(Global hwvtep) {
        InstanceIdentifier<Node> iid = getOvsdbConnectionInstance().getInstanceIdentifier();
        if(iid == null) {
            LOG.warn("InstanceIdentifier was null when it shouldn't be");
            /* This can be case for switch initiated connection */
            iid = HwvtepSouthboundMapper.getInstanceIdentifier(hwvtep);
            getOvsdbConnectionInstance().setInstanceIdentifier(iid);
        }
        return getOvsdbConnectionInstance().getInstanceIdentifier();
    }

    private NodeId getNodeId(Global hwvtep) {
        NodeKey nodeKey = getInstanceIdentifier(hwvtep).firstKeyOf(Node.class, NodeKey.class);
        return nodeKey.getNodeId();
    }
}
