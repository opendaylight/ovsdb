/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13;

import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcClassifier {
    private static final Logger LOG = LoggerFactory.getLogger(SfcClassifier.class);
    private DataBroker dataBroker;
    private Southbound southbound;
    private MdsalUtils mdsalUtils;
    public final static long REG_VALUE_FROM_LOCAL = 0x1L;
    public final static long REG_VALUE_FROM_REMOTE = 0x2L;
    public static final Class<? extends NxmNxReg> REG_FIELD = NxmNxReg0.class;
    private static final String OPENFLOW = "openflow:";

    public SfcClassifier(DataBroker dataBroker, Southbound southbound, MdsalUtils mdsalUtils) {
        this.dataBroker = dataBroker;
        this.southbound = southbound;
        this.mdsalUtils = mdsalUtils;
    }

    /*
     * (TABLE:50) EGRESS VM TRAFFIC TOWARDS TEP with NSH header
     * MATCH: Match fields passed through ACL entry
     * INSTRUCTION: SET TUNNELID AND GOTO TABLE TUNNEL TABLE (N)
     * TABLE=0,IN_PORT=2,DL_SRC=00:00:00:00:00:01 \
     * ACTIONS=SET_FIELD:5->TUN_ID,GOTO_TABLE=1"
     */
    public void programSfcClassiferFlows(Long dpidLong, short writeTable, String ruleName, Matches match,
                                         NshUtils nshHeader, long tunnelOfPort, boolean write) {
        String nodeName = OPENFLOW + dpidLong;
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        MatchBuilder matchBuilder = buildMatch(match);
        flowBuilder.setMatch(matchBuilder.build());

        String flowId = "sfcClass_" + ruleName + "_" + nshHeader.getNshNsp();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            List<Action> actionList = getNshAction(nshHeader);
            ActionBuilder ab = new ActionBuilder();

            ab.setAction(ActionUtils.outputAction(new NodeConnectorId(nodeName + ":" + tunnelOfPort)));
            ab.setOrder(actionList.size());
            ab.setKey(new ActionKey(actionList.size()));
            actionList.add(ab.build());

            ApplyActionsBuilder aab = new ApplyActionsBuilder();
            aab.setAction(actionList);

            InstructionBuilder ib = new InstructionBuilder();
            ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            List<Instruction> instructions = Lists.newArrayList();
            instructions.add(ib.build());

            InstructionsBuilder isb = new InstructionsBuilder();
            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    /*
     * (TABLE:50) EGRESS VM TRAFFIC TOWARDS TEP with NSH header
     * MATCH: Match fields passed through ACL entry
     * INSTRUCTION: SET TUNNELID AND GOTO TABLE TUNNEL TABLE (N)
     * TABLE=0,IN_PORT=2,DL_SRC=00:00:00:00:00:01 \
     * ACTIONS=SET_FIELD:5->TUN_ID,GOTO_TABLE=1"
     */
    public void programSfcEgressClassiferFlows(Long dpidLong, short writeTable, String ruleName, Matches match,
                                         NshUtils nshHeader, long destOfPort, boolean write) {
        String nodeName = OPENFLOW + dpidLong;
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        LOG.debug("Processing Egress Flow={} on Node={}, nshHeader={}, destPort={}", ruleName, dpidLong, nshHeader, destOfPort);
        MatchBuilder matchBuilder = buildMatch(match);
        matchBuilder = updateMatch(matchBuilder, nshHeader);
        flowBuilder.setMatch(matchBuilder.build());

        LOG.debug("UpdatedMatch={}", matchBuilder.build());
        String flowId = "sfcClass_" + ruleName + "_egress_" +nshHeader.getNshNsp();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            List<Action> actionList = Lists.newArrayList();;
            ActionBuilder ab = new ActionBuilder();

            ab.setAction(ActionUtils.outputAction(new NodeConnectorId(nodeName + ":" + destOfPort)));
            ab.setOrder(actionList.size());
            ab.setKey(new ActionKey(actionList.size()));
            actionList.add(ab.build());

            ApplyActionsBuilder aab = new ApplyActionsBuilder();
            aab.setAction(actionList);

            InstructionBuilder ib = new InstructionBuilder();
            ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            List<Instruction> instructions = Lists.newArrayList();
            instructions.add(ib.build());

            InstructionsBuilder isb = new InstructionsBuilder();
            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }
    
    private MatchBuilder updateMatch(MatchBuilder match, NshUtils nshHeader) {
        MatchUtils.addNxNsp(match, nshHeader.getNshNsp());
        MatchUtils.addNxNsi(match, nshHeader.getNshNsi());
        MatchUtils.addNxTunIdMatch(match, (int) nshHeader.getNshMetaC2());
        return match;
    }

    private List<Action> getNshAction(NshUtils header) {
        // Build the Actions to Add the NSH Header
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nshC1Load =
                ActionUtils.nxLoadNshc1RegAction(header.getNshMetaC1());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nshC2Load =
                ActionUtils.nxLoadNshc2RegAction(header.getNshMetaC2());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nspLoad =
                ActionUtils.nxSetNspAction(header.getNshNsp());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nsiLoad =
                ActionUtils.nxSetNsiAction(header.getNshNsi());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action loadChainTunVnid =
                ActionUtils.nxLoadTunIdAction(BigInteger.valueOf(header.getNshNsp()), false);
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action loadChainTunDest =
                ActionUtils.nxLoadTunIPv4Action(header.getNshTunIpDst().getValue(), false);

        int count = 0;
        List<Action> actionList = Lists.newArrayList();
      /*  actionList.add(new ActionBuilder().setOrder(count++).setAction(nshC1Load).build());
        actionList.add(new ActionBuilder().setOrder(count++).setAction(nshC2Load).build());
        actionList.add(new ActionBuilder().setOrder(count++).setAction(nspLoad).build());
        actionList.add(new ActionBuilder().setOrder(count++).setAction(nsiLoad).build());
        actionList.add(new ActionBuilder().setOrder(count++).setAction(loadChainTunDest).build());
        actionList.add(new ActionBuilder().setOrder(count++).setAction(loadChainTunVnid).build());*/
        return actionList;
    }

    public void programLocalInPort(Long dpidLong, String segmentationId, Long inPort,
                                   short writeTable, short goToTableId, boolean write) {
        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils.createInPortMatch(matchBuilder, dpidLong, inPort).build());
        String flowId = "sfcIngress_" + segmentationId + "_" + inPort;
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();

            InstructionUtils.createSetTunnelIdInstructions(ib, new BigInteger(segmentationId));
            ApplyActionsCase aac = (ApplyActionsCase) ib.getInstruction();
            List<Action> actionList = aac.getApplyActions().getAction();

            // TODO: Mark the packets as sfc classified?

            ActionBuilder ab = new ActionBuilder();
            ab.setAction(ActionUtils.nxLoadRegAction(new DstNxRegCaseBuilder().setNxReg(REG_FIELD).build(),
                    BigInteger.valueOf(REG_VALUE_FROM_LOCAL)));
            ab.setOrder(1);
            ab.setKey(new ActionKey(1));
            actionList.add(ab.build());

            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Next service GOTO Instructions Need to be appended to the List
            ib = InstructionUtils.createGotoTableInstructions(new InstructionBuilder(), goToTableId);
            ib.setOrder(1);
            ib.setKey(new InstructionKey(1));
            instructions.add(ib.build());

            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

/*
    public InstructionsBuilder buildActions(String ruleName, Actions actions, String datapathId) {
        InstructionBuilder ib = new InstructionBuilder();

        if (actions.getPacketHandling() instanceof Deny) {
            InstructionUtils.createDropInstructions(ib);
        } else if (actions.getPacketHandling() instanceof Permit) {
            //Permit actPermit = (Permit) actions.getPacketHandling();
        } else {
            InstructionUtils.createDropInstructions(ib);
        }

        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();
        instructions.add(ib.build());

        // Call the InstructionBuilder Methods Containing Actions
        ib = this.getMutablePipelineInstructionBuilder();
        ib.setOrder(1);
        ib.setKey(new InstructionKey(1));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);
        return isb;
    }*/

    public MatchBuilder buildMatch(Matches matches) {
        MatchBuilder matchBuilder = new MatchBuilder();

        if (matches.getAceType() instanceof AceIp) {
            AceIp aceIp = (AceIp)matches.getAceType();
            if (aceIp.getAceIpVersion() instanceof AceIpv4) {
                //AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();
                //MatchUtils.createSrcL3IPv4Match(matchBuilder, aceIpv4.getSourceIpv4Network());
                //MatchUtils.createDstL3IPv4Match(matchBuilder, aceIpv4.getDestinationIpv4Network());
                //MatchUtils.createIpProtocolMatch(matchBuilder, aceIp.getProtocol());
                MatchUtils.addLayer4Match(matchBuilder, aceIp.getProtocol().intValue(), 0,
                        aceIp.getDestinationPortRange().getLowerPort().getValue().intValue());
            }
        } else if (matches.getAceType() instanceof AceEth) {
            AceEth aceEth = (AceEth) matches.getAceType();
            MatchUtils.createEthSrcMatch(matchBuilder, new MacAddress(aceEth.getSourceMacAddress().getValue()));
            MatchUtils.createDestEthMatch(matchBuilder, new MacAddress(aceEth.getDestinationMacAddress().getValue()),
                    new MacAddress(aceEth.getDestinationMacAddressMask().getValue()));
        }

        return matchBuilder;
    }

    protected void writeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        LOG.debug("writeFlow: flowBuilder: {}, nodeBuilder: {}", flowBuilder.build(), nodeBuilder.build());
        mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, createNodePath(nodeBuilder), nodeBuilder.build());
        mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, createFlowPath(flowBuilder, nodeBuilder),
                flowBuilder.build());
    }

    /*private void writeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        LOG.debug("writeFlow: flowBuilder: {}, nodeBuilder: {}",
                flowBuilder.build(), nodeBuilder.build());
        WriteTransaction modification = dataBroker.newWriteOnlyTransaction();
        modification.put(LogicalDatastoreType.CONFIGURATION, createNodePath(nodeBuilder),
                nodeBuilder.build(), true);
        modification.put(LogicalDatastoreType.CONFIGURATION, createFlowPath(flowBuilder, nodeBuilder),
                flowBuilder.build(), true);

        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            LOG.debug("Transaction success for write of Flow {}", flowBuilder.getFlowName());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            modification.cancel();
        }
    }*/

    protected void removeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, createFlowPath(flowBuilder, nodeBuilder));
    }

    private NodeBuilder createNodeBuilder(String nodeId) {
        NodeBuilder builder = new NodeBuilder();
        builder.setId(new NodeId(nodeId));
        builder.setKey(new NodeKey(builder.getId()));
        return builder;
    }

    private static InstanceIdentifier<Flow> createFlowPath(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowBuilder.getTableId()))
                .child(Flow.class, flowBuilder.getKey()).build();
    }

    private static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>
    createNodePath(NodeBuilder nodeBuilder) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        nodeBuilder.getKey()).build();
    }
}
