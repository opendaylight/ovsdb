/*
 * Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * OpenStack Neutron v2.0 Firewall as a service
 * (FWaaS) bindings. See OpenStack Network API
 * v2.0 Reference for description of  the fields:
 * Implemented fields are as follows:
 *
 * id                 uuid-str
 * tenant_id          uuid-str
 * name               String
 * description        String
 * admin_state_up     Bool
 * status             String
 * shared             Bool
 * firewall_policy_id uuid-str
 * http://docs.openstack.org/api/openstack-network/2.0/openstack-network.pdf
 *
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronFirewall implements Serializable, INeutronObject {
    private static final long serialVersionUID = 1L;

    @XmlElement(name = "id")
    String firewallUUID;

    @XmlElement (name = "tenant_id")
    String firewallTenantID;

    @XmlElement (name = "name")
    String firewallName;

    @XmlElement (name = "description")
    String firewallDescription;

    @XmlElement (defaultValue = "true", name = "admin_state_up")
    Boolean firewallAdminStateIsUp;

    @XmlElement (name = "status")
    String firewallStatus;

    @XmlElement (defaultValue = "false", name = "shared")
    Boolean firewallIsShared;

    @XmlElement (name = "firewall_policy_id")
    String neutronFirewallPolicyID;

    public String getID() {
        return firewallUUID;
    }

    public void setID(String id) {
        firewallUUID = id;
    }

    // @deprecated use getID()
    public String getFirewallUUID() {
        return firewallUUID;
    }

    // @deprecated use setID()
    public void setFirewallUUID(String firewallUUID) {
        this.firewallUUID = firewallUUID;
    }

    public String getFirewallTenantID() {
        return firewallTenantID;
    }

    public void setFirewallTenantID(String firewallTenantID) {
        this.firewallTenantID = firewallTenantID;
    }

    public String getFirewallName() {
        return firewallName;
    }

    public void setFirewallName(String firewallName) {
        this.firewallName = firewallName;
    }

    public String getFirewallDescription() {
        return firewallDescription;
    }

    public void setFirewallDescription(String firewallDescription) {
        this.firewallDescription = firewallDescription;
    }

    public Boolean getFirewallAdminStateIsUp() {
        return firewallAdminStateIsUp;
    }

    public void setFirewallAdminStateIsUp(Boolean firewallAdminStateIsUp) {
        this.firewallAdminStateIsUp = firewallAdminStateIsUp;
    }

    public String getFirewallStatus() {
        return firewallStatus;
    }

    public void setFirewallStatus(String firewallStatus) {
        this.firewallStatus = firewallStatus;
    }

    public Boolean getFirewallIsShared() {
        return firewallIsShared;
    }

    public void setFirewallIsShared(Boolean firewallIsShared) {
        this.firewallIsShared = firewallIsShared;
    }

    public String getFirewallPolicyID() {
        return neutronFirewallPolicyID;
    }

    public void setNeutronFirewallPolicyID(String firewallPolicy) {
        this.neutronFirewallPolicyID = firewallPolicy;
    }

    public NeutronFirewall extractFields(List<String> fields) {
        NeutronFirewall ans = new NeutronFirewall();
        for (String s : fields) {
            switch (s) {
                case "id":
                    ans.setID(this.getID());
                    break;
                case "tenant_id":
                    ans.setFirewallTenantID(this.getFirewallTenantID());
                    break;
                case "name":
                    ans.setFirewallName(this.getFirewallName());
                    break;
                case "description":
                    ans.setFirewallDescription(this.getFirewallDescription());
                    break;
                case "admin_state_up":
                    ans.setFirewallAdminStateIsUp(firewallAdminStateIsUp);
                    break;
                case "status":
                    ans.setFirewallStatus(this.getFirewallStatus());
                    break;
                case "shared":
                    ans.setFirewallIsShared(firewallIsShared);
                    break;
                case "firewall_policy_id":
                    ans.setNeutronFirewallPolicyID(this.getFirewallPolicyID());
                    break;
            }
        }
        return ans;
    }

    @Override
    public String toString() {
        return "NeutronFirewall{" +
            "firewallUUID='" + firewallUUID + '\'' +
            ", firewallTenantID='" + firewallTenantID + '\'' +
            ", firewallName='" + firewallName + '\'' +
            ", firewallDescription='" + firewallDescription + '\'' +
            ", firewallAdminStateIsUp=" + firewallAdminStateIsUp +
            ", firewallStatus='" + firewallStatus + '\'' +
            ", firewallIsShared=" + firewallIsShared +
            ", firewallRulePolicyID=" + neutronFirewallPolicyID +
            '}';
    }
}
