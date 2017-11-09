/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DependencyQueue {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyQueue.class);
    private static final ThreadFactory threadFact = new ThreadFactoryBuilder().setNameFormat("hwvtep-waiting-job-%d").
            build();
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(threadFact);

    private final LinkedBlockingQueue<DependentJob> configWaitQueue = new LinkedBlockingQueue<>(
            HwvtepSouthboundConstants.WAITING_QUEUE_CAPACITY);
    private final LinkedBlockingQueue<DependentJob> opWaitQueue = new LinkedBlockingQueue<>(
            HwvtepSouthboundConstants.WAITING_QUEUE_CAPACITY);
    private final HwvtepDeviceInfo deviceInfo;
    private ScheduledFuture expiredTasksMonitorJob;

    @SuppressWarnings("unchecked")
    public DependencyQueue(HwvtepDeviceInfo hwvtepDeviceInfo) {
        this.deviceInfo = hwvtepDeviceInfo;
        expiredTasksMonitorJob = executorService.scheduleWithFixedDelay(() -> {
            try {
                LOG.debug("Processing dependencies");
                if (!deviceInfo.getConnectionInstance().getOvsdbClient().isActive()) {
                    expiredTasksMonitorJob.cancel(false);
                }
                deviceInfo.onOperDataAvailable();
            } catch (Throwable e) {
                //If execution of one run throws error , subsequent runs are suppressed, hence catching the throwable
                LOG.error("Failed to process dependencies", e);
            }
        }, 0, HwvtepSouthboundConstants.IN_TRANSIT_STATE_CHECK_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Tries to add the job to the waiting queue
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
     * Checks if any config data dependent jobs are ready to be processed and process them
     * @param connectionInstance The connection instance
     */
    public void processReadyJobsFromConfigQueue(HwvtepConnectionInstance connectionInstance) {
        processReadyJobs(connectionInstance, configWaitQueue);
    }

    /**
     * Checks if any operational data dependent jobs are ready to be processed and process them
     * @param connectionInstance The connection instance
     */
    public void processReadyJobsFromOpQueue(HwvtepConnectionInstance connectionInstance) {
        processReadyJobs(connectionInstance, opWaitQueue);
    }

    private void processReadyJobs(final HwvtepConnectionInstance hwvtepConnectionInstance,
                                  LinkedBlockingQueue<DependentJob> queue) {
        final List<DependentJob> readyJobs =  getReadyJobs(queue);
        if (readyJobs.size() > 0) {
            executorService.submit(() -> hwvtepConnectionInstance.transact(new TransactCommand() {
                HwvtepOperationalState operationalState;
                @Override
                public void execute(TransactionBuilder transactionBuilder) {
                    this.operationalState = new HwvtepOperationalState(hwvtepConnectionInstance);
                    for (DependentJob job : readyJobs) {
                        job.onDependencyResolved(operationalState, transactionBuilder);
                    }
                }

                @Override
                public void onFailure(TransactionBuilder deviceTransaction) {
                    readyJobs.forEach((job) -> job.onFailure(deviceTransaction));
                    operationalState.clearIntransitKeys();
                }

                @Override
                public void onSuccess(TransactionBuilder deviceTransaction) {
                    readyJobs.forEach((job) -> job.onSuccess(deviceTransaction));
                    operationalState.getDeviceInfo().onOperDataAvailable();
                }
            }));
        }
    }

    private List<DependentJob> getReadyJobs(LinkedBlockingQueue<DependentJob> queue) {
        List<DependentJob> readyJobs = new ArrayList<>();
        Iterator<DependentJob> jobIterator = queue.iterator();
        while(jobIterator.hasNext()) {
            DependentJob job = jobIterator.next();
            long currentTime = System.currentTimeMillis();

            //first check if its dependencies are met later check for expired status
            if (job.areDependenciesMet(deviceInfo)) {
                jobIterator.remove();
                readyJobs.add(job);
                continue;
            }
            if (job.isExpired(currentTime)) {
                jobIterator.remove();
                continue;
            }
        }
        return readyJobs;
    }

    public static void close() {
        executorService.shutdown();
    }

    public void submit(Runnable runnable) {
        executorService.submit(runnable);
    }
}