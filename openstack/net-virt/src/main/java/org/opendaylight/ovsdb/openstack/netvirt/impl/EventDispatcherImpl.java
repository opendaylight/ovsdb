/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker, Flavio Fernandes
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler;
import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class EventDispatcherImpl implements EventDispatcher, ConfigInterface {
    private static final Logger LOG = LoggerFactory.getLogger(EventDispatcher.class);
    private ExecutorService eventHandler;
    private volatile BlockingQueue<AbstractEvent> events;
    private AbstractHandler[] handlers;

    public EventDispatcherImpl() {
        events = new LinkedBlockingQueue<>();
        handlers = new AbstractHandler[AbstractEvent.HandlerType.size];
        eventHandler = Executors.newSingleThreadExecutor();
        start();
    }

    void start() {
        eventHandler.submit(new Runnable()  {
            @Override
            public void run() {
                Thread t = Thread.currentThread();
                t.setName("EventDispatcherImpl");
                LOG.info("EventDispatcherImpl: started {}", t.getName());
                while (true) {
                    AbstractEvent ev;
                    try {
                        ev = events.take();
                    } catch (InterruptedException e) {
                        LOG.info("The event handler thread was interrupted, shutting down", e);
                        return;
                    }
                    try {
                        dispatchEvent(ev);
                    } catch (Exception e) {
                        LOG.error("Exception in dispatching event {}", ev.toString(), e);
                    }
                }
            }
        });
        LOG.debug("event dispatcher is started");
    }

    void stop() {
        // stop accepting new tasks
        eventHandler.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!eventHandler.awaitTermination(10, TimeUnit.SECONDS)) {
                eventHandler.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!eventHandler.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOG.error("Dispatcher's event handler did not terminate");
                }
            }
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            eventHandler.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        LOG.debug("event dispatcher is stopped");
    }

    private void dispatchEvent(AbstractEvent ev) {
        AbstractHandler handler = handlers[ev.getHandlerType().ordinal()];
        if (handler == null) {
            LOG.warn("event dispatcher found no handler for {}", ev);
            return;
        }

        handler.processEvent(ev);
    }

    public void eventHandlerAdded(final ServiceReference ref, AbstractHandler handler){
        Long pid = (Long) ref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
        Object handlerTypeObject = ref.getProperty(Constants.EVENT_HANDLER_TYPE_PROPERTY);
        if (!(handlerTypeObject instanceof AbstractEvent.HandlerType)){
            LOG.error("Abstract handler reg failed to provide a valid handler type: {} ref: {} handler: {}",
                    handlerTypeObject, ref.getClass().getName(), handler.getClass().getName());
            return;
        }
        AbstractEvent.HandlerType handlerType = (AbstractEvent.HandlerType) handlerTypeObject;
        handlers[handlerType.ordinal()] = handler;
        LOG.info("eventHandlerAdded: handler: {}, pid: {}, type: {}",
                handler.getClass().getName(), pid, handlerType);
    }

    public void eventHandlerRemoved(final ServiceReference ref){
        Long pid = (Long) ref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
        Object handlerTypeObject = ref.getProperty(Constants.EVENT_HANDLER_TYPE_PROPERTY);
        if (!(handlerTypeObject instanceof AbstractEvent.HandlerType)){
            LOG.error("Abstract handler unreg failed to provide a valid handler type {}", handlerTypeObject);
            return;
        }
        AbstractEvent.HandlerType handlerType = (AbstractEvent.HandlerType) handlerTypeObject;
        handlers[handlerType.ordinal()] = null;
        LOG.debug("Event handler for type {} unregistered pid {}", handlerType, pid);
    }

    /**
     * Enqueue the event.
     *
     * @param event the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     */
    @Override
    public void enqueueEvent(AbstractEvent event) {
        if (event == null) {
            LOG.warn("enqueueEvent: event is null");
            return;
        }

        try {
            events.put(event);
        } catch (InterruptedException e) {
            LOG.error("Thread was interrupted while trying to enqueue event ", e);
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {}

    @Override
    public void setDependencies(Object impl) {}
}
