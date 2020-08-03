/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicalPortUpdateCommand
        extends AbstractTransactCommand<TerminationPoint, TerminationPointKey, PhysicalSwitchAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalPortUpdateCommand.class);
    private static final VlanBindingsUnMetDependencyGetter DEPENDENCY_GETTER = new VlanBindingsUnMetDependencyGetter();

    public PhysicalPortUpdateCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<TerminationPoint>> createds =
                extractCreated(getChanges(),TerminationPoint.class);
        if (!createds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<TerminationPoint>> created:
                createds.entrySet()) {
                updatePhysicalPort(transaction,  created.getKey(), created.getValue());
            }
        }
        Map<InstanceIdentifier<Node>, List<TerminationPoint>> updateds =
                extractUpdatedPorts(getChanges(), TerminationPoint.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<TerminationPoint>> updated:
                updateds.entrySet()) {
                updatePhysicalPort(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    public void updatePhysicalPort(final TransactionBuilder transaction,
                                   final InstanceIdentifier<Node> psNodeiid,
                                   final List<TerminationPoint> listPort) {
        if (listPort != null) {
            for (TerminationPoint port : listPort) {
                LOG.debug("Processing port {}", port);
                InstanceIdentifier<TerminationPoint> tpIId = psNodeiid.child(TerminationPoint.class, port.key());
                HwvtepPhysicalPortAugmentation hwvtepPhysicalPortAugmentation =
                        port.augmentation(HwvtepPhysicalPortAugmentation.class);
                if (hwvtepPhysicalPortAugmentation != null) {
                    onConfigUpdate(transaction, psNodeiid, port, tpIId);
                }
            }
        }
    }

    @Override
    public void onConfigUpdate(final TransactionBuilder transaction, final InstanceIdentifier psNodeiid,
                               final TerminationPoint port, final InstanceIdentifier tpIId, final Object... extraData) {
        doDeviceTransaction(transaction, psNodeiid, port, tpIId);
    }

    @Override
    public void doDeviceTransaction(final TransactionBuilder transaction, final InstanceIdentifier nodeIid,
                                    final TerminationPoint data, final InstanceIdentifier key,
                                    final Object... extraData) {
        LOG.debug("Processing port doDeviceTransaction {}", data);
        InstanceIdentifier<Node> psNodeiid = nodeIid;
        HwvtepPhysicalPortAugmentation port = data.augmentation(
                HwvtepPhysicalPortAugmentation.class);
        if (port == null) {
            LOG.info("No port augmentation found for port {}", data);
            return;
        }
        if (port.getHwvtepNodeName() == null) {
            LOG.info("No port hwvtep node name found for port {}", data);
            return;
        }
        LOG.debug("Creating a physical port named: {}", port.getHwvtepNodeName().getValue());
        getOperationalState().getDeviceInfo().updateConfigData(VlanBindings.class, key, data);
        HwvtepDeviceInfo.DeviceData deviceData = getOperationalState().getDeviceInfo()
                .getDeviceOperData(VlanBindings.class, key);
        PhysicalPort physicalPort = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                PhysicalPort.class);
        //get managing global node of physicalSwitchBelong
        setName(physicalPort, port);
        setDescription(physicalPort, port);
        if (deviceData == null || deviceData.getData() == null) {
            LOG.error("Updated the device oper cache for port from actual device {}", key);
            deviceData = super.fetchDeviceData(VlanBindings.class, key);
        }
        if (deviceData == null || deviceData.getData() == null) {
            LOG.warn("Port not present in opdata store {}", key);
        } else {
            if (deviceData.getData() == null || !(deviceData.getData() instanceof PhysicalPort)) {
                LOG.error("Failed to get the device data for port {}", getKeyStr(key));
            }
            Map<Long, UUID> bindingMap = setVlanBindings(nodeIid, physicalPort, data, key, transaction);
            PhysicalPort tp = (PhysicalPort) deviceData.getData();
            if (getOperationalState().isInReconciliation()) {
                if (tp.getVlanBindingsColumn() != null && tp.getVlanBindingsColumn().getData() != null) {
                    Map<Long, UUID> existing = new HashMap<>(tp.getVlanBindingsColumn().getData());
                    if (existing.size() == bindingMap.size()) {
                        boolean allMatched = bindingMap.entrySet().stream().allMatch(newEntry -> {
                            return Objects.equals(existing.get(newEntry.getKey()), newEntry.getValue());
                        });
                        if (allMatched) {
                            return;
                        }
                    }
                }
            }
            String nodeId = psNodeiid.firstKeyOf(Node.class).getNodeId().getValue();
            getOperationalState().getDeviceInfo().updateDeviceOperData(VlanBindings.class, key,
                    deviceData.getUuid(), deviceData.getData());
            //updated physical port only

            String existingPhysicalPortName = tp.getName();
            PhysicalPort extraPhyscialPort =
                    TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalPort.class);
            extraPhyscialPort.setName("");
            LOG.trace("execute: updating physical port: {} {}", nodeId, physicalPort);
            transaction.add(op.update(physicalPort)
                    .where(extraPhyscialPort.getNameColumn().getSchema().opEqual(existingPhysicalPortName))
                    .build());
            transaction.add(op.comment("Physical Port: Updating " + existingPhysicalPortName));
            updateControllerTxHistory(TransactionType.UPDATE, physicalPort);
            LOG.info("CONTROLLER - {} {}", TransactionType.UPDATE, physicalPort);

        }
        return;
    }

    private void setName(final PhysicalPort physicalPort, final HwvtepPhysicalPortAugmentation inputPhysicalPort) {
        if (inputPhysicalPort.getHwvtepNodeName() != null) {
            physicalPort.setName(inputPhysicalPort.getHwvtepNodeName().getValue());
        }
    }

    private static void setDescription(final PhysicalPort physicalPort,
            final HwvtepPhysicalPortAugmentation inputPhysicalPort) {
        if (inputPhysicalPort.getHwvtepNodeDescription() != null) {
            physicalPort.setDescription(inputPhysicalPort.getHwvtepNodeDescription());
        }
    }

    private Map<Long, UUID> setVlanBindings(final InstanceIdentifier<Node> psNodeiid,
                                            final PhysicalPort physicalPort,
                                            final TerminationPoint inputPhysicalPort,
                                            final InstanceIdentifier key,
                                            final TransactionBuilder transaction) {
        HwvtepPhysicalPortAugmentation portAugmentation = inputPhysicalPort.augmentation(
                HwvtepPhysicalPortAugmentation.class);
        Map<Long, UUID> bindingMap = new HashMap<>();
        //get UUID by LogicalSwitchRef
        for (VlanBindings vlanBinding : portAugmentation.nonnullVlanBindings().values()) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<LogicalSwitches> lswitchIid =
                (InstanceIdentifier<LogicalSwitches>) vlanBinding.getLogicalSwitchRef().getValue();

            Map inTransitDependencies = DEPENDENCY_GETTER.getInTransitDependencies(
                getOperationalState(), vlanBinding);
            Map configDependencies = DEPENDENCY_GETTER.getUnMetConfigDependencies(
                getOperationalState(), vlanBinding);

            if (!HwvtepSouthboundUtil.isEmptyMap(configDependencies)) {
                createConfigWaitJob(psNodeiid, inputPhysicalPort, key, configDependencies);
                continue;
            }
            if (!HwvtepSouthboundUtil.isEmptyMap(inTransitDependencies)) {
                createOperWaitingJob(psNodeiid, inputPhysicalPort, key, inTransitDependencies);
                continue;
            }

            UUID lsUUid = TransactUtils.getLogicalSwitchUUID(transaction, getOperationalState(), lswitchIid);
            if (lsUUid == null) {
                LOG.error("Could not get the logical switch uuid for {}", vlanBinding);
                continue;
            }
            bindingMap.put(vlanBinding.getVlanIdKey().getValue().longValue(), lsUUid);
        }
        physicalPort.setVlanBindings(bindingMap);
        return bindingMap;
    }

    private void createOperWaitingJob(final InstanceIdentifier<Node> psNodeiid,
                                      final TerminationPoint inputPhysicalPort,
                                      final InstanceIdentifier<TerminationPoint> key,
                                      final Map inTransitDependencies) {
        if (getDeviceInfo().isKeyInDependencyQueue(key)) {
            return;
        }
        DependentJob<VlanBindings> opWaitingJob = new DependentJob.OpWaitingJob(
                key, inputPhysicalPort, inTransitDependencies, getOperationalState().getTransactionId()) {
            @Override
            public void onDependencyResolved(final HwvtepOperationalState operationalState,
                                             final TransactionBuilder transactionBuilder) {
                LOG.info("physical port oper dependency resolved {}", key);//TODO delete
                PhysicalPortUpdateCommand.this.hwvtepOperationalState = operationalState;
                HwvtepDeviceInfo.DeviceData deviceData = getOperationalState().getDeviceInfo().getConfigData(
                        VlanBindings.class, key);
                TerminationPoint port = inputPhysicalPort;
                if (deviceData != null && deviceData.getData() != null) {
                    port = (TerminationPoint) deviceData.getData();
                }
                doDeviceTransaction(transactionBuilder, psNodeiid, port, key);
            }
        };
        LOG.info("Added the port to oper wait queue {}", key);//TODO delete
        getDeviceInfo().addJobToQueue(opWaitingJob);
    }

    private void createConfigWaitJob(final InstanceIdentifier<Node> psNodeiid,
                                     final TerminationPoint inputPhysicalPort,
                                     final InstanceIdentifier<TerminationPoint> key,
                                     final Map configDependencies) {
        if (getDeviceInfo().isKeyInDependencyQueue(key)) {
            return;
        }
        DependentJob<TerminationPoint> configWaitingJob = new DependentJob.ConfigWaitingJob(
                key, inputPhysicalPort, configDependencies) {
            @Override
            public void onDependencyResolved(final HwvtepOperationalState operationalState,
                                             final TransactionBuilder transactionBuilder) {
                LOG.info("physical port config dependency resolved {}", key);//TODO delete
                PhysicalPortUpdateCommand.this.hwvtepOperationalState = operationalState;
                HwvtepDeviceInfo.DeviceData deviceData = getOperationalState().getDeviceInfo().getConfigData(
                        VlanBindings.class, key);
                TerminationPoint port = inputPhysicalPort;
                if (deviceData != null && deviceData.getData() != null) {
                    port = (TerminationPoint) deviceData.getData();
                }
                doDeviceTransaction(transactionBuilder, psNodeiid, port, key);
            }
        };
        LOG.info("Added the port to config wait queue {}", key);//TODO delete
        getDeviceInfo().addJobToQueue(configWaitingJob);
    }

    static class VlanBindingsUnMetDependencyGetter extends UnMetDependencyGetter<VlanBindings> {

        @Override
        public List<InstanceIdentifier<?>> getLogicalSwitchDependencies(final VlanBindings data) {
            if (data == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(data.getLogicalSwitchRef().getValue());
        }

        @Override
        public List<InstanceIdentifier<?>> getTerminationPointDependencies(final VlanBindings data) {
            return Collections.emptyList();
        }
    }

    private static Map<InstanceIdentifier<Node>, List<TerminationPoint>> extractCreated(
            final Collection<DataTreeModification<Node>> changes, final Class<TerminationPoint> class1) {
        Map<InstanceIdentifier<Node>, List<TerminationPoint>> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    List<TerminationPoint> portListUpdated = new ArrayList<>();
                    for (TerminationPoint tp : created.nonnullTerminationPoint().values()) {
                        HwvtepPhysicalPortAugmentation hppAugmentation =
                                tp.augmentation(HwvtepPhysicalPortAugmentation.class);
                        if (hppAugmentation != null) {
                            portListUpdated.add(tp);
                        }
                    }
                    result.put(key, portListUpdated);
                }
            }
        }
        return result;
    }

    private static Map<InstanceIdentifier<Node>, List<TerminationPoint>> extractUpdatedPorts(
            final Collection<DataTreeModification<Node>> changes, final Class<TerminationPoint> class1) {
        Map<InstanceIdentifier<Node>, List<TerminationPoint>> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<TerminationPoint> portListUpdated = new ArrayList<>();
                    List<TerminationPoint> portListBefore = new ArrayList<>();
                    for (TerminationPoint tp : updated.nonnullTerminationPoint().values()) {
                        HwvtepPhysicalPortAugmentation hppAugmentation =
                                tp.augmentation(HwvtepPhysicalPortAugmentation.class);
                        if (hppAugmentation != null) {
                            portListUpdated.add(tp);
                        }
                    }
                    for (TerminationPoint tp : before.nonnullTerminationPoint().values()) {
                        HwvtepPhysicalPortAugmentation hppAugmentation =
                                tp.augmentation(HwvtepPhysicalPortAugmentation.class);
                        if (hppAugmentation != null) {
                            portListBefore.add(tp);
                        }
                    }
                    portListUpdated.removeAll(portListBefore);
                    result.put(key, portListUpdated);
                }
            }
        }
        return result;
    }

    @Override
    protected String getKeyStr(final InstanceIdentifier iid) {
        try {
            return ((TerminationPoint)iid.firstKeyOf(TerminationPoint.class)).getTpId().getValue();
        } catch (ClassCastException exp) {
            LOG.error("Error in getting the TerminationPoint id ", exp);
        }
        return super.getKeyStr(iid);
    }

}
