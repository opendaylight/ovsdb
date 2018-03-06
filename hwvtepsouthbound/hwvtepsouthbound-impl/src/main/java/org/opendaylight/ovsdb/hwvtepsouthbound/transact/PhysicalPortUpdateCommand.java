/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicalPortUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalPortUpdateCommand.class);
    private static final VlanBindingsUnMetDependencyGetter DEPENDENCY_GETTER = new VlanBindingsUnMetDependencyGetter();

    public PhysicalPortUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> createds =
                extractCreated(getChanges(),HwvtepPhysicalPortAugmentation.class);
        if (!createds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> created:
                createds.entrySet()) {
                updatePhysicalPort(transaction,  created.getKey(), created.getValue());
            }
        }
        Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> updateds =
                extractUpdatedPorts(getChanges(), HwvtepPhysicalPortAugmentation.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> updated:
                updateds.entrySet()) {
                updatePhysicalPort(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    public void updatePhysicalPort(final TransactionBuilder transaction,
                                   final InstanceIdentifier<Node> psNodeiid,
                                   final List<HwvtepPhysicalPortAugmentation> listPort) {
        //Get physical switch which the port belong to: in operation DS or new created
        for (HwvtepPhysicalPortAugmentation port : listPort) {
            LOG.debug("Creating a physical port named: {}", port.getHwvtepNodeName().getValue());
            HwvtepDeviceInfo.DeviceData deviceOperdata = getDeviceInfo().getDeviceOperData(TerminationPoint.class,
                    getTpIid(psNodeiid, port.getHwvtepNodeName().getValue()));
            if (deviceOperdata == null) {
                //create a physical port always happens from device
                LOG.error("Physical port {} not present in oper datastore", port.getHwvtepNodeName().getValue());
            } else {
                PhysicalPort physicalPort = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                        PhysicalPort.class);
                physicalPort.setName(port.getHwvtepNodeName().getValue());
                setVlanBindings(psNodeiid, physicalPort, port, transaction);
                setDescription(physicalPort, port);
                String existingPhysicalPortName = port.getHwvtepNodeName().getValue();
                PhysicalPort extraPhyscialPort =
                        TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), PhysicalPort.class);
                extraPhyscialPort.setName("");
                LOG.trace("execute: updating physical port: {}", physicalPort);
                transaction.add(op.update(physicalPort)
                        .where(extraPhyscialPort.getNameColumn().getSchema().opEqual(existingPhysicalPortName))
                        .build());
                transaction.add(op.comment("Physical Port: Updating " + existingPhysicalPortName));
                updateControllerTxHistory(TransactionType.UPDATE, physicalPort);
            }
        }
    }

    private void setDescription(PhysicalPort physicalPort, HwvtepPhysicalPortAugmentation inputPhysicalPort) {
        if (inputPhysicalPort.getHwvtepNodeDescription() != null) {
            physicalPort.setDescription(inputPhysicalPort.getHwvtepNodeDescription());
        }
    }

    private void setVlanBindings(final InstanceIdentifier<Node> psNodeiid,
                                 final PhysicalPort physicalPort,
                                 final HwvtepPhysicalPortAugmentation inputPhysicalPort,
                                 final TransactionBuilder transaction) {
        if (inputPhysicalPort.getVlanBindings() != null) {
            //get UUID by LogicalSwitchRef
            Map<Long, UUID> bindingMap = new HashMap<>();
            for (VlanBindings vlanBinding: inputPhysicalPort.getVlanBindings()) {
                InstanceIdentifier<VlanBindings> vlanIid = getVlanBindingIid(psNodeiid, physicalPort, vlanBinding);
                @SuppressWarnings("unchecked")
                InstanceIdentifier<LogicalSwitches> lswitchIid =
                        (InstanceIdentifier<LogicalSwitches>) vlanBinding.getLogicalSwitchRef().getValue();

                Map inTransitDependencies = DEPENDENCY_GETTER.getInTransitDependencies(
                        getOperationalState(), vlanBinding);
                Map configDependencies = DEPENDENCY_GETTER.getUnMetConfigDependencies(
                        getOperationalState(), vlanBinding);

                if (!HwvtepSouthboundUtil.isEmptyMap(configDependencies)) {
                    createConfigWaitJob(psNodeiid, inputPhysicalPort,
                            vlanBinding, configDependencies, vlanIid);
                    continue;
                }
                if (!HwvtepSouthboundUtil.isEmptyMap(inTransitDependencies)) {
                    createOperWaitingJob(psNodeiid, inputPhysicalPort,
                            vlanBinding, inTransitDependencies, vlanIid);
                    continue;
                }

                bindingMap.put(vlanBinding.getVlanIdKey().getValue().longValue(),
                        TransactUtils.getLogicalSwitchUUID(transaction, getOperationalState(), lswitchIid));
            }
            physicalPort.setVlanBindings(bindingMap);
        }
    }

    private void createOperWaitingJob(final InstanceIdentifier<Node> psNodeiid,
                                      final HwvtepPhysicalPortAugmentation inputPhysicalPort,
                                      final VlanBindings vlanBinding,
                                      final Map inTransitDependencies,
                                      final InstanceIdentifier<VlanBindings> vlanIid) {

        DependentJob<VlanBindings> opWaitingJob = new DependentJob.OpWaitingJob<VlanBindings>(
                vlanIid, vlanBinding, inTransitDependencies) {
            @Override
            public void onDependencyResolved(final HwvtepOperationalState operationalState,
                                             final TransactionBuilder transactionBuilder) {
                PhysicalPortUpdateCommand.this.threadLocalOperationalState.set(operationalState);
                PhysicalPortUpdateCommand.this.threadLocalDeviceTransaction.set(transactionBuilder);
                updatePhysicalPort(transactionBuilder, psNodeiid, Lists.newArrayList(inputPhysicalPort));
            }
        };
        getDeviceInfo().addJobToQueue(opWaitingJob);
    }

    private void createConfigWaitJob(final InstanceIdentifier<Node> psNodeiid,
                                     final HwvtepPhysicalPortAugmentation inputPhysicalPort,
                                     final VlanBindings vlanBinding,
                                     final Map configDependencies,
                                     final InstanceIdentifier<VlanBindings> vlanIid) {

        DependentJob<VlanBindings> configWaitingJob = new DependentJob.ConfigWaitingJob<VlanBindings>(
                vlanIid, vlanBinding, configDependencies) {
            @Override
            public void onDependencyResolved(final HwvtepOperationalState operationalState,
                                             final TransactionBuilder transactionBuilder) {
                PhysicalPortUpdateCommand.this.threadLocalOperationalState.set(operationalState);
                PhysicalPortUpdateCommand.this.threadLocalDeviceTransaction.set(transactionBuilder);
                updatePhysicalPort(transactionBuilder, psNodeiid, Lists.newArrayList(inputPhysicalPort));
            }
        };
        getDeviceInfo().addJobToQueue(configWaitingJob);
    }

    private InstanceIdentifier<TerminationPoint> getTpIid(final InstanceIdentifier<Node> psNodeiid,
                                                          final String portName) {
        return psNodeiid.child(
                TerminationPoint.class, new TerminationPointKey(new TpId(portName)));
    }

    private InstanceIdentifier<VlanBindings> getVlanBindingIid(
            final InstanceIdentifier<Node> psNodeiid,
            final PhysicalPort physicalPort,
            final VlanBindings vlanBinding) {

        return psNodeiid.child(
                TerminationPoint.class, new TerminationPointKey(new TpId(physicalPort.getName())))
                .augmentation(HwvtepPhysicalPortAugmentation.class)
                .child(VlanBindings.class, new VlanBindingsKey(vlanBinding.getVlanIdKey()));
    }

    static class VlanBindingsUnMetDependencyGetter extends UnMetDependencyGetter<VlanBindings> {

        @Override
        public List<InstanceIdentifier<?>> getLogicalSwitchDependencies(VlanBindings data) {
            if (data == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(data.getLogicalSwitchRef().getValue());
        }

        @Override
        public List<InstanceIdentifier<?>> getTerminationPointDependencies(VlanBindings data) {
            return Collections.emptyList();
        }
    }

    private Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<HwvtepPhysicalPortAugmentation> class1) {
        Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    List<HwvtepPhysicalPortAugmentation> portListUpdated = new ArrayList<>();
                    if (created.getTerminationPoint() != null) {
                        for (TerminationPoint tp : created.getTerminationPoint()) {
                            HwvtepPhysicalPortAugmentation hppAugmentation =
                                    tp.getAugmentation(HwvtepPhysicalPortAugmentation.class);
                            if (hppAugmentation != null) {
                                portListUpdated.add(hppAugmentation);
                            }
                        }
                    }
                    result.put(key, portListUpdated);
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> extractUpdatedPorts(
            Collection<DataTreeModification<Node>> changes, Class<HwvtepPhysicalPortAugmentation> class1) {
        Map<InstanceIdentifier<Node>, List<HwvtepPhysicalPortAugmentation>> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<HwvtepPhysicalPortAugmentation> portListUpdated = new ArrayList<>();
                    List<HwvtepPhysicalPortAugmentation> portListBefore = new ArrayList<>();
                    if (updated.getTerminationPoint() != null) {
                        for (TerminationPoint tp : updated.getTerminationPoint()) {
                            HwvtepPhysicalPortAugmentation hppAugmentation =
                                    tp.getAugmentation(HwvtepPhysicalPortAugmentation.class);
                            if (hppAugmentation != null) {
                                portListUpdated.add(hppAugmentation);
                            }
                        }
                    }
                    if (before.getTerminationPoint() != null) {
                        for (TerminationPoint tp : before.getTerminationPoint()) {
                            HwvtepPhysicalPortAugmentation hppAugmentation =
                                    tp.getAugmentation(HwvtepPhysicalPortAugmentation.class);
                            if (hppAugmentation != null) {
                                portListBefore.add(hppAugmentation);
                            }
                        }
                    }
                    portListUpdated.removeAll(portListBefore);
                    result.put(key, portListUpdated);
                }
            }
        }
        return result;
    }
}
