/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.device;

import java.util.List;

import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Delete;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.Update;

public class OperationHandler {

    void process(final List<Operation> operations,
                 final TxData currentTxData,
                 final DeviceData deviceData) {
        for (Operation operation : operations) {
            process(operation, currentTxData, deviceData);
        }
    }

    void process(final Operation operation,
                 final TxData currentTxData,
                 final DeviceData deviceData) {

        if (operation instanceof Insert) {
            processInsert(operation, currentTxData, deviceData);
        } else if (operation instanceof Update) {
            processUpdate(operation, currentTxData, deviceData);
        } else if (operation instanceof Delete) {
            processDelete(operation, currentTxData, deviceData);
        }
    }

    void processInsert(final Operation operation,
                       final TxData currentTxData,
                       final DeviceData deviceData) {

        String tableName = operation.getTable();
        UUID newUuid = new UUID(java.util.UUID.randomUUID().toString());
        Row insertedRow = TableUtil.createRow(operation, newUuid, ((Insert) operation).getRow(), null);

        currentTxData.addToCreatedData(newUuid, insertedRow);
        currentTxData.addTableNameForUuid(newUuid, tableName);
        currentTxData.addActualUuidForNamedUuid(new UUID(((Insert) operation).getUuidName()), newUuid);
    }

    void processUpdate(final Operation operation,
                       final TxData currentTxData,
                       final DeviceData deviceData) {

        String tableName = operation.getTable();
        Update update = (Update) operation;
        UUID updatedRowUuid = Predicates.GET_WHERE_UUID.apply(update.getWhere());

        Row oldRow = deviceData.getRowFromUuid(updatedRowUuid);
        Row updatedRow = TableUtil.createRow(operation, updatedRowUuid, update.getRow(), oldRow);

        currentTxData.addToUpdatedData(updatedRowUuid, updatedRow);
        currentTxData.addTableNameForUuid(updatedRowUuid, tableName);
    }

    void processDelete(final Operation operation,
                       final TxData currentTxData,
                       final DeviceData deviceData) {

        String tableName = operation.getTable();
        Delete delete = (Delete) operation;
        List<Condition> where = delete.getWhere();
        Condition condition = where.iterator().next();
        if (Predicates.IS_UUID_COLUMN_NAME.test(condition.getColumn())) {
            UUID deletedUuid = Predicates.GET_WHERE_UUID.apply(delete.getWhere());
            Row deletedRow = deviceData.getRowFromUuid(deletedUuid);
            if (deletedRow != null) {
                currentTxData.addToDeletedData(deletedUuid);
            }
        }
    }
}
