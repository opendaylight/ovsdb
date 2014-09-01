/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import com.google.common.collect.Lists;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityRule;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

public class IngressAclService extends AbstractServiceInstance {

    static final Logger logger = LoggerFactory.getLogger(IngressAclService.class);
    public static final Integer PROTO_MATCH_PRIORITY_DROP = 36006;
    public static final Integer PROTO_PORT_MATCH_PRIORITY_DROP = 36005;
    public static final Integer PREFIX_MATCH_PRIORITY_DROP = 36004;
    public static final Integer PREFIX_PROTO_MATCH_PRIORITY_DROP = 36003;
    public static final Integer PREFIX_PORT_MATCH_PRIORITY_DROP = 36002;
    public static final Integer PROTO_PORT_PREFIX_MATCH_PRIORITY_DROP = 36001;

    public static final Integer PROTO_MATCH_PRIORITY = 61010;
    public static final Integer PREFIX_MATCH_PRIORITY = 61009;
    public static final Integer PREFIX_PROTO_MATCH_PRIORITY = 61008;
    public static final Integer PROTO_PORT_MATCH_PRIORITY = 61009;
    public static final Integer PROTO_PORT_PREFIX_MATCH_PRIORITY = 36894;
    public static final Integer PREFIX_PORT_MATCH_PRIORITY = 36864;
    public static final int ACL_INGRESS_METADATA = 90;
    public static final int ACL_INGRESS_METADATA_MASK = 90;
    public static final int TCP_SYN = 0x002;
    private static final String OPENFLOW = "openflow:";
    public static final short INGRESS_ACL = 90;
    public static final short OUTBOUND_SNAT = 100;

    private static Long groupId = 1L;

    public IngressAclService() {
        super(Service.INGRESS_ACL);
    }

    public IngressAclService(Service service) {
        super(service);
    }

    @Override
    public boolean isBridgeInPipeline(String nodeId) {
        return true;
    }

