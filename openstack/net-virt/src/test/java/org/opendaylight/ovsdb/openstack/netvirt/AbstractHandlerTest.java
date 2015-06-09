/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
/**
 * Unit test for {@link AbstractHandler}
 */

public class AbstractHandlerTest {

    @InjectMocks private AbstractHandler abstractHandler = mock(AbstractHandler.class, Mockito.CALLS_REAL_METHODS);

    @Test
    public void testGetException() {
        Status status = mock(Status.class);

        when(status.getCode())
                .thenReturn(StatusCode.BADREQUEST)
                .thenReturn(StatusCode.CONFLICT)
                .thenReturn(StatusCode.NOTACCEPTABLE)
                .thenReturn(StatusCode.NOTFOUND)
                .thenReturn(StatusCode.GONE);

        assertEquals(
                "Error, getException() did not return the correct neutron API service error",
                HttpURLConnection.HTTP_BAD_REQUEST,
                AbstractHandler.getException(status));
        assertEquals(
                "Error, getException() did not return the correct neutron API service error",
                HttpURLConnection.HTTP_CONFLICT,
                AbstractHandler.getException(status));
        assertEquals(
                "Error, getException() did not return the correct neutron API service error",
                HttpURLConnection.HTTP_NOT_ACCEPTABLE,
                AbstractHandler.getException(status));
        assertEquals(
                "Error, getException() did not return the correct neutron API service error",
                HttpURLConnection.HTTP_NOT_FOUND,
                AbstractHandler.getException(status));
        assertEquals(
                "Error, getException() did not return the correct neutron API service error",
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                AbstractHandler.getException(status));
    }

    @Test
    public void testEnqueueEvent() throws Exception {
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        abstractHandler.setDispatcher(eventDispatcher);

        abstractHandler.enqueueEvent(mock(AbstractEvent.class));
        verify(eventDispatcher, times(1)).enqueueEvent(any(AbstractEvent.class));
    }
}
