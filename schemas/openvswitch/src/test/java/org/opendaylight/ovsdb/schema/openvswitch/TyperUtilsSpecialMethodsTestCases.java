/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
;

public class TyperUtilsSpecialMethodsTestCases extends OpenVswitchSchemaTestBase {
    Logger logger = LoggerFactory.getLogger(TyperUtilsSpecialMethodsTestCases.class);

    @Override
    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
     }

    @Test
    public void testToString() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Bridge bridge = this.ovs.createTypedRowWrapper(Bridge.class);
        assertNotNull(bridge);
        bridge.setName(TEST_BRIDGE_NAME);
        bridge.setStatus(ImmutableMap.of("key", "value"));
        bridge.setFloodVlans(Sets.newHashSet(34L));
        assertNotNull(bridge.toString());
    }

    @Test
    public void testEquals() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Bridge bridge = this.ovs.createTypedRowWrapper(Bridge.class);
        assertNotNull(bridge);
        bridge.setName(TEST_BRIDGE_NAME);
        bridge.setStatus(ImmutableMap.of("key", "value"));
        bridge.setFloodVlans(Sets.newHashSet(34L));

        assertTrue("Equals check on same Bridge object", bridge.equals(bridge));

        Bridge bridge2 = this.ovs.createTypedRowWrapper(Bridge.class);
        assertNotNull(bridge2);
        bridge2.setName(bridge.getName());
        bridge2.setStatus(bridge.getStatusColumn().getData());
        bridge2.setFloodVlans(bridge.getFloodVlansColumn().getData());

        assertTrue("Equals check for different Bridge objects with same content", bridge.equals(bridge2));

        bridge2.setStpEnable(true);
        assertFalse("Equals check for different Bridge objects with different content", bridge.equals(bridge2));

        Port port = this.ovs.createTypedRowWrapper(Port.class);
        port.setName(bridge.getName());
        assertFalse("Equals check for a Bridge object and Port Object", bridge.equals(port));

        assertFalse("Equals check for a Typed Proxy object and non-proxy object", port.equals("String"));
    }

    @Test
    public void testHashCode() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Bridge bridge = this.ovs.createTypedRowWrapper(Bridge.class);

        assertNotNull(bridge);
        bridge.setName(TEST_BRIDGE_NAME);
        bridge.setStatus(ImmutableMap.of("key", "value"));
        bridge.setFloodVlans(Sets.newHashSet(34L));

        assertNotSame(bridge.hashCode(), 0);
    }

    @Override
    public void update(Object context, UpdateNotification upadateNotification) {

    }

    @Override
    public void locked(Object context, List<String> ids) {

    }

    @Override
    public void stolen(Object context, List<String> ids) {

    }
}
