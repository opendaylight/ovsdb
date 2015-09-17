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
import org.opendaylight.ovsdb.openstack.netvirt.api.EgressAclProvider;
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

public class EgressAclService extends AbstractServiceInstance implements EgressAclProvider, ConfigInterface {

    private static final Logger LOG = LoggerFactory.getLogger(EgressAclService.class);
    private volatile SecurityServicesManager securityServicesManager;
    private static final int DHCP_SOURCE_PORT = 67;
    private static final int DHCP_DESTINATION_PORT = 68;
    private static final String HOST_MASK = "/32";

    public EgressAclService() {
        super(Service.EGRESS_ACL);
    }

    public EgressAclService(Service service) {
        super(service);
    }

    @Override
    public void programPortSecurityAcl(Long dpid, String segmentationId, String attachedMac, long localPort,
                                       NeutronSecurityGroup securityGroup,
                                       List<Neutron_IPs> srcAddressList, boolean write) {

        LOG.trace("programPortSecurityAcl: neutronSecurityGroup: {} ", securityGroup);
        if (securityGroup == null || securityGroup.getSecurityRules() == null) {
            return;
        }

        List<NeutronSecurityRule> portSecurityList = securityGroup.getSecurityRules();
        /* Iterate over the Port Security Rules in the Port Security Group bound to the port*/
        for (NeutronSecurityRule portSecurityRule : portSecurityList) {
            /**
             * Neutron Port Security Acl "egress" and "IPv4"
             * Check that the base conditions for flow based Port Security are true:
             * Port Security Rule Direction ("egress") and Protocol ("IPv4")
             * Neutron defines the direction "ingress" as the vSwitch to the VM as defined in:
             * http://docs.openstack.org/api/openstack-network/2.0/content/security_groups.html
             *
             */
            if (portSecurityRule.getSecurityRuleEthertype().equals("IPv4")
                    && portSecurityRule.getSecurityRuleDirection().equals("egress")) {
                LOG.debug("programPortSecurityAcl: Acl Rule matching IPv4 and ingress is: {} ", portSecurityRule);
                if (null == portSecurityRule.getSecurityRuleProtocol()) {
                    /* TODO Rework on the priority values */
                    egressAclIPv4(dpid, segmentationId, attachedMac,
                                  write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                } else if (null != portSecurityRule.getSecurityRemoteGroupID()) {
                  //Remote Security group is selected
                    List<Neutron_IPs> remoteSrcAddressList = securityServicesManager
                            .getVmListForSecurityGroup(srcAddressList,portSecurityRule.getSecurityRemoteGroupID());
                    if (null != remoteSrcAddressList) {
                        for (Neutron_IPs vmIp :remoteSrcAddressList ) {
                            switch (portSecurityRule.getSecurityRuleProtocol()) {
                            case MatchUtils.TCP:
                                egressAclTcp(dpid, segmentationId, attachedMac,
                                             portSecurityRule,vmIp.getIpAddress(), write,
                                             Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                                break;
                            case MatchUtils.UDP:
                                egressAclUdp(dpid, segmentationId, attachedMac,
                                             portSecurityRule,vmIp.getIpAddress(), write,
                                             Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
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
                        egressAclTcp(dpid, segmentationId, attachedMac,
                                     portSecurityRule, null, write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                        break;
                    case MatchUtils.UDP:
                        egressAclUdp(dpid, segmentationId, attachedMac,
                                     portSecurityRule, null, write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                        break;
                    default:
                        LOG.error("programPortSecurityAcl: Protocol not supported", portSecurityRule);
                    }
                }
            }
            /*
             * Code is refactored to handle all the protocols. More
             * protocols  will be added incrementrally
             * TODO Connection tracking will be used to track active TCP connections This code
             * may be reused then.
             */
            /* if (portSecurityRule.getSecurityRuleEthertype().equalsIgnoreCase("IPv4") &&
                portSecurityRule.getSecurityRuleDirection().equalsIgnoreCase("egress")) {
                LOG.debug("Egress IPV4 ACL  Port Security Rule: {} ", portSecurityRule);
                // ToDo: Implement Port Range

             *//**
             * TCP Proto (True), TCP Port Minimum (True), TCP Port Max (True), IP Prefix (True)
             *//*
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                    !String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                    !String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                    (!String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null") &&
                     !String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix())
                             .equalsIgnoreCase("0.0.0.0/0"))) {
                    LOG.debug(
                            "Rule #1 egress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    egressACLDefaultTcpDrop(dpid, segmentationId, attachedMac,
                                            Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY_DROP,
                                            true);
                    egressACLTcpPortWithPrefix(dpid, segmentationId,
                                               attachedMac, true, portSecurityRule.getSecurityRulePortMin(),
                                               portSecurityRule.getSecurityRuleRemoteIpPrefix(),
                                               Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                    continue;
                }
              *//**
              * TCP Proto (True), TCP Port Minimum (True), TCP Port Max (False), IP Prefix (True)
              *//*
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                    !String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                    String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                    (!String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null") &&
                     !String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix())
                             .equalsIgnoreCase("0.0.0.0/0"))) {
                    LOG.debug(
                            "Rule #2 egress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    egressACLDefaultTcpDrop(dpid, segmentationId, attachedMac,
                                            Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY_DROP,
                                            true);
                    egressACLTcpPortWithPrefix(dpid, segmentationId,
                                               attachedMac, true, portSecurityRule.getSecurityRulePortMin(),
                                               portSecurityRule.getSecurityRuleRemoteIpPrefix(),
                                               Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                    continue;
                }
               *//**
               * TCP Proto (True), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (True)
               *//*
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                    String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                    String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                    !String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null")) {
                    LOG.debug(
                            "Rule #3 egress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    egressACLDefaultTcpDrop(dpid, segmentationId, attachedMac, Constants.PROTO_PREFIX_MATCH_PRIORITY_DROP,
                                            true);
                    egressACLPermitAllProto(dpid, segmentationId, attachedMac, true,
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
                    LOG.debug(
                            "Rule #4 egress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    egressACLDefaultTcpDrop(dpid, segmentationId, attachedMac, Constants.PREFIX_MATCH_PRIORITY_DROP, true);
                    egressACLPermitAllProto(dpid, segmentationId, attachedMac, true,
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
                    LOG.debug(
                            "Rule #5 egress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    egressACLDefaultTcpDrop(dpid, segmentationId, attachedMac, Constants.PROTO_PORT_MATCH_PRIORITY_DROP,
                                            true);
                    egressACLTcpSyn(dpid, segmentationId,
                                    attachedMac, true, portSecurityRule.getSecurityRulePortMin(),
                                    Constants.PROTO_PORT_MATCH_PRIORITY);
                    continue;
                }
                  *//**
                  * TCP Proto (True), TCP Port Minimum (True), TCP Port Max (False), IP Prefix (False)
                  *//*
                if (String.valueOf(portSecurityRule.getSecurityRuleProtocol()).equalsIgnoreCase("tcp") &&
                    !String.valueOf(portSecurityRule.getSecurityRulePortMin()).equalsIgnoreCase("null") &&
                    String.valueOf(portSecurityRule.getSecurityRulePortMax()).equalsIgnoreCase("null") &&
                    String.valueOf(portSecurityRule.getSecurityRuleRemoteIpPrefix()).equalsIgnoreCase("null")) {
                    LOG.debug(
                            "Rule #6 egress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    egressACLDefaultTcpDrop(dpid, segmentationId, attachedMac,
                                            Constants.PROTO_PORT_MATCH_PRIORITY_DROP, true);
                    egressACLTcpSyn(dpid, segmentationId, attachedMac, true,
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
                    LOG.debug(
                            "Rule #7 egress PortSec Rule Matches -> TCP Protocol: {}, TCP Port Min: {}, TCP Port Max: {}, IP Prefix: {}",
                            portSecurityRule.getSecurityRuleProtocol(), portSecurityRule.getSecurityRulePortMin(),
                            portSecurityRule.getSecurityRulePortMax(),
                            portSecurityRule.getSecurityRuleRemoteIpPrefix());
                    // No need to drop until UDP/ICMP are implemented
                    // egressACLDefaultTcpDrop(dpid, segmentationId, attachedMac, PROTO_MATCH_PRIORITY_DROP, true);
                    egressAllowProto(dpid, segmentationId, attachedMac, true,
                                     portSecurityRule.getSecurityRuleProtocol(), Constants.PROTO_MATCH_PRIORITY);
                    continue;
                }
                LOG.debug("ACL Match combination not found for rule: {}", portSecurityRule);
            }*/
        }
    }

    @Override
    public void programFixedSecurityAcl(Long dpid, String segmentationId, String attachedMac,
                                        long localPort, List<Neutron_IPs> srcAddressList,
                                        boolean isLastPortinBridge, boolean isComputePort ,boolean write) {
        // If it is the only port in the bridge add the rule to allow any DHCP client traffic
        if (isLastPortinBridge) {
            egressAclDhcpAllowClientTrafficFromVm(dpid, write, Constants.PROTO_DHCP_CLIENT_TRAFFIC_MATCH_PRIORITY);
        }
        if (isComputePort) {
            // add rule to drop the DHCP server traffic originating from the vm.
            egressAclDhcpDropServerTrafficfromVm(dpid, localPort, write,
                                                 Constants.PROTO_DHCP_CLIENT_SPOOF_MATCH_PRIORITY_DROP);
            //Adds rule to check legitimate ip/mac pair for each packet from the vm
            for (Neutron_IPs srcAddress : srcAddressList) {
                String addressWithPrefix = srcAddress.getIpAddress() + HOST_MASK;
                egressAclAllowTrafficFromVmIpMacPair(dpid, localPort, attachedMac, addressWithPrefix,
                                                     Constants.PROTO_VM_IP_MAC_MATCH_PRIORITY,write);
            }
        }
    }

    /**
     * Allows IPv4 packet egress from the src mac address.
     * @param dpidLong the dpid
     * @param segmentationId the segementation id
     * @param srcMac the src mac address
     * @param write add or remove
     * @param protoPortMatchPriority the protocol match priority.
     */
    private void egressAclIPv4(Long dpidLong, String segmentationId, String srcMac,
                               boolean write, Integer protoPortMatchPriority ) {
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        String flowId = "Egress_IP" + segmentationId + "_" + srcMac + "_Permit_";
        matchBuilder = MatchUtils.createEtherMatchWithType(matchBuilder,srcMac,null);
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);
    }

    /**
     * Creates a egress match with src macaddress. If dest address is specified
     * destination specific match will be created. Otherwise a match with a
     * CIDR will be created.
     * @param dpidLong the dpid
     * @param segmentationId the segmentation id
     * @param srcMac the source mac address.
     * @param portSecurityRule the security rule in the SG
     * @param dstAddress the destination IP address
     * @param write add or delete
     * @param protoPortMatchPriority the protocol match priroty
     */
    private void egressAclTcp(Long dpidLong, String segmentationId, String srcMac,
                              NeutronSecurityRule portSecurityRule, String dstAddress,
                              boolean write, Integer protoPortMatchPriority) {
        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Egress_Custom_Tcp" + segmentationId + "_" + srcMac + "_";
        matchBuilder = MatchUtils.createEtherMatchWithType(matchBuilder,srcMac,null);
        if (portSecurityRule.getSecurityRulePortMin().equals(portSecurityRule.getSecurityRulePortMax())) {
            flowId = flowId + portSecurityRule.getSecurityRulePortMin();
            matchBuilder = MatchUtils.addLayer4Match(matchBuilder, MatchUtils.TCP_SHORT, 0,
                                                     portSecurityRule.getSecurityRulePortMin());
        } else {
            /*TODO TCP PortRange Match*/

        }

        if (null != dstAddress) {
            flowId = flowId + dstAddress;
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,null,
                                                        MatchUtils.iPv4PrefixFromIPv4Address(dstAddress));

        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,null,
                                                        new Ipv4Prefix(portSecurityRule
                                                                       .getSecurityRuleRemoteIpPrefix()));
        }
        flowId = flowId + "_Permit_";
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);

    }
    /**
     * Creates a egress match with src macaddress. If dest address is specified
     * destination specific match will be created. Otherwise a match with a
     * CIDR will be created.
     * @param dpidLong the dpid
     * @param segmentationId the segmentation id
     * @param srcMac the source mac address.
     * @param portSecurityRule the security rule in the SG
     * @param dstAddress the source IP address
     * @param write add or delete
     * @param protoPortMatchPriority the protocol match priroty
     */
    private void egressAclUdp(Long dpidLong, String segmentationId, String srcMac,
                              NeutronSecurityRule portSecurityRule, String dstAddress,
                              boolean write, Integer protoPortMatchPriority) {

        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Eress_UDP" + segmentationId + "_" + srcMac + "_";
        matchBuilder = MatchUtils.createEtherMatchWithType(matchBuilder,srcMac,null);
        if (portSecurityRule.getSecurityRulePortMin().equals(portSecurityRule.getSecurityRulePortMax())) {
            flowId = flowId + portSecurityRule.getSecurityRulePortMin();
            matchBuilder = MatchUtils.addLayer4Match(matchBuilder, MatchUtils.UDP_SHORT, 0,
                                                     portSecurityRule.getSecurityRulePortMin());
        } else {
            /*TODO UDP PortRange Match*/

        }

        if (null != dstAddress) {
            flowId = flowId + dstAddress;
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,null,
                                                        MatchUtils.iPv4PrefixFromIPv4Address(dstAddress));

        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder, null,
                                                        new Ipv4Prefix(portSecurityRule
                                                                       .getSecurityRuleRemoteIpPrefix()));
        }
        flowId = flowId + "_Permit_";
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);

    }
    public void egressACLDefaultTcpDrop(Long dpidLong, String segmentationId, String attachedMac,
                                        int priority, boolean write) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils.createSmacTcpPortWithFlagMatch(matchBuilder,
                                                                       attachedMac, Constants.TCP_SYN, segmentationId).build());
        LOG.debug("MatchBuilder contains: {}", flowBuilder.getMatch());

        String flowId = "TCP_Syn_Egress_Default_Drop_" + segmentationId + "_" + attachedMac;
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
            List<Instruction> instructions = Lists.newArrayList();

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

    public void egressACLTcpPortWithPrefix(Long dpidLong, String segmentationId, String attachedMac, boolean write,
                                           Integer securityRulePortMin, String securityRuleIpPrefix, Integer protoPortPrefixMatchPriority) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        PortNumber tcpPort = new PortNumber(securityRulePortMin);
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();
        Ipv4Prefix srcIpPrefix = new Ipv4Prefix(securityRuleIpPrefix);

        flowBuilder.setMatch(MatchUtils
                             .createSmacTcpSynDstIpPrefixTcpPort(matchBuilder, new MacAddress(attachedMac),
                                                                 tcpPort, Constants.TCP_SYN, segmentationId, srcIpPrefix).build());

        LOG.debug(" MatchBuilder contains:  {}", flowBuilder.getMatch());
        String flowId = "UcastEgress_" + segmentationId + "_" + attachedMac +
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



    public void egressAllowProto(Long dpidLong, String segmentationId, String attachedMac, boolean write,
                                 String securityRuleProtcol, Integer protoMatchPriority) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils
                             .createDmacIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, null).build());
        flowBuilder.setMatch(MatchUtils
                             .createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());

        LOG.debug("MatchBuilder contains:  {}", flowBuilder.getMatch());
        String flowId = "EgressAllProto_" + segmentationId + "_" +
                attachedMac + "_AllowEgressTCPSyn_" + securityRuleProtcol;
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

    public void egressACLPermitAllProto(Long dpidLong, String segmentationId, String attachedMac,
                                        boolean write, String securityRuleIpPrefix, Integer protoPortMatchPriority) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId))
                             .build());
        if (securityRuleIpPrefix != null) {
            Ipv4Prefix srcIpPrefix = new Ipv4Prefix(securityRuleIpPrefix);
            flowBuilder.setMatch(MatchUtils
                                 .createSmacIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, srcIpPrefix)
                                 .build());
        } else {
            flowBuilder.setMatch(MatchUtils
                                 .createSmacIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, null)
                                 .build());
        }
        LOG.debug("MatchBuilder contains: {}", flowBuilder.getMatch());
        String flowId = "Egress_Proto_ACL" + segmentationId + "_" +
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


    public void egressACLTcpSyn(Long dpidLong, String segmentationId, String attachedMac, boolean write,
                                Integer securityRulePortMin, Integer protoPortMatchPriority) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        PortNumber tcpPort = new PortNumber(securityRulePortMin);
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils.createSmacTcpSyn(matchBuilder, attachedMac, tcpPort,
                                                         Constants.TCP_SYN, segmentationId).build());

        LOG.debug("MatchBuilder contains: {}", flowBuilder.getMatch());
        String flowId = "Ucast_this.getTable()" + segmentationId + "_" + attachedMac + securityRulePortMin;
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

            LOG.debug("Instructions contain: {}", ib.getInstruction());
            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    /**
     * Adds flow to allow any DHCP client traffic.
     *
     * @param dpidLong the dpid
     * @param write whether to write or delete the flow
     * @param protoPortMatchPriority the priority
     */
    private void egressAclDhcpAllowClientTrafficFromVm(Long dpidLong,
                                                       boolean write, Integer protoPortMatchPriority) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);

        MatchUtils.createDhcpMatch(matchBuilder, DHCP_DESTINATION_PORT, DHCP_SOURCE_PORT).build();
        LOG.debug("egressAclDHCPAllowClientTrafficFromVm: MatchBuilder contains: {}", matchBuilder);
        String flowId = "Egress_DHCP_Client"  + "_Permit_";
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);
    }

    /**
     * Adds rule to prevent DHCP spoofing by the vm attached to the port.
     *
     * @param dpidLong the dpid
     * @param localPort the local port
     * @param write is write or delete
     * @param protoPortMatchPriority  the priority
     */
    private void egressAclDhcpDropServerTrafficfromVm(Long dpidLong, long localPort,
                                                      boolean write, Integer protoPortMatchPriority) {

        MatchBuilder matchBuilder = new MatchBuilder();
        //FlowBuilder flowBuilder = new FlowBuilder();
        MatchUtils.createInPortMatch(matchBuilder, dpidLong, localPort);
        MatchUtils.createDhcpMatch(matchBuilder, DHCP_SOURCE_PORT, DHCP_DESTINATION_PORT).build();
        LOG.debug("egressAclDHCPDropServerTrafficfromVM: MatchBuilder contains: {}", matchBuilder);
        String flowId = "Egress_DHCP_Server" + "_" + localPort + "_DROP_";
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, true);

    }

    /**
     * Adds rule to check legitimate ip/mac pair for each packet from the vm.
     *
     * @param dpidLong the dpid
     * @param localPort the local port
     * @param srcIp the vm ip address
     * @param attachedMac the vm mac address
     * @param protoPortMatchPriority  the priority
     * @param write is write or delete
     */
    private void egressAclAllowTrafficFromVmIpMacPair(Long dpidLong, long localPort,
                                                      String attachedMac, String srcIp,
                                                      Integer protoPortMatchPriority, boolean write) {
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createSrcL3Ipv4MatchWithMac(matchBuilder, new Ipv4Prefix(srcIp),new MacAddress(attachedMac));
        MatchUtils.createInPortMatch(matchBuilder, dpidLong, localPort);
        LOG.debug("egressAclAllowTrafficFromVmIpMacPair: MatchBuilder contains: {}", matchBuilder);
        String flowId = "Egress_Allow_VM_IP_MAC" + "_" + localPort + attachedMac + "_Permit_";
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
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
            ib.setKey(new InstructionKey(0));
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();
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
        super.setDependencies(bundleContext.getServiceReference(EgressAclProvider.class.getName()), this);
        securityServicesManager =
                (SecurityServicesManager) ServiceHelper.getGlobalInstance(SecurityServicesManager.class, this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
