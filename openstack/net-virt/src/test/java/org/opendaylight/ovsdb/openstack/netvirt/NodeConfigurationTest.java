/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;

/**
 * Unit test for {@link NodeConfiguration}
 */
public class NodeConfigurationTest {

    @Test
    public void testNodeConfiguration() {
        NodeConfiguration nodeConf = new NodeConfiguration();

        assertEquals("Error, did not populate the internal vlan queue correctly", Constants.MAX_VLAN - 1, nodeConf.getInternalVlans().size());
        assertNotNull("Error, tenant vlan map has not been initialized", nodeConf.getTenantVlanMap());
    }
}
