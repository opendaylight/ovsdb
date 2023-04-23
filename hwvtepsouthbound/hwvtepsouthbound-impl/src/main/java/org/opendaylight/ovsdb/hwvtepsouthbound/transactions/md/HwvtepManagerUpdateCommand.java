/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.Manager;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ManagersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.managers.ManagerOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.managers.ManagerOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.managers.ManagerOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepManagerUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepManagerUpdateCommand.class);

    private final Map<UUID, Manager> updatedMgrRows;
    private final Map<UUID, Manager> oldMgrRows;

    public HwvtepManagerUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedMgrRows = TyperUtils.extractRowsUpdated(Manager.class, getUpdates(), getDbSchema());
        oldMgrRows = TyperUtils.extractRowsOld(Manager.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Manager manager : updatedMgrRows.values()) {
            updateManager(transaction, manager);
        }
    }

    private void updateManager(ReadWriteTransaction transaction, Manager manager) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            LOG.debug("Connection {} is present", connection.orElseThrow());
            Node connectionNode = buildConnectionNode(manager);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
            addToDeviceUpdate(TransactionType.ADD, manager);
            LOG.info("DEVICE - {} {}", TransactionType.ADD, manager);
            // TODO: Delete entries that are no longer needed
        }
    }

    private Node buildConnectionNode(Manager manager) {
        // Update node with Manager reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        ManagersBuilder managersBuilder = new ManagersBuilder();
        if (manager.getTargetColumn().getData() != null && !manager.getTargetColumn().getData().isEmpty()) {
            managersBuilder.setTarget(new Uri(manager.getTargetColumn().getData()));
        }
        if (manager.getIsConnectedColumn().getData() != null) {
            managersBuilder.setIsConnected(manager.getIsConnectedColumn().getData());
        }
        ManagerOtherConfigsBuilder mocBuilder = new ManagerOtherConfigsBuilder();
        if (manager.getOtherConfigColumn().getData() != null && !manager.getOtherConfigColumn().getData().isEmpty()) {
            var mocList = BindingMap.<ManagerOtherConfigsKey, ManagerOtherConfigs>orderedBuilder();
            Map<String, String> ocList = manager.getOtherConfigColumn().getData();
            for (Entry<String, String> otherConfigEntry : ocList.entrySet()) {
                mocBuilder.setOtherConfigKey(otherConfigEntry.getKey());
                mocBuilder.setOtherConfigValue(otherConfigEntry.getValue());
                mocList.add(mocBuilder.build());
            }
            managersBuilder.setManagerOtherConfigs(mocList.build());
        }
        managersBuilder.setManagerUuid(new Uuid(manager.getUuid().toString()));

        return connectionNode
            .addAugmentation(new HwvtepGlobalAugmentationBuilder()
                .setManagers(BindingMap.of(managersBuilder.build()))
                .build())
            .build();
        // TODO Deletion of other config
    }
}


