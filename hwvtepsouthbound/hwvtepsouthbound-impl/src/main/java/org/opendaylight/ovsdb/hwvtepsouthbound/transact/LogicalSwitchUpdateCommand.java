/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil.schemaMismatchLog;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicalSwitchUpdateCommand
        extends AbstractTransactCommand<LogicalSwitches, LogicalSwitchesKey, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(LogicalSwitchUpdateCommand.class);

    public LogicalSwitchUpdateCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
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
            InstanceIdentifier<LogicalSwitches> lsKey = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                    .child(LogicalSwitches.class, lswitch.key());
            onConfigUpdate(transaction, nodeIid, lswitch, lsKey);
        }
    }

    @Override
    public void onConfigUpdate(final TransactionBuilder transaction,
                               final InstanceIdentifier<Node> nodeIid,
                               final LogicalSwitches lswitch,
                               final InstanceIdentifier lsKey,
                               final Object... extraData) {
        processDependencies(EmptyDependencyGetter.INSTANCE, transaction, nodeIid, lsKey, lswitch);
    }

    @Override
    public void doDeviceTransaction(final TransactionBuilder transaction,
                                    final InstanceIdentifier<Node> instanceIdentifier,
                                    final LogicalSwitches lswitch,
                                    final InstanceIdentifier lsKey,
                                    final Object... extraData) {
        LOG.debug("Creating logical switch named: {}", lswitch.getHwvtepNodeName());
        final HwvtepDeviceInfo.DeviceData operationalSwitchOptional =
                getDeviceInfo().getDeviceOperData(LogicalSwitches.class, lsKey);
        LogicalSwitch logicalSwitch = transaction.getTypedRowWrapper(LogicalSwitch.class);
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
            updateControllerTxHistory(TransactionType.ADD, logicalSwitch);
            LOG.info("CONTROLLER - {} {}", TransactionType.ADD, logicalSwitch);
        } else {
            String existingLogicalSwitchName = lswitch.getHwvtepNodeName().getValue();
            // Name is immutable, and so we *can't* update it.  So we use extraBridge for the schema stuff
            LogicalSwitch extraLogicalSwitch = transaction.getTypedRowWrapper(LogicalSwitch.class);
            extraLogicalSwitch.setName("");
            LOG.trace("execute: updating LogicalSwitch entry: {}", logicalSwitch);
            transaction.add(op.update(logicalSwitch)
                    .where(extraLogicalSwitch.getNameColumn().getSchema().opEqual(existingLogicalSwitchName))
                    .build());
            transaction.add(op.comment("Logical Switch: Updating " + existingLogicalSwitchName));
            updateControllerTxHistory(TransactionType.UPDATE, logicalSwitch);
            LOG.info("CONTROLLER - {} {}", TransactionType.UPDATE, logicalSwitch);
        }
    }

    private static void setDescription(final LogicalSwitch logicalSwitch, final LogicalSwitches inputSwitch) {
        if (inputSwitch.getHwvtepNodeDescription() != null) {
            logicalSwitch.setDescription(inputSwitch.getHwvtepNodeDescription());
        }
    }

    private static void setName(final LogicalSwitch logicalSwitch, final LogicalSwitches inputSwitch) {
        if (inputSwitch.getHwvtepNodeName() != null) {
            logicalSwitch.setName(inputSwitch.getHwvtepNodeName().getValue());
        }
    }

    private static void setTunnelKey(final LogicalSwitch logicalSwitch, final LogicalSwitches inputSwitch) {
        if (inputSwitch.getTunnelKey() != null) {
            Set<Long> tunnel = new HashSet<>();
            tunnel.add(Long.valueOf(inputSwitch.getTunnelKey()));
            logicalSwitch.setTunnelKey(tunnel);
        }
    }

    private static void setReplicationMode(final LogicalSwitch logicalSwitch, final LogicalSwitches inputSwitch) {
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
    protected Map<LogicalSwitchesKey, LogicalSwitches> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLogicalSwitches();
    }

    @Override
    protected boolean areEqual(final LogicalSwitches sw1, final LogicalSwitches sw2) {
        return sw1.key().equals(sw2.key()) && Objects.equals(sw1.getTunnelKey(), sw2.getTunnelKey());
    }

    @Override
    protected String getKeyStr(InstanceIdentifier<LogicalSwitches> iid) {
        return iid.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue();
    }
}
