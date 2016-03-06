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
 *
 * id                 uuid-str
 * tenant_id          uuid-str
 * type               String
 * delay              Integer
 * timeout            Integer
 * max_retries        Integer
 * http_method        String
 * url_path           String
 * expected_codes     String
 * admin_state_up     Boolean
 * status             String
 * http://docs.openstack.org/api/openstack-network/2.0/openstack-network.pdf
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronLoadBalancerHealthMonitor
    implements Serializable, INeutronObject {
    private static final long serialVersionUID = 1L;

    @XmlElement(name = "id")
    String loadBalancerHealthMonitorID;

    @XmlElement (name = "tenant_id")
    String loadBalancerHealthMonitorTenantID;

    @XmlElement (name = "type")
    String loadBalancerHealthMonitorType;

    @XmlElement (name = "delay")
    Integer loadBalancerHealthMonitorDelay;

    @XmlElement (name = "timeout")
    Integer loadBalancerHealthMonitorTimeout;

    @XmlElement (name = "max_retries")
    Integer loadBalancerHealthMonitorMaxRetries;

    @XmlElement (name = "http_method")
    String loadBalancerHealthMonitorHttpMethod;

    @XmlElement (name = "url_path")
    String loadBalancerHealthMonitorUrlPath;

    @XmlElement (name = "expected_codes")
    String loadBalancerHealthMonitorExpectedCodes;

    @XmlElement (defaultValue = "true", name = "admin_state_up")
    Boolean loadBalancerHealthMonitorAdminStateIsUp;

    @XmlElement (name = "pools")
    List<Neutron_ID> loadBalancerHealthMonitorPools;

    public String getID() {
        return loadBalancerHealthMonitorID;
    }

    public void setID(String id) {
        loadBalancerHealthMonitorID = id;
    }

    // @deprecated use getID()
    public String getLoadBalancerHealthMonitorID() {
        return loadBalancerHealthMonitorID;
    }

    // @deprecated use setID()
    public void setLoadBalancerHealthMonitorID(String loadBalancerHealthMonitorID) {
        this.loadBalancerHealthMonitorID = loadBalancerHealthMonitorID;
    }

    public String getLoadBalancerHealthMonitorTenantID() {
        return loadBalancerHealthMonitorTenantID;
    }

    public void setLoadBalancerHealthMonitorTenantID(String loadBalancerHealthMonitorTenantID) {
        this.loadBalancerHealthMonitorTenantID = loadBalancerHealthMonitorTenantID;
    }

    public String getLoadBalancerHealthMonitorType() {
        return loadBalancerHealthMonitorType;
    }

    public void setLoadBalancerHealthMonitorType(String loadBalancerHealthMonitorType) {
        this.loadBalancerHealthMonitorType = loadBalancerHealthMonitorType;
    }

    public Integer getLoadBalancerHealthMonitorDelay() {
        return loadBalancerHealthMonitorDelay;
    }

    public void setLoadBalancerHealthMonitorDelay(Integer loadBalancerHealthMonitorDelay) {
        this.loadBalancerHealthMonitorDelay = loadBalancerHealthMonitorDelay;
    }

    public Integer getLoadBalancerHealthMonitorTimeout() {
        return loadBalancerHealthMonitorTimeout;
    }

    public void setLoadBalancerHealthMonitorTimeout(Integer loadBalancerHealthMonitorTimeout) {
        this.loadBalancerHealthMonitorTimeout = loadBalancerHealthMonitorTimeout;
    }

    public Integer getLoadBalancerHealthMonitorMaxRetries() {
        return loadBalancerHealthMonitorMaxRetries;
    }

    public void setLoadBalancerHealthMonitorMaxRetries(Integer loadBalancerHealthMonitorMaxRetries) {
        this.loadBalancerHealthMonitorMaxRetries = loadBalancerHealthMonitorMaxRetries;
    }

    public String getLoadBalancerHealthMonitorHttpMethod() {
        return loadBalancerHealthMonitorHttpMethod;
    }

    public void setLoadBalancerHealthMonitorHttpMethod(String loadBalancerHealthMonitorHttpMethod) {
        this.loadBalancerHealthMonitorHttpMethod = loadBalancerHealthMonitorHttpMethod;
    }

    public String getLoadBalancerHealthMonitorUrlPath() {
        return loadBalancerHealthMonitorUrlPath;
    }

    public void setLoadBalancerHealthMonitorUrlPath(String loadBalancerHealthMonitorUrlPath) {
        this.loadBalancerHealthMonitorUrlPath = loadBalancerHealthMonitorUrlPath;
    }

    public String getLoadBalancerHealthMonitorExpectedCodes() {
        return loadBalancerHealthMonitorExpectedCodes;
    }

    public void setLoadBalancerHealthMonitorExpectedCodes(String loadBalancerHealthMonitorExpectedCodes) {
        this.loadBalancerHealthMonitorExpectedCodes = loadBalancerHealthMonitorExpectedCodes;
    }

    public Boolean getLoadBalancerHealthMonitorAdminStateIsUp() {
        return loadBalancerHealthMonitorAdminStateIsUp;
    }

    public void setLoadBalancerHealthMonitorAdminStateIsUp(Boolean loadBalancerHealthMonitorAdminStateIsUp) {
        this.loadBalancerHealthMonitorAdminStateIsUp = loadBalancerHealthMonitorAdminStateIsUp;
    }

    public List<Neutron_ID> getLoadBalancerHealthMonitorPools() {
        return loadBalancerHealthMonitorPools;
    }

    public void setLoadBalancerHealthMonitorPools(List<Neutron_ID> loadBalancerHealthMonitorPools) {
        this.loadBalancerHealthMonitorPools = loadBalancerHealthMonitorPools;
    }

    public NeutronLoadBalancerHealthMonitor extractFields(List<String> fields) {
        NeutronLoadBalancerHealthMonitor ans = new NeutronLoadBalancerHealthMonitor();
        for (String s : fields) {
            switch (s) {
                case "id":
                    ans.setID(this.getID());
                    break;
                case "tenant_id":
                    ans.setLoadBalancerHealthMonitorTenantID(this.getLoadBalancerHealthMonitorTenantID());
                    break;
                case "type":
                    ans.setLoadBalancerHealthMonitorType(this.getLoadBalancerHealthMonitorType());
                    break;
                case "delay":
                    ans.setLoadBalancerHealthMonitorDelay(this.getLoadBalancerHealthMonitorDelay());
                    break;
                case "timeout":
                    ans.setLoadBalancerHealthMonitorTimeout(this.getLoadBalancerHealthMonitorTimeout());
                    break;
                case "max_retries":
                    ans.setLoadBalancerHealthMonitorMaxRetries(this.getLoadBalancerHealthMonitorMaxRetries());
                    break;
                case "http_method":
                    ans.setLoadBalancerHealthMonitorHttpMethod(this.getLoadBalancerHealthMonitorHttpMethod());
                    break;
                case "url_path":
                    ans.setLoadBalancerHealthMonitorUrlPath(this.getLoadBalancerHealthMonitorUrlPath());
                    break;
                case "expected_codes":
                    ans.setLoadBalancerHealthMonitorExpectedCodes(this.getLoadBalancerHealthMonitorExpectedCodes());
                    break;
                case "admin_state_up":
                    ans.setLoadBalancerHealthMonitorAdminStateIsUp(loadBalancerHealthMonitorAdminStateIsUp);
                    break;
            }
        }
        return ans;
    }

    @Override public String toString() {
        return "NeutronLoadBalancerHealthMonitor{" +
                "loadBalancerHealthMonitorID='" + loadBalancerHealthMonitorID + '\'' +
                ", loadBalancerHealthMonitorTenantID='" + loadBalancerHealthMonitorTenantID + '\'' +
                ", loadBalancerHealthMonitorType='" + loadBalancerHealthMonitorType + '\'' +
                ", loadBalancerHealthMonitorDelay=" + loadBalancerHealthMonitorDelay +
                ", loadBalancerHealthMonitorTimeout=" + loadBalancerHealthMonitorTimeout +
                ", loadBalancerHealthMonitorMaxRetries=" + loadBalancerHealthMonitorMaxRetries +
                ", loadBalancerHealthMonitorHttpMethod='" + loadBalancerHealthMonitorHttpMethod + '\'' +
                ", loadBalancerHealthMonitorUrlPath='" + loadBalancerHealthMonitorUrlPath + '\'' +
                ", loadBalancerHealthMonitorExpectedCodes='" + loadBalancerHealthMonitorExpectedCodes + '\'' +
                ", loadBalancerHealthMonitorAdminStateIsUp=" + loadBalancerHealthMonitorAdminStateIsUp +
                '}';
    }
}
