/*
 * Copyright (c) 2015 Dell, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.standalone.openflow13;

import com.google.common.base.Preconditions;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.StringTokenizer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.INetvirtSfcOF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.ISfcClassifierService;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.NshUtils;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.acl.rev150105.RedirectToSfc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.Bridges;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.bridges.Bridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Open vSwitch OpenFlow 1.3 Networking Provider for Netvirt SFC
 * @author Arun Yerra
 */
public class NetvirtSfcStandaloneOF13Provider implements INetvirtSfcOF13Provider {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcStandaloneOF13Provider.class);
    private static final short TABLE_0_CLASSIFIER = 0;
    private static final short TABLE_3_INGR_ACL = 50;

    private volatile NodeCacheManager nodeCacheManager;
    private volatile Southbound southbound;
    private MdsalUtils mdsalUtils;
    private SfcClassifier sfcClassifier;

    // TBD:: Remove these constants after integrating with openstack.
    private static final String TUNNEL_DST = "192.168.50.75";
    private static final String TUNNEL_VNID = "10";
    private static final String CLIENT_PORT_NAME = "vethl-h35_2";
    private static final String SERVER_PORT_NAME = "vethl-h35_4";
    private static final String CLIENT_GPE_PORT_NAME = "sw1-vxlangpe-0";
    private static final String SERVER_GPE_PORT_NAME = "sw6-vxlangpe-0";
    private static final String INTERFACE_TYPE_VXLAN_GPE = "vxlangpe";

    /**
     * {@link NetvirtSfcStandaloneOF13Provider} constructor.
     * @param dataBroker MdSal {@link DataBroker}
     */
    public NetvirtSfcStandaloneOF13Provider(final DataBroker dataBroker) {
        Preconditions.checkNotNull(dataBroker, "Input dataBroker cannot be NULL!");
        mdsalUtils = new MdsalUtils(dataBroker);
        //this.setDependencies(null);
        sfcClassifier = new SfcClassifier(dataBroker, southbound, mdsalUtils);
    }

    public void removeClassifierRules(Sff sff, Acl acl) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addClassifierRules(Acl acl) {
        String aclName = acl.getAclName();
        Classifiers classifiers = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, getClassifierIid());
        if (classifiers == null) {
            LOG.debug("add: No Classifiers found");
            return;
        }

        LOG.debug("add: Classifiers: {}", classifiers);
        for (Classifier classifier : classifiers.getClassifier()) {
            if (classifier.getAcl().equals(aclName)) {
                if (classifier.getBridges() != null) {
                    addClassifierRules(classifier.getBridges(), acl);
                }
            }
        }
    }

    @Override
    public void removeClassifierRules(Acl acl) {
        String aclName = acl.getAclName();
        Classifiers classifiers = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, getClassifierIid());
        if (classifiers != null) {
            for (Classifier classifier : classifiers.getClassifier()) {
                if (classifier.getAcl().equalsIgnoreCase(aclName)) {
                    if (classifier.getSffs() != null) {
                        for (Sff sff : classifier.getSffs().getSff()) {
                            removeClassifierRules(sff, acl);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setSfcClassifierService(ISfcClassifierService sfcClassifierService) {

    }

    @Override
    public void addClassifierRules(Bridge bridge, Acl acl) {

    }

    @Override
    public void addClassifierRules(Bridges bridges, Acl acl) {
        Preconditions.checkNotNull(bridges, "Input bridges cannot be NULL!");
        Preconditions.checkNotNull(acl, "Input accesslist cannot be NULL!");

        for (Ace ace : acl.getAccessListEntries().getAce()) {
            processAclEntry(ace, bridges, true);
        }
    }

    private void processAclEntry(Ace entry, Bridges bridges, boolean write) {
        Matches matches = entry.getMatches();
        if (matches == null) {
            LOG.warn("processAclEntry: matches not found");
            return;
        }

        RenderedServicePath rsp = getRenderedServicePath(entry);
        if (rsp == null) {
            LOG.warn("Failed to get renderedServicePatch for entry: {}", entry);
            return;
        }

        LOG.info("processAclEntry: RSP: {}", rsp);
        List<RenderedServicePathHop> pathHopList = rsp.getRenderedServicePathHop();
        if (pathHopList.isEmpty()) {
            LOG.warn("Service Path = {} has empty hops!!", rsp.getName());
            return;
        }

        for (Bridge bridge : bridges.getBridge()) {
            if (bridge.getDirection().getIntValue() == 0) {
                Node bridgeNode = getBridgeNode(bridge.getName());
                if (bridgeNode == null) {
                    LOG.debug("processAclEntry: bridge {} not yet configured. Skip processing !!", bridge.getName());
                    continue;
                }

                long tunnelOfPort = southbound.getOFPort(bridgeNode, CLIENT_GPE_PORT_NAME);
                if (tunnelOfPort == 0L) {
                    LOG.error("programAclEntry: Could not identify tunnel port {} -> OF ({}) on {}",
                            CLIENT_GPE_PORT_NAME, tunnelOfPort, bridgeNode);
                    return;
                }

                long localOfPort = southbound.getOFPort(bridgeNode, CLIENT_PORT_NAME);
                if (localOfPort == 0L) {
                    LOG.error("programAclEntry: Could not identify local port {} -> OF ({}) on {}",
                            CLIENT_GPE_PORT_NAME, localOfPort, bridgeNode);
                    return;
                }

                // Find the first Hop within an RSP.
                // The classifier flow needs to send all matched traffic to this first hop SFF.
                RenderedServicePathFirstHop firstRspHop = SfcProviderRenderedPathAPI
                        .readRenderedServicePathFirstHop(new RspName(rsp.getName()));

                LOG.debug("First Hop IPAddress = {}, Port = {}", firstRspHop.getIp().getIpv4Address().getValue(),
                        firstRspHop.getPort().getValue());

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

                handleLocalInPort(southbound.getDataPathId(bridgeNode), rsp.getPathId().toString(), localOfPort,
                        TABLE_0_CLASSIFIER, TABLE_3_INGR_ACL, matches, true);

                handleSfcClassiferFlows(southbound.getDataPathId(bridgeNode), TABLE_3_INGR_ACL, entry.getRuleName(),
                        matches, nshHeader, tunnelOfPort, true);
            } else {
                Node bridgeNode = getBridgeNode(bridge.getName());
                if (bridgeNode == null) {
                    LOG.debug("processAclEntry: bridge {} not yet configured. Skip processing !!", bridge.getName());
                    continue;
                }

                long tunnelOfPort = southbound.getOFPort(bridgeNode, SERVER_GPE_PORT_NAME);
                if (tunnelOfPort == 0L) {
                    LOG.error("programAclEntry: Could not identify tunnel port {} -> OF ({}) on {}",
                            CLIENT_GPE_PORT_NAME, tunnelOfPort, bridgeNode);
                    return;
                }

                long localOfPort = southbound.getOFPort(bridgeNode, SERVER_PORT_NAME);
                if (localOfPort == 0L) {
                    LOG.error("programAclEntry: Could not identify local port {} -> OF ({}) on {}",
                            CLIENT_GPE_PORT_NAME, localOfPort, bridgeNode);
                    return;
                }

                RenderedServicePathHop lastRspHop = Iterables.getLast(rsp.getRenderedServicePathHop());

                LOG.debug("programAclEntry: Last Hop #: {}, nsi: {}", lastRspHop.getHopNumber().intValue(),
                        lastRspHop.getServiceIndex().intValue() - 1);

                NshUtils nshHeader = new NshUtils();
                nshHeader.setNshNsp(rsp.getPathId());
                nshHeader.setNshNsi((short)(lastRspHop.getServiceIndex().intValue() - 1));
                nshHeader.setNshMetaC2(Long.parseLong(TUNNEL_VNID));
                LOG.debug("programAclEntry: The Nsh Header = {}", nshHeader);

                //handleLocalEgressPort(southbound.getDataPathId(bridgeNode), rsp.getPathId().toString(), localOfPort,
                //        TABLE_0_CLASSIFIER, TABLE_3_INGR_ACL, true);

                handleEgressSfcClassiferFlows(southbound.getDataPathId(bridgeNode),
                        TABLE_0_CLASSIFIER, entry.getRuleName(), matches, nshHeader, tunnelOfPort, localOfPort, true);
            }
        }
    }

    private RenderedServicePath getRenderedServicePath (Ace entry) {
        RenderedServicePath rsp = null;
        RedirectToSfc sfcRedirect = entry.getActions().getAugmentation(RedirectToSfc.class);
        LOG.debug("Processing ACL entry = {} sfcRedirect = {}", entry.getRuleName(), sfcRedirect);
        if (sfcRedirect == null) {
            LOG.warn("processAClEntry: sfcRedirect is null");
            return null;
        }

        if (sfcRedirect.getRspName() != null) {
            rsp = getRenderedServicePathFromRsp(sfcRedirect.getRspName());
        } else if (sfcRedirect.getSfpName() != null) {
            LOG.warn("getRenderedServicePath: sfp not handled yet");
        } else {
            rsp = getRenderedServicePathFromSfc(entry);
        }
        LOG.info("getRenderedServicePath: rsp: {}", rsp);
        return rsp;
    }

    private RenderedServicePath getRenderedServicePathFromRsp(String rspName) {
        return null;//getRsp(rspName);
    }

    private RenderedServicePath getRenderedServicePathFromSfc (Ace entry) {
        RedirectToSfc sfcRedirect = entry.getActions().getAugmentation(RedirectToSfc.class);
        LOG.debug("Processing ACL entry = {} sfcRedirect = {}", entry.getRuleName(), sfcRedirect);
        if (sfcRedirect == null) {
            LOG.warn("processAClEntry: sfcRedirect is null");
            return null;
        }

        String sfcName = sfcRedirect.getSfcName();
        ServiceFunctionPath sfp = getSfp(sfcName);
        if (sfp == null || sfp.getName() == null) {
            LOG.warn("There is no configured SFP with sfcName = {}; so skip installing the ACL entry!!", sfcName);
            return null;
        }

        LOG.debug("Processing Redirect to SFC = {}, SFP = {}", sfcName, sfp);
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
                return null;
            }

            // If SFP is symmetric, create RSP in the reverse direction.
            if (sfp.isSymmetric()) {
                LOG.info("SFP = {} is symmetric, installing RSP in the reverse direction!!", sfpName);
                String rspNameRev = rspName + "-Reverse";
                RenderedServicePath rspReverse = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                        getRspId(rspNameRev));
                if (rspReverse == null) {
                    rspReverse = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(rsp);
                    if (rspReverse == null) {
                        LOG.warn("failed to add reverse RSP");
                        return null;
                    }
                }
            }
        }
        return rsp;
    }

    private void handleLocalEgressPort(long dataPathId, String s, long localOfPort, short writeTable,
                                       short gotoTable, boolean write) {

    }

    private void handleEgressSfcClassiferFlows(long dataPathId, short writeTable, String ruleName,
                                               Matches matches, NshUtils nshHeader, long tunnelOfPort,
                                               long outOfPort, boolean write) {
        sfcClassifier.programEgressSfcClassiferFlows(dataPathId, writeTable, ruleName, matches, nshHeader,
                tunnelOfPort, outOfPort, write);
    }

    private void handleSfcClassiferFlows(long dataPathId, short writeTable, String ruleName,
                                         Matches matches, NshUtils nshHeader, long tunnelOfPort,
                                         boolean write) {
        sfcClassifier.programSfcClassiferFlows(dataPathId, writeTable, ruleName, matches, nshHeader,
                tunnelOfPort, write);
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

    private InstanceIdentifier<Classifiers> getClassifierIid() {
        return InstanceIdentifier.create(Classifiers.class);
    }

    public void handleLocalInPort(long dpidLong, String segmentationId, Long inPort,
                                  short writeTable, short goToTableId, Matches matches, boolean write) {
        sfcClassifier.programLocalInPort(dpidLong, segmentationId, inPort, writeTable, goToTableId, matches, write);
    }

    @Override
    public void setDependencies(ServiceReference serviceReference) {
        nodeCacheManager = (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
    }

    @Override
    public void removeRsp(RenderedServicePath change) {

    }
    @Override
    public void updateRsp(RenderedServicePath change) {

    }
}
