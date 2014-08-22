package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;

/**
 * Openstack related events will be enqueued into a common event queue.
 * This interface provides access to an event dispatcher, as well as registration to link dispatcher to which handlers
 * dispatcher will utilize.
 */
public interface EventDispatcher {
    /**
     * Register singleton EventHandler that is responsible for a given event type.
     * @param handlerType the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent.HandlerType} event type that
     *                    handler is going to be responsible for.
     * @param handler the (@link org.opendaylight.ovsdb.openstack.netvirt.api.EventHandler} handler to be used.
     */
    public void registerEventHandler(AbstractEvent.HandlerType handlerType, EventHandler handler);

    /**
     * Undo registration of EventHandler for a given handler type.
     * @param handlerType the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent.HandlerType} event type that
     *                    handler is going to be responsible for.
     * @param handler the (@link org.opendaylight.ovsdb.openstack.netvirt.api.EventHandler} handler to unregister. If
     *                handler is not what dispatcher is currently using for the provided handlerType, nothing will
     *                change.
     */
    public void unregisterEventHandler(AbstractEvent.HandlerType handlerType, EventHandler handler);

    /**
     * Enqueue the event.
     * @param event the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     */
    public void enqueueEvent(AbstractEvent event);
}

