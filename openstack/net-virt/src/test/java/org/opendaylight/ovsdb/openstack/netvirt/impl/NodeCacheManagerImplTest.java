/*
 * Copyright (c) 2015, 2016 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.ovsdb.openstack.netvirt.NodeCacheManagerEvent;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheListener;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology
        .Node;
import org.osgi.framework.ServiceReference;

/**
 * Unit test for {@link NodeCacheManagerImpl}
 */
@RunWith(MockitoJUnitRunner.class)
public class NodeCacheManagerImplTest {

    @InjectMocks private NodeCacheManagerImpl nodeCacheManagerImpl;
    @Spy private Map<Long, NodeCacheListener> handlers = Maps.newHashMap();

    @Mock private Southbound southbound;

    @Test
    public void testProcessEvent() {
        NodeCacheManagerEvent ev = mock(NodeCacheManagerEvent.class);
        Node node = mock(Node.class);
        when(node.getNodeId()).thenReturn(mock(NodeId.class));
        when(ev.getNode()).thenReturn(node);

        when(ev.getAction()).thenReturn(Action.UPDATE);
        nodeCacheManagerImpl.processEvent(ev);
        assertEquals("Error, did not delete the event", 1, nodeCacheManagerImpl.getNodes().size());

        when(ev.getAction()).thenReturn(Action.DELETE);
        nodeCacheManagerImpl.processEvent(ev);
        assertEquals("Error, did not delete the event", 0, nodeCacheManagerImpl.getNodes().size());
    }

    @Test
    public void testCacheListenerAddedAndRemoved() {
        ServiceReference ref = mock(ServiceReference.class);
        when(ref.getProperty(org.osgi.framework.Constants.SERVICE_ID)).thenReturn(1L);

        // add
        nodeCacheManagerImpl.cacheListenerAdded(ref, mock(NodeCacheListener.class));
        assertEquals("Error, cacheListenerAdded() did not add any listener", 1, handlers.size());
        // remove
        nodeCacheManagerImpl.cacheListenerRemoved(ref);
        assertEquals("Error, cacheListenerAdded() did not remove any listener", 0, handlers.size());
    }

    @Test
    public void testGetOvsdbNodes() {
        addItem();

        when(southbound.extractOvsdbNode(any(Node.class))).thenReturn(mock(OvsdbNodeAugmentation.class));

        assertEquals("Error, getOvsdbNodes() did not return the correct value", 1, nodeCacheManagerImpl.getOvsdbNodes().size());
    }

    @Test
    public void testGetBridgeNodes() {
        addItem();

        when(southbound.getBridge(any(Node.class))).thenReturn(mock(OvsdbBridgeAugmentation.class));

        assertEquals("Error, getBridgeNodes() did not return the correct value", 1, nodeCacheManagerImpl.getBridgeNodes().size());
    }

    @Test
    public void testGetNodes() {
        addItem();

        assertEquals("Error, getNodes() did not return the correct value", 1, nodeCacheManagerImpl.getNodes().size());
    }

    private void addItem() {
        NodeCacheManagerEvent ev = mock(NodeCacheManagerEvent.class);
        Node node = mock(Node.class);
        when(node.getNodeId()).thenReturn(mock(NodeId.class));
        when(ev.getNode()).thenReturn(node);

        when(ev.getAction()).thenReturn(Action.UPDATE);
        nodeCacheManagerImpl.processEvent(ev);
    }

    @Test
    public void testSetDependencies() throws Exception {
        Southbound southbound = mock(Southbound.class);
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);

        ServiceHelper.overrideGlobalInstance(Southbound.class, southbound);
        ServiceHelper.overrideGlobalInstance(EventDispatcher.class, eventDispatcher);

        nodeCacheManagerImpl.setDependencies(mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
        assertEquals("Error, did not return the correct object", getSuperField("eventDispatcher"), eventDispatcher);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = NodeCacheManagerImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(nodeCacheManagerImpl);
    }

    private Object getSuperField(String fieldName) throws Exception {
        Field field = NodeCacheManagerImpl.class.getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(nodeCacheManagerImpl);
    }
}
