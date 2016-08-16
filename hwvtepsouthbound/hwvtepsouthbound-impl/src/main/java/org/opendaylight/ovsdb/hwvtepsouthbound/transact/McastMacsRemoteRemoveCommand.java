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
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class McastMacsRemoteRemoveCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(McastMacsRemoteRemoveCommand.class);

    public McastMacsRemoteRemoveCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> removeds =
                extractRemoved(getChanges(),RemoteMcastMacs.class);
        if (!removeds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<RemoteMcastMacs>> removed:
                removeds.entrySet()) {
                removeMcastMacRemote(transaction,  removed.getKey(), removed.getValue());
            }
        }
    }

    private void removeMcastMacRemote(TransactionBuilder transaction,
            InstanceIdentifier<Node> instanceIdentifier, List<RemoteMcastMacs> macList) {
        for (RemoteMcastMacs mac: macList) {
            LOG.debug("Removing remoteMcastMacs, mac address: {}", mac.getMacEntryKey().getValue());
            Optional<RemoteMcastMacs> operationalMacOptional =
                    getOperationalState().getRemoteMcastMacs(instanceIdentifier, mac.getKey());
            McastMacsRemote mcastMacsRemote = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    McastMacsRemote.class, null);
            if (operationalMacOptional.isPresent() && operationalMacOptional.get().getMacEntryUuid() != null) {
                //when mac entry is deleted, its referenced locator set and locators are deleted automatically.
                //TODO: locator in config DS is not deleted
                UUID macEntryUUID = new UUID(operationalMacOptional.get().getMacEntryUuid().getValue());
                mcastMacsRemote.getUuidColumn().setData(macEntryUUID);
                transaction.add(op.delete(mcastMacsRemote.getSchema()).
                        where(mcastMacsRemote.getUuidColumn().getSchema().opEqual(macEntryUUID)).build());
                transaction.add(op.comment("McastMacRemote: Deleting " + mac.getMacEntryKey().getValue()));
            } else {
                LOG.warn("Unable to delete remoteMcastMacs {} because it was not found in the operational store",
                        mac.getMacEntryKey().getValue());
            }
        }
    }

    private Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> extractRemoved(
            Collection<DataTreeModification<Node>> changes, Class<RemoteMcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<RemoteMcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                //If the node which remoteMcastMacs belong to is removed, all remoteMcastMacs should be removed too.
                Node removed = TransactUtils.getRemoved(mod);
                if (removed != null) {
                    List<RemoteMcastMacs> macListRemoved = null;
                    if (removed.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                        macListRemoved = removed.getAugmentation(HwvtepGlobalAugmentation.class).getRemoteMcastMacs();
                    }
                    if (macListRemoved != null) {
                        result.put(key, macListRemoved);
                    }
                }
                //If the node which remoteMcastMacs belong to is updated, and remoteMcastMacs may
                //be created or updated or deleted, we need to get deleted ones.
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
                    if (macListBefore != null) {
                        List<RemoteMcastMacs> macListRemoved = new ArrayList<RemoteMcastMacs>();
                        if (macListUpdated != null) {
                            macListBefore.removeAll(macListUpdated);
                        }
                        //then exclude updated remoteMcastMacs
                        if (macListUpdated != null) {
                            for (RemoteMcastMacs macBefore : macListBefore) {
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
