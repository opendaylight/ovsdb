/*
 * Copyright (c) 2015, 2016 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Unit test for {@link EventDispatcherImpl}
 */
public class EventDispatcherImplTest {

    private EventDispatcherImpl eventDispatcherImpl = new EventDispatcherImpl();

    private AbstractHandler handler = mock(AbstractHandler.class);
    private ServiceReference<?> ref = mock(ServiceReference.class);

    private AbstractEvent.HandlerType handlerTypeObject = AbstractEvent.HandlerType.NEUTRON_FLOATING_IP;

    @Before
    public void setUp() {
        Random r = new Random();

        when(ref.getProperty(org.osgi.framework.Constants.SERVICE_ID)).thenReturn(r.nextLong());
        when(ref.getProperty(Constants.EVENT_HANDLER_TYPE_PROPERTY)).thenReturn(handlerTypeObject);
    }

    /**
     * Test methods {@link EventDispatcherImpl#eventHandlerRemoved(ServiceReference)}
     * and {@link EventDispatcherImpl#eventHandlerAdded(ServiceReference, AbstractHandler)}
     */
    @Test
    public void testHandlerAddedAndRemoved() throws Exception{
        AbstractHandler[] handlers = ( AbstractHandler[]) getField("handlers");

        assertNotEquals("Error, handler should be null", handlers[handlerTypeObject.ordinal()], handler);

        eventDispatcherImpl.eventHandlerAdded(ref, handler);

        assertEquals("Error, did not return the added handler", handlers[handlerTypeObject.ordinal()], handler);

        eventDispatcherImpl.eventHandlerRemoved(ref);

        assertNull("Error, handler should be null as it has just been removed", handlers[handlerTypeObject.ordinal()]);
    }

    /**
     * Test method {@link EventDispatcherImpl#enqueueEvent(AbstractEvent)}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testEnqueueEvent() throws Exception{
        BlockingQueue<AbstractEvent> events = (BlockingQueue<AbstractEvent>) getField("events");

        assertEquals("Error, did not return the expected size, nothing has been added yet", 0, events.size());

        eventDispatcherImpl.eventHandlerAdded(ref, handler);

        AbstractEvent mockEvent = mock(AbstractEvent.class);
        when(mockEvent.getHandlerType()).thenReturn(handlerTypeObject);
        final int numEnqueues = 4;
        for (int i = 0; i < numEnqueues; i++) {
            eventDispatcherImpl.enqueueEvent(mockEvent);
        }

        // Because events are processed in a different thread, this is a race
        // We want to ensure that all the events were enqueued; so the sum of
        // what's been processed and what's still in the queue must equal the
        // number of events we enqueued
        verify(handler, times(numEnqueues - events.size())).processEvent(mockEvent);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = EventDispatcherImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(eventDispatcherImpl);
    }
}
