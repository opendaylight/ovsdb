/*
 * Copyright (c) 2015, 2016 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsLocal;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McastMacsLocalUpdateCommand
        extends AbstractTransactCommand<LocalMcastMacs, LocalMcastMacsKey, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(McastMacsLocalUpdateCommand.class);

    public McastMacsLocalUpdateCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LocalMcastMacs>> updateds =
                extractUpdated(getChanges(),LocalMcastMacs.class);
        if (!updateds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LocalMcastMacs>> updated:
                updateds.entrySet()) {
                updateMcastMacsLocal(transaction,  updated.getKey(), updated.getValue());
            }
        }
    }

    private void updateMcastMacsLocal(final TransactionBuilder transaction,
            final InstanceIdentifier<Node> instanceIdentifier, final List<LocalMcastMacs> localMcastMacs) {
        for (LocalMcastMacs localMcastMac: localMcastMacs) {
            LOG.debug("Creating localMcastMac, mac address: {}", localMcastMac.getMacEntryKey().getValue());
            final Optional<LocalMcastMacs> operationalMacOptional =
                    getOperationalState().getLocalMcastMacs(instanceIdentifier, localMcastMac.key());
            McastMacsLocal mcastMacsLocal = transaction.getTypedRowWrapper(McastMacsLocal.class);
            setIpAddress(mcastMacsLocal, localMcastMac);
            setLocatorSet(transaction, mcastMacsLocal, localMcastMac);
            setLogicalSwitch(mcastMacsLocal, localMcastMac);
            if (!operationalMacOptional.isPresent()) {
                setMac(mcastMacsLocal, localMcastMac, operationalMacOptional);
                LOG.trace("execute: create LocalMcastMac entry: {}", mcastMacsLocal);
                transaction.add(op.insert(mcastMacsLocal));
                transaction.add(op.comment("McastMacLocal: Creating " + localMcastMac.getMacEntryKey().getValue()));
            } else if (operationalMacOptional.orElseThrow().getMacEntryUuid() != null) {
                UUID macEntryUUID = new UUID(operationalMacOptional.orElseThrow().getMacEntryUuid().getValue());
                McastMacsLocal extraMac = transaction.getTypedRowSchema(McastMacsLocal.class);
                extraMac.getUuidColumn().setData(macEntryUUID);
                LOG.trace("execute: update LocalMcastMac entry: {}", mcastMacsLocal);
                transaction.add(op.update(mcastMacsLocal)
                        .where(extraMac.getUuidColumn().getSchema().opEqual(macEntryUUID))
                        .build());
                transaction.add(op.comment("McastMacLocal: Updating " + macEntryUUID));
            } else {
                LOG.warn("Unable to update localMcastMacs {} because uuid not found in the operational store",
                                localMcastMac.getMacEntryKey().getValue());
            }
        }
    }

    private void setLogicalSwitch(final McastMacsLocal mcastMacsLocal, final LocalMcastMacs inputMac) {
        if (inputMac.getLogicalSwitchRef() != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<LogicalSwitches> lswitchIid =
                   (InstanceIdentifier<LogicalSwitches>) inputMac.getLogicalSwitchRef().getValue();
            Optional<LogicalSwitches> operationalSwitchOptional =
                    getOperationalState().getLogicalSwitches(lswitchIid);
            if (operationalSwitchOptional.isPresent()) {
                Uuid logicalSwitchUuid = operationalSwitchOptional.orElseThrow().getLogicalSwitchUuid();
                UUID logicalSwitchUUID = new UUID(logicalSwitchUuid.getValue());
                mcastMacsLocal.setLogicalSwitch(logicalSwitchUUID);
            } else {
                LOG.warn(
                    "Create or update localMcastMac: No logical switch with iid {} found in operational datastore!",
                    lswitchIid);
            }
        }
    }

    private void setLocatorSet(final TransactionBuilder transaction, final McastMacsLocal mcastMacsLocal,
            final LocalMcastMacs inputMac) {
        if (inputMac.getLocatorSet() != null && !inputMac.getLocatorSet().isEmpty()) {
            UUID locatorSetUuid = TransactUtils.createPhysicalLocatorSet(getOperationalState(), transaction,
                    inputMac.getLocatorSet());
            mcastMacsLocal.setLocatorSet(locatorSetUuid);
        }
    }

    private static void setIpAddress(final McastMacsLocal mcastMacsLocal, final LocalMcastMacs inputMac) {
        if (inputMac.getIpaddr() != null) {
            mcastMacsLocal.setIpAddress(inputMac.getIpaddr().getIpv4Address().getValue());
        }
    }

    private static void setMac(final McastMacsLocal mcastMacsLocal, final LocalMcastMacs inputMac,
            final Optional<LocalMcastMacs> inputSwitchOptional) {
        if (inputMac.getMacEntryKey() != null) {
            if (inputMac.getMacEntryKey().equals(HwvtepSouthboundConstants.UNKNOWN_DST_MAC)) {
                mcastMacsLocal.setMac(HwvtepSouthboundConstants.UNKNOWN_DST_STRING);
            } else {
                mcastMacsLocal.setMac(inputMac.getMacEntryKey().getValue());
            }
        } else if (inputSwitchOptional.isPresent() && inputSwitchOptional.orElseThrow().getMacEntryKey() != null) {
            mcastMacsLocal.setMac(inputSwitchOptional.orElseThrow().getMacEntryKey().getValue());
        }
    }

    @Override
    protected Map<LocalMcastMacsKey, LocalMcastMacs> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLocalMcastMacs();
    }

    @Override
    protected String getKeyStr(InstanceIdentifier<LocalMcastMacs> iid) {
        return getLsKeyStr(iid.firstKeyOf(LocalMcastMacs.class).getLogicalSwitchRef().getValue());
    }
}
