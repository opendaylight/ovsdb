/*******************************************************************************
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Created the DummyProvider class for use in Unit Tests.
 *******************************************************************************/
package org.opendaylight.ovsdb.neutron.mocks;

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