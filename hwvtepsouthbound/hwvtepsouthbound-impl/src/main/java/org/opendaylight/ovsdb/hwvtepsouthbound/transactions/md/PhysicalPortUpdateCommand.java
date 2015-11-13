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
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.PhysicalPortIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class PhysicalPortUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(PhysicalPortUpdateCommand.class);
    private Map<UUID, PhysicalPort> updatedPPRows;
    private Map<UUID, PhysicalPort> oldPPRows;
    private Map<UUID, PhysicalSwitch> switchUpdatedRows;

    public PhysicalPortUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedPPRows = TyperUtils.extractRowsUpdated(PhysicalPort.class, getUpdates(),getDbSchema());
        oldPPRows = TyperUtils.extractRowsOld(PhysicalPort.class, getUpdates(),getDbSchema());
        switchUpdatedRows = TyperUtils.extractRowsUpdated(PhysicalSwitch.class, getUpdates(),getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        if ( updatedPPRows == null
                || updatedPPRows.isEmpty()) {
            return;
        }
        LOG.trace("PhysicalPortTable updated: {}", updatedPPRows);
        Optional<Node> node = readNode(transaction, connectionIId);
        if (node.isPresent()) {
            updateTerminationPoints(transaction, node.get());
        }
    }

    private void updateTerminationPoints(ReadWriteTransaction transaction, Node node) {
        for (Entry<UUID, PhysicalPort> pPortUpdate : updatedPPRows.entrySet()) {
            String portName = null;
            portName = pPortUpdate.getValue().getNameColumn().getData();
            Optional<InstanceIdentifier<Node>> switchIid = getTerminationPointSwitch(pPortUpdate.getKey());
            if (!switchIid.isPresent()) {
                switchIid = getTerminationPointSwitch( transaction, node, portName);
            }
            if (switchIid.isPresent()) {
                NodeId switchId = HwvtepSouthboundMapper.createManagedNodeId(switchIid.get());
                TerminationPointKey tpKey = new TerminationPointKey(new TpId(portName));
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                tpBuilder.setKey(tpKey);
                tpBuilder.setTpId(tpKey.getTpId());
                InstanceIdentifier<TerminationPoint> tpPath =
                        getInstanceIdentifier(switchIid.get(), pPortUpdate.getValue());
                HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder =
                        new HwvtepPhysicalPortAugmentationBuilder();
                buildTerminationPoint(tpAugmentationBuilder,pPortUpdate.getValue());
                tpBuilder.addAugmentation(HwvtepPhysicalPortAugmentation.class, tpAugmentationBuilder.build());
                if (oldPPRows.containsKey(pPortUpdate.getKey())) {
                    transaction.merge(LogicalDatastoreType.OPERATIONAL,
                            tpPath, tpBuilder.build());
                } else {
                    transaction.put(LogicalDatastoreType.OPERATIONAL,
                            tpPath, tpBuilder.build());
                }
            }
        }
    }

    private void buildTerminationPoint(HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder,
                    PhysicalPort portUpdate) {
        updatePhysicalPortId(portUpdate, tpAugmentationBuilder);
        updatePort(portUpdate, tpAugmentationBuilder);
    }

    private void updatePort(PhysicalPort portUpdate,
                    HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder) {
        updateVlanBindings(portUpdate, tpAugmentationBuilder);
    }

    private void updatePhysicalPortId(PhysicalPort portUpdate,
                    HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder) {
        PhysicalPortIdBuilder portIdBuilder = new PhysicalPortIdBuilder();
        portIdBuilder.setHwvtepNodeName(new HwvtepNodeName(portUpdate.getName()));
        if(portUpdate.getDescription() != null) {
            portIdBuilder.setHwvtepNodeDescription(portUpdate.getDescription());
        }

        tpAugmentationBuilder.setPhysicalPortId(portIdBuilder.build());
    }

    private void updateVlanBindings(PhysicalPort portUpdate,
                    HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder) {
        if(portUpdate.getVlanBindingsColumn() != null
                && portUpdate.getVlanBindingsColumn().getData() != null
                && !portUpdate.getVlanBindingsColumn().getData().isEmpty()) {
            Map<Long, UUID> vlanBindings = portUpdate.getVlanBindingsColumn().getData();
            Set<Long> vlanBindingsKeys = vlanBindings.keySet();
            List<VlanBindings> vlanBindingsList = new ArrayList<>();
            UUID vlanBindingValue = null;
            for(Long vlanBindingKey: vlanBindingsKeys) {
                vlanBindingValue = vlanBindings.get(vlanBindingKey);
                vlanBindingsList.add(createVlanBinding(vlanBindingKey, vlanBindingValue));
            }
            tpAugmentationBuilder.setVlanBindings(vlanBindingsList);
        }
    }

    private VlanBindings createVlanBinding(Long key, UUID value) {
        VlanBindingsBuilder vbBuilder = new VlanBindingsBuilder();
        VlanBindingsKey vbKey = new VlanBindingsKey(new VlanId(key.intValue()));
        vbBuilder.setKey(vbKey);
        vbBuilder.setVlanIdKey(vbKey.getVlanIdKey());
        /*TODO: 
         * Add ref to logicalswitch once code for LogicalSwitchUpdate is in place
        vbBuilder.setLogicalSwitch();
        */
        return vbBuilder.build();
    }

    private Optional<InstanceIdentifier<Node>> getTerminationPointSwitch( UUID portUUID) {
        for (UUID switchUUID : this.switchUpdatedRows.keySet()) {
            if (this.switchUpdatedRows.get(switchUUID).getPortsColumn().getData().contains(portUUID)) {
                return Optional.of(HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(),
                        this.switchUpdatedRows.get(switchUUID)));
            }
        }
        return Optional.absent();
    }

    private Optional<InstanceIdentifier<Node>> getTerminationPointSwitch(
                    final ReadWriteTransaction transaction, Node node, String tpName) {
                HwvtepGlobalAugmentation hwvtepNode = node.getAugmentation(HwvtepGlobalAugmentation.class);
                List<Switches> switchNodes = hwvtepNode.getSwitches();
                for ( Switches managedNodeEntry : switchNodes ) {
                    @SuppressWarnings("unchecked")
                    Node switchNode = readNode(transaction,
                            (InstanceIdentifier<Node>)managedNodeEntry.getSwitchRef().getValue()).get();
                    TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                    TerminationPointKey tpKey = new TerminationPointKey(new TpId(tpName));
                    tpBuilder.setKey(tpKey);
                    if (switchNode.getTerminationPoint().contains(tpBuilder.build())) {
                        PhysicalSwitchAugmentation pSwitchAugment
                            = switchNode.getAugmentation(PhysicalSwitchAugmentation.class);
                        return Optional.of((InstanceIdentifier<Node>)managedNodeEntry.getSwitchRef().getValue());
                    }
                }
                return Optional.absent();
            }

    private Optional<Node> readNode(final ReadWriteTransaction transaction, final InstanceIdentifier<Node> nodePath) {
        Optional<Node> node = Optional.absent();
        try {
            node = transaction.read(
                    LogicalDatastoreType.OPERATIONAL, nodePath)
                    .checkedGet();
        } catch (final ReadFailedException e) {
            LOG.warn("Read Operational/DS for Node fail! {}",
                    nodePath, e);
        }
        return node;
    }

    private InstanceIdentifier<TerminationPoint> getInstanceIdentifier(InstanceIdentifier<Node> switchIid,
                    PhysicalPort pPort) {
        return switchIid.child(TerminationPoint.class, new TerminationPointKey(new TpId(pPort.getName())));
    }

}
