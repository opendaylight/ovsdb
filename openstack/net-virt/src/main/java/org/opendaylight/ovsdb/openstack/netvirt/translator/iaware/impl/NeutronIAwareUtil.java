/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter_Interface;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.router.Interfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronIAwareUtil {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(NeutronIAwareUtil.class);

    private NeutronIAwareUtil() {
    }

    public static Object[] getInstances(Class<?> clazz,Object bundle) {
        Object instances[] = null;
        try {
            BundleContext bCtx = FrameworkUtil.getBundle(bundle.getClass())
                    .getBundleContext();

            ServiceReference<?>[] services = null;
                services = bCtx.getServiceReferences(clazz.getName(),
                        null);
            if (services != null) {
                instances = new Object[services.length];
                for (int i = 0; i < services.length; i++) {
                    instances[i] = bCtx.getService(services[i]);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Instance reference is NULL", e);
        }
        return instances;
    }

    public static List<Neutron_IPs> convertMDSalIpToNeutronIp(List<FixedIps> fixedIps) {
        List<Neutron_IPs> ips = null;
        if (fixedIps != null) {
            ips = new ArrayList<Neutron_IPs>();
            for (FixedIps mdIP : fixedIps) {
                Neutron_IPs ip = new Neutron_IPs();
                ip.setIpAddress(String.valueOf(mdIP.getIpAddress().getValue()));
                ip.setSubnetUUID(mdIP.getSubnetId().getValue());
                ips.add(ip);
            }
        }
        return ips;
    }

    public static NeutronRouter_Interface convertMDSalInterfaceToNeutronRouterInterface(
            Port routerInterface) {
        NeutronRouter_Interface neutronInterface = new NeutronRouter_Interface();
        String id = String.valueOf(routerInterface.getUuid().getValue());
        neutronInterface.setID(id);
        if (routerInterface.getTenantId() != null) {
            neutronInterface.setTenantID(routerInterface.getTenantId().getValue());
        }
        neutronInterface.setSubnetUUID(routerInterface.getFixedIps().get(0).getSubnetId().getValue());
        neutronInterface.setPortUUID(routerInterface.getUuid().getValue());
        return neutronInterface;
    }



}
