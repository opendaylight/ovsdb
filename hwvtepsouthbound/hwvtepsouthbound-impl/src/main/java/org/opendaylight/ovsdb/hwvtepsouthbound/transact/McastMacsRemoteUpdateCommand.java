/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

public class McastMacsRemoteUpdateCommand extends AbstractTransactCommand<RemoteMcastMacs, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(McastMacsRemoteUpdateCommand.class);
    private static final McastMacUnMetDependencyGetter MCAST_MAC_DATA_VALIDATOR = new McastMacUnMetDependencyGetter();

    public McastMacsRemoteUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> updateds =
                extractUpdated(getChanges(),RemoteMcastMacs.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<RemoteMcastMacs>> updated:
                updateds.entrySet()) {
                updateMcastMacRemote(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    private void updateMcastMacRemote(TransactionBuilder transaction,
            InstanceIdentifier<Node> instanceIdentifier, List<RemoteMcastMacs> macList) {
        for (RemoteMcastMacs mac: macList) {
            //add / update only if locator set got changed
            if (!HwvtepSouthboundUtil.isEmpty(mac.getLocatorSet())) {
                onConfigUpdate(transaction, instanceIdentifier, mac, null);
            }
        }
    }

    @Override
    public void onConfigUpdate(TransactionBuilder transaction,
                                  InstanceIdentifier<Node> nodeIid,
                                  RemoteMcastMacs remoteMcastMac,
                                  InstanceIdentifier macKey,
                                  Object... extraData) {
        InstanceIdentifier<RemoteMcastMacs> macIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class).
                child(RemoteMcastMacs.class, remoteMcastMac.getKey());
        processDependencies(MCAST_MAC_DATA_VALIDATOR, transaction, nodeIid, macIid, remoteMcastMac);
    }

    @Override
    public void doDeviceTransaction(TransactionBuilder transaction,
                                       InstanceIdentifier<Node> instanceIdentifier,
                                       RemoteMcastMacs mac,
                                       InstanceIdentifier macKey,
                                       Object... extraData) {
            LOG.debug("Creating remoteMcastMacs, mac address: {}", mac.getMacEntryKey().getValue());
            Optional<RemoteMcastMacs> operationalMacOptional =
                    getOperationalState().getRemoteMcastMacs(instanceIdentifier, mac.getKey());
            McastMacsRemote mcastMacsRemote = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), McastMacsRemote.class);
            setIpAddress(mcastMacsRemote, mac);
            setLocatorSet(transaction, mcastMacsRemote, mac);
            setLogicalSwitch(mcastMacsRemote, mac);
            if (!operationalMacOptional.isPresent()) {
                setMac(mcastMacsRemote, mac, operationalMacOptional);
                LOG.trace("execute: create RemoteMcastMac entry: {}", mcastMacsRemote);
                transaction.add(op.insert(mcastMacsRemote));
                transaction.add(op.comment("McastMacRemote: Creating " + mac.getMacEntryKey().getValue()));
            } else if (operationalMacOptional.get().getMacEntryUuid() != null) {
                UUID macEntryUUID = new UUID(operationalMacOptional.get().getMacEntryUuid().getValue());
                McastMacsRemote extraMac = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                                McastMacsRemote.class, null);
                extraMac.getUuidColumn().setData(macEntryUUID);
                LOG.trace("execute: update RemoteMcastMac entry: {}", mcastMacsRemote);
                transaction.add(op.update(mcastMacsRemote)
                        .where(extraMac.getUuidColumn().getSchema().opEqual(macEntryUUID))
                        .build());
                transaction.add(op.comment("McastMacRemote: Updating " + macEntryUUID));
            } else {
                LOG.warn("Unable to update remoteMcastMacs {} because uuid not found in the operational store",
                                mac.getMacEntryKey().getValue());
            }
    }

    private void setLogicalSwitch(McastMacsRemote mcastMacsRemote, RemoteMcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<LogicalSwitches> lswitchIid = (InstanceIdentifier<LogicalSwitches>) inputMac.getLogicalSwitchRef().getValue();
            Optional<LogicalSwitches> operationalSwitchOptional =
                    getOperationalState().getLogicalSwitches(lswitchIid);
            if (operationalSwitchOptional.isPresent()) {
                Uuid logicalSwitchUuid = operationalSwitchOptional.get().getLogicalSwitchUuid();
                UUID logicalSwitchUUID = new UUID(logicalSwitchUuid.getValue());
                mcastMacsRemote.setLogicalSwitch(logicalSwitchUUID);
            } else {
                mcastMacsRemote.setLogicalSwitch(TransactUtils.getLogicalSwitchUUID(lswitchIid));
            }
        }
    }

    private void setLocatorSet(TransactionBuilder transaction, McastMacsRemote mcastMacsRemote, RemoteMcastMacs inputMac) {
        if (inputMac.getLocatorSet() != null && !inputMac.getLocatorSet().isEmpty()) {
            UUID locatorSetUuid = TransactUtils.createPhysicalLocatorSet(getOperationalState(), transaction, inputMac.getLocatorSet());
            mcastMacsRemote.setLocatorSet(locatorSetUuid);
        }
    }

    private void setIpAddress(McastMacsRemote mcastMacsRemote, RemoteMcastMacs inputMac) {
        if (inputMac.getIpaddr() != null) {
            mcastMacsRemote.setIpAddress(inputMac.getIpaddr().getIpv4Address().getValue());
        }
    }

    private void setMac(McastMacsRemote mcastMacsRemote, RemoteMcastMacs inputMac,
            Optional<RemoteMcastMacs> inputSwitchOptional) {
        if (inputMac.getMacEntryKey() != null) {
            if (inputMac.getMacEntryKey().equals(HwvtepSouthboundConstants.UNKNOWN_DST_MAC)) {
                mcastMacsRemote.setMac(HwvtepSouthboundConstants.UNKNOWN_DST_STRING);
            } else {
                mcastMacsRemote.setMac(inputMac.getMacEntryKey().getValue());
            }
        } else if (inputSwitchOptional.isPresent() && inputSwitchOptional.get().getMacEntryKey() != null) {
            mcastMacsRemote.setMac(inputSwitchOptional.get().getMacEntryKey().getValue());
        }
    }

    @Override
    protected List<RemoteMcastMacs> getData(HwvtepGlobalAugmentation augmentation) {
        return augmentation.getRemoteMcastMacs();
    }

    @Override
    protected boolean areEqual(RemoteMcastMacs a, RemoteMcastMacs b) {
        return a.getKey().equals(b.getKey()) && Objects.equals(a.getLocatorSet(), b.getLocatorSet());
    }

    static class McastMacUnMetDependencyGetter extends UnMetDependencyGetter<RemoteMcastMacs> {

        public List<InstanceIdentifier<?>> getLogicalSwitchDependencies(RemoteMcastMacs data) {
            if (data == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(data.getLogicalSwitchRef().getValue());
        }

        public List<InstanceIdentifier<?>> getTerminationPointDependencies(RemoteMcastMacs data) {
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
}
