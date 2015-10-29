/*
 * Copyright (c) 2015 Dell, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13;

import com.google.common.util.concurrent.CheckedFuture;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.acl.rev150105.RedirectToSfc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.bridges.Bridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Open vSwitch OpenFlow 1.3 Networking Provider for Netvirt SFC
 * @author Arun Yerra
 */
public class NetvirtSfcOF13Provider implements INetvirtSfcOF13Provider{
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcOF13Provider.class);
    private static final int DEFAULT_FLOW_PRIORITY = 32768;
    private static final short TABLE_0_CLASSIFIER = 0;
    //private static final short TABLE_1_L2FORWARD = 30;
    //private static final short TABLE_2_L3FORWARD = 40;
    private static final short TABLE_3_INGR_ACL = 50;
    private static final String OPENFLOW = "openflow:";

    public static final long REG_VALUE_FROM_LOCAL = 0x1L;
    public static final long REG_VALUE_FROM_REMOTE = 0x2L;
    public static final Class<? extends NxmNxReg> REG_FIELD = NxmNxReg0.class;
    private volatile NodeCacheManager nodeCacheManager;
    private volatile Southbound southbound;
    private MdsalUtils mdsalUtils;
    private DataBroker dataBroker;

    // TBD:: Remove these constants after integrating with openstack.
    //private static final String NETWORK_TYPE_VXLAN = "vxlan";
    private static final String NETWORK_SEGMENT_ID = "10";
    private static final String LOCAL_TP_ID = "vethl-h35_2";
    private static final String INTERFACE_TYPE_VXLAN_GPE = "vxlangpe";
    private static final String GPE_IFACE_ID = "sw1-vxlangpe-0";

    /**
     * {@link NetvirtSfcOF13Provider} constructor.
     * @param dataBroker MdSal {@link DataBroker}
     */
    public NetvirtSfcOF13Provider(final DataBroker dataBroker) {
        Preconditions.checkNotNull(dataBroker, "Input dataBroker cannot be NULL!");
        this.dataBroker = dataBroker;
        mdsalUtils = new MdsalUtils(dataBroker);
        this.setDependencies(null);
    }

    @Override
    public void addClassifierRules(Sff sff, Acl acl) {
        Preconditions.checkNotNull(sff, "Input service function forwarder cannot be NULL!");
        Preconditions.checkNotNull(acl, "Input accesslist cannot be NULL!");

        // Validate if any service function forwarder exists by the name, using SFC provider APIs.
        ServiceFunctionForwarder serviceForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(new SffName(sff.getName()));
        if (serviceForwarder == null) {
            LOG.debug("Service Function Forwarder = {} not yet configured. Skip processing !!", sff.getName());
            return;
        }

        // If a service function forwarder exists, then get the corresponding OVS Bridge details and Openflow NodeId.
        // If OVS Bridge augmentation is configured, the following API returns NULL.
        String datapathId = null;
        Node node = null;
        // TODO: Replace with OVSDB methods and not use the SFC API
        datapathId = SfcOvsUtil.getOpenFlowNodeIdForSff(serviceForwarder);
        if (datapathId == null) {
            LOG.debug("Service Function Forwarder = {} is not augmented with "
                    + "OVS Bridge Information. Skip processing!!", sff.getName());
        }
        // If openflow Node Id is NULL, get all the bridge nodes using southbound apis and fetch
        // SFF with matching name. From this bridge name, get the openflow data path ID.

        node = getBridgeNode(serviceForwarder.getName().toString());
        if (node == null) {
            LOG.warn("Node doesn't exist for corresponding SFF={}", sff.getName());
            return;
        }

        datapathId = getDpid(node);
        LOG.debug("Processing the Classifier rules on Node={}", datapathId);
        if (datapathId != null) {
            // Program the OF flow on the corresponding open flow node.
            Iterator<Ace> itr = acl.getAccessListEntries().getAce().iterator();
            while (itr.hasNext()) {
                Ace entry = itr.next();
                processAclEntry(entry, node, true);
            }
        } else {
            LOG.warn("Skipping ACL processing on Node={} as DatapathID is NULL!!", sff.getName());
        }
    }

    @Override
    public void addClassifierRules(Bridge bridge, Acl acl) {
        Preconditions.checkNotNull(bridge, "Input bridge cannot be NULL!");
        Preconditions.checkNotNull(acl, "Input accesslist cannot be NULL!");

        Node bridgeNode = getBridgeNode(bridge.getName());
        if (bridgeNode == null) {
            LOG.debug("bridge {} not yet configured. Skip processing !!", bridge.getName());
            return;
        }

        // Program the OF flow on the corresponding open flow node.
        for (Ace ace : acl.getAccessListEntries().getAce()) {
            processAclEntry(ace, bridgeNode, true);
        }
    }

    private void processAclEntry(Ace entry, Node srcNode, boolean write) {
        RedirectToSfc sfcRedirect = entry.getActions().getAugmentation(RedirectToSfc.class);
        LOG.debug("Processing ACL entry = {} on Node = {} sfcRedirect = {}", entry.getRuleName(),
                srcNode.getNodeId(), sfcRedirect);
        if (sfcRedirect != null) {
            // Given SFP find the corresponding RSP.
            String sfcName = sfcRedirect.getRedirectSfc();
            LOG.debug("Processing Redirect to SFC = {}", sfcRedirect.getRedirectSfc());
            ServiceFunctionPath sfp = getSfp(sfcName);
            if (sfp == null || sfp.getName() == null) {
                LOG.warn("There is no configured SFP with sfcName = {}; so skip installing the ACL entry!!", sfcName);
                return;
            }

            LOG.debug("Processing Redirect to SFC = {}, SFP = {}", sfcRedirect.getRedirectSfc(), sfp);
            // If RSP doesn't exist, create an RSP.
            String sfpName = sfp.getName().getValue();
            RenderedServicePath rsp = getRspforSfp(sfpName);
            String rspName = sfp.getName().getValue() + "_rsp";
            if (rsp == null) {
                LOG.info("No configured RSP corresponding to SFP = {}, Creating new RSP = {}!!", sfpName, rspName);
                // Create RSP.
                CreateRenderedPathInput rspInput = new CreateRenderedPathInputBuilder()
                        .setParentServiceFunctionPath(sfpName)
                        .setName(rspName)
                        .setSymmetric(sfp.isSymmetric())
                        .build();
                rsp = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(sfp, rspInput);
                if (rsp == null) {
                    LOG.warn("failed to add RSP");
                    return;
                }

                // If SFP is symmetric, create RSP in the reverse direction.
                if (sfp.isSymmetric()) {
                    LOG.info("SFP = {} is symmetric, installing RSP in the reverse direction!!", sfpName);
                    String rspNameRev = rspName + "-Reverse";
                    RenderedServicePath rspReverse = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                                                                 this.getRspId(rspNameRev));
                    if (rspReverse == null) {
                        rspReverse = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(rsp);
                    }
                }
            }
            LOG.info("rsp: {}", rsp);

            // Find the first Hop within a RSP.
            List<RenderedServicePathHop> pathHopList = rsp.getRenderedServicePathHop();
            if (pathHopList.isEmpty()) {
                LOG.info("Service Path = {} has empty hops!!", sfpName);
                return;
            }

            RenderedServicePathFirstHop firstRspHop = SfcProviderRenderedPathAPI
                    .readRenderedServicePathFirstHop(new RspName(rspName));
            //String firstSff = firstHop.getServiceFunctionForwarderLocator();

            LOG.debug("First Hop IPAddress = {}, Port = {}", firstRspHop.getIp().getIpv4Address().getValue(),
                    firstRspHop.getPort().getValue().intValue());
            OvsdbTerminationPointAugmentation tunnelPort = southbound
                    .getTerminationPointOfBridge(srcNode, GPE_IFACE_ID);
            if (tunnelPort != null) {
                long tunnelOfPort = southbound.getOFPort(tunnelPort);
                LOG.debug("Tunnel Port = {}, OF Port Number = {}", tunnelPort.getName(), tunnelOfPort);
                if (tunnelOfPort == 0) {
                    LOG.error("programTunnelRules: Could not Identify Tunnel port {} -> OF ({}) on {}",
                            tunnelPort.getName(), tunnelOfPort, srcNode);
                    return;
                }
            }

            Matches match = entry.getMatches();

            NshUtils header = new NshUtils();
            // C1 is the normal overlay dest ip
            header.setNshMetaC1(convertToLongIp(getDestIp(match)));
            header.setNshMetaC2(Long.parseLong(NETWORK_SEGMENT_ID));
            header.setNshNsp(rsp.getPathId());

            RenderedServicePathHop firstHop = pathHopList.get(0);
            header.setNshNsi(firstHop.getServiceIndex());
            header.setNshTunIpDst(firstRspHop.getIp().getIpv4Address());
            header.setNshTunUdpPort(firstRspHop.getPort());

            LOG.debug("The Nsh Header = {}", header);
            OvsdbTerminationPointAugmentation localPort = getTerminationPoint(srcNode, LOCAL_TP_ID);

           /* String attachedMac = southbound.getInterfaceExternalIdsValue(localPort, Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                LOG.warn("No AttachedMac seen in {}", localPort);
                // return;
            }
            */
            LOG.debug("LocalPort ID={}, IP Address = {}", localPort.getName(), getSourceIp(match));
            handleLocalInPort(southbound.getDataPathId(srcNode), TABLE_0_CLASSIFIER, TABLE_3_INGR_ACL,
                    NETWORK_SEGMENT_ID, localPort.getOfport(), getSourceIp(match), true);

            // L2 Dst MAC forwarding flows.
            // L2 Flood Flows.
            // Set the Tunnel Destination IP, Dest MAC based on the destination IP address.
            // Replace SMAC & decrement TTL.
            // handleL3Flows(southbound.getDataPathId(srcNode), TABLE_1_ISOLATE_TENANT,
            // TABLE_2_INGRESS_ACL, destIp, l3SegmentId, destMac);
            // Set NSP & NSI values based on the Classifier Match actions.
            OvsdbTerminationPointAugmentation outPort = getTerminationPoint(srcNode, INTERFACE_TYPE_VXLAN_GPE);
            // TODO: commented out for test
            //handleSfcClassiferFlows(entry.getRuleName(), srcNode, match,
            //              TABLE_3_INGR_ACL, header, outPort.getOfport(), true);
            // Set NSHC1, NSHC2, Tunnel DestIP, port toward SFF1, output to Tunnel Port.

        }
    }

    private OvsdbTerminationPointAugmentation getTerminationPoint(Node srcNode, String localTpId) {
        List<OvsdbTerminationPointAugmentation> ports = southbound.extractTerminationPointAugmentations(srcNode);
        if (ports != null && !ports.isEmpty()) {
            for (OvsdbTerminationPointAugmentation port : ports) {
                // TBD :: For Demo, use the Tunnel ID as 10. Once openstack is integrated,
                // tunnel ID is created through Network Creation.
                if (port.getName().contains(localTpId)) {
                    return port;
                }
            }
        }
        return null;
    }

    private String getTunnelName(String networkTypeVxlan, Ipv4Address ipv4Address) {
        return networkTypeVxlan + "-" + ipv4Address.getValue();
    }

    /*
     * (TABLE:50) EGRESS VM TRAFFIC TOWARDS TEP with NSH header
     * MATCH: Match fields passed through ACL entry
     * INSTRUCTION: SET TUNNELID AND GOTO TABLE TUNNEL TABLE (N)
     * TABLE=0,IN_PORT=2,DL_SRC=00:00:00:00:00:01 \
     * ACTIONS=SET_FIELD:5->TUN_ID,GOTO_TABLE=1"
     */
    private void handleSfcClassiferFlows(String ruleName, Node node, Matches match,
                 short table3IngrAcl, NshUtils header, long ofPort, boolean write) {
        // Build the Actions to Add the NSH Header
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nshC1Load =
                        NshUtils.nxLoadNshc1RegAction(header.getNshMetaC1());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nshC2Load =
                        NshUtils.nxLoadNshc2RegAction(header.getNshMetaC2());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nspLoad =
                        NshUtils.nxSetNspAction(header.getNshNsp());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nsiLoad =
                        NshUtils.nxSetNsiAction(header.getNshNsi());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action loadChainTunVnid =
                        NshUtils.nxLoadTunIdAction(BigInteger.valueOf(header.getNshMetaC2()), false);
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action loadChainTunDest =
                        NshUtils.nxLoadTunIPv4Action(header.getNshTunIpDst().getValue(), false);

        int count = 0;
        List<Action> actionList = Lists.newArrayList();
        actionList.add(new ActionBuilder().setOrder(Integer.valueOf(count++)).setAction(nshC1Load).build());
        actionList.add(new ActionBuilder().setOrder(Integer.valueOf(count++)).setAction(nshC2Load).build());
        actionList.add(new ActionBuilder().setOrder(Integer.valueOf(count++)).setAction(nspLoad).build());
        actionList.add(new ActionBuilder().setOrder(Integer.valueOf(count++)).setAction(nsiLoad).build());
        actionList.add(new ActionBuilder().setOrder(Integer.valueOf(count++)).setAction(loadChainTunDest).build());
        actionList.add(new ActionBuilder().setOrder(Integer.valueOf(count++)).setAction(loadChainTunVnid).build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));

        List<Instruction> instructions = Lists.newArrayList();
        instructions.add(ib.build());

     // Set the Output Port/Iface
        InstructionUtils.createOutputPortInstructions(ib, southbound.getDataPathId(node), ofPort);
        ib.setOrder(1);
        ib.setKey(new InstructionKey(1));
        instructions.add(ib.build());

     // Add InstructionBuilder to the Instruction(s)Builder List
        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);

        String flowId = "NETVIRT_SFC_FLOW" + "_" + ruleName + "_" + header.getNshNsp();
        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));

        MatchBuilder mb = buildMatch(match);
        flowBuilder.setMatch(mb.build());
        flowBuilder.setPriority(DEFAULT_FLOW_PRIORITY);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(table3IngrAcl);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        flowBuilder.setInstructions(isb.build());

        if (write) {
            writeFlow(flowBuilder, createNodeBuilder(node.getNodeId().getValue()));
        } else {
            removeFlow(flowBuilder, createNodeBuilder(node.getNodeId().getValue()));
        }
    }

    /*
     * (TABLE:0) EGRESS VM TRAFFIC TOWARDS TEP
     * MATCH: DESTINATION ETHERNET ADDR AND OPENFLOW INPORT
     * INSTRUCTION: SET TUNNELID AND GOTO TABLE TUNNEL TABLE (N)
     * TABLE=0,IN_PORT=2,DL_SRC=00:00:00:00:00:01 \
     * ACTIONS=SET_FIELD:5->TUN_ID,GOTO_TABLE=1"
     */
    private void handleLocalInPort(long dpidLong, short writeTable, short goToTableId,
            String segmentationId, Long inPort, String sourceIp,
            boolean write) {
        programDropSrcIface(dpidLong, inPort, true);
        programLocalInPort(dpidLong, segmentationId, inPort, sourceIp, goToTableId, write);
    }

    private String getDestIp(Matches match) {
        if (match.getAceType() instanceof AceIp) {
            AceIp aceIp = (AceIp)match.getAceType();
            if (aceIp.getAceIpVersion() instanceof AceIpv4) {
                AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();
                if (aceIpv4.getDestinationIpv4Network() != null) {
                    String ipAddrPrefix = aceIpv4.getDestinationIpv4Network().getValue();
                    String ipAddr = new StringTokenizer(ipAddrPrefix, "/").nextToken();
                    return ipAddr;
                }
            }
        }
        return null;
    }

    private String getSourceIp(Matches match) {
        if (match.getAceType() instanceof AceIp) {
            AceIp aceIp = (AceIp)match.getAceType();
            if (aceIp.getAceIpVersion() instanceof AceIpv4) {
                AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();
                if (aceIpv4.getSourceIpv4Network() != null) {
                    String ipAddrPrefix = aceIpv4.getSourceIpv4Network().getValue();
                    //String ipAddr = new StringTokenizer(ipAddrPrefix, "/").nextToken();
                    return ipAddrPrefix;
                }
            }
        }
        return null;
    }

    private long convertToLongIp(String ipaddr) {
        LOG.debug("Converting String={} to Long", ipaddr);
        return InetAddresses.coerceToInteger(InetAddresses.forString(ipaddr)) & 0xFFFFFFFFL;
    }

    private void programLocalInPort(Long dpidLong, String segmentationId, Long inPort,
                                    String ipAddr, short goToTableId, boolean write) {
        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        //flowBuilder.setMatch(MatchUtils.createEthSrcMatch(matchBuilder, new MacAddress(attachedMac)).build());
        // TODO Broken In_Port Match
        flowBuilder.setMatch(MatchUtils.createInPortMatch(matchBuilder, dpidLong, inPort).build());
        //flowBuilder.setMatch(MatchUtils.createSrcL3IPv4Match(matchBuilder, new Ipv4Prefix(ipAddr)).build());
        String flowId = "LocalSfc_" + segmentationId + "_" + inPort;// + "_" + ipAddr.split("/",2)[0];
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId((short)0);//getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setPriority(8192);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // TODO Broken SetTunID
            //InstructionUtils.createSetTunnelIdInstructions(ib, new BigInteger(segmentationId));
            //ApplyActionsCase aac = (ApplyActionsCase) ib.getInstruction();
            //List<Action> actionList = aac.getApplyActions().getAction();

            //ActionBuilder ab = new ActionBuilder();
            //ab.setAction(ActionUtils.nxLoadRegAction(new DstNxRegCaseBuilder().setNxReg(REG_FIELD).build(),
            //        BigInteger.valueOf(REG_VALUE_FROM_LOCAL)));
            //ab.setOrder(1);
            //ab.setKey(new ActionKey(1));

            //actionList.add(ab.build());

            //ib.setOrder(0);
            //ib.setKey(new InstructionKey(0));
            //instructions.add(ib.build());

            // Next service GOTO Instructions Need to be appended to the List
            ib = InstructionUtils.createGotoTableInstructions(new InstructionBuilder(), goToTableId);
            ib.setOrder(0);//1);
            ib.setKey(new InstructionKey(0));//1));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());

            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    public void programDropSrcIface(Long dpidLong, Long inPort, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(MatchUtils.createInPortMatch(matchBuilder, dpidLong, inPort).build());

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Call the InstructionBuilder Methods Containing Actions
            InstructionUtils.createDropInstructions(ib);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
        }

        String flowId = "DropFilter_"+inPort;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId((short)50);//getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setPriority(8192);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        if (write) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    private Node getBridgeNode(String bridgeName) {
        //SffOvsBridgeAugmentation sffBridge = sff.getAugmentation(SffOvsBridgeAugmentation.class);
        //String brName = sffBridge.getOvsBridge().getBridgeName();

        final List<Node> nodes = nodeCacheManager.getBridgeNodes();
        if (nodes.isEmpty()) {
            LOG.debug("Noop with Classifier Creation on SFF={}. No Bridges configured YET!!", bridgeName);
        } else {
            for (Node node : nodes) {
                OvsdbBridgeAugmentation bridgeAugmentation = southbound.extractBridgeAugmentation(node);
                if (bridgeAugmentation != null) {
                    if (bridgeAugmentation.getBridgeName().getValue().equalsIgnoreCase(bridgeName)) {
                        LOG.info("Found matching OVSDB Bridge Name {}", node.getNodeId().getValue());
                        return node;
                    }
                }
            }
        }
        return null;
    }

