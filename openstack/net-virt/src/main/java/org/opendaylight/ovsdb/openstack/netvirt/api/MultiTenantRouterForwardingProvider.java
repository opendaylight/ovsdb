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

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.util.Set;
import java.util.UUID;

/**
 * A MultiTenantForwardingProvider provides Multi-Tenant L3 Forwarding
 */
public interface MultiTenantRouterForwardingProvider {

  void addStaticArpEntry(UUID tenantId, String macAddress, InetAddress ipAddress);

  void addIpRewriteRule(UUID tenantId, InetAddress matchAddress, InetAddress rewriteAddress);

  void addRouterInterface(UUID tenantId, String macAddress, Set<InterfaceAddress> addresses);

  void addForwardingTableEntry(UUID tenantId, InetAddress ipAddress, String macAddress);

  void addDefaultRouteEntry(UUID tenantId, String macAddress, InetAddress nextHop);

}
