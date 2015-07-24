/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.providers.NetvirtProvidersProvider;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Any ServiceInstance class that extends AbstractServiceInstance to be a part of the pipeline
 * have 2 basic requirements : <br>
 * 1. Program a default pipeline flow to take any unmatched traffic to the next table in the pipeline. <br>
 * 2. Get Pipeline Instructions from AbstractServiceInstance (using getMutablePipelineInstructionBuilder) and
 *    use it in any matching flows that needs to be further processed by next service in the pipeline.
 *
 */
public abstract class AbstractServiceInstance {
    public static final String SERVICE_PROPERTY ="serviceProperty";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractServiceInstance.class);
    public static final String OPENFLOW = "openflow:";
    private DataBroker dataBroker = null;
    // OSGi Services that we are dependent on.
    private volatile PipelineOrchestrator orchestrator;
    private volatile Southbound southbound;

    // Concrete Service that this AbstractServiceInstance represents
    private Service service;

    public AbstractServiceInstance (Service service) {
        this.service = service;
        this.dataBroker = NetvirtProvidersProvider.getDataBroker();
    }

    protected void setDependencies(final ServiceReference ref, AbstractServiceInstance serviceInstance) {
        this.orchestrator =
                (PipelineOrchestrator) ServiceHelper.getGlobalInstance(PipelineOrchestrator.class, serviceInstance);
        orchestrator.registerService(ref, serviceInstance);
        this.southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, serviceInstance);
    }

    public boolean isBridgeInPipeline (Node node){
        String bridgeName = southbound.getBridgeName(node);
        if (bridgeName != null && Constants.INTEGRATION_BRIDGE.equals(bridgeName)) {
            return true;
        }
        return false;
    }

    public short getTable() {
        return service.getTable();
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public NodeBuilder createNodeBuilder(String nodeId) {
        NodeBuilder builder = new NodeBuilder();
        builder.setId(new NodeId(nodeId));
        builder.setKey(new NodeKey(builder.getId()));
        return builder;
    }

    private static final InstanceIdentifier<Flow> createFlowPath(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowBuilder.getTableId()))
                .child(Flow.class, flowBuilder.getKey()).build();
    }

    private static final
    InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>
    createNodePath(NodeBuilder nodeBuilder) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        nodeBuilder.getKey()).build();
    }

    /**
     * This method returns the required Pipeline Instructions to by used by any matching flows that need
     * to be further processed by next service in the pipeline.
     *
     * Important to note that this is a convenience method which returns a mutable instructionBuilder which
     * needs to be further adjusted by the concrete ServiceInstance class such as setting the Instruction Order, etc.
     * @return Newly created InstructionBuilder to be used along with other instructions on the main flow
     */
    protected final InstructionBuilder getMutablePipelineInstructionBuilder() {
        Service nextService = orchestrator.getNextServiceInPipeline(service);
        if (nextService != null) {
            return InstructionUtils.createGotoTableInstructions(new InstructionBuilder(), nextService.getTable());
        } else {
            return InstructionUtils.createDropInstructions(new InstructionBuilder());
        }
    }

    protected void writeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        LOG.debug("writeFlow: flowBuilder: {}, nodeBuilder: {}",
                flowBuilder.build(), nodeBuilder.build());
        WriteTransaction modification = dataBroker.newWriteOnlyTransaction();
        modification.put(LogicalDatastoreType.CONFIGURATION, createNodePath(nodeBuilder),
                nodeBuilder.build(), true /*createMissingParents*/);
        modification.put(LogicalDatastoreType.CONFIGURATION, createFlowPath(flowBuilder, nodeBuilder),
                flowBuilder.build(), true /*createMissingParents*/);

        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            LOG.debug("Transaction success for write of Flow {}", flowBuilder.getFlowName());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            modification.cancel();
        }
    }

    protected void removeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        WriteTransaction modification = dataBroker.newWriteOnlyTransaction();
        modification.delete(LogicalDatastoreType.CONFIGURATION, createFlowPath(flowBuilder, nodeBuilder));

        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            LOG.debug("Transaction success for deletion of Flow {}", flowBuilder.getFlowName());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            modification.cancel();
        }
    }

    public Flow getFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        try {
            Optional<Flow> data =
                    readTx.read(LogicalDatastoreType.CONFIGURATION, createFlowPath(flowBuilder, nodeBuilder)).get();
            if (data.isPresent()) {
                return data.get();
            }
        } catch (InterruptedException|ExecutionException e) {
            LOG.error(e.getMessage(), e);
        }

        LOG.debug("Cannot find data for Flow {}", flowBuilder.getFlowName());
        return null;
    }

    public org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
    getOpenFlowNode(String nodeId) {

        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        try {
            Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> data =
                    readTx.read(LogicalDatastoreType.OPERATIONAL, createNodePath(createNodeBuilder(nodeId))).get();
            if (data.isPresent()) {
                return data.get();
            }
        } catch (InterruptedException|ExecutionException e) {
            LOG.error(e.getMessage(), e);
        }

        LOG.debug("Cannot find data for Node {}", nodeId);
        return null;
    }

    private Long getDpid(Node node) {
        Long dpid = 0L;
        dpid = southbound.getDataPathId(node);
        if (dpid == 0) {
            LOG.warn("getDpid: dpid not found: {}", node);
        }
        return dpid;
    }

    /**
     * Program Default Pipeline Flow.
     *
     * @param node on which the default pipeline flow is programmed.
     */
    protected void programDefaultPipelineRule(Node node) {
        if (!isBridgeInPipeline(node)) {
            //LOG.trace("Bridge is not in pipeline {} ", node);
            return;
        }
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();
        Long dpid = getDpid(node);
        if (dpid == 0L) {
            LOG.info("could not find dpid: {}", node.getNodeId());
            return;
        }
        String nodeName = OPENFLOW + getDpid(node);
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);

        // Create the OF Actions and Instructions
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        // Call the InstructionBuilder Methods Containing Actions
        InstructionBuilder ib = this.getMutablePipelineInstructionBuilder();
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        String flowId = "DEFAULT_PIPELINE_FLOW_"+service.getTable();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(0);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(service.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder);
    }
}
