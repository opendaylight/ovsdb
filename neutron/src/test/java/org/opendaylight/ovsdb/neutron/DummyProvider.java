package org.opendaylight.ovsdb.neutron;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.neutron.provider.ProviderNetworkManager;

public class DummyProvider extends ProviderNetworkManager {

    @Override
    public boolean hasPerTenantTunneling() {
        return true;
    }

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey) {
        return null;
    }

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey, Node source, Interface intf) {
        return null;
    }

    @Override
    public void initializeFlowRules(Node node) {
        return;
    }

    @Override
    public void initializeOFFlowRules(Node openflowNode) {
        return;
    }
}