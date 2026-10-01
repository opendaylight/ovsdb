/*
 * Copyright (c) 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UcastMacsRemoteRemoveCommand
        extends AbstractTransactCommand<RemoteUcastMacs, RemoteUcastMacsKey, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(UcastMacsRemoteRemoveCommand.class);

    public UcastMacsRemoteRemoveCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<DataObjectIdentifier<Node>, List<RemoteUcastMacs>> removeds =
                extractRemoved(getChanges(),RemoteUcastMacs.class);
        if (!removeds.isEmpty()) {
            for (var removed : removeds.entrySet()) {
                onConfigUpdate(transaction, removed.getKey(), removed.getValue());
            }
        }
    }

    public void onConfigUpdate(final TransactionBuilder transaction,
                               final DataObjectIdentifier<Node> nodeIid,
                               final List<RemoteUcastMacs> macs) {
        for (RemoteUcastMacs mac : macs) {
            var macKey = nodeIid.toBuilder()
                .augmentation(HwvtepGlobalAugmentation.class)
                .child(RemoteUcastMacs.class, mac.key())
                .build();
            getDeviceInfo().clearConfigData(RemoteUcastMacs.class, macKey);
            onConfigUpdate(transaction, nodeIid, mac, macKey);
        }
    }

    @Override
    public void onConfigUpdate(final TransactionBuilder transaction,
                               final DataObjectIdentifier<Node> nodeIid,
                               final RemoteUcastMacs remoteMcastMac,
                               final DataObjectIdentifier macKey,
                               final Object... extraData) {
        processDependencies(EmptyDependencyGetter.INSTANCE, transaction, nodeIid, macKey, remoteMcastMac);
    }

    @Override
    public void doDeviceTransaction(final TransactionBuilder transaction,
                                    final DataObjectIdentifier<Node> instanceIdentifier,
                                    final RemoteUcastMacs mac,
                                    final DataObjectIdentifier macKey,
                                    final Object... extraData) {
        removeUcastMacRemote(transaction, instanceIdentifier, List.of(mac));
    }

    private void removeUcastMacRemote(final TransactionBuilder transaction,
                                      final DataObjectIdentifier<Node> instanceIdentifier,
                                      final List<RemoteUcastMacs> macList) {
        String nodeId = instanceIdentifier.getFirstKeyOf(Node.class).getNodeId().getValue();
        final var op = ops();

        for (RemoteUcastMacs mac: macList) {
            final var macIid = instanceIdentifier.toBuilder()
                .augmentation(HwvtepGlobalAugmentation.class)
                .child(RemoteUcastMacs.class, mac.key())
                .build();
            HwvtepDeviceInfo.DeviceData deviceData = getDeviceOpData(RemoteUcastMacs.class, macIid);
            UcastMacsRemote ucastMacsRemote = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    UcastMacsRemote.class, null);
            LOG.debug("Remove received for remoteUcastMacs, key: {} txId: {}", macIid,
                    getOperationalState().getTransactionId());
            boolean deleted = false;
            if (deviceData != null && deviceData.getUuid() != null) {
                if (deviceData.getData() != null && deviceData.getData() instanceof UcastMacsRemote
                        && ((UcastMacsRemote)deviceData.getData()).getLogicalSwitchColumn() != null) {
                    UUID logicalSwitchUid = ((UcastMacsRemote) deviceData.getData()).getLogicalSwitchColumn().getData();
                    if (logicalSwitchUid != null) {
                        deleted = true;
                        updateCurrentTxDeleteData(RemoteUcastMacs.class, macIid, mac);
                        transaction.add(op.delete(ucastMacsRemote.getSchema())
                                .where(ucastMacsRemote.getLogicalSwitchColumn().getSchema()
                                        .opEqual(logicalSwitchUid))
                                .and(ucastMacsRemote.getMacColumn().getSchema().opEqual(mac.getMacEntryKey()
                                        .getValue())).build());
                        LOG.info("CONTROLLER - {} Mac:{} LS:{} Node:{}", TransactionType.DELETE,
                            mac.getMacEntryKey(), logicalSwitchUid, nodeId);

                    }
                }
            }
            if (!deleted && deviceData != null && deviceData.getUuid() != null) {
                updateCurrentTxDeleteData(RemoteUcastMacs.class, macIid, mac);
                UUID macEntryUUID = deviceData.getUuid();
                ucastMacsRemote.getUuidColumn().setData(macEntryUUID);
                transaction.add(op.delete(ucastMacsRemote.getSchema())
                        .where(ucastMacsRemote.getUuidColumn().getSchema().opEqual(macEntryUUID)).build());
                LOG.info("CONTROLLER - {} {} Node:{}", TransactionType.DELETE, ucastMacsRemote, nodeId);
            } else {
                LOG.trace("Remove failed to find in op datastore key:{} txId:{}", macIid, getOperationalState()
                        .getTransactionId());
            }
            getDeviceInfo().clearConfigData(RemoteUcastMacs.class, macIid);
        }
    }

    @Override
    protected Map<RemoteUcastMacsKey, RemoteUcastMacs> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getRemoteUcastMacs();
    }

    @Override
    protected boolean areEqual(final RemoteUcastMacs remoteUcastMacs1, final RemoteUcastMacs remoteUcastMacs2) {
        return Objects.equals(remoteUcastMacs1.key(), remoteUcastMacs2.key());
    }

    @Override
    public void onSuccess(final TransactionBuilder tx) {
        for (MdsalUpdate mdsalUpdate : updates) {
            RemoteUcastMacs mac = (RemoteUcastMacs) mdsalUpdate.getNewData();
            DataObjectIdentifier<RemoteUcastMacs> macIid = mdsalUpdate.getKey();
            getDeviceInfo().removeRemoteUcast(
                    ((DataObjectIdentifier<LogicalSwitches>) mac.getLogicalSwitchRef().getValue()), macIid);
        }
        getDeviceInfo().onOperDataAvailable();
    }

    @Override
    protected boolean isDeleteCmd() {
        return true;
    }

    @Override
    protected String getKeyStr(final DataObjectIdentifier<RemoteUcastMacs> iid) {
        return getLsKeyStr(iid.getFirstKeyOf(RemoteUcastMacs.class).getLogicalSwitchRef().getValue());
    }
}
