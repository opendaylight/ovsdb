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
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SfcUtils.class);
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

    public InstanceIdentifier<ServiceFunction> getSfId(String sfName) {
        return InstanceIdentifier.builder(ServiceFunctions.class)
                .child(ServiceFunction.class, new ServiceFunctionKey(SfName.getDefaultInstance(sfName))).build();
    }

    public RenderedServicePath getRsp(String rspName) {
        return mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, getRspId(rspName));
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

    public IpAddress getSfIpAddress(String sfname) {
        ServiceFunction serviceFunction =
                SfcProviderServiceFunctionAPI.readServiceFunction(SfName.getDefaultInstance(sfname));

        if (serviceFunction == null) {
            LOG.info("Failed to read ServiceFunction: {}", sfname);
            return null;
        }

        return getSfIpAddress(serviceFunction);
    }

    public IpAddress getSfIpAddress(ServiceFunction serviceFunction) {
        if (serviceFunction == null) {
            LOG.info("getSfIp: Servicefunction is null");
            return null;
        }

        Ip ipLocator = (Ip) serviceFunction.getSfDataPlaneLocator().get(0).getLocatorType();
        return ipLocator.getIp();
    }

    public PortNumber getSfPort(ServiceFunction serviceFunction) {
        if (serviceFunction == null) {
            LOG.info("getSfIp: Servicefunction is null");
            return null;
        }

        Ip ipLocator = (Ip) serviceFunction.getSfDataPlaneLocator().get(0).getLocatorType();
        return ipLocator.getPort();
    }

    public Ip getSfIp(ServiceFunction serviceFunction) {
        if (serviceFunction == null) {
            LOG.info("getSfIp: Servicefunction is null");
            return null;
        }

        return (Ip)serviceFunction.getSfDataPlaneLocator().get(0).getLocatorType();
    }

    public String getSfDplName(ServiceFunction serviceFunction) {
        String sfDplName = null;
        if (serviceFunction == null) {
            LOG.warn("getSfDplName: Servicefunction is null");
            return null;
        }

        sfDplName = serviceFunction.getSfDataPlaneLocator().get(0).getName().getValue();
        return sfDplName;
    }

    public Ip getSffIp(ServiceFunctionForwarder serviceFunctionForwarder) {
        if (serviceFunctionForwarder == null) {
            LOG.info("getSfIp: ServicefunctionForwarder is null");
            return null;
        }

        return (Ip)serviceFunctionForwarder.getSffDataPlaneLocator().get(0).getDataPlaneLocator().getLocatorType();
    }
}
