/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.concurrent.Future;

/**
 * Created by Anil Vishnoi (avishnoi@Brocade.com) on 3/15/16.
 */
public interface TaskManager {

    abstract boolean isTaskAlreadyQueued(InstanceIdentifier<?> iid);
    abstract boolean cancleTask(InstanceIdentifier<?> iid);
    abstract void cacheTask(InstanceIdentifier<?> iid, Future<?> taskFuture);
}
