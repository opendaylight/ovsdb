/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter_Interface;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter_NetworkReference;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronRouterAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.router.Interfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.router.external_gateway_info.ExternalFixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronRouterChangeListener implements DataChangeListener, AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(NeutronRouterChangeListener.class);

    private ListenerRegistration<DataChangeListener> registration;
    private DataBroker db;

    public NeutronRouterChangeListener(DataBroker db){
        this.db = db;
        InstanceIdentifier<Router> path = InstanceIdentifier
                .create(Neutron.class)
                .child(Routers.class)
                .child(Router.class);
        LOG.debug("Register listener for Neutron Router model data changes");
        registration =
                this.db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, this, DataChangeScope.ONE);

    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.trace("Data changes : {}",changes);

        Object[] subscribers = NeutronIAwareUtil.getInstances(INeutronRouterAware.class, this);
        createRouter(changes, subscribers);
        updateRouter(changes, subscribers);
        deleteRouter(changes, subscribers);
    }

    private void createRouter(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> newRouter : changes.getCreatedData().entrySet()) {
            NeutronRouter router = fromMd((Router)newRouter.getValue());
            for(Object entry: subscribers){
                INeutronRouterAware subscriber = (INeutronRouterAware)entry;
                subscriber.neutronRouterCreated(router);
            }
        }

    }

    private void updateRouter(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> updateRouter : changes.getUpdatedData().entrySet()) {
            NeutronRouter router = fromMd((Router)updateRouter.getValue());
            for(Object entry: subscribers){
                INeutronRouterAware subscriber = (INeutronRouterAware)entry;
                subscriber.neutronRouterUpdated(router);
            }
        }
    }

    private void deleteRouter(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (InstanceIdentifier<?> deletedRouterPath : changes.getRemovedPaths()) {
            NeutronRouter router = fromMd((Router)changes.getOriginalData().get(deletedRouterPath));
            for(Object entry: subscribers){
                INeutronRouterAware subscriber = (INeutronRouterAware)entry;
                subscriber.neutronRouterDeleted(router);
            }
        }
    }

    /*
     * This method is borrowed from NeutronRouterInterface.java class of Neutron Northbound class.
     * We will be utilizing similar code from other classes from the same package of neutron project.
     */
    private NeutronRouter fromMd(Router router) {
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

    @Override
    public void close() throws Exception {
        registration.close();
    }
}
