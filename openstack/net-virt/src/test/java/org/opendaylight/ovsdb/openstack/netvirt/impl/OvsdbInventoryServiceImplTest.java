/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryListener;

/**
 * Unit test for {@link NodeCacheManagerImpl}
 */
@RunWith(MockitoJUnitRunner.class)
public class OvsdbInventoryServiceImplTest {

    @InjectMocks private OvsdbInventoryServiceImpl ovsdbInventoryServiceImpl = new OvsdbInventoryServiceImpl(mock(ProviderContext.class));


    @Test
    public void testListenerAdded() throws Exception{
        Set<OvsdbInventoryListener> listeners = OvsdbInventoryServiceImpl.getOvsdbInventoryListeners();
        OvsdbInventoryListener listener = mock(OvsdbInventoryListener.class);

        ovsdbInventoryServiceImpl.listenerAdded(listener);
        assertEquals("Error, did not add the listener", 1, listeners.size());

        ovsdbInventoryServiceImpl.listenerRemoved(listener);
        assertEquals("Error, did not delete the listener", 0, listeners.size());
    }
}
