/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McastMacsRemoteUpdateCommand
        extends AbstractTransactCommand<RemoteMcastMacs, RemoteMcastMacsKey, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(McastMacsRemoteUpdateCommand.class);

    @VisibleForTesting
    static final McastMacUnMetDependencyGetter MCAST_MAC_DATA_VALIDATOR = new McastMacUnMetDependencyGetter();

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
            InstanceIdentifier<RemoteMcastMacs> macIid = instanceIdentifier
                    .augmentation(HwvtepGlobalAugmentation.class)
                    .child(RemoteMcastMacs.class, mac.key());
            updateConfigData(RemoteMcastMacs.class, macIid, mac);
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

        String nodeId = instanceIdentifier.firstKeyOf(Node.class).getNodeId().getValue();
        LOG.debug("Creating remoteMcastMacs, mac address: {} {}", nodeId, mac.getMacEntryKey().getValue());

        McastMacsRemote mcastMacsRemote = transaction.getTypedRowWrapper(McastMacsRemote.class);
        setIpAddress(mcastMacsRemote, mac);
        setLogicalSwitch(transaction, mcastMacsRemote, mac);
        setMac(mcastMacsRemote, mac);
        InstanceIdentifier<RemoteMcastMacs> macIid = instanceIdentifier.augmentation(HwvtepGlobalAugmentation.class)
                .child(RemoteMcastMacs.class, mac.key());
        HwvtepDeviceInfo.DeviceData deviceData = super.fetchDeviceData(RemoteMcastMacs.class, macIid);
        if (deviceData == null) {
            setLocatorSet(transaction, mcastMacsRemote, mac);
            transaction.add(op.insert(mcastMacsRemote));
            updateCurrentTxData(RemoteMcastMacs.class, macIid, new UUID("uuid"), mac);
            updateControllerTxHistory(TransactionType.ADD, mcastMacsRemote);
            LOG.info("CONTROLLER - {} {}", TransactionType.ADD, mcastMacsRemote);
        } else if (deviceData.getUuid() != null) {
            setLocatorSet(transaction, mcastMacsRemote, mac);
            UUID macEntryUUID = deviceData.getUuid();
            McastMacsRemote extraMac = transaction.getTypedRowSchema(McastMacsRemote.class);
            extraMac.getUuidColumn().setData(macEntryUUID);
            transaction.add(op.update(mcastMacsRemote)
                    .where(extraMac.getUuidColumn().getSchema().opEqual(macEntryUUID))
                    .build());
            updateControllerTxHistory(TransactionType.UPDATE, mcastMacsRemote);
            LOG.info("CONTROLLER - {} {}", TransactionType.UPDATE, mcastMacsRemote);
            addToUpdates(macIid, mac);
        } else {
            LOG.error("Unable to update remoteMcastMacs {} {} because uuid not found in the operational"
                    + " store txId: {}", mac, deviceData, getOperationalState().getTransactionId());
        }
        updateConfigData(RemoteMcastMacs.class, macIid, mac);
    }

    private void setLogicalSwitch(final TransactionBuilder transaction, final McastMacsRemote mcastMacsRemote,
            final RemoteMcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<LogicalSwitches> lswitchIid =
                    (InstanceIdentifier<LogicalSwitches>) inputMac.getLogicalSwitchRef().getValue();
            UUID logicalSwitchUUID = TransactUtils.getLogicalSwitchUUID(transaction, getOperationalState(), lswitchIid);
            if (logicalSwitchUUID != null) {
                mcastMacsRemote.setLogicalSwitch(logicalSwitchUUID);
            }
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

    private static void setIpAddress(final McastMacsRemote mcastMacsRemote, final RemoteMcastMacs inputMac) {
        if (inputMac.getIpaddr() != null) {
            mcastMacsRemote.setIpAddress(inputMac.getIpaddr().getIpv4Address().getValue());
        }
    }

    private static void setMac(final McastMacsRemote mcastMacsRemote, final RemoteMcastMacs inputMac) {
        if (inputMac.getMacEntryKey() != null) {
            if (inputMac.getMacEntryKey().equals(HwvtepSouthboundConstants.UNKNOWN_DST_MAC)) {
                mcastMacsRemote.setMac(HwvtepSouthboundConstants.UNKNOWN_DST_STRING);
            } else {
                mcastMacsRemote.setMac(inputMac.getMacEntryKey().getValue());
            }
        }
    }

    @Override
    protected Map<RemoteMcastMacsKey, RemoteMcastMacs> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getRemoteMcastMacs();
    }

    @Override
    protected boolean areEqual(final RemoteMcastMacs macs1, final RemoteMcastMacs macs2) {
        if (getOperationalState().isInReconciliation()) {
            return Objects.equals(macs1.key(), macs2.key())
                    && compareLocatorSets(macs1.getLocatorSet(), macs2.getLocatorSet());
        }
        return Objects.equals(macs1.key(), macs2.key())
                && Objects.equals(macs1.getLocatorSet(), macs2.getLocatorSet());
    }

    private static boolean compareLocatorSets(List<LocatorSet> locatorSet1, List<LocatorSet> locatorSet2) {
        if (locatorSet1 == null) {
            locatorSet1 = Collections.emptyList();
        }
        if (locatorSet2 == null) {
            locatorSet2 = Collections.emptyList();
        }
        if (locatorSet1.size() != locatorSet2.size()) {
            return false;
        }
        Set<LocatorSet> set1 = Sets.newHashSet(locatorSet1);
        Set<LocatorSet> set2 = Sets.newHashSet(locatorSet2);
        return set1.containsAll(set2);
    }

    static class McastMacUnMetDependencyGetter extends UnMetDependencyGetter<RemoteMcastMacs> {

        @Override
        public List<InstanceIdentifier<?>> getLogicalSwitchDependencies(final RemoteMcastMacs data) {
            if (data == null) {
                return Collections.emptyList();
            }
            return Lists.newArrayList(data.getLogicalSwitchRef().getValue());
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
    public void onSuccess(final TransactionBuilder tx) {
        for (MdsalUpdate mdsalUpdate : updates) {
            updateLocatorRefCounts(mdsalUpdate);
            RemoteMcastMacs mac = (RemoteMcastMacs) mdsalUpdate.getNewData();
            InstanceIdentifier<RemoteMcastMacs> macIid = mdsalUpdate.getKey();
            getDeviceInfo().updateRemoteMcast(
                    (InstanceIdentifier<LogicalSwitches>) mac.getLogicalSwitchRef().getValue(), macIid, mac);
        }
    }

    @Override
    protected String getKeyStr(final InstanceIdentifier<RemoteMcastMacs> iid) {
        return getLsKeyStr(iid.firstKeyOf(RemoteMcastMacs.class).getLogicalSwitchRef().getValue());
    }
}
