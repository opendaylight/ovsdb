/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.ovssfc;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
//import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class Flows {
    private static final Logger logger = LoggerFactory.getLogger(Flows.class);
    private OvsSfcProvider ovsSfcProvider = OvsSfcProvider.getOvsSfcProvider();
    private static final short TABLE_0_DEFAULT_INGRESS = 0;
    private static final short TABLE_1_CLASSIFIER = 5;
    private static final short TABLE_2_NEXTHOP = 6;
    private static final short SFC_PRIORITY = 100;
    private static final String OPENFLOW = "openflow:";

    private NodeBuilder createNodeBuilder (String nodeId) {
        NodeBuilder builder = new NodeBuilder();
        builder.setId(new NodeId(nodeId));
        builder.setKey(new NodeKey(builder.getId()));
        return builder;
    }

    private void removeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        DataBroker dataBroker = OvsSfcProvider.getOvsSfcProvider().getDataBroker();
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

    private void writeFlow (FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        DataBroker dataBroker = OvsSfcProvider.getOvsSfcProvider().getDataBroker();
        if (dataBroker == null) {
            logger.error("ERROR finding reference for DataBroker. Please check MD-SAL support on the Controller.");
            return;
        }

        ReadWriteTransaction modification = dataBroker.newReadWriteTransaction();
        InstanceIdentifier<Flow> path1 = InstanceIdentifier
                .builder(Nodes.class).child(
                        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class).child(Table.class,
                        new TableKey(flowBuilder.getTableId()))
                .child(Flow.class, flowBuilder.getKey()).build();

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

    private void writeLLDPRule(Long dpidLong) {

        String nodeName = OPENFLOW + dpidLong;
        EtherType etherType = new EtherType(0x88CCL);

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(MatchUtils.createEtherTypeMatch(matchBuilder, etherType).build());

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        // Call the InstructionBuilder Methods Containing Actions
        InstructionUtils.createSendToControllerInstructions(nodeName, ib);
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        String flowId = "LLDP";
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setPriority(10);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId((short) 0);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder);
    }

    /*
     * Create a NORMAL Table Miss Flow Rule
     * Match: any
     * Action: forward to NORMAL pipeline
     */
    private void writeNormalRule (Long dpidLong) {
        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        //flowBuilder.setMatch(MatchUtils.createInPortMatch(matchBuilder, dpidLong, 1L).build());

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        // Call the InstructionBuilder Methods Containing Actions
        InstructionUtils.createNormalInstructions(nodeName, ib);
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        String flowId = "NORMAL";
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(10);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(TABLE_0_DEFAULT_INGRESS);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder);
    }

    public void initializeFlowRules (Long dpid) {
        /*
         * Table(0) Rule #1
         * ----------------
         * Match: LLDP (0x88CCL)
         * Action: Packet_In to Controller Reserved Port
         */

        //Uri value = new Uri(OutputPortValues.CONTROLLER.toString());
        //Uri value2 = new Uri(InputPortValues);

        writeLLDPRule(dpid);
        writeNormalRule(dpid);
    }

    public static MatchBuilder createInPortLocalMatch(MatchBuilder matchBuilder, Long dpidLong, String portId) {

        NodeConnectorId ncid = new NodeConnectorId("openflow:" + dpidLong + ":" + portId);
        logger.debug("createInPortMatch() Node Connector ID is - Type=openflow: DPID={} inPort={} ", dpidLong, "LOCAL");
        matchBuilder.setInPort(NodeConnectorId.getDefaultInstance(ncid.getValue()));
        matchBuilder.setInPort(ncid);

        return matchBuilder;
    }

    /*
     * Match: in_port, tcp, tcp_dst
     * Actions: mod_vlan_vid, resubmit(,30)
     */
    public void writeIngressAcl (Long dpid, long inPort, int vlan, boolean write) {
        String nodeName = OPENFLOW + dpid;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        //flowBuilder.setMatch(createInPortLocalMatch(matchBuilder, dpid, OutputPortValues.LOCAL.toString()).build());
        PortNumber portNumber = new PortNumber(80);
        //flowBuilder.setMatch(MatchUtils.createSetDstTcpMatch(matchBuilder, portNumber).build());

        String flowId = "ingressAcl_"+portNumber.getValue();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(100);
        //flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(TABLE_0_DEFAULT_INGRESS);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Call the InstructionBuilder Methods Containing Actions
/*
            // GOTO Instructions Need to be added first to the List
            InstructionUtils.createGotoTableInstructions(ib, (short)5);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());
*/
            // Set VLAN ID Instruction
            InstructionUtils.createSetVlanInstructions(ib, new VlanId(vlan));
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());
            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());

            //ActionUtils.nxResubmitAction(0, (short)6);
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }

    }
}
