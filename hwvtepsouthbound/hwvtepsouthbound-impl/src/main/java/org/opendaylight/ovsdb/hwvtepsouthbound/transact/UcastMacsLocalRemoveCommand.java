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
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class UcastMacsLocalRemoveCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(UcastMacsLocalRemoveCommand.class);

    public UcastMacsLocalRemoveCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LocalUcastMacs>> removeds =
                extractRemoved(getChanges(),LocalUcastMacs.class);
        if (!removeds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LocalUcastMacs>> removed:
                removeds.entrySet()) {
                removeUcastMacLocal(transaction,  removed.getKey(), removed.getValue());
            }
        }
    }

    private void removeUcastMacLocal(TransactionBuilder transaction,
            InstanceIdentifier<Node> instanceIdentifier, List<LocalUcastMacs> macList) {
        for (LocalUcastMacs mac: macList) {
            LOG.debug("Removing remoteUcastMacs, mac address: {}", mac.getMacEntryKey().getValue());
            Optional<LocalUcastMacs> operationalMacOptional =
                    getOperationalState().getLocalUcastMacs(instanceIdentifier, mac.getKey());
            UcastMacsLocal ucastMacsLocal = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    UcastMacsLocal.class, null);
            if (operationalMacOptional.isPresent() && operationalMacOptional.get().getMacEntryUuid() != null) {
                //when mac entry is deleted, its referenced locators are deleted automatically.
                //locators in config DS is not deleted and user need to be removed explicitly by user.
                UUID macEntryUUID = new UUID(operationalMacOptional.get().getMacEntryUuid().getValue());
                transaction.add(op.delete(ucastMacsLocal.getSchema()).
                        where(ucastMacsLocal.getUuidColumn().getSchema().opEqual(macEntryUUID)).build());
                transaction.add(op.comment("UcastMacLocal: Deleting " + mac.getMacEntryKey().getValue()));
            } else {
                LOG.warn("Unable to delete remoteUcastMacs {} because it was not found in the operational store",
                        mac.getMacEntryKey().getValue());
            }
        }
    }

    private Map<InstanceIdentifier<Node>, List<LocalUcastMacs>> extractRemoved(
            Collection<DataTreeModification<Node>> changes, Class<LocalUcastMacs> class1) {
        Map<InstanceIdentifier<Node>, List<LocalUcastMacs>> result
            = new HashMap<InstanceIdentifier<Node>, List<LocalUcastMacs>>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                //If the node which remoteUcastMacs belong to is removed, all remoteUcastMacs should be removed too.
                Node removed = TransactUtils.getRemoved(mod);
                if (removed != null) {
                    List<LocalUcastMacs> macListRemoved = null;
                    if (removed.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                        macListRemoved = removed.getAugmentation(HwvtepGlobalAugmentation.class).getLocalUcastMacs();
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
                    List<LocalUcastMacs> macListUpdated = null;
                    List<LocalUcastMacs> macListBefore = null;
                    HwvtepGlobalAugmentation hgUpdated = updated.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgUpdated != null) {
                        macListUpdated = hgUpdated.getLocalUcastMacs();
                    }
                    HwvtepGlobalAugmentation hgBefore = before.getAugmentation(HwvtepGlobalAugmentation.class);
                    if (hgBefore != null) {
                        macListBefore = hgBefore.getLocalUcastMacs();
                    }
                    if (macListBefore != null) {
                        List<LocalUcastMacs> macListRemoved = new ArrayList<LocalUcastMacs>();
                        if (macListUpdated != null) {
                            macListBefore.removeAll(macListUpdated);
                        }
                        //then exclude updated remoteUcastMacs
                        for (LocalUcastMacs macBefore: macListBefore) {
                            int i = 0;
                            for(; i < macListUpdated.size(); i++) {
                                if (macBefore.getMacEntryKey().equals(macListUpdated.get(i).getMacEntryKey())) {
                                    break;
                                }
                            }
                            if (i == macListUpdated.size()) {
                                macListRemoved.add(macBefore);
                            }
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