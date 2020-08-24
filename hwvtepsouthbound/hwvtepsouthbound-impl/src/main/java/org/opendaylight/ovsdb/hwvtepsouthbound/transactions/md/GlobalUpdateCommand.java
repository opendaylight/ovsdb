/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSchemaConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
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
    private final Map<UUID, Global> updatedHwvtepRows =
            TyperUtils.extractRowsUpdated(Global.class, getUpdates(), getDbSchema());

    public GlobalUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Global hwvtepGlobal : updatedHwvtepRows.values()) {
            final InstanceIdentifier<Node> nodePath = getInstanceIdentifier(hwvtepGlobal);
            LOG.trace("Processing hardware_vtep update for nodePath: {}", nodePath);

            HwvtepGlobalAugmentationBuilder hwvtepGlobalBuilder = new HwvtepGlobalAugmentationBuilder();
            try {
                Version version = getOvsdbConnectionInstance().getSchema(
                        HwvtepSchemaConstants.HARDWARE_VTEP).get().getVersion();
                hwvtepGlobalBuilder.setDbVersion(version.toString());
            } catch (InterruptedException | ExecutionException e) {
                LOG.debug("Failed to get schema version on {} due to {}",
                        getOvsdbConnectionInstance().getConnectionInfo(), e.getMessage());
            }
            hwvtepGlobalBuilder.setConnectionInfo(getConnectionInfo());
            NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setNodeId(getNodeId(hwvtepGlobal));
            HwvtepGlobalAugmentation hwvtepGlobalAugmentation = hwvtepGlobalBuilder.build();
            nodeBuilder.addAugmentation(hwvtepGlobalAugmentation);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, nodePath, nodeBuilder.build());
            getOvsdbConnectionInstance().setHwvtepGlobalAugmentation(hwvtepGlobalAugmentation);
            addToDeviceUpdate(TransactionType.ADD, hwvtepGlobal);
            LOG.info("DEVICE - {} {}", TransactionType.ADD, hwvtepGlobal);
        }
    }

    private InstanceIdentifier<Node> getInstanceIdentifier(Global hwvtep) {
        InstanceIdentifier<Node> iid = getOvsdbConnectionInstance().getInstanceIdentifier();
        if (iid == null) {
            LOG.warn("InstanceIdentifier was null when it shouldn't be");
            /* This can be case for switch initiated connection */
            iid = HwvtepSouthboundMapper.getInstanceIdentifier(hwvtep);
            getOvsdbConnectionInstance().setInstanceIdentifier(iid);
        }
        return getOvsdbConnectionInstance().getInstanceIdentifier();
    }

    private NodeId getNodeId(Global hwvtep) {
        NodeKey nodeKey = getInstanceIdentifier(hwvtep).firstKeyOf(Node.class);
        return nodeKey.getNodeId();
    }
}
