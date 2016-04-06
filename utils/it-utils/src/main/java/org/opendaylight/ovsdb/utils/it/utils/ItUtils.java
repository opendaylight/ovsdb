/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.it.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.NotifyingDataChangeListener;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains various utility methods used in OVSDB integration tests (IT).
 */
public class ItUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ItUtils.class);
    MdsalUtils mdsalUtils;
    SouthboundUtils southboundUtils;
    DataBroker dataBroker;
    PipelineOrchestrator pipelineOrchestrator;

    /**
     * Create a new ItUtils instance
     * @param dataBroker  md-sal data broker
     */
    public ItUtils(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        mdsalUtils = new MdsalUtils(dataBroker);
        southboundUtils = new SouthboundUtils(mdsalUtils);
        pipelineOrchestrator =
                (PipelineOrchestrator) ServiceHelper.getGlobalInstance(PipelineOrchestrator.class, this);
    }

    /**
     * Get a NodeInfo instance initialized with this ItUtil's DataBroker
     * @param connectionInfo ConnectionInfo for the OVSDB server
     * @param waitList For tracking outstanding md-sal events notifications
     * @return
     */
    public NodeInfo createNodeInfo(ConnectionInfo connectionInfo, List<NotifyingDataChangeListener> waitList) {
        return new NodeInfo(connectionInfo, this, waitList);
    }

    /**
     * Checks whether the OVSDB controller is connected. This method will retry 10 times and will through an
     * AssertionError for any number of unexpected states.
     * @param connectionInfo
     * @return true if connected
     * @throws InterruptedException
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
                controllerEntry = bridge.getControllerEntry().iterator().next();
                assertEquals(controllerTarget, controllerEntry.getTarget().getValue());
                if (controllerEntry.isIsConnected()) {
                    LOG.info("isControllerConnected exit: true {}", controllerTarget);
                    return true;
                }
            }
            Thread.sleep(1000);
        }
        LOG.info("isControllerConnected exit: false {}", controllerTarget);
        return false;
    }

    public static DataBroker getDatabroker(BindingAwareBroker.ProviderContext providerContext) {
        DataBroker dataBroker = providerContext.getSALService(DataBroker.class);
        assertNotNull("dataBroker should not be null", dataBroker);
        return dataBroker;
    }

    public Boolean getNetvirtTopology() {
        LOG.info("getNetvirtTopology: looking for {}...", ItConstants.NETVIRT_TOPOLOGY_ID);
        Boolean found = false;
        final TopologyId topologyId = new TopologyId(new Uri(ItConstants.NETVIRT_TOPOLOGY_ID));
        InstanceIdentifier<Topology> path =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, new TopologyKey(topologyId));
        for (int i = 0; i < 60; i++) {
            Topology topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, path);
            if (topology != null) {
                LOG.info("getNetvirtTopology: found {}...", ItConstants.NETVIRT_TOPOLOGY_ID);
                found = true;
                break;
            } else {
                LOG.info("getNetvirtTopology: still looking (try {})...", i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while waiting for {}", ItConstants.NETVIRT_TOPOLOGY_ID, e);
                }
            }
        }
        return found;
    }

    /**
     * Verify that the given flow was installed in a table. This method will wait 10 seconds for the flows
     * to appear in each of the md-sal CONFIGURATION and OPERATIONAL data stores
     * @param datapathId
     * @param flowId The "name" of the flow, e.g., "TunnelFloodOut_100"
     * @param table
     * @throws InterruptedException
     */
    public void verifyFlow(long datapathId, String flowId, short table) throws InterruptedException {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder nodeBuilder =
                FlowUtils.createNodeBuilder(datapathId);
        FlowBuilder flowBuilder =
                FlowUtils.initFlowBuilder(new FlowBuilder(), flowId, table);
        InstanceIdentifier<Flow> iid = FlowUtils.createFlowPath(flowBuilder, nodeBuilder);

        NotifyingDataChangeListener waitForIt = new NotifyingDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                iid, null);
        waitForIt.registerDataChangeListener(dataBroker);
        waitForIt.waitForCreation(10000);

        Flow flow = FlowUtils.getFlow(flowBuilder, nodeBuilder, dataBroker.newReadOnlyTransaction(), LogicalDatastoreType.CONFIGURATION);
        assertNotNull("Could not find flow in config: " + flowBuilder.build() + "--" + nodeBuilder.build(), flow);

        waitForIt = new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid, null);
        waitForIt.registerDataChangeListener(dataBroker);
        waitForIt.waitForCreation(10000);

        flow = FlowUtils.getFlow(flowBuilder, nodeBuilder, dataBroker.newReadOnlyTransaction(), LogicalDatastoreType.OPERATIONAL);
        assertNotNull("Could not find flow in operational: " + flowBuilder.build() + "--" + nodeBuilder.build(),
                flow);
    }

    /**
     * Convenience version of verifyFlow that takes a Service object instead of the table number
     * @see ItUtils#verifyFlow(long, String, short)
     * @throws InterruptedException
     */
    public void verifyFlow(long datapathId, String flowId, Service service) throws InterruptedException {
        verifyFlow(datapathId, flowId, pipelineOrchestrator.getTable(service));
    }
}
