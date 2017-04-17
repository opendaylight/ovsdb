/*
 * Copyright (c) 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
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

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UcastMacsRemoteRemoveCommand extends AbstractTransactCommand<RemoteUcastMacs, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(UcastMacsRemoteRemoveCommand.class);

    public UcastMacsRemoteRemoveCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<RemoteUcastMacs>> removeds =
                extractRemoved(getChanges(),RemoteUcastMacs.class);
        if (!removeds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<RemoteUcastMacs>> removed:
                removeds.entrySet()) {
                removeUcastMacRemote(transaction,  removed.getKey(), removed.getValue());
            }
        }
    }


    private void removeUcastMacRemote(final TransactionBuilder transaction,
                                      final InstanceIdentifier<Node> instanceIdentifier,
                                      final List<RemoteUcastMacs> macList) {
        for (RemoteUcastMacs mac: macList) {
            onConfigUpdate(transaction, instanceIdentifier, mac, null);
        }
    }

    @Override
    public void onConfigUpdate(final TransactionBuilder transaction,
                               final InstanceIdentifier<Node> nodeIid,
                               final RemoteUcastMacs remoteUcastMacs,
                               final InstanceIdentifier macKey,
                               final Object... extraData) {
        InstanceIdentifier<RemoteUcastMacs> macIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class).
                child(RemoteUcastMacs.class, remoteUcastMacs.getKey());
        processDependencies(null, transaction, nodeIid, macIid, remoteUcastMacs);
    }

    @Override
    public void doDeviceTransaction(final TransactionBuilder transaction,
                                    final InstanceIdentifier<Node> instanceIdentifier,
                                    final RemoteUcastMacs mac,
                                    final InstanceIdentifier macKey,
                                    final Object... extraData) {
            LOG.debug("Removing remoteUcastMacs, mac address: {}", mac.getMacEntryKey().getValue());
            InstanceIdentifier<RemoteUcastMacs> macIid = instanceIdentifier.augmentation(HwvtepGlobalAugmentation.class).
                    child(RemoteUcastMacs.class, mac.getKey());
            HwvtepDeviceInfo.DeviceData deviceData =
                    getOperationalState().getDeviceInfo().getDeviceOperData(RemoteUcastMacs.class, macIid);
            UcastMacsRemote ucastMacsRemote = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    UcastMacsRemote.class, null);
            if (deviceData != null && deviceData.getUuid() != null) {
                //when mac entry is deleted, its referenced locators are deleted automatically.
                //locators in config DS is not deleted and need to be removed explicitly by user.
                UUID macEntryUUID = deviceData.getUuid();
                ucastMacsRemote.getUuidColumn().setData(macEntryUUID);
                transaction.add(op.delete(ucastMacsRemote.getSchema()).
                        where(ucastMacsRemote.getUuidColumn().getSchema().opEqual(macEntryUUID)).build());
                transaction.add(op.comment("UcastMacRemote: Deleting " + mac.getMacEntryKey().getValue()));
                getOperationalState().getDeviceInfo().markKeyAsInTransit(RemoteUcastMacs.class, macKey);
            } else {
                LOG.warn("Unable to delete remoteUcastMacs {} because it was not found in the operational store",
                        mac.getMacEntryKey().getValue());
            }
            updateCurrentTxDeleteData(RemoteUcastMacs.class, macIid, mac);
    }

    protected List<RemoteUcastMacs> getData(HwvtepGlobalAugmentation augmentation) {
        return augmentation.getRemoteUcastMacs();
    }

    @Override
    protected boolean isRemoveCommand() {
        return true;
    }
}
