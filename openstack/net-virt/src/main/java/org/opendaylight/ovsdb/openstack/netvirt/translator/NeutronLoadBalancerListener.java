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
 * OpenStack Neutron v2.0 Load Balancer as a service
 * (LBaaS) bindings. See OpenStack Network API
 * v2.0 Reference for description of  the fields:
 * Implemented fields are as follows:
 *
 * id                 uuid-str
 * default_pool_id    String
 * tenant_id          uuid-str
 * name               String
 * description        String
 * shared             Bool
 * protocol           String
 * protocol_port      String
 * load_balancer_id   String
 * admin_state_up     Boolean
 * status             String
 *
 * http://docs.openstack.org/api/openstack-network/2.0/openstack-network.pdf
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronLoadBalancerListener
    implements Serializable, INeutronObject {
    private static final long serialVersionUID = 1L;

    @XmlElement(name = "id")
    String loadBalancerListenerID;

    @XmlElement (name = "default_pool_id")
    String neutronLoadBalancerListenerDefaultPoolID;

    @XmlElement (name = "connection_limit")
    Integer neutronLoadBalancerListenerConnectionLimit;

    @XmlElement (name = "tenant_id")
    String loadBalancerListenerTenantID;

    @XmlElement (name = "name")
    String loadBalancerListenerName;

    @XmlElement (name = "description")
    String loadBalancerListenerDescription;

    @XmlElement (defaultValue = "true", name = "admin_state_up")
    Boolean loadBalancerListenerAdminStateIsUp;

    @XmlElement (name = "protocol")
    String neutronLoadBalancerListenerProtocol;

    @XmlElement (name = "protocol_port")
    String neutronLoadBalancerListenerProtocolPort;

    @XmlElement (name = "load_balancers")
    List<Neutron_ID> neutronLoadBalancerListenerLoadBalancerIDs;

    public String getID() {
        return loadBalancerListenerID;
    }

    public void setID(String id) {
        loadBalancerListenerID = id;
    }

    // @deprecated use getID()
    public String getLoadBalancerListenerID() {
        return loadBalancerListenerID;
    }

    // @deprecated use setID()
    public void setLoadBalancerListenerID(String loadBalancerListenerID) {
        this.loadBalancerListenerID = loadBalancerListenerID;
    }

    public String getLoadBalancerListenerTenantID() {
        return loadBalancerListenerTenantID;
    }

    public void setLoadBalancerListenerTenantID(String loadBalancerListenerTenantID) {
        this.loadBalancerListenerTenantID = loadBalancerListenerTenantID;
    }

    public String getLoadBalancerListenerName() {
        return loadBalancerListenerName;
    }

    public void setLoadBalancerListenerName(String loadBalancerListenerName) {
        this.loadBalancerListenerName = loadBalancerListenerName;
    }

    public String getLoadBalancerListenerDescription() {
        return loadBalancerListenerDescription;
    }

    public void setLoadBalancerListenerDescription(String loadBalancerListenerDescription) {
        this.loadBalancerListenerDescription = loadBalancerListenerDescription;
    }

    public Boolean getLoadBalancerListenerAdminStateIsUp() {
        return loadBalancerListenerAdminStateIsUp;
    }

    public void setLoadBalancerListenerAdminStateIsUp(Boolean loadBalancerListenerAdminStateIsUp) {
        this.loadBalancerListenerAdminStateIsUp = loadBalancerListenerAdminStateIsUp;
    }

    public String getNeutronLoadBalancerListenerProtocol() {
        return neutronLoadBalancerListenerProtocol;
    }

    public void setNeutronLoadBalancerListenerProtocol(String neutronLoadBalancerListenerProtocol) {
        this.neutronLoadBalancerListenerProtocol = neutronLoadBalancerListenerProtocol;
    }

    public String getNeutronLoadBalancerListenerProtocolPort() {
        return neutronLoadBalancerListenerProtocolPort;
    }

    public void setNeutronLoadBalancerListenerProtocolPort(String neutronLoadBalancerListenerProtocolPort) {
        this.neutronLoadBalancerListenerProtocolPort = neutronLoadBalancerListenerProtocolPort;
    }

    public String getNeutronLoadBalancerListenerDefaultPoolID() {
        return neutronLoadBalancerListenerDefaultPoolID;
    }

    public void setNeutronLoadBalancerListenerDefaultPoolID(String neutronLoadBalancerListenerDefaultPoolID) {
        this.neutronLoadBalancerListenerDefaultPoolID = neutronLoadBalancerListenerDefaultPoolID;
    }

    public Integer getNeutronLoadBalancerListenerConnectionLimit() {
        return neutronLoadBalancerListenerConnectionLimit;
    }

    public void setNeutronLoadBalancerListenerConnectionLimit(Integer neutronLoadBalancerListenerConnectionLimit) {
        this.neutronLoadBalancerListenerConnectionLimit = neutronLoadBalancerListenerConnectionLimit;
    }

    public List<Neutron_ID> getNeutronLoadBalancerListenerLoadBalancerIDs() {
        return neutronLoadBalancerListenerLoadBalancerIDs;
    }

    public void setNeutronLoadBalancerListenerLoadBalancerIDs(List<Neutron_ID> neutronLoadBalancerListenerLoadBalancerIDs) {
        this.neutronLoadBalancerListenerLoadBalancerIDs = neutronLoadBalancerListenerLoadBalancerIDs;
    }

    public NeutronLoadBalancerListener extractFields(List<String> fields) {
        NeutronLoadBalancerListener ans = new NeutronLoadBalancerListener();
        for (String s : fields) {
            switch (s) {
                case "id":
                    ans.setID(this.getID());
                    break;
                case "default_pool_id":
                    ans.setNeutronLoadBalancerListenerDefaultPoolID(this.getNeutronLoadBalancerListenerDefaultPoolID());
                    break;
                case "tenant_id":
                    ans.setLoadBalancerListenerTenantID(this.getLoadBalancerListenerTenantID());
                    break;
                case "name":
                    ans.setLoadBalancerListenerName(this.getLoadBalancerListenerName());
                    break;
                case "description":
                    ans.setLoadBalancerListenerDescription(this.getLoadBalancerListenerDescription());
                    break;
                case "protocol":
                    ans.setNeutronLoadBalancerListenerProtocol(this.getNeutronLoadBalancerListenerProtocol());
                    break;
                case "protocol_port":
                    ans.setNeutronLoadBalancerListenerProtocolPort(this.getNeutronLoadBalancerListenerProtocolPort());
                    break;
                case "admin_state_up":
                    ans.setLoadBalancerListenerAdminStateIsUp(loadBalancerListenerAdminStateIsUp);
                    break;
            }
        }
        return ans;
    }

    @Override public String toString() {
        return "NeutronLoadBalancerListener{" +
                "loadBalancerListenerID='" + loadBalancerListenerID + '\'' +
                ", neutronLoadBalancerListenerDefaultPoolID='" + neutronLoadBalancerListenerDefaultPoolID + '\'' +
                ", neutronLoadBalancerListenerConnectionLimit='" + neutronLoadBalancerListenerConnectionLimit + '\'' +
                ", loadBalancerListenerTenantID='" + loadBalancerListenerTenantID + '\'' +
                ", loadBalancerListenerName='" + loadBalancerListenerName + '\'' +
                ", loadBalancerListenerDescription='" + loadBalancerListenerDescription + '\'' +
                ", loadBalancerListenerAdminStateIsUp=" + loadBalancerListenerAdminStateIsUp +
                ", neutronLoadBalancerListenerProtocol='" + neutronLoadBalancerListenerProtocol + '\'' +
                ", neutronLoadBalancerListenerProtocolPort='" + neutronLoadBalancerListenerProtocolPort + '\'' +
                '}';
    }
}
