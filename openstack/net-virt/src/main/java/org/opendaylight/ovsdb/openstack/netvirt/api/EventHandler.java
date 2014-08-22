package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;

/**
 * Openstack related events can originate from north and south.
 * This interface provides a layer of abstraction between the event dispatcher and the
 * handler.
 */
public interface EventHandler {

    /**
     * Process the event.
     * @param event the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see EventDispatcher
     */
    public void processEvent(AbstractEvent event);

}
