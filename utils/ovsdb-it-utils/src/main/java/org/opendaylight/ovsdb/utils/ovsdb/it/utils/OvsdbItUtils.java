/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.ovsdb.it.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.NotifyingDataChangeListener;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains various utility methods used in OVSDB integration tests (IT).
 */
public class OvsdbItUtils {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbItUtils.class);
    MdsalUtils mdsalUtils;
    SouthboundUtils southboundUtils;
    DataBroker dataBroker;

    /**
     * Create a new OvsdbItUtils instance.
     * @param dataBroker  md-sal data broker
     */
    public OvsdbItUtils(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        mdsalUtils = new MdsalUtils(dataBroker);
        southboundUtils = new SouthboundUtils(mdsalUtils);
    }

    /**
     * Get a NodeInfo instance initialized with this ItUtil's DataBroker.
     * @param connectionInfo ConnectionInfo for the OVSDB server
     * @param waitList For tracking outstanding md-sal events notifications
     * @return a new NodeInfo object
     */
    public NodeInfo createNodeInfo(ConnectionInfo connectionInfo,
            List<NotifyingDataChangeListener> waitList) {
        return new NodeInfo(connectionInfo, this, waitList);
    }

    /**
     * Checks whether the OVSDB controller is connected. This method will retry 10 times and will through an
     * AssertionError for any number of unexpected states.
     * @param connectionInfo where to connect to
     * @return true if connected
     * @throws InterruptedException if interrupted while waiting for connection to appear
     */
    public boolean isControllerConnected(ConnectionInfo connectionInfo) throws InterruptedException {
        LOG.info("isControllerConnected enter");
        ControllerEntry controllerEntry;
        Node ovsdbNode = southboundUtils.getOvsdbNode(connectionInfo);
        assertNotNull("ovsdb node not found", ovsdbNode);

        String controllerTarget = southboundUtils.getControllersFromOvsdbNode(ovsdbNode).get(0);
        assertNotNull("Failed to get controller target", controllerTarget);

        for (int i = 0; i < 10; i++) {
            LOG.info("isControllerConnected try {}: looking for controller: {}", i, controllerTarget);
            OvsdbBridgeAugmentation bridge =
                    southboundUtils.getBridge(connectionInfo, "br-int");
            if (bridge != null && bridge.getControllerEntry() != null) {
                controllerEntry = bridge.getControllerEntry().values().iterator().next();
                assertEquals(controllerTarget, controllerEntry.getTarget().getValue());
                if (controllerEntry.getIsConnected()) {
                    LOG.info("isControllerConnected exit: true {}", controllerTarget);
                    return true;
                }
            }
            Thread.sleep(1000);
        }
        LOG.info("isControllerConnected exit: false {}", controllerTarget);
        return false;
    }
}
