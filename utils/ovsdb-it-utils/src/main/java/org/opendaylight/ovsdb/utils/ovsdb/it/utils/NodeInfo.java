/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.ovsdb.it.utils;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
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

    private final ConnectionInfo connectionInfo;
    private final InstanceIdentifier<Node> ovsdbIid;
    private final InstanceIdentifier<Node> bridgeIid;
    private final List<NotifyingDataChangeListener> waitList;
    private final OvsdbItUtils itUtils;

    private long datapathId;
    private Node ovsdbNode;
    private Node bridgeNode;
    private NotifyingDataChangeListener ovsdbWaiter;
    private NotifyingDataChangeListener bridgeWaiter;


    /**
     * Create a new NodeInfo object.
     * @param connectionInfo of the OVSDB node
     * @param itUtils OvsdbItUtils instance
     * @param waitList for tracking outstanding md-sal events
     */
    NodeInfo(final ConnectionInfo connectionInfo, final OvsdbItUtils itUtils,
             final List<NotifyingDataChangeListener> waitList) {
        this.connectionInfo = connectionInfo;
        this.itUtils = itUtils;
        this.waitList = waitList;
        ovsdbIid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        bridgeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo, INTEGRATION_BRIDGE_NAME);
    }

    private void addWaiters() {
        ovsdbWaiter = new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL,
            NotifyingDataChangeListener.BIT_CREATE, ovsdbIid, waitList);
        ovsdbWaiter.registerDataChangeListener(itUtils.dataBroker);
        bridgeWaiter = new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL,
            NotifyingDataChangeListener.BIT_CREATE, bridgeIid, waitList);
        bridgeWaiter.registerDataChangeListener(itUtils.dataBroker);
    }

    private void closeWaiters() throws Exception {
        ovsdbWaiter.close();
        bridgeWaiter.close();
    }

    /**
     * Connect to the OVSDB node, wait for the connection to be established and for the integration bridge
     * to be successfully created. Contains assertions for unexpected states
     * @throws InterruptedException if interrupted while waiting for connection
     */
    public void connect() throws Exception {
        addWaiters();

        assertNotNull("connection failed", itUtils.southboundUtils.addOvsdbNode(connectionInfo, 0));

        ovsdbWaiter.waitForCreation();
        ovsdbNode = itUtils.southboundUtils.getOvsdbNode(connectionInfo);
        assertNotNull("node is not connected", ovsdbNode);

        bridgeWaiter.waitForCreation();
        assertTrue("Controller " + SouthboundUtils.connectionInfoToString(connectionInfo)
                + " is not connected", itUtils.isControllerConnected(connectionInfo));

        bridgeNode = itUtils.southboundUtils.getBridgeNode(ovsdbNode, INTEGRATION_BRIDGE_NAME);
        assertNotNull("bridge " + INTEGRATION_BRIDGE_NAME + " was not found", bridgeNode);
        datapathId = itUtils.southboundUtils.getDataPathId(bridgeNode);
        String datapathIdString = itUtils.southboundUtils.getDatapathId(bridgeNode);
        LOG.info("NodeInfo.connect: bridgeNode: {}, datapathId: {} - {}", bridgeNode, datapathIdString, datapathId);
        assertNotEquals("datapathId was not found", datapathId, 0);
    }

    /**
     * Remove integration bridge and teardown connection. Contains assertions for unexpected states.
     * @throws InterruptedException if interrupted while waiting for disconnect to complete
     */
    public void disconnect() throws Exception {
        ovsdbWaiter.setMask(NotifyingDataChangeListener.BIT_DELETE);
        bridgeWaiter.setMask(NotifyingDataChangeListener.BIT_DELETE);
        assertTrue(itUtils.southboundUtils.deleteBridge(connectionInfo, INTEGRATION_BRIDGE_NAME, 0));
        bridgeWaiter.waitForDeletion();
        assertNull("Bridge should not be found", itUtils.mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeIid));
        assertTrue(itUtils.southboundUtils.disconnectOvsdbNode(connectionInfo, 0));
        ovsdbWaiter.waitForDeletion();
        assertNull("Ovsdb node should not be found",
                itUtils.mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, ovsdbIid));
        closeWaiters();
    }
}
