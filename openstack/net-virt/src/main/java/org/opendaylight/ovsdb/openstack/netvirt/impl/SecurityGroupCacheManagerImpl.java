/*
 * Copyright (c) 2015 Hewlett-Packard Enterprise and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityGroupCacheManger;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Aswin Suryanarayanan.
 */

public class SecurityGroupCacheManagerImpl implements ConfigInterface, SecurityGroupCacheManger{

    private final Map<String, Set<String>> securityGroupCache = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(SecurityGroupCacheManagerImpl.class);
    private volatile SecurityServicesManager securityServicesManager;
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile NeutronL3Adapter neutronL3Adapter;

    @Override
    public void portAdded(String securityGroupUuid, String portUuid) {
        LOG.debug("In portAdded securityGroupUuid:" + securityGroupUuid + " portUuid:" + portUuid);
        NeutronPort port = neutronPortCache.getPort(portUuid);
        if (port == null) {
            port = neutronL3Adapter.getPortFromCleanupCache(portUuid);
            if (port == null) {
                LOG.error("In portAdded no neutron port found:" + " portUuid:" + portUuid);
                return;
            }
        }
        processPortAdded(securityGroupUuid,port);
    }

    @Override
    public void portRemoved(String securityGroupUuid, String portUuid) {
        LOG.debug("In portRemoved securityGroupUuid:" + securityGroupUuid + " portUuid:" + portUuid);
        NeutronPort port = neutronPortCache.getPort(portUuid);

        if (port == null) {
            port = neutronL3Adapter.getPortFromCleanupCache(portUuid);
            if (port == null) {
                LOG.error("In portRemoved no neutron port found:" + " portUuid:" + portUuid);
                return;
            }
        }
        processPortRemoved(securityGroupUuid,port);
    }

    @Override
    public void addToCache(String remoteSgUuid, String portUuid) {
        LOG.debug("In addToCache remoteSgUuid:" + remoteSgUuid + "portUuid:" + portUuid);
        Set<String> portList = securityGroupCache.get(remoteSgUuid);
        if (null == portList) {
            portList = new HashSet<>();
            securityGroupCache.put(remoteSgUuid, portList);
        }
        portList.add(portUuid);
    }

    @Override
    public void removeFromCache(String remoteSgUuid, String portUuid) {
        LOG.debug("In removeFromCache remoteSgUuid:" + remoteSgUuid + " portUuid:" + portUuid);
        Set<String> portList = securityGroupCache.get(remoteSgUuid);
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
        Set<String> portList = this.securityGroupCache.get(securityGroupUuid);
        if (null == portList) {
            return;
        }
        for (String cachedportUuid : portList) {
            if (cachedportUuid.equals(port.getID())) {
                continue;
            }
            NeutronPort cachedport = neutronPortCache.getPort(cachedportUuid);
            if (null == cachedport) {
                LOG.error("In processPortAdded cachedport port not found in neuton cache:"
                            + " cachedportUuid:" + cachedportUuid);
                return;
            }
            List<NeutronSecurityRule> remoteSecurityRules = retrieveSecurityRules(securityGroupUuid, cachedportUuid);
            for (NeutronSecurityRule securityRule : remoteSecurityRules) {
                if (port.getFixedIPs() == null) {
                    continue;
                }
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
        Set<String> portList = this.securityGroupCache.get(securityGroupUuid);
        if (null == portList) {
            return;
        }
        for (String cachedportUuid : portList) {
            if (cachedportUuid.equals(port.getID())) {
                continue;
            }
            NeutronPort cachedport = neutronPortCache.getPort(cachedportUuid);
            if (cachedport == null) {
                cachedport = neutronL3Adapter.getPortFromCleanupCache(cachedportUuid);
                if (null == cachedport) {
                    LOG.error("In processPortRemoved cachedport port not found in neuton cache:"
                                + " cachedportUuid:" + cachedportUuid);
                    return;
                }
            }
            List<NeutronSecurityRule> remoteSecurityRules = retrieveSecurityRules(securityGroupUuid, cachedportUuid);
            for (NeutronSecurityRule securityRule : remoteSecurityRules) {
                if (port.getFixedIPs() == null) {
                    continue;
                }
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
        if (port == null) {
            port = neutronL3Adapter.getPortFromCleanupCache(portUuid);
            if (null == port) {
                LOG.error("In retrieveSecurityRules no neutron port found:" + " portUuid:" + portUuid);
                return null;
            }
        }
        List<NeutronSecurityRule> remoteSecurityRules = new ArrayList<>();
        List<NeutronSecurityGroup> securityGroups = port.getSecurityGroups();
        for (NeutronSecurityGroup securityGroup : securityGroups) {
            List<NeutronSecurityRule> securityRules = securityGroup.getSecurityRules();
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
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        neutronL3Adapter =
                (NeutronL3Adapter) ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, this);
        securityServicesManager =
                (SecurityServicesManager) ServiceHelper.getGlobalInstance(SecurityServicesManager.class, this);
        neutronPortCache = (INeutronPortCRUD) ServiceHelper.getGlobalInstance(INeutronPortCRUD.class, this);
        init();
    }

    @Override
    public void setDependencies(Object impl) {
    }
}