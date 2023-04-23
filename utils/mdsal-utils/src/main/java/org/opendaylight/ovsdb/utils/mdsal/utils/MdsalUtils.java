/*
 * Copyright (c) 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdsalUtils {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtils.class);
    private static int MDSAL_MAX_READ_TRIALS = Integer.getInteger("mdsalutil.max.tries", 30);
    private static int MDSAL_READ_SLEEP_INTERVAL_MS = Integer.getInteger("mdsalutil.sleep.between.mdsal.reads", 1000);

    private final DataBroker databroker;

    /**
     * Class constructor setting the data broker.
     *
     * @param dataBroker the {@link DataBroker}
     */
    public MdsalUtils(DataBroker dataBroker) {
        this.databroker = dataBroker;
    }

    /**
     * Executes delete as a blocking transaction.
     *
     * @param store {@link LogicalDatastoreType} which should be modified
     * @param path {@link InstanceIdentifier} to read from
     * @param <D> the data object type
     * @return the result of the request
     */
    public <D extends DataObject> boolean delete(
            final LogicalDatastoreType store, final InstanceIdentifier<D> path)  {
        boolean result = false;
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        transaction.delete(store, path);
        FluentFuture<? extends CommitInfo> future = transaction.commit();
        try {
            future.get();
            result = true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to delete {} ", path, e);
        }
        return result;
    }

    /**
     * Executes merge as a blocking transaction.
     *
     * @param logicalDatastoreType {@link LogicalDatastoreType} which should be modified
     * @param path {@link InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the result of the request
     */
    public <D extends DataObject> boolean merge(
            final LogicalDatastoreType logicalDatastoreType, final InstanceIdentifier<D> path, D data)  {
        boolean result = false;
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        transaction.mergeParentStructureMerge(logicalDatastoreType, path, data);
        FluentFuture<? extends CommitInfo> future = transaction.commit();
        try {
            future.get();
            result = true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to merge {} ", path, e);
        }
        return result;
    }

    /**
     * Executes put as a blocking transaction.
     *
     * @param logicalDatastoreType {@link LogicalDatastoreType} which should be modified
     * @param path {@link InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the result of the request
     */
    public <D extends DataObject> boolean put(
            final LogicalDatastoreType logicalDatastoreType, final InstanceIdentifier<D> path, D data)  {
        boolean result = false;
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        transaction.mergeParentStructurePut(logicalDatastoreType, path, data);
        FluentFuture<? extends CommitInfo> future = transaction.commit();
        try {
            future.get();
            result = true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to put {} ", path, e);
        }
        return result;
    }

    /**
     * Executes read as a blocking transaction.
     *
     * @param store {@link LogicalDatastoreType} to read
     * @param path {@link InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the result as the data object requested
     */
    public <D extends DataObject> D read(
            final LogicalDatastoreType store, final InstanceIdentifier<? extends DataObject> path) {
        Optional<D> optionalDataObject = readOptional(store, path);
        if (optionalDataObject.isPresent()) {
            return optionalDataObject.orElseThrow();
        }
        LOG.debug("{}: Failed to read {}",
                Thread.currentThread().getStackTrace()[1], path);
        return null;
    }

    public <D extends DataObject> Optional<D> readOptional(
            final LogicalDatastoreType store, final InstanceIdentifier<? extends DataObject> path)  {
        int trialNo = 0;
        ReadTransaction transaction = databroker.newReadOnlyTransaction();
        do {
            try {
                Optional<D> result = transaction.read(store, (InstanceIdentifier<D>)path).get();
                transaction.close();
                return result;
            } catch (InterruptedException | ExecutionException e) {
                if (trialNo == 0) {
                    logReadFailureError(path, " mdsal Read failed exception retrying the read after sleep");
                }
                try {
                    transaction.close();
                    Thread.sleep(MDSAL_READ_SLEEP_INTERVAL_MS);
                    transaction = databroker.newReadOnlyTransaction();
                } catch (InterruptedException e1) {
                    logReadFailureError(path, " Sleep interrupted");
                }
            }
        } while (trialNo++ < MDSAL_MAX_READ_TRIALS);
        logReadFailureError(path, " All read trials exceeded");
        return Optional.empty();
    }


    public boolean exists(
        final LogicalDatastoreType store, final InstanceIdentifier<? extends DataObject> path) {
        int trialNo = 0;
        ReadTransaction transaction = databroker.newReadOnlyTransaction();
        do {
            try {
                FluentFuture<Boolean> result = transaction.exists(store, path);
                transaction.close();
                return result.get().booleanValue();
            } catch (InterruptedException | ExecutionException e) {
                if (trialNo == 0) {
                    logReadFailureError(path, " mdsal Read failed exception retrying the read after sleep");
                }
                try {
                    transaction.close();
                    Thread.sleep(MDSAL_READ_SLEEP_INTERVAL_MS);
                    transaction = databroker.newReadOnlyTransaction();
                } catch (InterruptedException e1) {
                    logReadFailureError(path, " Sleep interrupted");
                }
            }
        } while (trialNo++ < MDSAL_MAX_READ_TRIALS);
        logReadFailureError(path, " All read trials exceeded");
        return false;
    }

    private <D extends DataObject> void logReadFailureError(
            InstanceIdentifier<D> path, String cause) {
        LOG.error("{}: Failed to read {} Cause : {}", Thread.currentThread().getStackTrace()[2], path, cause);

    }
}
