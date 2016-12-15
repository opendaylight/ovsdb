/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration.HwvtepReconciliationManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvokerImpl;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
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

public class HwvtepSouthboundProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundProvider.class);
    private static final String ENTITY_TYPE = "ovsdb-hwvtepsouthbound-provider";

    public static DataBroker getDb() {
        return db;
    }

    private static DataBroker db;
    private final EntityOwnershipService entityOwnershipService;
    private final OvsdbConnection ovsdbConnection;

    private HwvtepConnectionManager cm;
    private TransactionInvoker txInvoker;
    private EntityOwnershipCandidateRegistration registration;
    private HwvtepsbPluginInstanceEntityOwnershipListener providerOwnershipChangeListener;
    private HwvtepDataChangeListener hwvtepDTListener;
    private HwvtepReconciliationManager hwvtepReconciliationManager;

    public HwvtepSouthboundProvider(final DataBroker dataBroker,
            final EntityOwnershipService entityOwnershipServiceDependency,
            final OvsdbConnection ovsdbConnection,
            final SchemaService schemaService,
            final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer) {
        this.db = dataBroker;
        this.entityOwnershipService = entityOwnershipServiceDependency;
        registration = null;
        this.ovsdbConnection = ovsdbConnection;
        HwvtepSouthboundUtil.setInstanceIdentifierCodec(new InstanceIdentifierCodec(schemaService,
                bindingNormalizedNodeSerializer));
        LOG.info("HwvtepSouthboundProvider ovsdbConnectionService: {}", ovsdbConnection);
    }

    /**
     * Used by blueprint when starting the container.
     */
    public void init() {
        LOG.info("HwvtepSouthboundProvider Session Initiated");
        txInvoker = new TransactionInvokerImpl(db);
        cm = new HwvtepConnectionManager(db, txInvoker, entityOwnershipService);
        hwvtepDTListener = new HwvtepDataChangeListener(db, cm);
        hwvtepReconciliationManager = new HwvtepReconciliationManager(db, cm);
        //Register listener for entityOnwership changes
        providerOwnershipChangeListener =
                new HwvtepsbPluginInstanceEntityOwnershipListener(this,this.entityOwnershipService);

        //register instance entity to get the ownership of the provider
        Entity instanceEntity = new Entity(ENTITY_TYPE, ENTITY_TYPE);
        try {
            Optional<EntityOwnershipState> ownershipStateOpt = entityOwnershipService.getOwnershipState(instanceEntity);
            registration = entityOwnershipService.registerCandidate(instanceEntity);
            if (ownershipStateOpt.isPresent()) {
                EntityOwnershipState ownershipState = ownershipStateOpt.get();
                if (ownershipState.hasOwner() && !ownershipState.isOwner()) {
                    ovsdbConnection.registerConnectionListener(cm);
                    ovsdbConnection.startOvsdbManager(HwvtepSouthboundConstants.DEFAULT_OVSDB_PORT);
                }
            }
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.warn("HWVTEP Southbound Provider instance entity {} was already "
                    + "registered for ownership", instanceEntity, e);
        }
    }

    @Override
    public void close() throws Exception {
        LOG.info("HwvtepSouthboundProvider Closed");
        if(cm != null){
            cm.close();
            cm = null;
        }
        if(registration != null) {
            registration.close();
            registration = null;
        }
        if(providerOwnershipChangeListener != null) {
            providerOwnershipChangeListener.close();
            providerOwnershipChangeListener = null;
        }
        if(hwvtepDTListener != null) {
            hwvtepDTListener.close();
            hwvtepDTListener = null;
        }
    }

    private void initializeHwvtepTopology(LogicalDatastoreType type) {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID));
        ReadWriteTransaction transaction = db.newReadWriteTransaction();
        CheckedFuture<Optional<Topology>, ReadFailedException> hwvtepTp = transaction.read(type, path);
        try {
            if (!hwvtepTp.get().isPresent()) {
                TopologyBuilder tpb = new TopologyBuilder();
                tpb.setTopologyId(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
                transaction.put(type, path, tpb.build(), true);
                transaction.submit();
            } else {
                transaction.cancel();
            }
        } catch (Exception e) {
            LOG.error("Error initializing hwvtep topology", e);
        }
    }

    public void handleOwnershipChange(EntityOwnershipChange ownershipChange) {
        if (ownershipChange.isOwner()) {
            LOG.info("*This* instance of HWVTEP southbound provider is set as a MASTER instance");
            LOG.info("Initialize HWVTEP topology {} in operational and config data store if not already present"
                    ,HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
            initializeHwvtepTopology(LogicalDatastoreType.OPERATIONAL);
            initializeHwvtepTopology(LogicalDatastoreType.CONFIGURATION);
        } else {
            LOG.info("*This* instance of HWVTEP southbound provider is set as a SLAVE instance");
        }
        ovsdbConnection.registerConnectionListener(cm);
        ovsdbConnection.startOvsdbManager(HwvtepSouthboundConstants.DEFAULT_OVSDB_PORT);
    }

    private class HwvtepsbPluginInstanceEntityOwnershipListener implements EntityOwnershipListener {
        private HwvtepSouthboundProvider hsp;
        private EntityOwnershipListenerRegistration listenerRegistration;

        HwvtepsbPluginInstanceEntityOwnershipListener(HwvtepSouthboundProvider hsp,
                EntityOwnershipService entityOwnershipService) {
            this.hsp = hsp;
            listenerRegistration = entityOwnershipService.registerListener(ENTITY_TYPE, this);
        }

        public void close() {
            this.listenerRegistration.close();
        }
        @Override
        public void ownershipChanged(EntityOwnershipChange ownershipChange) {
            hsp.handleOwnershipChange(ownershipChange);
        }
    }

}
