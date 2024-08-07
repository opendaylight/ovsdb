/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyQueue {
    private static final Logger LOG = LoggerFactory.getLogger(DependencyQueue.class);

    private final LinkedBlockingQueue<DependentJob> configWaitQueue = new LinkedBlockingQueue<>(
            HwvtepSouthboundConstants.WAITING_QUEUE_CAPACITY);
    private final LinkedBlockingQueue<DependentJob> opWaitQueue = new LinkedBlockingQueue<>(
            HwvtepSouthboundConstants.WAITING_QUEUE_CAPACITY);
    private final HwvtepDeviceInfo deviceInfo;
    private final Executor executor;

    @SuppressWarnings("checkstyle:IllegalCatch")
    public DependencyQueue(final ScheduledExecutorService executor, HwvtepDeviceInfo hwvtepDeviceInfo) {
        this.executor = executor;
        this.deviceInfo = hwvtepDeviceInfo;

        final AtomicReference<ScheduledFuture<?>> expiredTasksMonitorJob = new AtomicReference<>();
        expiredTasksMonitorJob.set(executor.scheduleWithFixedDelay(() -> {
            try {
                LOG.debug("Processing dependencies");
                if (!deviceInfo.getConnectionInstance().getOvsdbClient().isActive()) {
                    if (expiredTasksMonitorJob.get() != null) {
                        expiredTasksMonitorJob.get().cancel(false);
                    }
                }
                deviceInfo.onOperDataAvailable();
            } catch (RuntimeException e) {
                //If execution of one run throws error , subsequent runs are suppressed, hence catching the throwable
                LOG.error("Failed to process dependencies", e);
            }
        }, 0, HwvtepSouthboundConstants.IN_TRANSIT_STATE_CHECK_PERIOD_MILLIS, TimeUnit.MILLISECONDS));
    }

    /**
     * Tries to add the job to the waiting queue.
     *
     * @param waitingJob The job to be enqueued
     * @return true if it is successfully added to the queue
     */
    public boolean addToQueue(DependentJob waitingJob) {
        boolean addedToQueue;
        if (waitingJob instanceof DependentJob.ConfigWaitingJob) {
            addedToQueue = configWaitQueue.offer(waitingJob);
        } else {
            addedToQueue = opWaitQueue.offer(waitingJob);
        }
        if (addedToQueue) {
            LOG.debug("Added the waiting job {} to queue", waitingJob.getKey());
        } else {
            LOG.error("Failed to add the waiting job to queue {}", waitingJob.getKey());
        }
        return addedToQueue;
    }

    /**
     * Checks if any config data dependent jobs are ready to be processed and process them.
     *
     * @param connectionInstance The connection instance
     */
    public void processReadyJobsFromConfigQueue(HwvtepConnectionInstance connectionInstance) {
        processReadyJobs(connectionInstance, configWaitQueue);
    }

    /**
     * Checks if any operational data dependent jobs are ready to be processed and process them.
     *
     * @param connectionInstance The connection instance
     */
    public void processReadyJobsFromOpQueue(HwvtepConnectionInstance connectionInstance) {
        processReadyJobs(connectionInstance, opWaitQueue);
    }

    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    private void processReadyJobs(final HwvtepConnectionInstance hwvtepConnectionInstance,
                                  LinkedBlockingQueue<DependentJob> queue) {
        final var readyJobs = getReadyJobs(queue);
        readyJobs.forEach(job -> {
            executor.execute(() -> hwvtepConnectionInstance.transact(new TransactCommand() {
                final HwvtepOperationalState operationalState = new HwvtepOperationalState(hwvtepConnectionInstance);
                final AtomicInteger retryCount = new AtomicInteger(5);

                @Override
                public boolean retry() {
                    return retryCount.decrementAndGet() > 0;
                }

                @Override
                public void execute(TransactionBuilder transactionBuilder) {
                    deviceInfo.clearKeyFromDependencyQueue(job.getKey());
                    if (operationalState.getConnectionInstance() != null
                        && operationalState.getConnectionInstance().isActive()) {
                        job.onDependencyResolved(operationalState, transactionBuilder);
                    }
                }

                @Override
                public void onFailure(TransactionBuilder tx) {
                    job.onFailure();
                    operationalState.clearIntransitKeys();

                }

                @Override
                public void onSuccess(TransactionBuilder tx) {
                    job.onSuccess();
                    operationalState.getDeviceInfo().onOperDataAvailable();
                }
            }));
        });
    }

    private List<DependentJob> getReadyJobs(LinkedBlockingQueue<DependentJob> queue) {
        List<DependentJob> readyJobs = new ArrayList<>();
        Iterator<DependentJob> jobIterator = queue.iterator();
        while (jobIterator.hasNext()) {
            DependentJob job = jobIterator.next();
            long currentTime = System.currentTimeMillis();
            if (job.areDependenciesMet(deviceInfo)) {
                jobIterator.remove();
                readyJobs.add(job);
                continue;
            }
            if (job.isExpired(currentTime)) {
                deviceInfo.clearKeyFromDependencyQueue(job.getKey());
                jobIterator.remove();
                continue;
            }
        }
        return readyJobs;
    }

    public void submit(Runnable runnable) {
        executor.execute(runnable);
    }
}
