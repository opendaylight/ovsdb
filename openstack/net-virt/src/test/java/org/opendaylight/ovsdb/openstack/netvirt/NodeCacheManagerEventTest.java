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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;

/**
 * Unit test for {@link NodeCacheManagerEvent}
 */
/* TODO SB_MIGRATION */
@Ignore
public class NodeCacheManagerEventTest {

    private NodeCacheManagerEvent nodeCacheManagerEvent;

    @Before
    public void setUp() {
//        /* TODO SB_MIGRATION */
//        nodeCacheManagerEvent = new NodeCacheManagerEvent("nodeIdentifier", Action.ADD);
    }

    @Test
    public void testToString() {
        assertEquals("Error, toString() did not return the correct string", "NodeCacheManagerEvent [action=ADD, nodeIdentifier=nodeIdentifier]", nodeCacheManagerEvent.toString());
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
