/*
 * Copyright (c) 2014, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Contract for a transactional invoker.
 */
public interface TransactInvoker {
    /**
     * Invoke the given transactional command, with the given bridge state, on the given events.
     *
     * @param command The transactional command.
     * @param state The bridge state.
     * @param events The events to be processed.
     */
    void invoke(TransactCommand command, BridgeOperationalState state,
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events);
}
