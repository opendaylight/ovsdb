/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent.HandlerType;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;

/**
 * Unit test for {@link AbstractEvent}
 */
public class AbstractEventTest {

    private AbstractEventChild1 abstractEvent1;
    private AbstractEventChild2 abstractEvent2;
    private AbstractEventChild2 abstractEvent3;

    class AbstractEventChild1 extends AbstractEvent{
        protected AbstractEventChild1(HandlerType handlerType, Action action) {
            super(handlerType, action);
        }

    }

    class AbstractEventChild2 extends AbstractEvent{
        protected AbstractEventChild2(HandlerType handlerType, Action action) {
            super(handlerType, action);
        }
    }


    @Before
    public void setUp(){
         abstractEvent1 = new AbstractEventChild1(HandlerType.SOUTHBOUND, Action.DELETE);
         abstractEvent2 = new AbstractEventChild2(HandlerType.NEUTRON_FLOATING_IP, Action.ADD);
         abstractEvent3 = abstractEvent2;
    }

    @Test
    public void testAbstractEvent(){
        assertEquals("Error, getAction() did not return the correct value", Action.DELETE, abstractEvent1.getAction());

        assertEquals("Error, getHandlerType() did not return the correct value", HandlerType.SOUTHBOUND, abstractEvent1.getHandlerType());

        assertTrue("Error, equals() did not succeed", abstractEvent2.equals(abstractEvent3));

        assertNotNull("Error, hashCode() did not return any value", abstractEvent1.hashCode());
        assertEquals("Error, hashCode() is not consistent", abstractEvent2.hashCode(), abstractEvent3.hashCode());

        assertEquals("Error, toString() did not return the correct value",
                "AbstractEvent [transactionId=1 handlerType=SOUTHBOUND action=DELETE]", abstractEvent1.toString());
    }
}
