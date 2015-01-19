/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthboundProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SouthboundProvider.class);
    private static final Integer DEFAULT_OVSDB_PORT = 6640;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("SouthboundProvider Session Initiated");
        OvsdbConnection ovsdbConnection = new OvsdbConnectionService();
        OvsdbConnectionListener connectionListener = new OvsdbConnectionListenerImpl(session.getSALService(DataBroker.class));
        ovsdbConnection.registerConnectionListener(connectionListener);
        ovsdbConnection.startOvsdbManager(DEFAULT_OVSDB_PORT);
    }

    @Override
    public void close() throws Exception {
        LOG.info("SouthboundProvider Closed");
    }
}
