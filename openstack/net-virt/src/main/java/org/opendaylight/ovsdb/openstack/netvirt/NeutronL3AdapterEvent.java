/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;

public class NeutronL3AdapterEvent extends AbstractEvent {
    public enum SubType {
        SUBTYPE_EXTERNAL_MAC_UPDATE;  // TODO: Add more subtypes as they come here

        public static final int size = HandlerType.values().length;
    }

    private final SubType subtype;

    private final Long bridgeDpid;
    private final IpAddress gatewayIpAddress;
    private final MacAddress macAddress;

    public NeutronL3AdapterEvent(final Long bridgeDpid, final IpAddress gatewayIpAddress, final MacAddress macAddress) {
        super(HandlerType.NEUTRON_L3_ADAPTER, Action.UPDATE);

        this.subtype = SubType.SUBTYPE_EXTERNAL_MAC_UPDATE;
        this.bridgeDpid = bridgeDpid;
        this.gatewayIpAddress = gatewayIpAddress;
        this.macAddress = macAddress;
    }

    public SubType getSubType() {
        return subtype;
    }

    public Long getBridgeDpid() {
        return bridgeDpid;
    }
    public IpAddress getGatewayIpAddress() {
        return gatewayIpAddress;
    }
    public MacAddress getMacAddress() {
        return macAddress;
    }

    @Override
    public String toString() {
        return "NeutronL3AdapterEvent [handler=" + super.getHandlerType()
                + ", action=" + super.getAction()
                + ", subtype=" + subtype
                + ", bridgeDpid=" + bridgeDpid
                + ", gatewayIpAddress=" + gatewayIpAddress
                + ", macAddress=" + macAddress
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((subtype == null) ? 0 : subtype.hashCode());
        result = prime * result + ((bridgeDpid == null) ? 0 : bridgeDpid.hashCode());
        result = prime * result + ((gatewayIpAddress == null) ? 0 : gatewayIpAddress.hashCode());
        result = prime * result + ((macAddress == null) ? 0 : macAddress.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        NeutronL3AdapterEvent other = (NeutronL3AdapterEvent) obj;
        if (subtype == null) {
            if (other.subtype != null) {
                return false;
            }
        } else if (!subtype.equals(other.subtype)) {
            return false;
        }
        if (bridgeDpid == null) {
            if (other.bridgeDpid != null) {
                return false;
            }
        } else if (!bridgeDpid.equals(other.bridgeDpid)) {
            return false;
        }
        if (gatewayIpAddress == null) {
            if (other.gatewayIpAddress != null) {
                return false;
            }
        } else if (!gatewayIpAddress.equals(other.gatewayIpAddress)) {
            return false;
        }
        if (macAddress == null) {
            if (other.macAddress != null) {
                return false;
            }
        } else if (!macAddress.equals(other.macAddress)) {
            return false;
        }
        return true;
    }
}
