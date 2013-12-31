/*******************************************************************************
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Added unit tests for the PortHandler class.
 *******************************************************************************/

package org.opendaylight.ovsdb.neutron;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;

import java.net.HttpURLConnection;

public class PortHandlerTest {

    private NeutronPort port;
    private PortHandler portHandler;

    @Before
    public void setUp() {
        port = new NeutronPort();
        portHandler = new PortHandler();
    }

    @Test
    public void testCanCreatePort() throws Exception {
        assertEquals(portHandler.canCreatePort(port), HttpURLConnection.HTTP_CREATED);
    }

    @Test
    public void testCanUpdatePort() throws Exception {
        assertEquals(portHandler.canUpdatePort(port, port), HttpURLConnection.HTTP_OK);
        assertEquals(portHandler.canUpdatePort(port, null), HttpURLConnection.HTTP_BAD_REQUEST);
    }

    @Test
    public void testCanDeletePort() throws Exception {
        assertEquals(portHandler.canDeletePort(port), HttpURLConnection.HTTP_OK);
    }

}
