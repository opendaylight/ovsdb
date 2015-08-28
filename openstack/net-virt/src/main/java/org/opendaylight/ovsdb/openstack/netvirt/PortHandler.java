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

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronPortAware;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Port.
 */
public class PortHandler extends AbstractHandler implements INeutronPortAware, ConfigInterface {
    private static final Logger LOG = LoggerFactory.getLogger(PortHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile NodeCacheManager nodeCacheManager;
    private volatile NeutronL3Adapter neutronL3Adapter;
    private volatile Southbound southbound;

    /**
     * Invoked when a port creation is requested
     * to indicate if the specified port can be created.
     *
     * @param port     An instance of proposed new Port object.
     * @return A HTTP status code to the creation request.
     */
    @Override
    public int canCreatePort(NeutronPort port) {
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a port has been created.
     *
     * @param neutronPort An instance of new Neutron Port object.
     */
    @Override
    public void neutronPortCreated(NeutronPort neutronPort) {
        enqueueEvent(new NorthboundEvent(neutronPort, Action.ADD));
    }
    private void doNeutronPortCreated(NeutronPort neutronPort) {
        LOG.debug(" Port-ADD successful for tenant-id - {}, network-id - {}, port-id - {}",
                     neutronPort.getTenantID(), neutronPort.getNetworkUUID(),
                     neutronPort.getID());
        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.ADD);
    }

    /**
     * Invoked when a port update is requested
     * to indicate if the specified port can be changed
     * using the specified delta.
     *
     * @param delta    Updates to the port object using patch semantics.
     * @param original An instance of the Neutron Port object
     *                  to be updated.
     * @return A HTTP status code to the update request.
     */
    @Override
    public int canUpdatePort(NeutronPort delta,
                             NeutronPort original) {
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a port has been updated.
     *
     * @param neutronPort An instance of modified Neutron Port object.
     */
    @Override
    public void neutronPortUpdated(NeutronPort neutronPort) {
        enqueueEvent(new NorthboundEvent(neutronPort, Action.UPDATE));
    }
    private void doNeutronPortUpdated(NeutronPort neutronPort) {
        LOG.debug("Handling neutron update port {}", neutronPort);
        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.UPDATE);
    }

    /**
     * Invoked when a port deletion is requested
     * to indicate if the specified port can be deleted.
     *
     * @param port     An instance of the Neutron Port object to be deleted.
     * @return A HTTP status code to the deletion request.
     */
    @Override
    public int canDeletePort(NeutronPort port) {
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a port has been deleted.
     *
     * @param neutronPort  An instance of deleted Neutron Port object.
     */
    @Override
    public void neutronPortDeleted(NeutronPort neutronPort) {
        enqueueEvent(new NorthboundEvent(neutronPort, Action.DELETE));
    }
    private void doNeutronPortDeleted(NeutronPort neutronPort) {
        LOG.debug("Handling neutron delete port {}", neutronPort);
        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.DELETE);
        //TODO: Need to implement getNodes
        List<Node> nodes = nodeCacheManager.getNodes();
        for (Node node : nodes) {
            try {
                List<OvsdbTerminationPointAugmentation> ports = southbound.getTerminationPointsOfBridge(node);
                for (OvsdbTerminationPointAugmentation port : ports) {
                    String neutronPortId =
                            southbound.getInterfaceExternalIdsValue(port, Constants.EXTERNAL_ID_INTERFACE_ID);
                    if (neutronPortId != null && neutronPortId.equalsIgnoreCase(neutronPort.getPortUUID())) {
                        LOG.trace("neutronPortDeleted: Delete interface {}", port.getName());
                        southbound.deleteTerminationPoint(node, port.getName());
                        break;
                    }
                }
            } catch (Exception e) {
                LOG.error("Exception during handlingNeutron port delete", e);
            }
        }
        LOG.debug(" PORT delete successful for tenant-id - {}, network-id - {}, port-id - {}",
                     neutronPort.getTenantID(), neutronPort.getNetworkUUID(),
                     neutronPort.getID());
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
                doNeutronPortCreated(ev.getPort());
                break;
            case DELETE:
                doNeutronPortDeleted(ev.getPort());
                break;
            case UPDATE:
                doNeutronPortUpdated(ev.getPort());
                break;
            default:
                LOG.warn("Unable to process event action {}", ev.getAction());
                break;
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        nodeCacheManager =
                (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        neutronL3Adapter =
                (NeutronL3Adapter) ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, this);
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        eventDispatcher =
                (EventDispatcher) ServiceHelper.getGlobalInstance(EventDispatcher.class, this);
        eventDispatcher.eventHandlerAdded(
                bundleContext.getServiceReference(INeutronPortAware.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
