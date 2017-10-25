/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.device;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepMonitorCallback;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactCommand;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactInvoker;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Select;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceTransactionInvokerImpl implements TransactInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceTransactionInvokerImpl.class);

    private final HwvtepConnectionInstance connectionInstance;
    private final DatabaseSchema dbSchema;
    private final HwvtepMonitorCallback callback;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat("device-updates-%d").build());

    private final DeviceData deviceData = new DeviceData();
    private final TxValidator txValidator = new TxValidator();
    private final OperationHandler operationHandler = new OperationHandler();
    private final List<TableUpdates> heldUpdates = new ArrayList<>();
    private volatile boolean holdUpdates = false;

    public DeviceTransactionInvokerImpl(final HwvtepConnectionInstance connectionInstance,
                                        final DatabaseSchema dbSchema,
                                        final HwvtepMonitorCallback callback) {
        this.connectionInstance = connectionInstance;
        this.dbSchema = dbSchema;
        this.callback = callback;
    }

    public void holdUpdates() {
        holdUpdates = true;
    }

    public void releaseUpdates() {
        holdUpdates = false;
        heldUpdates.forEach(update -> fireUpdateCallback(update));
        fireUpdateForOrphanedLocators();
    }

    @Override
    public void invoke(final TransactCommand command) {
        TransactionBuilder tb = null;
        try {
            tb = new TransactionBuilder(connectionInstance.getOvsdbClient(), dbSchema);
            command.execute(tb);
            List<Operation> operations = tb.getOperations();
            if (operations == null || operations.isEmpty()) {
                return;
            }
            TxData currentTxData = new TxData();
            TableUpdates tableUpdates = validateAndCommit(operations, currentTxData);
            if (holdUpdates) {
                heldUpdates.add(tableUpdates);
            } else {
                fireUpdateCallback(tableUpdates);
                fireUpdateForOrphanedLocators();
            }
            command.onSuccess(tb);
        } catch (Exception e) {
            command.onFailure(tb);
            LOG.error("Failed to handle client request ", e);
        }
    }

    private TableUpdates validateAndCommit(List<Operation> operations, TxData currentTxData) {
        operationHandler.process(operations, currentTxData, deviceData);
        currentTxData.replaceNamedUuidRefs();
        txValidator.validate(currentTxData, deviceData);
        TableUpdates tableUpdates = TableUtil.createTableUpdate(currentTxData, deviceData);
        deviceData.commit(currentTxData);
        return tableUpdates;
    }

    private void fireUpdateCallback(TableUpdates tableUpdates) {
        executorService.submit(() -> {
            try {
                callback.update(tableUpdates, dbSchema);
            } catch (Exception e) {
                LOG.error("Failed to update monitor callback ", e);
            }
        });
    }

    private void fireUpdateForOrphanedLocators() {
        deviceData.clearLocatorSets();
        Map<UUID, Row> clearedLocators = deviceData.clearLocators();
        if (!clearedLocators.isEmpty()) {
            TxData txData = new TxData();
            clearedLocators.keySet().forEach(uuid -> {
                txData.addToDeletedData(uuid);
                txData.addTableNameForUuid(uuid, Predicates.PHYSICAL_LOCATOR);
            });
            Map<String, TableUpdate> tableUpdateMap = new HashMap<>();
            txData.getDeletedUuids().forEach(deleted -> {
            TableUtil.createTableUpdate(new ImmutablePair<UUID, Row>(deleted, null), tableUpdateMap, txData,
                    deviceData, (deviceData, entry) -> clearedLocators.get(deleted), (deviceData, entry) -> null);});
            TableUpdates tableUpdates = new TableUpdates(tableUpdateMap);
            executorService.submit(() -> {
                try {
                    LOG.error("Submitting cleared locators callback");
                    callback.update(tableUpdates, dbSchema);
                } catch (Exception e) {
                    LOG.error("Failed to update monitor callback ", e);
                }
            });
        }
    }

    public List<OperationResult> select(List<Operation> operations) {
        List<OperationResult> results = new ArrayList<>();
        if (operations == null || operations.isEmpty()) {
            return results;
        }
        for (Operation operation : operations) {
            OperationResult operationResult = new OperationResult();
            if (operation instanceof Select) {
                List<Condition> conditions = ((Select) operation).getWhere();
                Condition condition = conditions.iterator().next();
                Row row = null;
                if (Predicates.IS_UUID_COLUMN_NAME.test(condition.getColumn())) {
                    UUID requestedUuid = Predicates.GET_WHERE_UUID.apply(((Select) operation).getWhere());
                    row = deviceData.getRowFromUuid(requestedUuid);
                } else {
                    String columnName = condition.getColumn();
                    Object columnValue = condition.getValue();
                    row = deviceData.getRow(operation.getTable(), columnName, columnValue);
                }
                if (row != null) {
                    operationResult.setRows(Lists.newArrayList(row));
                } else {
                    operationResult.setRows(Lists.newArrayList());
                }
            }
            results.add(operationResult);
        }
        return results;
    }
}
