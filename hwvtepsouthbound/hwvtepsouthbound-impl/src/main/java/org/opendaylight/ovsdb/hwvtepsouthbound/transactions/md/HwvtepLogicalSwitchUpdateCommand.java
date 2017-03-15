/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
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

import com.google.common.base.Optional;

public class HwvtepLogicalSwitchUpdateCommand extends AbstractTransactionCommand {

    private Map<UUID, LogicalSwitch> updatedLSRows;

    public HwvtepLogicalSwitchUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedLSRows = TyperUtils.extractRowsUpdated(LogicalSwitch.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Entry<UUID, LogicalSwitch> entry : updatedLSRows.entrySet()) {
            updateLogicalSwitch(transaction, entry.getValue());
        }
    }

    private void updateLogicalSwitch(ReadWriteTransaction transaction, LogicalSwitch lSwitch) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            Node connectionNode = buildConnectionNode(lSwitch);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
            InstanceIdentifier<LogicalSwitches> switchIid = getOvsdbConnectionInstance().getInstanceIdentifier()
                    .augmentation(HwvtepGlobalAugmentation.class)
                    .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName(lSwitch.getName())));
            getOvsdbConnectionInstance().getDeviceInfo().updateDeviceOperData(LogicalSwitches.class, switchIid,
                    lSwitch.getUuid(), lSwitch);
            // TODO: Delete entries that are no longer needed
        }
    }

    private Node buildConnectionNode(LogicalSwitch lSwitch) {
        //Update node with LogicalSwitch reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        List<LogicalSwitches> lSwitches = new ArrayList<>();
        LogicalSwitchesBuilder lsBuilder = new LogicalSwitchesBuilder();
        lsBuilder.setLogicalSwitchUuid(new Uuid(lSwitch.getUuid().toString()));
        lsBuilder.setHwvtepNodeDescription(lSwitch.getDescription());
        HwvtepNodeName hwvtepName = new HwvtepNodeName(lSwitch.getName());
        lsBuilder.setHwvtepNodeName(hwvtepName);
        lsBuilder.setKey(new LogicalSwitchesKey(hwvtepName));
        if (lSwitch.getTunnelKeyColumn().getData() != null && !lSwitch.getTunnelKeyColumn().getData().isEmpty()) {
            lsBuilder.setTunnelKey(lSwitch.getTunnelKeyColumn().getData().iterator().next().toString());
        }
        lSwitches.add(lsBuilder.build());
        hgAugmentationBuilder.setLogicalSwitches(lSwitches);
        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());
        return connectionNode.build();
    }

}
