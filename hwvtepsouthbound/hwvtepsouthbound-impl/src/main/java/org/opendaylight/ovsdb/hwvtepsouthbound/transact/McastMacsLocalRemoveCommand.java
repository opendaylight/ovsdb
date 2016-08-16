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
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsLocal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class McastMacsLocalRemoveCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(McastMacsLocalRemoveCommand.class);

    public McastMacsLocalRemoveCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LocalMcastMacs>> removeds =
                extractRemoved(getChanges(),LocalMcastMacs.class);
        if (!removeds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LocalMcastMacs>> removed:
                removeds.entrySet()) {
                removeMcastMacLocal(transaction,  removed.getKey(), removed.getValue());
            }
        }
    }

    private void removeMcastMacLocal(TransactionBuilder transaction,
            InstanceIdentifier<Node> instanceIdentifier, List<LocalMcastMacs> macList) {
        for (LocalMcastMacs mac: macList) {
            LOG.debug("Removing localMcastMacs, mac address: {}", mac.getMacEntryKey().getValue());
            Optional<LocalMcastMacs> operationalMacOptional =
                    getOperationalState().getLocalMcastMacs(instanceIdentifier, mac.getKey());
            McastMacsLocal mcastMacsLocal = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    McastMacsLocal.class, null);
            if (operationalMacOptional.isPresent() && operationalMacOptional.get().getMacEntryUuid() != null) {
                //when mac entry is deleted, its referenced locator set and locators are deleted automatically.
                //TODO: locator in config DS is not deleted
                UUID macEntryUUID = new UUID(operationalMacOptional.get().getMacEntryUuid().getValue());
                mcastMacsLocal.getUuidColumn().setData(macEntryUUID);
                transaction.add(op.delete(mcastMacsLocal.getSchema()).
                        where(mcastMacsLocal.getUuidColumn().getSchema().opEqual(macEntryUUID)).build());
                transaction.add(op.comment("McastMacLocal: Deleting " + mac.getMacEntryKey().getValue()));
            } else {
                LOG.warn("Unable to delete localMcastMacs {} because it was not found in the operational store",
                        mac.getMacEntryKey().getValue());
            }
        }
    }

    private Map<InstanceIdentifier<Node>, List<LocalMcastMacs>> extractRemoved(
            Collection<DataTreeModification<Node>> changes, Class<LocalMcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<LocalMcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<LocalMcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                //If the node which localMcastMacs belong to is removed, all localMcastMacs should be removed too.
                Node removed = TransactUtils.getRemoved(mod);
                if (removed != null) {
                    List<LocalMcastMacs> macListRemoved = null;
                    if (removed.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                        macListRemoved = removed.getAugmentation(HwvtepGlobalAugmentation.class).getLocalMcastMacs();
                    }
                    if (macListRemoved != null) {
                        result.put(key, macListRemoved);
                    }
                }
                //If the node which localMcastMacs belong to is updated, and localMcastMacs may
                //be created or updated or deleted, we need to get deleted ones.
                Node updated = TransactUtils.getUpdated(mod);
                Node before = mod.getDataBefore();
                if (updated != null && before != null) {
                    List<LocalMcastMacs> macListUpdated = null;
                    List<LocalMcastMacs> macListBefore = null;
                    HwvtepGlobalAugmentation hgUpdated = updated.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgUpdated != null) {
                        macListUpdated = hgUpdated.getLocalMcastMacs();
                    }
                    HwvtepGlobalAugmentation hgBefore = before.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgBefore != null) {
                        macListBefore = hgBefore.getLocalMcastMacs();
                    }
                    if (macListBefore != null) {
                        List<LocalMcastMacs> macListRemoved = new ArrayList<LocalMcastMacs>();
                        if (macListUpdated != null) {
                            macListBefore.removeAll(macListUpdated);
                        }
                        //then exclude updated localMcastMacs
                        if (macListUpdated != null) {
                            for (LocalMcastMacs macBefore : macListBefore) {
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
