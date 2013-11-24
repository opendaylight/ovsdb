package org.opendaylight.ovsdb.neutron.provider;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;


class OF13ProviderManager extends ProviderNetworkManager {
    @Override
    public boolean hasPerTenantTunneling() {
        return false;
    }

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey, Node source) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey) {
        // TODO Auto-generated method stub
        return null;
    }
}
