/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "network")
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronNetwork implements Serializable, INeutronObject {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    private static final long serialVersionUID = 1L;

    @XmlElement (name = "id")
    String networkUUID;

    @XmlElement (name = "name")
    String networkName;

    @XmlElement (defaultValue = "true", name = "admin_state_up")
    Boolean adminStateUp;

    @XmlElement (defaultValue = "false", name = "shared")
    Boolean shared;

    @XmlElement (name = "tenant_id")
    String tenantID;

    //    @XmlElement (defaultValue = "false", name = "router:external")
    @XmlElement (defaultValue="false", namespace="router", name="external")
    Boolean routerExternal;

    //    @XmlElement (defaultValue = "flat", name = "provider:network_type")
    @XmlElement (namespace="provider", name="network_type")
    String providerNetworkType;

    //    @XmlElement (name = "provider:physical_network")
    @XmlElement (namespace="provider", name="physical_network")
    String providerPhysicalNetwork;

    //    @XmlElement (name = "provider:segmentation_id")
    @XmlElement (namespace="provider", name="segmentation_id")
    String providerSegmentationID;

    @XmlElement (name = "status")
    String status;

    @XmlElement (name="segments")
    List<NeutronNetwork_Segment> segments;

    @XmlElement (name="vlan_transparent")
    Boolean vlanTransparent;

    @XmlElement (name="mtu")
    Integer mtu;

    /* This attribute lists the ports associated with an instance
     * which is needed for determining if that instance can be deleted
     */

    public NeutronNetwork() {
    }

    public void initDefaults() {
        if (status == null) {
            status = "ACTIVE";
        }
        if (adminStateUp == null) {
            adminStateUp = true;
        }
        if (shared == null) {
            shared = false;
        }
        if (routerExternal == null) {
            routerExternal = false;
        }
        if (providerNetworkType == null) {
            providerNetworkType = "flat";
        }
    }

    public String getID() { return networkUUID; }

    public void setID(String id) { this.networkUUID = id; }

    public String getNetworkUUID() {
        return networkUUID;
    }

    public void setNetworkUUID(String networkUUID) {
        this.networkUUID = networkUUID;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public boolean isAdminStateUp() {
        return adminStateUp;
    }

    public Boolean getAdminStateUp() { return adminStateUp; }

    public void setAdminStateUp(boolean newValue) {
        adminStateUp = newValue;
    }

    public boolean isShared() { return shared; }

    public Boolean getShared() { return shared; }

    public void setShared(boolean newValue) {
        shared = newValue;
    }

    public String getTenantID() {
        return tenantID;
    }

    public void setTenantID(String tenantID) {
        this.tenantID = tenantID;
    }

    public boolean isRouterExternal() { return routerExternal; }

    public Boolean getRouterExternal() { return routerExternal; }

    public void setRouterExternal(boolean newValue) {
        routerExternal = newValue;
    }

    public String getProviderNetworkType() {
        return providerNetworkType;
    }

    public void setProviderNetworkType(String providerNetworkType) {
        this.providerNetworkType = providerNetworkType;
    }

    public String getProviderPhysicalNetwork() {
        return providerPhysicalNetwork;
    }

    public void setProviderPhysicalNetwork(String providerPhysicalNetwork) {
        this.providerPhysicalNetwork = providerPhysicalNetwork;
    }

    public String getProviderSegmentationID() {
        return providerSegmentationID;
    }

    public void setProviderSegmentationID(String providerSegmentationID) {
        this.providerSegmentationID = providerSegmentationID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSegments(List<NeutronNetwork_Segment> segments) {
        this.segments = segments;
    }

    public List<NeutronNetwork_Segment> getSegments() {
        return segments;
    }

    public Boolean getVlanTransparent() {
        return vlanTransparent;
    }

    public void setVlanTransparent(Boolean input) {
        this.vlanTransparent = input;
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer input) {
        mtu = input;
    }

    /**
     * This method copies selected fields from the object and returns them
     * as a new object, suitable for marshaling.
     *
     * @param fields
     *            List of attributes to be extracted
     * @return an OpenStackNetworks object with only the selected fields
     * populated
     */

    public NeutronNetwork extractFields(List<String> fields) {
        NeutronNetwork ans = new NeutronNetwork();
        for (String s : fields) {
            switch (s) {
                case "id":
                    ans.setNetworkUUID(this.getNetworkUUID());
                    break;
                case "name":
                    ans.setNetworkName(this.getNetworkName());
                    break;
                case "admin_state_up":
                    ans.setAdminStateUp(adminStateUp);
                    break;
                case "status":
                    ans.setStatus(this.getStatus());
                    break;
                case "shared":
                    ans.setShared(shared);
                    break;
                case "tenant_id":
                    ans.setTenantID(this.getTenantID());
                    break;
                case "external":
                    ans.setRouterExternal(this.getRouterExternal());
                    break;
                case "segmentation_id":
                    ans.setProviderSegmentationID(this.getProviderSegmentationID());
                    break;
                case "physical_network":
                    ans.setProviderPhysicalNetwork(this.getProviderPhysicalNetwork());
                    break;
                case "network_type":
                    ans.setProviderNetworkType(this.getProviderNetworkType());
                    break;
            }
        }
        return ans;
    }

    @Override
    public String toString() {
        return "NeutronNetwork [networkUUID=" + networkUUID + ", networkName=" + networkName + ", adminStateUp="
                + adminStateUp + ", shared=" + shared + ", tenantID=" + tenantID + ", routerExternal=" + routerExternal
                + ", providerNetworkType=" + providerNetworkType + ", providerPhysicalNetwork="
                + providerPhysicalNetwork + ", providerSegmentationID=" + providerSegmentationID + ", status=" + status
                + ", segments = " + segments + "]";
    }
}

