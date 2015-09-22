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
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvokerImpl;
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
    private final OvsdbConnection ovsdbConnection;

    public SouthboundProvider(OvsdbConnection ovsdbConnection) {
        this.ovsdbConnection = ovsdbConnection;
    }

    public static DataBroker getDb() {
        return db;
    }

    //private DataBroker db;
    private static DataBroker db;
    private OvsdbConnectionManager cm;
//    private OvsdbNodeDataChangeListener ovsdbNodeListener;
//    private OvsdbManagedNodeDataChangeListener ovsdbManagedNodeListener;
//    private OvsdbTerminationPointDataChangeListener ovsdbTerminationPointListener;
    private TransactionInvoker txInvoker;
    private OvsdbDataChangeListener ovsdbDataChangeListener;


    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("SouthboundProvider Session Initiated");
        db = session.getSALService(DataBroker.class);
        this.txInvoker = new TransactionInvokerImpl(db);
        cm = new OvsdbConnectionManager(db,txInvoker);
        ovsdbDataChangeListener = new OvsdbDataChangeListener(db,cm);
//        ovsdbNodeListener = new OvsdbNodeDataChangeListener(db, cm);
//        ovsdbManagedNodeListener = new OvsdbManagedNodeDataChangeListener(db, cm);
//        ovsdbTerminationPointListener = new OvsdbTerminationPointDataChangeListener(db, cm);
        initializeOvsdbTopology(LogicalDatastoreType.OPERATIONAL);
        initializeOvsdbTopology(LogicalDatastoreType.CONFIGURATION);
        ovsdbConnection.registerConnectionListener(cm);
        ovsdbConnection.startOvsdbManager(SouthboundConstants.DEFAULT_OVSDB_PORT);
    }

    @Override
    public void close() throws Exception {
        LOG.info("SouthboundProvider Closed");
        cm.close();
        ovsdbDataChangeListener.close();
//        ovsdbNodeListener.close();
//        ovsdbManagedNodeListener.close();
//        ovsdbTerminationPointListener.close();
    }

    private void initializeOvsdbTopology(LogicalDatastoreType type) {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));
        ReadWriteTransaction transaction = db.newReadWriteTransaction();
        initializeTopology(transaction,type);
        CheckedFuture<Optional<Topology>, ReadFailedException> ovsdbTp = transaction.read(type, path);
        try {
            if (!ovsdbTp.get().isPresent()) {
                TopologyBuilder tpb = new TopologyBuilder();
                tpb.setTopologyId(SouthboundConstants.OVSDB_TOPOLOGY_ID);
                transaction.put(type, path, tpb.build());
                transaction.submit();
            } else {
                transaction.cancel();
            }
        } catch (Exception e) {
            LOG.error("Error initializing ovsdb topology", e);
        }
    }

    private void initializeTopology(ReadWriteTransaction transaction, LogicalDatastoreType type) {
        InstanceIdentifier<NetworkTopology> path = InstanceIdentifier.create(NetworkTopology.class);
        CheckedFuture<Optional<NetworkTopology>, ReadFailedException> topology = transaction.read(type,path);
        try {
            if (!topology.get().isPresent()) {
                NetworkTopologyBuilder ntb = new NetworkTopologyBuilder();
                transaction.put(type,path,ntb.build());
            }
        } catch (Exception e) {
            LOG.error("Error initializing ovsdb topology {}",e);
        }
    }
}
