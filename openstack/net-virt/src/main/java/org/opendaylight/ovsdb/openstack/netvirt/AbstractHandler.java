/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

/**
 * OpenStack related events originate from multiple north callbacks as well as south.
 * This interface provides a layer of abstraction between the event dispatcher and the
 * handlers.
 */
public abstract class AbstractHandler {
    static final Logger logger = LoggerFactory.getLogger(AbstractHandler.class);

    /*public AbstractHandler() {
        logger.info(">>>>> init {}", this.getClass());
    }*/

    // The implementation for each of these services is resolved by the OSGi Service Manager
    protected volatile EventDispatcher eventDispatcher;

    /*void init() {
        logger.info(">>>>> init {}", this.getClass());
    }*/

    /**
     * Convert failure status returned by the  manager into
     * neutron API service errors.
     *
     * @param status  manager status
     * @return  An error to be returned to neutron API service.
     */
    protected static final int getException(Status status) {
        int result = HttpURLConnection.HTTP_INTERNAL_ERROR;

        assert !status.isSuccess();

        StatusCode code = status.getCode();
        logger.debug(" Exception code - {}, description - {}",
                code, status.getDescription());

        if (code == StatusCode.BADREQUEST) {
            result = HttpURLConnection.HTTP_BAD_REQUEST;
        } else if (code == StatusCode.CONFLICT) {
            result = HttpURLConnection.HTTP_CONFLICT;
        } else if (code == StatusCode.NOTACCEPTABLE) {
            result = HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        } else if (code == StatusCode.NOTFOUND) {
            result = HttpURLConnection.HTTP_NOT_FOUND;
        } else {
            result = HttpURLConnection.HTTP_INTERNAL_ERROR;
        }

        return result;
    }

    /**
     * Enqueue the event.
     *
     * @param abstractEvent the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher
     */
    protected void enqueueEvent(AbstractEvent abstractEvent) {
        logger.info("enqueueEvent: evenDispatcher: {} - {}", eventDispatcher, abstractEvent);
        Preconditions.checkNotNull(eventDispatcher);
        eventDispatcher.enqueueEvent(abstractEvent);
    }

    /**
     * Process the event.
     *
     * @param abstractEvent the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher
     */
    public abstract void processEvent(AbstractEvent abstractEvent);

}
