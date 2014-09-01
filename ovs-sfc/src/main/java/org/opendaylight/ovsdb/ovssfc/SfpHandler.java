/*
* Copyright (C) 2014 Red Hat, Inc.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*
* Authors : Sam Hague
*/
package org.opendaylight.ovsdb.ovssfc;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
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

import java.util.List;
import java.util.concurrent.ExecutionException;

public class SfpHandler {
    private static final Logger logger = LoggerFactory.getLogger(SfpHandler.class);
    private OvsSfcProvider ovsSfcProvider = OvsSfcProvider.getOvsSfcProvider();
    private static final short TABLE_0_DEFAULT_INGRESS = 0;
    private static final short TABLE_1_CLASSIFIER = 30;
    private static final short TABLE_2_NEXTHOP = 31;
    private static final String OPENFLOW = "openflow:";
    private int vlan;

    public int getVlan () {
        return vlan;
    }

    public void setVlan (int vlan) {
        this.vlan = vlan;
    }

    void processSfp (SfcEvent.Action action, ServiceFunctionPath serviceFunctionPath) {
        logger.trace("\nOVSSFC Enter: {}, action: {}\n   sfp: {}",
                Thread.currentThread().getStackTrace()[1],
                action.toString(),
                serviceFunctionPath.toString());

        switch (action) {
            case CREATE:
            case UPDATE:
                sfpUpdate(serviceFunctionPath);
                break;
            case DELETE:
                break;
            default:
                break;
        }

        logger.trace("\nOVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }

    void processSfps (SfcEvent.Action action, ServiceFunctionPaths serviceFunctionPaths) {
        logger.trace("\nOVSSFC Enter: {}, action: {}\n   sfps: {}",
                Thread.currentThread().getStackTrace()[1],
                action.toString(),
                serviceFunctionPaths.toString());

        switch (action) {
        case CREATE:
        case UPDATE:
            break;
        case DELETE:
            break;
        default:
            break;
        }

        logger.trace("\nOVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }

    /*
     * Get the ingress ssf. This sff will take the ingress acl flows.
     * Get the acl.
     * Get the system-id from the sff to find the ovs node.
     * Program flows.
     *
     */
    private void sfpUpdate (ServiceFunctionPath serviceFunctionPath) {
        logger.trace("\nOVSSFC {}\n Building SFP {}",
                Thread.currentThread().getStackTrace()[1],
                serviceFunctionPath.getName());

        //AccessListEntries accessListEntries = ovsSfcProvider.aclUtils.getAccessList(serviceFunctionPath.getName());
        //logger.trace("\n   acl: {}", accessListEntries);

        String serviceFunctionForwarderName;
        ServiceFunctionForwarder serviceFunctionForwarder = null;
        Short startingIndex = serviceFunctionPath.getStartingIndex();
        List<ServicePathHop> servicePathHopList = serviceFunctionPath.getServicePathHop();
        for (ServicePathHop servicePathHop : servicePathHopList) {
            logger.trace("\n   sph: {}", servicePathHop);

            serviceFunctionForwarderName = servicePathHop.getServiceFunctionForwarder();
            serviceFunctionForwarder = ovsSfcProvider.sffUtils.readServiceFunctionForwarder(serviceFunctionForwarderName);
            if (serviceFunctionForwarder != null) {
                String systemId = ovsSfcProvider.sffUtils.getSystemId(serviceFunctionForwarder);
                Node ovsNode = ovsSfcProvider.ovsUtils.getNode(systemId);
                if (ovsNode != null) {
                    if (servicePathHop.getServiceIndex().equals(startingIndex)) {
                        initializeFlowRules(ovsNode,serviceFunctionForwarderName);
                    }
                }
            }
        }
    }

    private NodeBuilder createNodeBuilder (String nodeId) {
        NodeBuilder builder = new NodeBuilder();
        builder.setId(new NodeId(nodeId));
        builder.setKey(new NodeKey(builder.getId()));
        return builder;
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

        CheckedFuture <Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            logger.debug("Transaction success for write of Flow "+flowBuilder.getFlowName());
        } catch (InterruptedException|ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
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

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        // Call the InstructionBuilder Methods Containing Actions
        InstructionUtils.createNormalInstructions(ib);
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
        flowBuilder.setPriority(0);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId((short) 0);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder);
    }

    private void initializeFlowRules (Node ovsNode, String bridgeName) {
        String bridgeUuid = ovsSfcProvider.ovsUtils.getBridgeUUID(ovsNode, bridgeName);
        if (bridgeUuid == null) {
            return;
        }

        Long dpid = ovsSfcProvider.ovsUtils.getDpid(ovsNode, bridgeUuid);

        if (dpid == 0L) {
            logger.debug("Openflow Datapath-ID not set for the bridge in {}", ovsNode);
            return;
        }

        /*
         * Table(0) Rule #1
         * ----------------
         * Match: LLDP (0x88CCL)
         * Action: Packet_In to Controller Reserved Port
         */

        //writeLLDPRule(dpid);
        writeNormalRule(dpid);
    }
}