/*
    private InstructionsBuilder buildActions(String ruleName, Actions actions, String datapathId) {
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

    private ServiceFunctionPath getSfp(String redirectSfc) {
        ServiceFunctionPaths sfps = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        if (sfps != null) {
            for (ServiceFunctionPath sfp: sfps.getServiceFunctionPath()) {
                if (sfp.getServiceChainName().getValue().equalsIgnoreCase(redirectSfc)) {
                    return sfp;
                }
            }
        }
        return null;
    }

    private RenderedServicePath getRspforSfp(String sfpName) {
        RenderedServicePaths rsps = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, this.getRspsId());
        if (rsps == null) {
            LOG.debug("RSP has not been configured yet for SFP = {}", sfpName);
            return null;
        }
        for (RenderedServicePath rsp : rsps.getRenderedServicePath()) {
            if (rsp.getParentServiceFunctionPath() !=  null) {
                if (rsp.getParentServiceFunctionPath().getValue().equalsIgnoreCase(sfpName)) {
                    return rsp;
                }
            }
        }
        return null;
    }

    private MatchBuilder buildMatch(Matches matches) {
        MatchBuilder matchBuilder = new MatchBuilder();

        if (matches.getAceType() instanceof AceIp) {
            AceIp aceIp = (AceIp)matches.getAceType();
            if (aceIp.getAceIpVersion() instanceof AceIpv4) {
                AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();
                MatchUtils.createSrcL3IPv4Match(matchBuilder, aceIpv4.getSourceIpv4Network());
                //MatchUtils.createDstL3IPv4Match(matchBuilder, aceIpv4.getDestinationIpv4Network());
                //MatchUtils.createIpProtocolMatch(matchBuilder, aceIp.getProtocol());
                MatchUtils.addLayer4Match(matchBuilder, aceIp.getProtocol().intValue(),
                        0,
                        aceIp.getDestinationPortRange().getLowerPort().getValue().intValue());
            }
        } else if (matches.getAceType() instanceof AceEth) {
            AceEth aceEth = (AceEth) matches.getAceType();
            MatchUtils.createEthSrcMatch(matchBuilder, new MacAddress(aceEth.getSourceMacAddress().getValue()));
            MatchUtils.createDestEthMatch(matchBuilder, new MacAddress(aceEth.getDestinationMacAddress().getValue()),
                    new MacAddress(aceEth.getDestinationMacAddressMask().getValue()));
        }

        //MatchUtils.createInPortMatch(matchBuilder, Long.getLong(dpId), Long.getLong(matches.getInputInterface()));
        return matchBuilder;
    }

    @Override
    public void removeClassifierRules(Sff sff, Acl acl) {
        // TODO Auto-generated method stub
    }

    /*
    protected void writeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        LOG.debug("writeFlow: flowBuilder: {}, nodeBuilder: {}",
                flowBuilder.build(), nodeBuilder.build());
        mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, createNodePath(nodeBuilder), nodeBuilder.build());
        mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, createFlowPath(flowBuilder, nodeBuilder), flowBuilder.build());
    }
*/
    protected void writeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        LOG.debug("writeFlow: flowBuilder: {}, nodeBuilder: {}",
                flowBuilder.build(), nodeBuilder.build());
        WriteTransaction modification = dataBroker.newWriteOnlyTransaction();
        modification.put(LogicalDatastoreType.CONFIGURATION, createNodePath(nodeBuilder),
                nodeBuilder.build(), true /*createMissingParents*/);
        modification.put(LogicalDatastoreType.CONFIGURATION, createFlowPath(flowBuilder, nodeBuilder),
                flowBuilder.build(), true /*createMissingParents*/);

        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            LOG.debug("Transaction success for write of Flow {}", flowBuilder.getFlowName());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            modification.cancel();
        }
    }

    protected void removeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, createFlowPath(flowBuilder, nodeBuilder));
    }

    private String getDpid(Node node) {
        long dpid = southbound.getDataPathId(node);
        if (dpid == 0) {
            LOG.warn("getDpid: DPID could not be found for node: {}", node.getNodeId().getValue());
        }
        return String.valueOf(dpid);
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

    private NodeBuilder createNodeBuilder(String nodeId) {
        NodeBuilder builder = new NodeBuilder();
        builder.setId(new NodeId(nodeId));
        builder.setKey(new NodeKey(builder.getId()));
        return builder;
    }

    private InstanceIdentifier<RenderedServicePaths> getRspsId() {
        return InstanceIdentifier.builder(RenderedServicePaths.class).build();
    }

    private InstanceIdentifier<RenderedServicePath> getRspId(String rspName) {
        return InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, new RenderedServicePathKey(new RspName(rspName))).build();
    }

    private short getTable() {
        return Service.INGRESS_ACL.getTable();
    }

    private void setDependencies(ServiceReference serviceReference) {
        nodeCacheManager = (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
    }
}
