/*******************************************************************************
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Added unit tests for the BaseHandler class.
 *                     - Migrated test cases to JUnit4
 *******************************************************************************/

package org.opendaylight.ovsdb.neutron;

import java.net.HttpURLConnection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

@RunWith(JUnit4.class)
public class BaseHandlerTest {

    String nullString = null;
    String uuid = "ce044452-f22e-4ea4-a3ec-d1cde80cf996";
    String tenantId = "8d62bfa112fb4247aa20edc74235c1ce";
    String neutronId = "6b8fd2";

    @Test
    public void testGetException() {
        Status mockStatus = mock(Status.class);

        when(mockStatus.getCode()).thenReturn(StatusCode.BADREQUEST);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, BaseHandler.getException(mockStatus));

        when(mockStatus.getCode()).thenReturn(StatusCode.CONFLICT);
        assertEquals(HttpURLConnection.HTTP_CONFLICT, BaseHandler.getException(mockStatus));

        when(mockStatus.getCode()).thenReturn(StatusCode.NOTACCEPTABLE);
        assertEquals(HttpURLConnection.HTTP_NOT_ACCEPTABLE, BaseHandler.getException(mockStatus));

        when(mockStatus.getCode()).thenReturn(StatusCode.NOTFOUND);
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, BaseHandler.getException(mockStatus));

        when(mockStatus.getCode()).thenReturn(StatusCode.INTERNALERROR);
        assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, BaseHandler.getException(mockStatus));

    }

    @Test
    public void testIsValidNeutronID() {
        assertFalse(BaseHandler.isValidNeutronID(nullString));
        assertTrue(BaseHandler.isValidNeutronID(uuid));
        assertTrue(BaseHandler.isValidNeutronID(tenantId));
        assertTrue(BaseHandler.isValidNeutronID(neutronId));

    }

    @Test
    public void testConvertNeutronIDToKey() {

        String uuidResult = BaseHandler.convertNeutronIDToKey(uuid);
        String uuidExpected = "ce044452f22eea4a3ecd1cde80cf996";

        String tenantResult = BaseHandler.convertNeutronIDToKey(tenantId);
        String tenantExpected = "8d62bfa112fb247aa20edc74235c1ce";

        String neutronResult = BaseHandler.convertNeutronIDToKey(neutronId);

        assertEquals(uuidExpected, uuidResult);
        assertEquals(tenantExpected, tenantResult);
        assertEquals(neutronId, neutronResult);

    }
}
