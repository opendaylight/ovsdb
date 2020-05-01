/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import javax.inject.Singleton;

@Singleton
public class Scheduler implements AutoCloseable {
    private static final ThreadFactory NAMED_THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("ovsdb-sched-%d").build();
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE
            = Executors.newSingleThreadScheduledExecutor(NAMED_THREAD_FACTORY);

    public static ScheduledExecutorService getScheduledExecutorService() {
        return SCHEDULED_EXECUTOR_SERVICE;
    }

    @Override
    public void close() {
        SCHEDULED_EXECUTOR_SERVICE.shutdown();
    }
}
