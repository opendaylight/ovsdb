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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UcastMacsRemoteUpdateCommand extends AbstractTransactCommand<RemoteUcastMacs, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(UcastMacsRemoteUpdateCommand.class);
    private static final UcastMacUnMetDependencyGetter UCAST_MAC_DATA_VALIDATOR = new UcastMacUnMetDependencyGetter();

    public UcastMacsRemoteUpdateCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<RemoteUcastMacs>> updateds =
                extractUpdated(getChanges(),RemoteUcastMacs.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<RemoteUcastMacs>> updated:
                updateds.entrySet()) {
                updateUcastMacsRemote(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    private void updateUcastMacsRemote(final TransactionBuilder transaction,
                                       final InstanceIdentifier<Node> instanceIdentifier,
                                       final List<RemoteUcastMacs> remoteUcastMacs) {
        if (remoteUcastMacs == null) {
            return;
        }
        for (RemoteUcastMacs remoteUcastMac : remoteUcastMacs) {
            onConfigUpdate(transaction, instanceIdentifier, remoteUcastMac, null);
        }
    }

    @Override
    public void onConfigUpdate(final TransactionBuilder transaction,
                               final InstanceIdentifier<Node> nodeIid,
                               final RemoteUcastMacs remoteUcastMacs,
                               final InstanceIdentifier macKey,
                               final Object... extraData) {
        InstanceIdentifier<RemoteUcastMacs> macIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                .child(RemoteUcastMacs.class, remoteUcastMacs.key());
        processDependencies(UCAST_MAC_DATA_VALIDATOR, transaction, nodeIid, macIid, remoteUcastMacs);
    }

    @Override
    public void doDeviceTransaction(final TransactionBuilder transaction,
                                    final InstanceIdentifier<Node> instanceIdentifier,
                                    final RemoteUcastMacs remoteUcastMac,
                                    final InstanceIdentifier macKey,
                                    final Object... extraData) {
        LOG.debug("Creating remoteUcastMacs, mac address: {}", remoteUcastMac.getMacEntryKey().getValue());
        final HwvtepDeviceInfo.DeviceData deviceData =
                getOperationalState().getDeviceInfo().getDeviceOperData(RemoteUcastMacs.class, macKey);

        UcastMacsRemote ucastMacsRemote = transaction.getTypedRowWrapper(UcastMacsRemote.class);
        setIpAddress(ucastMacsRemote, remoteUcastMac);
        setLocator(transaction, ucastMacsRemote, remoteUcastMac);
        setLogicalSwitch(transaction, ucastMacsRemote, remoteUcastMac);
        if (deviceData == null) {
            setMac(ucastMacsRemote, remoteUcastMac);
            LOG.trace("doDeviceTransaction: creating RemotUcastMac entry: {}", ucastMacsRemote);
            transaction.add(op.insert(ucastMacsRemote));
            getOperationalState().getDeviceInfo().markKeyAsInTransit(RemoteUcastMacs.class, macKey);
            updateCurrentTxData(RemoteUcastMacs.class, macKey, new UUID("uuid"), remoteUcastMac);
        } else if (deviceData.getUuid() != null) {
            UUID macEntryUUID = deviceData.getUuid();
            UcastMacsRemote extraMac = transaction.getTypedRowSchema(UcastMacsRemote.class);
            extraMac.getUuidColumn().setData(macEntryUUID);
            LOG.trace("doDeviceTransaction: updating RemotUcastMac entry: {}", ucastMacsRemote);
            transaction.add(op.update(ucastMacsRemote)
                    .where(extraMac.getUuidColumn().getSchema().opEqual(macEntryUUID))
                    .build());
        } else {
            LOG.warn("Unable to update remoteMcastMacs {} because uuid not found in the operational store",
                    remoteUcastMac.getMacEntryKey().getValue());
        }
    }

    private void setLogicalSwitch(final TransactionBuilder transaction, final UcastMacsRemote ucastMacsRemote,
            final RemoteUcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<LogicalSwitches> lswitchIid =
                    (InstanceIdentifier<LogicalSwitches>) inputMac.getLogicalSwitchRef().getValue();
            ucastMacsRemote.setLogicalSwitch(TransactUtils.getLogicalSwitchUUID(
                    transaction, getOperationalState(), lswitchIid));
        }
    }

    private void setLocator(final TransactionBuilder transaction, final UcastMacsRemote ucastMacsRemote,
            final RemoteUcastMacs inputMac) {
        //get UUID by locatorRef
        if (inputMac.getLocatorRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<TerminationPoint> iid = (InstanceIdentifier<TerminationPoint>)
                    inputMac.getLocatorRef().getValue();
            UUID locatorUuid = TransactUtils.createPhysicalLocator(transaction, getOperationalState(), iid);
            if (locatorUuid != null) {
                ucastMacsRemote.setLocator(locatorUuid);
            }
        }
    }

    private static void setIpAddress(final UcastMacsRemote ucastMacsRemote, final RemoteUcastMacs inputMac) {
        if (inputMac.getIpaddr() != null) {
            ucastMacsRemote.setIpAddress(inputMac.getIpaddr().getIpv4Address().getValue());
        }
    }

    private static void setMac(final UcastMacsRemote ucastMacsRemote, final RemoteUcastMacs inputMac) {
        if (inputMac.getMacEntryKey() != null) {
            ucastMacsRemote.setMac(inputMac.getMacEntryKey().getValue());
        }
    }

    @Override
    protected List<RemoteUcastMacs> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getRemoteUcastMacs();
    }

    static class UcastMacUnMetDependencyGetter extends UnMetDependencyGetter<RemoteUcastMacs> {

        @Override
        public List<InstanceIdentifier<?>> getLogicalSwitchDependencies(final RemoteUcastMacs data) {
            if (data == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(data.getLogicalSwitchRef().getValue());
        }

        @Override
        public List<InstanceIdentifier<?>> getTerminationPointDependencies(final RemoteUcastMacs data) {
            if (data == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(data.getLocatorRef().getValue());
        }
    }

    @Override
    public void onCommandSucceeded() {
        for (MdsalUpdate mdsalUpdate : updates.get(getDeviceTransaction())) {
            RemoteUcastMacs newMac = (RemoteUcastMacs) mdsalUpdate.getNewData();
            InstanceIdentifier<RemoteUcastMacs> macIid = mdsalUpdate.getKey();
            RemoteUcastMacs oldMac = (RemoteUcastMacs) mdsalUpdate.getOldData();
            if (oldMac != null && !oldMac.equals(newMac)) {
                getDeviceInfo().decRefCount(macIid, oldMac.getLocatorRef().getValue());
            }
            getDeviceInfo().updateRemoteUcast(
                    (InstanceIdentifier<LogicalSwitches>) newMac.getLogicalSwitchRef().getValue(), macIid, newMac);
        }
    }
}
