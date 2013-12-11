/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.neutron.provider;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.lib.table.Interface;


class OF13ProviderManager extends ProviderNetworkManager {
    @Override
    public boolean hasPerTenantTunneling() {
        return false;
    }

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey, Node source, Interface intf) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey) {
        // TODO Auto-generated method stub
        return null;
    }
}
