/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class ProviderNetworkManagerImpl implements ConfigInterface, NetworkingProviderManager {
    private static final Logger LOG = LoggerFactory.getLogger(ProviderNetworkManagerImpl.class);
    private HashMap<Long, ProviderEntry> providers = Maps.newHashMap();
    private HashMap<Node, NetworkingProvider> nodeToProviderMapping = Maps.newHashMap();
    private volatile OvsdbInventoryService ovsdbInventoryService;

    @Override
    public NetworkingProvider getProvider(Node node) {
        if (nodeToProviderMapping.get(node) != null) {
            return nodeToProviderMapping.get(node);
        }

        final String targetVersion = Constants.OPENFLOW13;
        Predicate<ProviderEntry> providerEntryPredicate = new Predicate<ProviderEntry>() {
            @Override
            public boolean apply(ProviderEntry providerEntry) {
                //ToDo: This should match on southboundProtocol and providerType too
                return providerEntry.getProperties().get(Constants.OPENFLOW_VERSION_PROPERTY).equals(targetVersion);
            }
        };

        Iterable<ProviderEntry> matchingProviders = Iterables.filter(providers.values(), providerEntryPredicate);
        if (!matchingProviders.iterator().hasNext()) {
            LOG.error("No providers matching {} found", targetVersion);
        }

        // Return the first match as only have one matching provider today
        // ToDo: Tie-breaking logic
        NetworkingProvider provider = matchingProviders.iterator().next().getProvider();
        nodeToProviderMapping.put(node, provider);
        return provider;
    }

    public void providerAdded(final ServiceReference ref, final NetworkingProvider provider){
        Map <String, String> properties = Maps.newHashMap();
        Long pid = (Long) ref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
        properties.put(Constants.SOUTHBOUND_PROTOCOL_PROPERTY,
                (String) ref.getProperty(Constants.SOUTHBOUND_PROTOCOL_PROPERTY));
        properties.put(Constants.OPENFLOW_VERSION_PROPERTY,
                (String) ref.getProperty(Constants.OPENFLOW_VERSION_PROPERTY));
        properties.put(Constants.PROVIDER_TYPE_PROPERTY, (String) ref.getProperty(Constants.PROVIDER_TYPE_PROPERTY));
        providers.put(pid, new ProviderEntry(provider, properties));
        LOG.info("Neutron Networking Provider Registered: {}, with {} and pid={}",
                provider.getClass().getName(), properties.toString(), pid);

        ovsdbInventoryService.providersReady();
    }

    public void providerRemoved(final ServiceReference ref){
        Long pid = (Long)ref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
        providers.remove(pid);
        LOG.info("Neutron Networking Provider Removed: {}", pid);
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        ovsdbInventoryService =
                (OvsdbInventoryService) ServiceHelper.getGlobalInstance(OvsdbInventoryService.class, this);
    }

    @Override
    public void setDependencies(Object impl) {

    }

    private class ProviderEntry {
        NetworkingProvider provider;
        Map<String, String> properties;

        ProviderEntry(NetworkingProvider provider, Map<String, String> properties) {
            this.provider = provider;
            this.properties = properties;
        }

        public NetworkingProvider getProvider() {
            return provider;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }

}
