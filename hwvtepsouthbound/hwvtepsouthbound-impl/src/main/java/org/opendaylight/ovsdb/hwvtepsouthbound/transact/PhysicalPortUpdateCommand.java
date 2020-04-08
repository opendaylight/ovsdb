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
        //Get physical switch which the port belong to: in operation DS or new created
        for (TerminationPoint tp : listPort) {
            HwvtepPhysicalPortAugmentation port = tp.augmentation(HwvtepPhysicalPortAugmentation.class);
            LOG.debug("Creating a physical port named: {}", port.getHwvtepNodeName().getValue());
            InstanceIdentifier<TerminationPoint> key = getTpIid(psNodeiid, port.getHwvtepNodeName().getValue());

            getOperationalState().getDeviceInfo().updateConfigData(VlanBindings.class, key, tp);
            HwvtepDeviceInfo.DeviceData deviceOperdata = getDeviceInfo().getDeviceOperData(VlanBindings.class, key);
            if (deviceOperdata == null || deviceOperdata.getData() == null) {
                LOG.error("Updated the device oper cache for port from actual device {}", key);
                deviceOperdata = super.fetchDeviceData(VlanBindings.class, key);
            }
            if (deviceOperdata == null || deviceOperdata.getData() == null) {
                //create a physical port always happens from device
                LOG.error("Physical port {} not present in oper datastore", port.getHwvtepNodeName().getValue());
            } else {
                PhysicalPort physicalPort = transaction.getTypedRowWrapper(PhysicalPort.class);
                physicalPort.setName(port.getHwvtepNodeName().getValue());
                setVlanBindings(psNodeiid, physicalPort, tp, transaction);
                setDescription(physicalPort, port);
                String existingPhysicalPortName = port.getHwvtepNodeName().getValue();
                PhysicalPort extraPhyscialPort = transaction.getTypedRowWrapper(PhysicalPort.class);
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

    private static void setDescription(final PhysicalPort physicalPort,
            final HwvtepPhysicalPortAugmentation inputPhysicalPort) {
        if (inputPhysicalPort.getHwvtepNodeDescription() != null) {
            physicalPort.setDescription(inputPhysicalPort.getHwvtepNodeDescription());
        }
    }

    private void setVlanBindings(final InstanceIdentifier<Node> psNodeiid,
                                 final PhysicalPort physicalPort,
                                 final TerminationPoint tp,
                                 final TransactionBuilder transaction) {
        HwvtepPhysicalPortAugmentation inputPhysicalPort = tp.augmentation(HwvtepPhysicalPortAugmentation.class);
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
                    createConfigWaitJob(psNodeiid, tp,
                            vlanBinding, configDependencies, vlanIid);
                    continue;
                }
                if (!HwvtepSouthboundUtil.isEmptyMap(inTransitDependencies)) {
                    createOperWaitingJob(psNodeiid, tp,
                            vlanBinding, inTransitDependencies, vlanIid);
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
        }
    }

    private void createOperWaitingJob(final InstanceIdentifier<Node> psNodeiid,
                                      final TerminationPoint inputPhysicalPort,
                                      final VlanBindings vlanBinding,
                                      final Map inTransitDependencies,
                                      final InstanceIdentifier<VlanBindings> vlanIid) {

        DependentJob<VlanBindings> opWaitingJob = new DependentJob.OpWaitingJob<VlanBindings>(
                vlanIid, vlanBinding, inTransitDependencies, getOperationalState().getTransactionId()) {
            @Override
            public void onDependencyResolved(final HwvtepOperationalState operationalState,
                                             final TransactionBuilder transactionBuilder) {
                hwvtepOperationalState = operationalState;
                deviceTransaction = transactionBuilder;
                updatePhysicalPort(transactionBuilder, psNodeiid, Lists.newArrayList(inputPhysicalPort));
            }
        };
        getDeviceInfo().addJobToQueue(opWaitingJob);
    }

    private void createConfigWaitJob(final InstanceIdentifier<Node> psNodeiid,
                                     final TerminationPoint inputPhysicalPort,
                                     final VlanBindings vlanBinding,
                                     final Map configDependencies,
                                     final InstanceIdentifier<VlanBindings> vlanIid) {

        DependentJob<VlanBindings> configWaitingJob = new DependentJob.ConfigWaitingJob<VlanBindings>(
                vlanIid, vlanBinding, configDependencies) {
            @Override
            public void onDependencyResolved(final HwvtepOperationalState operationalState,
                                             final TransactionBuilder transactionBuilder) {
                hwvtepOperationalState = operationalState;
                deviceTransaction = transactionBuilder;
                updatePhysicalPort(transactionBuilder, psNodeiid, Lists.newArrayList(inputPhysicalPort));
            }
        };
        getDeviceInfo().addJobToQueue(configWaitingJob);
    }

    private static InstanceIdentifier<TerminationPoint> getTpIid(final InstanceIdentifier<Node> psNodeiid,
                                                                 final String portName) {
        return psNodeiid.child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));
    }

    private static InstanceIdentifier<VlanBindings> getVlanBindingIid(final InstanceIdentifier<Node> psNodeiid,
                                                                      final PhysicalPort physicalPort,
                                                                      final VlanBindings vlanBinding) {
        return getTpIid(psNodeiid, physicalPort.getName())
                .augmentation(HwvtepPhysicalPortAugmentation.class)
                .child(VlanBindings.class, new VlanBindingsKey(vlanBinding.getVlanIdKey()));
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
                    if (created.getTerminationPoint() != null) {
                        for (TerminationPoint tp : created.getTerminationPoint()) {
                            HwvtepPhysicalPortAugmentation hppAugmentation =
                                    tp.augmentation(HwvtepPhysicalPortAugmentation.class);
                            if (hppAugmentation != null) {
                                portListUpdated.add(tp);
                            }
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
                    if (updated.getTerminationPoint() != null) {
                        for (TerminationPoint tp : updated.getTerminationPoint()) {
                            HwvtepPhysicalPortAugmentation hppAugmentation =
                                    tp.augmentation(HwvtepPhysicalPortAugmentation.class);
                            if (hppAugmentation != null) {
                                portListUpdated.add(tp);
                            }
                        }
                    }
                    if (before.getTerminationPoint() != null) {
                        for (TerminationPoint tp : before.getTerminationPoint()) {
                            HwvtepPhysicalPortAugmentation hppAugmentation =
                                    tp.augmentation(HwvtepPhysicalPortAugmentation.class);
                            if (hppAugmentation != null) {
                                portListBefore.add(tp);
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

    protected String getKeyStr(InstanceIdentifier iid) {
        try {
            return ((TerminationPoint)iid.firstKeyOf(TerminationPoint.class)).getTpId().getValue();
        } catch (ClassCastException exp) {
            LOG.error("Error in getting the TerminationPoint id ", exp);
        }
        return super.getKeyStr(iid);
    }

}
