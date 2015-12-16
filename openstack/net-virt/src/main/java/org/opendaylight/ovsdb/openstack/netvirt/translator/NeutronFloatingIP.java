/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronFloatingIP implements Serializable, INeutronObject {
    private static final long serialVersionUID = 1L;

    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement (name = "id")
    String floatingIPUUID;

    @XmlElement (name = "floating_network_id")
    String floatingNetworkUUID;

    @XmlElement (name = "port_id")
    String portUUID;

    @XmlElement (name = "fixed_ip_address")
    String fixedIPAddress;

    @XmlElement (name = "floating_ip_address")
    String floatingIPAddress;

    @XmlElement (name = "tenant_id")
    String tenantUUID;

    @XmlElement (name="router_id")
    String routerUUID;

    @XmlElement (name="status")
    String status;

    public NeutronFloatingIP() {
    }

    public String getID() {
        return floatingIPUUID;
    }

    public void setID(String id) {
        floatingIPUUID = id;
    }

    // @deprecated use getID()
    public String getFloatingIPUUID() {
        return floatingIPUUID;
    }

    // @deprecated use setID()
    public void setFloatingIPUUID(String floatingIPUUID) {
        this.floatingIPUUID = floatingIPUUID;
    }

    public String getFloatingNetworkUUID() {
        return floatingNetworkUUID;
    }

    public void setFloatingNetworkUUID(String floatingNetworkUUID) {
        this.floatingNetworkUUID = floatingNetworkUUID;
    }

    public String getPortUUID() {
        return portUUID;
    }

    public String getRouterUUID() {
        return routerUUID;
    }

    public void setPortUUID(String portUUID) {
        this.portUUID = portUUID;
    }

    public String getFixedIPAddress() {
        return fixedIPAddress;
    }

    public void setFixedIPAddress(String fixedIPAddress) {
        this.fixedIPAddress = fixedIPAddress;
    }

    public String getFloatingIPAddress() {
        return floatingIPAddress;
    }

    public void setFloatingIPAddress(String floatingIPAddress) {
        this.floatingIPAddress = floatingIPAddress;
    }

    public String getTenantUUID() {
        return tenantUUID;
    }

    public void setTenantUUID(String tenantUUID) {
        this.tenantUUID = tenantUUID;
    }

    public void setRouterUUID(String routerUUID) {
        this.routerUUID = routerUUID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * This method copies selected fields from the object and returns them
     * as a new object, suitable for marshaling.
     *
     * @param fields
     *            List of attributes to be extracted
     * @return an OpenStackFloatingIPs object with only the selected fields
     * populated
     */

    public NeutronFloatingIP extractFields(List<String> fields) {
        NeutronFloatingIP ans = new NeutronFloatingIP();
        for (String s : fields) {
            switch (s) {
                case "id":
                    ans.setID(this.getID());
                    break;
                case "floating_network_id":
                    ans.setFloatingNetworkUUID(this.getFloatingNetworkUUID());
                    break;
                case "port_id":
                    ans.setPortUUID(this.getPortUUID());
                    break;
                case "fixed_ip_address":
                    ans.setFixedIPAddress(this.getFixedIPAddress());
                    break;
                case "floating_ip_address":
                    ans.setFloatingIPAddress(this.getFloatingIPAddress());
                    break;
                case "tenant_id":
                    ans.setTenantUUID(this.getTenantUUID());
                    break;
                case "router_id":
                    ans.setRouterUUID(this.getRouterUUID());
                    break;
                case "status":
                    ans.setStatus(this.getStatus());
                    break;
            }
        }
        return ans;
    }

    @Override
    public String toString() {
        return "NeutronFloatingIP{" +
            "fipUUID='" + floatingIPUUID + '\'' +
            ", fipFloatingNetworkId='" + floatingNetworkUUID + '\'' +
            ", fipPortUUID='" + portUUID + '\'' +
            ", fipFixedIPAddress='" + fixedIPAddress + '\'' +
            ", fipFloatingIPAddress=" + floatingIPAddress +
            ", fipTenantId='" + tenantUUID + '\'' +
            ", fipRouterId='" + routerUUID + '\'' +
            ", fipStatus='" + status + '\'' +
            '}';
    }

    public void initDefaults() {
    }
}
