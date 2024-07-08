/*
 * Copyright (c) 2015, 2016 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McastMacsRemoteRemoveCommand
        extends AbstractTransactCommand<RemoteMcastMacs, RemoteMcastMacsKey, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(McastMacsRemoteRemoveCommand.class);

    public McastMacsRemoteRemoveCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> removeds =
                extractRemoved(getChanges(),RemoteMcastMacs.class);
        if (!removeds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<RemoteMcastMacs>> removed:
                removeds.entrySet()) {
                removeMcastMacRemote(transaction,  removed.getKey(), removed.getValue());
            }
        }
        //Remove the ones whose locator set got emptied
        Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> updated =
                extractUpdated(getChanges(),RemoteMcastMacs.class);
        if (!HwvtepSouthboundUtil.isEmptyMap(updated)) {
            for (Entry<InstanceIdentifier<Node>, List<RemoteMcastMacs>> entry:
                    updated.entrySet()) {
                List<RemoteMcastMacs> updatedList = entry.getValue();
                List<RemoteMcastMacs> tobeRemovedList = new ArrayList<>();
                if (!HwvtepSouthboundUtil.isEmpty(updatedList)) {
                    for (RemoteMcastMacs mac: updatedList) {
                        if (HwvtepSouthboundUtil.isEmpty(mac.getLocatorSet())) {
                            tobeRemovedList.add(mac);
                        }
                    }
                    removeMcastMacRemote(transaction, entry.getKey(), tobeRemovedList);
                }
            }
        }
    }

    private void removeMcastMacRemote(final TransactionBuilder transaction,
                                      final InstanceIdentifier<Node> nodeIid, final List<RemoteMcastMacs> macList) {
        for (RemoteMcastMacs mac : macList) {
            InstanceIdentifier<RemoteMcastMacs> macKey = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                    .child(RemoteMcastMacs.class, mac.key());
            onConfigUpdate(transaction, nodeIid, mac, macKey);
        }
    }

    @Override
    public void onConfigUpdate(final TransactionBuilder transaction,
                               final InstanceIdentifier<Node> nodeIid,
                               final RemoteMcastMacs remoteMcastMac,
                               final InstanceIdentifier macKey,
                               final Object... extraData) {
        processDependencies(EmptyDependencyGetter.INSTANCE, transaction, nodeIid, macKey, remoteMcastMac);
    }

    @Override
    public void doDeviceTransaction(final TransactionBuilder transaction,
                                    final InstanceIdentifier<Node> instanceIdentifier,
                                    final RemoteMcastMacs mac,
                                    final InstanceIdentifier macIid,
                                    final Object... extraData) {
        String nodeId = instanceIdentifier.firstKeyOf(Node.class).getNodeId().getValue();
        clearConfigData(RemoteMcastMacs.class, macIid);
        long transactionId = getOperationalState().getTransactionId();
        LOG.debug("Remove received for RemoteMcastMacs key: {} txId: {}", macIid, transactionId);
        HwvtepDeviceInfo.DeviceData deviceData = getDeviceOpData(RemoteMcastMacs.class, macIid);
        McastMacsRemote mcastMacsRemote = transaction.getTypedRowSchema(McastMacsRemote.class);
        boolean deleted = false;
        if (deviceData != null && deviceData.getData() != null && deviceData.getData() instanceof McastMacsRemote
                && ((McastMacsRemote)deviceData.getData()).getLogicalSwitchColumn() != null) {
            UUID logicalSwitchUid = ((McastMacsRemote)deviceData.getData()).getLogicalSwitchColumn().getData();
            if (logicalSwitchUid != null) {
                transaction.add(op.delete(mcastMacsRemote.getSchema())
                        .where(mcastMacsRemote.getLogicalSwitchColumn().getSchema().opEqual(logicalSwitchUid)).build());
                deleted = true;
                updateCurrentTxDeleteData(RemoteMcastMacs.class, macIid, mac);
                updateControllerTxHistory(TransactionType.DELETE, new StringBuilder(mcastMacsRemote.toString())
                        .append(":  LS: ").append(logicalSwitchUid));
                LOG.info("CONTROLLER - {} {} LS:{} Node:{}", TransactionType.DELETE,
                    mcastMacsRemote, logicalSwitchUid, nodeId);

            }
        }
        if (!deleted && deviceData != null) {
            UUID macEntryUUID = deviceData.getUuid();
            if (macEntryUUID != null) {
                mcastMacsRemote.getUuidColumn().setData(macEntryUUID);
                updateCurrentTxDeleteData(RemoteMcastMacs.class, macIid, mac);
                transaction.add(op.delete(mcastMacsRemote.getSchema())
                        .where(mcastMacsRemote.getUuidColumn().getSchema().opEqual(macEntryUUID)).build());
                updateControllerTxHistory(TransactionType.DELETE, new StringBuilder(mcastMacsRemote.toString())
                        .append(":  Mac : ").append(macEntryUUID));
                LOG.info("CONTROLLER - {} {} Mac :{} Node:{}", TransactionType.DELETE,
                    mcastMacsRemote, macEntryUUID, nodeId);
            } else {
                LOG.error("Failed to delete remote mcast entry as it is not found in device {}", macIid);
                getDeviceInfo().clearConfigData(RemoteMcastMacs.class, macIid);
                return;
            }
        }
    }

    @Override
    protected Map<RemoteMcastMacsKey, RemoteMcastMacs> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getRemoteMcastMacs();
    }

    @Override
    protected boolean areEqual(final RemoteMcastMacs macs1, final RemoteMcastMacs macs2) {
        return macs1.key().equals(macs2.key()) && Objects.equals(macs1.getLocatorSet(), macs2.getLocatorSet());
    }

    @Override
    protected boolean isDeleteCmd() {
        return true;
    }

    @Override
    protected String getKeyStr(InstanceIdentifier<RemoteMcastMacs> iid) {
        return getLsKeyStr(iid.firstKeyOf(RemoteMcastMacs.class).getLogicalSwitchRef().getValue());
    }
}
