/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.net.InetAddress;

/**
 * A Router
 */
public interface Router {

    void addInterface(String interfaceName, InetAddress address, int mask);

    void addInterface(String interfaceName, String macAddress, InetAddress address, int mask);

    void updateInterface(String interfaceName, InetAddress address, int mask);

    void updateInterface(String interfaceName, String macAddress, InetAddress address, int mask);

    void removeInterface(String interfaceName);

    void addRoute(String destinationCidr, InetAddress nextHop);

    void addRoute(String destinationCidr, InetAddress nextHop, Integer priority);

    void removeRoute(String destinationCidr, InetAddress nextHop);

    void removeRoute(String destinationCidr, InetAddress nextHop, Integer priority);

    void addDefaultRoute(InetAddress nextHop);

    void addDefaultRoute(InetAddress nextHop, Integer priority);

    void addNatRule(InetAddress matchAddress, InetAddress rewriteAddress);

}
