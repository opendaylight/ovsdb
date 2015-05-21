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
import org.junit.Ignore;
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
 * Unit test for {@link EventDispatcherImpl}
 */
/* TODO SB_MIGRATION */ @Ignore
@RunWith(MockitoJUnitRunner.class)
public class EventDispatcherImplTest {

    @Mock AbstractHandler handler;
    @InjectMocks EventDispatcherImpl eventDispatcherImpl;

    private AbstractEvent.HandlerType handlerTypeObject = AbstractEvent.HandlerType.NEUTRON_FLOATING_IP;
    private ServiceReference ref = mock(ServiceReference.class);

    @Before
    public void setUp() {
        Random r = new Random();

        /* TODO SB_MIGRATION */
        //eventDispatcherImpl.init();
        eventDispatcherImpl.start();

        when(ref.getProperty(org.osgi.framework.Constants.SERVICE_ID)).thenReturn(r.nextLong());
        when(ref.getProperty(Constants.EVENT_HANDLER_TYPE_PROPERTY)).thenReturn(handlerTypeObject);
    }

    /**
     * Test methods {@link EventDispatcherImpl#eventHandlerRemoved(ServiceReference)}
     * and {@link EventDispatcherImpl#eventHandlerAdded(ServiceReference, AbstractHandler)}
     */
    @Test
    public void testHandlerAddedAndRemoved() throws Exception{
        AbstractHandler[] handlers = ( AbstractHandler[]) getClassField("handlers");

        assertNotEquals("Error, handler should be null", handlers[handlerTypeObject.ordinal()], handler);

        eventDispatcherImpl.eventHandlerAdded(ref, handler);

        assertEquals("Error, did not return the added handler", handlers[handlerTypeObject.ordinal()], handler);

        eventDispatcherImpl.eventHandlerRemoved(ref);

        assertNull("Error, handler should be null as it has just been removed", handlers[handlerTypeObject.ordinal()]);
    }

    /**
     * Test method {@link EventDispatcherImpl#enqueueEvent(AbstractEvent)}
     */
    @Test
    public void testEnqueueEvent() throws Exception{
        BlockingQueue<AbstractEvent> events = (BlockingQueue<AbstractEvent>) getClassField("events");

        assertEquals("Error, did not return the expected size, nothing has been added yet", 0, events.size());

        eventDispatcherImpl.enqueueEvent(mock(AbstractEvent.class));

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
