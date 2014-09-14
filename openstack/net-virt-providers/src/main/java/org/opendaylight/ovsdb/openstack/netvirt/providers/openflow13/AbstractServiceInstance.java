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

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;

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
    private static final Logger logger = LoggerFactory.getLogger(AbstractServiceInstance.class);
    public static final String OPENFLOW = "openflow:";
    // OSGi Services that we are dependent on.
    private volatile MdsalConsumer mdsalConsumer;
    private volatile PipelineOrchestrator orchestrator;

    // Concrete Service that this AbstractServiceInstance represent
    private Service service;

    public AbstractServiceInstance (Service service) {
        this.service = service;
    }

    // Let the Concrete service instance class decide if a Bride is part of the pipeline or not.
    public abstract boolean isBridgeInPipeline (String nodeId);

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

    /**
     * This method returns the required Pipeline Instructions to by used by any matching flows that needs
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
        Preconditions.checkNotNull(mdsalConsumer);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return;
        }

        DataBroker dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            logger.error("ERROR finding reference for DataBroker. Please check MD-SAL support on the Controller.");
            return;
        }

        ReadWriteTransaction modification = dataBroker.newReadWriteTransaction();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodePath = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, nodeBuilder.getKey()).toInstance();

        modification.put(LogicalDatastoreType.CONFIGURATION, nodePath, nodeBuilder.build(), true);
        InstanceIdentifier<Flow> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Table.class,
                new TableKey(flowBuilder.getTableId())).child(Flow.class, flowBuilder.getKey()).build();

        modification.put(LogicalDatastoreType.CONFIGURATION, path1, flowBuilder.build(), true /*createMissingParents*/);

        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            logger.debug("Transaction success for write of Flow "+flowBuilder.getFlowName());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            modification.cancel();
        }
    }

    protected void removeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        Preconditions.checkNotNull(mdsalConsumer);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service.");
            return;
        }

        DataBroker dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            logger.error("ERROR finding reference for DataBroker. Please check MD-SAL support on the Controller.");
            return;
        }

        WriteTransaction modification = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Flow> path1 = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                               .rev130819.nodes.Node.class, nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class).child(Table.class,
                                                           new TableKey(flowBuilder.getTableId())).child(Flow.class, flowBuilder.getKey()).build();
        //modification.delete(LogicalDatastoreType.OPERATIONAL, nodeBuilderToInstanceId(nodeBuilder));
        //modification.delete(LogicalDatastoreType.OPERATIONAL, path1);
        //modification.delete(LogicalDatastoreType.CONFIGURATION, nodeBuilderToInstanceId(nodeBuilder));
        modification.delete(LogicalDatastoreType.CONFIGURATION, path1);

        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            logger.debug("Transaction success for deletion of Flow "+flowBuilder.getFlowName());
        } catch (InterruptedException|ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public Flow getFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        Preconditions.checkNotNull(mdsalConsumer);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return null;
        }

        DataBroker dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            logger.error("ERROR finding reference for DataBroker. Please check MD-SAL support on the Controller.");
            return null;
        }

        InstanceIdentifier<Flow> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Table.class,
                new TableKey(flowBuilder.getTableId())).child(Flow.class, flowBuilder.getKey()).build();

        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        try {
            Optional<Flow> data = readTx.read(LogicalDatastoreType.CONFIGURATION, path1).get();
            if (data.isPresent()) {
                return data.get();
            }
        } catch (InterruptedException|ExecutionException e) {
            logger.error(e.getMessage(), e);
        }

        logger.debug("Cannot find data for Flow " + flowBuilder.getFlowName());
        return null;
    }

    /**
     * Program Default Pipeline Flow.
     *
     * @param nodeId Node on which the default pipeline flow is programmed.
     */
    protected void programDefaultPipelineRule(String nodeId) {
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeId);

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
