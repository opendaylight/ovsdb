/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker, Flavio Fernandes
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;

/**
 * Openstack related events will be enqueued into a common event queue.
 * This interface provides access to an event dispatcher, as well as registration to link dispatcher to which handlers
 * dispatcher will utilize.
 */
public interface EventDispatcher {
    /**
     * Enqueue the event.
     * @param event the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     */
    public void enqueueEvent(AbstractEvent event);
}

