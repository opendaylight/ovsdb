/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.it;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Assert;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.utils.NotifyingDataChangeListener;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for connections to an OVSDB node. Contains various info for the node
 * as public data members.
 */
public class NodeInfo {
    private static final Logger LOG = LoggerFactory.getLogger(NodeInfo.class);
    public static final String INTEGRATION_BRIDGE_NAME = "br-int";

    private ConnectionInfo connectionInfo;
    private InstanceIdentifier<Node> ovsdbIid;
    InstanceIdentifier<Node> bridgeIid;
    public long datapathId;
    public Node ovsdbNode;
    public Node bridgeNode;
    NotifyingDataChangeListener ovsdbWaiter;
    NotifyingDataChangeListener bridgeWaiter;
    List<NotifyingDataChangeListener> waitList;
    ItUtils itUtils;

    /**
     * Create a new NodeInfo object
     * @param connectionInfo of the OVSDB node
     * @param itUtils ItUtils instance
     * @param waitList for tracking outstanding md-sal events
     */
    NodeInfo(ConnectionInfo connectionInfo, ItUtils itUtils, List<NotifyingDataChangeListener> waitList) {
        this.connectionInfo = connectionInfo;
        this.itUtils = itUtils;
        this.waitList = waitList;
        ovsdbIid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        bridgeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo, INTEGRATION_BRIDGE_NAME);
    }

    /**
     * Connect to the OVSDB node, wait for the connection to be established and for the integration bridge
     * to be successfully created. Contains assertions for unexpected states
     * @throws InterruptedException
     */
    public void connect() throws InterruptedException {
        ovsdbWaiter = new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, ovsdbIid, waitList);
        ovsdbWaiter.registerDataChangeListener(itUtils.dataBroker);
        bridgeWaiter = new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, bridgeIid, waitList);
        bridgeWaiter.registerDataChangeListener(itUtils.dataBroker);

        Assert.assertNotNull("connection failed", itUtils.southboundUtils.addOvsdbNode(connectionInfo, 0));

        ovsdbWaiter.waitForCreation();
        ovsdbNode = itUtils.southboundUtils.getOvsdbNode(connectionInfo);
        Assert.assertNotNull("node is not connected", ovsdbNode);

        bridgeWaiter.waitForCreation();
        Assert.assertTrue("Controller " + SouthboundUtils.connectionInfoToString(connectionInfo)
                + " is not connected", itUtils.isControllerConnected(connectionInfo));

        bridgeNode = itUtils.southbound.getBridgeNode(ovsdbNode, INTEGRATION_BRIDGE_NAME);
        Assert.assertNotNull("bridge " + INTEGRATION_BRIDGE_NAME + " was not found", bridgeNode);
        datapathId = itUtils.southbound.getDataPathId(bridgeNode);
        String datapathIdString = itUtils.southbound.getDatapathId(bridgeNode);
        LOG.info("testNetVirt: bridgeNode: {}, datapathId: {} - {}", bridgeNode, datapathIdString, datapathId);
        Assert.assertNotEquals("datapathId was not found", datapathId, 0);
    }

    /**
     * Remove integration bridge and teardown connection. Contains assertions for unexpected states.
     * @throws InterruptedException
     */
    public void disconnect() throws InterruptedException {
        Assert.assertTrue(itUtils.southboundUtils.deleteBridge(connectionInfo, INTEGRATION_BRIDGE_NAME, 0));
        itUtils.southboundUtils.deleteBridge(connectionInfo, INTEGRATION_BRIDGE_NAME, 0);
        bridgeWaiter.waitForDeletion();
        Node bridgeNode = itUtils.mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeIid);
        Assert.assertNull("Bridge should not be found", bridgeNode);
        Assert.assertTrue(itUtils.southboundUtils.disconnectOvsdbNode(connectionInfo, 0));
        itUtils.southboundUtils.disconnectOvsdbNode(connectionInfo, 0);
        ovsdbWaiter.waitForDeletion();
        Node ovsdbNode = itUtils.mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, ovsdbIid);
        Assert.assertNull("Ovsdb node should not be found", ovsdbNode);
    }

}
