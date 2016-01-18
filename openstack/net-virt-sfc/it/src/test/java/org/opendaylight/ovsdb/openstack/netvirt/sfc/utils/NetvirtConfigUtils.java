/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.utils;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.providers.config.rev160109.NetvirtProvidersConfigBuilder;

public class NetvirtConfigUtils {
    public NetvirtProvidersConfigBuilder netvirtProvidersConfigBuilder(
            NetvirtProvidersConfigBuilder netvirtProvidersConfigBuilder, short tableOffset) {
        return netvirtProvidersConfigBuilder.setTableOffset(tableOffset);
    }
}
