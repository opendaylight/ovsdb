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
 * A Router that is Multi-Tenant Aware
 * Each tenant has a Unique Identifier which is used to isolate interfaces/routes
 */
public interface MultiTenantAwareRouter {

  void addInterface(UUID tenantId, String interfaceName, Set<InterfaceAddress> addresses);

  void addInterface(UUID tenantId, String interfaceName, String macAddress, Set<InterfaceAddress> addresses);

  void updateInterface(UUID tenantId, String interfaceName, Set<InterfaceAddress> addresses);

  void updateInterface(UUID tenantId, String interfaceName, String macAddress, Set<InterfaceAddress> addresses);

  void removeInterface(UUID tenantId, String interfaceName);

  void addRoute(UUID tenantId, InterfaceAddress destination, InetAddress nextHop);

  void addRoute(UUID tenantId, InterfaceAddress destination, InetAddress nextHop, Integer priority);

  void removeRoute(UUID tenantId, InterfaceAddress destination, InetAddress nextHop);

  void removeRoute(UUID tenantId, InterfaceAddress destination, InetAddress nextHop, Integer priority);

  void addDefaultRoute(UUID tenantId, InetAddress nextHop);

  void addDefaultRoute(UUID tenantId, InetAddress nextHop, Integer priority);


}
