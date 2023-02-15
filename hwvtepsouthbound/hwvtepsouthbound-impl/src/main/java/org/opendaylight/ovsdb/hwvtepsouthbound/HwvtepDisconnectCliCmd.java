/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.concurrent.ExecutionException;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Service
@Command(scope = "hwvtep", name = "disconnect", description = "Disconnect a node")
public class HwvtepDisconnectCliCmd implements Action {
    public static final TopologyId HWVTEP_TOPOLOGY_ID = new TopologyId(new Uri("hwvtep:1"));

    @Reference
    private DataBroker dataBroker;

    @Option(name = "-nodeid", description = "Node Id", required = false, multiValued = false)
    private String nodeid;

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    @Override
    public Object execute() throws InterruptedException, ExecutionException {
        final var nodeKey = new NodeKey(new NodeId(new Uri(nodeid + "/disconnect")));

        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(HWVTEP_TOPOLOGY_ID))
            .child(Node.class, nodeKey)
            .build(),
            new NodeBuilder().withKey(nodeKey).build());
        tx.commit().get();
        System.out.println("Successfully disconnected " + nodeid);
        return "";
    }
}
