/*
* Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*
* Authors : Sam Hague
*/
package org.opendaylight.ovsdb.northbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NorthboundUtils.class)
public class OvsdbNorthboundV3Test {
    private static final String USER = "admin";

    @Test
    public void testSetSecurityContext () {
        SecurityContext securityContext = mock(SecurityContext.class);
        Principal principal = mock(Principal.class);

        when(securityContext.getUserPrincipal()).thenReturn(null)
                .thenReturn(principal);
        when(principal.getName()).thenReturn(USER);

        OvsdbNorthboundV3 ovsdbNorthboundV3 = new OvsdbNorthboundV3();

        // Check if SecurityContext is null
        ovsdbNorthboundV3.setSecurityContext(null);
        String userName = ovsdbNorthboundV3.getUserName();
        assertNull(userName);

        // Check if user has no Principal
        ovsdbNorthboundV3.setSecurityContext(securityContext);
        userName = ovsdbNorthboundV3.getUserName();
        assertNull(userName);

        // Success case
        ovsdbNorthboundV3.setSecurityContext(securityContext);
        userName = ovsdbNorthboundV3.getUserName();
        assertEquals(USER, userName);
    }

    @Test
    public void testGetNode () {
        PowerMockito.mockStatic(NorthboundUtils.class);
        when(NorthboundUtils.isAuthorized(anyString(), eq("default"), eq(Privilege.WRITE), anyObject()))
                .thenReturn(false)
                .thenReturn(true);

        OvsdbNorthboundV3 ovsdbNorthboundV3 = new OvsdbNorthboundV3();

        // Check for unauthorized user
        try {
            NodeResource nodeResource = ovsdbNorthboundV3.getNode();
            fail("Expected an UnauthorizedException to be thrown");
        } catch (UnauthorizedException e) {
            assertSame(UnauthorizedException.class, e.getClass());
        }

        // Success case
        NodeResource nodeResource = ovsdbNorthboundV3.getNode();
        assertNotNull(nodeResource);
    }
}
