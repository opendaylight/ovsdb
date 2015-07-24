/*
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Sam Hague
 */
package org.opendaylight.ovsdb.compatibility.plugin.api;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeUtils {
    private static final Logger LOG = LoggerFactory.getLogger(NodeUtils.class);

    public static String getId (String identifier) {
        String id = identifier;

        String[] pair = identifier.split("\\|");
        if (pair[0].equals("OVS")) {
            id = pair[1];
        }
        return id;
    }

    public static Node getMdsalNode (org.opendaylight.controller.sal.core.Node salNode) {
        String identifier = salNode.getNodeIDString();

        NodeId nodeId = new NodeId("OVS" + "|" + identifier);
        NodeKey nodeKey = new NodeKey(nodeId);
        Node node = new NodeBuilder()
                .setId(nodeId)
                .setKey(nodeKey)
                .build();

        return node;
    }

    public static org.opendaylight.controller.sal.core.Node getSalNode (Node mdsalNode) {
        String identifier = NodeUtils.getId(mdsalNode.getId().getValue());
        org.opendaylight.controller.sal.core.Node node = null;

        try {
            node = new org.opendaylight.controller.sal.core.Node("OVS", identifier);
        } catch (ConstructionException e) {
            LOG.error("Failed to allocate sal Node", e);
        }

        return node;
    }

    public static List<org.opendaylight.controller.sal.core.Node> getSalNodes (List<Node> mdsalNodes) {
        List<org.opendaylight.controller.sal.core.Node> nodes = new ArrayList<>();

        for (Node mdsalNode : mdsalNodes) {
            nodes.add(NodeUtils.getSalNode(mdsalNode));
        }
        return nodes;
    }
}
