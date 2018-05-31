/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import com.google.common.base.Optional;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.AutoAttach;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Autoattach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.AutoattachKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbAutoAttachRemovedCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbAutoAttachRemovedCommand.class);

    private Map<UUID, AutoAttach> removedAutoAttachRows;

    public OvsdbAutoAttachRemovedCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        removedAutoAttachRows = TyperUtils.extractRowsRemoved(AutoAttach.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (removedAutoAttachRows == null || removedAutoAttachRows.isEmpty()) {
            return;
        }

        final InstanceIdentifier<Node> nodeIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, nodeIId);
        if (ovsdbNode.isPresent()) {
            final InstanceIdentifier<Node> ovsdbNodeIid =
                    SouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance().getNodeId());
            // FIXME: Iterate on external_ids instead of uuid when Open vSwitch supports external_ids column
            for (final UUID autoAttachUuid : removedAutoAttachRows.keySet()) {
                final AutoattachKey autoAttachKey = getAutoAttachKeyToRemove(ovsdbNode.get(), autoAttachUuid);
                if (autoAttachKey != null) {
                    final InstanceIdentifier<Autoattach> iid = ovsdbNodeIid
                            .augmentation(OvsdbNodeAugmentation.class)
                            .child(Autoattach.class, autoAttachKey);
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, iid);
                    LOG.debug("AutoAttach table {} for Ovsdb Node {} is deleted", autoAttachUuid.toString(),
                            ovsdbNode.get().getNodeId());
                } else {
                    LOG.warn("AutoAttach table {} not found for Ovsdb Node {} to delete", autoAttachUuid.toString(),
                            ovsdbNode.get().getNodeId());
                }
            }
        }
    }

    private AutoattachKey getAutoAttachKeyToRemove(Node node, UUID autoAttachUuid) {
        final List<Autoattach> autoAttachList = node.augmentation(OvsdbNodeAugmentation.class).getAutoattach();
        if (autoAttachList == null || autoAttachList.isEmpty()) {
            return null;
        }
        for (final Autoattach autoAttach : autoAttachList) {
            if (autoAttach.getAutoattachUuid()
                    .equals(new Uuid(autoAttachUuid.toString()))) {
                return autoAttach.key();
            }
        }
        return null;
    }
}
