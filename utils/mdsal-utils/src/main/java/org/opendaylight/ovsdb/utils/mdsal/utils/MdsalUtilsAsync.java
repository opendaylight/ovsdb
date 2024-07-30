/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdsalUtilsAsync {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtilsAsync.class);

    private final DataBroker databroker;

    /**
     * Class constructor setting the data broker.
     *
     * @param dataBroker the {@link DataBroker}
     */
    public MdsalUtilsAsync(final DataBroker dataBroker) {
        this.databroker = dataBroker;
    }

    /**
     * Executes delete as a non blocking transaction and returns the future.
     *
     * @param store
     *            {@link LogicalDatastoreType} which should be modified
     * @param path
     *            {@link InstanceIdentifier} to read from
     * @return The {@link FluentFuture} object to which you can assign a
     *         callback
     */
    public <D extends DataObject> FluentFuture<? extends CommitInfo> delete(
                                    final LogicalDatastoreType store,
                                    final InstanceIdentifier<D> path)  {
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        transaction.delete(store, path);
        return transaction.commit();
    }

    /**
     * Executes delete as a non blocking transaction and assign a default callback.
     *
     * @param store
     *            {@link LogicalDatastoreType} which should be modified
     * @param path
     *            {@link InstanceIdentifier} to read from
     * @param operationDesc
     *            A brief description of the operation to perform
     */
    public <D extends DataObject> void delete(
                                    final LogicalDatastoreType store,
                                    final InstanceIdentifier<D> path,
                                    final String operationDesc)  {
        assignDefaultCallback(delete(store, path), operationDesc);
    }

    /**
     * Executes put as non blocking transaction and return the future.
     *
     * @param logicalDatastoreType
     *            {@link LogicalDatastoreType} which should be modified
     * @param path
     *            {@link InstanceIdentifier} for path to read
     * @param <D>
     *            The data object type
     * @return The {@link FluentFuture} object to which you can assign a
     *         callback
     */
    public <D extends DataObject> FluentFuture<? extends CommitInfo> put(
                                        final LogicalDatastoreType logicalDatastoreType,
                                        final InstanceIdentifier<D> path,
                                        final D data)  {
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        transaction.mergeParentStructurePut(logicalDatastoreType, path, data);
        return transaction.commit();
    }

    /**
     * Executes put as non blocking transaction and assign default callback.
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
        assignDefaultCallback(put(logicalDatastoreType, path, data), operationDesc);
    }

    /**
     * Executes merge as non blocking transaction and return the future.
     *
     * @param logicalDatastoreType
     *            {@link LogicalDatastoreType} which should be modified
     * @param path
     *            {@link InstanceIdentifier} for path to read
     * @param <D>
     *            The data object type
     * @param withParent
     *            Whether or not to create missing parent.
     * @return The {@link FluentFuture} object to which you can assign a
     *         callback
     */
    // FIXME: eliminate the boolean flag here to separate out the distinct code paths
    public <D extends DataObject> FluentFuture<? extends CommitInfo> merge(
                                        final LogicalDatastoreType logicalDatastoreType,
                                        final InstanceIdentifier<D> path,
                                        final D data,
                                        final boolean withParent)  {
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        if (withParent) {
            transaction.mergeParentStructureMerge(logicalDatastoreType, path, data);
        } else {
            transaction.merge(logicalDatastoreType, path, data);
        }
        return transaction.commit();
    }

    /**
     * Executes merge as non blocking transaction and assign default callback.
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
    // FIXME: eliminate the boolean flag here to separate out the distinct code paths
    public <D extends DataObject> void merge(
                                        final LogicalDatastoreType logicalDatastoreType,
                                        final InstanceIdentifier<D> path,
                                        final D data,
                                        final String operationDesc,
                                        final boolean withParent)  {
        assignDefaultCallback(merge(logicalDatastoreType, path, data, withParent), operationDesc);
    }

    /**
     * Executes read as non blocking transaction and assign a default callback
     * to close the transaction.
     *
     * @param store
     *            {@link LogicalDatastoreType} to read
     * @param path
     *            {@link InstanceIdentifier} for path to read
     * @param <D>
     *            The data object type
     * @return The {@link FluentFuture} object to which you can assign a
     *         callback
     */
    public <D extends DataObject> FluentFuture<Optional<D>> read(
                                        final LogicalDatastoreType store,
                                        final InstanceIdentifier<D> path)  {
        final ReadTransaction transaction = databroker.newReadOnlyTransaction();
        final FluentFuture<Optional<D>> future = transaction.read(store, path);
        final FutureCallback<Optional<D>> closeTransactionCallback = new FutureCallback<Optional<D>>() {
            @Override
            public void onSuccess(final Optional<D> result) {
                transaction.close();
            }

            @Override
            public void onFailure(final Throwable ex) {
                transaction.close();
            }
        };
        future.addCallback(closeTransactionCallback, MoreExecutors.directExecutor());
        return future;
    }

    /**
     * Assign a default callback to a {@link FluentFuture}. It will either log
     * a message at DEBUG level if the transaction succeed, or will log at ERROR
     * level and throw an {@link IllegalStateException} if the transaction
     * failed.
     *
     * @param transactionFuture
     *            The transaction to commit.
     * @param operationDesc
     *            A description of the transaction to commit.
     */
    void assignDefaultCallback(final FluentFuture<? extends CommitInfo> transactionFuture,
            final String operationDesc) {
        transactionFuture.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Transaction({}) SUCCESSFUL", operationDesc);
            }

            @Override
            public void onFailure(final Throwable ex) {
                LOG.error("Transaction({}) FAILED!", operationDesc, ex);
                throw new IllegalStateException("  Transaction(" + operationDesc + ") not committed correctly", ex);
            }
        }, MoreExecutors.directExecutor());
    }
}
