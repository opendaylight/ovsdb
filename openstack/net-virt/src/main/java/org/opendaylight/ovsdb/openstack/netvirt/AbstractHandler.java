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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.ovsdb.plugin.api.Status;
import org.opendaylight.ovsdb.plugin.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

/**
 * OpenStack related events originate from multiple north callbacks as well as south.
 * This interface provides a layer of abstraction between the event dispatcher and the
 * handlers.
 */
public abstract class AbstractHandler implements BindingAwareConsumer {

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(AbstractHandler.class);

    private DataBroker dataBroker;

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile EventDispatcher eventDispatcher;

    @Override
    public void onSessionInitialized(BindingAwareBroker.ConsumerContext session) {
        dataBroker = session.getSALService(DataBroker.class);
        logger.info("AbstractHandler Initialized with CONSUMER CONTEXT {}", session.toString());
        processSessionInitialized();
    }

    protected abstract void processSessionInitialized();

    protected DataBroker getDataBroker() {
        return dataBroker;
    }

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
     * @param event the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher
     */
    protected void enqueueEvent(AbstractEvent abstractEvent) {
        Preconditions.checkNotNull(eventDispatcher);
        eventDispatcher.enqueueEvent(abstractEvent);
    }

    /**
     * Process the event.
     *
     * @param event the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher
     */
    public abstract void processEvent(AbstractEvent abstractEvent);

}
