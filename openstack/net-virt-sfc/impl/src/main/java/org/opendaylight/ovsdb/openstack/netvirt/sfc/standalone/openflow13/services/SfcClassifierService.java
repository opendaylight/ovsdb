/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.standalone.openflow13.services;

import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.ISfcClassifierService;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.NshUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcClassifierService extends AbstractServiceInstance implements ConfigInterface, ISfcClassifierService {
    private static final Logger LOG = LoggerFactory.getLogger(SfcClassifierService.class);

    public SfcClassifierService(Service service) {
        super(service);
    }

    public SfcClassifierService() {
        super(Service.SFC_CLASSIFIER);
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(ISfcClassifierService.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {}

    @Override
    public void programIngressClassifier(long dataPathId, String ruleName, Matches matches, NshUtils nshHeader, long vxGpeOfPort, boolean write) {

    }

    @Override
    public void programSfcTable(long dataPathId, long vxGpeOfPort, short goToTableId, boolean write) {

    }

    @Override
    public void programEgressClassifier1(long dataPathId, long vxGpeOfPort, long nsp, short nsi, int tunnelOfPort, int tunnelId, short gotoTableId, boolean write) {

    }

    @Override
    public void programEgressClassifier(long dataPathId, long vxGpeOfPort, long nsp, short nsi, long sfOfPort, int tunnelId, boolean write) {

    }

    @Override
    public void programEgressClassifierBypass(long dataPathId, long vxGpeOfPort, long nsp, short nsi, long sfOfPort, int tunnelId, boolean write) {

    }

    @Override
    public void program_sfEgress(long dataPathId, int dstPort, boolean write) {

    }

    @Override
    public void program_sfIngress(long dataPathId, int dstPort, long sfOfPort, String ipAddress, String sfDplName, boolean write) {

    }

    @Override
    public void programStaticArpEntry(long dataPathId, long ofPort, String macAddressStr, String ipAddress, boolean write) {

    }
}
