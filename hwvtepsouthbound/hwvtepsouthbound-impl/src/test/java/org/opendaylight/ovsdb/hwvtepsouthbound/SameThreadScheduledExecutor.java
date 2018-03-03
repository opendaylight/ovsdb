/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public abstract class SameThreadScheduledExecutor implements ScheduledExecutorService {

    @Override
    public Future<?> submit(Runnable runnable) {
        runnable.run();
        SettableFuture<?> ft = SettableFuture.create();
        ft.set(null);
        return ft;
    }
}
