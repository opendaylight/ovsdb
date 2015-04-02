/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import static org.junit.Assert.*;

import java.net.HttpURLConnection;

import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.plugin.api.Status;
import org.opendaylight.ovsdb.plugin.api.StatusCode;

/**
 * Unit test for {@link AbstractHandler}
 */
public class AbstractHandlerTest {

    @Test
    public void testAbstractHandler() {
        Status status = Mockito.mock(Status.class);

        Mockito.when(status.getCode())
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
}
