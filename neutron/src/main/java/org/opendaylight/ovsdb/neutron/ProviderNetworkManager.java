package org.opendaylight.ovsdb.neutron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProviderNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(ProviderNetworkManager.class);
    private static ProviderNetworkManager provider = new ProviderNetworkManager();
    private ProviderNetworkManager() {
    }

    public static ProviderNetworkManager getManager() {
        return provider;
    }
}
