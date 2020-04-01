/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.mdsal.utils;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DefaultBatchHandler implements ResourceHandler {

    private DataBroker dataBroker;
    private Integer batchSize;
    private Integer batchInterval;
    public LogicalDatastoreType datastoreType;

    public DefaultBatchHandler(DataBroker dataBroker, LogicalDatastoreType dataStoreType, Integer batchSize,
                               Integer batchInterval) {

        this.dataBroker = dataBroker;
        this.batchSize = batchSize;
        this.batchInterval = batchInterval;
        this.datastoreType = dataStoreType;

    }

    public void update(WriteTransaction tx, LogicalDatastoreType logicalDatastoreType,
                       final InstanceIdentifier identifier, final Object original, final Object update,
                       List<SubTransaction> transactionObjects) {
        if ((update != null) && !(update instanceof DataObject)) {
            return;
        }
        if (logicalDatastoreType != getDatastoreType()) {
            return;
        }

        SubTransaction subTransaction = new SubTransactionImpl();
        subTransaction.setAction(SubTransaction.UPDATE);
        subTransaction.setInstance(update);
        subTransaction.setInstanceIdentifier(identifier);
        transactionObjects.add(subTransaction);

        tx.merge(logicalDatastoreType, identifier, (DataObject) update, true);
    }

    public void create(WriteTransaction tx, final LogicalDatastoreType logicalDatastoreType,
                       final InstanceIdentifier identifier, final Object data,
                       List<SubTransaction> transactionObjects) {
        if ((data != null) && !(data instanceof DataObject)) {
            return;
        }
        if (logicalDatastoreType != getDatastoreType()) {
            return;
        }

        SubTransaction subTransaction = new SubTransactionImpl();
        subTransaction.setAction(SubTransaction.CREATE);
        subTransaction.setInstance(data);
        subTransaction.setInstanceIdentifier(identifier);
        transactionObjects.add(subTransaction);

        tx.put(logicalDatastoreType, identifier, (DataObject) data, true);
    }

    public void delete(WriteTransaction tx, final LogicalDatastoreType logicalDatastoreType,
                       final InstanceIdentifier identifier, final Object data,
                       List<SubTransaction> transactionObjects) {
        if ((data != null) && !(data instanceof DataObject)) {
            return;
        }
        if (logicalDatastoreType != getDatastoreType()) {
            return;
        }

        SubTransaction subTransaction = new SubTransactionImpl();
        subTransaction.setAction(SubTransaction.DELETE);
        subTransaction.setInstanceIdentifier(identifier);
        transactionObjects.add(subTransaction);

        tx.delete(logicalDatastoreType, identifier);
    }

    public DataBroker getResourceBroker() {
        return dataBroker;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getBatchInterval() {
        return batchInterval;
    }

    public LogicalDatastoreType getDatastoreType() {
        return datastoreType;
    }
}

