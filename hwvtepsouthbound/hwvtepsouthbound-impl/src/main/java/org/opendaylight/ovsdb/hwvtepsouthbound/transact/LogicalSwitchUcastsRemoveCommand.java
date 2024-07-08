/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogicalSwitchUcastsRemoveCommand
        extends AbstractTransactCommand<LogicalSwitches, LogicalSwitchesKey, HwvtepGlobalAugmentation> {

    private static final Logger LOG = LoggerFactory.getLogger(LogicalSwitchUcastsRemoveCommand.class);

    private final AtomicInteger retryCount = new AtomicInteger(5);
    private final LogicalSwitches logicalSwitches;
    private final InstanceIdentifier<Node> nodeIid;
    private final List<LogicalSwitches> deletedLs;

    volatile Map<String, Map<Long, UUID>> updatedPortBindings = new HashMap<>();
    private boolean firstAttempt = true;

    public LogicalSwitchUcastsRemoveCommand(final HwvtepOperationalState state,
                                            final Collection<DataTreeModification<Node>> changes,
                                            final List<LogicalSwitches> deletedLs,
                                            final LogicalSwitches logicalSwitches) {
        super(state, changes);
        this.deletedLs = deletedLs;
        this.logicalSwitches = logicalSwitches;
        this.nodeIid = getOperationalState().getConnectionInstance().getInstanceIdentifier();
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        InstanceIdentifier<LogicalSwitches> lsKey = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, logicalSwitches.key());
        HwvtepDeviceInfo.DeviceData deviceData  = super.<LogicalSwitch>fetchDeviceData(LogicalSwitches.class, lsKey);
        
        if (deviceData != null && deviceData.getUuid() != null) {
            final var op = ops();

            UUID logicalSwitchUuid = deviceData.getUuid();

            UcastMacsRemote ucastMacsRemote = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    UcastMacsRemote.class, null);
            transaction.add(op.delete(ucastMacsRemote.getSchema())
                    .where(ucastMacsRemote.getLogicalSwitchColumn().getSchema().opEqual(logicalSwitchUuid)).build());

            UcastMacsLocal ucastMacsLocal = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    UcastMacsLocal.class, null);
            transaction.add(op.delete(ucastMacsLocal.getSchema())
                    .where(ucastMacsLocal.getLogicalSwitchColumn().getSchema().opEqual(logicalSwitchUuid)).build());

            McastMacsRemote mcastMacsRemote = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    McastMacsRemote.class, null);
            transaction.add(op.delete(mcastMacsRemote.getSchema())
                    .where(mcastMacsRemote.getLogicalSwitchColumn().getSchema().opEqual(logicalSwitchUuid)).build());

            McastMacsLocal mcastMacsLocal = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    McastMacsLocal.class, null);
            transaction.add(op.delete(mcastMacsLocal.getSchema())
                    .where(mcastMacsLocal.getLogicalSwitchColumn().getSchema().opEqual(logicalSwitchUuid)).build());

            if (firstAttempt) {
                LogicalSwitch logicalSwitch = TyperUtils.getTypedRowWrapper(
                        transaction.getDatabaseSchema(), LogicalSwitch.class, null);
                transaction.add(op.delete(logicalSwitch.getSchema()).where(logicalSwitch.getNameColumn().getSchema()
                        .opEqual(logicalSwitches.getHwvtepNodeName().getValue())).build());
                updateControllerTxHistory(TransactionType.DELETE, new StringBuilder(logicalSwitch.toString())
                        .append(": ").append(logicalSwitches.getHwvtepNodeName()).append(" ")
                        .append(logicalSwitches.getLogicalSwitchUuid()).append(" ")
                        .append(logicalSwitches.getTunnelKey()));
                LOG.info("CONTROLLER - {} {} {} {} {}", TransactionType.DELETE, logicalSwitch,
                        logicalSwitches.getHwvtepNodeName(), logicalSwitches.getLogicalSwitchUuid(),
                        logicalSwitches.getTunnelKey());
            } else {
                for (Entry<String, Map<Long, UUID>> entry : updatedPortBindings.entrySet()) {
                    PhysicalPort physicalPort = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                            PhysicalPort.class);
                    physicalPort.setName(entry.getKey());
                    physicalPort.setVlanBindings(entry.getValue());
                    transaction.add(op.update(physicalPort)
                            .where(physicalPort.getNameColumn().getSchema().opEqual(physicalPort.getName())).build());
                    updateControllerTxHistory(TransactionType.UPDATE, physicalPort);
                    LOG.info("CONTROLLER - {} {}", TransactionType.UPDATE, physicalPort);
                }
            }
        } else {
            firstAttempt = false;
            onSuccess(transaction);
        }
    }

    @Override
    protected Map<LogicalSwitchesKey, LogicalSwitches> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLogicalSwitches();
    }

    @Override
    protected boolean areEqual(final LogicalSwitches logicalSwitches1 , final LogicalSwitches logicalSwitches2) {
        return logicalSwitches1.key().equals(logicalSwitches2.key())
                && Objects.equals(logicalSwitches1.getTunnelKey(), logicalSwitches2.getTunnelKey());
    }

    @Override
    public void onSuccess(final TransactionBuilder tx) {
        if (firstAttempt) {
            //LOG.error("check succeeded in deletion of logical swtich at first attempt ");
            //succeed in removing the logical switch upon first attempt
            return;
        }
        //LOG.error("check succeeded in deletion of logical swtich after first attempt ");
        PlainLogicalSwitchRemoveCmd cmd = new PlainLogicalSwitchRemoveCmd(
                newOperState(), getChanges(), logicalSwitches, HwvtepSouthboundConstants.LS_REMOVE_RETRIES);
        hwvtepOperationalState.getConnectionInstance().transact(cmd);
    }

    @Override
    public void onFailure(final TransactionBuilder tx) {
        //Failed to remove logical swith upon first attempt,
        //will attempt to remove the local ucasts and vlan bindings alone in the next attemtps
        firstAttempt = false;
        getFreshPortBindingsExcludingDeleted();
        super.onFailure(tx);
    }

    private void getFreshPortBindingsExcludingDeleted() {
        Set<UUID> deletedLsUuids = new HashSet<>();
        for (LogicalSwitches ls : deletedLs) {
            InstanceIdentifier<LogicalSwitches> lsKey = nodeIid
                    .augmentation(HwvtepGlobalAugmentation.class)
                    .child(LogicalSwitches.class, ls.key());
            HwvtepDeviceInfo.DeviceData deviceData  =
                    super.<LogicalSwitch>fetchDeviceData(LogicalSwitches.class, lsKey);
            if (deviceData.getUuid() != null) {
                deletedLsUuids.add(deviceData.getUuid());
            }
        }
        updatedPortBindings = new HashMap<>();
        List<TypedBaseTable> portsFromDevice = getOperationalState().getConnectionInstance()
                .getHwvtepTableReader().getHwvtepTableEntries(VlanBindings.class);
        if (portsFromDevice == null || portsFromDevice.isEmpty()) {
            LOG.debug("Check did not get any bindings from tor while doing failure of logical switch delete");
            return;
        }
        portsFromDevice.stream()
                .map(row -> (PhysicalPort) row)
                .filter(port -> port.getVlanBindingsColumn() != null && port.getVlanBindingsColumn().getData() != null)
                .forEach(port -> {
                    int originalBindingsSize = port.getVlanBindingsColumn().getData().size();
                    Map<Long, UUID> bindingsAfterExclusion = excludeVlanBindings(deletedLsUuids, port);
                    if (bindingsAfterExclusion.size() < originalBindingsSize) {
                        updatedPortBindings.put(port.getName(), bindingsAfterExclusion);
                    }
                });
    }

    private static Map<Long, UUID> excludeVlanBindings(final Set<UUID> deletedLsUuids, final PhysicalPort port) {
        return port.getVlanBindingsColumn().getData()
                .entrySet().stream()
                .peek(entry -> {
                    if (deletedLsUuids.contains(entry.getValue())) {
                        LOG.trace("check Excluding the vlan binding {}", entry.getValue());
                    }
                })
                .filter(entry -> !deletedLsUuids.contains(entry.getValue()))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    @Override
    public boolean retry() {
        boolean ret = retryCount.decrementAndGet() > 0;
        return ret;
    }

    @Override
    protected boolean isDeleteCmd() {
        return true;
    }

    @Override
    protected String getKeyStr(InstanceIdentifier<LogicalSwitches> iid) {
        return iid.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue();
    }
}
