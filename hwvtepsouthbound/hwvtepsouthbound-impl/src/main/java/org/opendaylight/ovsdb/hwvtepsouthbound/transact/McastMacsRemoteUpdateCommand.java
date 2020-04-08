/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McastMacsRemoteUpdateCommand extends AbstractTransactCommand<RemoteMcastMacs, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(McastMacsRemoteUpdateCommand.class);
    private static final McastMacUnMetDependencyGetter MCAST_MAC_DATA_VALIDATOR = new McastMacUnMetDependencyGetter();

    public McastMacsRemoteUpdateCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> updateds =
                extractUpdated(getChanges(),RemoteMcastMacs.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<RemoteMcastMacs>> updated:
                updateds.entrySet()) {
                updateMcastMacRemote(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    private void updateMcastMacRemote(final TransactionBuilder transaction,
            final InstanceIdentifier<Node> instanceIdentifier, final List<RemoteMcastMacs> macList) {
        for (RemoteMcastMacs mac: macList) {
            //add / update only if locator set got changed
            if (!HwvtepSouthboundUtil.isEmpty(mac.getLocatorSet())) {
                onConfigUpdate(transaction, instanceIdentifier, mac, null);
            }
        }
    }

    @Override
    public void onConfigUpdate(final TransactionBuilder transaction,
                               final InstanceIdentifier<Node> nodeIid,
                               final RemoteMcastMacs remoteMcastMac,
                               final InstanceIdentifier macKey,
                               final Object... extraData) {
        InstanceIdentifier<RemoteMcastMacs> macIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                .child(RemoteMcastMacs.class, remoteMcastMac.key());
        processDependencies(MCAST_MAC_DATA_VALIDATOR, transaction, nodeIid, macIid, remoteMcastMac);
    }

    @Override
    public void doDeviceTransaction(final TransactionBuilder transaction,
                                       final InstanceIdentifier<Node> instanceIdentifier,
                                       final RemoteMcastMacs mac,
                                       final InstanceIdentifier macKey,
                                       final Object... extraData) {
        LOG.debug("Creating remoteMcastMacs, mac address: {}", mac.getMacEntryKey().getValue());
        final HwvtepDeviceInfo.DeviceData operationalMacOptional =
                getDeviceInfo().getDeviceOperData(RemoteMcastMacs.class, macKey);
        McastMacsRemote mcastMacsRemote = transaction.getTypedRowWrapper(McastMacsRemote.class);
        setIpAddress(mcastMacsRemote, mac);
        setLocatorSet(transaction, mcastMacsRemote, mac);
        setLogicalSwitch(transaction, mcastMacsRemote, mac);
        if (operationalMacOptional == null) {
            setMac(mcastMacsRemote, mac);
            LOG.trace("execute: create RemoteMcastMac entry: {}", mcastMacsRemote);
            transaction.add(op.insert(mcastMacsRemote));
            transaction.add(op.comment("McastMacRemote: Creating " + mac.getMacEntryKey().getValue()));
            updateCurrentTxData(RemoteMcastMacs.class, macKey, TXUUID, mac);
            updateControllerTxHistory(TransactionType.ADD, mcastMacsRemote);
        } else if (operationalMacOptional.getUuid() != null) {
            UUID macEntryUUID = operationalMacOptional.getUuid();
            McastMacsRemote extraMac = transaction.getTypedRowSchema(McastMacsRemote.class);
            extraMac.getUuidColumn().setData(macEntryUUID);
            LOG.trace("execute: update RemoteMcastMac entry: {}", mcastMacsRemote);
            transaction.add(op.update(mcastMacsRemote)
                    .where(extraMac.getUuidColumn().getSchema().opEqual(macEntryUUID))
                    .build());
            transaction.add(op.comment("McastMacRemote: Updating " + macEntryUUID));
            updateControllerTxHistory(TransactionType.UPDATE, mcastMacsRemote);
            //add to updates so that tep ref counts can be updated upon success
            addToUpdates(macKey, mac);
        } else {
            LOG.warn("Unable to update remoteMcastMacs {} because uuid not found in the operational store",
                    mac.getMacEntryKey().getValue());
        }
    }

    private void setLogicalSwitch(final TransactionBuilder transaction, final McastMacsRemote mcastMacsRemote,
            final RemoteMcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<LogicalSwitches> lswitchIid =
                    (InstanceIdentifier<LogicalSwitches>) inputMac.getLogicalSwitchRef().getValue();
            mcastMacsRemote.setLogicalSwitch(TransactUtils.getLogicalSwitchUUID(transaction,
                    getOperationalState(), lswitchIid));
        }
    }

    private void setLocatorSet(final TransactionBuilder transaction, final McastMacsRemote mcastMacsRemote,
            final RemoteMcastMacs inputMac) {
        if (inputMac.getLocatorSet() != null && !inputMac.getLocatorSet().isEmpty()) {
            UUID locatorSetUuid = TransactUtils.createPhysicalLocatorSet(getOperationalState(),
                    transaction, inputMac.getLocatorSet());
            mcastMacsRemote.setLocatorSet(locatorSetUuid);
        }
    }

    private void setIpAddress(final McastMacsRemote mcastMacsRemote, final RemoteMcastMacs inputMac) {
        if (inputMac.getIpaddr() != null) {
            mcastMacsRemote.setIpAddress(inputMac.getIpaddr().getIpv4Address().getValue());
        }
    }

    private void setMac(final McastMacsRemote mcastMacsRemote, final RemoteMcastMacs inputMac) {
        if (inputMac.getMacEntryKey() != null) {
            if (inputMac.getMacEntryKey().equals(HwvtepSouthboundConstants.UNKNOWN_DST_MAC)) {
                mcastMacsRemote.setMac(HwvtepSouthboundConstants.UNKNOWN_DST_STRING);
            } else {
                mcastMacsRemote.setMac(inputMac.getMacEntryKey().getValue());
            }
        }
    }

    @Override
    protected List<RemoteMcastMacs> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getRemoteMcastMacs();
    }

    @Override
    protected boolean areEqual(final RemoteMcastMacs macs1, final RemoteMcastMacs macs2) {
        return macs1.key().equals(macs2.key()) && Objects.equals(macs1.getLocatorSet(), macs2.getLocatorSet());
    }

    static class McastMacUnMetDependencyGetter extends UnMetDependencyGetter<RemoteMcastMacs> {

        @Override
        public List<InstanceIdentifier<?>> getLogicalSwitchDependencies(final RemoteMcastMacs data) {
            if (data == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(data.getLogicalSwitchRef().getValue());
        }

        @Override
        public List<InstanceIdentifier<?>> getTerminationPointDependencies(final RemoteMcastMacs data) {
            if (data == null || HwvtepSouthboundUtil.isEmpty(data.getLocatorSet())) {
                return Collections.emptyList();
            }
            List<InstanceIdentifier<?>> locators = new ArrayList<>();
            for (LocatorSet locator: data.getLocatorSet()) {
                locators.add(locator.getLocatorRef().getValue());
            }
            return locators;
        }
    }

    @SuppressFBWarnings("UC_USELESS_OBJECT")
    private void updateLocatorRefCounts(final MdsalUpdate mdsalUpdate) {
        //decrement the refcounts from old mcast mac
        //increment the refcounts for new mcast mac
        RemoteMcastMacs newMac = (RemoteMcastMacs) mdsalUpdate.getNewData();
        RemoteMcastMacs oldMac = (RemoteMcastMacs) mdsalUpdate.getOldData();
        InstanceIdentifier<RemoteMcastMacs> macIid = mdsalUpdate.getKey();

        if (oldMac != null && !oldMac.equals(newMac)) {
            if (oldMac.getLocatorSet() != null) {
                List<LocatorSet> removedLocators = new ArrayList<>(oldMac.getLocatorSet());
                if (newMac.getLocatorSet() != null) {
                    removedLocators.removeAll(newMac.getLocatorSet());
                }
                removedLocators.forEach(iid -> getDeviceInfo().decRefCount(macIid, iid.getLocatorRef().getValue()));
            }
        }
    }

    @Override
    protected void onCommandSucceeded() {
        for (MdsalUpdate mdsalUpdate : updates) {
            updateLocatorRefCounts(mdsalUpdate);
            RemoteMcastMacs mac = (RemoteMcastMacs) mdsalUpdate.getNewData();
            InstanceIdentifier<RemoteMcastMacs> macIid = mdsalUpdate.getKey();
            getDeviceInfo().updateRemoteMcast(
                    (InstanceIdentifier<LogicalSwitches>) mac.getLogicalSwitchRef().getValue(), macIid, mac);
        }
    }
}
