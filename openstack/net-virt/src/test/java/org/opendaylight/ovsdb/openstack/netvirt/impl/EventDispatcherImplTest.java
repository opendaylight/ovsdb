/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
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
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Unit test for class EventDispatcherImpl
 */
@RunWith(MockitoJUnitRunner.class)
public class EventDispatcherImplTest {

    @Mock AbstractHandler handler;
    @InjectMocks EventDispatcherImpl eventDispatcherImpl;

    private AbstractEvent.HandlerType handlerTypeObject = AbstractEvent.HandlerType.NEUTRON_FLOATING_IP;
    public ServiceReference ref = mock(ServiceReference.class);

    @Before
    public void setUp() {
        Random r = new Random();

        eventDispatcherImpl.init();
        eventDispatcherImpl.start();

        // configure serviceReference
        when(ref.getProperty(org.osgi.framework.Constants.SERVICE_ID)).thenReturn(r.nextLong());
        when(ref.getProperty(Constants.EVENT_HANDLER_TYPE_PROPERTY)).thenReturn(handlerTypeObject);
    }

    /**
     * Test method {@link EventDispatcherImpl#eventHandlerAdded(ServiceReference, AbstractHandler)}
     */
    @Test
    public void testeventHandlerAdded() throws Exception{
        // get handlers from EventDispatcherImpl for test purposes
        AbstractHandler[] handlers = ( AbstractHandler[]) getClassField("handlers");

        // test when should be null
        assertNotEquals(handlers[handlerTypeObject.ordinal()], handler);

        // add handler
        eventDispatcherImpl.eventHandlerAdded(ref, handler);

        // test when handler added
        assertEquals(handlers[handlerTypeObject.ordinal()], handler);
    }

    /**
     * Test method {@link EventDispatcherImpl#eventHandlerRemoved(ServiceReference)}
     */
    @Test
    public void testHandlerRemoved() throws Exception{
        // get handlers from EventDispatcherImpl for test purposes
        AbstractHandler[] handlers = ( AbstractHandler[]) getClassField("handlers");

        // add a handler
        eventDispatcherImpl.eventHandlerAdded(ref, handler);

        // test when handler added
        assertEquals(handlers[handlerTypeObject.ordinal()], handler);

        // remove handler
        eventDispatcherImpl.eventHandlerRemoved(ref);

        // test once handler removed
        assertNull(handlers[handlerTypeObject.ordinal()]);
    }

    /**
     * Test method {@link EventDispatcherImpl#enqueueEvent(AbstractEvent)}
     */
    @Test
    public void testEnqueueEvent() throws Exception{
        // get events from EventDispatcherImpl for test purposes
        BlockingQueue<AbstractEvent> events = (BlockingQueue<AbstractEvent>) getClassField("events");

        // test before enqueue event
        assertEquals("Error, did not return the expected size, nothing has been added yet", 0, events.size());

        // enqueue event
        eventDispatcherImpl.enqueueEvent(mock(AbstractEvent.class));

        // test after enqueue event
        assertEquals("Error, did not return the expected size", 1, events.size());
    }

    /**
     * Get the specified field from EventDispatcherImpl using reflection
     * @param fieldName - the field to retrieve
     * @return the desired field
     */
    private Object getClassField(String fieldName) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
        Field field = EventDispatcherImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(eventDispatcherImpl);
    }
}
