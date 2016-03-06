/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.net.InetAddress;
import java.util.UUID;

/**
 * A Router that is Multi-Tenant Aware
 * Each tenant has a Unique Identifier which is used to isolate interfaces/routes
 */
public interface MultiTenantAwareRouter {

    void addInterface(UUID tenantId, String interfaceName, InetAddress address, int mask);

    void addInterface(UUID tenantId, String interfaceName, String macAddress, InetAddress address, int mask);

    void updateInterface(UUID tenantId, String interfaceName, InetAddress address, int mask);

    void updateInterface(UUID tenantId, String interfaceName, String macAddress, InetAddress address, int mask);

    void removeInterface(UUID tenantId, String interfaceName);

    void addRoute(UUID tenantId, String destinationCidr, InetAddress nextHop);

    void addRoute(UUID tenantId, String destinationCidr, InetAddress nextHop, Integer priority);

    void removeRoute(UUID tenantId, String destinationCidr, InetAddress nextHop);

    void removeRoute(UUID tenantId, String destinationCidr, InetAddress nextHop, Integer priority);

    void addDefaultRoute(UUID tenantId, InetAddress nextHop);

    void addDefaultRoute(UUID tenantId, InetAddress nextHop, Integer priority);

    void addNatRule(UUID tenantId, InetAddress matchAddress, InetAddress rewriteAddress);

}
