/*
 * Copyright (c) 2015, 2016 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class UcastMacsRemoteRemoveCommand extends AbstractTransactCommand {
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

    private void removeUcastMacRemote(TransactionBuilder transaction,
            InstanceIdentifier<Node> instanceIdentifier, List<RemoteUcastMacs> macList) {
        for (RemoteUcastMacs mac: macList) {
            LOG.debug("Removing remoteUcastMacs, mac address: {}", mac.getMacEntryKey().getValue());
            Optional<RemoteUcastMacs> operationalMacOptional =
                    getOperationalState().getRemoteUcastMacs(instanceIdentifier, mac.getKey());
            UcastMacsRemote ucastMacsRemote = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    UcastMacsRemote.class, null);
            if (operationalMacOptional.isPresent() && operationalMacOptional.get().getMacEntryUuid() != null) {
                //when mac entry is deleted, its referenced locators are deleted automatically.
                //locators in config DS is not deleted and need to be removed explicitly by user.
                UUID macEntryUUID = new UUID(operationalMacOptional.get().getMacEntryUuid().getValue());
                ucastMacsRemote.getUuidColumn().setData(macEntryUUID);
                transaction.add(op.delete(ucastMacsRemote.getSchema()).
                        where(ucastMacsRemote.getUuidColumn().getSchema().opEqual(macEntryUUID)).build());
                transaction.add(op.comment("UcastMacRemote: Deleting " + mac.getMacEntryKey().getValue()));
            } else {
                LOG.warn("Unable to delete remoteUcastMacs {} because it was not found in the operational store",
                        mac.getMacEntryKey().getValue());
            }
            InstanceIdentifier<RemoteUcastMacs> macIid = instanceIdentifier.augmentation(HwvtepGlobalAugmentation.class).
                    child(RemoteUcastMacs.class, mac.getKey());
            updateCurrentTxDeleteData(macIid, mac);
        }
    }

    private Map<InstanceIdentifier<Node>, List<RemoteUcastMacs>> extractRemoved(
            Collection<DataTreeModification<Node>> changes, Class<RemoteUcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<RemoteUcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<RemoteUcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                //If the node which remoteUcastMacs belong to is removed, all remoteUcastMacs should be removed too.
                Node removed = TransactUtils.getRemoved(mod);
                if (removed != null) {
                    List<RemoteUcastMacs> macListRemoved = null;
                    if (removed.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                        macListRemoved = removed.getAugmentation(HwvtepGlobalAugmentation.class).getRemoteUcastMacs();
                    }
                    if (macListRemoved != null) {
                        result.put(key, macListRemoved);
                    }
                }
                //If the node which remoteUcastMacs belong to is updated, and remoteUcastMacs may
                //be created or updated or deleted, we need to get deleted ones.
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<RemoteUcastMacs> macListUpdated = null;
                    List<RemoteUcastMacs> macListBefore = null;
                    HwvtepGlobalAugmentation hgUpdated = updated.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgUpdated != null) {
                        macListUpdated = hgUpdated.getRemoteUcastMacs();
                    }
                    HwvtepGlobalAugmentation hgBefore = before.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgBefore != null) {
                        macListBefore = hgBefore.getRemoteUcastMacs();
                    }
                    if (macListBefore != null) {
                        List<RemoteUcastMacs> macListRemoved = new ArrayList<RemoteUcastMacs>();
                        if (macListUpdated != null) {
                            macListBefore.removeAll(macListUpdated);
                        }
                        //then exclude updated remoteUcastMacs
                        if (macListUpdated != null) {
                            for (RemoteUcastMacs macBefore : macListBefore) {
                                int i = 0;
                                for (; i < macListUpdated.size(); i++) {
                                    if (macBefore.getKey().equals(macListUpdated.get(i).getKey())) {
                                        break;
                                    }
                                }
                                if (i == macListUpdated.size()) {
                                    macListRemoved.add(macBefore);
                                }
                            }
                        } else {
                            macListRemoved.addAll(macListBefore);
                        }
                        if (!macListRemoved.isEmpty()) {
                            result.put(key, macListRemoved);
                        }
                    }
                }
            }
        }
        return result;
    }
}
