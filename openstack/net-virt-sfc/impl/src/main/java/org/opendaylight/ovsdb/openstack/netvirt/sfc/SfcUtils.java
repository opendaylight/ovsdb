/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SfcUtils {
    private MdsalUtils mdsalUtils;

    public SfcUtils(MdsalUtils mdsalUtils) {
        this.mdsalUtils = mdsalUtils;
    }

    public InstanceIdentifier<Classifiers> getClassifierIid() {
        return InstanceIdentifier.create(Classifiers.class);
    }

    public InstanceIdentifier<RenderedServicePaths> getRspsId() {
        return InstanceIdentifier.builder(RenderedServicePaths.class).build();
    }

    public InstanceIdentifier<RenderedServicePath> getRspId(String rspName) {
        return InstanceIdentifier.builder(RenderedServicePaths.class)
                .child(RenderedServicePath.class, new RenderedServicePathKey(new RspName(rspName))).build();
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
}
