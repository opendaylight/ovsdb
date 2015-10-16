/*
 *  Copyright (C) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 */
package org.opendaylight.ovsdb.utils.mdsal.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

public class NodeUtilsTest {
    private static final String OVS = "OVS";
    private static final String IDENTIFIER = "192.168.120.31:45001";
    private static final String OVS_IDENTIFIER = OVS + "|" + IDENTIFIER;
    private static final String BAD_IDENTIFIER = "BAD" + "|" + IDENTIFIER;

    @Test
    public void testGetId () {
        String identifier = NodeUtils.getId(OVS_IDENTIFIER);
        assertEquals("getId(" + OVS_IDENTIFIER + ") should return " + IDENTIFIER,
                identifier, IDENTIFIER);

        identifier = NodeUtils.getId(OVS);
        assertEquals("getId(" + OVS + ") should return " + OVS,
                identifier, OVS);

        identifier = NodeUtils.getId(BAD_IDENTIFIER);
        assertEquals("getId(" + BAD_IDENTIFIER + ") should return " + BAD_IDENTIFIER,
                identifier, BAD_IDENTIFIER);
    }

    @Test
    public void testGetOpenFlowNode () {
        Node node = NodeUtils.getOpenFlowNode(OVS_IDENTIFIER);
        assertNotNull("node should not be null", node);
        assertEquals("id should be " + OVS_IDENTIFIER,
                node.getId().getValue(), OVS_IDENTIFIER);
    }
}

