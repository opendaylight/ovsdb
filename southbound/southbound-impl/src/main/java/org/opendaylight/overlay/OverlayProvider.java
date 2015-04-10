/*
* Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.overlay;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.overlay.transactions.md.utils.TransactionInvoker;
import org.opendaylight.overlay.transactions.md.utils.TransactionInvokerImpl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlayProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OverlayProvider.class);
    private DataBroker db;
    private TransactionInvoker txInvoker;
    private OverlayOperationalDataChangeListener overlayOperationalDataChangeListener;
    private OverlayConfigDataChangeListener overlayConfigDataChangeListener;


    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("OverlayProvider Session Initiated");
        db = session.getSALService(DataBroker.class);
        this.txInvoker = new TransactionInvokerImpl(db);
        overlayOperationalDataChangeListener = new OverlayOperationalDataChangeListener(db);
        overlayConfigDataChangeListener = new OverlayConfigDataChangeListener(db);
        initializeOverlayTopology(LogicalDatastoreType.OPERATIONAL);
        initializeOverlayTopology(LogicalDatastoreType.CONFIGURATION);
    }

    @Override
    public void close() throws Exception {
        LOG.info("OverlayProvider Closed");
        overlayOperationalDataChangeListener.close();
        overlayConfigDataChangeListener.close();
    }

    private void initializeOverlayTopology(LogicalDatastoreType type) {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OverlayConstants.OVERLAY_TOPOLOGY_ID));
        ReadWriteTransaction transaction = db.newReadWriteTransaction();
        initializeTopology(transaction, type);
        CheckedFuture<Optional<Topology>, ReadFailedException> overlayTp = transaction.read(type, path);
        try {
            if (!overlayTp.get().isPresent()) {
                TopologyBuilder tpb = new TopologyBuilder();
                tpb.setTopologyId(OverlayConstants.OVERLAY_TOPOLOGY_ID);
                transaction.put(type, path, tpb.build());
                transaction.submit();
            } else {
                transaction.cancel();
            }
        } catch (Exception e) {
            LOG.error("Error initializing overlay topology {}", e);
        }
    }

    private void initializeTopology(ReadWriteTransaction transaction, LogicalDatastoreType type) {
        InstanceIdentifier<NetworkTopology> path = InstanceIdentifier.create(NetworkTopology.class);
        CheckedFuture<Optional<NetworkTopology>, ReadFailedException> topology = transaction.read(type, path);
        try {
            if (!topology.get().isPresent()) {
                NetworkTopologyBuilder ntb = new NetworkTopologyBuilder();
                transaction.put(type, path, ntb.build());
            }
        } catch (Exception e) {
            LOG.error("Error initializing topology {}", e);
        }
    }
}
