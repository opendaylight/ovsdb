/*******************************************************************************
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Added unit tests for the SubnetHandler class.
 *******************************************************************************/

package org.opendaylight.ovsdb.neutron;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;

import java.net.HttpURLConnection;

public class SubnetHandlerTest {

    NeutronSubnet subnet;
    SubnetHandler subnetHandler;

    @Before
    public void setUp() {

        subnet = new NeutronSubnet();
        subnetHandler = new SubnetHandler();
    }

    @Test
    public void testCanCreateSubnet() throws Exception {
        assertEquals(subnetHandler.canCreateSubnet(subnet), HttpURLConnection.HTTP_CREATED);
    }

    @Test
    public void testCanUpdateSubnet() throws Exception {
        assertEquals(subnetHandler.canUpdateSubnet(subnet, subnet), HttpURLConnection.HTTP_CREATED);
    }

    @Test
    public void testCanDeleteSubnet() throws Exception {
        assertEquals(subnetHandler.canDeleteSubnet(subnet), HttpURLConnection.HTTP_CREATED);
    }
}
