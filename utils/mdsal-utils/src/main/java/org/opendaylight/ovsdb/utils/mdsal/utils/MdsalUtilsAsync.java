/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdsalUtilsAsync {

    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtilsAsync.class);
    private final DataBroker databroker;

    /**
     * Class constructor setting the data broker.
     *
     * @param dataBroker the {@link org.opendaylight.controller.md.sal.binding.api.DataBroker}
     */
    public MdsalUtilsAsync(final DataBroker dataBroker) {
        this.databroker = dataBroker;
    }

    /**
     * Executes delete as a non blocking transaction.
     *
     * @param store
     *            {@link LogicalDatastoreType} which should be modified
     * @param path
     *            {@link InstanceIdentifier} to read from
     * @param <D>
     *            The data object type
     * @param operationDesc
     *            A brief description of the operation to perform
     */
    public <D extends DataObject> void delete(
                                    final LogicalDatastoreType store,
                                    final InstanceIdentifier<D> path,
                                    final String operationDesc)  {
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        transaction.delete(store, path);
        commitTransaction(transaction, operationDesc);
    }

    /**
     * Executes put as non blocking transaction.
     *
     * @param logicalDatastoreType
     *            {@link LogicalDatastoreType} which should be modified
     * @param path
     *            {@link InstanceIdentifier} for path to read
     * @param <D>
     *            The data object type
     * @param operationDesc
     *            A brief description of the operation to perform
     */
    public <D extends DataObject> void put(
                                        final LogicalDatastoreType logicalDatastoreType,
                                        final InstanceIdentifier<D> path,
                                        final D data,
                                        final String operationDesc)  {
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        transaction.put(logicalDatastoreType, path, data, true);
        commitTransaction(transaction, operationDesc);
    }

    /**
     * Executes merge as non blocking transaction.
     *
     * @param logicalDatastoreType
     *            {@link LogicalDatastoreType} which should be modified
     * @param path
     *            {@link InstanceIdentifier} for path to read
     * @param <D>
     *            The data object type
     * @param operationDesc
     *            A brief description of the operation to perform
     * @param withParent
     *            Whether or not to create missing parent.
     */
    public <D extends DataObject> void merge(
                                        final LogicalDatastoreType logicalDatastoreType,
                                        final InstanceIdentifier<D> path,
                                        final D data,
                                        final String operationDesc,
                                        final boolean withParent)  {
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        transaction.merge(logicalDatastoreType, path, data, withParent);
        commitTransaction(transaction, operationDesc);
    }

    /**
     * Executes read as non blocking transaction.
     *
     * @param store
     *            {@link LogicalDatastoreType} to read
     * @param path
     *            {@link InstanceIdentifier} for path to read
     * @param <D>
     *            The data object type
     * @return The result as a checkedFuture
     */
    public <D extends DataObject> CheckedFuture<Optional<D>, ReadFailedException> read(
                                        final LogicalDatastoreType store,
                                        final InstanceIdentifier<D> path)  {
        final ReadOnlyTransaction transaction = databroker.newReadOnlyTransaction();
        final CheckedFuture<Optional<D>, ReadFailedException> future = transaction.read(store, path);
        final FutureCallback<Optional<D>> closeTransactionCallback = new FutureCallback<Optional<D>>() {
            @Override
            public void onSuccess(final Optional<D> result) {
                transaction.close();
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Read transaction failed!", t);
            }
        };
        Futures.addCallback(future, closeTransactionCallback);
        return future;
    }

    /**
     * Submit a write transaction and assigned a callback.
     *
     * @param transaction
     *            The transaction to commit.
     * @param operationDesc
     *            A description of the transaction to commit.
     */
    void commitTransaction(final WriteTransaction transaction, final String operationDesc) {
        LOG.trace("Committing Transaction {}:{}", operationDesc, transaction.getIdentifier());
        final CheckedFuture<Void, TransactionCommitFailedException> result = transaction.submit();

        Futures.addCallback(result, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Transaction({}) {} SUCCESSFUL", operationDesc, transaction.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Transaction({}) {} FAILED!", operationDesc, transaction.getIdentifier(), t);
                throw new IllegalStateException("  Transaction(" + operationDesc + ") not committed correctly", t);
            }
        });
    }
}
