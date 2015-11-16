/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronRouter implements Serializable, INeutronObject {
    private static final long serialVersionUID = 1L;

    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes
    @XmlElement (name = "id")
    String routerUUID;

    @XmlElement (name = "name")
    String name;

    @XmlElement (defaultValue = "true", name = "admin_state_up")
    Boolean adminStateUp;

    @XmlElement (name = "status")
    String status;

    @XmlElement (name = "tenant_id")
    String tenantID;

    @XmlElement (name = "external_gateway_info", nillable = true)
    NeutronRouter_NetworkReference externalGatewayInfo;

    @XmlElement (name = "distributed")
    Boolean distributed;

    @XmlElement (name = "gw_port_id", nillable = true)
    String gatewayPortId;

    @XmlElement (name = "routes")
    List<String> routes;

    /* Holds a map of OpenStackRouterInterfaces by subnet UUID
     * used for internal mapping to DOVE
     */
    Map<String, NeutronRouter_Interface> interfaces;

    public NeutronRouter() {
        interfaces = new HashMap<>();
    }

    public String getID() { return routerUUID; }

    public void setID(String id) { this.routerUUID = id; }

    public String getRouterUUID() {
        return routerUUID;
    }

    public void setRouterUUID(String routerUUID) {
        this.routerUUID = routerUUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAdminStateUp() {
        if (adminStateUp == null) {
            return true;
        }
        return adminStateUp;
    }

    public Boolean getAdminStateUp() { return adminStateUp; }

    public void setAdminStateUp(Boolean newValue) {
        adminStateUp = newValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTenantID() {
        return tenantID;
    }

    public void setTenantID(String tenantID) {
        this.tenantID = tenantID;
    }

    public NeutronRouter_NetworkReference getExternalGatewayInfo() {
        return externalGatewayInfo;
    }

    public void setExternalGatewayInfo(NeutronRouter_NetworkReference externalGatewayInfo) {
        this.externalGatewayInfo = externalGatewayInfo;
    }

    public Boolean getDistributed() {
        return distributed;
    }

    public void setDistributed(Boolean distributed) {
        this.distributed = distributed;
    }

    public String getGatewayPortId() {
        return gatewayPortId;
    }

    public void setGatewayPortId(String gatewayPortId) {
        this.gatewayPortId = gatewayPortId;
    }

    public List<String> getRoutes() {
        return routes;
    }

    public void setRoutes(List<String> routes) {
        this.routes = routes;
    }

    /**
     * This method copies selected fields from the object and returns them
     * as a new object, suitable for marshaling.
     *
     * @param fields
     *            List of attributes to be extracted
     * @return an OpenStackRouters object with only the selected fields
     * populated
     */
    public NeutronRouter extractFields(List<String> fields) {
        NeutronRouter ans = new NeutronRouter();
        for (String s : fields) {
            switch (s) {
                case "id":
                    ans.setRouterUUID(this.getRouterUUID());
                    break;
                case "name":
                    ans.setName(this.getName());
                    break;
                case "admin_state_up":
                    ans.setAdminStateUp(this.getAdminStateUp());
                    break;
                case "status":
                    ans.setStatus(this.getStatus());
                    break;
                case "tenant_id":
                    ans.setTenantID(this.getTenantID());
                    break;
                case "external_gateway_info":
                    ans.setExternalGatewayInfo(this.getExternalGatewayInfo());
                    break;
                case "distributed":
                    ans.setDistributed(this.getDistributed());
                    break;
                case "gw_port_id":
                    ans.setGatewayPortId(this.getGatewayPortId());
                    break;
                case "routes":
                    ans.setRoutes(this.getRoutes());
                    break;
            }
        }
        return ans;
    }

    public void setInterfaces(Map<String, NeutronRouter_Interface> input) {
        interfaces = input;
    }

    public Map<String, NeutronRouter_Interface> getInterfaces() {
        return interfaces;
    }

    public void addInterface(String s, NeutronRouter_Interface i) {
        interfaces.put(s, i);
    }

    public void removeInterface(String s) {
        interfaces.remove(s);
    }

    public void initDefaults() {
        adminStateUp = true;
    }

    @Override
    public String toString() {
        return "NeutronRouter [" +
            "id=" + routerUUID +
            ", name=" + name +
            ", adminStateUp=" + adminStateUp +
            ", status=" + status +
            ", tenantID=" + tenantID +
            ", external_gateway_info=" + externalGatewayInfo +
            ", distributed=" + distributed +
            ", gw_port_id=" + gatewayPortId +
            ", routes=" + routes +
            ", interfaces=" + interfaces +
            "]";
    }

}
