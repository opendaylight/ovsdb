package org.opendaylight.ovsdb.neutron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(InternalNetworkManager.class);

    private static InternalNetworkManager internalNetwork = new InternalNetworkManager();
    private InternalNetworkManager() {
    }

    public static InternalNetworkManager getManager() {
        return internalNetwork;
    }
}
