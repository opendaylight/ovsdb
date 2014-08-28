/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker, Flavio Fernandes
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.util.Set;

/**
 * A MultiTenantForwardingProvider provides Multi-Tenant L3 Forwarding
 */
public interface MultiTenantRouterForwardingProvider {

    Status programStaticArpEntry(Node node, Long dpid, String segmentationId,
                                 String macAddress, InetAddress ipAddress, AbstractEvent.Action action);

    Status programIpRewriteRule(Node node, Long dpid, String segmentationId, InetAddress matchAddress,
                                InetAddress rewriteAddress, AbstractEvent.Action action);

    Status programIpRewriteExclusion(Node node, Long dpid, String segmentationId,
                                     InterfaceAddress excludedAddress, AbstractEvent.Action action);

    Status programRouterInterface(Node node, Long dpid, String segmentationId, String macAddress,
                                  Set<InterfaceAddress> addresses, AbstractEvent.Action action);

    Status programForwardingTableEntry(Node node, Long dpid, String segmentationId, InetAddress ipAddress,
                                       String macAddress, AbstractEvent.Action action);

    Status programDefaultRouteEntry(Node node, Long dpid, String segmentationId, String macAddress,
                                    InetAddress nextHop, AbstractEvent.Action action);

}
