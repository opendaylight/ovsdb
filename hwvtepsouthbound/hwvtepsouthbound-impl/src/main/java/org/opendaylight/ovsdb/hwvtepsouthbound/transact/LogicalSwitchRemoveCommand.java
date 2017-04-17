/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class LogicalSwitchRemoveCommand extends AbstractTransactCommand<LogicalSwitches, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(LogicalSwitchRemoveCommand.class);

    public LogicalSwitchRemoveCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LogicalSwitches>> removeds =
                extractRemoved(getChanges(),LogicalSwitches.class);
        if (removeds != null) {
            for (Entry<InstanceIdentifier<Node>, List<LogicalSwitches>> created: removeds.entrySet()) {
                if (!HwvtepSouthboundUtil.isEmpty(created.getValue())) {
                    for (LogicalSwitches lswitch : created.getValue()) {
                        InstanceIdentifier<LogicalSwitches> lsKey = created.getKey().augmentation(
                                HwvtepGlobalAugmentation.class).child(LogicalSwitches.class, lswitch.getKey());
                        updateCurrentTxDeleteData(LogicalSwitches.class, lsKey, lswitch);
                    }
                    getOperationalState().getDeviceInfo().scheduleTransaction(new TransactCommand() {
                        @Override
                        public void execute(TransactionBuilder transactionBuilder) {
                            LOG.debug("Running delete logical switch in seperate tx {}", created.getKey());
                            removeLogicalSwitch(transactionBuilder, created.getKey(), created.getValue());
                        }

                        @Override
                        public void onConfigUpdate(TransactionBuilder transaction, InstanceIdentifier nodeIid,
                                                   Identifiable data, InstanceIdentifier key, Object... extraData) {
                        }

                        @Override
                        public void doDeviceTransaction(TransactionBuilder transaction, InstanceIdentifier nodeIid,
                                                        Identifiable data, InstanceIdentifier key, Object... extraData) {
                        }
                    });
                }
            }
        }
    }

    private void removeLogicalSwitch(final TransactionBuilder transaction,
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
            LOG.debug("Removing logcial switch named: {}", lswitch.getHwvtepNodeName().getValue());
            Optional<LogicalSwitches> operationalSwitchOptional =
                    getOperationalState().getLogicalSwitches(instanceIdentifier, lswitch.getKey());
            LogicalSwitch logicalSwitch = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), LogicalSwitch.class, null);

            if (operationalSwitchOptional.isPresent() &&
                    operationalSwitchOptional.get().getLogicalSwitchUuid() != null) {
                UUID logicalSwitchUuid = new UUID(operationalSwitchOptional.get().getLogicalSwitchUuid().getValue());
                transaction.add(op.delete(logicalSwitch.getSchema())
                        .where(logicalSwitch.getUuidColumn().getSchema().opEqual(logicalSwitchUuid)).build());
                transaction.add(op.comment("Logical Switch: Deleting " + lswitch.getHwvtepNodeName().getValue()));
                getOperationalState().getDeviceInfo().markKeyAsInTransit(RemoteMcastMacs.class, lsKey);
            } else {
                LOG.warn("Unable to delete logical switch {} because it was not found in the operational store",
                        lswitch.getHwvtepNodeName().getValue());
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

    @Override
    protected boolean isRemoveCommand() {
        return true;
    }
}
