/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static org.mockito.Matchers.any;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;

/**
* Unit test for {@link OvsdbClientKey}
*
* @author Sam Hague (shague@redhat.com)
*/
@RunWith(PowerMockRunner.class)
@PrepareForTest(SouthboundMapper.class)
public class OvsdbClientKeyTest {
    private static final String ADDRESS_STR = "192.168.120.1";
    private static final String PORT_STR = "6640";
    private OvsdbClientKey ovsdbClientKeyTest;
    private InstanceIdentifier<Node> nodePath;

    @Before
    public void setUp() {
        Ipv4Address ipv4 = new Ipv4Address(ADDRESS_STR);
        PortNumber port = new PortNumber(Integer.parseInt(PORT_STR));
        ovsdbClientKeyTest = new OvsdbClientKey(new IpAddress(ipv4), port);

        String uriString = SouthboundConstants.OVSDB_URI_PREFIX + "://" + ADDRESS_STR + ":" + PORT_STR;
        Uri uri = new Uri(uriString);
        NodeId nodeId = new NodeId(uri);
        nodePath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(nodeId));
    }

    @Test
    public void testToInstanceIndentifier() {
        Assert.assertNotNull("OvsdbClientKey should not be null", ovsdbClientKeyTest);

        PowerMockito.mockStatic(SouthboundMapper.class);
        PowerMockito.when(SouthboundMapper.createInstanceIdentifier(any(IpAddress.class), any(PortNumber.class)))
                .thenReturn(nodePath);

        Assert.assertEquals("Failed to return " + nodePath, nodePath, ovsdbClientKeyTest.toInstanceIndentifier());
    }
}