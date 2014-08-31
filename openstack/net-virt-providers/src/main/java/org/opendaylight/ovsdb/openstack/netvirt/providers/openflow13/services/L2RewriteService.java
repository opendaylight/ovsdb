/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;

public class L2RewriteService extends AbstractServiceInstance {
    public L2RewriteService() {
        super(Service.L2_REWRITE);
    }

    public L2RewriteService(Service service) {
        super(service);
    }

    public abstract boolean isBridgeInPipeline (String nodeId) {
        return true;
    }
}