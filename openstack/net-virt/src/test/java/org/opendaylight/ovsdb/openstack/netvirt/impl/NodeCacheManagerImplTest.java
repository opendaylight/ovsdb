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
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.opendaylight.ovsdb.openstack.netvirt.NodeCacheManagerEvent;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheListener;
//import org.opendaylight.ovsdb.utils.mdsal.node.NodeUtils;
import org.osgi.framework.ServiceReference;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Maps;

/**
 * Unit test for {@link NodeCacheManagerImpl}
 */
/* TODO SB_MIGRATION */ @Ignore
@RunWith(PowerMockRunner.class)
//@PrepareForTest(NodeUtils.class)
public class NodeCacheManagerImplTest {

    @InjectMocks NodeCacheManagerImpl nodeCacheManagerImpl;
    @Spy private Map<Long, NodeCacheListener> handlers = Maps.newHashMap();

    @Test
    public void testProcessEvent() {
        NodeCacheManagerEvent ev = mock(NodeCacheManagerEvent.class);
        when(ev.getNodeIdentifier()).thenReturn("node_identifier");

        //PowerMockito.mockStatic(NodeUtils.class);
        //when(NodeUtils.getOpenFlowNode(anyString())).thenReturn(mock(Node.class));

        when(ev.getAction()).thenReturn(Action.ADD);
        nodeCacheManagerImpl.processEvent(ev);
        assertEquals("Error, did not add the event", 1, nodeCacheManagerImpl.getBridgeNodes().size());

        when(ev.getAction()).thenReturn(Action.DELETE);
        nodeCacheManagerImpl.processEvent(ev);
        assertEquals("Error, did not delete the event", 0, nodeCacheManagerImpl.getBridgeNodes().size());
    }

    @Test
    public void testCacheListenerAddedAndRemoved() {
        ServiceReference ref = mock(ServiceReference.class);
        when(ref.getProperty(org.osgi.framework.Constants.SERVICE_ID)).thenReturn(Long.valueOf(1));

        // add
        nodeCacheManagerImpl.cacheListenerAdded(ref, mock(NodeCacheListener.class));
        assertEquals("Error, cacheListenerAdded() did not add any listener", 1, handlers.size());
        // remove
        nodeCacheManagerImpl.cacheListenerRemoved(ref);
        assertEquals("Error, cacheListenerAdded() did not remove any listener", 0, handlers.size());
    }
}
