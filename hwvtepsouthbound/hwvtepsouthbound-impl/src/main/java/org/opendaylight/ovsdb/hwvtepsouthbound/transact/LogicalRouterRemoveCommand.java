/*
 * Copyright © 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalRouter;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRouters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRoutersKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicalRouterRemoveCommand
        extends AbstractTransactCommand<LogicalRouters, LogicalRoutersKey, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(LogicalRouterRemoveCommand.class);

    public LogicalRouterRemoveCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LogicalRouters>> removed =
              extractRemoved(getChanges(),LogicalRouters.class);

        for (Entry<InstanceIdentifier<Node>, List<LogicalRouters>> created: removed.entrySet()) {
            if (!HwvtepSouthboundUtil.isEmpty(created.getValue())) {
                getOperationalState().getDeviceInfo().scheduleTransaction(new TransactCommand<>() {
                    @Override
                    public void execute(final TransactionBuilder transactionBuilder) {
                        HwvtepConnectionInstance connectionInstance = getDeviceInfo().getConnectionInstance();
                        HwvtepOperationalState operState = new HwvtepOperationalState(
                                connectionInstance.getDataBroker(), connectionInstance, Collections.emptyList());
                        hwvtepOperationalState = operState;
                        deviceTransaction = transactionBuilder;
                        LOG.debug("Running delete logical router in seperate tx {}", created.getKey());
                        removeLogicalRouter(transactionBuilder, created.getKey(), created.getValue());
                    }


                    @Override
                    public void onSuccess(final TransactionBuilder deviceTransaction) {
                        LogicalRouterRemoveCommand.this.onSuccess(deviceTransaction);
                    }

                    @Override
                    public void onFailure(final TransactionBuilder deviceTransaction) {
                        LogicalRouterRemoveCommand.this.onFailure(deviceTransaction);
                    }
                });
            }
        }
    }

    private void removeLogicalRouter(final TransactionBuilder transaction,
            final InstanceIdentifier<Node> instanceIdentifier, final List<LogicalRouters> routerList) {
        final var op = ops();

        for (LogicalRouters lrouter: routerList) {
            LOG.debug("Removing logical router named: {}", lrouter.getHwvtepNodeName().getValue());
            Optional<LogicalRouters> operationalRouterOptional =
                    getOperationalState().getLogicalRouters(instanceIdentifier, lrouter.key());

            if (operationalRouterOptional.isPresent()
                    && operationalRouterOptional.orElseThrow().getLogicalRouterUuid() != null) {
                LogicalRouter logicalRouter = transaction.getTypedRowSchema(LogicalRouter.class);
                UUID logicalRouterUuid = new UUID(
                    operationalRouterOptional.orElseThrow().getLogicalRouterUuid().getValue());
                transaction.add(op.delete(logicalRouter.getSchema())
                        .where(logicalRouter.getUuidColumn().getSchema().opEqual(logicalRouterUuid)).build());
                transaction.add(op.comment("Logical Router: Deleting " + lrouter.getHwvtepNodeName().getValue()));
                updateControllerTxHistory(TransactionType.DELETE, logicalRouter);
            } else {
                LOG.warn("Unable to delete logical router {} because it was not found in the operational data store",
                        lrouter.getHwvtepNodeName().getValue());
            }
        }
    }

    @Override
    protected Map<LogicalRoutersKey, LogicalRouters> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLogicalRouters();
    }

    @Override
    protected boolean areEqual(final LogicalRouters routers1, final LogicalRouters routers2) {
        return routers1.key().equals(routers2.key());
    }

    @Override
    public boolean isDeleteCmd() {
        return true;
    }
}
