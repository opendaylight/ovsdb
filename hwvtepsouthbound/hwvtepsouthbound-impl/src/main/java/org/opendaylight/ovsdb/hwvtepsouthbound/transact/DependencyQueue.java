/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

public class DependencyQueue {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyQueue.class);
    private static ThreadFactory threadFact = new ThreadFactoryBuilder().setNameFormat("hwvtep-waiting-job-%d").build();
    private static ExecutorService executorService = Executors.newSingleThreadScheduledExecutor(threadFact);

    private final LinkedBlockingQueue<DependentJob> configWaitQueue = new LinkedBlockingQueue<>(HwvtepSouthboundConstants.WAITING_QUEUE_CAPACITY);
    private final LinkedBlockingQueue<DependentJob> opWaitQueue = new LinkedBlockingQueue<>(HwvtepSouthboundConstants.WAITING_QUEUE_CAPACITY);
    private final HwvtepDeviceInfo deviceInfo;

    public DependencyQueue(HwvtepDeviceInfo hwvtepDeviceInfo) {
        this.deviceInfo = hwvtepDeviceInfo;
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
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    hwvtepConnectionInstance.transact(new TransactCommand() {
                        @Override
                        public void execute(TransactionBuilder transactionBuilder) {
                            HwvtepOperationalState operationalState = new HwvtepOperationalState(hwvtepConnectionInstance);
                            for (DependentJob job : readyJobs) {
                                job.onDependencyResolved(operationalState, transactionBuilder);
                            }
                        }
                    });
                }
            });
        }
    }

    private List<DependentJob> getReadyJobs(LinkedBlockingQueue<DependentJob> queue) {
        List<DependentJob> readyJobs = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        Iterator<DependentJob> jobIterator = queue.iterator();
        while(jobIterator.hasNext()) {
            DependentJob job = jobIterator.next();
            if (job.isExpired(currentTime)) {
                jobIterator.remove();
                continue;
            }
            if (job.areDependenciesMet(deviceInfo)) {
                jobIterator.remove();
                readyJobs.add(job);
            }
        }
        return readyJobs;
    }

    public void submit(Runnable runnable) {
        executorService.submit(runnable);
    }
}