    public void programPortSecurityACL(Node node, Long dpid, String segmentationId, String attachedMac,
            long localPort, NeutronSecurityGroup securityGroup) {

        logger.debug(
                "programPortSecurityACL neutronSecurityGroup:  {} ", securityGroup);
        List<NeutronSecurityRule> portSecurityList = securityGroup.getSecurityRules();
        /* Iterate over the Port Security Rules in the Port Security Group*/
        for (NeutronSecurityRule portSecurityRule : portSecurityList) {
            /**
             * Neutron Port Security ACL "ingress" and "IPv4"
             *
             * Check that the base conditions for flow based Port Security are true:
             * Port Security Rule Direction ("ingress") and Protocol ("IPv4")
             * Neutron defines the direction "ingress" as the vSwitch to the VM as defined in:
             *  http://docs.openstack.org/api/openstack-network/2.0/content/security_groups.html
             *
             */
            if (portSecurityRule.getSecurityRuleEthertype().equalsIgnoreCase("IPv4") &&
                    portSecurityRule.getSecurityRuleDirection().equalsIgnoreCase("ingress")) {
                logger.debug("** ForEach Rule ** programPortSecurityACL Port Security Rule -> {} ",
                        portSecurityRule);

                /**
                 * TCP Proto (True), TCP Port Minimum (True), TCP Port Max (True), IP Prefix (True)
                 */
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        (!String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null") &&
                                !String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix())
                                        .equalsIgnoreCase("0.0.0.0/0"))) {
                    logger.debug(
                            "Rule #1 PortSec Rule is: TCP Protocol {}, TCP Port Min {}, TCP Port Max {}, IP Prefix {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    handleIngressDefaultTcpDrop(dpid, segmentationId, attachedMac,
                            PROTO_PORT_PREFIX_MATCH_PRIORITY_DROP,
                            true);
                    handleLocalIngressTcpPortWithPrefix(dpid, segmentationId,
                            attachedMac, true, portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix(), PROTO_PORT_PREFIX_MATCH_PRIORITY);
                    continue;
                }

                /*
                * TCP Proto (True), TCP Port Minimum (True), TCP Port Max (False), IP Prefix (True)
                */
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        (!String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null") &&
                                !String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix())
                                        .equalsIgnoreCase("0.0.0.0/0"))) {
                    logger.debug(
                            "Rule #2 PortSec Rule is: TCP Protocol {}, TCP Port Min {}, TCP Port Max {}, IP Prefix {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    handleIngressDefaultTcpDrop(dpid, segmentationId, attachedMac,
                            PROTO_PORT_PREFIX_MATCH_PRIORITY_DROP,
                            true);
                    handleLocalIngressTcpPortWithPrefix(dpid, segmentationId,
                            attachedMac, true, portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix(), PROTO_PORT_PREFIX_MATCH_PRIORITY);
                    continue;
                }

                /*
                * TCP Proto (True), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (True)
                */
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        !String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null")) {
                    logger.debug(
                            "Rule #3 PortSec Rule is: TCP Protocol {}, TCP Port Min {}, TCP Port Max {}, IP Prefix {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    handleIngressDefaultTcpDrop(dpid, segmentationId, attachedMac, PREFIX_PROTO_MATCH_PRIORITY_DROP,
                            true);
                    handleLocalIngressPermitAllProto(dpid, segmentationId, attachedMac, true,
                            portSecurityRule.getSecurityRuleRemoteIpPrefix(), PREFIX_PROTO_MATCH_PRIORITY);
                    continue;
                }

                /*
                * TCP Proto (False), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (True)
                */
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        (!String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null") &&
                                !String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix())
                                        .equalsIgnoreCase("0.0.0.0/0"))) {
                    logger.debug(
                            "Rule #4 PortSec Rule is: TCP Protocol {}, TCP Port Min {}, TCP Port Max {}, IP Prefix {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    handleIngressDefaultTcpDrop(dpid, segmentationId, attachedMac, PREFIX_MATCH_PRIORITY_DROP, true);
                    handleLocalIngressPermitAllProto(dpid, segmentationId, attachedMac, true,
                            portSecurityRule.getSecurityRuleRemoteIpPrefix(), PREFIX_MATCH_PRIORITY);
                    continue;
                }

                /*
                * TCP Proto (True), TCP Port Minimum (True), TCP Port Max (True), IP Prefix (False)
                */
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null")) {
                    logger.debug(
                            "Rule #5 PortSec Rule is: TCP Protocol {}, TCP Port Min {}, TCP Port Max {}, IP Prefix {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    handleIngressDefaultTcpDrop(dpid, segmentationId, attachedMac, PROTO_PORT_MATCH_PRIORITY_DROP,
                            true);
                    handleLocalIngressTcpSyn(dpid, segmentationId,
                            attachedMac, true, portSecurityRule.getSecurityRulePortMin(), PROTO_PORT_MATCH_PRIORITY);
                    continue;
                }

                /*
                * TCP Proto (True), TCP Port Minimum (True), TCP Port Max (False), IP Prefix (False)
                */
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null")) {
                    logger.debug(
                            "Rule #6 PortSec Rule is: TCP Protocol {}, TCP Port Min {}, TCP Port Max {}, IP Prefix {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    handleIngressDefaultTcpDrop(dpid, segmentationId, attachedMac,
                            PROTO_PORT_MATCH_PRIORITY_DROP, true);
                    handleLocalIngressTcpSyn(dpid, segmentationId, attachedMac, true,
                            portSecurityRule.getSecurityRulePortMin(), PROTO_PORT_MATCH_PRIORITY);
                    continue;
                }

                /*
                * TCP Proto (True), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (False or 0.0.0.0/0)
                * Match: tcp,tun_id=0x582,tcp_flags=0x002
                * Actions: resubmit(,90),write_metadata:0xf/0xc
                * Priority: 60002
                */
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        ((String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null")) ||
                                String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix())
                                        .equalsIgnoreCase("0.0.0.0/0"))) {
                    logger.debug(
                            "Rule #7 PortSec Rule is: TCP Protocol {}, TCP Port Min {}, TCP Port Max {}, IP Prefix {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());

                    handleIngressAllowProto(dpid, segmentationId, attachedMac, true,
                            portSecurityRule.getSecurityRuleProtocol(), PROTO_MATCH_PRIORITY);
                    continue;
                }
                logger.debug("ACL Match combination not found for rule: {}", portSecurityRule);
            }

            /**
             * Neutron Port Security ACL "egress" and "IPv4"
             *
             * Check that the base conditions for flow based Port Security are true:
             * Port Security Rule Direction ("egress") and Protocol ("IPv4")
             * Neutron defines the direction "ingress" as the vSwitch to the VM as defined in:
             * http://docs.openstack.org/api/openstack-network/2.0/content/security_groups.html
             *
             */
            if (portSecurityRule.getSecurityRuleEthertype().equalsIgnoreCase("IPv4") &&
                    portSecurityRule.getSecurityRuleDirection().equalsIgnoreCase("egress")) {
                logger.debug("Egress IPV4 ACL  Port Security Rule: {} ", portSecurityRule);
                // TODO Finish Egress Table Implementation
                continue;

            }
        }
    }

    public void handleLocalIngressTcpSyn(Long dpidLong,
            String segmentationId,
            String attachedMac, boolean write,
            Integer securityRulePortMin, Integer protoPortMatchPriority) {

        logger.debug("handleLocalIngressTcpSyn called");

        String nodeName = OPENFLOW + dpidLong;
        PortNumber tcpPort = new PortNumber(securityRulePortMin);

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);

        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils.createDmacTcpSyn(matchBuilder, attachedMac, tcpPort,
                TCP_SYN, segmentationId).build());

        logger.debug("handleLocalIngressTcpSyn MatchBuilder contains:  {}", flowBuilder.getMatch());
        String flowId = "UcastOut_ACL2" + segmentationId + "_" + attachedMac + securityRulePortMin;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(false);
        flowBuilder.setPriority(protoPortMatchPriority);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(INGRESS_ACL);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {

            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            //  getMutablePipelineInstructionBuilder();
            //  int index = instructionsList.size();

            List<Instruction> instructionsList = Lists.newArrayList();
            InstructionUtils.createGotoTableInstructions(ib, OUTBOUND_SNAT);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
            //        } else {
            //            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    public void handleLocalIngressTcpPortWithPrefix(Long dpidLong,
            String segmentationId,
            String attachedMac, boolean write,
            Integer securityRulePortMin, String securityRuleIpPrefix, Integer protoPortPrefixMatchPriority) {

        String nodeName = OPENFLOW + dpidLong;
        PortNumber tcpPort = new PortNumber(securityRulePortMin);

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();
        Ipv4Prefix srcIpPrefix = new Ipv4Prefix(securityRuleIpPrefix);

        flowBuilder.setMatch(MatchUtils
                .createDmacTcpSynIpPrefixTcpPort(matchBuilder, new MacAddress(attachedMac),
                        tcpPort, TCP_SYN, segmentationId, srcIpPrefix).build());

        logger.debug(" MatchBuilder contains:  {}", flowBuilder.getMatch());
        String flowId = "UcastOut2_" + segmentationId + "_" + attachedMac +
                securityRulePortMin + securityRuleIpPrefix;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(false);
        flowBuilder.setPriority(protoPortPrefixMatchPriority);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(INGRESS_ACL);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {

            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            List<Instruction> instructionsList = Lists.newArrayList();
            InstructionUtils.createGotoTableInstructions(ib, OUTBOUND_SNAT);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            // TODO Add removeflow to AnstractServiceInstance
            //removeFlow(flowBuilder, nodeBuilder);
        }
    }

    public void handleIngressAllowProto(Long dpidLong,
            String segmentationId, String attachedMac, boolean write,
            String securityRuleProtcol, Integer protoMatchPriority) {

        logger.debug("handleIngressAllowProto ALL FLOWS ALLOWED");

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils
                .createMacSrcIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, null).build());

        flowBuilder.setMatch(MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        logger.debug("handleLocalIngressTcpSyn MatchBuilder contains:  {}", flowBuilder.getMatch());

        String flowId = "UcastOut_" + segmentationId + "_" +
                attachedMac + "_AllowTCPSynPrefix_" + securityRuleProtcol;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(false);
        flowBuilder.setPriority(protoMatchPriority);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(INGRESS_ACL);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {

            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();
            InstructionUtils.createGotoTableInstructions(ib, OUTBOUND_SNAT);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            // TODO Add removeflow to AnstractServiceInstance
            //removeFlow(flowBuilder, nodeBuilder);
        }
    }


    public void handleIngressDefaultTcpDrop(Long dpidLong,
            String segmentationId,
            String attachedMac, int priority, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils.createTcpPortWithFlagMatch(matchBuilder,
                attachedMac, TCP_SYN, segmentationId).build());
        logger.debug("MatchBuilder contains: {}", flowBuilder.getMatch());

        String flowId = "PortSec_TCP_Syn_Default_Drop_" + attachedMac;
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(false);
        flowBuilder.setPriority(priority);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(INGRESS_ACL);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Set the Output Port/Iface
            InstructionUtils.createDropInstructions(ib);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            // TODO Add removeflow to AnstractServiceInstance
            //removeFlow(flowBuilder, nodeBuilder);
        }
    }

    public void handleLocalIngressPermitAllProto(Long dpidLong, String segmentationId, String attachedMac,
            boolean write, String securityRuleIpPrefix, Integer protoPortMatchPriority) {

        logger.debug(" handleLocalIngressPermitAllProto IPv4 Prefix {}", securityRuleIpPrefix);

        String nodeName = OPENFLOW + dpidLong;
        Ipv4Prefix srcIpPrefix = new Ipv4Prefix(securityRuleIpPrefix);

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId))
                .build());
        if (securityRuleIpPrefix != null) {
            flowBuilder.setMatch(MatchUtils
                    .createMacSrcIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, srcIpPrefix)
                    .build());
        } else {
            flowBuilder.setMatch(MatchUtils
                    .createMacSrcIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, null)
                    .build());
        }
        logger.debug("MatchBuilder contains: {}", flowBuilder.getMatch());

        String flowId = "UcastOut5_" + segmentationId + "_" +
                attachedMac + "_Permit_" + securityRuleIpPrefix;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(false);
        flowBuilder.setPriority(protoPortMatchPriority);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(INGRESS_ACL);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            List<Instruction> instructionsList = Lists.newArrayList();
            InstructionUtils.createGotoTableInstructions(ib, OUTBOUND_SNAT);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            // TODO Add removeflow to AnstractServiceInstance
            //removeFlow(flowBuilder, nodeBuilder);
        }
    }
}