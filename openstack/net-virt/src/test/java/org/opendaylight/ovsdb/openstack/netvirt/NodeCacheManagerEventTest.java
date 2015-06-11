/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * Unit test for {@link NodeCacheManagerEvent}
 */

@RunWith(MockitoJUnitRunner.class)
public class NodeCacheManagerEventTest {

    @InjectMocks private NodeCacheManagerEvent nodeCacheManagerEvent;

    @Test
    public void testToString() {
        Node node = mock(Node.class);
        nodeCacheManagerEvent = new NodeCacheManagerEvent(node, Action.ADD);
        assertEquals("Error, toString() did not return the correct string", "NodeCacheManagerEvent [action=ADD, node=" + node + "]", nodeCacheManagerEvent.toString());
    }

    @Test
    public void testHashCode() {
        assertNotNull("Error, hashCode shouldn't be null", nodeCacheManagerEvent.hashCode());
    }

    @Test
    public void testEquals() {
        assertTrue("Error, the two object should be equal", nodeCacheManagerEvent.equals(nodeCacheManagerEvent));
        assertFalse("Error, the two object should not be equal", nodeCacheManagerEvent.equals(new String("dummy")));
    }
}
