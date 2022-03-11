/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.utils.mdsal.utils.Scheduler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlainLogicalSwitchRemoveCmd
        extends AbstractTransactCommand<LogicalSwitches, LogicalSwitchesKey, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(PlainLogicalSwitchRemoveCmd.class);

    private AtomicInteger retryCount = new AtomicInteger(5);
    private final LogicalSwitches logicalSwitches;
    private final InstanceIdentifier<Node> nodeIid;

    public PlainLogicalSwitchRemoveCmd(final HwvtepOperationalState state,
                                       final Collection<DataTreeModification<Node>> changes,
                                       final LogicalSwitches logicalSwitches,
                                       final int retryCount) {
        super(state, changes);
        this.logicalSwitches = logicalSwitches;
        this.retryCount = new AtomicInteger(retryCount);
        this.nodeIid = getOperationalState().getConnectionInstance().getInstanceIdentifier();
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        LogicalSwitch logicalSwitch = TyperUtils.getTypedRowWrapper(
                transaction.getDatabaseSchema(), LogicalSwitch.class, null);
        transaction.add(op.delete(logicalSwitch.getSchema())
                .where(logicalSwitch.getNameColumn().getSchema().opEqual(
                        logicalSwitches.getHwvtepNodeName().getValue())).build());
    }

    @Override
    protected Map<LogicalSwitchesKey, LogicalSwitches> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLogicalSwitches();
    }

    @Override
    protected boolean areEqual(final LogicalSwitches logicalSwitches1 , final LogicalSwitches logicalSwitches2) {
        return logicalSwitches1.key().equals(logicalSwitches2.key())
                && Objects.equals(logicalSwitches1.getTunnelKey(), logicalSwitches2.getTunnelKey());
    }

    @Override
    public boolean retry() {
        boolean ret = retryCount.decrementAndGet() > 0;
        if (ret) {
            Scheduler.getScheduledExecutorService().schedule(() -> {
                getOperationalState().getConnectionInstance().transact(this);
            }, HwvtepSouthboundConstants.LS_REMOVE_DELAY_SECS, TimeUnit.SECONDS);
        } else {
            LOG.error("Failed in deletion of logical switch {}", logicalSwitches);
        }
        return ret;
    }

    @Override
    protected boolean isDeleteCmd() {
        return true;
    }
}
