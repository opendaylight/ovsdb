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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
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
    private static final int DHCPV6_SOURCE_PORT = 547;
    private static final int DHCPV6_DESTINATION_PORT = 546;
    private static final String HOST_MASK = "/32";
    private static final String V6_HOST_MASK = "/128";
    private static final String IP_VERSION_4 = "IPv4";
    private static final String IP_VERSION_6 = "IPv6";
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

            if (portSecurityRule.getSecurityRuleDirection().equals("egress")) {
                LOG.debug("programPortSecurityGroup: Acl Rule matching IP and ingress is: {} ", portSecurityRule);
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
        String securityRuleEtherType = portSecurityRule.getSecurityRuleEthertype();
        boolean isIpv6 = securityRuleEtherType.equals(IP_VERSION_6);
        if (!securityRuleEtherType.equals(IP_VERSION_6) && !securityRuleEtherType.equals(IP_VERSION_4)) {
            LOG.debug("programPortSecurityRule: SecurityRuleEthertype {} does not match IPv4/v6.", securityRuleEtherType);
            return;
        }

        if (null == portSecurityRule.getSecurityRuleProtocol()) {
            /* TODO Rework on the priority values */
            egressAclIP(dpid, isIpv6, segmentationId, attachedMac,
                          write, Constants.PROTO_PORT_PREFIX_MATCH_PRIORITY);
        } else {
            String ipaddress = null;
            if (null != vmIp) {
                ipaddress = vmIp.getIpAddress();
                try {
                    InetAddress address = InetAddress.getByName(ipaddress);
                    if ((isIpv6 && (address instanceof Inet4Address)) || (!isIpv6 && address instanceof Inet6Address)) {
                        LOG.debug("programPortSecurityRule: Remote vmIP {} does not match with SecurityRuleEthertype {}.", ipaddress, securityRuleEtherType);
                        return;
                    }
                } catch (UnknownHostException e) {
                    LOG.warn("Invalid IP address {}", ipaddress);
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
              case MatchUtils.ICMPV6:
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
        matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,srcMac,null);

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
        syncFlow(flowId, nodeBuilder, matchBuilder, priority, write, false, securityServicesManager.isConntrackEnabled());
    }

    @Override
    public void programFixedSecurityGroup(Long dpid, String segmentationId, String attachedMac,
                                        long localPort, List<Neutron_IPs> srcAddressList,
                                        boolean isLastPortinBridge, boolean isComputePort ,boolean write) {
        // If it is the only port in the bridge add the rule to allow any DHCP client traffic
        //if (isLastPortinBridge) {
            egressAclDhcpAllowClientTrafficFromVm(dpid, write, Constants.PROTO_DHCP_CLIENT_TRAFFIC_MATCH_PRIORITY);
            egressAclDhcpv6AllowClientTrafficFromVm(dpid, write, Constants.PROTO_DHCP_CLIENT_TRAFFIC_MATCH_PRIORITY);
       // }
        if (isComputePort) {
            programArpRule(dpid, segmentationId, localPort, attachedMac, write);
            if (securityServicesManager.isConntrackEnabled()) {
                programEgressAclFixedConntrackRule(dpid, segmentationId, localPort, attachedMac, write);
            }
            // add rule to drop the DHCP server traffic originating from the vm.
            egressAclDhcpDropServerTrafficfromVm(dpid, localPort, write,
                                                 Constants.PROTO_DHCP_CLIENT_SPOOF_MATCH_PRIORITY_DROP);
            egressAclDhcpv6DropServerTrafficfromVm(dpid, localPort, write,
                                                   Constants.PROTO_DHCP_CLIENT_SPOOF_MATCH_PRIORITY_DROP);
            //Adds rule to check legitimate ip/mac pair for each packet from the vm
            for (Neutron_IPs srcAddress : srcAddressList) {
                try {
                    InetAddress address = InetAddress.getByName(srcAddress.getIpAddress());
                    if (address instanceof Inet4Address) {
                        String addressWithPrefix = srcAddress.getIpAddress() + HOST_MASK;
                        egressAclAllowTrafficFromVmIpMacPair(dpid, localPort, attachedMac, addressWithPrefix,
                                                             Constants.PROTO_VM_IP_MAC_MATCH_PRIORITY,write);
                    } else if (address instanceof Inet6Address) {
                        String addressWithPrefix = srcAddress.getIpAddress() + V6_HOST_MASK;
                        egressAclAllowTrafficFromVmIpV6MacPair(dpid, localPort, attachedMac, addressWithPrefix,
                                                               Constants.PROTO_VM_IP_MAC_MATCH_PRIORITY,write);
                    }
                } catch(UnknownHostException e) {
                    LOG.warn("Invalid IP address {}", srcAddress.getIpAddress());
                }
            }
        }
    }

    private void programArpRule(Long dpid, String segmentationId, long localPort, String attachedMac, boolean write) {
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        String flowId = "Egress_ARP_" + segmentationId + "_" + localPort + "_";

        EthernetMatchBuilder ethernetType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0806L));
        ethernetType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetType.build());

        ArpMatchBuilder arpDstMatch = new ArpMatchBuilder();
        ArpSourceHardwareAddressBuilder arpSrc = new ArpSourceHardwareAddressBuilder();
        arpSrc.setAddress(new MacAddress(attachedMac));
        arpDstMatch.setArpSourceHardwareAddress(arpSrc.build());
        matchBuilder.setLayer3Match(arpDstMatch.build());

        syncFlow(flowId, nodeBuilder, matchBuilder, Constants.PROTO_MATCH_PRIORITY, write, false, false);
    }

    private void programEgressAclFixedConntrackRule(Long dpid,
            String segmentationId, long localPort, String attachMac, boolean write) {
         try {
             String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;
             programConntrackUntrackRule(nodeName, segmentationId, localPort,attachMac,
                                         Constants.CT_STATE_UNTRACKED_PRIORITY, write );
             programConntrackTrackedPlusEstRule(nodeName, dpid, segmentationId, localPort,
                                         Constants.CT_STATE_TRACKED_EST_PRIORITY, write );
             programConntrackNewDropRule(nodeName, dpid, segmentationId, localPort,
                                              Constants.CT_STATE_NEW_PRIORITY_DROP, write );
             LOG.info("programEgressAclFixedConntrackRule :  default connection tracking rule are added.");
         } catch (Exception e) {
             LOG.error("Failed to add default conntrack rules : " , e);
         }
     }

     private void programConntrackUntrackRule(String nodeName, String segmentationId,
                                              long localPort, String attachMac, Integer priority, boolean write) {
         MatchBuilder matchBuilder = new MatchBuilder();
         NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
         String flowName = "Egress_Fixed_Conntrk_Untrk_" + segmentationId + "_" + localPort + "_";
         matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder, attachMac, null);
         matchBuilder = MatchUtils.addCtState(matchBuilder,0x00,0X80);
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
             LOG.info("EGRESS:default programConntrackUntrackRule() flows are written");
         } else {
             removeFlow(flowBuilder, nodeBuilder);
         }
     }

     private void programConntrackTrackedPlusEstRule(String nodeName, Long dpid, String segmentationId,
                                                   long localPort,Integer priority, boolean write) {
         MatchBuilder matchBuilder = new MatchBuilder();
         NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
         String flowName = "Egress_Fixed_Conntrk_TrkEst_" + segmentationId + "_" + localPort + "_";
         matchBuilder = MatchUtils.createInPortMatch(matchBuilder, dpid, localPort);
         matchBuilder = MatchUtils.addCtState(matchBuilder,0x82, 0x82);
         FlowBuilder flowBuilder = new FlowBuilder();
         flowBuilder.setMatch(matchBuilder.build());
         FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);
         if (write) {
             InstructionBuilder ib = new InstructionBuilder();
             List<Instruction> instructionsList = Lists.newArrayList();
             InstructionsBuilder isb = new InstructionsBuilder();
             // got to next table instruction
             ib = this.getMutablePipelineInstructionBuilder();
             ib.setOrder(0);
             ib.setKey(new InstructionKey(0));
             instructionsList.add(ib.build());
             isb.setInstruction(instructionsList);
             flowBuilder.setInstructions(isb.build());
             writeFlow(flowBuilder, nodeBuilder);
             LOG.info("EGRESS:default programConntrackTrackedPlusEstRule() flows are written");
         } else {
             removeFlow(flowBuilder, nodeBuilder);
         }
     }

     private void programConntrackNewDropRule(String nodeName, Long dpid, String segmentationId,
                                              long localPort, Integer priority, boolean write) {
         MatchBuilder matchBuilder = new MatchBuilder();
         NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
         String flowName = "Egress_Fixed_Conntrk_NewDrop_" + segmentationId + "_" + localPort + "_";
         matchBuilder = MatchUtils.createInPortMatch(matchBuilder, dpid, localPort);
         matchBuilder = MatchUtils.addCtState(matchBuilder,0x01, 0x01);
         FlowBuilder flowBuilder = new FlowBuilder();
         flowBuilder.setMatch(matchBuilder.build());
         FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(priority);
         if (write) {
             InstructionBuilder ib = new InstructionBuilder();
             InstructionsBuilder isb = new InstructionsBuilder();
             List<Instruction> instructions = Lists.newArrayList();
             InstructionUtils.createDropInstructions(ib);
             ib.setOrder(0);
             ib.setKey(new InstructionKey(0));
             instructions.add(ib.build());
             isb.setInstruction(instructions);
             LOG.debug("Instructions contain: {}", ib.getInstruction());
             flowBuilder.setInstructions(isb.build());
             writeFlow(flowBuilder, nodeBuilder);
             LOG.info("EGRESS:default programConntrackNewDropRule() flows are written");
         } else {
             removeFlow(flowBuilder, nodeBuilder);
         }
     }

    /**
     * Allows IPv4/v6 packet egress from the src mac address.
     * @param dpidLong the dpid
     * @param segmentationId the segementation id
     * @param srcMac the src mac address
     * @param write add or remove
     * @param protoPortMatchPriority the protocol match priority.
     */
    private void egressAclIP(Long dpidLong, boolean isIpv6, String segmentationId, String srcMac,
                               boolean write, Integer protoPortMatchPriority ) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Egress_IP" + segmentationId + "_" + srcMac + "_Permit_";
        if (isIpv6) {
            matchBuilder = MatchUtils.createV6EtherMatchWithType(matchBuilder,srcMac,null);
        } else {
            matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,srcMac,null);
        }
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, false);
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
        boolean isIpv6 = portSecurityRule.getSecurityRuleEthertype().equals(IP_VERSION_6);
        if (isIpv6) {
            matchBuilder = MatchUtils.createV6EtherMatchWithType(matchBuilder,srcMac,null);
        } else {
            matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,srcMac,null);
        }

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
            if (isIpv6) {
                matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder,null,
                        MatchUtils.iPv6PrefixFromIPv6Address(dstAddress));
            } else {
                matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,null,
                        MatchUtils.iPv4PrefixFromIPv4Address(dstAddress));
            }
        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            if (isIpv6) {
                matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder,null,
                        new Ipv6Prefix(portSecurityRule.getSecurityRuleRemoteIpPrefix()));
            } else {
                matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,null,
                        new Ipv4Prefix(portSecurityRule.getSecurityRuleRemoteIpPrefix()));
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

    private void egressAclIcmp(Long dpidLong, String segmentationId, String srcMac,
            NeutronSecurityRule portSecurityRule, String dstAddress,
            boolean write, Integer protoPortMatchPriority) {

        boolean isIpv6 = portSecurityRule.getSecurityRuleEthertype().equals(IP_VERSION_6);
        if (isIpv6) {
            egressAclIcmpV6(dpidLong, segmentationId, srcMac, portSecurityRule, dstAddress, write, protoPortMatchPriority);
        } else {
            egressAclIcmpV4(dpidLong, segmentationId, srcMac, portSecurityRule, dstAddress, write, protoPortMatchPriority);
        }
    }

    /**
     * Creates a icmp egress match with src macaddress. If dest address is specified
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
    private void egressAclIcmpV4(Long dpidLong, String segmentationId, String srcMac,
                                 NeutronSecurityRule portSecurityRule, String dstAddress,
                                 boolean write, Integer protoPortMatchPriority) {

        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Egress_ICMP_" + segmentationId + "_" + srcMac + "_";
        matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,srcMac,null);
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
            if (!portSecurityRule.getSecurityRuleRemoteIpPrefix().contains("/0")) {
                matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,null,
                    new Ipv4Prefix(portSecurityRule.getSecurityRuleRemoteIpPrefix()));
            }
        }
        flowId = flowId + "_Permit";
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, securityServicesManager.isConntrackEnabled());
    }

    /**
     * Creates a icmpv6 egress match with src macaddress. If dest address is specified
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
    private void egressAclIcmpV6(Long dpidLong, String segmentationId, String srcMac,
                                 NeutronSecurityRule portSecurityRule, String dstAddress,
                                 boolean write, Integer protoPortMatchPriority) {

        MatchBuilder matchBuilder = new MatchBuilder();
        String flowId = "Egress_ICMP_" + segmentationId + "_" + srcMac + "_";
        matchBuilder = MatchUtils.createV6EtherMatchWithType(matchBuilder,srcMac,null);

        /*Custom ICMP Match */
        if (portSecurityRule.getSecurityRulePortMin() != null &&
                             portSecurityRule.getSecurityRulePortMax() != null) {
            flowId = flowId + portSecurityRule.getSecurityRulePortMin().shortValue() + "_"
                    + portSecurityRule.getSecurityRulePortMax().shortValue() + "_";
            matchBuilder = MatchUtils.createICMPv6Match(matchBuilder,
                    portSecurityRule.getSecurityRulePortMin().shortValue(),
                    portSecurityRule.getSecurityRulePortMax().shortValue());
        } else {
            /* All ICMP Match */ // We are getting from neutron NULL for both min and max
            flowId = flowId + "all" + "_" ;
            matchBuilder = MatchUtils.createICMPv6Match(matchBuilder, MatchUtils.ALL_ICMP, MatchUtils.ALL_ICMP);
        }
        if (null != dstAddress) {
            flowId = flowId + dstAddress;
            matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder,null,
                    MatchUtils.iPv6PrefixFromIPv6Address(dstAddress));
        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder,null,
                    new Ipv6Prefix(portSecurityRule.getSecurityRuleRemoteIpPrefix()));
        }
        flowId = flowId + "_Permit";
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        syncFlow(flowId, nodeBuilder, matchBuilder, protoPortMatchPriority, write, false, false);
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
        boolean isIpv6 = portSecurityRule.getSecurityRuleEthertype().equals(IP_VERSION_6);
        if (isIpv6) {
            matchBuilder = MatchUtils.createV6EtherMatchWithType(matchBuilder,srcMac,null);	
        } else {
            matchBuilder = MatchUtils.createV4EtherMatchWithType(matchBuilder,srcMac,null);
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
        if (null != dstAddress) {
            flowId = flowId + dstAddress;
            if (isIpv6) {
                matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder,null,
                        MatchUtils.iPv6PrefixFromIPv6Address(dstAddress));
            } else {
                matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder,null,
                        MatchUtils.iPv4PrefixFromIPv4Address(dstAddress));
            }
        } else if (null != portSecurityRule.getSecurityRuleRemoteIpPrefix()) {
            flowId = flowId + portSecurityRule.getSecurityRuleRemoteIpPrefix();
            if (isIpv6) {
                matchBuilder = MatchUtils.addRemoteIpv6Prefix(matchBuilder, null,
                        new Ipv6Prefix(portSecurityRule
                                       .getSecurityRuleRemoteIpPrefix()));
            } else {
                matchBuilder = MatchUtils.addRemoteIpPrefix(matchBuilder, null,
                        new Ipv4Prefix(portSecurityRule
                                       .getSecurityRuleRemoteIpPrefix()));
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
        syncFlow(flowName, nodeBuilder, matchBuilder, priority, write, false, false);
    }

    /**
     * Adds flow to allow any DHCP IPv6 client traffic.
     *
     * @param dpidLong the dpid
     * @param write whether to write or delete the flow
     * @param priority the priority
     */
    private void egressAclDhcpv6AllowClientTrafficFromVm(Long dpidLong,
                                                         boolean write, Integer priority) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        String flowName = "Egress_DHCPv6_Client"  + "_Permit_";
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createDhcpv6Match(matchBuilder, DHCPV6_DESTINATION_PORT, DHCPV6_SOURCE_PORT);
        syncFlow(flowName, nodeBuilder, matchBuilder, priority, write, false, false);
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
        syncFlow(flowName, nodeBuilder, matchBuilder, priority, write, true, false);
    }

    /**
     * Adds rule to prevent DHCPv6 spoofing by the vm attached to the port.
     *
     * @param dpidLong the dpid
     * @param localPort the local port
     * @param write is write or delete
     * @param priority  the priority
     */
    private void egressAclDhcpv6DropServerTrafficfromVm(Long dpidLong, long localPort,
                                                        boolean write, Integer priority) {

        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        String flowName = "Egress_DHCPv6_Server" + "_" + localPort + "_DROP_";
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createInPortMatch(matchBuilder, dpidLong, localPort);
        MatchUtils.createDhcpv6Match(matchBuilder, DHCPV6_SOURCE_PORT, DHCPV6_DESTINATION_PORT);
        syncFlow(flowName, nodeBuilder, matchBuilder, priority, write, true, false);
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
        syncFlow(flowName, nodeBuilder, matchBuilder, priority, write, false, false);
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
    private void egressAclAllowTrafficFromVmIpV6MacPair(Long dpidLong, long localPort,
                                                        String attachedMac, String srcIp,
                                                        Integer priority, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        String flowName = "Egress_Allow_VM_IPv6_MAC" + "_" + localPort + attachedMac + "_Permit_";
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createSrcL3Ipv6MatchWithMac(matchBuilder, new Ipv6Prefix(srcIp),new MacAddress(attachedMac));
        MatchUtils.createInPortMatch(matchBuilder, dpidLong, localPort);
        LOG.debug("egressAclAllowTrafficFromVmIpMacPair: MatchBuilder contains: {}", matchBuilder);
        syncFlow(flowName, nodeBuilder, matchBuilder, priority, write, false, false);
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
            ib.setKey(new InstructionKey(0));
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructionsList = Lists.newArrayList();
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
        super.setDependencies(bundleContext.getServiceReference(EgressAclProvider.class.getName()), this);
        securityServicesManager =
                (SecurityServicesManager) ServiceHelper.getGlobalInstance(SecurityServicesManager.class, this);
        securityGroupCacheManger =
                (SecurityGroupCacheManger) ServiceHelper.getGlobalInstance(SecurityGroupCacheManger.class, this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
