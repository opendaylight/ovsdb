/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import com.google.common.collect.Lists;

import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.EgressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityGroupCacheManger;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
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

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class EgressAclService extends AbstractServiceInstance implements EgressAclProvider, ConfigInterface {

    private static final Logger LOG = LoggerFactory.getLogger(EgressAclService.class);
    private volatile SecurityServicesManager securityServicesManager;
    private volatile SecurityGroupCacheManger securityGroupCacheManger;
    private static final int DHCP_SOURCE_PORT = 67;
    private static final int DHCP_DESTINATION_PORT = 68;
    private static final String HOST_MASK = "/32";
    private static final int PORT_RANGE_MIN = 1;
    private static final int PORT_RANGE_MAX = 65535;

    public EgressAclService() {
        super(Service.EGRESS_ACL);
    }

    public EgressAclService(Service service) {
        super(service);
    }

    @Override
    public void programPortSecurityGroup(Long dpid, String segmentationId, String attachedMac, long localPort,
                                       NeutronSecurityGroup securityGroup, String portUuid, boolean write) {

        LOG.trace("programPortSecurityGroup: neutronSecurityGroup: {} ", securityGroup);
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

            if (portSecurityRule == null ||
                    portSecurityRule.getSecurityRuleEthertype() == null ||
                    portSecurityRule.getSecurityRuleDirection() == null) {
                continue;
            }

            if ("IPv4".equals(portSecurityRule.getSecurityRuleEthertype())
                    && portSecurityRule.getSecurityRuleDirection().equals("egress")) {
                LOG.debug("programPortSecurityGroup: Acl Rule matching IPv4 and ingress is: {} ", portSecurityRule);
                if (null != portSecurityRule.getSecurityRemoteGroupID()) {
                    //Remote Security group is selected
                    List<Neutron_IPs> remoteSrcAddressList = securityServicesManager
                            .getVmListForSecurityGroup(portUuid,portSecurityRule.getSecurityRemoteGroupID());
                    if (null != remoteSrcAddressList) {
                        for (Neutron_IPs vmIp :remoteSrcAddressList ) {

                            programPortSecurityRule(dpid, segmentationId, attachedMac,
                                                    localPort, portSecurityRule, vmIp, write);
                        }
                        if (write) {
                            securityGroupCacheManger.addToCache(portSecurityRule.getSecurityRemoteGroupID(), portUuid);
                        } else {
                            securityGroupCacheManger.removeFromCache(portSecurityRule.getSecurityRemoteGroupID(),
                                                                     portUuid);
                        }
                    }
                } else {
                    programPortSecurityRule(dpid, segmentationId, attachedMac, localPort,
                                            portSecurityRule, null, write);
                }
                if (write) {
                    securityGroupCacheManger.portAdded(securityGroup.getSecurityGroupUUID(), portUuid);
                } else {
                    securityGroupCacheManger.portRemoved(securityGroup.getSecurityGroupUUID(), portUuid);
                }
            }
        }
    }

    @Override
    public void programPortSecurityRule(Long dpid, String segmentationId, String attachedMac,
                                        long localPort, NeutronSecurityRule portSecurityRule,
                                        Neutron_IPs vmIp, boolean write) {
        if (null == portSecurityRule.getSecurityRuleProtocol()) {
            /* TODO Rework on the priority values */
            egressAclIPv4(dpid, segmentationId, attachedMac,
                          write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
        } else {
            String ipaddress = null;
            if (null != vmIp) {
                ipaddress = vmIp.getIpAddress();
                try {
                    InetAddress address = InetAddress.getByName(ipaddress);
                    // TODO: remove this when ipv6 support is implemented
                    if (address instanceof Inet6Address) {
                        LOG.debug("Skipping ip address {}. IPv6 support is not yet implemented.", address);
                        return;
                    }
                } catch (UnknownHostException e) {
                    LOG.warn("Invalid ip address {}", ipaddress, e);
                    return;
                }
            }

            if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
                String prefixStr = portSecurityRule.getSecurityRuleRemoteIpPrefix();
                try {
                    IpPrefix ipPrefix = IpPrefixBuilder.getDefaultInstance(prefixStr);
                    // TODO: remove this when ipv6 support is implemented
                    if (ipPrefix.getIpv6Prefix() != null) {
                        LOG.debug("Skipping ip prefix {}. IPv6 support is not yet implemented.", ipPrefix);
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    LOG.warn("Invalid ip prefix {}", prefixStr, e);
                    return;
                }
            }

            switch (portSecurityRule.getSecurityRuleProtocol()) {
              case MatchUtils.TCP:
                  LOG.debug("programPortSecurityRule: Rule matching TCP", portSecurityRule);
                  egressAclTcp(dpid, segmentationId, attachedMac,
                               portSecurityRule,ipaddress, write,
                               Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                  break;
              case MatchUtils.UDP:
                  LOG.debug("programPortSecurityRule: Rule matching UDP", portSecurityRule);
                  egressAclUdp(dpid, segmentationId, attachedMac,
                               portSecurityRule, ipaddress, write,
                               Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                  break;
              case MatchUtils.ICMP:
                  LOG.debug("programPortSecurityRule: Rule matching ICMP", portSecurityRule);
                  egressAclIcmp(dpid, segmentationId, attachedMac,
                                portSecurityRule, ipaddress,write,
                                Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                  break;
              default:
                  LOG.info("programPortSecurityAcl: Protocol is not TCP/UDP/ICMP but other " +
                          "protocol = ", portSecurityRule.getSecurityRuleProtocol());
                  egressOtherProtocolAclHandler(dpid, segmentationId, attachedMac,
                                      portSecurityRule, ipaddress, write,
                                      Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                  break;
            }
        }
    }

    private void egressOtherProtocolAclHandler(Long dpidLong, String segmentationId, String srcMac,
                                               NeutronSecurityRule portSecurityRule, String dstAddress,
                                               boolean write, Integer priority) {
        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Egress_Other_" + segmentationId + "_" + srcMac + "_";
        matchBuilder = MatchUtils.createEtherMatchWithType(matchBuilder,srcMac,null);

        short proto = 0;
        try {
            Integer protocol = new Integer(portSecurityRule.getSecurityRuleProtocol());
            proto = protocol.shortValue();
            flowId = flowId + proto;
        } catch (NumberFormatException e) {
            LOG.error("Protocol vlaue conversion failure", e);
        }
        matchBuilder = MatchUtils.createIpProtocolMatch(matchBuilder, proto);

        if (null != dstAddress) {
            flowId = flowId + dstAddress;
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder, null,
                                                         MatchUtils.iPv4PrefixFromIPv4Address(dstAddress));

        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder, null,
                    new Ipv4Prefix(portSecurityRule.getSecurityRuleRemoteIpPrefix()));
        }
        flowId = flowId + "_Permit";
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        syncFlow(flowId, nodeBuilder, matchBuilder, priority, write, false);
    }

    @Override
    public void programFixedSecurityGroup(Long dpid, String segmentationId, String attachedMac,
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
                try {
                    InetAddress address = InetAddress.getByName(srcAddress.getIpAddress());
                    if (address instanceof Inet4Address) {
                        String addressWithPrefix = srcAddress.getIpAddress() + HOST_MASK;
                        egressAclAllowTrafficFromVmIpMacPair(dpid, localPort, attachedMac, addressWithPrefix,
                                                             Constants.PROTO_VM_IP_MAC_MATCH_PRIORITY,write);
                    } else {
                        LOG.debug("Skipping IPv6 address {}. IPv6 support is not yet implemented.",
                                  srcAddress.getIpAddress());
                    }
                } catch(UnknownHostException e) {
                    LOG.warn("Invalid IP address {}", srcAddress.getIpAddress());
                }
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
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        MatchBuilder matchBuilder = new MatchBuilder();
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
        boolean portRange = false;
        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Egress_TCP_" + segmentationId + "_" + srcMac + "_";
        matchBuilder = MatchUtils.createEtherMatchWithType(matchBuilder,srcMac,null);

        /* Custom TCP Match */
        if (portSecurityRule.getSecurityRulePortMin().equals(portSecurityRule.getSecurityRulePortMax())) {
            flowId = flowId + portSecurityRule.getSecurityRulePortMin() + "_";
            matchBuilder = MatchUtils.addLayer4Match(matchBuilder, MatchUtils.TCP_SHORT, 0,
                                                     portSecurityRule.getSecurityRulePortMin());
        } else {
            /* All TCP Match */
            if (portSecurityRule.getSecurityRulePortMin().equals(PORT_RANGE_MIN)
                    && portSecurityRule.getSecurityRulePortMax().equals(PORT_RANGE_MAX)) {
                flowId = flowId + portSecurityRule.getSecurityRulePortMin() + "_"
                            + portSecurityRule.getSecurityRulePortMax() + "_";
                matchBuilder = MatchUtils.addLayer4Match(matchBuilder, MatchUtils.TCP_SHORT, 0, 0);
            } else {
                portRange = true;
            }
        }
        if (null != dstAddress) {
            flowId = flowId + dstAddress;
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,null,
                                      MatchUtils.iPv4PrefixFromIPv4Address(dstAddress));

        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,null,
                                      new Ipv4Prefix(portSecurityRule.getSecurityRuleRemoteIpPrefix()));
        }
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        if (portRange) {
            Map<Integer, Integer> portMaskMap = MatchUtils
                    .getLayer4MaskForRange(portSecurityRule.getSecurityRulePortMin(),
                                           portSecurityRule.getSecurityRulePortMax());
            for (Integer port: portMaskMap.keySet()) {
                String rangeflowId = flowId + port + "_" + portMaskMap.get(port) + "_";
                rangeflowId = rangeflowId + "_Permit";
                MatchUtils.addLayer4MatchWithMask(matchBuilder, MatchUtils.TCP_SHORT,
                                                  0, port, portMaskMap.get(port));
                syncFlow(rangeflowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);
            }
        } else {
            flowId = flowId + "_Permit";
            syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);
        }
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
     * @param protoPortMatchPriority the protocol match priority
     */
    private void egressAclIcmp(Long dpidLong, String segmentationId, String srcMac,
                               NeutronSecurityRule portSecurityRule, String dstAddress,
                               boolean write, Integer protoPortMatchPriority) {

        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Egress_ICMP_" + segmentationId + "_" + srcMac + "_";
        matchBuilder = MatchUtils.createEtherMatchWithType(matchBuilder,srcMac,null);
        /*Custom ICMP Match */
        if (portSecurityRule.getSecurityRulePortMin() != null &&
                             portSecurityRule.getSecurityRulePortMax() != null) {
            flowId = flowId + portSecurityRule.getSecurityRulePortMin().shortValue() + "_"
                    + portSecurityRule.getSecurityRulePortMax().shortValue() + "_";
            matchBuilder = MatchUtils.createICMPv4Match(matchBuilder,
                    portSecurityRule.getSecurityRulePortMin().shortValue(),
                    portSecurityRule.getSecurityRulePortMax().shortValue());
        } else {
            /* All ICMP Match */ // We are getting from neutron NULL for both min and max
            flowId = flowId + "all" + "_" ;
            matchBuilder = MatchUtils.createICMPv4Match(matchBuilder, MatchUtils.ALL_ICMP, MatchUtils.ALL_ICMP);
        }
        if (null != dstAddress) {
            flowId = flowId + dstAddress;
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,null,
                    MatchUtils.iPv4PrefixFromIPv4Address(dstAddress));
        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,null,
                    new Ipv4Prefix(portSecurityRule.getSecurityRuleRemoteIpPrefix()));
        }
        flowId = flowId + "_Permit";
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
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
        boolean portRange = false;
        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Egress_UDP_" + segmentationId + "_" + srcMac + "_";
        matchBuilder = MatchUtils.createEtherMatchWithType(matchBuilder,srcMac,null);

        /* Custom UDP Match */
        if (portSecurityRule.getSecurityRulePortMin().equals(portSecurityRule.getSecurityRulePortMax())) {
            flowId = flowId + portSecurityRule.getSecurityRulePortMin() + "_";
            matchBuilder = MatchUtils.addLayer4Match(matchBuilder, MatchUtils.UDP_SHORT, 0,
                                                     portSecurityRule.getSecurityRulePortMin());
        } else {
            /* All UDP Match */
            if (portSecurityRule.getSecurityRulePortMin().equals(PORT_RANGE_MIN)
                    && portSecurityRule.getSecurityRulePortMax().equals(PORT_RANGE_MAX)) {
                flowId = flowId + portSecurityRule.getSecurityRulePortMin() + "_"
                    + portSecurityRule.getSecurityRulePortMax() + "_";
                matchBuilder = MatchUtils.addLayer4Match(matchBuilder, MatchUtils.UDP_SHORT, 0, 0);
            } else {
                portRange = true;
            }
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
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpidLong;
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        if (portRange) {
            Map<Integer, Integer> portMaskMap = MatchUtils
                    .getLayer4MaskForRange(portSecurityRule.getSecurityRulePortMin(),
                                           portSecurityRule.getSecurityRulePortMax());
            for (Integer port: portMaskMap.keySet()) {
                String rangeflowId = flowId + port + "_" + portMaskMap.get(port) + "_";
                rangeflowId = rangeflowId + "_Permit";
                MatchUtils.addLayer4MatchWithMask(matchBuilder, MatchUtils.UDP_SHORT,
                                                  0, port, portMaskMap.get(port));
                syncFlow(rangeflowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);
            }
        } else {
            flowId = flowId + "_Permit";
            syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false);
        }
    }

    public void egressACLDefaultTcpDrop(Long dpidLong, String segmentationId, String attachedMac,
                                        int priority, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "TCP_Syn_Egress_Default_Drop_" + segmentationId + "_" + attachedMac;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createSmacTcpPortWithFlagMatch(matchBuilder, attachedMac, Constants.TCP_SYN, segmentationId);
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();

            InstructionUtils.createDropInstructions(ib);
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

    public void egressACLTcpPortWithPrefix(Long dpidLong, String segmentationId, String attachedMac, boolean write,
                                           Integer securityRulePortMin, String securityRuleIpPrefix,
                                           Integer priority) {
        PortNumber tcpPort = new PortNumber(securityRulePortMin);
        Ipv4Prefix srcIpPrefix = new Ipv4Prefix(securityRuleIpPrefix);

        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "UcastEgress_" + segmentationId + "_" + attachedMac
                + securityRulePortMin + securityRuleIpPrefix;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createSmacTcpSynDstIpPrefixTcpPort(matchBuilder, new MacAddress(attachedMac),
                        tcpPort, Constants.TCP_SYN, segmentationId, srcIpPrefix);
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();

            InstructionBuilder ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);

            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    public void egressAllowProto(Long dpidLong, String segmentationId, String attachedMac, boolean write,
                                 String securityRuleProtcol, Integer priority) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "EgressAllProto_" + segmentationId + "_"
                + attachedMac + "_AllowEgressTCPSyn_" + securityRuleProtcol;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createDmacIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, null);
        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();

            InstructionBuilder ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);

            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    public void egressACLPermitAllProto(Long dpidLong, String segmentationId, String attachedMac,
                                        boolean write, String securityRuleIpPrefix, Integer priority) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "Egress_Proto_ACL" + segmentationId + "_" +
                attachedMac + "_Permit_" + securityRuleIpPrefix;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
        if (securityRuleIpPrefix != null) {
            Ipv4Prefix srcIpPrefix = new Ipv4Prefix(securityRuleIpPrefix);
            MatchUtils.createSmacIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, srcIpPrefix);
        } else {
            MatchUtils.createSmacIpTcpSynMatch(matchBuilder, new MacAddress(attachedMac), null, null);
        }
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();

            InstructionBuilder ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);

            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    public void egressACLTcpSyn(Long dpidLong, String segmentationId, String attachedMac, boolean write,
                                Integer securityRulePortMin, Integer priority) {
        PortNumber tcpPort = new PortNumber(securityRulePortMin);

        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "Ucast_this.getTable()" + segmentationId + "_" + attachedMac + securityRulePortMin;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createSmacTcpSyn(matchBuilder, attachedMac, tcpPort, Constants.TCP_SYN, segmentationId);
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();

            InstructionBuilder ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);

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
     * @param priority the priority
     */
    private void egressAclDhcpAllowClientTrafficFromVm(Long dpidLong,
                                                       boolean write, Integer priority) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        String flowName = "Egress_DHCP_Client"  + "_Permit_";
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createDhcpMatch(matchBuilder, DHCP_DESTINATION_PORT, DHCP_SOURCE_PORT);
        syncFlow(flowName, nodeBuilder, matchBuilder, priority, write, false);
    }

    /**
     * Adds rule to prevent DHCP spoofing by the vm attached to the port.
     *
     * @param dpidLong the dpid
     * @param localPort the local port
     * @param write is write or delete
     * @param priority  the priority
     */
    private void egressAclDhcpDropServerTrafficfromVm(Long dpidLong, long localPort,
                                                      boolean write, Integer priority) {

        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        String flowName = "Egress_DHCP_Server" + "_" + localPort + "_DROP_";
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createInPortMatch(matchBuilder, dpidLong, localPort);
        MatchUtils.createDhcpMatch(matchBuilder, DHCP_SOURCE_PORT, DHCP_DESTINATION_PORT);
        syncFlow(flowName, nodeBuilder, matchBuilder, priority, write, true);
    }

    /**
     * Adds rule to check legitimate ip/mac pair for each packet from the vm.
     *
     * @param dpidLong the dpid
     * @param localPort the local port
     * @param srcIp the vm ip address
     * @param attachedMac the vm mac address
     * @param priority  the priority
     * @param write is write or delete
     */
    private void egressAclAllowTrafficFromVmIpMacPair(Long dpidLong, long localPort,
                                                      String attachedMac, String srcIp,
                                                      Integer priority, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        String flowName = "Egress_Allow_VM_IP_MAC" + "_" + localPort + attachedMac + "_Permit_";
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createSrcL3Ipv4MatchWithMac(matchBuilder, new Ipv4Prefix(srcIp),new MacAddress(attachedMac));
        MatchUtils.createInPortMatch(matchBuilder, dpidLong, localPort);
        LOG.debug("egressAclAllowTrafficFromVmIpMacPair: MatchBuilder contains: {}", matchBuilder);
        syncFlow(flowName, nodeBuilder, matchBuilder, priority, write, false);
    }

    /**
     * Add or remove flow to the node.
     *
     * @param flowName the the flow id
     * @param nodeBuilder the node builder
     * @param matchBuilder the matchbuilder
     * @param priority the protocol priority
     * @param write whether it is a write
     * @param drop whether it is a drop or forward
     */
    private void syncFlow(String flowName, NodeBuilder nodeBuilder,
                          MatchBuilder matchBuilder, Integer priority,
                          boolean write, boolean drop) {
        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(matchBuilder.build());
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);

        if (write) {
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
        securityGroupCacheManger =
                (SecurityGroupCacheManger) ServiceHelper.getGlobalInstance(SecurityGroupCacheManger.class, this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
