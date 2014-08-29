/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker, Flavio Fernandes
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.controller.networkconfig.neutron.NeutronFloatingIP;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter_Interface;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;

public class NorthboundEvent extends AbstractEvent {

    private NeutronPort port;
    private NeutronSubnet subnet;
    private NeutronRouter router;
    private NeutronRouter_Interface routerInterface;
    private NeutronFloatingIP neutronFloatingIP;
    private NeutronNetwork neutronNetwork;

    NorthboundEvent(NeutronPort port, Action action) {
        super(HandlerType.NEUTRON_PORT, action);
        this.port = port;
    }

    NorthboundEvent(NeutronSubnet subnet, Action action) {
        super(HandlerType.NEUTRON_SUBNET, action);
        this.subnet = subnet;
    }

    NorthboundEvent(NeutronRouter router, Action action) {
        super(HandlerType.NEUTRON_ROUTER, action);
        this.router = router;
    }

    NorthboundEvent(NeutronRouter router, NeutronRouter_Interface routerInterface, Action action) {
        super(HandlerType.NEUTRON_ROUTER, action);
        this.router = router;
        this.routerInterface = routerInterface;
    }

    NorthboundEvent(NeutronFloatingIP neutronFloatingIP, Action action) {
        super(HandlerType.NEUTRON_FLOATING_IP, action);
        this.neutronFloatingIP = neutronFloatingIP;
    }

    NorthboundEvent(NeutronNetwork neutronNetwork, Action action) {
        super(HandlerType.NEUTRON_NETWORK, action);
        this.neutronNetwork = neutronNetwork;
    }

    public NeutronPort getPort() {
        return port;
    }
    public NeutronSubnet getSubnet() {
        return subnet;
    }
    public NeutronRouter getRouter() {
        return router;
    }
    public NeutronRouter_Interface getRouterInterface() {
        return routerInterface;
    }
    public NeutronFloatingIP getNeutronFloatingIP() {
        return neutronFloatingIP;
    }
    public NeutronNetwork getNeutronNetwork() {
        return neutronNetwork;
    }

    @Override
    public String toString() {
        return "NorthboundEvent [action=" + super.getAction()
               + ", port=" + port
               + ", subnet=" + subnet
               + ", router=" + router
               + ", routerInterface=" + routerInterface
               + ", floatingIP=" + neutronFloatingIP
               + ", network=" + neutronNetwork
               + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((port == null) ? 0 : port.hashCode());
        result = prime * result + ((subnet == null) ? 0 : subnet.hashCode());
        result = prime * result + ((router == null) ? 0 : router.hashCode());
        result = prime * result + ((routerInterface == null) ? 0 : routerInterface.hashCode());
        result = prime * result + ((neutronFloatingIP == null) ? 0 : neutronFloatingIP.hashCode());
        result = prime * result + ((neutronNetwork == null) ? 0 : neutronNetwork.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        NorthboundEvent other = (NorthboundEvent) obj;
        if (port == null) {
            if (other.port != null)
                return false;
        } else if (!port.equals(other.port))
            return false;
        if (subnet == null) {
            if (other.subnet != null)
                return false;
        } else if (!subnet.equals(other.subnet))
            return false;
        if (router == null) {
            if (other.router != null)
                return false;
        } else if (!router.equals(other.router))
            return false;
        if (routerInterface == null) {
            if (other.routerInterface != null)
                return false;
        } else if (!routerInterface.equals(other.routerInterface))
            return false;
        if (neutronFloatingIP == null) {
            if (other.neutronFloatingIP != null)
                return false;
        } else if (!neutronFloatingIP.equals(other.neutronFloatingIP))
            return false;
        if (neutronNetwork == null) {
            if (other.neutronNetwork != null)
                return false;
        } else if (!neutronNetwork.equals(other.neutronNetwork))
            return false;
        return true;
    }
}
