/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */
package org.opendaylight.ovsdb.neutron.provider;

import org.opendaylight.ovsdb.neutron.IAdminConfigManager;
import org.opendaylight.ovsdb.neutron.IInternalNetworkManager;
import org.opendaylight.ovsdb.neutron.ITenantNetworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProviderNetworkManager implements IProviderNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(ProviderNetworkManager.class);
    private NetworkProvider provider;
    protected static final String OPENFLOW_10 = "1.0";
    protected static final String OPENFLOW_13 = "1.3";

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile IAdminConfigManager adminConfigManager;
    private volatile IInternalNetworkManager internalNetworkManager;
    private volatile ITenantNetworkManager tenantNetworkManager;

    public NetworkProvider getProvider() {
        if (provider != null) return provider;
        String ofVersion = System.getProperty("ovsdb.of.version", OPENFLOW_10);
        switch (ofVersion) {
            case OPENFLOW_13:
                provider = new OF13Provider(adminConfigManager, internalNetworkManager, tenantNetworkManager);
                break;
            case OPENFLOW_10:
            default:
                provider = new OF10Provider(adminConfigManager, internalNetworkManager, tenantNetworkManager);
                break;
        }
        return provider;
    }

}
