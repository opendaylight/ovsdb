/*
 * Copyright (c) 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicalSwitchRemoveCommand extends AbstractTransactCommand<LogicalSwitches, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(LogicalSwitchRemoveCommand.class);

    public LogicalSwitchRemoveCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LogicalSwitches>> removeds =
                extractRemoved(getChanges(),LogicalSwitches.class);

        for (Entry<InstanceIdentifier<Node>, List<LogicalSwitches>> created: removeds.entrySet()) {
            if (!HwvtepSouthboundUtil.isEmpty(created.getValue())) {
                HwvtepConnectionInstance connectionInstance = getDeviceInfo().getConnectionInstance();
                getDeviceInfo().scheduleTransaction(new TransactCommand() {
                    @Override
                    public void execute(final TransactionBuilder transactionBuilder) {
                        HwvtepOperationalState operState = new HwvtepOperationalState(
                                connectionInstance.getDataBroker(), connectionInstance, Collections.EMPTY_LIST);
                        hwvtepOperationalState = operState;
                        deviceTransaction = deviceTransaction;
                        LOG.debug("Running delete logical switch in seperate tx {}", created.getKey());
                        removeLogicalSwitch(transactionBuilder, created.getKey(), created.getValue());
                    }

                    @Override
                    public void onSuccess(final TransactionBuilder deviceTransaction) {
                        LogicalSwitchRemoveCommand.this.onSuccess(deviceTransaction);
                    }

                    @Override
                    public void onFailure(final TransactionBuilder deviceTransaction) {
                        LogicalSwitchRemoveCommand.this.onFailure(deviceTransaction);
                    }
                });
            }
        }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void removeLogicalSwitch(final TransactionBuilder transaction,
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
        processDependencies(null, transaction, nodeIid, lsKey, lswitch);
    }

    @Override
    public void doDeviceTransaction(final TransactionBuilder transaction,
                                    final InstanceIdentifier<Node> instanceIdentifier,
                                    final LogicalSwitches lswitch,
                                    final InstanceIdentifier lsKey,
                                    final Object... extraData) {
        LOG.debug("Removing logical switch named: {}", lswitch.getHwvtepNodeName().getValue());
        HwvtepDeviceInfo.DeviceData deviceData  = getOperationalState().getDeviceInfo().getDeviceOperData(
                LogicalSwitches.class, lsKey);
        LogicalSwitch logicalSwitch = transaction.getTypedRowSchema(LogicalSwitch.class);

        if (deviceData != null && deviceData.getUuid() != null) {
            UUID logicalSwitchUuid = deviceData.getUuid();
            transaction.add(op.delete(logicalSwitch.getSchema())
                    .where(logicalSwitch.getUuidColumn().getSchema().opEqual(logicalSwitchUuid)).build());

            UcastMacsRemote ucastMacsRemote = transaction.getTypedRowSchema(UcastMacsRemote.class);
            transaction.add(op.delete(ucastMacsRemote.getSchema())
                    .where(ucastMacsRemote.getLogicalSwitchColumn().getSchema().opEqual(logicalSwitchUuid)).build());

            UcastMacsLocal ucastMacsLocal = transaction.getTypedRowSchema(UcastMacsLocal.class);
            transaction.add(op.delete(ucastMacsLocal.getSchema())
                    .where(ucastMacsLocal.getLogicalSwitchColumn().getSchema().opEqual(logicalSwitchUuid)).build());

            McastMacsRemote mcastMacsRemote = transaction.getTypedRowSchema(McastMacsRemote.class);
            transaction.add(op.delete(mcastMacsRemote.getSchema())
                    .where(mcastMacsRemote.getLogicalSwitchColumn().getSchema().opEqual(logicalSwitchUuid)).build());

            McastMacsLocal mcastMacsLocal = transaction.getTypedRowSchema(McastMacsLocal.class);
            transaction.add(op.delete(mcastMacsLocal.getSchema())
                    .where(mcastMacsLocal.getLogicalSwitchColumn().getSchema().opEqual(logicalSwitchUuid)).build());
            updateCurrentTxDeleteData(LogicalSwitches.class, lsKey, lswitch);
            updateControllerTxHistory(TransactionType.DELETE, lswitch);
        } else {
            LOG.warn("Unable to delete logical switch {} because it was not found in the operational store",
                    lswitch.getHwvtepNodeName().getValue());
        }
    }

    @Override
    protected List<LogicalSwitches> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLogicalSwitches();
    }

    @Override
    protected boolean areEqual(final LogicalSwitches sw1, final LogicalSwitches sw2) {
        return sw1.key().equals(sw2.key()) && Objects.equals(sw1.getTunnelKey(), sw2.getTunnelKey());
    }

    @Override
    protected boolean isDeleteCmd() {
        return true;
    }

    @Override
    public void onCommandSucceeded() {
        if (getDeviceTransaction() == null || !updates.containsKey(getDeviceTransaction())) {
            return;
        }
        for (MdsalUpdate mdsalUpdate : updates.get(getDeviceTransaction())) {
            getDeviceInfo().clearLogicalSwitchRefs(mdsalUpdate.getKey());
        }
    }
}
