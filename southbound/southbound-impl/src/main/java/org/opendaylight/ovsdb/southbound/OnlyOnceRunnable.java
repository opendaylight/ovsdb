/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.opendaylight.ovsdb.utils.mdsal.utils.Scheduler;

class OnlyOnceRunnable implements Runnable {

    volatile boolean alreadyRun = false;
    List<OnlyOnceRunnable> holder;
    Runnable runnable;

    OnlyOnceRunnable(Runnable runnable, List<OnlyOnceRunnable> holder) {
        this.runnable = runnable;
        this.holder = holder;
        holder.add(this);
        Scheduler.getScheduledExecutorService().schedule(this, 60, TimeUnit.SECONDS);
    }

    public void run() {
        if (!alreadyRun) {
            runnable.run();
        }
        holder.remove(this);
        alreadyRun = true;
    }
}
