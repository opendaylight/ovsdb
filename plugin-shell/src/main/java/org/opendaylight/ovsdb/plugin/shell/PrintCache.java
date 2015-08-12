/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.plugin.shell;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;

@Command(scope = "ovs", name = "printCache", description="Prints OVSDB Cache")
public class PrintCache extends OsgiCommandSupport{
    private OvsdbInventoryService ovsdbInventory;

    @Argument(index=0, name="nodeName", description="Node Name", required=true, multiValued=false)
    String nodeName = null;

    @Override
    protected Object doExecute() throws Exception {
        NodeId nodeId = new NodeId(nodeName);
        NodeKey nodeKey = new NodeKey(nodeId);
        Node node = new NodeBuilder()
                .setId(nodeId)
                .setKey(nodeKey)
                .build();
        ovsdbInventory.printCache(node);
        return null;
    }

    public void setOvsdbInventory(OvsdbInventoryService inventoryService){
        this.ovsdbInventory = inventoryService;
    }
}
