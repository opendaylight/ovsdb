/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import java.math.BigInteger;
import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.IngressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class IngressAclService extends AbstractServiceInstance implements IngressAclProvider, ConfigInterface {

    private static final Logger LOG = LoggerFactory.getLogger(IngressAclService.class);
    private volatile SecurityServicesManager securityServicesManager;

    public IngressAclService() {
        super(Service.INGRESS_ACL);
    }

    public IngressAclService(Service service) {
        super(service);
    }

    @Override
    public void programPortSecurityAcl(Long dpid, String segmentationId, String attachedMac,
                                       long localPort, NeutronSecurityGroup securityGroup,
                                       List<Neutron_IPs> srcAddressList, boolean write) {

        LOG.trace("programLocalBridgeRulesWithSec neutronSecurityGroup: {} ", securityGroup);
        List<NeutronSecurityRule> portSecurityList = securityGroup.getSecurityRules();
        /* Iterate over the Port Security Rules in the Port Security Group bound to the port*/
        for (NeutronSecurityRule portSecurityRule : portSecurityList) {
            /**
             * Neutron Port Security Acl "ingress" and "IPv4"
             * Check that the base conditions for flow based Port Security are true:
             * Port Security Rule Direction ("ingress") and Protocol ("IPv4")
             * Neutron defines the direction "ingress" as the vSwitch to the VM as defined in:
             * http://docs.openstack.org/api/openstack-network/2.0/content/security_groups.html
             *
             */

            if ("IPv4".equals(portSecurityRule.getSecurityRuleEthertype())
                    && "ingress".equals(portSecurityRule.getSecurityRuleDirection())) {
                LOG.debug("Acl Rule matching IPv4 and ingress is: {} ", portSecurityRule);
                if (null == portSecurityRule.getSecurityRuleProtocol()) {
                    ingressAclIPv4(dpid, segmentationId, attachedMac,
                                   write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                } else if (null != portSecurityRule.getSecurityRemoteGroupID()) {
                    //Remote Security group is selected
                    List<Neutron_IPs> remoteSrcAddressList = securityServicesManager
                            .getVmListForSecurityGroup(srcAddressList,portSecurityRule.getSecurityRemoteGroupID());
                    if (null != remoteSrcAddressList) {
                        for (Neutron_IPs vmIp :remoteSrcAddressList ) {
                            switch (portSecurityRule.getSecurityRuleProtocol()) {
                            case MatchUtils.TCP:
                                ingressAclTcp(dpid, segmentationId, attachedMac, portSecurityRule,vmIp.getIpAddress(),
                                              write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                                break;
                            case MatchUtils.UDP:
                                ingressAclUdp(dpid, segmentationId, attachedMac, portSecurityRule,vmIp.getIpAddress(),
                                              write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                                break;
                            default:
                                LOG.error("programPortSecurityAcl: Protocol not supported", portSecurityRule);
                                break;
                            }
                        }
                    }
                } else {
                    //CIDR is selected
                    switch (portSecurityRule.getSecurityRuleProtocol()) {
                    case MatchUtils.TCP:
                        ingressAclTcp(dpid, segmentationId, attachedMac,
                                      portSecurityRule, null, write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                        break;
                    case MatchUtils.UDP:
                        ingressAclUdp(dpid, segmentationId, attachedMac,
                                      portSecurityRule, null, write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                        break;
                    default:
                        LOG.error("programPortSecurityAcl: Protocol not supported", portSecurityRule);
                    }
                }

                /**
                 * TCP Proto (True), TCP Port Minimum (True), TCP Port Max (True), IP Prefix (True)
                 * TODO Some part of the code will be  used when conntrack is supported

                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        (!String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null") &&
                                !String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix())
                                        .equalsIgnoreCase("0.0.0.0/0"))) {
                    LOG.debug("Rule #1 ingress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    ingressACLDefaultTcpDrop(dpid, segmentationId, attachedMac,
                            Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY_DROP,
                            true);
                    ingressACLTcpPortWithPrefix(dpid, segmentationId,
                            attachedMac, true, portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix(), Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                    continue;
                }
                /**
                 * TCP Proto (True), TCP Port Minimum (True), TCP Port Max (False), IP Prefix (True)
                 */
                /*if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        (!String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null") &&
                                !String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix())
                                        .equalsIgnoreCase("0.0.0.0/0"))) {
                    LOG.debug("Rule #2 ingress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    ingressACLDefaultTcpDrop(dpid, segmentationId, attachedMac,
                                             Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY_DROP,
                            true);
                    ingressACLTcpPortWithPrefix(dpid, segmentationId,
                            attachedMac, true, portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix(), Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                    continue;
                }
                 *//**
                 * TCP Proto (True), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (True)
                 *//*
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        !String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null")) {
                    LOG.debug("Rule #3 ingress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    ingressACLDefaultTcpDrop(dpid, segmentationId, attachedMac, Constants.PROTO_PREFIX_MATCH_PRIORITY_DROP,
                            true);
                    ingressACLPermitAllProto(dpid, segmentationId, attachedMac, true,
                            portSecurityRule.getSecurityRuleRemoteIpPrefix(), Constants.PROTO_PREFIX_MATCH_PRIORITY);
                    continue;
                }
                  *//**
                  * TCP Proto (False), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (True)
                  *//*
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        (!String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null") &&
                                !String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix())
                                        .equalsIgnoreCase("0.0.0.0/0"))) {
                    LOG.debug("Rule #4 ingress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    ingressACLDefaultTcpDrop(dpid, segmentationId, attachedMac, Constants.PREFIX_MATCH_PRIORITY_DROP, true);
                    ingressACLPermitAllProto(dpid, segmentationId, attachedMac, true,
                            portSecurityRule.getSecurityRuleRemoteIpPrefix(), Constants.PREFIX_MATCH_PRIORITY);
                    continue;
                }
                   *//**
                   * TCP Proto (True), TCP Port Minimum (True), TCP Port Max (True), IP Prefix (False)
                   *//*
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null")) {
                    LOG.debug("Rule #5 ingress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    ingressACLDefaultTcpDrop(dpid, segmentationId, attachedMac, Constants.PROTO_PORT_MATCH_PRIORITY_DROP,
                            true);
                    ingressACLTcpSyn(dpid, segmentationId,
                            attachedMac, true, portSecurityRule.getSecurityRulePortMin(),
                            Constants.PREFIX_PORT_MATCH_PRIORITY_DROP);
                    continue;
                }
                    *//**
                    * TCP Proto (True), TCP Port Minimum (True), TCP Port Max (False), IP Prefix (False)
                    *//*
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        !String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null")) {
                    LOG.debug("Rule #6 ingress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    ingressACLDefaultTcpDrop(dpid, segmentationId, attachedMac,
                                             Constants.PROTO_PORT_MATCH_PRIORITY_DROP, true);
                    ingressACLTcpSyn(dpid, segmentationId, attachedMac, true,
                            portSecurityRule.getSecurityRulePortMin(), Constants.PROTO_PORT_MATCH_PRIORITY);
                    continue;
                }
                     *//**
                     * TCP Proto (True), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (False or 0.0.0.0/0)
                     *//*
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                        String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                        ((String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null")) ||
                                String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix())
                                        .equalsIgnoreCase("0.0.0.0/0"))) {
                    LOG.debug("Rule #7 ingress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    // No need to drop until UDP/ICMP are implemented
                    // ingressACLDefaultTcpDrop(dpid, segmentationId, attachedMac, PROTO_MATCH_PRIORITY_DROP, true);
                    handleIngressAllowProto(dpid, segmentationId, attachedMac, true,
                            portSecurityRule.getSecurityRuleProtocol(), Constants.PROTO_MATCH_PRIORITY);
                    continue;
                }*/
                LOG.debug("Ingress Acl Match combination not found for rule: {}", portSecurityRule);
            }
        }
    }

    @Override
    public void programFixedSecurityAcl(Long dpid, String segmentationId, String dhcpMacAddress,
                                        long localPort, boolean isLastPortinSubnet,
                                        boolean isComputePort, boolean write) {
        //If this port is the only port in the compute node add the DHCP server rule.
        if (isLastPortinSubnet && isComputePort ) {
            ingressAclDhcpAllowServerTraffic(dpid, segmentationId,dhcpMacAddress,
                                             write,Constants.PROTO_DHCP_SERVER_MATCH_PRIORITY);
        }
    }

    /**
     * Allows IPv4 packet ingress to the destination mac address.
     * @param dpidLong the dpid
     * @param segmentationId the segementation id
     * @param dstMac the destination mac address
     * @param write add or remove
     * @param protoPortMatchPriority the protocol match priority.
     */
    private void ingressAclIPv4(Long dpidLong, String segmentationId, String dstMac,
                               boolean write, Integer protoPortMatchPriority ) {
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        String flowId = "Ingress_IP" + segmentationId + "_" + dstMac + "_Permit_";
        matchBuilder = MatchUtils.createEtherMatchWithType(matchBuilder,null,dstMac);
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);

    }
    /**
     * Creates a ingress match to the dst macaddress. If src address is specified
     * source specific match will be created. Otherwise a match with a CIDR will
     * be created.
     * @param dpidLong the dpid
     * @param segmentationId the segmentation id
     * @param dstMac the destination mac address.
     * @param portSecurityRule the security rule in the SG
     * @param srcAddress the destination IP address
     * @param write add or delete
     * @param protoPortMatchPriority the protocol match priroty
     */
    private void ingressAclTcp(Long dpidLong, String segmentationId, String dstMac,
                              NeutronSecurityRule portSecurityRule, String srcAddress, boolean write,
                              Integer protoPortMatchPriority ) {

        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowId = "Ingress_Custom_Tcp" + segmentationId + "_" + dstMac + "_";
        matchBuilder = MatchUtils.createEtherMatchWithType(matchBuilder,null,dstMac);
        if (portSecurityRule.getSecurityRulePortMin().equals(portSecurityRule.getSecurityRulePortMax())) {
            flowId = flowId + portSecurityRule.getSecurityRulePortMin();
            matchBuilder = MatchUtils.addLayer4Match(matchBuilder, MatchUtils.TCP_SHORT, 0,
                                                    portSecurityRule.getSecurityRulePortMin());
        } else {
            /*TODO TCP PortRange Match*/

        }

        if (null != srcAddress) {
            flowId = flowId + srcAddress;
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                                                        MatchUtils.iPv4PrefixFromIPv4Address(srcAddress),null);

        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                                                        new Ipv4Prefix(portSecurityRule
                                                                       .getSecurityRuleRemoteIpPrefix()),null);
        }
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        flowId = flowId + "_Permit_";
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);

    }

    /**
     * Creates a ingress match to the dst macaddress. If src address is specified
     * source specific match will be created. Otherwise a match with a CIDR will
     * be created.
     * @param dpidLong the dpid
     * @param segmentationId the segmentation id
     * @param dstMac the destination mac address.
     * @param portSecurityRule the security rule in the SG
     * @param srcAddress the destination IP address
     * @param write add or delete
     * @param protoPortMatchPriority the protocol match priroty
     */
    private void ingressAclUdp(Long dpidLong, String segmentationId, String dstMac,
                              NeutronSecurityRule portSecurityRule, String srcAddress,
                              boolean write, Integer protoPortMatchPriority ) {
        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "ingressAclUDP" + segmentationId + "_" + dstMac + "_";
        matchBuilder = MatchUtils.createEtherMatchWithType(matchBuilder,null,dstMac);
        if (portSecurityRule.getSecurityRulePortMin().equals(portSecurityRule.getSecurityRulePortMax())) {
            flowId = flowId + portSecurityRule.getSecurityRulePortMin();
            matchBuilder = MatchUtils.addLayer4Match(matchBuilder, MatchUtils.UDP_SHORT, 0,
                                                    portSecurityRule.getSecurityRulePortMin());
        } else {
            /*TODO TCP PortRange Match*/

        }

        if (null != srcAddress) {
            flowId = flowId + srcAddress;
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                                                        MatchUtils.iPv4PrefixFromIPv4Address(srcAddress), null);

        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                                      new Ipv4Prefix(portSecurityRule.getSecurityRuleRemoteIpPrefix()),null);
        }
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        flowId = flowId + "_Permit_";
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);

    }

    public void ingressACLTcpSyn(Long dpidLong, String segmentationId, String attachedMac, boolean write,
                                 Integer securityRulePortMin, Integer protoPortMatchPriority) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        PortNumber tcpPort = new PortNumber(securityRulePortMin);
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils.createDmacTcpSynMatch(matchBuilder, attachedMac, tcpPort,
                                                              Constants.TCP_SYN, segmentationId).build());

        LOG.debug("ingressACLTcpSyn MatchBuilder contains:  {}", flowBuilder.getMatch());
        String flowId = "UcastOut_ACL2_" + segmentationId + "_" + attachedMac + securityRulePortMin;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(false);
        flowBuilder.setPriority(protoPortMatchPriority);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();

            InstructionBuilder ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);

            LOG.debug("Instructions are: {}", ib.getInstruction());
            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    public void ingressACLTcpPortWithPrefix(Long dpidLong, String segmentationId, String attachedMac,
                                            boolean write, Integer securityRulePortMin, String securityRuleIpPrefix,
                                            Integer protoPortPrefixMatchPriority) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        PortNumber tcpPort = new PortNumber(securityRulePortMin);

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = this.createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();
        Ipv4Prefix srcIpPrefix = new Ipv4Prefix(securityRuleIpPrefix);

        flowBuilder.setMatch(MatchUtils
                             .createDmacTcpSynDstIpPrefixTcpPort(matchBuilder, new MacAddress(attachedMac),
                                                                 tcpPort, Constants.TCP_SYN, segmentationId, srcIpPrefix).build());

        LOG.debug(" MatchBuilder contains:  {}", flowBuilder.getMatch());
        String flowId = "UcastOut2_" + segmentationId + "_" + attachedMac +
                securityRulePortMin + securityRuleIpPrefix;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(false);
        flowBuilder.setPriority(protoPortPrefixMatchPriority);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionsBuilder isb = new InstructionsBuilder();

            List<Instruction> instructionsList = Lists.newArrayList();
            InstructionBuilder ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);

            LOG.debug("Instructions contain: {}", ib.getInstruction());
            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    public void handleIngressAllowProto(Long dpidLong, String segmentationId, String attachedMac, boolean write,
                                        String securityRuleProtcol, Integer protoMatchPriority) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils
                             .createDmacIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, null).build());
        flowBuilder.setMatch(MatchUtils
                             .createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        LOG.debug("MatchBuilder contains: {}", flowBuilder.getMatch());

        String flowId = "UcastOut_" + segmentationId + "_" +
                attachedMac + "_AllowTCPSynPrefix_" + securityRuleProtcol;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(false);
        flowBuilder.setPriority(protoMatchPriority);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();

            InstructionBuilder ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(1);
            ib.setKey(new InstructionKey(1));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);
            LOG.debug("Instructions contain: {}", ib.getInstruction());

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }


    public void ingressACLDefaultTcpDrop(Long dpidLong, String segmentationId, String attachedMac,
                                         int priority, boolean write) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils.createDmacTcpPortWithFlagMatch(matchBuilder,
                                                                       attachedMac, Constants.TCP_SYN, segmentationId).build());

        LOG.debug("MatchBuilder contains: {}", flowBuilder.getMatch());
        String flowId = "PortSec_TCP_Syn_Default_Drop_" + segmentationId + "_" + attachedMac;
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(false);
        flowBuilder.setPriority(priority);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
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
            LOG.debug("Instructions contain: {}", ib.getInstruction());
            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    public void ingressACLPermitAllProto(Long dpidLong, String segmentationId, String attachedMac,
                                         boolean write, String securityRuleIpPrefix, Integer protoPortMatchPriority) {
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        Ipv4Prefix srcIpPrefix = new Ipv4Prefix(securityRuleIpPrefix);
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId))
                             .build());
        if (securityRuleIpPrefix != null) {
            flowBuilder.setMatch(MatchUtils
                                 .createDmacIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, srcIpPrefix)
                                 .build());
        } else {
            flowBuilder.setMatch(MatchUtils
                                 .createDmacIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, null)
                                 .build());
        }

        LOG.debug("MatchBuilder contains: {}", flowBuilder.getMatch());
        String flowId = "IngressProto_ACL_" + segmentationId + "_" +
                attachedMac + "_Permit_" + securityRuleIpPrefix;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(false);
        flowBuilder.setPriority(protoPortMatchPriority);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();

            ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(1);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);

            LOG.debug("Instructions contain: {}", ib.getInstruction());
            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    /**
     * Add rule to ensure only DHCP server traffic from the specified mac is allowed.
     *
     * @param dpidLong the dpid
     * @param segmentationId the segmentation id
     * @param dhcpMacAddress the DHCP server mac address
     * @param write is write or delete
     * @param protoPortMatchPriority the priority
     */
    private void ingressAclDhcpAllowServerTraffic(Long dpidLong, String segmentationId, String dhcpMacAddress,
                                                  boolean write, Integer protoPortMatchPriority) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        MatchUtils.createDhcpServerMatch(matchBuilder, dhcpMacAddress, 67, 68).build();
        LOG.debug("ingressAclDHCPAllowServerTraffic: MatchBuilder contains: {}", matchBuilder);
        String flowId = "Ingress_DHCP_Server" + segmentationId + "_" + dhcpMacAddress + "_Permit_";
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);
    }

    /**
     * Add or remove flow to the node.
     *
     * @param flowId the the flow id
     * @param nodeBuilder the node builder
     * @param matchBuilder the matchbuilder
     * @param protoPortMatchPriority the protocol priority
     * @param write whether it is a write
     * @param drop whether it is a drop or forward
     */
    private void syncFlow(String flowId, NodeBuilder nodeBuilder,
                          MatchBuilder matchBuilder,Integer protoPortMatchPriority,
                          boolean write,boolean drop) {
        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(false);
        flowBuilder.setPriority(protoPortMatchPriority);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = this.getMutablePipelineInstructionBuilder();
            if (drop) {
                InstructionUtils.createDropInstructions(ib);
            }
            ib.setOrder(0);
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }

    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(IngressAclProvider.class.getName()), this);
        securityServicesManager =
                (SecurityServicesManager) ServiceHelper.getGlobalInstance(SecurityServicesManager.class, this);
    }

    @Override
    public void setDependencies(Object impl) {

    }
}
