/**
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.plugin.shell;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;

@Command(scope = "ovs", name = "printCache", description="Prints OVSDB Cache")
public class PrintCache extends OsgiCommandSupport{
    private OvsdbInventoryService ovsdbInventory;

    @Argument(index=0, name="nodeName", description="Node Name", required=true, multiValued=false)
    String nodeName = null;

    @Override
    protected Object doExecute() throws Exception {
        Node node = Node.fromString(nodeName);
        ovsdbInventory.printCache(node);
        return null;
    }

    public void setOvsdbInventory(OvsdbInventoryService inventoryService){
        this.ovsdbInventory = inventoryService;
    }
}
