package org.opendaylight.ovsdb.neutron.provider;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ProviderNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(ProviderNetworkManager.class);
    private static ProviderNetworkManager provider;

    public static ProviderNetworkManager getManager() {
        if (provider != null) return provider;
        if (System.getProperty("OF1.3_Provider") != null) {
            provider = new OF13ProviderManager();
        } else {
            provider = new OF10ProviderManager();
        }
        return provider;
    }

    public abstract boolean hasPerTenantTunneling();
    public abstract Status createTunnels(String tunnelType, String tunnelKey);
    public abstract Status createTunnels(String tunnelType, String tunnelKey, Node source);
}
