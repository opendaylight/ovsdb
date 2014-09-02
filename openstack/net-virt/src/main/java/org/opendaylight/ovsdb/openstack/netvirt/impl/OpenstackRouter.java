/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker, Flavio Fernandes
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.ovsdb.openstack.netvirt.api.MultiTenantAwareRouter;

import java.net.InetAddress;
import java.util.UUID;

/**
 * OpenStack router implements the MultiTenantAwareRouter interfaces It provides routing functionality for multiple
 * tenants in an OpenStack cloud
 */
public class OpenstackRouter implements MultiTenantAwareRouter {

    @Override
    public void addInterface(UUID tenantId, String interfaceName, InetAddress address, int mask) {

    }

    @Override
    public void addInterface(UUID tenantId, String interfaceName, String macAddress, InetAddress address, int mask) {

    }

    @Override
    public void updateInterface(UUID tenantId, String interfaceName, InetAddress address, int mask) {

    }

    @Override
    public void updateInterface(UUID tenantId, String interfaceName, String macAddress, InetAddress address, int mask) {

    }

    @Override
    public void removeInterface(UUID tenantId, String interfaceName) {

    }

    @Override
    public void addRoute(UUID tenantId, String destinationCidr, InetAddress nextHop) {

    }

    @Override
    public void addRoute(UUID tenantId, String destinationCidr, InetAddress nextHop, Integer priority) {

    }

    @Override
    public void removeRoute(UUID tenantId, String destinationCidr, InetAddress nextHop) {

    }

    @Override
    public void removeRoute(UUID tenantId, String destinationCidr, InetAddress nextHop, Integer priority) {

    }

    @Override
    public void addDefaultRoute(UUID tenantId, InetAddress nextHop) {

    }

    @Override
    public void addDefaultRoute(UUID tenantId, InetAddress nextHop, Integer priority) {

    }

    @Override
    public void addNatRule(UUID tenantId, InetAddress matchAddress, InetAddress rewriteAddress) {

    }
}
