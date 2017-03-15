/*
 * Copyright © 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalRouter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRouters;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicalRouterRemoveCommand extends AbstractTransactCommand<LogicalRouters, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(LogicalRouterRemoveCommand.class);

    public LogicalRouterRemoveCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LogicalRouters>> removed =
              extractRemoved(getChanges(),LogicalRouters.class);
      if (removed != null) {
          for (Entry<InstanceIdentifier<Node>, List<LogicalRouters>> created: removed.entrySet()) {
              if (!HwvtepSouthboundUtil.isEmpty(created.getValue())) {
                  for (LogicalRouters lRouter : created.getValue()) {
                      InstanceIdentifier<LogicalRouters> lsKey = created.getKey().augmentation(
                              HwvtepGlobalAugmentation.class).child(LogicalRouters.class, lRouter.getKey());
                      updateCurrentTxDeleteData(LogicalRouters.class, lsKey, lRouter);
                  }
                  getOperationalState().getDeviceInfo().scheduleTransaction(new TransactCommand() {
                      @Override
                      public void execute(TransactionBuilder transactionBuilder) {
                          LOG.debug("Running delete logical router in seperate tx {}", created.getKey());
                          removeLogicalRouter(transactionBuilder, created.getKey(), created.getValue());
                      }

                      @Override
                      public void onConfigUpdate(TransactionBuilder transaction, InstanceIdentifier nodeIid,
                                                 Identifiable data, InstanceIdentifier key, Object... extraData) {
                      }

                      @Override
                      public void doDeviceTransaction(TransactionBuilder transaction, InstanceIdentifier nodeIid,
                                                      Identifiable data, InstanceIdentifier key, Object... extraData) {
                      }
                  });
              }
          }
      }
    }

      private void removeLogicalRouter(TransactionBuilder transaction,
              final InstanceIdentifier<Node> instanceIdentifier, final List<LogicalRouters> lRouterList) {
          for (LogicalRouters lrouter: lRouterList) {
              LOG.debug("Removing logical router named: {}", lrouter.getHwvtepNodeName().getValue());
              Optional<LogicalRouters> operationalRouterOptional =
                      getOperationalState().getLogicalRouters(instanceIdentifier, lrouter.getKey());

              if (operationalRouterOptional.isPresent() &&
                      operationalRouterOptional.get().getLogicalRouterUuid() != null) {
                  LogicalRouter logicalRouter = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), LogicalRouter.class, null);
                  UUID logicalRouterUuid = new UUID(operationalRouterOptional.get().getLogicalRouterUuid().getValue());
                  transaction.add(op.delete(logicalRouter.getSchema())
                          .where(logicalRouter.getUuidColumn().getSchema().opEqual(logicalRouterUuid)).build());
                  transaction.add(op.comment("Logical Router: Deleting " + lrouter.getHwvtepNodeName().getValue()));
              } else {
                  LOG.warn("Unable to delete logical router {} because it was not found in the operational data store",
                          lrouter.getHwvtepNodeName().getValue());
              }
          }
      }

      @Override
      protected List<LogicalRouters> getData(final HwvtepGlobalAugmentation augmentation) {
          return augmentation.getLogicalRouters();
      }

      @Override
      protected boolean areEqual(final LogicalRouters a , final LogicalRouters b) {
          return a.getKey().equals(b.getKey());
      }
}