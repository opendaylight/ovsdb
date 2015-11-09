/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.workaround.services;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.INetvirtSfcOF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.SfcUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.acl.rev150105.RedirectToSfc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.Bridges;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.bridges.Bridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetvirtSfcWorkaroundOF13Provider implements INetvirtSfcOF13Provider {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcWorkaroundOF13Provider.class);
    private volatile NodeCacheManager nodeCacheManager;
    private volatile Southbound southbound;
    private MdsalUtils mdsalUtils;
    private SfcUtils sfcUtils;
    private SfcClassifierService sfcClassifierService;

    public NetvirtSfcWorkaroundOF13Provider(final DataBroker dataBroker, MdsalUtils mdsalUtils, SfcUtils sfcUtils) {
        Preconditions.checkNotNull(dataBroker, "Input dataBroker cannot be NULL!");
        Preconditions.checkNotNull(mdsalUtils, "Input mdsalUtils cannot be NULL!");
        Preconditions.checkNotNull(sfcUtils, "Input sfcUtils cannot be NULL!");

        this.mdsalUtils = mdsalUtils;
        this.sfcUtils = sfcUtils;
        this.setDependencies(null);
    }

    @Override
    public void addClassifierRules(Bridge bridge, Acl acl) {

    }

    @Override
    public void addClassifierRules(Bridges bridges, Acl acl) {
        Preconditions.checkNotNull(bridges, "Input bridges cannot be NULL!");
        Preconditions.checkNotNull(acl, "Input acl cannot be NULL!");

        for (Ace ace : acl.getAccessListEntries().getAce()) {
            processAclEntry(ace, bridges);
        }
    }

    @Override
    public void removeClassifierRules(Sff sff, Acl acl) {

    }

    @Override
    public void addClassifierRules(Acl acl) {
        String aclName = acl.getAclName();
        Classifiers classifiers = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, sfcUtils.getClassifierIid());
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

    }

    private void processAclEntry(Ace entry, Bridges bridges) {
        Matches matches = entry.getMatches();
        Preconditions.checkNotNull(matches, "Input bridges cannot be NULL!");

        RenderedServicePath rsp = getRenderedServicePath(entry);
        if (rsp == null) {
            LOG.warn("Failed to get renderedServicePatch for entry: {}", entry);
            return;
        }

        //handleIngressClassifier();
        //handleEgressClassifier();
        //handleSfLoopback();

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

    private void setDependencies(ServiceReference serviceReference) {
        nodeCacheManager = (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        sfcClassifierService = (SfcClassifierService) ServiceHelper.getGlobalInstance(SfcClassifierService.class, this);
    }
}
