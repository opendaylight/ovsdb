/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CacheHelper {

    private CacheHelper(){}

    public static <K> Map<K, List<OnlyOnceRunnable>> createCacheForDeleteJobs() {
        return new ConcurrentHashMap<K, List<OnlyOnceRunnable>>() {
            @Override
            public List<OnlyOnceRunnable> remove(Object key) {
                List<OnlyOnceRunnable> result = super.remove(key);
                if (result != null) {
                    result.forEach(runnable -> runnable.run());
                }
                return result;
            }
        };
    }

    public static <K> void runAfterKeyIsRemoved(Runnable job, Map<K, List<OnlyOnceRunnable>> deletePendingJobs, K key) {
        synchronized (deletePendingJobs) {
            List<OnlyOnceRunnable> jobs = deletePendingJobs.get(key);
            if (jobs != null) {
                new OnlyOnceRunnable(job, jobs);
                return;
            }
        }
        job.run();
    }
}
