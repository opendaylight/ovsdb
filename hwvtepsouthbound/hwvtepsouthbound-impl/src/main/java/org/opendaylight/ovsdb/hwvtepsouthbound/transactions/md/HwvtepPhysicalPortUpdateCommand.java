/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.events.PortEvent;
import org.opendaylight.ovsdb.hwvtepsouthbound.events.ReconcilePortEvent;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.PhysicalPortUpdateCommand;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.SwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.PortFaultStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.PortFaultStatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.PortFaultStatusKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepPhysicalPortUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepPhysicalPortUpdateCommand.class);

    private final Map<UUID, PhysicalPort> updatedPPRows;
    private final Map<UUID, PhysicalPort> oldPPRows;
    private final Map<UUID, PhysicalSwitch> switchUpdatedRows;
    private final Set<UUID> skipReconciliationPorts;
    private final MdsalUtils mdsalUtils;

    public HwvtepPhysicalPortUpdateCommand(final HwvtepConnectionInstance key, final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedPPRows = TyperUtils.extractRowsUpdated(PhysicalPort.class, getUpdates(), getDbSchema());
        oldPPRows = TyperUtils.extractRowsOld(PhysicalPort.class, getUpdates(), getDbSchema());
        switchUpdatedRows = TyperUtils.extractRowsUpdated(PhysicalSwitch.class, getUpdates(), getDbSchema());
        skipReconciliationPorts = new HashSet<>();
        mdsalUtils = new MdsalUtils(key.getDataBroker());
        for (Entry<UUID, PhysicalPort> portUpdateEntry : updatedPPRows.entrySet()) {
            Optional<InstanceIdentifier<Node>> switchIid = getTerminationPointSwitch(portUpdateEntry.getKey());
            if (switchIid.isPresent()) {
                if (getDeviceInfo().getDeviceOperData(Node.class, switchIid.orElseThrow()) == null) {
                    //This is the first update from switch do not have to do reconciliation of this port
                    //it is taken care by switch reconciliation
                    skipReconciliationPorts.add(portUpdateEntry.getKey());
                }
            }
        }

    }

    @Override
    public void execute(final ReadWriteTransaction transaction) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        if (updatedPPRows.isEmpty()) {
            return;
        }
        LOG.trace("PhysicalPortTable updated: {}", updatedPPRows);
        Optional<Node> node = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (node.isPresent()) {
            updateTerminationPoints(transaction, node.orElseThrow());
            // TODO: Handle Deletion of VLAN Bindings
        }
    }

    private void updateTerminationPoints(final ReadWriteTransaction transaction, final Node node) {
        for (Entry<UUID, PhysicalPort> portUpdateEntry : updatedPPRows.entrySet()) {
            PhysicalPort portUpdate = portUpdateEntry.getValue();
            String portName = portUpdate.getNameColumn().getData();
            Optional<InstanceIdentifier<Node>> switchIid = getTerminationPointSwitch(portUpdateEntry.getKey());
            if (!switchIid.isPresent()) {
                switchIid = getFromDeviceOperCache(portUpdate.getUuid());
                if (!switchIid.isPresent()) {
                    LOG.debug("Failed to find node from the DeviceOperCache for port {}. Get it from the DS.",
                            portUpdate);
                    switchIid = getTerminationPointSwitch(transaction, node, portName);
                }
            }
            if (switchIid.isPresent()) {
                TerminationPointKey tpKey = new TerminationPointKey(new TpId(portName));
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                tpBuilder.withKey(tpKey);
                tpBuilder.setTpId(tpKey.getTpId());
                final InstanceIdentifier<TerminationPoint> tpPath =
                    getInstanceIdentifier(switchIid.orElseThrow(), portUpdate);
                HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder =
                        new HwvtepPhysicalPortAugmentationBuilder();
                buildTerminationPoint(tpAugmentationBuilder, portUpdate);
                setPortFaultStatus(tpAugmentationBuilder, portUpdate);
                tpBuilder.addAugmentation(tpAugmentationBuilder.build());
                if (oldPPRows.containsKey(portUpdateEntry.getKey())) {
                    transaction.merge(LogicalDatastoreType.OPERATIONAL, tpPath, tpBuilder.build());
                } else {
                    transaction.put(LogicalDatastoreType.OPERATIONAL, tpPath, tpBuilder.build());
                }
                NodeId psNodeId = tpPath.firstKeyOf(Node.class).getNodeId();
                if (getDeviceInfo().getDeviceOperData(TerminationPoint.class, tpPath) == null) {
                    addToDeviceUpdate(TransactionType.ADD, new PortEvent(portUpdate, psNodeId));
                } else {
                    addToDeviceUpdate(TransactionType.UPDATE, new PortEvent(portUpdate, psNodeId));
                }
                reconcileToPort(portUpdate, tpPath);
                addToUpdateTx(VlanBindings.class, tpPath, portUpdate.getUuid(), portUpdate);
                // Update with Deleted VlanBindings
                if (oldPPRows.get(portUpdateEntry.getKey()) != null
                        && oldPPRows.get(portUpdateEntry.getKey()).getVlanBindingsColumn() != null) {
                    List<InstanceIdentifier<VlanBindings>> vlanBindingsList = new ArrayList<>();
                    Map<Long, UUID> oldVb = oldPPRows.get(portUpdateEntry.getKey()).getVlanBindingsColumn().getData();
                    Map<Long, UUID> updatedVb = portUpdateEntry.getValue().getVlanBindingsColumn().getData();
                    for (Map.Entry<Long, UUID> oldVbEntry : oldVb.entrySet()) {
                        Long key = oldVbEntry.getKey();
                        if (!updatedVb.containsKey(key)) {
                            VlanBindings vlanBindings = createVlanBinding(key, oldVbEntry.getValue());
                            InstanceIdentifier<VlanBindings> vbIid = getInstanceIdentifier(tpPath, vlanBindings);
                            vlanBindingsList.add(vbIid);
                        }
                        deleteEntries(transaction, vlanBindingsList);
                    }
                }
                // Update with Deleted portfaultstatus
                deleteEntries(transaction,getPortFaultStatusToRemove(tpPath, portUpdate));
            } else {
                LOG.warn("switchIid was not found for port {}", portUpdate);
            }
        }
    }

    private void reconcileToPort(final PhysicalPort portUpdate,
                                 final InstanceIdentifier<TerminationPoint> tpPath) {
        if (skipReconciliationPorts.contains(portUpdate.getUuid())) {
            //case of port added along with switch add
            //switch reconciliation will take care of this port along with other ports
            return;
        }
        if (getDeviceInfo().getDeviceOperData(VlanBindings.class, tpPath) != null) {
            //case of port update not new port add
            return;

        }
        //case of individual port add , reconcile to this port
        addToUpdateTx(VlanBindings.class, tpPath, portUpdate.getUuid(), portUpdate);
        HwvtepDeviceInfo.DeviceData data = getDeviceInfo().getConfigData(VlanBindings.class, tpPath);
        if (data == null || data.getData() == null) {
            LOG.error("No config data present ");
        } else {
            addToDeviceUpdate(TransactionType.ADD,
                    new ReconcilePortEvent(portUpdate, tpPath.firstKeyOf(Node.class).getNodeId()));
            LOG.info("addToDeviceUpdate {}", portUpdate);
            getDeviceInfo().updateDeviceOperData(VlanBindings.class, tpPath,
                    portUpdate.getUuid(), portUpdate);
            getDeviceInfo().scheduleTransaction(transactionBuilder -> {
                InstanceIdentifier psIid = tpPath.firstIdentifierOf(Node.class);
                HwvtepOperationalState operState = new HwvtepOperationalState(getOvsdbConnectionInstance());
                PhysicalPortUpdateCommand portUpdateCommand = new PhysicalPortUpdateCommand(
                        operState, Collections.emptyList());
                TerminationPoint cfgPoint = (TerminationPoint) data.getData();
                portUpdateCommand.updatePhysicalPort(transactionBuilder, psIid,
                            Lists.newArrayList(cfgPoint));

            });
        }
    }

    private static <T extends DataObject> void deleteEntries(final ReadWriteTransaction transaction,
            final List<InstanceIdentifier<T>> entryIids) {
        for (InstanceIdentifier<T> entryIid : entryIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, entryIid);
        }
    }

    private InstanceIdentifier<VlanBindings> getInstanceIdentifier(final InstanceIdentifier<TerminationPoint> tpPath,
            final VlanBindings vlanBindings) {
        return HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), tpPath, vlanBindings);
    }

    private static InstanceIdentifier<TerminationPoint> getInstanceIdentifier(final InstanceIdentifier<Node> switchIid,
            final PhysicalPort port) {
        return switchIid.child(TerminationPoint.class, new TerminationPointKey(new TpId(port.getName())));
    }

    private void buildTerminationPoint(final HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder,
            final PhysicalPort portUpdate) {
        updatePhysicalPortId(portUpdate, tpAugmentationBuilder);
        updatePort(portUpdate, tpAugmentationBuilder);
    }

    private void updatePort(final PhysicalPort portUpdate,
            final HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder) {
        updateVlanBindings(portUpdate, tpAugmentationBuilder);
        tpAugmentationBuilder.setPhysicalPortUuid(new Uuid(portUpdate.getUuid().toString()));
    }

    private static void updatePhysicalPortId(final PhysicalPort portUpdate,
            final HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder) {
        tpAugmentationBuilder.setHwvtepNodeName(new HwvtepNodeName(portUpdate.getName()));
        if (portUpdate.getDescription() != null) {
            tpAugmentationBuilder.setHwvtepNodeDescription(portUpdate.getDescription());
        }
    }

    private void updateVlanBindings(final PhysicalPort portUpdate,
            final HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder) {
        Map<Long, UUID> vlanBindings = portUpdate.getVlanBindingsColumn().getData();
        if (vlanBindings != null && !vlanBindings.isEmpty()) {
            var vlanBindingsList = BindingMap.<VlanBindingsKey, VlanBindings>orderedBuilder();
            for (Map.Entry<Long, UUID> vlanBindingEntry : vlanBindings.entrySet()) {
                Long vlanBindingKey = vlanBindingEntry.getKey();
                UUID vlanBindingValue = vlanBindingEntry.getValue();
                if (vlanBindingValue != null && vlanBindingKey != null) {
                    vlanBindingsList.add(createVlanBinding(vlanBindingKey, vlanBindingValue));
                }
            }
            tpAugmentationBuilder.setVlanBindings(vlanBindingsList.build());
        }
    }

    private VlanBindings createVlanBinding(final Long key, final UUID value) {
        VlanBindingsBuilder vbBuilder = new VlanBindingsBuilder();
        VlanBindingsKey vbKey = new VlanBindingsKey(new VlanId(Uint16.valueOf(key)));
        vbBuilder.withKey(vbKey);
        vbBuilder.setVlanIdKey(vbKey.getVlanIdKey());
        HwvtepLogicalSwitchRef switchRef = this.getLogicalSwitchRef(value);
        vbBuilder.setLogicalSwitchRef(switchRef);
        return vbBuilder.build();
    }

    private HwvtepLogicalSwitchRef getLogicalSwitchRef(final UUID switchUUID) {
        LogicalSwitch logicalSwitch = getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitch(switchUUID);
        if (logicalSwitch != null) {
            InstanceIdentifier<LogicalSwitches> switchIid =
                    HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), logicalSwitch);
            return new HwvtepLogicalSwitchRef(switchIid.toIdentifier());
        }
        LOG.debug("Failed to get LogicalSwitch {}", switchUUID);
        LOG.trace("Available LogicalSwitches: {}",
                        getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitches().values());
        return null;
    }

    private Optional<InstanceIdentifier<Node>> getTerminationPointSwitch(final UUID portUUID) {
        for (PhysicalSwitch updatedPhysicalSwitch : switchUpdatedRows.values()) {
            if (updatedPhysicalSwitch.getPortsColumn().getData().contains(portUUID)) {
                return Optional.of(HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(),
                        updatedPhysicalSwitch));
            }
        }
        return Optional.empty();
    }

    private static Optional<InstanceIdentifier<Node>> getTerminationPointSwitch(final ReadWriteTransaction transaction,
            final Node node, final String tpName) {
        HwvtepGlobalAugmentation hwvtepNode = node.augmentation(HwvtepGlobalAugmentation.class);
        Map<SwitchesKey, Switches> switchNodes = hwvtepNode.getSwitches();
        if (switchNodes != null && !switchNodes.isEmpty()) {
            for (Switches managedNodeEntry : switchNodes.values()) {
                @SuppressWarnings("unchecked")
                Node switchNode = HwvtepSouthboundUtil.readNode(transaction,
                    ((DataObjectIdentifier<Node>) managedNodeEntry.getSwitchRef().getValue()).toLegacy()).orElseThrow();
                TerminationPointKey tpKey = new TerminationPointKey(new TpId(tpName));
                TerminationPoint terminationPoint = switchNode.nonnullTerminationPoint().get(tpKey);
                if (terminationPoint != null) {
                    return Optional.of(
                        ((DataObjectIdentifier<Node>) managedNodeEntry.getSwitchRef().getValue()).toLegacy());
                }
            }
        } else {
            LOG.trace("PhyscialSwitch not present for the Port {}", tpName);
        }
        return Optional.empty();
    }

    private static void setPortFaultStatus(final HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder,
            final PhysicalPort portUpdate) {
        if (portUpdate.getPortFaultStatusColumn() != null && portUpdate.getPortFaultStatusColumn().getData() != null
                && !portUpdate.getPortFaultStatusColumn().getData().isEmpty()) {
            var portFaultStatusLst = portUpdate.getPortFaultStatusColumn().getData().stream()
                .map(portFaultStatus -> new PortFaultStatusBuilder().setPortFaultStatusKey(portFaultStatus).build())
                .collect(BindingMap.toOrderedMap());
            tpAugmentationBuilder.setPortFaultStatus(portFaultStatusLst);
        }
    }

    private List<InstanceIdentifier<PortFaultStatus>> getPortFaultStatusToRemove(
            final InstanceIdentifier<TerminationPoint> tpPath, final PhysicalPort port) {
        requireNonNull(tpPath);
        requireNonNull(port);
        List<InstanceIdentifier<PortFaultStatus>> result = new ArrayList<>();
        PhysicalPort oldPort = oldPPRows.get(port.getUuid());
        if (oldPort != null && oldPort.getPortFaultStatusColumn() != null) {
            for (String portFltStat : oldPort.getPortFaultStatusColumn().getData()) {
                if (port.getPortFaultStatusColumn() == null
                        || !port.getPortFaultStatusColumn().getData().contains(portFltStat)) {
                    InstanceIdentifier<PortFaultStatus> iid = tpPath.augmentation(HwvtepPhysicalPortAugmentation.class)
                            .child(PortFaultStatus.class, new PortFaultStatusKey(portFltStat));
                    result.add(iid);
                }
            }
        }
        return result;
    }

    private  Optional<InstanceIdentifier<Node>> getFromDeviceOperCache(final UUID uuid) {

        InstanceIdentifier<TerminationPoint> terminationPointIid =
                getOvsdbConnectionInstance()
                .getDeviceInfo().getDeviceOperKey(TerminationPoint.class, uuid);
        if (terminationPointIid != null) {
            return Optional.of(terminationPointIid.firstIdentifierOf(Node.class));
        }
        return Optional.empty();
    }
}
