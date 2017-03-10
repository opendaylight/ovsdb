/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

public class UcastMacsLocalRemoveCommand extends AbstractTransactCommand<LocalUcastMacs, HwvtepGlobalAugmentation> {
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
                ucastMacsLocal.getUuidColumn().setData(macEntryUUID);
                transaction.add(op.delete(ucastMacsLocal.getSchema()).
                        where(ucastMacsLocal.getUuidColumn().getSchema().opEqual(macEntryUUID)).build());
                transaction.add(op.comment("UcastMacLocal: Deleting " + mac.getMacEntryKey().getValue()));
            } else {
                LOG.warn("Unable to delete remoteUcastMacs {} because it was not found in the operational store",
                        mac.getMacEntryKey().getValue());
            }
        }
    }

    @Override
    protected List<LocalUcastMacs> getData(HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLocalUcastMacs();
    }

    @Override
    protected boolean cascadeDelete() {
        return true;
    }

    @Override
    protected UnMetDependencyGetter getDependencyGetter() {
        return MAC_DEPENDENCY_GETTER;
    }

    static UnMetDependencyGetter MAC_DEPENDENCY_GETTER = new MacDependencyGetter();

    public static class MacDependencyGetter extends UnMetDependencyGetter<LocalUcastMacs> {
        @Override
        public List<InstanceIdentifier<?>> getLogicalSwitchDependencies(LocalUcastMacs data) {
            return Collections.singletonList(data.getLogicalSwitchRef().getValue());
        }

        @Override
        public List<InstanceIdentifier<?>> getTerminationPointDependencies(LocalUcastMacs data) {
            return Collections.emptyList();
        }
    }
}