/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronCRUDInterfaces {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(NeutronCRUDInterfaces.class);

    private INeutronNetworkCRUD networkInterface;
    private INeutronSubnetCRUD subnetInterface;
    private INeutronPortCRUD portInterface;
    private INeutronRouterCRUD routerInterface;
    private INeutronFloatingIPCRUD fipInterface;
    private INeutronSecurityGroupCRUD sgInterface;
    private INeutronSecurityRuleCRUD srInterface;
    private INeutronFirewallCRUD fwInterface;
    private INeutronFirewallPolicyCRUD fwpInterface;
    private INeutronFirewallRuleCRUD fwrInterface;
    private INeutronLoadBalancerCRUD lbInterface;
    private INeutronLoadBalancerPoolCRUD lbpInterface;
    private INeutronLoadBalancerListenerCRUD lblInterface;
    private INeutronLoadBalancerHealthMonitorCRUD lbhmInterface;
    private INeutronLoadBalancerPoolMemberCRUD lbpmInterface;
    public NeutronCRUDInterfaces() {
    }

    public INeutronNetworkCRUD getNetworkInterface() {
        return networkInterface;
    }

    public INeutronSubnetCRUD getSubnetInterface() {
        return subnetInterface;
    }

    public INeutronPortCRUD getPortInterface() {
        return portInterface;
    }

    public INeutronRouterCRUD getRouterInterface() {
        return routerInterface;
    }

    public INeutronFloatingIPCRUD getFloatingIPInterface() {
        return fipInterface;
    }

    public INeutronSecurityGroupCRUD getSecurityGroupInterface() {
        return sgInterface;
    }

    public INeutronSecurityRuleCRUD getSecurityRuleInterface() {
        return srInterface;
    }

    public INeutronFirewallCRUD getFirewallInterface() {
        return fwInterface;
    }

    public INeutronFirewallPolicyCRUD getFirewallPolicyInterface() {
        return fwpInterface;
    }

    public INeutronFirewallRuleCRUD getFirewallRuleInterface() {
        return fwrInterface;
    }

    public INeutronLoadBalancerCRUD getLoadBalancerInterface() {
        return lbInterface;
    }

    public INeutronLoadBalancerPoolCRUD getLoadBalancerPoolInterface() {
        return lbpInterface;
    }

    public INeutronLoadBalancerListenerCRUD getLoadBalancerListenerInterface() {
        return lblInterface;
    }

    public INeutronLoadBalancerHealthMonitorCRUD getLoadBalancerHealthMonitorInterface() {
        return lbhmInterface;
    }

    public INeutronLoadBalancerPoolMemberCRUD getLoadBalancerPoolMemberInterface() {
        return lbpmInterface;
    }

    public NeutronCRUDInterfaces fetchINeutronNetworkCRUD(Object obj) {
        networkInterface = (INeutronNetworkCRUD) getInstances(INeutronNetworkCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronSubnetCRUD(Object obj) {
        subnetInterface = (INeutronSubnetCRUD) getInstances(INeutronSubnetCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronPortCRUD(Object obj) {
        portInterface = (INeutronPortCRUD) getInstances(INeutronPortCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronRouterCRUD(Object obj) {
        routerInterface = (INeutronRouterCRUD) getInstances(INeutronRouterCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronFloatingIPCRUD(Object obj) {
        fipInterface = (INeutronFloatingIPCRUD) getInstances(INeutronFloatingIPCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronSecurityGroupCRUD(Object obj) {
        sgInterface = (INeutronSecurityGroupCRUD) getInstances(INeutronSecurityGroupCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronSecurityRuleCRUD(Object obj) {
        srInterface = (INeutronSecurityRuleCRUD) getInstances(INeutronSecurityRuleCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronFirewallCRUD(Object obj) {
        fwInterface = (INeutronFirewallCRUD) getInstances(INeutronFirewallCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronFirewallPolicyCRUD(Object obj) {
        fwpInterface = (INeutronFirewallPolicyCRUD) getInstances(INeutronFirewallPolicyCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronFirewallRuleCRUD(Object obj) {
        fwrInterface = (INeutronFirewallRuleCRUD) getInstances(INeutronFirewallRuleCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronLoadBalancerCRUD(Object obj) {
        lbInterface = (INeutronLoadBalancerCRUD) getInstances(INeutronLoadBalancerCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronLoadBalancerPoolCRUD(Object obj) {
        lbpInterface = (INeutronLoadBalancerPoolCRUD) getInstances(INeutronLoadBalancerPoolCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronLoadBalancerListenerCRUD(Object obj) {
        lblInterface = (INeutronLoadBalancerListenerCRUD) getInstances(INeutronLoadBalancerListenerCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronLoadBalancerHealthMonitorCRUD(Object obj) {
        lbhmInterface = (INeutronLoadBalancerHealthMonitorCRUD) getInstances(INeutronLoadBalancerHealthMonitorCRUD.class, obj);
        return this;
    }

    public NeutronCRUDInterfaces fetchINeutronLoadBalancerPoolMemberCRUD(Object obj) {
        lbpmInterface = (INeutronLoadBalancerPoolMemberCRUD) getInstances(INeutronLoadBalancerPoolMemberCRUD.class, obj);
        return this;
    }

    public Object getInstances(Class<?> clazz, Object bundle) {
        try {
            BundleContext bCtx = FrameworkUtil.getBundle(bundle.getClass()).getBundleContext();

            ServiceReference<?>[] services = null;
            services = bCtx.getServiceReferences(clazz.getName(), null);
            if (services != null) {
                return bCtx.getService(services[0]);
            }
        } catch (Exception e) {
            LOGGER.error("Error in getInstances", e);
        }
        return null;
    }
}
