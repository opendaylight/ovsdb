/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestratorImpl;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.osgi.framework.BundleContext;

/**
 * Unit tests for {@link NetvirtProvidersProvider}
 */
public class NetvirtProvidersProviderTest {

    /**
     * Test for {@link NetvirtProvidersProvider#getTableOffset()}
     */
    @Test
    public void testGetTableOffset() {
        short tableOffset = 10;
        NetvirtProvidersProvider netvirtProvidersProvider = new NetvirtProvidersProvider(null, null, tableOffset);
        assertEquals("Table offset was not set", tableOffset, NetvirtProvidersProvider.getTableOffset());
    }

    /**
     * Test for method {@link NetvirtProvidersProvider#setTableOffset(short)}
     */
    @Test
    public void testSetTableOffset() {
        // verify a good value can be set
        short tableOffset = 0;
        NetvirtProvidersProvider netvirtProvidersProvider = new NetvirtProvidersProvider(null, null, tableOffset);

        tableOffset = 10;
        NetvirtProvidersProvider.setTableOffset(tableOffset);
        assertEquals("tableOffset was not set", tableOffset, NetvirtProvidersProvider.getTableOffset());
    }

    /**
     * Negative test for method {@link NetvirtProvidersProvider#setTableOffset(short)}
     */
    @Test
    public void testTableOffsetNegative() {
        // verify an out of range value is not set
        short tableOffset = 0;
        NetvirtProvidersProvider netvirtProvidersProvider = new NetvirtProvidersProvider(null, null, tableOffset);

        short tableOffsetBad = (short)(256 - Service.L2_FORWARDING.getTable());
        NetvirtProvidersProvider.setTableOffset(tableOffsetBad);
        assertEquals("tableOffset should not be set", 0, NetvirtProvidersProvider.getTableOffset());
    }
}
