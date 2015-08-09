/*
 * Copyright (c) 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.mdsal.node;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;

public class NodeUtils {

    public static String getId (String identifier) {
        String id = identifier;

        String[] pair = identifier.split("\\|");
        if ((pair.length > 1) && (pair[0].equals("OVS"))) {
            id = pair[1];
        }
        return id;
    }

    public static Node getOpenFlowNode (String identifier) {
        NodeId nodeId = new NodeId(identifier);
        NodeKey nodeKey = new NodeKey(nodeId);

        return new NodeBuilder()
                .setId(nodeId)
                .setKey(nodeKey)
                .build();
    }
}
