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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
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

public class McastMacsRemoteRemoveCommand extends AbstractTransactCommand<RemoteMcastMacs, HwvtepGlobalAugmentation> {
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
        //Remove the ones whose locator set got emptied
        Map<InstanceIdentifier<Node>, List<RemoteMcastMacs>> updated =
                extractUpdated(getChanges(),RemoteMcastMacs.class);
        if (!HwvtepSouthboundUtil.isEmptyMap(updated)) {
            for (Entry<InstanceIdentifier<Node>, List<RemoteMcastMacs>> entry:
                    updated.entrySet()) {
                List<RemoteMcastMacs> updatedList = entry.getValue();
                List<RemoteMcastMacs> tobeRemovedList = new ArrayList<>();
                if (!HwvtepSouthboundUtil.isEmpty(updatedList)) {
                    for (RemoteMcastMacs mac: updatedList) {
                        if (HwvtepSouthboundUtil.isEmpty(mac.getLocatorSet())) {
                            tobeRemovedList.add(mac);
                        }
                    }
                    removeMcastMacRemote(transaction, entry.getKey(), tobeRemovedList);
                }
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
		//could potentially delete the locators it is referring to
                transaction.add(op.delete(mcastMacsRemote.getSchema()).
                        where(mcastMacsRemote.getUuidColumn().getSchema().opEqual(macEntryUUID)).build());
                transaction.add(op.comment("McastMacRemote: Deleting " + mac.getMacEntryKey().getValue()));
            } else {
                LOG.warn("Unable to delete remoteMcastMacs {} because it was not found in the operational store",
                        mac.getMacEntryKey().getValue());
            }
            InstanceIdentifier<RemoteMcastMacs> macIid = instanceIdentifier.augmentation(HwvtepGlobalAugmentation.class).
                    child(RemoteMcastMacs.class, mac.getKey());
            updateCurrentTxDeleteData(RemoteMcastMacs.class, macIid, mac);
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
}
