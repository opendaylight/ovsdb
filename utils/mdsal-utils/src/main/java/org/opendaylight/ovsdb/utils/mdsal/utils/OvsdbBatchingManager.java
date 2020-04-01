/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataStoreUnavailableException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class lets other modules submit their CRUD methods to it. This class
 * will then supply a single transaction to such CRUD methods of the
 * subscribers, on which such subscribers write data to that transaction.
 * Finally the framework attempts to reliably write this single transaction
 * which represents a batch of an ordered list of entities owned by that subscriber,
 * to be written/updated/removed from a specific datastore as registered by the subscriber.
 */
public class OvsdbBatchingManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbBatchingManager.class);
    private static final int INITIAL_DELAY = 3000;
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    private static final int THREAD_POOL_SIZE = 1;
    private static final int MAX_MDSAL_TX_ATTEMPTS = 10;

    private DataBroker broker;
    private static final int PERIODICITY_IN_MS = Integer.getInteger("ovsdb.batch.interval", 1000);
    private static final int BATCH_SIZE = Integer.getInteger("ovsdb.batch.size", 20);
    private static String controllerExceptionsPkg = "org.opendaylight.controller.cluster.datastore.exceptions";

    public enum ShardResource {
        OPERATIONAL_TOPOLOGY(LogicalDatastoreType.OPERATIONAL);
        BlockingQueue<ActionableResource> queue = new LinkedBlockingQueue<>();
        LogicalDatastoreType datastoreType;

        ShardResource(LogicalDatastoreType datastoreType) {
            this.datastoreType = datastoreType;
        }

        public LogicalDatastoreType getDatastoreType() {
            return datastoreType;
        }

        BlockingQueue<ActionableResource> getQueue() {
            return queue;
        }
    }

    private final ConcurrentHashMap<String, Pair<BlockingQueue<ActionableResource>, ResourceHandler>>
            resourceHandlerMapper = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ScheduledExecutorService>
            resourceBatchingThreadMapper = new ConcurrentHashMap<>();

    private final Map<String, Set<InstanceIdentifier<?>>> pendingModificationByResourceType = new ConcurrentHashMap<>();

    private static OvsdbBatchingManager instance;

    static {
        instance = new OvsdbBatchingManager();
    }

    public static OvsdbBatchingManager getInstance() {
        return instance;
    }

    @Override
    public void close() {
        LOG.info("OVSDB ResourceBatchingManager Closed, closing all batched resources");
        resourceBatchingThreadMapper.values().forEach(ScheduledExecutorService::shutdown);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void registerBatchableResource(String resourceType, final BlockingQueue<ActionableResource> resQueue,
                                          final ResourceHandler resHandler) {
        try {
            Preconditions.checkNotNull(resQueue, "ResourceQueue to use for batching cannot not be null.");
            Preconditions.checkNotNull(resHandler, "ResourceHandler cannot not be null.");
            if (resourceHandlerMapper.containsKey(resourceType)) {
                throw new RuntimeException("Resource type already registered");
            }
            resourceHandlerMapper.put(resourceType, new ImmutablePair<>(resQueue, resHandler));
            ScheduledThreadPoolExecutor resDelegatorService =
                    (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(THREAD_POOL_SIZE,
                    new ThreadFactoryBuilder().setNameFormat("ovsdb").build());
            resourceBatchingThreadMapper.put(resourceType, resDelegatorService);
            LOG.info("Registered resourceType {} with batchSize {} and batchInterval {}", resourceType,
                    resHandler.getBatchSize(), resHandler.getBatchInterval());
            resDelegatorService.scheduleWithFixedDelay(new Batcher(resourceType), INITIAL_DELAY,
                    resHandler.getBatchInterval(), TIME_UNIT);
            pendingModificationByResourceType.putIfAbsent(resourceType, ConcurrentHashMap.newKeySet());
        } catch (Exception e) {
            LOG.error("Failed to register defaults " , e);
        }
    }

    public void registerDefaultBatchHandlers(DataBroker dataBroker) {
        LOG.info("Registering OVSDB default batch handlers");
        Integer batchSize = Integer.getInteger("ovsdb.batch.size", BATCH_SIZE);
        Integer batchInterval = Integer.getInteger("ovsdb.batch.interval", PERIODICITY_IN_MS);

        for (ShardResource shardResource : ShardResource.values()) {
            if (resourceHandlerMapper.containsKey(shardResource.name())) {
                continue;
            }
            DefaultBatchHandler batchHandler = new DefaultBatchHandler(dataBroker, shardResource.datastoreType,
                    batchSize, batchInterval);
            registerBatchableResource(shardResource.name(), shardResource.getQueue(), batchHandler);
        }
        LOG.info("Registered OVSDB default batch handlers");
    }

    private void beforeModification(String resoureType, InstanceIdentifier<?> iid) {
        pendingModificationByResourceType.get(resoureType).add(iid);
    }

    void afterModification(String resoureType, InstanceIdentifier<?> iid) {
        pendingModificationByResourceType.get(resoureType).remove(iid);
    }

    /**
     * Reads the identifier of the given resource type.
     * Not to be used by the applications  which uses their own resource queue
     *
     * @param resourceType resource type that was registered with batch manager
     * @param identifier   identifier to be read
     * @return a CheckFuture containing the result of the read
     */
    public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(
            String resourceType, InstanceIdentifier<T> identifier) {
        BlockingQueue<ActionableResource> queue = getQueue(resourceType);
        if (queue != null) {
            if (pendingModificationByResourceType.get(resourceType).contains(identifier)) {
                SettableFuture<Optional<T>> readFuture = SettableFuture.create();
                queue.add(new ActionableReadResource<>(identifier, readFuture));
                return Futures.makeChecked(readFuture, ReadFailedException.MAPPER);
            } else {
                ResourceHandler resourceHandler = resourceHandlerMapper.get(resourceType).getRight();
                try (ReadOnlyTransaction tx = resourceHandler.getResourceBroker().newReadOnlyTransaction()) {
                    return tx.read(resourceHandler.getDatastoreType(), identifier);
                }
            }
        }
        return Futures.immediateFailedCheckedFuture(new ReadFailedException(
                "No batch handler was registered for resource " + resourceType));
    }

    public ListenableFuture<Void> merge(ShardResource shardResource, InstanceIdentifier<?> identifier,
                                        DataObject updatedData) {
        LOG.trace("Adding the iid to merge operation : {}", identifier);
        BlockingQueue<ActionableResource> queue = shardResource.getQueue();
        if (queue != null) {
            beforeModification(shardResource.name(), identifier);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.UPDATE, updatedData, null/*oldData*/);
            queue.add(actResource);
            return actResource.getResultFt();
        }
        return Futures
                .immediateFailedFuture(new IllegalStateException("Queue missing for provided shardResource "
                        + shardResource.name()));
    }

    public void merge(String resourceType, InstanceIdentifier<?> identifier, DataObject updatedData) {
        LOG.trace("Adding the iid to merge operation : {}", identifier);
        BlockingQueue<ActionableResource> queue = getQueue(resourceType);
        if (queue != null) {
            beforeModification(resourceType, identifier);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.UPDATE, updatedData, null/*oldData*/);
            queue.add(actResource);
        }
    }

    public void addMdsalTask(ShardResource shardResource, ActionableResource resource) {
        boolean offered = shardResource.getQueue().offer(resource);
        LOG.trace("MdsalTask added to queue - {}", offered);
    }

    public ListenableFuture<Void> delete(ShardResource shardResource, InstanceIdentifier<?> identifier) {
        LOG.trace("Adding the iid to delete operation : {}", identifier);
        BlockingQueue<ActionableResource> queue = shardResource.getQueue();
        if (queue != null) {
            beforeModification(shardResource.name(), identifier);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.DELETE, null, null/*oldData*/);
            queue.add(actResource);
            return actResource.getResultFt();
        }
        return Futures
                .immediateFailedFuture(new IllegalStateException("Queue missing for provided shardResource "
                        + shardResource.name()));
    }

    public void delete(String resourceType, InstanceIdentifier<?> identifier) {
        LOG.trace("Adding the iid to delete operation : {}", identifier);
        BlockingQueue<ActionableResource> queue = getQueue(resourceType);
        if (queue != null) {
            beforeModification(resourceType, identifier);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.DELETE, null, null/*oldData*/);
            queue.add(actResource);
        }
    }

    public ListenableFuture<Void> put(ShardResource shardResource, InstanceIdentifier<?> identifier,
                                      DataObject updatedData) {
        LOG.trace("Adding the iid to put operation : {}", identifier);
        BlockingQueue<ActionableResource> queue = shardResource.getQueue();
        if (queue != null) {
            beforeModification(shardResource.name(), identifier);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.CREATE, updatedData, null/*oldData*/);
            queue.add(actResource);
            return actResource.getResultFt();
        }
        return Futures
                .immediateFailedFuture(new IllegalStateException("Queue missing for provided shardResource "
                        + shardResource.name()));
    }

    public void put(String resourceType, InstanceIdentifier<?> identifier, DataObject updatedData) {
        LOG.trace("Adding the iid to put operation : {}", identifier);
        BlockingQueue<ActionableResource> queue = getQueue(resourceType);
        if (queue != null) {
            beforeModification(resourceType, identifier);
            ActionableResource actResource = new ActionableResourceImpl(identifier.toString(),
                    identifier, ActionableResource.CREATE, updatedData, null/*oldData*/);
            queue.add(actResource);
        }
    }

    private BlockingQueue<ActionableResource> getQueue(String resourceType) {
        if (resourceHandlerMapper.containsKey(resourceType)) {
            return resourceHandlerMapper.get(resourceType).getLeft();
        }
        return null;
    }

    public void deregisterBatchableResource(String resourceType) {
        ScheduledExecutorService scheduledThreadPoolExecutor = resourceBatchingThreadMapper.get(resourceType);
        if (scheduledThreadPoolExecutor != null) {
            scheduledThreadPoolExecutor.shutdown();
        }
        resourceHandlerMapper.remove(resourceType);
        resourceBatchingThreadMapper.remove(resourceType);
    }

    private class Batcher implements Runnable {
        private final String resourceType;

        Batcher(String resourceType) {
            this.resourceType = resourceType;
        }

        @Override
        public void run() {
            List<ActionableResource> resList = new ArrayList<>();
            LOG.info("Running the ovs batch handler with Batch Size : {}, Interval : {}", BATCH_SIZE,
                    PERIODICITY_IN_MS);
            try {
                Pair<BlockingQueue<ActionableResource>, ResourceHandler> resMapper =
                        resourceHandlerMapper.get(resourceType);
                if (resMapper == null) {
                    LOG.error("Unable to find resourceMapper for batching the ResourceType {}", resourceType);
                    return;
                }
                BlockingQueue<ActionableResource> resQueue = resMapper.getLeft();
                ResourceHandler resHandler = resMapper.getRight();
                resList.add(resQueue.take());
                resQueue.drainTo(resList);

                long start = System.currentTimeMillis();
                int batchSize = resHandler.getBatchSize();

                int batches = resList.size() / batchSize;
                if (resList.size() > batchSize) {
                    LOG.info("Batched up resources of size {} into batches {} for resourcetype {}",
                            resList.size(), batches, resourceType);
                    for (int i = 0, j = 0; i < batches; j = j + batchSize,i++) {
                        new MdsalDsTask<>(resourceType, resList.subList(j, j + batchSize)).process();
                    }
                    // process remaining routes
                    LOG.trace("Picked up 1 size {} ", resList.subList(batches * batchSize, resList.size()).size());
                    new MdsalDsTask<>(resourceType, resList.subList(batches * batchSize, resList.size())).process();
                } else {
                    // process less than OR == batchsize routes
                    LOG.trace("Picked up 2 size {}", resList.size());
                    new MdsalDsTask<>(resourceType, resList).process();
                }

                long timetaken = System.currentTimeMillis() - start;
                LOG.info("Total taken ##time = {}ms for resourceList of size {} for resourceType {}",
                        timetaken, resList.size(), resourceType);

            } catch (InterruptedException e) {
                LOG.error("InterruptedException during run()", e);
            }

        }
    }

    private class MdsalDsTask<T extends DataObject> {
        String resourceType;
        List<ActionableResource> actResourceList;
        int attemptNo  = 0;

        MdsalDsTask(String resourceType, List<ActionableResource> actResourceList) {
            this.resourceType = resourceType;
            this.actResourceList = actResourceList;
        }

        @SuppressWarnings("checkstyle:IllegalCatch")
        public void process() {
            try {
                if (attemptNo++ < MAX_MDSAL_TX_ATTEMPTS) {
                    process2();
                }
            } catch (Throwable e) {
                LOG.error("Failed to process ", e);
            }
        }

        @SuppressWarnings("unchecked")
        public void process2() {
            LOG.trace("Picked up 3 size {} of resourceType {}", actResourceList.size(), resourceType);
            Pair<BlockingQueue<ActionableResource>, ResourceHandler> resMapper =
                    resourceHandlerMapper.get(resourceType);
            if (resMapper == null) {
                LOG.error("Unable to find resourceMapper for batching the ResourceType {}", resourceType);
                return;
            }
            ResourceHandler resHandler = resMapper.getRight();
            DataBroker dataBroker = resHandler.getResourceBroker();
            LogicalDatastoreType dsType = resHandler.getDatastoreType();
            ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
            List<SubTransaction> transactionObjects = new ArrayList<>();
            Map<SubTransaction, SettableFuture<Void>> txMap = new HashMap<>();
            for (ActionableResource parent : actResourceList) {
                if (parent instanceof ActionableReadResource) {
                    ((ActionableReadResource) parent).setModifications(Arrays.asList(parent));
                }
                for (ActionableResource actResource : parent.getModifications()) {
                    int startSize = transactionObjects.size();
                    switch (actResource.getAction()) {
                        case ActionableResource.CREATE:
                            resHandler.create(tx, dsType, actResource.getInstanceIdentifier(),
                                    actResource.getInstance(), transactionObjects);
                            break;
                        case ActionableResource.UPDATE:
                            Object updated = actResource.getInstance();
                            Object original = actResource.getOldInstance();
                            resHandler.update(tx, dsType, actResource.getInstanceIdentifier(), original,
                                    updated,transactionObjects);
                            break;
                        case ActionableResource.DELETE:
                            resHandler.delete(tx, dsType, actResource.getInstanceIdentifier(),
                                    actResource.getInstance(), transactionObjects);
                            break;
                        case ActionableResource.READ:
                            ActionableReadResource<DataObject> readAction =
                                    (ActionableReadResource<DataObject>)actResource;
                            ListenableFuture<Optional<DataObject>> future =
                                    tx.read(dsType, readAction.getInstanceIdentifier());
                            Futures.addCallback(future, new FutureCallback<Optional<DataObject>>() {
                                @Override
                                public void onSuccess(Optional<DataObject> result) {
                                    readAction.getReadFuture().set(result);
                                }

                                @Override
                                public void onFailure(Throwable failure) {
                                    readAction.getReadFuture().setException(failure);
                                }
                            }, MoreExecutors.directExecutor());
                            break;
                        default:
                            LOG.error("Unable to determine Action for ResourceType {} with ResourceKey {}",
                                    resourceType, actResource.getKey());
                    }
                    int endSize = transactionObjects.size();
                    if (endSize > startSize) {
                        txMap.put(transactionObjects.get(endSize - 1),
                                (SettableFuture<Void>) actResource.getResultFt());
                    }
                }
            }
            long start = System.currentTimeMillis();
            ListenableFuture<Void> futures = tx.submit();

            try {
                futures.get();
                actResourceList.forEach(actionableResource -> {
                    ((SettableFuture<Void>) actionableResource.getResultFt()).set(null);
                    postCommit(actionableResource.getAction(), actionableResource.getInstanceIdentifier());
                });
                long time = System.currentTimeMillis() - start;
                LOG.trace("##### Time taken for {} = {}ms", actResourceList.size(), time);

            } catch (InterruptedException | ExecutionException e) {
                if (isAskTimedOut(e.getCause())) {
                    process();
                    return;
                }
                LOG.error("Exception occurred while batch writing to datastore", e);
                LOG.info("Trying to submit transaction operations one at a time for resType {}", resourceType);
                for (SubTransaction object : transactionObjects) {
                    WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
                    switch (object.getAction()) {
                        case SubTransaction.CREATE:
                            writeTransaction.put(dsType, object.getInstanceIdentifier(),
                                    (DataObject) object.getInstance(), true);
                            break;
                        case SubTransaction.DELETE:
                            writeTransaction.delete(dsType, object.getInstanceIdentifier());
                            break;
                        case SubTransaction.UPDATE:
                            writeTransaction.merge(dsType, object.getInstanceIdentifier(),
                                    (DataObject) object.getInstance(), true);
                            break;
                        default:
                            LOG.error("Unable to determine Action for transaction object with id {}",
                                    object.getInstanceIdentifier());
                    }
                    ListenableFuture<Void> futureOperation = writeTransaction.submit();
                    try {
                        futureOperation.get();
                        if (txMap.containsKey(object)) {
                            txMap.get(object).set(null);
                        } else {
                            LOG.error("Subtx object {} has no Actionable-resource associated with it !! ",
                                    object.getInstanceIdentifier());
                        }
                    } catch (InterruptedException | ExecutionException exception) {
                        if (txMap.containsKey(object)) {
                            txMap.get(object).setException(exception);
                        }
                        LOG.error("Error {} to datastore (path, data) : ({}, {})", object.getAction(),
                                object.getInstanceIdentifier(), object.getInstance(), exception);
                    } finally {
                        postCommit(object.getAction(), object.getInstanceIdentifier());
                    }
                }
            }
        }

        private void postCommit(int action, InstanceIdentifier iid) {
            switch (action) {
                case ActionableResource.CREATE:
                case ActionableResource.UPDATE:
                case ActionableResource.DELETE:
                    afterModification(resourceType, iid);
                    break;
                default:
                    break;
            }
        }
    }

    private String clsName(Throwable clz) {
        if (clz != null) {
            return clz.getClass().getSimpleName().toLowerCase(Locale.getDefault());
        }
        return "";
    }

    private String pkgName(Throwable clz) {
        if (clz != null) {
            return clz.getClass().getPackage().getName();
        }
        return "";
    }

    boolean isAskTimedOut(Throwable throwable) {
        if (clsName(throwable).contains("AskTime") || clsName(throwable.getCause()).contains("AskTime")) {
            return true;
        }
        if (throwable instanceof DataStoreUnavailableException
                || throwable.getCause() instanceof DataStoreUnavailableException) {
            return true;
        }
        String pkgName = pkgName(throwable);
        String pkgName2 = pkgName(throwable.getCause());
        if (controllerExceptionsPkg.equals(pkgName) || controllerExceptionsPkg.equals(pkgName2)) {
            return true;
        }
        return false;
    }

    public static class ActionableReadResource<T extends DataObject> extends ActionableResourceImpl {
        private final SettableFuture<Optional<T>> readFuture;

        ActionableReadResource(InstanceIdentifier<T> identifier, SettableFuture<Optional<T>> readFuture) {
            super(identifier.toString(), identifier, ActionableResource.READ, null, null);
            this.readFuture = readFuture;
        }

        SettableFuture<Optional<T>> getReadFuture() {
            return readFuture;
        }
    }
}