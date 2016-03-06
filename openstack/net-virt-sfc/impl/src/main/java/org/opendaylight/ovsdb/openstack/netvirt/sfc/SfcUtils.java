/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import java.util.List;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.acl.rev150105.RedirectToSfc;
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

    public ServiceFunctionPath getSfp(String sfcName) {
        ServiceFunctionPath sfpFound = null;
        ServiceFunctionPaths sfps = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        if (sfps != null) {
            for (ServiceFunctionPath sfp: sfps.getServiceFunctionPath()) {
                if (sfp.getServiceChainName().getValue().equalsIgnoreCase(sfcName)) {
                    sfpFound = sfp;
                }
            }
        }
        return sfpFound;
    }

    private AccessLists readAccessLists() {
        InstanceIdentifier<AccessLists> path = InstanceIdentifier.create(AccessLists.class);
        return mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
    }

    public Ace getAce(RenderedServicePath rsp) {
        return getAce(rsp.getName().getValue(), rsp.getParentServiceFunctionPath().getValue(),
                rsp.getServiceChainName().getValue());
    }

    // TODO: optimize this by adding a ACL to RSP mapping in the netvirt-classifier when the ACL is processed
    public Ace getAce(String rspName, String sfpName, String sfcName) {
        Ace aceFound = null;
        AccessLists accessLists = readAccessLists();
        if (accessLists != null) {
            List<Acl> acls = accessLists.getAcl();
            if (acls != null) {
                for (Acl acl : acls) {
                    AccessListEntries accessListEntries = acl.getAccessListEntries();
                    if (accessListEntries != null) {
                        List<Ace> aces = accessListEntries.getAce();
                        for (Ace ace : aces) {
                            RedirectToSfc sfcRedirect = ace.getActions().getAugmentation(RedirectToSfc.class);
                            if (sfcRedirect != null) {
                                if ((sfcRedirect.getRspName() != null && sfcRedirect.getRspName().equals(rspName)) ||
                                    (sfcRedirect.getSfcName() != null && sfcRedirect.getSfcName().equals(sfcName)) ||
                                    (sfcRedirect.getSfpName() != null && sfcRedirect.getSfpName().equals(sfpName))) {
                                    aceFound = ace;
                                    break;
                                }
                            }
                        }
                    }
                    if (aceFound != null) {
                        break;
                    }
                }
            }
        }

        LOG.info("getAce: {}", aceFound);
        return aceFound;
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
        if (serviceFunctionForwarder != null &&
                serviceFunctionForwarder.getSffDataPlaneLocator() != null &&
                serviceFunctionForwarder.getSffDataPlaneLocator().get(0) != null &&
                serviceFunctionForwarder.getSffDataPlaneLocator().get(0).getDataPlaneLocator() != null) {
            return (Ip)serviceFunctionForwarder.getSffDataPlaneLocator().get(0)
                    .getDataPlaneLocator().getLocatorType();
        } else {
            LOG.info("getSffIp: ServiceFunctionForwarder is null");
            return null;
        }
    }

    public Ip getSffIp(SffName sffName) {
        ServiceFunctionForwarder serviceFunctionForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
        return getSffIp(serviceFunctionForwarder);
    }

    public RenderedServicePathHop getFirstHop(RenderedServicePath rsp) {
        List<RenderedServicePathHop> pathHopList = rsp.getRenderedServicePathHop();
        if (pathHopList.isEmpty()) {
            LOG.warn("handleRenderedServicePath: RSP {} has empty hops!!", rsp.getName());
            return null;
        }

        return pathHopList.get(0);
    }

    public RenderedServicePathHop getLastHop(RenderedServicePath rsp) {
        List<RenderedServicePathHop> pathHopList = rsp.getRenderedServicePathHop();
        if (pathHopList.isEmpty()) {
            LOG.warn("handleRenderedServicePath: RSP {} has empty hops!!", rsp.getName());
            return null;
        }

        return pathHopList.get(pathHopList.size()-1);
    }
}
