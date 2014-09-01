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

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.api.RoutingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;

import java.net.InetAddress;

public class RoutingService extends AbstractServiceInstance implements RoutingProvider {
    public RoutingService() {
        super(Service.ROUTING);
    }

    public RoutingService(Service service) {
        super(service);
    }

    @Override
    public boolean isBridgeInPipeline (String nodeId) {
        return true;
    }

    @Override
    public Status programRouterInterface(Node node, Long dpid, String segmentationId, String macAddress,
                                         InetAddress address, int mask, AbstractEvent.Action action) {
        return new Status(StatusCode.NOTIMPLEMENTED);
    }

    @Override
    public Status programDefaultRouteEntry(Node node, Long dpid, String segmentationId, String macAddress,
                                           InetAddress nextHop, AbstractEvent.Action action) {
        return new Status(StatusCode.NOTIMPLEMENTED);
    }
}
