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
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.OutboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;

import java.net.InetAddress;

public class OutboundNatService extends AbstractServiceInstance implements OutboundNatProvider {
    public OutboundNatService() {
        super(Service.OUTBOUND_NAT);
    }

    public OutboundNatService(Service service) {
        super(service);
    }

    @Override
    public boolean isBridgeInPipeline (String nodeId) {
        return true;
    }

    @Override
    public Status programIpRewriteRule(Node node, Long dpid, String segmentationId, InetAddress matchAddress,
                                       InetAddress rewriteAddress, Action action) {
        return new Status(StatusCode.NOTIMPLEMENTED);
    }

    @Override
    public Status programIpRewriteExclusion(Node node, Long dpid, String segmentationId, String excludedCidr,
                                            Action action) {
        return new Status(StatusCode.NOTIMPLEMENTED);
    }
}
