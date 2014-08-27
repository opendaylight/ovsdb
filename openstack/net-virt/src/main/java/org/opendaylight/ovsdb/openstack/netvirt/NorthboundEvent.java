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

import org.opendaylight.controller.networkconfig.neutron.NeutronPort;

public class NorthboundEvent extends AbstractEvent {

    private NeutronPort port;

    NorthboundEvent(NeutronPort port, Action action) {
        super(HandlerType.NEUTRON_PORT, action);
        this.port = port;
    }

    public NeutronPort getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "NorthboundEvent [action=" + super.getAction() + ", port=" + port + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((port == null) ? 0 : port.hashCode());
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
        return true;
    }
}
