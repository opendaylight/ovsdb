/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class SouthboundProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SouthboundProvider.class);
    private static final Integer DEFAULT_OVSDB_PORT = 6640;
    private DataBroker db;
    private OvsdbNodeDataChangeListener ovsdbNodeListener;
    private OvsdbConnectionListenerImpl connectionListener;


    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("SouthboundProvider Session Initiated");
        db = session.getSALService(DataBroker.class);
        initializeOvsdbTopology(LogicalDatastoreType.OPERATIONAL);
        initializeOvsdbTopology(LogicalDatastoreType.CONFIGURATION);
        ovsdbNodeListener = new OvsdbNodeDataChangeListener(db);
        OvsdbConnection ovsdbConnection = new OvsdbConnectionService();
        connectionListener = new OvsdbConnectionListenerImpl(db);
        ovsdbConnection.registerConnectionListener(connectionListener);
        ovsdbConnection.startOvsdbManager(DEFAULT_OVSDB_PORT);
    }

    @Override
    public void close() throws Exception {
        LOG.info("SouthboundProvider Closed");
        ovsdbNodeListener.close();
        connectionListener.close();
    }

    private void initializeOvsdbTopology(LogicalDatastoreType type) {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));
        ReadWriteTransaction transaction = db.newReadWriteTransaction();
        initializeTopology(transaction,type);
        CheckedFuture<Optional<Topology>, ReadFailedException> ovsdbTp = transaction.read(type, path);
        try {
            if(!ovsdbTp.get().isPresent()) {
                TopologyBuilder tpb = new TopologyBuilder();
                tpb.setTopologyId(SouthboundConstants.OVSDB_TOPOLOGY_ID);
                transaction.put(type, path, tpb.build());
                transaction.submit();
            } else {
                transaction.cancel();
            }
        } catch (Exception e) {
            LOG.error("Error initializing ovsdb topology {}",e);
        }
    }

    private void initializeTopology(ReadWriteTransaction t, LogicalDatastoreType type) {
        InstanceIdentifier<NetworkTopology> path = InstanceIdentifier
                .create(NetworkTopology.class);
       CheckedFuture<Optional<NetworkTopology>, ReadFailedException> tp = t.read(type,path);
       try {
           if(!tp.get().isPresent()) {
               NetworkTopologyBuilder ntb = new NetworkTopologyBuilder();
               t.put(type,path,ntb.build());
           }
       } catch (Exception e) {
           LOG.error("Error initializing ovsdb topology {}",e);
       }
    }
}
