/*
 * Copyright (c) 2015 Dell, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;

import java.util.List;
import java.util.StringTokenizer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.acl.rev150105.RedirectToSfc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.bridges.Bridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.EgressDirection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.IngressDirection;
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
    private static final short TABLE_0_CLASSIFIER = 0;
    //private static final short TABLE_1_L2FORWARD = 30;
    //private static final short TABLE_2_L3FORWARD = 40;
    private static final short TABLE_3_INGR_ACL = 50;

    public static final long REG_VALUE_FROM_LOCAL = 0x1L;
    public static final long REG_VALUE_FROM_REMOTE = 0x2L;
    public static final Class<? extends NxmNxReg> REG_FIELD = NxmNxReg0.class;
    private volatile NodeCacheManager nodeCacheManager;
    private volatile Southbound southbound;
    private MdsalUtils mdsalUtils;
    private DataBroker dataBroker;
    private SfcClassifier sfcClassifier;

    // TBD:: Remove these constants after integrating with openstack.
    private static final String TUNNEL_DST = "192.168.50.75";
    private static final String TUNNEL_VNID = "10";
    private static final String CLIENT_PORT_NAME = "vethl-h35_2";
    private static final String SERVER_PORT_NAME = "vethl-h35_4";
    private static final String CLIENT_GPE_PORT_NAME = "sw1-vxlangpe-0";
    private static final String SERVER_GPE_PORT_NAME = "sw6-vxlangpe-0";
    private static final String INTERFACE_TYPE_VXLAN_GPE = "vxlangpe";

    private static final ImmutableBiMap<Class<? extends Direction>,String> DIRECTION_MAP
    = new ImmutableBiMap.Builder<Class<? extends Direction>,String>()
    .put(EgressDirection.class,"egress")
    .put(IngressDirection.class,"ingress")
    .build();
    /**
     * {@link NetvirtSfcOF13Provider} constructor.
     * @param dataBroker MdSal {@link DataBroker}
     */
    public NetvirtSfcOF13Provider(final DataBroker dataBroker) {
        Preconditions.checkNotNull(dataBroker, "Input dataBroker cannot be NULL!");
        this.dataBroker = dataBroker;
        mdsalUtils = new MdsalUtils(dataBroker);
        this.setDependencies(null);
        sfcClassifier = new SfcClassifier(dataBroker, southbound, mdsalUtils);
    }

    public void removeClassifierRules(Sff sff, Acl acl) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addClassifierRules(Bridge bridge, Acl acl, String direction) {
        Preconditions.checkNotNull(bridge, "Input bridge cannot be NULL!");
        Preconditions.checkNotNull(acl, "Input accesslist cannot be NULL!");

        Node bridgeNode = getBridgeNode(bridge.getName());
        if (bridgeNode == null) {
            LOG.debug("bridge {} not yet configured. Skip processing !!", bridge.getName());
            return;
        }

        // TODO: Find all nodes needing the classifier and add classifier to them
        for (Ace ace : acl.getAccessListEntries().getAce()) {
            if (direction.equalsIgnoreCase(DIRECTION_MAP.get(IngressDirection.class))) {
                processIngressAclEntry(ace, bridgeNode, true);
            } else {
                // Derive the Tunnel ID  & Tunnel Destination and use it as Match.
                NshUtils nshHeader = new NshUtils();
                // C1 is the normal overlay dest ip and c2 is the vnid
                // Hardcoded for now, netvirt integration will have those values
                nshHeader.setNshMetaC1(NshUtils.convertIpAddressToLong(new Ipv4Address(TUNNEL_DST)));
                nshHeader.setNshMetaC2(Long.parseLong(TUNNEL_VNID));

                // Derive the RSP ID and RSP path Index.. For Demo they're hardcoded.
                nshHeader.setNshNsp(10);
                nshHeader.setNshNsi((short)253);
                LOG.debug("The Nsh Header = {}, while applying Egress Classifier", nshHeader);
                processEgressAclEntry(ace, bridgeNode, nshHeader, true);
            }
        }
    }

    private void processEgressAclEntry(Ace ace, Node bridgeNode, NshUtils nshHeader, boolean write) {
        Matches matches = ace.getMatches();
        if (matches == null) {
            LOG.warn("processAclEntry: matches not found");
            return;
        }

        long ingTunnelPort = southbound.getOFPort(bridgeNode, SERVER_GPE_PORT_NAME);
        if (ingTunnelPort == 0L) {
            LOG.error("programAclEntry: Could not identify tunnel port {} -> OF ({}) on {}",
                    CLIENT_GPE_PORT_NAME, ingTunnelPort, bridgeNode);
            return;
        }

        long destOfPort = southbound.getOFPort(bridgeNode, SERVER_PORT_NAME);
        if (destOfPort == 0L) {
            LOG.error("programAclEntry: Could not identify local port {} -> OF ({}) on {}",
                    CLIENT_GPE_PORT_NAME, destOfPort, bridgeNode);
            return;
        }

        LOG.debug("Processing Egress ACL entry= ingrPort={}, destPort={}, nsp= {}, DPID={}", ingTunnelPort, destOfPort,
                                       String.valueOf(nshHeader.getNshNsp()), southbound.getDataPathId(bridgeNode));

        handleLocalInPort(southbound.getDataPathId(bridgeNode), String.valueOf(nshHeader.getNshNsp()), ingTunnelPort,
                TABLE_0_CLASSIFIER, TABLE_3_INGR_ACL, true);

        handleSfcEgressClassiferFlows(southbound.getDataPathId(bridgeNode), TABLE_3_INGR_ACL, ace.getRuleName(),
                matches, nshHeader, destOfPort, true);
    }

    private void processIngressAclEntry(Ace entry, Node srcNode, boolean write) {
        Matches matches = entry.getMatches();
        if (matches == null) {
            LOG.warn("processAclEntry: matches not found");
            return;
        }

        RedirectToSfc sfcRedirect = entry.getActions().getAugmentation(RedirectToSfc.class);
        LOG.debug("Processing ACL entry = {} on Node = {} sfcRedirect = {}", entry.getRuleName(),
                srcNode.getNodeId(), sfcRedirect);
        if (sfcRedirect == null) {
            LOG.warn("processAClEntry: sfcRedirect is null");
            return;
        }

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
            LOG.info("No configured RSP corresponding to SFP = {}, Creating new RSP = {}", sfpName, rspName);
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
                    if (rspReverse == null) {
                        LOG.warn("failed to add reverse RSP");
                        return;
                    }
                }
            }
        }

        LOG.info("processAclEntry: RSP: {}", rsp);

        // Find the first Hop within an RSP.
        // The classifier flow needs to send all matched traffic to this first hop SFF.
        List<RenderedServicePathHop> pathHopList = rsp.getRenderedServicePathHop();
        if (pathHopList.isEmpty()) {
            LOG.warn("Service Path = {} has empty hops!!", sfpName);
            return;
        }

        RenderedServicePathFirstHop firstRspHop = SfcProviderRenderedPathAPI
                .readRenderedServicePathFirstHop(new RspName(rspName));

        LOG.debug("First Hop IPAddress = {}, Port = {}", firstRspHop.getIp().getIpv4Address().getValue(),
                firstRspHop.getPort().getValue().intValue());
        long tunnelOfPort = southbound.getOFPort(srcNode, CLIENT_GPE_PORT_NAME);
        if (tunnelOfPort == 0L) {
            LOG.error("programAclEntry: Could not identify tunnel port {} -> OF ({}) on {}",
                    CLIENT_GPE_PORT_NAME, tunnelOfPort, srcNode);
            return;
        }

        long localOfPort = southbound.getOFPort(srcNode, CLIENT_PORT_NAME);
        if (localOfPort == 0L) {
            LOG.error("programAclEntry: Could not identify local port {} -> OF ({}) on {}",
                    CLIENT_GPE_PORT_NAME, localOfPort, srcNode);
            return;
        }

        NshUtils nshHeader = new NshUtils();
        // C1 is the normal overlay dest ip and c2 is the vnid
        // Hardcoded for now, netvirt integration will have those values
        nshHeader.setNshMetaC1(NshUtils.convertIpAddressToLong(new Ipv4Address(TUNNEL_DST)));
        nshHeader.setNshMetaC2(Long.parseLong(TUNNEL_VNID));
        nshHeader.setNshNsp(rsp.getPathId());

        RenderedServicePathHop firstHop = pathHopList.get(0);
        nshHeader.setNshNsi(firstHop.getServiceIndex());
        nshHeader.setNshTunIpDst(firstRspHop.getIp().getIpv4Address());
        nshHeader.setNshTunUdpPort(firstRspHop.getPort());
        LOG.debug("The Nsh Header = {}", nshHeader);

        handleLocalInPort(southbound.getDataPathId(srcNode), rsp.getPathId().toString(), localOfPort,
                TABLE_0_CLASSIFIER, TABLE_3_INGR_ACL, true);

        handleSfcClassiferFlows(southbound.getDataPathId(srcNode), TABLE_3_INGR_ACL, entry.getRuleName(),
                matches, nshHeader, tunnelOfPort, true);
    }

    private void handleSfcClassiferFlows(long dataPathId, short writeTable, String ruleName,
                                         Matches matches, NshUtils nshHeader, long tunnelOfPort,
                                         boolean write) {
        sfcClassifier.programSfcClassiferFlows(dataPathId, writeTable, ruleName, matches, nshHeader,
                tunnelOfPort, write);
    }

    private void handleSfcEgressClassiferFlows(long dataPathId, short writeTable, String ruleName,
            Matches matches, NshUtils nshHeader, long destOfPort,
            boolean write) {
        sfcClassifier.programSfcEgressClassiferFlows(dataPathId, writeTable, ruleName, matches, nshHeader,
                destOfPort, write);
    }

    private InstanceIdentifier<RenderedServicePaths> getRspsId() {
        return InstanceIdentifier.builder(RenderedServicePaths.class).build();
    }

    private InstanceIdentifier<RenderedServicePath> getRspId(String rspName) {
        return InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, new RenderedServicePathKey(new RspName(rspName))).build();
    }

    public Node getBridgeNode(String bridgeName) {
        Node nodeFound = null;
        final List<Node> nodes = nodeCacheManager.getBridgeNodes();
        if (nodes != null && !nodes.isEmpty()) {
            for (Node node : nodes) {
                if (southbound.getBridge(node, bridgeName) != null) {
                    nodeFound = node;
                    break;
                }
            }
        }
        return nodeFound;
    }

    public RenderedServicePath getRspforSfp(String sfpName) {
        RenderedServicePath rspFound = null;
        RenderedServicePaths rsps = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, this.getRspsId());
        if (rsps != null) {
            for (RenderedServicePath rsp : rsps.getRenderedServicePath()) {
                if (rsp.getParentServiceFunctionPath() != null) {
                    if (rsp.getParentServiceFunctionPath().getValue().equals(sfpName)) {
                        rspFound = rsp;
                    }
                }
            }
        }
        return rspFound;
    }

    public ServiceFunctionPath getSfp(String redirectSfc) {
        ServiceFunctionPath sfpFound = null;
        ServiceFunctionPaths sfps = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        if (sfps != null) {
            for (ServiceFunctionPath sfp: sfps.getServiceFunctionPath()) {
                if (sfp.getServiceChainName().getValue().equalsIgnoreCase(redirectSfc)) {
                    sfpFound = sfp;
                }
            }
        }
        return sfpFound;
    }

