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
import org.opendaylight.ovsdb.openstack.netvirt.api.IngressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityGroupCacheManger;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class IngressAclService extends AbstractServiceInstance implements IngressAclProvider, ConfigInterface {
    private static final Logger LOG = LoggerFactory.getLogger(IngressAclService.class);
    private volatile SecurityServicesManager securityServicesManager;
    private volatile SecurityGroupCacheManger securityGroupCacheManger;
    private static final int PORT_RANGE_MIN = 1;
    private static final int PORT_RANGE_MAX = 65535;

    public IngressAclService() {
        super(Service.INGRESS_ACL);
    }

    public IngressAclService(Service service) {
        super(service);
    }

    @Override
    public void programPortSecurityGroup(Long dpid, String segmentationId, String attachedMac,
                                       long localPort, NeutronSecurityGroup securityGroup,
                                       String portUuid, boolean write) {

        LOG.trace("programPortSecurityGroup neutronSecurityGroup: {} ", securityGroup);
        if (securityGroup == null || securityGroup.getSecurityRules() == null) {
            return;
        }

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

            if (portSecurityRule == null ||
                    portSecurityRule.getSecurityRuleEthertype() == null ||
                    portSecurityRule.getSecurityRuleDirection() == null) {
                continue;
            }

            if ("ingress".equals(portSecurityRule.getSecurityRuleDirection())) {
                LOG.debug("programPortSecurityGroup: Rule matching IP and ingress is: {} ", portSecurityRule);
                if (null != portSecurityRule.getSecurityRemoteGroupID()) {
                    //Remote Security group is selected
                    List<Neutron_IPs> remoteSrcAddressList = securityServicesManager
                            .getVmListForSecurityGroup(portUuid,portSecurityRule.getSecurityRemoteGroupID());
                    if (null != remoteSrcAddressList) {
                        for (Neutron_IPs vmIp :remoteSrcAddressList ) {
                            programPortSecurityRule(dpid, segmentationId, attachedMac, localPort,
                                                    portSecurityRule, vmIp, write);
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
            boolean isIpv6 = portSecurityRule.getSecurityRuleEthertype().equals("IPv6");
            ingressAclIP(dpid, isIpv6, segmentationId, attachedMac,
                         write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
        } else {
            String ipaddress = null;
            if (null != vmIp) {
                ipaddress = vmIp.getIpAddress();
            } 

            switch (portSecurityRule.getSecurityRuleProtocol()) {
              case MatchUtils.TCP:
                  LOG.debug("programPortSecurityRule: Rule matching TCP", portSecurityRule);
                  ingressAclTcp(dpid, segmentationId, attachedMac, portSecurityRule, ipaddress,
                              write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                  break;
              case MatchUtils.UDP:
                  LOG.debug("programPortSecurityRule: Rule matching UDP", portSecurityRule);
                  ingressAclUdp(dpid, segmentationId, attachedMac, portSecurityRule, ipaddress,
                                write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                  break;
              case MatchUtils.ICMP:
              case MatchUtils.ICMPV6:
                  LOG.debug("programPortSecurityRule: Rule matching ICMP", portSecurityRule);
                  ingressAclIcmp(dpid, segmentationId, attachedMac, portSecurityRule, ipaddress,
                                 write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                  break;
              default:
                  LOG.info("programPortSecurityAcl: Protocol is not TCP/UDP/ICMP but other " +
                          "protocol = ", portSecurityRule.getSecurityRuleProtocol());
                  ingressOtherProtocolAclHandler(dpid, segmentationId, attachedMac, portSecurityRule,
                              null, write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
                  break;
            }
        }
    }

    private void ingressOtherProtocolAclHandler(Long dpidLong, String segmentationId, String dstMac,
          NeutronSecurityRule portSecurityRule, String srcAddress,
          boolean write, Integer protoPortMatchPriority) {

          MatchBuilder matchBuilder = new MatchBuilder();
          String flowId = "Ingress_Other_" + segmentationId + "_" + dstMac + "_";
          matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,null,dstMac);
          short proto = 0;
          try {
              Integer protocol = new Integer(portSecurityRule.getSecurityRuleProtocol());
              proto = protocol.shortValue();
              flowId = flowId + proto;
          } catch (NumberFormatException e) {
              LOG.error("Protocol vlaue conversion failure", e);
          }
          matchBuilder = MatchUtils.createIpProtocolMatch(matchBuilder, proto);
          if (null != srcAddress) {
              flowId = flowId + srcAddress;
              matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                                        MatchUtils.iPv4PrefixFromIPv4Address(srcAddress), null);
          } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
              flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
              matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                                        new Ipv4Prefix(portSecurityRule.getSecurityRuleRemoteIpPrefix()),null);
          }
          NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
          flowId = flowId + "_Permit";
          syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, securityServicesManager.isConntrackEnabled());
    }

    @Override
    public void programFixedSecurityGroup(Long dpid, String segmentationId, String dhcpMacAddress,
                                        long localPort, boolean isLastPortinSubnet,
                                        boolean isComputePort, String attachMac, boolean write) {
        //If this port is the only port in the compute node add the DHCP server rule.
        if (isLastPortinSubnet && isComputePort ) {
            ingressAclDhcpAllowServerTraffic(dpid, segmentationId,dhcpMacAddress,
                                             write,Constants.PROTO_DHCP_SERVER_MATCH_PRIORITY);
            ingressAclDhcpv6AllowServerTraffic(dpid, segmentationId,dhcpMacAddress,
                                               write,Constants.PROTO_DHCP_SERVER_MATCH_PRIORITY);
        }
        if (isComputePort) {
            if (securityServicesManager.isConntrackEnabled()) {
                programIngressAclFixedConntrackRule(dpid, segmentationId, attachMac, localPort, write);
            }
            programArpRule(dpid, segmentationId, localPort, attachMac, write);
        }
    }

    private void programArpRule(Long dpid, String segmentationId, long localPort, String attachMac, boolean write) {
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        String flowId = "Ingress_ARP_" + segmentationId + "_" + localPort + "_";
        EthernetMatchBuilder ethernetType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0806L));
        ethernetType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetType.build());

        ArpMatchBuilder arpDstMatch = new ArpMatchBuilder();
        ArpTargetHardwareAddressBuilder arpDst = new ArpTargetHardwareAddressBuilder();
        arpDst.setAddress(new MacAddress(attachMac));
        arpDstMatch.setArpTargetHardwareAddress(arpDst.build());
        matchBuilder.setLayer3Match(arpDstMatch.build());
        syncFlow(flowId, nodeBuilder, matchBuilder, Constants.PROTO_MATCH_PRIORITY, write, false, securityServicesManager.isConntrackEnabled());
    }

    private void programIngressAclFixedConntrackRule(Long dpid,
           String segmentationId, String attachMac, long localPort, boolean write) {
        try {
            String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;
            programConntrackUntrackRule(nodeName, segmentationId, localPort, attachMac,
                                        Constants.CT_STATE_UNTRACKED_PRIORITY, write );
            programConntrackTrackedPlusEstRule(nodeName, segmentationId, localPort, attachMac,
                                        Constants.CT_STATE_TRACKED_EST_PRIORITY, write );
            programConntrackNewDropRule(nodeName, segmentationId, localPort, attachMac,
                                             Constants.CT_STATE_NEW_PRIORITY_DROP, write );
            LOG.info("programIngressAclFixedConntrackRule :  default connection tracking rule are added.");
        } catch (Exception e) {
            LOG.error("Failed to add default conntrack rules : " , e);
        }
    }

    private void programConntrackUntrackRule(String nodeName, String segmentationId,
                                             long localPort, String attachMac, Integer priority, boolean write) {
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        String flowName = "Ingress_Fixed_Conntrk_Untrk_" + segmentationId + "_" + localPort + "_";
        matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,null,attachMac);
        matchBuilder = MatchUtils.addCtState(matchBuilder,0x00, 0x80);
        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(matchBuilder.build());
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);
        if (write) {
            InstructionBuilder ib = new InstructionBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();
            InstructionsBuilder isb = new InstructionsBuilder();
            ActionBuilder ab = new ActionBuilder();
            ab.setAction(ActionUtils.nxConntrackAction(0, 0L, 0, (short)0x0));
            // 0xff means no table, 0x0 is table = 0
            // nxConntrackAction(Integer flags, Long zoneSrc,Integer conntrackZone, Short recircTable)
            ab.setOrder(0);
            ab.setKey(new ActionKey(0));
            List<Action> actionList = Lists.newArrayList();
            actionList.add(ab.build());
            ApplyActionsBuilder aab = new ApplyActionsBuilder();
            aab.setAction(actionList);

            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
            instructionsList.add(ib.build());
            isb.setInstruction(instructionsList);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
            LOG.info("INGRESS:default programConntrackUntrackRule() flows are written");
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    private void programConntrackTrackedPlusEstRule(String nodeName, String segmentationId,
                                                  long localPort, String attachMac,Integer priority, boolean write) {
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        String flowName = "Ingress_Fixed_Conntrk_TrkEst_" + segmentationId + "_" + localPort + "_";
        matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,null,attachMac);
        matchBuilder = MatchUtils.addCtState(matchBuilder,0x82, 0x82);
        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(matchBuilder.build());
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);
        if (write) {
            InstructionBuilder ib = new InstructionBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();
            InstructionsBuilder isb = new InstructionsBuilder();

            ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
             isb.setInstruction(instructionsList);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
            LOG.info("INGRESS:default programConntrackTrackedPlusEstRule() flows are written");
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    private void programConntrackNewDropRule(String nodeName, String segmentationId,
                                             long localPort, String attachMac, Integer priority, boolean write) {
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        String flowName = "Ingress_Fixed_Conntrk_NewDrop_" + segmentationId + "_" + localPort + "_";
        matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,null,attachMac);
        matchBuilder = MatchUtils.addCtState(matchBuilder,0x01, 0x01);
        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(matchBuilder.build());
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);
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
            LOG.info("INGRESS:default programConntrackNewDropRule flows are written");
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    /**
     * Allows an IPv4/v6 packet ingress to the destination mac address.
     * @param dpidLong the dpid
     * @param segmentationId the segementation id
     * @param dstMac the destination mac address
     * @param write add or remove
     * @param protoPortMatchPriority the protocol match priority.
     */
    private void ingressAclIP(Long dpidLong, boolean isIpv6, String segmentationId, String dstMac,
                              boolean write, Integer protoPortMatchPriority ) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Ingress_IP" + segmentationId + "_" + dstMac + "_Permit_";
        if (isIpv6) {
            matchBuilder = MatchUtils.createV6EtherMatchWithType(matchBuilder,null,dstMac);
        }else {
            matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,null,dstMac);
        }
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, securityServicesManager.isConntrackEnabled());
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
        boolean portRange = false;
        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Ingress_TCP_" + segmentationId + "_" + dstMac + "_";
        boolean isIpv6 = portSecurityRule.getSecurityRuleEthertype().equals("IPv6");
        if (isIpv6) {
            matchBuilder = MatchUtils.createV6EtherMatchWithType(matchBuilder,null,dstMac);
        } else {
            matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,null,dstMac);
        }

        /* Custom TCP Match*/
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
        if (null != srcAddress) {
            flowId = flowId + srcAddress;
            if (isIpv6) {
                matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder,
                        MatchUtils.iPv6PrefixFromIPv6Address(srcAddress),null);
            } else {
                matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                        MatchUtils.iPv4PrefixFromIPv4Address(srcAddress),null);
            }
        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            if (isIpv6) {
                matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder,
                        new Ipv6Prefix(portSecurityRule
                                       .getSecurityRuleRemoteIpPrefix()),null);
            } else {
                matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                        new Ipv4Prefix(portSecurityRule
                                       .getSecurityRuleRemoteIpPrefix()),null);
            }
        }
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        if (portRange) {
            Map<Integer, Integer> portMaskMap = MatchUtils
                    .getLayer4MaskForRange(portSecurityRule.getSecurityRulePortMin(),
                                           portSecurityRule.getSecurityRulePortMax());
            for (Integer port: portMaskMap.keySet()) {
                String rangeflowId = flowId + port + "_" + portMaskMap.get(port) + "_";
                rangeflowId = rangeflowId + "_Permit";
                MatchUtils.addLayer4MatchWithMask(matchBuilder, MatchUtils.TCP_SHORT,
                                                  0, port, portMaskMap.get(port));
                syncFlow(rangeflowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, securityServicesManager.isConntrackEnabled());
            }
        } else {
            flowId = flowId + "_Permit";
            syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, securityServicesManager.isConntrackEnabled());
        }
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
        boolean portRange = false;
        boolean isIpv6 = portSecurityRule.getSecurityRuleEthertype().equals("IPv6");
        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Ingress_UDP_" + segmentationId + "_" + dstMac + "_";
        if (isIpv6)  {
            matchBuilder = MatchUtils.createV6EtherMatchWithType(matchBuilder,null,dstMac);
        }else {
            matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,null,dstMac);
        }

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
        if (null != srcAddress) {
            flowId = flowId + srcAddress;
            if (isIpv6) {
                matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder,
                        MatchUtils.iPv6PrefixFromIPv6Address(srcAddress), null);
            } else {
                matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                        MatchUtils.iPv4PrefixFromIPv4Address(srcAddress), null);
            }
        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            if (isIpv6) {
                matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder,
                        new Ipv6Prefix(portSecurityRule
                                       .getSecurityRuleRemoteIpPrefix()),null);
            } else {
                matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                        new Ipv4Prefix(portSecurityRule
                                       .getSecurityRuleRemoteIpPrefix()),null);
            }
        }
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        if (portRange) {
            Map<Integer, Integer> portMaskMap = MatchUtils
                    .getLayer4MaskForRange(portSecurityRule.getSecurityRulePortMin(),
                                           portSecurityRule.getSecurityRulePortMax());
            for (Integer port: portMaskMap.keySet()) {
                String rangeflowId = flowId + port + "_" + portMaskMap.get(port) + "_";
                rangeflowId = rangeflowId + "_Permit";
                MatchUtils.addLayer4MatchWithMask(matchBuilder, MatchUtils.UDP_SHORT,
                                                   0, port, portMaskMap.get(port));
                syncFlow(rangeflowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, securityServicesManager.isConntrackEnabled());
            }
        } else {
            flowId = flowId + "_Permit";
            syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, securityServicesManager.isConntrackEnabled());
        }
    }

    private void ingressAclIcmp(Long dpidLong, String segmentationId, String dstMac,
            NeutronSecurityRule portSecurityRule, String srcAddress,
            boolean write, Integer protoPortMatchPriority) {

        boolean isIpv6 = portSecurityRule.getSecurityRuleEthertype().equals("IPv6");
        if (isIpv6) {
            ingressAclIcmpV6(dpidLong, segmentationId, dstMac, portSecurityRule, srcAddress, write, protoPortMatchPriority);
        } else {
            ingressAclIcmpV4(dpidLong, segmentationId, dstMac, portSecurityRule, srcAddress, write, protoPortMatchPriority);
        }
    }

    /**
     * Creates a ingress icmp match to the dst macaddress. If src address is specified
     * source specific match will be created. Otherwise a match with a CIDR will
     * be created.
     * @param dpidLong the dpid
     * @param segmentationId the segmentation id
     * @param dstMac the destination mac address.
     * @param portSecurityRule the security rule in the SG
     * @param srcAddress the destination IP address
     * @param write add or delete
     * @param protoPortMatchPriority the protocol match priority
     */
    private void ingressAclIcmpV4(Long dpidLong, String segmentationId, String dstMac,
                                  NeutronSecurityRule portSecurityRule, String srcAddress,
                                  boolean write, Integer protoPortMatchPriority) {

        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Ingress_ICMP_" + segmentationId + "_" + dstMac + "_";
        matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,null,dstMac);

        /* Custom ICMP Match */
        if (portSecurityRule.getSecurityRulePortMin() != null &&
                portSecurityRule.getSecurityRulePortMax() != null) {
            flowId = flowId + portSecurityRule.getSecurityRulePortMin().shortValue() + "_"
                    + portSecurityRule.getSecurityRulePortMax().shortValue() + "_";
            matchBuilder = MatchUtils.createICMPv4Match(matchBuilder,
                    portSecurityRule.getSecurityRulePortMin().shortValue(),
                    portSecurityRule.getSecurityRulePortMax().shortValue());
        } else {
            /* All ICMP Match */
            flowId = flowId + "all" + "_";
            matchBuilder = MatchUtils.createICMPv4Match(matchBuilder,MatchUtils.ALL_ICMP, MatchUtils.ALL_ICMP);
        }
        if (null != srcAddress) {
            flowId = flowId + srcAddress;
            matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                   MatchUtils.iPv4PrefixFromIPv4Address(srcAddress), null);
        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            if (!portSecurityRule.getSecurityRuleRemoteIpPrefix().contains("/0")) {
                matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,
                                         new Ipv4Prefix(portSecurityRule.getSecurityRuleRemoteIpPrefix()),null);
            }
        }
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        flowId = flowId + "_Permit";
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, securityServicesManager.isConntrackEnabled());
    }

    /**
     * Creates a ingress icmpv6 match to the dst macaddress. If src address is specified
     * source specific match will be created. Otherwise a match with a CIDR will
     * be created.
     * @param dpidLong the dpid
     * @param segmentationId the segmentation id
     * @param dstMac the destination mac address.
     * @param portSecurityRule the security rule in the SG
     * @param srcAddress the destination IP address
     * @param write add or delete
     * @param protoPortMatchPriority the protocol match priority
     */
    private void ingressAclIcmpV6(Long dpidLong, String segmentationId, String dstMac,
                                  NeutronSecurityRule portSecurityRule, String srcAddress,
                                  boolean write, Integer protoPortMatchPriority) {

        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Ingress_ICMP_" + segmentationId + "_" + dstMac + "_";
        matchBuilder = MatchUtils.createV6EtherMatchWithType(matchBuilder,null,dstMac);

        /* Custom ICMP Match */
        if (portSecurityRule.getSecurityRulePortMin() != null &&
                portSecurityRule.getSecurityRulePortMax() != null) {
            flowId = flowId + portSecurityRule.getSecurityRulePortMin().shortValue() + "_"
                    + portSecurityRule.getSecurityRulePortMax().shortValue() + "_";
            matchBuilder = MatchUtils.createICMPv6Match(matchBuilder,
                    portSecurityRule.getSecurityRulePortMin().shortValue(),
                    portSecurityRule.getSecurityRulePortMax().shortValue());
        } else {
            /* All ICMP Match */
            flowId = flowId + "all" + "_";
            matchBuilder = MatchUtils.createICMPv6Match(matchBuilder,MatchUtils.ALL_ICMP, MatchUtils.ALL_ICMP);
        }
        if (null != srcAddress) {
            flowId = flowId + srcAddress;
            matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder,
                    MatchUtils.iPv6PrefixFromIPv6Address(srcAddress), null);
        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder,
                    new Ipv6Prefix(portSecurityRule
                                   .getSecurityRuleRemoteIpPrefix()),null);
        }
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        flowId = flowId + "_Permit";
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, false);
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

        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createDhcpServerMatch(matchBuilder, dhcpMacAddress, 67, 68).build();
        String flowId = "Ingress_DHCP_Server" + segmentationId + "_" + dhcpMacAddress + "_Permit_";
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, false);
    }

    /**
     * Add rule to ensure only DHCPv6 server traffic from the specified mac is allowed.
     *
     * @param dpidLong the dpid
     * @param segmentationId the segmentation id
     * @param dhcpMacAddress the DHCP server mac address
     * @param write is write or delete
     * @param protoPortMatchPriority the priority
     */
    private void ingressAclDhcpv6AllowServerTraffic(Long dpidLong, String segmentationId, String dhcpMacAddress,
                                                    boolean write, Integer protoPortMatchPriority) {

        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createDhcpv6ServerMatch(matchBuilder, dhcpMacAddress, 547, 546).build();
        String flowId = "Ingress_DHCPv6_Server" + segmentationId + "_" + dhcpMacAddress + "_Permit_";
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, false);
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
     * @param isCtCommit commit the connection or CT to track
     */
    private void syncFlow(String flowName, NodeBuilder nodeBuilder,
                          MatchBuilder matchBuilder, Integer priority,
                          boolean write, boolean drop, boolean isCtCommit) {
        MatchBuilder matchBuilder1 = matchBuilder;
        if (isCtCommit) {
            matchBuilder1 = MatchUtils.addCtState(matchBuilder1,0x81, 0x81);
        }
        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(matchBuilder1.build());
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);

        if (write) {
            InstructionBuilder ib = this.getMutablePipelineInstructionBuilder();
            InstructionBuilder ib1 = new InstructionBuilder();
            ActionBuilder ab = new ActionBuilder();
            ApplyActionsBuilder aab = new ApplyActionsBuilder();
            if (drop) {
                InstructionUtils.createDropInstructions(ib);
            }
            ib.setOrder(0);
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();
            ib.setKey(new InstructionKey(0));
            instructionsList.add(ib.build());
            if (isCtCommit) {
                LOG.info("Adding Conntarck rule, flowname = " + flowName);
                ab.setAction(ActionUtils.nxConntrackAction(1, 0L, 0, (short)0xff));
                ab.setOrder(0);
                ab.setKey(new ActionKey(0));
                List<Action> actionList = Lists.newArrayList();
                actionList.add(ab.build());
                aab.setAction(actionList);
                ib1.setOrder(1);
                ib1.setKey(new InstructionKey(1));
                ib1.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
                instructionsList.add(ib1.build());
            }
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
        securityGroupCacheManger =
                (SecurityGroupCacheManger) ServiceHelper.getGlobalInstance(SecurityGroupCacheManger.class, this);
    }

    @Override
    public void setDependencies(Object impl) {

    }
}
