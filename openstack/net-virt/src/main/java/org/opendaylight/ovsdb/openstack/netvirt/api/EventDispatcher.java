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
import org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler;

/**
 * Openstack related events will be enqueued into a common event queue.
 * This interface provides access to an event dispatcher, as well as registration to link dispatcher to which handlers
 * dispatcher will utilize.
 */
public interface EventDispatcher {
    /**
     * Register singleton AbstractHandler that is responsible for a given event type.
     * @param handlerType the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent.HandlerType} event type that
     *                    handler is going to be responsible for.
     * @param handler the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler} handler to be used.
     */
    public void registerEventHandler(AbstractEvent.HandlerType handlerType, AbstractHandler handler);

    /**
     * Undo registration of AbstractHandler for a given handler type.
     * @param handlerType the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent.HandlerType} event type that
     *                    handler is going to be responsible for.
     * @param handler the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler} handler to unregister. If
     *                handler is not what dispatcher is currently using for the provided handlerType, nothing will
     */
    public void unregisterEventHandler(AbstractEvent.HandlerType handlerType, AbstractHandler handler);

    /**
     * Enqueue the event.
     * @param event the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     */
    public void enqueueEvent(AbstractEvent event);
}

