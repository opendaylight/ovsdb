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

/**
 * A Router
 */
public interface Router {

  void addInterface(String interfaceName, Set<InterfaceAddress> addresses);

  void addInterface(String interfaceName, String macAddress, Set<InterfaceAddress> addresses);

  void updateInterface(String interfaceName, Set<InterfaceAddress> addresses);

  void updateInterface(String interfaceName, String macAddress, Set<InterfaceAddress> addresses);

  void removeInterface(String interfaceName);

  void addRoute(InterfaceAddress destination, InetAddress nextHop);

  void addRoute(InterfaceAddress destination, InetAddress nextHop, Integer priority);

  void removeRoute(InterfaceAddress destination, InetAddress nextHop);

  void removeRoute(InterfaceAddress destination, InetAddress nextHop, Integer priority);

  void addDefaultRoute(InetAddress nextHop);

  void addDefaultRoute(InetAddress nextHop, Integer priority);

  void addNatRule(InetAddress matchAddress, InetAddress rewriteAddress);

}
