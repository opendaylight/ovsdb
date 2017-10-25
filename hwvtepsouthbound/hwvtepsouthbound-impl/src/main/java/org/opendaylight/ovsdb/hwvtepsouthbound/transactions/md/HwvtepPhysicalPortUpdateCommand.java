/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.PhysicalPortUpdateCommand;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.PortFaultStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.PortFaultStatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.PortFaultStatusKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class HwvtepPhysicalPortUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepPhysicalPortUpdateCommand.class);
    private Map<UUID, PhysicalPort> updatedPPRows;
    private Map<UUID, PhysicalPort> oldPPRows;
    private Map<UUID, PhysicalSwitch> switchUpdatedRows;
    private Set<UUID> skipReconciliationPorts;

    public HwvtepPhysicalPortUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedPPRows = TyperUtils.extractRowsUpdated(PhysicalPort.class, getUpdates(), getDbSchema());
        oldPPRows = TyperUtils.extractRowsOld(PhysicalPort.class, getUpdates(), getDbSchema());
        switchUpdatedRows = TyperUtils.extractRowsUpdated(PhysicalSwitch.class, getUpdates(), getDbSchema());
        skipReconciliationPorts = new HashSet<>();
        for (Entry<UUID, PhysicalPort> pPortUpdateEntry : updatedPPRows.entrySet()) {
            Optional<InstanceIdentifier<Node>> switchIid = getTerminationPointSwitch(pPortUpdateEntry.getKey());
            if (switchIid.isPresent()) {
                if (getDeviceInfo().getDeviceOperData(Node.class, switchIid.get()) == null) {
                    //This is the first update from switch do not have to do reconciliation of this port
                    //it is taken care by switch reconciliation
                    skipReconciliationPorts.add(pPortUpdateEntry.getKey());
                }
            }
        }

    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        if (updatedPPRows.isEmpty()) {
            return;
        }
        LOG.trace("PhysicalPortTable updated: {}", updatedPPRows);
        Optional<Node> node = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (node.isPresent()) {
            updateTerminationPoints(transaction, node.get());
            // TODO: Handle Deletion of VLAN Bindings
        }
    }

    private void updateTerminationPoints(ReadWriteTransaction transaction, Node node) {
        for (Entry<UUID, PhysicalPort> pPortUpdateEntry : updatedPPRows.entrySet()) {
            PhysicalPort pPortUpdate = pPortUpdateEntry.getValue();
            String portName = pPortUpdate.getNameColumn().getData();
            Optional<InstanceIdentifier<Node>> switchIid = getTerminationPointSwitch(pPortUpdateEntry.getKey());
            if (!switchIid.isPresent()) {
                switchIid = getTerminationPointSwitch(transaction, node, portName);
            }
            if (switchIid.isPresent()) {
                TerminationPointKey tpKey = new TerminationPointKey(new TpId(portName));
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                tpBuilder.setKey(tpKey);
                tpBuilder.setTpId(tpKey.getTpId());
                InstanceIdentifier<TerminationPoint> tpPath = getInstanceIdentifier(switchIid.get(), pPortUpdate);
                HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder =
                        new HwvtepPhysicalPortAugmentationBuilder();
                buildTerminationPoint(tpAugmentationBuilder, pPortUpdate);
                setPortFaultStatus(tpAugmentationBuilder, pPortUpdate);
                tpBuilder.addAugmentation(HwvtepPhysicalPortAugmentation.class, tpAugmentationBuilder.build());
                if (oldPPRows.containsKey(pPortUpdateEntry.getKey())) {
                    transaction.merge(LogicalDatastoreType.OPERATIONAL, tpPath, tpBuilder.build());
                } else {
                    transaction.put(LogicalDatastoreType.OPERATIONAL, tpPath, tpBuilder.build());
                }
                reconcileToPort(transaction, pPortUpdate, tpPath);
                getDeviceInfo().updateDeviceOperData(TerminationPoint.class, tpPath,
                        pPortUpdate.getUuid(), pPortUpdate);
                // Update with Deleted VlanBindings
                if (oldPPRows.get(pPortUpdateEntry.getKey()) != null
                        && oldPPRows.get(pPortUpdateEntry.getKey()).getVlanBindingsColumn() != null) {
                    List<InstanceIdentifier<VlanBindings>> vBIiList = new ArrayList<>();
                    Map<Long, UUID> oldVb = oldPPRows.get(pPortUpdateEntry.getKey()).getVlanBindingsColumn().getData();
                    Map<Long, UUID> updatedVb = pPortUpdateEntry.getValue().getVlanBindingsColumn().getData();
                    for (Map.Entry<Long, UUID> oldVbEntry : oldVb.entrySet()) {
                        Long key = oldVbEntry.getKey();
                        if (!updatedVb.containsKey(key)) {
                            VlanBindings vBindings = createVlanBinding(key, oldVbEntry.getValue());
                            InstanceIdentifier<VlanBindings> vBid = getInstanceIdentifier(tpPath, vBindings);
                            vBIiList.add(vBid);
                        }
                        deleteEntries(transaction, vBIiList);
                    }
                }
                // Update with Deleted portfaultstatus
                deleteEntries(transaction,getPortFaultStatusToRemove( tpPath, pPortUpdate));
            }
        }
    }

    private void reconcileToPort(final ReadWriteTransaction transaction,
                                 final PhysicalPort pPortUpdate,
                                 final InstanceIdentifier<TerminationPoint> tpPath) {
        if (skipReconciliationPorts.contains(pPortUpdate.getUuid())) {
            //case of port added along with switch add
            //switch reconciliation will take care of this port along with other ports
            return;
        }
        if (getDeviceInfo().getDeviceOperData(TerminationPoint.class, tpPath) != null) {
            //case of port update not new port add
            return;
        }
        //case of individual port add , reconcile to this port
        getDeviceInfo().updateDeviceOperData(TerminationPoint.class, tpPath, pPortUpdate.getUuid(), pPortUpdate);
        Futures.addCallback(transaction.read(LogicalDatastoreType.CONFIGURATION, tpPath),
                new FutureCallback<Optional<TerminationPoint>>() {
                    @Override
                    public void onSuccess(Optional<TerminationPoint> optionalConfigTp) {
                        if (!optionalConfigTp.isPresent() || optionalConfigTp.get().getAugmentation(
                                HwvtepPhysicalPortAugmentation.class) == null) {
                            //TODO port came with some vlan bindings clean them up use PortRemovedCommand
                            return;
                        }
                        getDeviceInfo().updateDeviceOperData(TerminationPoint.class, tpPath,
                                pPortUpdate.getUuid(), pPortUpdate);
                        TerminationPoint configTp = optionalConfigTp.get();
                        getDeviceInfo().scheduleTransaction((transactionBuilder) -> {
                            InstanceIdentifier psIid = tpPath.firstIdentifierOf(Node.class);
                            HwvtepOperationalState operState = new HwvtepOperationalState(getOvsdbConnectionInstance());
                            PhysicalPortUpdateCommand portUpdateCommand = new PhysicalPortUpdateCommand(
                                    operState, Collections.EMPTY_LIST);
                            portUpdateCommand.updatePhysicalPort(transactionBuilder, psIid,
                                    Lists.newArrayList(configTp.getAugmentation(HwvtepPhysicalPortAugmentation.class)));
                        });
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                    }
                });
    }

    private <T extends DataObject> void deleteEntries(ReadWriteTransaction transaction,
            List<InstanceIdentifier<T>> entryIids) {
        for (InstanceIdentifier<T> entryIid : entryIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, entryIid);
        }
    }

    private InstanceIdentifier<VlanBindings> getInstanceIdentifier(InstanceIdentifier<TerminationPoint> tpPath,
            VlanBindings vBindings) {
        return HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), tpPath, vBindings);
    }

    private void buildTerminationPoint(HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder,
            PhysicalPort portUpdate) {
        updatePhysicalPortId(portUpdate, tpAugmentationBuilder);
        updatePort(portUpdate, tpAugmentationBuilder);
    }

    private void updatePort(PhysicalPort portUpdate, HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder) {
        updateVlanBindings(portUpdate, tpAugmentationBuilder);
        tpAugmentationBuilder.setPhysicalPortUuid(new Uuid(portUpdate.getUuid().toString()));
    }

    private void updatePhysicalPortId(PhysicalPort portUpdate,
            HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder) {
        tpAugmentationBuilder.setHwvtepNodeName(new HwvtepNodeName(portUpdate.getName()));
        if (portUpdate.getDescription() != null) {
            tpAugmentationBuilder.setHwvtepNodeDescription(portUpdate.getDescription());
        }
    }

    private void updateVlanBindings(PhysicalPort portUpdate,
            HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder) {
        Map<Long, UUID> vlanBindings = portUpdate.getVlanBindingsColumn().getData();
        if (vlanBindings != null && !vlanBindings.isEmpty()) {
            List<VlanBindings> vlanBindingsList = new ArrayList<>();
            for (Map.Entry<Long, UUID> vlanBindingEntry : vlanBindings.entrySet()) {
                Long vlanBindingKey = vlanBindingEntry.getKey();
                UUID vlanBindingValue = vlanBindingEntry.getValue();
                if (vlanBindingValue != null && vlanBindingKey != null) {
                    vlanBindingsList.add(createVlanBinding(vlanBindingKey, vlanBindingValue));
                }
            }
            tpAugmentationBuilder.setVlanBindings(vlanBindingsList);
        }
    }

    private VlanBindings createVlanBinding(Long key, UUID value) {
        VlanBindingsBuilder vbBuilder = new VlanBindingsBuilder();
        VlanBindingsKey vbKey = new VlanBindingsKey(new VlanId(key.intValue()));
        vbBuilder.setKey(vbKey);
        vbBuilder.setVlanIdKey(vbKey.getVlanIdKey());
        HwvtepLogicalSwitchRef lSwitchRef = this.getLogicalSwitchRef(value);
        vbBuilder.setLogicalSwitchRef(lSwitchRef);
        return vbBuilder.build();
    }

    private HwvtepLogicalSwitchRef getLogicalSwitchRef(UUID switchUUID) {
        LogicalSwitch logicalSwitch = getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitch(switchUUID);
        if (logicalSwitch != null) {
            InstanceIdentifier<LogicalSwitches> lSwitchIid =
                    HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), logicalSwitch);
            return new HwvtepLogicalSwitchRef(lSwitchIid);
        }
        LOG.debug("Failed to get LogicalSwitch {}", switchUUID);
        LOG.trace("Available LogicalSwitches: {}",
                        getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitches().values());
        return null;
    }

    private Optional<InstanceIdentifier<Node>> getTerminationPointSwitch(UUID portUUID) {
        for (PhysicalSwitch updatedPhysicalSwitch : switchUpdatedRows.values()) {
            if (updatedPhysicalSwitch.getPortsColumn().getData().contains(portUUID)) {
                return Optional.of(HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(),
                        updatedPhysicalSwitch));
            }
        }
        return Optional.absent();
    }

    private Optional<InstanceIdentifier<Node>> getTerminationPointSwitch(final ReadWriteTransaction transaction,
            Node node, String tpName) {
        HwvtepGlobalAugmentation hwvtepNode = node.getAugmentation(HwvtepGlobalAugmentation.class);
        List<Switches> switchNodes = hwvtepNode.getSwitches();
        for (Switches managedNodeEntry : switchNodes) {
            @SuppressWarnings("unchecked")
            Node switchNode = HwvtepSouthboundUtil
                    .readNode(transaction, (InstanceIdentifier<Node>) managedNodeEntry.getSwitchRef().getValue()).get();
            TerminationPointKey tpKey = new TerminationPointKey(new TpId(tpName));
            if (switchNode.getTerminationPoint() != null) {
                for (TerminationPoint terminationPoint : switchNode.getTerminationPoint()) {
                    if (terminationPoint.getKey().equals(tpKey)) {
                        return Optional.of((InstanceIdentifier<Node>) managedNodeEntry.getSwitchRef().getValue());
                    }
                }
            }
        }
        return Optional.absent();
    }

    private InstanceIdentifier<TerminationPoint> getInstanceIdentifier(InstanceIdentifier<Node> switchIid,
            PhysicalPort pPort) {
        return switchIid.child(TerminationPoint.class, new TerminationPointKey(new TpId(pPort.getName())));
    }

    private void setPortFaultStatus(HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder,
            PhysicalPort portUpdate) {
        if (portUpdate.getPortFaultStatusColumn() != null && portUpdate.getPortFaultStatusColumn().getData() != null
                && !portUpdate.getPortFaultStatusColumn().getData().isEmpty()) {
            List<PortFaultStatus> portFaultStatusLst = new ArrayList<>();
            for (String portFaultStatus : portUpdate.getPortFaultStatusColumn().getData()) {
                portFaultStatusLst.add(new PortFaultStatusBuilder().setKey(new PortFaultStatusKey(portFaultStatus))
                        .setPortFaultStatusKey(portFaultStatus).build());
            }
            tpAugmentationBuilder.setPortFaultStatus(portFaultStatusLst);
        }
    }

    private List<InstanceIdentifier<PortFaultStatus>> getPortFaultStatusToRemove(
            InstanceIdentifier<TerminationPoint> tpPath, PhysicalPort pPort) {
        Preconditions.checkNotNull(tpPath);
        Preconditions.checkNotNull(pPort);
        List<InstanceIdentifier<PortFaultStatus>> result = new ArrayList<>();
        PhysicalPort oldPort = oldPPRows.get(pPort.getUuid());
        if (oldPort != null && oldPort.getPortFaultStatusColumn() != null) {
            for (String portFltStat : oldPort.getPortFaultStatusColumn().getData()) {
                if (pPort.getPortFaultStatusColumn() == null
                        || !pPort.getPortFaultStatusColumn().getData().contains(portFltStat)) {
                    InstanceIdentifier<PortFaultStatus> iid = tpPath.augmentation(HwvtepPhysicalPortAugmentation.class)
                            .child(PortFaultStatus.class, new PortFaultStatusKey(portFltStat));
                    result.add(iid);
                }
            }
        }
        return result;
    }

}
