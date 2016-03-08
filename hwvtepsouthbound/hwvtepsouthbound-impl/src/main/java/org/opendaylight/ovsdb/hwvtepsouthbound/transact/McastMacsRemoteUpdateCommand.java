/*
 * Copyright (c) 2015, 2016 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class McastMacsRemoteUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(McastMacsRemoteUpdateCommand.class);

    public McastMacsRemoteUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> createds =
                extractCreated(getChanges(),RemoteMcastMacs.class);
        if (!createds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<RemoteMcastMacs>> created:
                createds.entrySet()) {
                updateMcastMacRemote(transaction,  created.getKey(), created.getValue());
            }
        }
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
            } else {
                RemoteMcastMacs updatedMac = operationalMacOptional.get();
                String existingMac = updatedMac.getMacEntryKey().getValue();
                McastMacsRemote extraMac = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), McastMacsRemote.class);
                extraMac.setMac("");
                LOG.trace("execute: update RemoteMcastMac entry: {}", mcastMacsRemote);
                transaction.add(op.update(mcastMacsRemote)
                        .where(extraMac.getMacColumn().getSchema().opEqual(existingMac))
                        .build());
                transaction.add(op.comment("McastMacRemote: Updating " + mac.getMacEntryKey().getValue()));
            }
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
                LOG.warn("Create or update remoteMcastMac: NO logical switch with iid {} found in operational datastore!",
                        lswitchIid);
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

    private Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> extractCreated(
            Collection<DataTreeModification<Node>> changes, Class<RemoteMcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<RemoteMcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    List<RemoteMcastMacs> macListUpdated = null;
                    HwvtepGlobalAugmentation hgAugmentation = created.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgAugmentation != null) {
                        macListUpdated = hgAugmentation.getRemoteMcastMacs();
                    }
                    if (macListUpdated != null) {
                        result.put(key, macListUpdated);
                    }
                }
            }
        }
        return result;
    }

    private Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> extractUpdated(
            Collection<DataTreeModification<Node>> changes, Class<RemoteMcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<RemoteMcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<RemoteMcastMacs> macListUpdated = null;
                    List<RemoteMcastMacs> macListBefore = null;
                    HwvtepGlobalAugmentation hgUpdated = updated.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgUpdated != null) {
                        macListUpdated = hgUpdated.getRemoteMcastMacs();
                    }
                    HwvtepGlobalAugmentation hgBefore = before.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgBefore != null) {
                        macListBefore = hgBefore.getRemoteMcastMacs();
                    }
                    if (macListUpdated != null) {
                        if (macListBefore != null) {
                            macListUpdated.removeAll(macListBefore);
                        }
                        result.put(key, macListUpdated);
                    }
                }
            }
        }
        return result;
    }
}
