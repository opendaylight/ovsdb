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

import java.net.InetAddress;

/**
 * This interface allows Routing flows to be written to devices
 */
public interface RoutingProvider {

    Status programRouterInterface(Node node, Long dpid, String srcSegId, String dstSegId, String macAddress,
                                  InetAddress address, int mask, Action action);

    Status programDefaultRouteEntry(Node node, Long dpid, String segmentationId, String macAddress,
                                    InetAddress nextHop, Action action);

}
