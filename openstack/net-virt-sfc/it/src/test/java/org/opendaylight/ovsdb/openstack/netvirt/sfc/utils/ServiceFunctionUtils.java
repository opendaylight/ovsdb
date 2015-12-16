/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.utils;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.entry.SfDataPlaneLocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;

public class ServiceFunctionUtils extends AbstractUtils {
    public SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder(SfDataPlaneLocatorBuilder sfDataPlaneLocatorBuilder,
                                                               String ip, int port, String dplName, String sffName) {
        return sfDataPlaneLocatorBuilder
                .setLocatorType(ipBuilder(ip, port).build())
                .setName(new SfDataPlaneLocatorName(dplName))
                .setTransport(VxlanGpe.class)
                .setServiceFunctionForwarder(new SffName(sffName));
    }

    public ServiceFunctionBuilder serviceFunctionBuilder(ServiceFunctionBuilder serviceFunctionBuilder,
                                                         String ip, String sfName,
                                                         List<SfDataPlaneLocator> sfDataPlaneLocatorList,
                                                         SftType type) {
        return serviceFunctionBuilder
                .setSfDataPlaneLocator(sfDataPlaneLocatorList)
                .setName(new SfName(sfName))
                .setIpMgmtAddress(new IpAddress(ip.toCharArray()))
                .setType(type)
                .setNshAware(true);
    }

    public ServiceFunctionsBuilder serviceFunctionsBuilder(ServiceFunctionsBuilder serviceFunctionsBuilder,
                                                           List<ServiceFunction> serviceFunctionList) {
        return serviceFunctionsBuilder.setServiceFunction(serviceFunctionList);
    }

    public ServiceFunctionBuilder serviceFunctionBuilder(String sfIp, int port, String sf1DplName,
                                                         String sffname, String sfName) {
        SfDataPlaneLocatorBuilder sfDataPlaneLocator =
                sfDataPlaneLocatorBuilder(new SfDataPlaneLocatorBuilder(), sfIp, port, sf1DplName, sffname);
        List<SfDataPlaneLocator> sfDataPlaneLocatorList =
                list(new ArrayList<SfDataPlaneLocator>(), sfDataPlaneLocator);
        return serviceFunctionBuilder(
                new ServiceFunctionBuilder(), sfIp, sfName, sfDataPlaneLocatorList, new SftType("firewall"));
    }


}
