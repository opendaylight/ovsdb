/*
 * Copyright (c) 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UcastMacsRemoteUpdateCommand
        extends AbstractTransactCommand<RemoteUcastMacs, RemoteUcastMacsKey, HwvtepGlobalAugmentation> {
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
            InstanceIdentifier<RemoteUcastMacs> macIid =
                    instanceIdentifier.augmentation(HwvtepGlobalAugmentation.class)
                            .child(RemoteUcastMacs.class, remoteUcastMac.key());
            getDeviceInfo().updateConfigData(RemoteUcastMacs.class, macIid, remoteUcastMac);
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
        LOG.debug("DoDeviceTransaction remoteUcastMacs, mac address: {}", remoteUcastMac.getMacEntryKey().getValue());
        updateConfigData(RemoteUcastMacs.class, macKey, remoteUcastMac);
        HwvtepDeviceInfo.DeviceData deviceData = getDeviceOpData(RemoteUcastMacs.class, macKey);
        UcastMacsRemote ucastMacsRemote = transaction.getTypedRowWrapper(UcastMacsRemote.class);
        setIpAddress(ucastMacsRemote, remoteUcastMac);
        setLogicalSwitch(transaction, ucastMacsRemote, remoteUcastMac);
        //TODO handle multiple inserts
        if (deviceData  == null) {
            setLocator(transaction, ucastMacsRemote, remoteUcastMac);
            setMac(ucastMacsRemote, remoteUcastMac);
            LOG.trace("DoDeviceTransaction: creating RemotUcastMac entry: {} txId: {}", macKey,
                    getOperationalState().getTransactionId());
            transaction.add(ops().insert(ucastMacsRemote));
            updateCurrentTxData(RemoteUcastMacs.class, macKey, new UUID("uuid"), remoteUcastMac);
            LOG.info("CONTROLLER - {} {}", TransactionType.ADD, ucastMacsRemote);
            return;
        } else if (deviceData.getUuid() != null) {
            UUID newLocator = setLocator(transaction, ucastMacsRemote, remoteUcastMac);
            if (deviceData.getData() != null) {
                UcastMacsRemote existing = (UcastMacsRemote) deviceData.getData();
                if (existing.getLocatorColumn() != null) {
                    UUID oldLocatorUuid = existing.getLocatorColumn().getData();
                    if (Objects.equals(newLocator, oldLocatorUuid)) {
                        return;
                    }
                }
            }
            setMac(ucastMacsRemote, remoteUcastMac);
            UUID macEntryUUID = deviceData.getUuid();
            UcastMacsRemote extraMac = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    UcastMacsRemote.class, null);
            extraMac.getUuidColumn().setData(macEntryUUID);
            LOG.trace("doDeviceTransaction: updating RemotUcastMac entry: {} txId: {}", macKey,
                    getOperationalState().getTransactionId());
            transaction.add(ops().update(ucastMacsRemote)
                    .where(extraMac.getUuidColumn().getSchema().opEqual(macEntryUUID))
                    .build());
            LOG.info("CONTROLLER - {} {}", TransactionType.UPDATE, ucastMacsRemote);
            addToUpdates(macKey, remoteUcastMac);
        } else {
            LOG.warn("Unable to update remoteUcastMacs {} because uuid not found in the operational store txId: {}",
                getNodeKeyStr(macKey)   , getOperationalState().getTransactionId());
        }
        return;
    }

    private void setLogicalSwitch(final TransactionBuilder transaction, final UcastMacsRemote ucastMacsRemote,
            final RemoteUcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<LogicalSwitches> lswitchIid =
                    ((DataObjectIdentifier<LogicalSwitches>) inputMac.getLogicalSwitchRef().getValue()).toLegacy();
            UUID logicalSwitchUUID = TransactUtils.getLogicalSwitchUUID(transaction, getOperationalState(), lswitchIid);
            if (logicalSwitchUUID != null) {
                ucastMacsRemote.setLogicalSwitch(TransactUtils.getLogicalSwitchUUID(transaction, getOperationalState(),
                        lswitchIid));
            }
        }
    }

    private UUID setLocator(final TransactionBuilder transaction, final UcastMacsRemote ucastMacsRemote,
            final RemoteUcastMacs inputMac) {
        //get UUID by locatorRef
        if (inputMac.getLocatorRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<TerminationPoint> iid =
                    ((DataObjectIdentifier<TerminationPoint>) inputMac.getLocatorRef().getValue()).toLegacy();
            UUID locatorUuid = TransactUtils.createPhysicalLocator(transaction, getOperationalState(), iid);
            ucastMacsRemote.setLocator(locatorUuid);
            return locatorUuid;
        }
        return null;
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
    protected Map<RemoteUcastMacsKey, RemoteUcastMacs> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getRemoteUcastMacs();
    }

    static class UcastMacUnMetDependencyGetter extends UnMetDependencyGetter<RemoteUcastMacs> {

        @Override
        public List<InstanceIdentifier<?>> getLogicalSwitchDependencies(final RemoteUcastMacs data) {
            if (data == null) {
                return Collections.emptyList();
            }
            return Lists.newArrayList(((DataObjectIdentifier<?>) data.getLogicalSwitchRef().getValue()).toLegacy());
        }

        @Override
        public List<InstanceIdentifier<?>> getTerminationPointDependencies(final RemoteUcastMacs data) {
            if (data == null) {
                return Collections.emptyList();
            }
            return Lists.newArrayList(((DataObjectIdentifier<?>) data.getLocatorRef().getValue()).toLegacy());
        }
    }

    @Override
    protected boolean areEqual(final RemoteUcastMacs remoteUcastMacs1, final RemoteUcastMacs remoteUcastMacs2) {
        return Objects.equals(remoteUcastMacs1.key(), remoteUcastMacs2.key())
                && Objects.equals(remoteUcastMacs1.getLocatorRef(), remoteUcastMacs2.getLocatorRef());
    }

    @Override
    public void onSuccess(final TransactionBuilder tx) {
        for (MdsalUpdate mdsalUpdate : updates) {
            RemoteUcastMacs mac = (RemoteUcastMacs) mdsalUpdate.getNewData();
            InstanceIdentifier<RemoteUcastMacs> macIid = mdsalUpdate.getKey();
            getDeviceInfo().updateRemoteUcast(
                ((DataObjectIdentifier<LogicalSwitches>) mac.getLogicalSwitchRef().getValue()).toLegacy(), macIid, mac);
        }
    }

    @Override
    protected String getKeyStr(final InstanceIdentifier<RemoteUcastMacs> iid) {
        return getLsKeyStr(iid.firstKeyOf(RemoteUcastMacs.class).getLogicalSwitchRef().getValue());
    }
}
