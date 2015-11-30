/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.workaround;

import com.google.common.base.Preconditions;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbTables;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.INetvirtSfcOF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.ISfcClassifierService;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.NshUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.SfcUtils;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.acl.rev150105.RedirectToSfc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.Bridges;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.bridges.Bridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetvirtSfcWorkaroundOF13Provider implements INetvirtSfcOF13Provider {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcWorkaroundOF13Provider.class);
    private volatile NodeCacheManager nodeCacheManager;
    private volatile Southbound southbound;
    private volatile ISfcClassifierService sfcClassifierService;
    private static final short SFC_TABLE = 150;
    private static final int GPE_PORT = 6633;
    private static final String NETWORK_TYPE_VXLAN = "vxlan";
    private MdsalUtils mdsalUtils;
    private SfcUtils sfcUtils;
    private static final String VXGPE = "vxgpe";
    private static final String TUNNEL_DST = "192.168.120.31";
    private static final String TUNNEL_VNID = "10";
    public static final String TUNNEL_ENDPOINT_KEY = "local_ip";

    public NetvirtSfcWorkaroundOF13Provider(final DataBroker dataBroker, MdsalUtils mdsalUtils, SfcUtils sfcUtils) {
        Preconditions.checkNotNull(dataBroker, "Input dataBroker cannot be NULL!");
        Preconditions.checkNotNull(mdsalUtils, "Input mdsalUtils cannot be NULL!");
        Preconditions.checkNotNull(sfcUtils, "Input sfcUtils cannot be NULL!");

        this.mdsalUtils = mdsalUtils;
        this.sfcUtils = sfcUtils;
        //this.setDependencies(null);
    }

    public void setSfcClassifierService(ISfcClassifierService sfcClassifierService) {
        this.sfcClassifierService = sfcClassifierService;
    }

    @Override
    public void addClassifierRules(Bridge bridge, Acl acl) {

    }

    @Override
    public void addClassifierRules(Bridges bridges, Acl acl) {
        Preconditions.checkNotNull(bridges, "Input bridges cannot be NULL!");
        Preconditions.checkNotNull(acl, "Input acl cannot be NULL!");
    }

    @Override
    public void removeClassifierRules(Sff sff, Acl acl) {

    }

    @Override
    public void addClassifierRules(Acl acl) {
        String aclName = acl.getAclName();
        Classifiers classifiers = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, sfcUtils.getClassifierIid());
        if (classifiers == null) {
            LOG.debug("addClassifierRules: No Classifiers found");
            return;
        }

        LOG.debug("addClassifierRules: Classifiers: {}", classifiers);
        for (Classifier classifier : classifiers.getClassifier()) {
            if (classifier.getAcl().equals(aclName)) {
                for (Ace ace : acl.getAccessListEntries().getAce()) {
                    processAclEntry(ace);
                }
            }
        }
    }

    @Override
    public void removeClassifierRules(Acl acl) {

    }

    private void processAclEntry(Ace entry) {
        Matches matches = entry.getMatches();
        Preconditions.checkNotNull(matches, "Input bridges cannot be NULL!");

        RenderedServicePath rsp = getRenderedServicePath(entry);
        if (rsp == null) {
            LOG.warn("Failed to get renderedServicePatch for entry: {}", entry);
            return;
        }

        handleRenderedServicePath(rsp, entry);
    }

    private void handleRenderedServicePathOld(RenderedServicePath rsp, Ace entry) {
        LOG.info("handleRenderedServicePath: RSP: {}", rsp);

        Matches matches = entry.getMatches();
        if (matches == null) {
            LOG.warn("processAclEntry: matches not found");
            return;
        }

        List<RenderedServicePathHop> pathHopList = rsp.getRenderedServicePathHop();
        if (pathHopList.isEmpty()) {
            LOG.warn("handleRenderedServicePath: RSP {} has empty hops!!", rsp.getName());
            return;
        }
        LOG.info("handleRenderedServicePath: pathHopList: {}", pathHopList);

        final List<Node> bridgeNodes = nodeCacheManager.getBridgeNodes();
        if (bridgeNodes == null || bridgeNodes.isEmpty()) {
            LOG.warn("handleRenderedServicePath: There are no bridges to process");
            return;
        }
        for (Node bridgeNode : bridgeNodes) {
            OvsdbBridgeAugmentation ovsdbBridgeAugmentation = southbound.getBridge(bridgeNode, "br-int");
            if (ovsdbBridgeAugmentation == null) {
                continue;
            }
            long vxGpeOfPort = getOFPort(bridgeNode, VXGPE);
            if (vxGpeOfPort == 0L) {
                LOG.warn("programAclEntry: Could not identify tunnel port {} -> OF ({}) on {}",
                        VXGPE, vxGpeOfPort, bridgeNode);
                continue;
            }
            long dataPathId = southbound.getDataPathId(bridgeNode);
            if (dataPathId == 0L) {
                LOG.warn("programAclEntry: Could not identify datapathId on {}", bridgeNode);
                continue;
            }

            // Find the first Hop within an RSP.
            // The classifier flow needs to send all matched traffic to this first hop SFF.
            RenderedServicePathFirstHop firstRspHop = SfcProviderRenderedPathAPI
                    .readRenderedServicePathFirstHop(rsp.getName());

            LOG.info("handleRenderedServicePath: firstRspHop: {}", firstRspHop);
            LOG.debug("handleRenderedServicePath: First Hop IPAddress = {}, Port = {}",
                    firstRspHop.getIp().getIpv4Address().getValue(),
                    firstRspHop.getPort().getValue());

            NshUtils nshHeader = new NshUtils();
            nshHeader.setNshMetaC1(NshUtils.convertIpAddressToLong(new Ipv4Address(TUNNEL_DST)));
            nshHeader.setNshMetaC2(Long.parseLong(TUNNEL_VNID)); // get from register //get from
            nshHeader.setNshNsp(rsp.getPathId());

            RenderedServicePathHop firstHop = pathHopList.get(0);
            RenderedServicePathHop lastHop = pathHopList.get(pathHopList.size()-1);
            ServiceFunction serviceFunction =
                    SfcProviderServiceFunctionAPI.readServiceFunction(
                            SfName.getDefaultInstance(firstHop.getServiceFunctionName().getValue()));
            if (serviceFunction == null) {
                LOG.warn("programAclEntry: Could not identify ServiceFunction {} on {}",
                        firstHop.getServiceFunctionName().getValue(), bridgeNode);
                continue;
            }

            nshHeader.setNshNsi(firstHop.getServiceIndex());
            // workaround: bypass sff and got directly to sf
            //nshHeader.setNshTunIpDst(firstRspHop.getIp().getIpv4Address());
            IpAddress sfIpAddress = sfcUtils.getSfIpAddress(serviceFunction);
            String sfDplName = sfcUtils.getSfDplName(serviceFunction);
            //sfcUtils.getSfIp(firstHop.getServiceFunctionName().getValue());
            nshHeader.setNshTunIpDst(sfIpAddress.getIpv4Address());
            nshHeader.setNshTunUdpPort(firstRspHop.getPort());
            LOG.debug("handleRenderedServicePath: NSH Header = {}", nshHeader);

            sfcClassifierService.programIngressClassifier(dataPathId, entry.getRuleName(), matches,
                    nshHeader, vxGpeOfPort, true);

            sfcClassifierService.program_sfEgress(dataPathId, GPE_PORT, true);
            long sfOfPort = getSfPort(bridgeNode, sfDplName);

            String sfMac = getMacFromExternalIds(bridgeNode, sfDplName);
            String sfIpString = new String(sfIpAddress.getValue());
            LOG.info("handleRenderedServicePath: sfDplName: {}, sfMac: {}, sfOfPort: {}, sfIpAddress: {}",
                    sfDplName, sfMac, sfOfPort, sfIpString);
            if (sfMac != null) { // install if the sf is on this bridge, expand when using multiple bridges
                sfcClassifierService.program_sfIngress(dataPathId, GPE_PORT, sfOfPort, sfIpString, sfDplName, true);
                sfcClassifierService.programStaticArpEntry(dataPathId, 0L, sfMac, sfIpString, true);
            }

            short lastServiceindex = (short)((lastHop.getServiceIndex()).intValue() - 1);
            sfcClassifierService.programEgressClassifier(dataPathId, vxGpeOfPort, rsp.getPathId(),
                    lastServiceindex, sfOfPort, 0, true);
            sfcClassifierService.programEgressClassifierBypass(dataPathId, vxGpeOfPort, rsp.getPathId(),
                    lastServiceindex, sfOfPort, 0, true);

            sfcClassifierService.programSfcTable(dataPathId, vxGpeOfPort, SFC_TABLE, true);
        }
    }

    private void handleRenderedServicePath(RenderedServicePath rsp, Ace entry) {
        LOG.info("handleRenderedServicePath: RSP: {}", rsp);

        Matches matches = entry.getMatches();
        if (matches == null) {
            LOG.warn("processAclEntry: matches not found");
            return;
        }

        List<RenderedServicePathHop> pathHopList = rsp.getRenderedServicePathHop();
        if (pathHopList.isEmpty()) {
            LOG.warn("handleRenderedServicePath: RSP {} has empty hops!!", rsp.getName());
            return;
        }
        LOG.info("handleRenderedServicePath: pathHopList: {}", pathHopList);

        RenderedServicePathFirstHop firstRspHop = SfcProviderRenderedPathAPI
                .readRenderedServicePathFirstHop(rsp.getName());
        LOG.info("handleRenderedServicePath: firstRspHop: {}", firstRspHop);

        RenderedServicePathHop firstHop = pathHopList.get(0);
        RenderedServicePathHop lastHop = pathHopList.get(pathHopList.size()-1);

        final List<Node> bridgeNodes = nodeCacheManager.getBridgeNodes();
        if (bridgeNodes == null || bridgeNodes.isEmpty()) {
            LOG.warn("handleRenderedServicePath: There are no bridges to process");
            return;
        }
        for (RenderedServicePathHop hop : pathHopList) {
            for (Node bridgeNode : bridgeNodes) {
                // ignore bridges other than br-int
                OvsdbBridgeAugmentation ovsdbBridgeAugmentation = southbound.getBridge(bridgeNode, "br-int");
                if (ovsdbBridgeAugmentation == null) {
                    continue;
                }
                long vxGpeOfPort = getOFPort(bridgeNode, VXGPE);
                if (vxGpeOfPort == 0L) {
                    LOG.warn("programAclEntry: Could not identify gpe vtep {} -> OF ({}) on {}",
                            VXGPE, vxGpeOfPort, bridgeNode);
                    continue;
                }
                long dataPathId = southbound.getDataPathId(bridgeNode);
                if (dataPathId == 0L) {
                    LOG.warn("programAclEntry: Could not identify datapathId on {}", bridgeNode);
                    continue;
                }

                ServiceFunction serviceFunction =
                        SfcProviderServiceFunctionAPI.readServiceFunction(firstHop.getServiceFunctionName());
                if (serviceFunction == null) {
                    LOG.warn("programAclEntry: Could not identify ServiceFunction {} on {}",
                            firstHop.getServiceFunctionName().getValue(), bridgeNode);
                    continue;
                }
                ServiceFunctionForwarder serviceFunctionForwarder =
                        SfcProviderServiceForwarderAPI
                                .readServiceFunctionForwarder(hop.getServiceFunctionForwarder());
                if (serviceFunctionForwarder == null) {
                    LOG.warn("programAclEntry: Could not identify ServiceFunctionForwarder {} on {}",
                            firstHop.getServiceFunctionName().getValue(), bridgeNode);
                    continue;
                }

                handleSf(bridgeNode, serviceFunction);
                handleSff(bridgeNode, serviceFunctionForwarder, serviceFunction, hop, firstHop, lastHop,
                        entry.getRuleName(), matches, vxGpeOfPort, rsp);
                if (firstHop == lastHop) {
                    handleSff(bridgeNode, serviceFunctionForwarder, serviceFunction, hop, null, lastHop,
                            entry.getRuleName(), matches, vxGpeOfPort, rsp);
                }
            }
        }
    }

    private void handleSff(Node bridgeNode, ServiceFunctionForwarder serviceFunctionForwarder,
                           ServiceFunction serviceFunction,
                           RenderedServicePathHop hop,
                           RenderedServicePathHop firstHop,
                           RenderedServicePathHop lastHop,
                           String ruleName, Matches matches,
                           long vxGpeOfPort, RenderedServicePath rsp) {
        long dataPathId = southbound.getDataPathId(bridgeNode);

        if (hop == firstHop) {
            LOG.info("handleSff: first hop processing {} - {}",
                    bridgeNode.getNodeId(), serviceFunctionForwarder.getName());
            NshUtils nshHeader = new NshUtils();
            nshHeader.setNshNsp(rsp.getPathId());
            nshHeader.setNshNsi(firstHop.getServiceIndex());
            if (isSffOnBridge(bridgeNode, serviceFunctionForwarder)) {
                LOG.info("handleSff: sff and bridge are the same: {} - {}, skipping first sff",
                        bridgeNode.getNodeId(), serviceFunctionForwarder.getName());
                Ip ip = sfcUtils.getSfIp(serviceFunction);
                nshHeader.setNshTunIpDst(ip.getIp().getIpv4Address());
                nshHeader.setNshTunUdpPort(ip.getPort());
            } else {
                LOG.info("handleSff: sff and bridge are not the same: {} - {}, sending to first sff",
                        bridgeNode.getNodeId(), serviceFunctionForwarder.getName());
                Ip ip = sfcUtils.getSffIp(serviceFunctionForwarder);
                nshHeader.setNshTunIpDst(ip.getIp().getIpv4Address());
                nshHeader.setNshTunUdpPort(ip.getPort());
            }
            sfcClassifierService.programIngressClassifier(dataPathId, ruleName, matches,
                    nshHeader, vxGpeOfPort, true);
        } else if (hop == lastHop) {
            LOG.info("handleSff: last hop processing {} - {}",
                    bridgeNode.getNodeId(), serviceFunctionForwarder.getName());
            short lastServiceindex = (short)((lastHop.getServiceIndex()).intValue() - 1);
            String sfDplName = sfcUtils.getSfDplName(serviceFunction);
            long sfOfPort = getSfPort(bridgeNode, sfDplName);
            sfcClassifierService.programEgressClassifier(dataPathId, vxGpeOfPort, rsp.getPathId(),
                    lastServiceindex, sfOfPort, 0, true);
            sfcClassifierService.programEgressClassifierBypass(dataPathId, vxGpeOfPort, rsp.getPathId(),
                    lastServiceindex, sfOfPort, 0, true);
        } else {
            // add typical sff flows
        }

        sfcClassifierService.programSfcTable(dataPathId, vxGpeOfPort, SFC_TABLE, true);
    }

    void handleSf(Node bridgeNode, ServiceFunction serviceFunction) {
        if (isSfOnBridge(bridgeNode, serviceFunction)) {
            LOG.info("handleSf: sf and bridge are on the same node: {} - {}, adding workaround and arp",
                    bridgeNode.getNodeId(), serviceFunction.getName());
            long dataPathId = southbound.getDataPathId(bridgeNode);
            Ip ip = sfcUtils.getSfIp(serviceFunction);
            String sfIpAddr = String.valueOf(ip.getIp().getValue());
            int sfIpPort = ip.getPort().getValue(); //GPE_PORT
            String sfDplName = sfcUtils.getSfDplName(serviceFunction);
            long sfOfPort = getSfPort(bridgeNode, sfDplName);
            String sfMac = getMacFromExternalIds(bridgeNode, sfDplName);
            if (sfMac == null) {
                LOG.warn("handleSff: could not find mac for {} on {}", sfDplName, bridgeNode);
                return;
            }
            //should be sffdplport, but they should all be the same 6633/4790
            sfcClassifierService.program_sfEgress(dataPathId, sfIpPort, true);
            sfcClassifierService.program_sfIngress(dataPathId, sfIpPort, sfOfPort, sfIpAddr, sfDplName, true);
            sfcClassifierService.programStaticArpEntry(dataPathId, 0L, sfMac, sfIpAddr, true);
        }
    }

    private boolean isSffOnBridge(Node bridgeNode, ServiceFunctionForwarder serviceFunctionForwarder) {
        String local_ip = "";
        Ip ip = sfcUtils.getSffIp(serviceFunctionForwarder);
        Node ovsdbNode = southbound.readOvsdbNode(bridgeNode);
        if (ovsdbNode != null) {
            OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
            if (ovsdbNodeAugmentation != null && ovsdbNodeAugmentation.getOpenvswitchOtherConfigs() != null) {
                southbound.getOtherConfig(ovsdbNode, OvsdbTables.OPENVSWITCH, TUNNEL_ENDPOINT_KEY);
            }

        }
        return local_ip.equals(String.valueOf(ip.getIp().getValue()));
    }

    private boolean isSfOnBridge(Node bridgeNode, ServiceFunction serviceFunction) {
        String sfDplName = sfcUtils.getSfDplName(serviceFunction);
        long sfOfPort = getSfPort(bridgeNode, sfDplName);
        return sfOfPort != 0L;
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
        return sfcUtils.getRsp(rspName);
    }

    private RenderedServicePath getRenderedServicePathFromSfc (Ace entry) {
        RedirectToSfc sfcRedirect = entry.getActions().getAugmentation(RedirectToSfc.class);
        LOG.debug("Processing ACL entry = {} sfcRedirect = {}", entry.getRuleName(), sfcRedirect);
        if (sfcRedirect == null) {
            LOG.warn("processAClEntry: sfcRedirect is null");
            return null;
        }

        String sfcName = sfcRedirect.getSfcName();
        ServiceFunctionPath sfp = sfcUtils.getSfp(sfcName);
        if (sfp == null || sfp.getName() == null) {
            LOG.warn("There is no configured SFP with sfcName = {}; so skip installing the ACL entry!!", sfcName);
            return null;
        }

        LOG.debug("Processing Redirect to SFC = {}, SFP = {}", sfcName, sfp);
        // If RSP doesn't exist, create an RSP.
        String sfpName = sfp.getName().getValue();
        RenderedServicePath rsp = sfcUtils.getRspforSfp(sfpName);
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
                        sfcUtils.getRspId(rspNameRev));
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

    // loop through all ports looking for vxlan types, skip vxgpe, keep the rest
    // first pass we only have two tunnels: one for normal vxlan and the other for gpe
    // so just return the first non-gpe vxlan port
    private long getTunnelOfPort(Node bridgeNode, String vxGpePortName) {
        long port = 0L;
        List<OvsdbTerminationPointAugmentation> ovsdbTerminationPointAugmentations =
                southbound.getTerminationPointsOfBridge(bridgeNode);
        if (!ovsdbTerminationPointAugmentations.isEmpty()) {
            for (OvsdbTerminationPointAugmentation terminationPointAugmentation :
                    ovsdbTerminationPointAugmentations) {
                if (terminationPointAugmentation.getInterfaceType() ==
                        SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP.get(NETWORK_TYPE_VXLAN)) {
                    if (!terminationPointAugmentation.getName().equals(vxGpePortName)) {
                        port = terminationPointAugmentation.getOfport();
                        break;
                    }
                }
            }
        }
        return port;
    }

    private long getSfPort(Node bridgeNode, String sfPortName) {
        return getOFPort(bridgeNode, sfPortName);
    }

    private long getOFPort(Node bridgeNode, String portName) {
        long ofPort = 0L;
        OvsdbTerminationPointAugmentation port =
                southbound.extractTerminationPointAugmentation(bridgeNode, portName);
        if (port != null) {
            ofPort = southbound.getOFPort(port);
        }
        if (ofPort == 0L) {
            for (int i = 0; i < 5; i++) {
                LOG.info("Looking for ofPort {}, try: {}", portName, i);
                TerminationPoint tp = southbound.readTerminationPoint(bridgeNode, null, portName);
                if (tp != null) {
                    port = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                    if (port != null) {
                        ofPort = southbound.getOFPort(port);
                        break;
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return ofPort;
    }

    private String getMacFromOptions(Node bridgeNode, String portName) {
        String mac = null;
        OvsdbTerminationPointAugmentation port = southbound.extractTerminationPointAugmentation(bridgeNode, portName);
        LOG.info("getMac: portName: {}, bridgeNode: {},,, port: {}", portName, bridgeNode, port);
        if (port != null && port.getOptions() != null) {
            //mac = southbound.getOptionsValue(port.getOptions(), EXTERNAL_ID_VM_MAC);
            for (Options option : port.getOptions()) {
                LOG.info("getMac: option: {}", option);
                if (option.getOption().equals(Constants.EXTERNAL_ID_VM_MAC)) {
                    mac = option.getValue();
                    break;
                }
            }
        }
        return mac;
    }

    private String getMacFromExternalIds(Node bridgeNode, String portName) {
        String mac = null;
        OvsdbTerminationPointAugmentation port = southbound.getTerminationPointOfBridge(bridgeNode, portName);
        LOG.info("getMac: portName: {}, bridgeNode: {},,, port: {}", portName, bridgeNode, port);
        if (port != null && port.getInterfaceExternalIds() != null) {
            mac = southbound.getInterfaceExternalIdsValue(port, Constants.EXTERNAL_ID_VM_MAC);
        }
        return mac;
    }

    @Override
    public void setDependencies(ServiceReference serviceReference) {
        nodeCacheManager = (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        sfcClassifierService =
                (ISfcClassifierService) ServiceHelper.getGlobalInstance(ISfcClassifierService.class, this);
        LOG.info("sfcClassifierService= {}", sfcClassifierService);
    }
}
