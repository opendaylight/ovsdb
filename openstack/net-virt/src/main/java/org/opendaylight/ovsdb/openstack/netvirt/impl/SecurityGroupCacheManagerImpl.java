/*
 * Copyright (c) 2014, 2015 HP, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityGroupCacheManger;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronPortCRUD;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Aswin Suryanarayanan.
 */

public class SecurityGroupCacheManagerImpl implements ConfigInterface, SecurityGroupCacheManger{

    private final Map<String, HashSet<String>> securityGroupCache  =
            new ConcurrentHashMap<String, HashSet<String>>();
    private static final Logger LOG = LoggerFactory.getLogger(NodeCacheManagerImpl.class);
    private volatile SecurityServicesManager securityServicesManager;
    private volatile INeutronPortCRUD neutronPortCache;

    @Override
    public void portAdded(String securityGroupUuid, String portUuid) {
        LOG.debug("In portAdded securityGroupUuid:" + securityGroupUuid + " portUuid:" + portUuid);
        NeutronPort port = neutronPortCache.getPort(portUuid);
        processPortAdded(securityGroupUuid,port);
    }

    @Override
    public void portRemoved(String securityGroupUuid, String portUuid) {
        LOG.debug("In portRemoved securityGroupUuid:" + securityGroupUuid + " portUuid:" + portUuid);
        NeutronPort port = neutronPortCache.getPort(portUuid);
        processPortRemoved(securityGroupUuid,port);
    }

    @Override
    public void addToCache(String remoteSgUuid, String portUuid) {
        LOG.debug("In addToCache remoteSgUuid:" + remoteSgUuid + "portUuid:" + portUuid);
        HashSet<String> portList = securityGroupCache.get(remoteSgUuid);
        if (null == portList) {
            portList = new HashSet<String>();
            securityGroupCache.put(remoteSgUuid, portList);
        }
        portList.add(portUuid);
    }

    @Override
    public void removeFromCache(String remoteSgUuid, String portUuid) {
        LOG.debug("In removeFromCache remoteSgUuid:" + remoteSgUuid + " portUuid:" + portUuid);
        HashSet<String> portList = securityGroupCache.get(remoteSgUuid);
        if (null == portList) {
            return;
        }
        for (Iterator<String> iterator = portList.iterator(); iterator.hasNext();) {
            String cachedPort = iterator.next();
            if (cachedPort.equals(portUuid)) {
                iterator.remove();
                break;
            }
        }
        if (portList.isEmpty()) {
            securityGroupCache.remove(remoteSgUuid);
        }
    }

    private void processPortAdded(String securityGroupUuid, NeutronPort port) {
        /*
         * Itreate through the cache maintained for the security group added. For each port in the cache
         * add the rule to allow traffic to/from the new port added.
         */
        LOG.debug("In processPortAdded securityGroupUuid:" + securityGroupUuid + " NeutronPort:" + port);
        HashSet<String> portList = this.securityGroupCache.get(securityGroupUuid);
        if (null == portList) {
            return;
        }
        for (String cachedportUuid : portList) {
            if (cachedportUuid.equals(port.getID())) {
                continue;
            }
            NeutronPort cachedport = neutronPortCache.getPort(cachedportUuid);
            if (null == cachedport) {
                return;
            }
            List<NeutronSecurityRule> remoteSecurityRules = retrieveSecurityRules(securityGroupUuid, cachedportUuid);
            for (NeutronSecurityRule securityRule:remoteSecurityRules) {
                for (Neutron_IPs vmIp : port.getFixedIPs()) {
                    securityServicesManager.syncSecurityRule(cachedport, securityRule, vmIp, true);
                }
            }
        }
    }

    private void processPortRemoved(String securityGroupUuid, NeutronPort port) {
        /*
         * Itreate through the cache maintained for the security group added. For each port in the cache remove
         * the rule to allow traffic to/from the  port that got deleted.
         */
        LOG.debug("In processPortRemoved securityGroupUuid:" + securityGroupUuid + " port:" + port);
        HashSet<String> portList = this.securityGroupCache.get(securityGroupUuid);
        if (null == portList) {
            return;
        }
        for (String cachedportUuid : portList) {
            if (cachedportUuid.equals(port.getID())) {
                continue;
            }
            NeutronPort cachedport = neutronPortCache.getPort(cachedportUuid);
            if (null == cachedport) {
                return;
            }
            List<NeutronSecurityRule> remoteSecurityRules = retrieveSecurityRules(securityGroupUuid, cachedportUuid);
            for (NeutronSecurityRule securityRule:remoteSecurityRules) {
                for (Neutron_IPs vmIp : port.getFixedIPs()) {
                    securityServicesManager.syncSecurityRule(cachedport, securityRule, vmIp, false);
                }
            }
        }
    }

    private List<NeutronSecurityRule> retrieveSecurityRules(String securityGroupUuid, String portUuid) {
        /*
         * Get the list of security rules in the port with portUuid that has securityGroupUuid as a remote
         * security group.
         */
        LOG.debug("In retrieveSecurityRules securityGroupUuid:" + securityGroupUuid + " portUuid:" + portUuid);
        NeutronPort port = neutronPortCache.getPort(portUuid);
        if (null == port) {
            return null;
        }
        List<NeutronSecurityRule> remoteSecurityRules = new ArrayList<NeutronSecurityRule>();
        List<NeutronSecurityGroup> securityGroups = port.getSecurityGroups();
        for (NeutronSecurityGroup securtiyGroup : securityGroups) {
            List<NeutronSecurityRule> securityRules = securtiyGroup.getSecurityRules();
            for (NeutronSecurityRule securityRule : securityRules) {
                if (securityGroupUuid.equals(securityRule.getSecurityRemoteGroupID())) {
                    remoteSecurityRules.add(securityRule);
                }
            }
        }
        return remoteSecurityRules;
    }

    private void init() {
        /*
         * Rebuild the cache in case of a restart.
         */
        List<NeutronPort> portList = neutronPortCache.getAllPorts();
        for (NeutronPort port:portList) {
            List<NeutronSecurityGroup> securityGroupList = port.getSecurityGroups();
            if ( null != securityGroupList) {
                for (NeutronSecurityGroup securityGroup : securityGroupList) {
                    List<NeutronSecurityRule> securityRuleList = securityGroup.getSecurityRules();
                    if ( null != securityRuleList) {
                        for (NeutronSecurityRule securityRule : securityRuleList) {
                            if (null != securityRule.getSecurityRemoteGroupID()) {
                                this.addToCache(securityRule.getSecurityRemoteGroupID(), port.getID());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setDependencies(ServiceReference serviceReference) {
        securityServicesManager =
                (SecurityServicesManager) ServiceHelper.getGlobalInstance(SecurityServicesManager.class, this);
        neutronPortCache = (INeutronPortCRUD) ServiceHelper.getGlobalInstance(INeutronPortCRUD.class, this);
        init();
    }

    @Override
    public void setDependencies(Object impl) {
    }
}
