/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */
package org.opendaylight.ovsdb.neutron.impl;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.neutron.api.ConfigurationService;
import org.opendaylight.ovsdb.neutron.api.NetworkingProvider;
import org.opendaylight.ovsdb.neutron.api.NetworkingProviderManager;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ProviderNetworkManagerImpl implements NetworkingProviderManager {

    static final Logger logger = LoggerFactory.getLogger(ProviderNetworkManagerImpl.class);
    // Service Properties
    private static final String SOUTHBOUND_PROTOCOL = "southboundProtocol";
    private static final String PROVIDER_TYPE = "providerType";
    private static final String OPENFLOW_VERSION = "openflowVersion";

    // The provider for each of these services is resolved by the OSGi Service Manager
    private volatile ConfigurationService configurationService;

    private HashMap<String, ProviderEntry> providers = Maps.newHashMap();
    private HashMap<Node, NetworkingProvider> nodeToProviderMapping = Maps.newHashMap();

    public NetworkingProvider getProvider(Node node) {
        // This will change if/when we move to a commons library
        Preconditions.checkArgument(node.getType().equals("OVS"));

        if (nodeToProviderMapping.get(node) != null) {
            return nodeToProviderMapping.get(node);
        }

        final String targetVersion = configurationService.getOpenflowVersion(node);

        Predicate<ProviderEntry> providerEntryPredicate = new Predicate<ProviderEntry>() {
            @Override
            public boolean apply(ProviderEntry providerEntry) {
                //ToDo: This should match on southboundProtocol and providerType too
                return providerEntry.getProperties().get(OPENFLOW_VERSION).equals(targetVersion);
            }
        };

        Iterable<ProviderEntry> matchingProviders = Iterables.filter(providers.values(), providerEntryPredicate);
        if (!matchingProviders.iterator().hasNext()) {
            logger.error("No providers matching {} found", targetVersion);
        }

        // Return the first match as only have one matching provider today
        // ToDo: Tie-breaking logic
        NetworkingProvider provider = matchingProviders.iterator().next().getProvider();
        nodeToProviderMapping.put(node, provider);
        return provider;
    }

    public void providerAdded(ServiceReference ref, NetworkingProvider provider){
        Map <String, String> properties = Maps.newHashMap();
        String pid = (String)ref.getProperty(Constants.SERVICE_PID);
        properties.put(SOUTHBOUND_PROTOCOL, (String) ref.getProperty(SOUTHBOUND_PROTOCOL));
        properties.put(OPENFLOW_VERSION, (String) ref.getProperty(OPENFLOW_VERSION));
        properties.put(PROVIDER_TYPE, (String) ref.getProperty(PROVIDER_TYPE));
        providers.put(pid, new ProviderEntry(provider, properties));
        logger.info("Neutron Networking Provider Registered: {}", provider.getClass().getName());
    }

    public void providerRemoved(ServiceReference ref){
        String pid = (String)ref.getProperty(Constants.SERVICE_PID);
        providers.remove(pid);
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
