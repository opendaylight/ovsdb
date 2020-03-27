/*
 * Copyright © 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.lib.operations.Delete;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.operations.Update;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactInvokerImpl implements TransactInvoker {
    private static final Logger LOG = LoggerFactory.getLogger(TransactInvokerImpl.class);
    private final HwvtepConnectionInstance connectionInstance;
    private final DatabaseSchema dbSchema;

    public TransactInvokerImpl(HwvtepConnectionInstance connectionInstance, DatabaseSchema dbSchema) {
        this.connectionInstance = connectionInstance;
        this.dbSchema = dbSchema;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void invoke(TransactCommand command) {
        TransactionBuilder tb = new TransactionBuilder(connectionInstance.getOvsdbClient(), dbSchema);
        command.execute(tb);
        ListenableFuture<List<OperationResult>> result = tb.execute();
        LOG.debug("invoke: command: {}, tb: {}", command, tb);
        if (tb.getOperations().size() > 0) {
            try {
                List<OperationResult> got = result.get();
                LOG.debug("HWVTEP transaction result: {}", got);
                boolean errorOccured = false;
                if (got != null && got.size() > 0) {
                    for (OperationResult opResult : got) {
                        if (!Strings.isNullOrEmpty(opResult.getError())) {
                            LOG.error("HWVTEP transaction operation failed {} {}",
                                    opResult.getError(), opResult.getDetails());
                            errorOccured = true;
                        }
                    }
                }
                if (errorOccured) {
                    printError(tb);
                    command.onFailure(tb);
                } else {
                    command.onSuccess(tb);
                }
                if (errorOccured) {
                    if (!command.retry()) {
                        LOG.error("Failed on second attempt too aborting the transaction {}", command);
                        return;
                    }
                    LOG.error("Retrying the failed command {}", command);
                    //Oper data is going to be filled with latest data after monitor callback update comes and is
                    // processed but do not wait till then , pull the latest data and retry the failing command
                    //The command could be failing due to reference to stale physical locator/logical switch.
                    //pull the latest locators and put them in device oper data and retry the command.
                    connectionInstance.getDeviceInfo().clearDeviceOperData(LogicalSwitches.class);
                    connectionInstance.getDeviceInfo().clearDeviceOperData(TerminationPoint.class);

                    connectionInstance.getHwvtepTableReader().refreshLogicalSwitches();
                    connectionInstance.getHwvtepTableReader().refreshLocators();
                    connectionInstance.transact(command);
                }
            } catch (Exception e) {
                LOG.error("Transact execution exception: {} {}",
                        tb, connectionInstance.getInstanceIdentifier(), e);
            }

            LOG.trace("invoke exit command: {}, tb: {}", command, tb);
        }
    }

    void printError(TransactionBuilder tb) {
        StringBuilder sb = new StringBuilder();
        for (Operation op : tb.getOperations()) {
            if (op instanceof Insert) {
                Insert insert = (Insert)op;
                Map<String, Object> row = insert.getRow();
                sb.append("insert [");
                if (row != null) {
                    for (Entry<String, Object> entry : row.entrySet()) {
                        sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append(" , ");
                    }
                }
                sb.append("]   ");
            } else if (op instanceof Delete) {
                Delete delete = (Delete)op;
                sb.append("delete from ");
                sb.append(delete.getTableSchema().getName());
            } else if (op instanceof Update) {
                Update update = (Update)op;
                sb.append("update [");
                Map<String, Object> row = update.getRow();
                if (row != null) {
                    for (Entry<String, Object> entry : row.entrySet()) {
                        sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append(" , ");
                    }
                }
                sb.append(']');
            }
        }
        LOG.error("Failed transaction {}", sb);
    }
}
