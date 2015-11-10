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
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.INetvirtSfcOF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.ISfcClassifierService;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.NshUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.SfcUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.workaround.services.SfcClassifierService;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
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

    public void setSfcClassifierService(ISfcClassifierService sfcClassifierService) {
        this.sfcClassifierService = sfcClassifierService;
    }

    private MdsalUtils mdsalUtils;
    private SfcUtils sfcUtils;
    private static final String VXGPE = "vxgpe";
    private static final String TUNNEL_DST = "192.168.120.31";
    private static final String TUNNEL_VNID = "10";

    public NetvirtSfcWorkaroundOF13Provider(final DataBroker dataBroker, MdsalUtils mdsalUtils, SfcUtils sfcUtils) {
        Preconditions.checkNotNull(dataBroker, "Input dataBroker cannot be NULL!");
        Preconditions.checkNotNull(mdsalUtils, "Input mdsalUtils cannot be NULL!");
        Preconditions.checkNotNull(sfcUtils, "Input sfcUtils cannot be NULL!");

        this.mdsalUtils = mdsalUtils;
        this.sfcUtils = sfcUtils;
        //this.setDependencies(null);
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

        handleIngressClassifier(rsp, entry);
        //handleEgressClassifier();
        //handleSfLoopback();
    }

    private void handleIngressClassifier(RenderedServicePath rsp, Ace entry) {
        LOG.info("handleIngressClassifier: RSP: {}", rsp);

        Matches matches = entry.getMatches();
        if (matches == null) {
            LOG.warn("processAclEntry: matches not found");
            return;
        }

        List<RenderedServicePathHop> pathHopList = rsp.getRenderedServicePathHop();
        if (pathHopList.isEmpty()) {
            LOG.warn("handleIngressClassifier: RSP {} has empty hops!!", rsp.getName());
            return;
        }
        LOG.info("handleIngressClassifier: pathHopList: {}", pathHopList);

        final List<Node> bridgeNodes = nodeCacheManager.getBridgeNodes();
        if (bridgeNodes == null || bridgeNodes.isEmpty()) {
            LOG.warn("handleIngressClassifier: There are no bridges to process");
            return;
        }
        for (Node bridgeNode : bridgeNodes) {
            OvsdbBridgeAugmentation ovsdbBridgeAugmentation = southbound.getBridge(bridgeNode, "br-int");
            if (ovsdbBridgeAugmentation == null) {
                continue;
            }
            long vxGpeOfPort = southbound.getOFPort(bridgeNode, VXGPE);
            if (vxGpeOfPort == 0L) {
                LOG.warn("programAclEntry: Could not identify tunnel port {} -> OF ({}) on {}",
                        VXGPE, vxGpeOfPort, bridgeNode);
                continue;
            }

            // Find the first Hop within an RSP.
            // The classifier flow needs to send all matched traffic to this first hop SFF.
            RenderedServicePathFirstHop firstRspHop = SfcProviderRenderedPathAPI
                    .readRenderedServicePathFirstHop(rsp.getName());

            LOG.info("handleIngressClassifier: firstRspHop: {}", firstRspHop);
            LOG.debug("handleIngressClassifier: First Hop IPAddress = {}, Port = {}",
                    firstRspHop.getIp().getIpv4Address().getValue(),
                    firstRspHop.getPort().getValue().intValue());

            NshUtils nshHeader = new NshUtils();
            nshHeader.setNshMetaC1(NshUtils.convertIpAddressToLong(new Ipv4Address(TUNNEL_DST)));
            nshHeader.setNshMetaC2(Long.parseLong(TUNNEL_VNID));
            nshHeader.setNshNsp(rsp.getPathId());

            RenderedServicePathHop firstHop = pathHopList.get(0);
            nshHeader.setNshNsi(firstHop.getServiceIndex());
            // workaround: bypass sff and got directly to sf
            //nshHeader.setNshTunIpDst(firstRspHop.getIp().getIpv4Address());
            IpAddress sfIpAddress = sfcUtils.getSfIp(firstHop.getServiceFunctionName().getValue());
            nshHeader.setNshTunIpDst(sfIpAddress.getIpv4Address());
            nshHeader.setNshTunUdpPort(firstRspHop.getPort());
            LOG.debug("handleIngressClassifier: NSH Header = {}", nshHeader);

            sfcClassifierService.programIngressClassifier(
                    southbound.getDataPathId(bridgeNode), entry.getRuleName(),
                    matches, nshHeader, vxGpeOfPort, true);
        }
    }

    // loop through sf's:
    // - program arp responder
    // - program sf to sff
    // - program sff to sf
    private void handleSfWorkaround(RenderedServicePath rsp) {

    }


    private RenderedServicePath getRenderedServicePath (Ace entry) {
        RedirectToSfc sfcRedirect = entry.getActions().getAugmentation(RedirectToSfc.class);
        LOG.debug("Processing ACL entry = {} sfcRedirect = {}", entry.getRuleName(), sfcRedirect);
        if (sfcRedirect == null) {
            LOG.warn("processAClEntry: sfcRedirect is null");
            return null;
        }

        String sfcName = sfcRedirect.getRedirectSfc();
        ServiceFunctionPath sfp = sfcUtils.getSfp(sfcName);
        if (sfp == null || sfp.getName() == null) {
            LOG.warn("There is no configured SFP with sfcName = {}; so skip installing the ACL entry!!", sfcName);
            return null;
        }

        LOG.debug("Processing Redirect to SFC = {}, SFP = {}", sfcRedirect.getRedirectSfc(), sfp);
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

    public Long getOFPort(Node bridgeNode, String portName) {
        Long ofPort = 0L;
        OvsdbTerminationPointAugmentation port = southbound.extractTerminationPointAugmentation(bridgeNode, portName);
        if (port != null) {
            ofPort = southbound.getOFPort(port);
        }
        for (int i = 0; i < 5; i++) {
            LOG.info("Looking for ofPort {}, try: {}", portName, i);
            if (ofPort == 0L) {
                TerminationPoint tp = southbound.readTerminationPoint(bridgeNode, null, portName);
                if (tp != null) {
                    port = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                    if (port != null) {
                        ofPort = southbound.getOFPort(port);
                        break;
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ofPort;
    }

    @Override
    public void setDependencies(ServiceReference serviceReference) {
        nodeCacheManager = (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        sfcClassifierService = (ISfcClassifierService) ServiceHelper.getGlobalInstance(ISfcClassifierService.class, this);
        LOG.info("sfcClassifierService= {}", sfcClassifierService);
    }
}
