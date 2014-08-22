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
