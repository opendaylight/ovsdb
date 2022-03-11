/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepLogicalSwitchUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepLogicalSwitchUpdateCommand.class);

    private final Map<UUID, LogicalSwitch> updatedLSRows;

    public HwvtepLogicalSwitchUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedLSRows = TyperUtils.extractRowsUpdated(LogicalSwitch.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Entry<UUID, LogicalSwitch> entry : updatedLSRows.entrySet()) {
            updateLogicalSwitch(transaction, entry.getValue());
        }
    }

    private void updateLogicalSwitch(ReadWriteTransaction transaction, LogicalSwitch logicalSwitch) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            Node connectionNode = buildConnectionNode(logicalSwitch);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
            InstanceIdentifier<LogicalSwitches> switchIid = getOvsdbConnectionInstance().getInstanceIdentifier()
                    .augmentation(HwvtepGlobalAugmentation.class)
                    .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName(logicalSwitch.getName())));
            addToUpdateTx(LogicalSwitches.class, switchIid, logicalSwitch.getUuid(), logicalSwitch);
            addToDeviceUpdate(TransactionType.ADD, logicalSwitch);
            // TODO: Delete entries that are no longer needed
        }
    }

    private Node buildConnectionNode(LogicalSwitch logicalSwitch) {
        //Update node with LogicalSwitch reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        LogicalSwitchesBuilder lsBuilder = new LogicalSwitchesBuilder();
        lsBuilder.setLogicalSwitchUuid(new Uuid(logicalSwitch.getUuid().toString()));
        lsBuilder.setHwvtepNodeDescription(logicalSwitch.getDescription());
        HwvtepGlobalAugmentation hwvtepGlobalAugmentation = getOvsdbConnectionInstance().getHwvtepGlobalAugmentation();
        if (hwvtepGlobalAugmentation != null) {
            Version minVersion = Version.fromString("1.6.0");
            Version dbVersion = Version.fromString(hwvtepGlobalAugmentation.getDbVersion());
            if (dbVersion.compareTo(minVersion) >= 0) {
                if (logicalSwitch.getReplicationModeColumn().getData() != null
                        && !logicalSwitch.getReplicationModeColumn().getData().isEmpty()) {
                    lsBuilder.setReplicationMode(logicalSwitch.getReplicationModeColumn().getData().iterator().next());
                    LOG.debug("setReplicationMode to: {}",
                            logicalSwitch.getReplicationModeColumn().getData().iterator().next());
                }
            }
        }
        HwvtepNodeName hwvtepName = new HwvtepNodeName(logicalSwitch.getName());
        lsBuilder.setHwvtepNodeName(hwvtepName);
        lsBuilder.withKey(new LogicalSwitchesKey(hwvtepName));
        if (logicalSwitch.getTunnelKeyColumn().getData() != null
                && !logicalSwitch.getTunnelKeyColumn().getData().isEmpty()) {
            lsBuilder.setTunnelKey(logicalSwitch.getTunnelKeyColumn().getData().iterator().next().toString());
        }

        return connectionNode
            .addAugmentation(new HwvtepGlobalAugmentationBuilder()
                .setLogicalSwitches(BindingMap.of(lsBuilder.build()))
                .build())
            .build();
    }
}
