/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil.schemaMismatchLog;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class LogicalSwitchUpdateCommand extends AbstractTransactCommand<LogicalSwitches, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(LogicalSwitchUpdateCommand.class);

    public LogicalSwitchUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LogicalSwitches>> updateds =
                extractUpdated(getChanges(),LogicalSwitches.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LogicalSwitches>> updated:
                updateds.entrySet()) {
                updateLogicalSwitch(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    public void updateLogicalSwitch(final TransactionBuilder transaction,
                                     final InstanceIdentifier<Node> nodeIid, final List<LogicalSwitches> lswitchList) {
        for (LogicalSwitches lswitch: lswitchList) {
            InstanceIdentifier<LogicalSwitches> lsKey = nodeIid.augmentation(HwvtepGlobalAugmentation.class).
                    child(LogicalSwitches.class, lswitch.getKey());
            onConfigUpdate(transaction, nodeIid, lswitch, lsKey);
        }
    }

    @Override
    public void onConfigUpdate(final TransactionBuilder transaction,
                               final InstanceIdentifier<Node> nodeIid,
                               final LogicalSwitches lswitch,
                               final InstanceIdentifier lsKey,
                               final Object... extraData) {
        processDependencies(null, transaction, nodeIid, lsKey, lswitch);
    }

    @Override
    public void doDeviceTransaction(final TransactionBuilder transaction,
                                    final InstanceIdentifier<Node> instanceIdentifier,
                                    final LogicalSwitches lswitch,
                                    final InstanceIdentifier lsKey,
                                    final Object... extraData) {
            LOG.debug("Creating logical switch named: {}", lswitch.getHwvtepNodeName());
            HwvtepDeviceInfo.DeviceData operationalSwitchOptional =
                    getDeviceInfo().getDeviceOperData(LogicalSwitches.class, lsKey);
            LogicalSwitch logicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), LogicalSwitch.class);
            setDescription(logicalSwitch, lswitch);
            setTunnelKey(logicalSwitch, lswitch);
            setReplicationMode(logicalSwitch, lswitch);
            if (operationalSwitchOptional == null) {
                setName(logicalSwitch, lswitch);
                LOG.trace("execute: creating LogicalSwitch entry: {}", logicalSwitch);
                transaction.add(op.insert(logicalSwitch).withId(TransactUtils.getLogicalSwitchId(lswitch)));
                transaction.add(op.comment("Logical Switch: Creating " + lswitch.getHwvtepNodeName().getValue()));
                UUID lsUuid = new UUID(TransactUtils.getLogicalSwitchId(lswitch));
                updateCurrentTxData(LogicalSwitches.class, lsKey, lsUuid, lswitch);
            } else {
                String existingLogicalSwitchName = lswitch.getHwvtepNodeName().getValue();
                // Name is immutable, and so we *can't* update it.  So we use extraBridge for the schema stuff
                LogicalSwitch extraLogicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), LogicalSwitch.class);
                extraLogicalSwitch.setName("");
                LOG.trace("execute: updating LogicalSwitch entry: {}", logicalSwitch);
                transaction.add(op.update(logicalSwitch)
                        .where(extraLogicalSwitch.getNameColumn().getSchema().opEqual(existingLogicalSwitchName))
                        .build());
                transaction.add(op.comment("Logical Switch: Updating " + existingLogicalSwitchName));
            }
    }

    private void setDescription(LogicalSwitch logicalSwitch, LogicalSwitches inputSwitch) {
        if(inputSwitch.getHwvtepNodeDescription() != null) {
            logicalSwitch.setDescription(inputSwitch.getHwvtepNodeDescription());
        }
    }

    private void setName(LogicalSwitch logicalSwitch, LogicalSwitches inputSwitch) {
        if (inputSwitch.getHwvtepNodeName() != null) {
            logicalSwitch.setName(inputSwitch.getHwvtepNodeName().getValue());
        }
    }

    private void setTunnelKey(LogicalSwitch logicalSwitch, LogicalSwitches inputSwitch) {
        if (inputSwitch.getTunnelKey() != null) {
            Set<Long> tunnel = new HashSet<>();
            tunnel.add(Long.valueOf(inputSwitch.getTunnelKey()));
            logicalSwitch.setTunnelKey(tunnel);
        }
    }

    private void setReplicationMode(LogicalSwitch logicalSwitch, LogicalSwitches inputSwitch) {
        if (inputSwitch.getReplicationMode() != null) {
            Set<String> mode = new HashSet<>();
            mode.add(inputSwitch.getReplicationMode());
            try {
                logicalSwitch.setReplicationMode(mode);
            }
            catch (SchemaVersionMismatchException e) {
                schemaMismatchLog("replication_mode", "Logical_Switch", e);
            }
        }
    }

    @Override
    protected List<LogicalSwitches> getData(HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLogicalSwitches();
    }

    @Override
    protected boolean areEqual(LogicalSwitches a , LogicalSwitches b) {
        return a.getKey().equals(b.getKey()) && Objects.equals(a.getTunnelKey(), b.getTunnelKey());
    }
}
