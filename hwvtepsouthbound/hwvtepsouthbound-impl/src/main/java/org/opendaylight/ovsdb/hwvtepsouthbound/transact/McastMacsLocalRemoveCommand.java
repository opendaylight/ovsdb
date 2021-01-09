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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsLocal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McastMacsLocalRemoveCommand
        extends AbstractTransactCommand<LocalMcastMacs, LocalMcastMacsKey, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(McastMacsLocalRemoveCommand.class);

    public McastMacsLocalRemoveCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LocalMcastMacs>> removeds =
                extractRemoved(getChanges(),LocalMcastMacs.class);
        if (!removeds.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, List<LocalMcastMacs>> removed:
                removeds.entrySet()) {
                removeMcastMacLocal(transaction,  removed.getKey(), removed.getValue());
            }
        }
    }

    private void removeMcastMacLocal(final TransactionBuilder transaction,
            final InstanceIdentifier<Node> instanceIdentifier, final List<LocalMcastMacs> macList) {
        for (LocalMcastMacs mac: macList) {
            LOG.debug("Removing localMcastMacs, mac address: {}", mac.getMacEntryKey().getValue());
            Optional<LocalMcastMacs> operationalMacOptional =
                    getOperationalState().getLocalMcastMacs(instanceIdentifier, mac.key());
            McastMacsLocal mcastMacsLocal = transaction.getTypedRowSchema(McastMacsLocal.class);
            if (operationalMacOptional.isPresent() && operationalMacOptional.get().getMacEntryUuid() != null) {
                //when mac entry is deleted, its referenced locator set and locators are deleted automatically.
                //TODO: locator in config DS is not deleted
                UUID macEntryUUID = new UUID(operationalMacOptional.get().getMacEntryUuid().getValue());
                mcastMacsLocal.getUuidColumn().setData(macEntryUUID);
                transaction.add(op.delete(mcastMacsLocal.getSchema())
                        .where(mcastMacsLocal.getUuidColumn().getSchema().opEqual(macEntryUUID)).build());
                transaction.add(op.comment("McastMacLocal: Deleting " + mac.getMacEntryKey().getValue()));
            } else {
                LOG.warn("Unable to delete localMcastMacs {} because it was not found in the operational store",
                        mac.getMacEntryKey().getValue());
            }
        }
    }

    @Override
    protected Map<LocalMcastMacsKey, LocalMcastMacs> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLocalMcastMacs();
    }

    @Override
    protected boolean cascadeDelete() {
        return true;
    }

    @Override
    protected UnMetDependencyGetter<LocalMcastMacs> getDependencyGetter() {
        return MacDependencyGetter.INSTANCE;
    }

    @Override
    protected boolean isDeleteCmd() {
        return true;
    }

    @Override
    protected String getKeyStr(InstanceIdentifier<LocalMcastMacs> iid) {
        return getLsKeyStr(iid.firstKeyOf(LocalMcastMacs.class).getLogicalSwitchRef().getValue());
    }

    // FIXME: hide/move/make this final
    public static class MacDependencyGetter extends UnMetDependencyGetter<LocalMcastMacs> {
        static final MacDependencyGetter INSTANCE = new MacDependencyGetter();

        protected MacDependencyGetter() {
            // Hidden on purpose
        }

        @Override
        public List<InstanceIdentifier<?>> getLogicalSwitchDependencies(final LocalMcastMacs data) {
            return List.of(data.getLogicalSwitchRef().getValue());
        }

        @Override
        public List<InstanceIdentifier<?>> getTerminationPointDependencies(final LocalMcastMacs data) {
            return List.of();
        }
    }
}