/*
 * (TABLE:0) EGRESS VM TRAFFIC TOWARDS TEP
 * MATCH: DESTINATION ETHERNET ADDR AND OPENFLOW INPORT
 * INSTRUCTION: SET TUNNELID AND GOTO TABLE TUNNEL TABLE (N)
 * TABLE=0,IN_PORT=2,DL_SRC=00:00:00:00:00:01 \
 * ACTIONS=SET_FIELD:5->TUN_ID,GOTO_TABLE=1"
 */
    public String getDestIp(Matches match) {
        if (match.getAceType() instanceof AceIp) {
            AceIp aceIp = (AceIp)match.getAceType();
            if (aceIp.getAceIpVersion() instanceof AceIpv4) {
                AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();
                if (aceIpv4.getDestinationIpv4Network() != null) {
                    String ipAddrPrefix = aceIpv4.getDestinationIpv4Network().getValue();
                    return new StringTokenizer(ipAddrPrefix, "/").nextToken();
                }
            }
        }
        return null;
    }

    public String getSourceIp(Matches match) {
        if (match.getAceType() instanceof AceIp) {
            AceIp aceIp = (AceIp)match.getAceType();
            if (aceIp.getAceIpVersion() instanceof AceIpv4) {
                AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();
                if (aceIpv4.getSourceIpv4Network() != null) {
                    //String ipAddr = new StringTokenizer(ipAddrPrefix, "/").nextToken();
                    return aceIpv4.getSourceIpv4Network().getValue();
                }
            }
        }
        return null;
    }

    public void handleLocalInPort(long dpidLong, String segmentationId, Long inPort,
                                  short writeTable, short goToTableId, boolean write) {
        sfcClassifier.programLocalInPort(dpidLong, segmentationId, inPort, writeTable, goToTableId, write);
    }

    private void setDependencies(ServiceReference serviceReference) {
        nodeCacheManager = (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
    }
}
