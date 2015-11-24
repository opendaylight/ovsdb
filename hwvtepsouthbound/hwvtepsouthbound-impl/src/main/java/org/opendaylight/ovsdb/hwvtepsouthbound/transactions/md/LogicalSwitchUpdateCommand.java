/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.SwitchesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class LogicalSwitchUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LogicalSwitchUpdateCommand.class);
    private Map<UUID, LogicalSwitch> updatedLSRows;
    private Map<UUID, LogicalSwitch> oldLSRows;

    public LogicalSwitchUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedLSRows = TyperUtils.extractRowsUpdated(LogicalSwitch.class, getUpdates(),getDbSchema());
        oldLSRows = TyperUtils.extractRowsOld(LogicalSwitch.class, getUpdates(),getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if(updatedLSRows != null && !updatedLSRows.isEmpty()) {
            for (Entry<UUID, LogicalSwitch> entry : updatedLSRows.entrySet()) {
                updateLogicalSwitch(transaction, entry.getValue());
            }
        }
    }

    private void updateLogicalSwitch(ReadWriteTransaction transaction, LogicalSwitch lSwitch) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            LOG.debug("Connection {} is present",connection);
            Node connectionNode = buildConnectionNode(lSwitch);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
            // Update the Logical Switch with whatever data we are getting
            InstanceIdentifier<Node> lsIid = getInstanceIdentifier(lSwitch);
            Node lsNode = buildLogicalSwitchNode(lSwitch);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, lsIid, lsNode);
//            TODO: Delete entries that are no longer needed
        }
    }

    private Node buildLogicalSwitchNode(LogicalSwitch lSwitch) {
        return null;
/*        NodeBuilder lsNodeBuilder = new NodeBuilder();
        NodeId psNodeId = getNodeId(lSwitch);
        lsNodeBuilder.setNodeId(psNodeId);
        LogicalSwitchesBuilder lsAugBuilder = new LogicalSwitchesBuilder();
        setLogicalSwitchId(lsAugBuilder, lSwitch);
        lsNodeBuilder.addAugmentation(LogicalSwitches.class, lsAugBuilder.build());

        LOG.trace("Built with the intent to store PhysicalSwitch data {}",
                lsAugBuilder.build());
        return lsNodeBuilder.build();*/
    }

    private void setLogicalSwitchId(LogicalSwitchesBuilder lsAugBuilder, LogicalSwitch lSwitch) {
        lsAugBuilder.setHwvtepNodeName(new HwvtepNodeName(lSwitch.getName()));
        if(lSwitch.getDescription() != null) {
            lsAugBuilder.setHwvtepNodeDescription(lSwitch.getDescription());
        }
    }

    private Node buildConnectionNode(LogicalSwitch lSwitch) {
        //Update node with PhysicalSwitch reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());

        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        List<Switches> switches = new ArrayList<>();
        InstanceIdentifier<Node> switchIid = HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(),
                        lSwitch);
        hgAugmentationBuilder.setSwitches(switches);
        /* FIXME:
         * TODO: This need to be revisited after fix in yang file.
         * It should ideally be HwvtepSwitchRef
         */
        Switches logicalSwitch = new SwitchesBuilder().setSwitchRef(
                        new HwvtepPhysicalSwitchRef(switchIid)).build(); 
        switches.add(logicalSwitch);

        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());

        LOG.debug("Update node with logicalswitch ref {}",
                hgAugmentationBuilder.getSwitches().iterator().next());
        return connectionNode.build();
    }

    private InstanceIdentifier<Node> getInstanceIdentifier(LogicalSwitch lSwitch) {
        return HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(),
                lSwitch);
    }

    private NodeId getNodeId(LogicalSwitch lSwitch) {
        NodeKey nodeKey = getInstanceIdentifier(lSwitch).firstKeyOf(Node.class, NodeKey.class);
        return nodeKey.getNodeId();
    }
}
