/*
 * Copyright (c) 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
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
import java.util.Objects;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicalSwitchRemoveCommand
        extends AbstractTransactCommand<LogicalSwitches, LogicalSwitchesKey, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(LogicalSwitchRemoveCommand.class);
    List<LogicalSwitches> deletedLs;

    public LogicalSwitchRemoveCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LogicalSwitches>> removeds =
                extractRemoved(getChanges(), LogicalSwitches.class);
        if (removeds != null) {
            for (Entry<InstanceIdentifier<Node>, List<LogicalSwitches>> deleted: removeds.entrySet()) {
                deletedLs = deleted.getValue();
                for (LogicalSwitches lswitch : deleted.getValue()) {
                    InstanceIdentifier<LogicalSwitches> lsKey =
                            deleted.getKey().augmentation(HwvtepGlobalAugmentation.class)
                                    .child(LogicalSwitches.class, lswitch.key());
                    getDeviceInfo().clearConfigData(LogicalSwitches.class, lsKey);
                    onConfigUpdate(transaction, deleted.getKey(), lswitch, lsKey);
                }
            }
        }
    }

    @Override
    public void onConfigUpdate(final TransactionBuilder transaction, final InstanceIdentifier<Node> nodeIid,
                               final LogicalSwitches logicalSwitches, final InstanceIdentifier lsKey,
                               final Object... extraData) {
        processDependencies(EmptyDependencyGetter.INSTANCE, transaction, nodeIid, lsKey, logicalSwitches);
    }

    @Override
    public void doDeviceTransaction(final TransactionBuilder transaction,
                                    final InstanceIdentifier<Node> instanceIdentifier,
                                    final LogicalSwitches lswitch,
                                    final InstanceIdentifier lsKey,
                                    final Object... extraData) {
        LogicalSwitchUcastsRemoveCommand cmd = new LogicalSwitchUcastsRemoveCommand(
                newOperState(), getChanges(), deletedLs, lswitch);
        markKeyAsInTransit(LogicalSwitches.class, lsKey);
        clearConfigData(LogicalSwitches.class, lsKey);
        hwvtepOperationalState.getConnectionInstance().transact(cmd);
    }

    @Override
    protected Map<LogicalSwitchesKey, LogicalSwitches> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLogicalSwitches();
    }

    @Override
    protected boolean areEqual(final LogicalSwitches sw1, final LogicalSwitches sw2) {
        return sw1.key().equals(sw2.key()) && Objects.equals(sw1.getTunnelKey(), sw2.getTunnelKey());
    }

    @Override
    protected boolean isDeleteCmd() {
        return true;
    }

    @Override
    public void onCommandSucceeded() {
        for (MdsalUpdate mdsalUpdate : updates) {
            getDeviceInfo().clearLogicalSwitchRefs(mdsalUpdate.getKey());
        }
    }

    @Override
    protected String getKeyStr(InstanceIdentifier<LogicalSwitches> iid) {
        return iid.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue();
    }
}
