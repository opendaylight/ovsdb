/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.workaround.services;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.ISfcClassifierService;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.NshUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcClassifierService extends AbstractServiceInstance implements ConfigInterface, ISfcClassifierService {
    private static final Logger LOG = LoggerFactory.getLogger(SfcClassifierService.class);
    private static final short UDP_SHORT = 17;
    static int cookieIndex = 0;
    private FlowCache flowCache = new FlowCache();

    private enum FlowID {
        FLOW_INGRESSCLASS(1), FLOW_SFINGRESS(2), FLOW_SFEGRESS(3), FLOW_SFARP(4),
        FLOW_EGRESSCLASSUNUSED(5), FLOW_EGRESSCLASS(6), FLOW_EGRESSCLASSBYPASS(7), FLOW_SFCTABLE(8);

        private int value;
        FlowID(int value) {
            this.value = value;
        }
    }

    private BigInteger getCookie(FlowID flowID) {
        String cookieString = String.format("1110%02d%010d", flowID.value, cookieIndex++);
        return new BigInteger(cookieString, 16);
    }

    private BigInteger getCookie(FlowID flowID, short nsp, short nsi) {
        String cookieString = String.format("1110%02d%03d%03d0%03d", flowID.value, 0, nsp, nsi);
        return new BigInteger(cookieString, 16);
    }

    public SfcClassifierService(Service service) {
        super(service);
    }

    public SfcClassifierService() {
        super(Service.SFC_CLASSIFIER);
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(ISfcClassifierService.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {}

    private FlowBuilder initFlowBuilder(FlowBuilder flowBuilder, String flowName, short table, FlowID flowID) {
        FlowUtils.initFlowBuilder(flowBuilder, flowName, table)
                .setCookie(new FlowCookie(getCookie(flowID)))
                .setCookieMask(new FlowCookie(getCookie(flowID)));
        return flowBuilder;
    }

    private FlowBuilder initFlowBuilder(FlowBuilder flowBuilder, String flowName, short table, FlowID flowID,
                                        short nsp, short nsi) {
        FlowUtils.initFlowBuilder(flowBuilder, flowName, table)
                .setCookie(new FlowCookie(getCookie(flowID, nsp, nsi)))
                .setCookieMask(new FlowCookie(getCookie(flowID, nsp, nsi)));
        return flowBuilder;
    }

    private void writeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder, String rspName, FlowID flowID) {
        flowCache.addFlow(flowBuilder, nodeBuilder, rspName, flowID.value);
        writeFlow(flowBuilder, nodeBuilder);
    }

    private void removeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder, String rspName, FlowID flowID) {
        flowCache.removeFlow(rspName, flowID.value);
        removeFlow(flowBuilder, nodeBuilder);
    }

    @Override
    public void programIngressClassifier(long dataPathId, String ruleName, Matches matches, long nsp, short nsi,
                                         NshUtils nshHeader, long vxGpeOfPort, String rspName, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dataPathId);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = FlowNames.getSfcIngressClass(ruleName, nsp, nsi);
        initFlowBuilder(flowBuilder, flowName, getTable(), FlowID.FLOW_INGRESSCLASS,
                (short)nshHeader.getNshNsp(), nshHeader.getNshNsi());

        MatchBuilder matchBuilder = new AclMatches(matches).buildMatch();
        MatchUtils.addNxRegMatch(matchBuilder,
                MatchUtils.RegMatch.of(FlowUtils.REG_FIELD, FlowUtils.REG_VALUE_FROM_LOCAL));
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            ActionBuilder ab = new ActionBuilder();
            List<Action> actionList = new ArrayList<>();

            ab.setAction(ActionUtils.nxMoveTunIdtoNshc2());
            ab.setOrder(actionList.size());
            ab.setKey(new ActionKey(actionList.size()));
            actionList.add(ab.build());

            getNshAction(nshHeader, actionList);

            ab.setAction(ActionUtils.outputAction(FlowUtils.getNodeConnectorId(dataPathId, vxGpeOfPort)));
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
            writeFlow(flowBuilder, nodeBuilder, rspName, FlowID.FLOW_INGRESSCLASS);
        } else {
            removeFlow(flowBuilder, nodeBuilder, rspName, FlowID.FLOW_INGRESSCLASS);
        }
    }

    @Override
    public void programSfcTable(long dataPathId, long vxGpeOfPort, short goToTableId, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dataPathId);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = FlowNames.getSfcTable(vxGpeOfPort);
        initFlowBuilder(flowBuilder, flowName, getTable(Service.CLASSIFIER), FlowID.FLOW_SFCTABLE)
                .setPriority(1000);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createInPortMatch(matchBuilder, dataPathId, vxGpeOfPort);
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();
            InstructionBuilder ib =
                    InstructionUtils.createGotoTableInstructions(new InstructionBuilder(), goToTableId);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    @Override
    public void programEgressClassifier1(long dataPathId, long vxGpeOfPort, long nsp, short nsi,
                                         int tunnelOfPort, int tunnelId, short gotoTableId, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dataPathId);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = FlowNames.getSfcEgressClass1(vxGpeOfPort);
        initFlowBuilder(flowBuilder, flowName, getTable(Service.CLASSIFIER), FlowID.FLOW_EGRESSCLASSUNUSED,
                (short)nsp, nsi);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createInPortMatch(matchBuilder, dataPathId, vxGpeOfPort);
        MatchUtils.addNxNspMatch(matchBuilder, nsp);
        MatchUtils.addNxNsiMatch(matchBuilder, nsi);
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();

            InstructionBuilder ib = InstructionUtils.createGotoTableInstructions(new InstructionBuilder(), getTable());
            ib.setOrder(instructions.size());
            ib.setKey(new InstructionKey(instructions.size()));
            instructions.add(ib.build());

            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    @Override
    public void programEgressClassifier(long dataPathId, long vxGpeOfPort, long nsp, short nsi,
                                        long sfOfPort, int tunnelId, String rspName, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dataPathId);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = FlowNames.getSfcEgressClass(vxGpeOfPort, nsp, nsi);
        initFlowBuilder(flowBuilder, flowName, getTable(Service.SFC_CLASSIFIER), FlowID.FLOW_EGRESSCLASS,
                (short)nsp, nsi);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createInPortMatch(matchBuilder, dataPathId, vxGpeOfPort);
        MatchUtils.addNxNspMatch(matchBuilder, nsp);
        MatchUtils.addNxNsiMatch(matchBuilder, nsi);
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();
            List<Action> actionList = Lists.newArrayList();

            ActionBuilder ab = new ActionBuilder();

            ab.setAction(
                    ActionUtils.nxLoadRegAction(new DstNxRegCaseBuilder().setNxReg(FlowUtils.REG_FIELD).build(),
                    BigInteger.valueOf(FlowUtils.REG_VALUE_FROM_LOCAL)));
            ab.setOrder(actionList.size());
            ab.setKey(new ActionKey(actionList.size()));
            actionList.add(ab.build());

            ab.setAction(ActionUtils.nxMoveNshc2ToTunId());
            ab.setOrder(actionList.size());
            ab.setKey(new ActionKey(actionList.size()));
            actionList.add(ab.build());

            ab.setAction(ActionUtils.nxResubmitAction((int)sfOfPort, getTable(Service.CLASSIFIER)));
            ab.setOrder(actionList.size());
            ab.setKey(new ActionKey(actionList.size()));
            actionList.add(ab.build());

            ApplyActionsBuilder aab = new ApplyActionsBuilder();
            aab.setAction(actionList);
            InstructionBuilder ib = new InstructionBuilder();
            ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder, rspName, FlowID.FLOW_EGRESSCLASS);
        } else {
            removeFlow(flowBuilder, nodeBuilder, rspName, FlowID.FLOW_EGRESSCLASS);
        }
    }

    @Override
    public void programEgressClassifierBypass(long dataPathId, long vxGpeOfPort, long nsp, short nsi,
                                              long sfOfPort, int tunnelId, String rspName, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dataPathId);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = FlowNames.getSfcEgressClassBypass(nsp, nsi, sfOfPort);
        initFlowBuilder(flowBuilder, flowName, getTable(Service.CLASSIFIER),
                FlowID.FLOW_EGRESSCLASSBYPASS, (short)nsp, nsi)
                .setPriority(40000);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createInPortMatch(matchBuilder, dataPathId, sfOfPort);
        MatchUtils.addNxRegMatch(matchBuilder,
                MatchUtils.RegMatch.of(FlowUtils.REG_FIELD, FlowUtils.REG_VALUE_FROM_LOCAL));
        MatchUtils.addNxNspMatch(matchBuilder, nsp);
        MatchUtils.addNxNsiMatch(matchBuilder, nsi);
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();

            InstructionBuilder ib;
            ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder, rspName, FlowID.FLOW_EGRESSCLASSBYPASS);
        } else {
            removeFlow(flowBuilder, nodeBuilder, rspName, FlowID.FLOW_EGRESSCLASSBYPASS);
        }
    }

    // packet from sf to sff that need to go out local
    @Override
    public void program_sfEgress(long dataPathId, int dstPort, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dataPathId);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = FlowNames.getSfEgress(dstPort);
        initFlowBuilder(flowBuilder, flowName, getTable(), FlowID.FLOW_SFEGRESS);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createIpProtocolMatch(matchBuilder, UDP_SHORT);
        MatchUtils.addLayer4Match(matchBuilder, UDP_SHORT, 0, dstPort);
        MatchUtils.addNxRegMatch(matchBuilder,
                MatchUtils.RegMatch.of(FlowUtils.REG_FIELD, FlowUtils.REG_VALUE_FROM_LOCAL));
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();
            InstructionUtils.createLocalInstructions(ib, dataPathId);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    // looped back sff to sf packets
    @Override
    public void program_sfIngress(long dataPathId, int dstPort, long sfOfPort,
                                  String ipAddress, String sfDplName, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dataPathId);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = FlowNames.getSfIngress(dstPort, ipAddress);
        initFlowBuilder(flowBuilder, flowName, Service.CLASSIFIER.getTable(), FlowID.FLOW_SFINGRESS);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createIpProtocolMatch(matchBuilder, UDP_SHORT);
        Ipv4Prefix ipCidr = MatchUtils.iPv4PrefixFromIPv4Address(ipAddress);
        MatchUtils.createDstL3IPv4Match(matchBuilder, new Ipv4Prefix(ipCidr));
        MatchUtils.addLayer4Match(matchBuilder, UDP_SHORT, 0, dstPort);
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();
            InstructionUtils.createOutputPortInstructions(ib, dataPathId, sfOfPort);

            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    @Override
    public void programStaticArpEntry(long dataPathId, long ofPort, String macAddressStr,
                                      String ipAddress, String rspName, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dataPathId);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = FlowNames.getArpResponder(ipAddress);
        initFlowBuilder(flowBuilder, flowName, getTable(Service.ARP_RESPONDER), FlowID.FLOW_SFARP)
                .setPriority(1024);

        MacAddress macAddress = new MacAddress(macAddressStr);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createInPortReservedMatch(matchBuilder, dataPathId, OutputPortValues.LOCAL.toString());
        MatchUtils.createEtherTypeMatch(matchBuilder, new EtherType(Constants.ARP_ETHERTYPE));
        MatchUtils.createArpDstIpv4Match(matchBuilder, MatchUtils.iPv4PrefixFromIPv4Address(ipAddress));
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();
            ApplyActionsBuilder aab = new ApplyActionsBuilder();
            ActionBuilder ab = new ActionBuilder();
            List<Action> actionList = Lists.newArrayList();

            // Move Eth Src to Eth Dst
            ab.setAction(ActionUtils.nxMoveEthSrcToEthDstAction());
            ab.setOrder(0);
            ab.setKey(new ActionKey(0));
            actionList.add(ab.build());

            // Set Eth Src
            ab.setAction(ActionUtils.setDlSrcAction(new MacAddress(macAddress)));
            ab.setOrder(1);
            ab.setKey(new ActionKey(1));
            actionList.add(ab.build());

            // Set ARP OP
            ab.setAction(ActionUtils.nxLoadArpOpAction(BigInteger.valueOf(FlowUtils.ARP_OP_REPLY)));
            ab.setOrder(2);
            ab.setKey(new ActionKey(2));
            actionList.add(ab.build());

            // Move ARP SHA to ARP THA
            ab.setAction(ActionUtils.nxMoveArpShaToArpThaAction());
            ab.setOrder(3);
            ab.setKey(new ActionKey(3));
            actionList.add(ab.build());

            // Move ARP SPA to ARP TPA
            ab.setAction(ActionUtils.nxMoveArpSpaToArpTpaAction());
            ab.setOrder(4);
            ab.setKey(new ActionKey(4));
            actionList.add(ab.build());

            // Load Mac to ARP SHA
            ab.setAction(ActionUtils.nxLoadArpShaAction(macAddress));
            ab.setOrder(5);
            ab.setKey(new ActionKey(5));
            actionList.add(ab.build());

            // Load IP to ARP SPA
            ab.setAction(ActionUtils.nxLoadArpSpaAction(ipAddress));
            ab.setOrder(6);
            ab.setKey(new ActionKey(6));
            actionList.add(ab.build());

            // Output of InPort
            ab.setAction(ActionUtils.outputAction(
                    FlowUtils.getSpecialNodeConnectorId(dataPathId, OutputPortValues.INPORT.toString())));
            ab.setOrder(7);
            ab.setKey(new ActionKey(7));
            actionList.add(ab.build());

            // Create Apply Actions Instruction
            aab.setAction(actionList);
            ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder, rspName, FlowID.FLOW_SFARP);
        } else {
            removeFlow(flowBuilder, nodeBuilder, rspName, FlowID.FLOW_SFARP);
        }
    }

    private List<Action> getNshAction(NshUtils header, List<Action> actionList) {
        // Build the Actions to Add the NSH Header
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nshC1Load =
                ActionUtils.nxLoadNshc1RegAction(header.getNshMetaC1());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nspLoad =
                ActionUtils.nxSetNspAction(header.getNshNsp());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nsiLoad =
                ActionUtils.nxSetNsiAction(header.getNshNsi());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action loadChainTunVnid =
                ActionUtils.nxLoadTunIdAction(BigInteger.valueOf(header.getNshNsp()), false);
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action loadChainTunDest =
                ActionUtils.nxLoadTunIPv4Action(header.getNshTunIpDst().getValue(), false);

        int count = actionList.size();
        actionList.add(new ActionBuilder()
                .setKey(new ActionKey(count)).setOrder(count++).setAction(nshC1Load).build());
        actionList.add(new ActionBuilder()
                .setKey(new ActionKey(count)).setOrder(count++).setAction(nspLoad).build());
        actionList.add(new ActionBuilder()
                .setKey(new ActionKey(count)).setOrder(count++).setAction(nsiLoad).build());
        actionList.add(new ActionBuilder()
                .setKey(new ActionKey(count)).setOrder(count++).setAction(loadChainTunDest).build());
        actionList.add(new ActionBuilder()
                .setKey(new ActionKey(count)).setOrder(count).setAction(loadChainTunVnid).build());
        return actionList;
    }

    private static FlowID flowSet[] = {FlowID.FLOW_INGRESSCLASS, FlowID.FLOW_EGRESSCLASS,
            FlowID.FLOW_EGRESSCLASSBYPASS, FlowID.FLOW_SFARP};

    @Override
    public void clearFlows(DataBroker dataBroker, String rspName) {
        Map<Integer, InstanceIdentifier<Flow>> flowMap = flowCache.getFlows(rspName);
        if (flowMap != null) {
            for (FlowID flowID : flowSet) {
                InstanceIdentifier<Flow> path = flowMap.get(flowID.value);
                if (path != null) {
                    flowCache.removeFlow(rspName, flowID.value);
                    removeFlow(dataBroker, path);
                }
            }
        }
    }

    private void removeFlow(DataBroker dataBroker, InstanceIdentifier<Flow> path) {
        WriteTransaction modification = dataBroker.newWriteOnlyTransaction();
        modification.delete(LogicalDatastoreType.CONFIGURATION, path);

        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            LOG.debug("Transaction success for deletion of Flow {}", path);
        } catch (Exception e) {
            LOG.error("Failed to remove flow {}", path, e);
            modification.cancel();
        }
    }
}
