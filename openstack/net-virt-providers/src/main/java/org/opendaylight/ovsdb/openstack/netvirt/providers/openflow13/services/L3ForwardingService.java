/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.L3ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;

import java.net.InetAddress;

public class L3ForwardingService extends AbstractServiceInstance implements L3ForwardingProvider {
    public L3ForwardingService() {
        super(Service.L3_FORWARDING);
    }

    public L3ForwardingService(Service service) {
        super(service);
    }

    @Override
    public boolean isBridgeInPipeline (String nodeId) {
        return true;
    }

    @Override
    public Status programForwardingTableEntry(Node node, Long dpid, String segmentationId, InetAddress ipAddress,
                                              String macAddress, Action action) {
        return null;
    }
}
