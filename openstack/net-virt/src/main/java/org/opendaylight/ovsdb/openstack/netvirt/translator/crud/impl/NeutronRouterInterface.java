/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter_Interface;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter_NetworkReference;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronRouterCRUD;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.RouterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.router.ExternalGatewayInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.router.ExternalGatewayInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.router.Interfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.router.external_gateway_info.ExternalFixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.router.external_gateway_info.ExternalFixedIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronRouterInterface extends  AbstractNeutronInterface<Router, NeutronRouter> implements INeutronRouterCRUD {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeutronRouterInterface.class);
    // methods needed for creating caches


    NeutronRouterInterface(ProviderContext providerContext) {
        super(providerContext);
    }


    // IfNBRouterCRUD Interface methods

    @Override
    public boolean routerExists(String uuid) {
        Router router = readMd(createInstanceIdentifier(toMd(uuid)));
        return router != null;
    }

    @Override
    public NeutronRouter getRouter(String uuid) {
        Router router = readMd(createInstanceIdentifier(toMd(uuid)));
        if (router == null) {
            return null;
        }
        return fromMd(router);
    }

    @Override
    public List<NeutronRouter> getAllRouters() {
        Set<NeutronRouter> allRouters = new HashSet<NeutronRouter>();
        Routers routers = readMd(createInstanceIdentifier());
        if (routers != null) {
            for (Router router: routers.getRouter()) {
                allRouters.add(fromMd(router));
            }
        }
        LOGGER.debug("Exiting getAllRouters, Found {} Routers", allRouters.size());
        List<NeutronRouter> ans = new ArrayList<NeutronRouter>();
        ans.addAll(allRouters);
        return ans;
    }

    @Override
    public boolean addRouter(NeutronRouter input) {
        if (routerExists(input.getID())) {
            return false;
        }
        addMd(input);
        return true;
    }

    @Override
    public boolean removeRouter(String uuid) {
        if (!routerExists(uuid)) {
            return false;
        }
        return removeMd(toMd(uuid));
    }

    @Override
    public boolean updateRouter(String uuid, NeutronRouter delta) {
        if (!routerExists(uuid)) {
            return false;
        }
        updateMd(delta);
        return true;
    }

    @Override
    public boolean routerInUse(String routerUUID) {
        if (!routerExists(routerUUID)) {
            return true;
        }
        NeutronRouter target = getRouter(routerUUID);
        return (target.getInterfaces().size() > 0);
    }

    @Override
    protected Router toMd(NeutronRouter router) {

        RouterBuilder routerBuilder = new RouterBuilder();

        if (router.getRouterUUID() != null) {
            routerBuilder.setUuid(toUuid(router.getRouterUUID()));
        }
        if (router.getName() != null) {
            routerBuilder.setName(router.getName());
        }
        if (router.getTenantID() != null && !router.getTenantID().isEmpty()) {
            routerBuilder.setTenantId(toUuid(router.getTenantID()));
        }
        if (router.getStatus() != null) {
            routerBuilder.setStatus(router.getStatus());
        }
        if (router.getGatewayPortId() != null && !router.getGatewayPortId().isEmpty()) {
            routerBuilder.setGatewayPortId(toUuid(router.getGatewayPortId()));
        }
        routerBuilder.setAdminStateUp(router.getAdminStateUp());
        routerBuilder.setDistributed(router.getDistributed());
        if (router.getRoutes() != null) {
            List<String> routes = new ArrayList<String>();
            for (String route : router.getRoutes()) {
                routes.add(route);
            }
            routerBuilder.setRoutes(routes);
        }
        if (router.getExternalGatewayInfo() != null) {
            ExternalGatewayInfo externalGatewayInfo = null;
            List<NeutronRouter_NetworkReference> neutronRouter_NetworkReferences = new ArrayList<NeutronRouter_NetworkReference>();
            neutronRouter_NetworkReferences.add(router.getExternalGatewayInfo());
            for (NeutronRouter_NetworkReference externalGatewayInfos : neutronRouter_NetworkReferences) {
                ExternalGatewayInfoBuilder builder = new ExternalGatewayInfoBuilder();
                builder.setEnableSnat(externalGatewayInfos.getEnableSNAT());
                builder.setExternalNetworkId(toUuid(externalGatewayInfos.getNetworkID()));
                if (externalGatewayInfos.getExternalFixedIPs() != null) {
                    List<ExternalFixedIps> externalFixedIps = new ArrayList<ExternalFixedIps>();
                    for (Neutron_IPs eIP : externalGatewayInfos.getExternalFixedIPs()) {
                        ExternalFixedIpsBuilder eFixedIpBuilder = new ExternalFixedIpsBuilder();
                        eFixedIpBuilder.setIpAddress(new IpAddress(eIP.getIpAddress().toCharArray()));
                        eFixedIpBuilder.setSubnetId(toUuid(eIP.getSubnetUUID()));
                        externalFixedIps.add(eFixedIpBuilder.build());
                    }
                    builder.setExternalFixedIps(externalFixedIps);
                }
                externalGatewayInfo = builder.build();
            }
            routerBuilder.setExternalGatewayInfo(externalGatewayInfo);
        }
        if (router.getInterfaces() != null) {
            Map<String, NeutronRouter_Interface> mapInterfaces = new HashMap<String, NeutronRouter_Interface>();
            List<Interfaces> interfaces = new ArrayList<Interfaces>();
            for (Entry<String, NeutronRouter_Interface> entry : mapInterfaces.entrySet()) {
                interfaces.add((Interfaces) entry.getValue());
            }
            routerBuilder.setInterfaces(interfaces);
        }
        if (router.getID() != null) {
            routerBuilder.setUuid(toUuid(router.getID()));
        } else {
            LOGGER.warn("Attempting to write neutron router without UUID");
        }
        return routerBuilder.build();
    }

    @Override
    protected InstanceIdentifier<Router> createInstanceIdentifier(Router router) {
        return InstanceIdentifier.create(Neutron.class)
                 .child(Routers.class)
                 .child(Router.class, router.getKey());
    }

    protected InstanceIdentifier<Routers> createInstanceIdentifier() {
        return InstanceIdentifier.create(Neutron.class).child(Routers.class);
    }

    @Override
    protected Router toMd(String uuid) {
        RouterBuilder routerBuilder = new RouterBuilder();
        routerBuilder.setUuid(toUuid(uuid));
        return routerBuilder.build();
    }

    public NeutronRouter fromMd(Router router) {
        NeutronRouter result = new NeutronRouter();
        result.setID(String.valueOf(router.getUuid().getValue()));
        result.setName(router.getName());
        result.setTenantID(String.valueOf(router.getTenantId().getValue()));
        result.setAdminStateUp(router.isAdminStateUp());
        result.setStatus(router.getStatus());
        result.setDistributed(router.isDistributed());
        if (router.getGatewayPortId() != null) {
            result.setGatewayPortId(String.valueOf(router.getGatewayPortId().getValue()));
        }
        if (router.getRoutes() != null) {
            List<String> routes = new ArrayList<String>();
            for (String route : router.getRoutes()) {
                routes.add(route);
            }
            result.setRoutes(routes);
        }

        if (router.getExternalGatewayInfo() != null) {
            NeutronRouter_NetworkReference extGwInfo = new NeutronRouter_NetworkReference();
            extGwInfo.setNetworkID(String.valueOf(router.getExternalGatewayInfo().getExternalNetworkId().getValue()));
            extGwInfo.setEnableSNAT(router.getExternalGatewayInfo().isEnableSnat());
            if (router.getExternalGatewayInfo().getExternalFixedIps() != null) {
                List<Neutron_IPs> fixedIPs = new ArrayList<Neutron_IPs>();
                for (ExternalFixedIps mdFixedIP : router.getExternalGatewayInfo().getExternalFixedIps()) {
                     Neutron_IPs fixedIP = new Neutron_IPs();
                     fixedIP.setSubnetUUID(String.valueOf(mdFixedIP.getSubnetId().getValue()));
                     fixedIP.setIpAddress(String.valueOf(mdFixedIP.getIpAddress().getValue()));
                     fixedIPs.add(fixedIP);
                }
                extGwInfo.setExternalFixedIPs(fixedIPs);
            }
            result.setExternalGatewayInfo(extGwInfo);
        }

        if (router.getInterfaces() != null) {
            Map<String, NeutronRouter_Interface> interfaces = new HashMap<String, NeutronRouter_Interface>();
            for (Interfaces mdInterface : router.getInterfaces()) {
                NeutronRouter_Interface pojoInterface = new NeutronRouter_Interface();
                String id = String.valueOf(mdInterface.getUuid().getValue());
                pojoInterface.setID(id);
                pojoInterface.setTenantID(String.valueOf(mdInterface.getTenantId().getValue()));
                pojoInterface.setSubnetUUID(String.valueOf(mdInterface.getSubnetId().getValue()));
                pojoInterface.setPortUUID(String.valueOf(mdInterface.getPortId().getValue()));
                interfaces.put(id, pojoInterface);
            }
            result.setInterfaces(interfaces);
        }
        return result;
    }
    
    public static void registerNewInterface(BundleContext context,
            ProviderContext providerContext,
            List<ServiceRegistration<?>> registrations) {
    	NeutronRouterInterface neutronRouterInterface = new NeutronRouterInterface(providerContext);
    	ServiceRegistration<INeutronRouterCRUD> neutronRouterInterfaceRegistration = context.registerService(INeutronRouterCRUD.class, neutronRouterInterface, null);
    	if(neutronRouterInterfaceRegistration != null) {
    		registrations.add(neutronRouterInterfaceRegistration);
    	}
	}
}
