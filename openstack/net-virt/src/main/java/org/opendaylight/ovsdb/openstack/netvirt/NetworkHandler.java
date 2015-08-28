/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import java.net.HttpURLConnection;
import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronNetworkCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronNetworkAware;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Network.
 */
public class NetworkHandler extends AbstractHandler implements INeutronNetworkAware, ConfigInterface {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkHandler.class);
    public static final String NETWORK_TYPE_VXLAN = "vxlan";
    public static final String NETWORK_TYPE_GRE = "gre";
    public static final String NETWORK_TYPE_VLAN = "vlan";

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile BridgeConfigurationManager bridgeConfigurationManager;
    private volatile NodeCacheManager nodeCacheManager;
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile NeutronL3Adapter neutronL3Adapter;
    private volatile Southbound southbound;

    /**
     * Invoked when a network creation is requested
     * to indicate if the specified network can be created.
     *
     * @param network  An instance of proposed new Neutron Network object.
     * @return A HTTP status code to the creation request.
     */
    @Override
    public int canCreateNetwork(NeutronNetwork network) {
        if (network.isShared()) {
            LOG.error(" Network shared attribute not supported ");
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        }

        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a network has been created.
     *
     * @param network  An instance of new Neutron Network object.
     */
    @Override
    public void neutronNetworkCreated(NeutronNetwork network) {
        enqueueEvent(new NorthboundEvent(network, Action.ADD));
    }
    private void doNeutronNetworkCreated(NeutronNetwork network) {
        neutronL3Adapter.handleNeutronNetworkEvent(network, Action.ADD);
    }

    /**
     * Invoked when a network update is requested
     * to indicate if the specified network can be changed
     * using the specified delta.
     *
     * @param delta     Updates to the network object using patch semantics.
     * @param original  An instance of the Neutron Network object
     *                  to be updated.
     * @return A HTTP status code to the update request.
     */
    @Override
    public int canUpdateNetwork(NeutronNetwork delta,
                                NeutronNetwork original) {
        if (delta.isShared()) {
            LOG.error(" Network shared attribute not supported ");
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        }

        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a network has been updated.
     *
     * @param network An instance of modified Neutron Network object.
     */
    @Override
    public void neutronNetworkUpdated(NeutronNetwork network) {
        enqueueEvent(new NorthboundEvent(network, Action.UPDATE));
    }
    private void doNeutronNetworkUpdated(NeutronNetwork network) {
        neutronL3Adapter.handleNeutronNetworkEvent(network, Action.UPDATE);
    }

    /**
     * Invoked when a network deletion is requested
     * to indicate if the specified network can be deleted.
     *
     * @param network  An instance of the Neutron Network object to be deleted.
     * @return A HTTP status code to the deletion request.
     */
    @Override
    public int canDeleteNetwork(NeutronNetwork network) {
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a network has been deleted.
     *
     * @param network  An instance of deleted Neutron Network object.
     */
    @Override
    public void neutronNetworkDeleted(NeutronNetwork network) {
        enqueueEvent(new NorthboundEvent(network, Action.DELETE));
    }
    private void doNeutronNetworkDeleted(NeutronNetwork network) {
        neutronL3Adapter.handleNeutronNetworkEvent(network, Action.DELETE);

        /* Is this the last Neutron tenant network */
        List <NeutronNetwork> networks;
        if (neutronNetworkCache != null) {
            networks = neutronNetworkCache.getAllNetworks();
            if (networks.isEmpty()) {
                LOG.trace("neutronNetworkDeleted: last tenant network, delete tunnel ports...");
                List<Node> nodes = nodeCacheManager.getNodes();

                for (Node node : nodes) {
                    List<String> phyIfName = bridgeConfigurationManager.getAllPhysicalInterfaceNames(node);
                    try {
                        List<OvsdbTerminationPointAugmentation> ports = southbound.getTerminationPointsOfBridge(node);
                        for (OvsdbTerminationPointAugmentation port : ports) {
                            if (southbound.isTunnel(port)) {
                                LOG.trace("Delete tunnel interface {}", port.getName());
                                southbound.deleteTerminationPoint(node, port.getName());
                            } else if (!phyIfName.isEmpty() && phyIfName.contains(port.getName())) {
                                LOG.trace("Delete physical interface {}", port.getName());
                                southbound.deleteTerminationPoint(node, port.getName());
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Exception during handlingNeutron network delete", e);
                    }
                }
            }
        }
        tenantNetworkManager.networkDeleted(network.getID());
    }

    /**
     * Process the event.
     *
     * @param abstractEvent the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher
     */
    @Override
    public void processEvent(AbstractEvent abstractEvent) {
        if (!(abstractEvent instanceof NorthboundEvent)) {
            LOG.error("Unable to process abstract event {}", abstractEvent);
            return;
        }
        NorthboundEvent ev = (NorthboundEvent) abstractEvent;
        switch (ev.getAction()) {
            case ADD:
                doNeutronNetworkCreated(ev.getNeutronNetwork());
                break;
            case UPDATE:
                doNeutronNetworkUpdated(ev.getNeutronNetwork());
                break;
            case DELETE:
                doNeutronNetworkDeleted(ev.getNeutronNetwork());
                break;
            default:
                LOG.warn("Unable to process event action {}", ev.getAction());
                break;
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        tenantNetworkManager =
                (TenantNetworkManager) ServiceHelper.getGlobalInstance(TenantNetworkManager.class, this);
        bridgeConfigurationManager =
                (BridgeConfigurationManager) ServiceHelper.getGlobalInstance(BridgeConfigurationManager.class, this);
        nodeCacheManager =
                (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        neutronL3Adapter =
                (NeutronL3Adapter) ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, this);
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        eventDispatcher =
                (EventDispatcher) ServiceHelper.getGlobalInstance(EventDispatcher.class, this);
        eventDispatcher.eventHandlerAdded(
                bundleContext.getServiceReference(INeutronNetworkAware.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {
        if (impl instanceof INeutronNetworkCRUD) {
            neutronNetworkCache = (INeutronNetworkCRUD)impl;
        }
    }
}
