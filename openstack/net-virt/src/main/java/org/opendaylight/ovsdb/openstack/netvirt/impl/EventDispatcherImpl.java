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
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class EventDispatcherImpl implements EventDispatcher {

    static final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);
    private ExecutorService eventHandler;
    private BlockingQueue<AbstractEvent> events;

    private volatile AbstractHandler[] handlers;

    void init() {
        eventHandler = Executors.newSingleThreadExecutor();
        this.events = new LinkedBlockingQueue<>();
        this.handlers = new AbstractHandler[AbstractEvent.HandlerType.size];
    }

    void start() {
        eventHandler.submit(new Runnable()  {
            @Override
            public void run() {
                while (true) {
                    AbstractEvent ev;
                    try {
                        ev = events.take();
                    } catch (InterruptedException e) {
                        logger.info("The event handler thread was interrupted, shutting down", e);
                        return;
                    }
                    dispatchEvent(ev);
                }
            }
        });
        logger.debug("event dispatcher is started");
    }

    void stop() {
        // stop accepting new tasks
        eventHandler.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!eventHandler.awaitTermination(10, TimeUnit.SECONDS)) {
                eventHandler.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!eventHandler.awaitTermination(10, TimeUnit.SECONDS))
                    logger.error("Dispatcher's event handler did not terminate");
            }
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            eventHandler.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        logger.debug("event dispatcher is stopped");
    }

    private void dispatchEvent(AbstractEvent ev) {
        AbstractHandler handler = handlers[ev.getHandlerType().ordinal()];
        if (handler == null) {
            logger.warn("event dispatcher found no handler for " + ev);
            return;
        }

        handler.processEvent(ev);
    }

    /**
     * Register AbstractHandler that is responsible for a given event type.
     *  @param handlerType the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent.HandlerType} event type
     *                     that handler is going to be responsible for.
     * @param handler      the (@link org.opendaylight.ovsdb.openstack.netvirt.api.AbstractHandler} handler to be used.
     */
    @Override
    public void registerEventHandler(AbstractEvent.HandlerType handlerType, AbstractHandler handler) {
        Preconditions.checkNotNull(handler);
        handlers[handlerType.ordinal()] = handler;
    }

    /**
     * Undo registration of AbstractHandler for a given handler type.
     *  @param handlerType the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent.HandlerType} event type that
     *                     handler is going to be responsible for.
     * @param handler      the (@link org.opendaylight.ovsdb.openstack.netvirt.api.AbstractHandler} handler to
     *                     unregister. If handler is not what dispatcher is currently using for the provided
     *                     handlerType, this function does nothing.
     */
    @Override
    public void unregisterEventHandler(AbstractEvent.HandlerType handlerType, AbstractHandler handler) {
        Preconditions.checkNotNull(handler);
        if (handlers[handlerType.ordinal()].equals(handler)) {
            handlers[handlerType.ordinal()] = null;
        }
    }

    /**
     * Enqueue the event.
     *
     * @param event the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     */
    @Override
    public void enqueueEvent(AbstractEvent event) {
        if (event == null) {
            return;
        }

        try {
            events.put(event);
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted while trying to enqueue event ", e);
        }
    }
}
