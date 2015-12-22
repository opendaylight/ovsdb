/*
 * Copyright (c) 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.mdsal.openflow;

import com.google.common.base.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowUtils {
    private static final Logger LOG = LoggerFactory.getLogger(FlowUtils.class);
    private static final String OPENFLOW = "openflow";
    public final static long REG_VALUE_FROM_LOCAL = 0x1L;
    public final static long REG_VALUE_FROM_REMOTE = 0x2L;
    public static final Class<? extends NxmNxReg> REG_FIELD = NxmNxReg0.class;
    public static final int ARP_OP_REQUEST = 0x1;
    public static final int ARP_OP_REPLY = 0x2;


    public static String getNodeName(long dpidLong) {
        return OPENFLOW + ":" + dpidLong;
    }

    public static NodeConnectorId getNodeConnectorId(long ofPort, String nodeName) {
        return new NodeConnectorId(nodeName + ":" + ofPort);
    }

    public static NodeConnectorId getSpecialNodeConnectorId(long dpidLong, String portName) {
        return new NodeConnectorId(getNodeName(dpidLong) + ":" + portName);
    }

    public static NodeConnectorId getNodeConnectorId(long dpidLong, long ofPort) {
        return getNodeConnectorId(ofPort, getNodeName(dpidLong));
    }

    public static NodeBuilder createNodeBuilder(String nodeId) {
        NodeBuilder builder = new NodeBuilder();
        builder.setId(new NodeId(nodeId));
        builder.setKey(new NodeKey(builder.getId()));
        return builder;
    }

    public static NodeBuilder createNodeBuilder(long dpidLong) {
        return createNodeBuilder(getNodeName(dpidLong));
    }

    public static InstanceIdentifier<Flow> createFlowPath(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowBuilder.getTableId()))
                .child(Flow.class, flowBuilder.getKey()).build();
    }

    public static InstanceIdentifier<Node> createNodePath(NodeBuilder nodeBuilder) {
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeBuilder.getKey()).build();
    }

    public static Flow getFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder,
                               ReadOnlyTransaction readTx, final LogicalDatastoreType store) {
        try {
            Optional<Flow> data = readTx.read(store, createFlowPath(flowBuilder, nodeBuilder)).get();
            if (data.isPresent()) {
                return data.get();
            }
        } catch (InterruptedException|ExecutionException e) {
            LOG.error(e.getMessage(), e);
        }

        LOG.info("Cannot find data for Flow {} in {}", flowBuilder.getFlowName(), store);
        return null;
    }

    public static FlowBuilder getPipelineFlow(short table, short gotoTable) {
        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(new MatchBuilder().build());

        String flowName = "DEFAULT_PIPELINE_FLOW_" + table;
        return initFlowBuilder(flowBuilder, flowName, table)
                .setPriority(0);
    }

    /**
     * Sets up common defaults for the given flow builder: a flow identifier and key based on the given flow name,
     * strict, no barrier, the given table identifier, no hard timeout and no idle timeout.
     *
     * @param flowBuilder The flow builder.
     * @param flowName The flow name.
     * @param table The table.
     * @return The flow builder.
     */
    public static FlowBuilder initFlowBuilder(FlowBuilder flowBuilder, String flowName, short table) {
        final FlowId flowId = new FlowId(flowName);
        flowBuilder
                .setId(flowId)
                .setStrict(false)
                .setBarrier(false)
                .setTableId(table)
                .setKey(new FlowKey(flowId))
                .setFlowName(flowName)
                .setHardTimeout(0)
                .setIdleTimeout(0);
        return flowBuilder;
    }
}
