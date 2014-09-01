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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

/**
 * Any ServiceInstance class that extends AbstractServiceInstance to be a part of the pipeline
 * have 2 basic requirements : <br>
 * 1. Program a default pipeline flow to take any unmatched traffic to the next table in the pipeline. <br>
 * 2. Get Pipeline Instructions from AbstractServiceInstance (using getMutablePipelineInstructionBuilder) and
 *    use it in any matching flows that needs to be further processed by next service in the pipeline.
 *
 */
public abstract class AbstractServiceInstance implements OpendaylightInventoryListener, Runnable, TransactionChainListener {
    public static final String SERVICE_PROPERTY ="serviceProperty";
    private static final Logger logger = LoggerFactory.getLogger(AbstractServiceInstance.class);

    // OSGi Services that we are dependent on.
    private volatile MdsalConsumer mdsalConsumer;
    private volatile PipelineOrchestrator orchestrator;

    // Concrete Service that this AbstractServiceInstance represent
    private Service service;

    private BindingTransactionChain txChain;

    // Process Notification in its own thread
    Thread thread = null;
    private final BlockingQueue<String> queue = new LinkedBlockingDeque<>();

    public AbstractServiceInstance (Service service) {
        this.service = service;
    }

    // Let the Concrete service instance class decide if a Bride is part of the pipeline or not.
    public abstract boolean isBridgeInPipeline (String nodeId);

    public int getTable() {
        return service.getTable();
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public void start() {
        // Register for OpenFlow bridge/node Creation notification.
        NotificationProviderService notificationService = mdsalConsumer.getNotificationService();
        if (notificationService != null) {
            notificationService.registerNotificationListener(this);
        }
        this.txChain =  mdsalConsumer.getDataBroker().createTransactionChain(this);

        // Never block a Notification thread. Process the notification in its own Thread.
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("AbstractServiceInstance-"+service.toString());
        thread.start();
    }

    protected NodeBuilder createNodeBuilder(String nodeId) {
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
        InstanceIdentifier<Flow> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Table.class,
                new TableKey(flowBuilder.getTableId())).child(Flow.class, flowBuilder.getKey()).build();

        //modification.put(LogicalDatastoreType.OPERATIONAL, path1, flowBuilder.build());
        modification.put(LogicalDatastoreType.CONFIGURATION, path1, flowBuilder.build(), true /*createMissingParents*/);


        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            logger.debug("Transaction success for write of Flow "+flowBuilder.getFlowName());
        } catch (InterruptedException|ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
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

        String flowId = "DEFAULT_PIPELINE_FLOW";
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

    @Override
    public void onNodeConnectorRemoved(NodeConnectorRemoved nodeConector) {
    }

    @Override
    public void onNodeConnectorUpdated(NodeConnectorUpdated nodeConnector) {
    }

    @Override
    public void onNodeRemoved(NodeRemoved node) {
    }


    @Override
    public void run() {
        try {
            for (; ; ) {
                String nodeId = queue.take();
                this.programDefaultPipelineRule(nodeId);
            }
        } catch (InterruptedException e) {
            logger.warn("Processing interrupted, terminating", e);
        }

        while (!queue.isEmpty()) {
            queue.poll();
        }

    }

    void enqueue(final String nodeId) {
        try {
            queue.put(nodeId);
        } catch (InterruptedException e) {
            logger.warn("Failed to enqueue operation {}", nodeId, e);
        }
    }

    /**
     * Process the Node update notification. Check for Openflow node and make sure if the bridge is part of the Pipeline before
     * programming the Pipeline specific flows.
     */
    @Override
    public void onNodeUpdated(NodeUpdated nodeUpdated) {
        NodeRef ref = nodeUpdated.getNodeRef();
        InstanceIdentifier<Node> identifier = (InstanceIdentifier<Node>) ref.getValue();
        logger.info("GOT NOTIFICATION FOR "+identifier.toString());
        final NodeKey key = identifier.firstKeyOf(Node.class, NodeKey.class);
        final String nodeId = key.getId().getValue();
        if (!this.isBridgeInPipeline(nodeId)) {
            logger.debug("Bridge {} is not in pipeline", nodeId);
            return;
        }
        if (key != null && key.getId().getValue().contains("openflow")) {
            InstanceIdentifierBuilder<Node> builder = ((InstanceIdentifier<Node>) ref.getValue()).builder();
            InstanceIdentifierBuilder<FlowCapableNode> augmentation = builder.augmentation(FlowCapableNode.class);
            final InstanceIdentifier<FlowCapableNode> path = augmentation.build();
            CheckedFuture readFuture = txChain.newReadWriteTransaction().read(LogicalDatastoreType.OPERATIONAL, path);
            Futures.addCallback(readFuture, new FutureCallback<Optional<? extends DataObject>>() {
                @Override
                public void onSuccess(Optional<? extends DataObject> optional) {
                    if (!optional.isPresent()) {
                        enqueue(nodeId);
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    logger.debug(String.format("Can't retrieve node data for node %s. Writing node data with table0.", nodeId));
                    enqueue(nodeId);
                }
            });
        }
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
            final Throwable cause) {
        logger.error("Failed to export Flow Capable Inventory, Transaction {} failed.",transaction.getIdentifier(),cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
    }
}
