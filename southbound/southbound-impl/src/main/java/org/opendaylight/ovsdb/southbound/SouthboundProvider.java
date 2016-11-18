/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvokerImpl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthboundProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SouthboundProvider.class);
    private static final String ENTITY_TYPE = "ovsdb-southbound-provider";

    public static DataBroker getDb() {
        return db;
    }

    private static DataBroker db;
    private OvsdbConnectionManager cm;
    private TransactionInvoker txInvoker;
    private OvsdbDataTreeChangeListener ovsdbDataTreeChangeListener;
    private final EntityOwnershipService entityOwnershipService;
    private EntityOwnershipCandidateRegistration registration;
    private SouthboundPluginInstanceEntityOwnershipListener providerOwnershipChangeListener;
    private final OvsdbConnection ovsdbConnection;
    private final InstanceIdentifierCodec instanceIdentifierCodec;
    private static final String SKIP_MONITORING_MANAGER_STATUS_PARAM = "skip-monitoring-manager-status";

    public SouthboundProvider(final DataBroker dataBroker,
            final EntityOwnershipService entityOwnershipServiceDependency,
            final OvsdbConnection ovsdbConnection,
            final SchemaService schemaService,
            final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer) {
        this.db = dataBroker;
        this.entityOwnershipService = entityOwnershipServiceDependency;
        registration = null;
        this.ovsdbConnection = ovsdbConnection;

        this.instanceIdentifierCodec = new InstanceIdentifierCodec(schemaService,
                bindingNormalizedNodeSerializer);
        LOG.info("SouthboundProvider ovsdbConnectionService Initialized");
    }

    /**
     * Used by blueprint when starting the container.
     */
    public void init() {
        LOG.info("SouthboundProvider Session Initiated");
        this.txInvoker = new TransactionInvokerImpl(db);
        cm = new OvsdbConnectionManager(db,txInvoker,entityOwnershipService, ovsdbConnection, instanceIdentifierCodec);
        ovsdbDataTreeChangeListener = new OvsdbDataTreeChangeListener(db, cm, instanceIdentifierCodec);

        //Register listener for entityOnwership changes
        providerOwnershipChangeListener =
                new SouthboundPluginInstanceEntityOwnershipListener(this,this.entityOwnershipService);

        //register instance entity to get the ownership of the provider
        Entity instanceEntity = new Entity(ENTITY_TYPE, ENTITY_TYPE);
        try {
            Optional<EntityOwnershipState> ownershipStateOpt = entityOwnershipService.getOwnershipState(instanceEntity);
            registration = entityOwnershipService.registerCandidate(instanceEntity);
            if (ownershipStateOpt.isPresent()) {
                EntityOwnershipState ownershipState = ownershipStateOpt.get();
                if (ownershipState.hasOwner() && !ownershipState.isOwner()) {
                    ovsdbConnection.registerConnectionListener(cm);
                    ovsdbConnection.startOvsdbManager(SouthboundConstants.DEFAULT_OVSDB_PORT);
                    LOG.info("*This* instance of OVSDB southbound provider is set as a SLAVE instance");
                }
            }
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.warn("OVSDB Southbound Provider instance entity {} was already "
                    + "registered for ownership", instanceEntity, e);
        }
    }

    @Override
    public void close() {
        LOG.info("SouthboundProvider Closed");
        cm.close();
        ovsdbDataTreeChangeListener.close();
        registration.close();
        providerOwnershipChangeListener.close();
    }

    private void initializeOvsdbTopology(LogicalDatastoreType type) {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));
        ReadWriteTransaction transaction = db.newReadWriteTransaction();
        CheckedFuture<Optional<Topology>, ReadFailedException> ovsdbTp = transaction.read(type, path);
        try {
            if (!ovsdbTp.get().isPresent()) {
                TopologyBuilder tpb = new TopologyBuilder();
                tpb.setTopologyId(SouthboundConstants.OVSDB_TOPOLOGY_ID);
                transaction.put(type, path, tpb.build(), true);
                transaction.submit();
            } else {
                transaction.cancel();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error initializing ovsdb topology", e);
        }
    }

    public void handleOwnershipChange(EntityOwnershipChange ownershipChange) {
        if (ownershipChange.isOwner()) {
            LOG.info("*This* instance of OVSDB southbound provider is set as a MASTER instance");
            LOG.info("Initialize OVSDB topology {} in operational and config data store if not already present",
                    SouthboundConstants.OVSDB_TOPOLOGY_ID);
            initializeOvsdbTopology(LogicalDatastoreType.OPERATIONAL);
            initializeOvsdbTopology(LogicalDatastoreType.CONFIGURATION);
        } else {
            LOG.info("*This* instance of OVSDB southbound provider is set as a SLAVE instance");
        }
        ovsdbConnection.registerConnectionListener(cm);
        ovsdbConnection.startOvsdbManager(SouthboundConstants.DEFAULT_OVSDB_PORT);
    }

    private class SouthboundPluginInstanceEntityOwnershipListener implements EntityOwnershipListener {
        private final SouthboundProvider sp;
        private final EntityOwnershipListenerRegistration listenerRegistration;

        SouthboundPluginInstanceEntityOwnershipListener(SouthboundProvider sp,
                EntityOwnershipService entityOwnershipService) {
            this.sp = sp;
            listenerRegistration = entityOwnershipService.registerListener(ENTITY_TYPE, this);
        }

        public void close() {
            this.listenerRegistration.close();
        }

        @Override
        public void ownershipChanged(EntityOwnershipChange ownershipChange) {
            sp.handleOwnershipChange(ownershipChange);
        }
    }

    public void updateConfigParameter(Map<String, Object> configParameters) {
        if (configParameters != null && !configParameters.isEmpty()) {
            LOG.debug("Config parameters received : {}", configParameters.entrySet());
            for (Map.Entry<String, Object> paramEntry : configParameters.entrySet()) {
                if (paramEntry.getKey().equalsIgnoreCase(SKIP_MONITORING_MANAGER_STATUS_PARAM)) {
                    setSkipMonitoringManagerStatus(Boolean.parseBoolean((String)paramEntry.getValue()));
                    break;
                }
            }
        }
    }

    public void setSkipMonitoringManagerStatus(boolean flag) {
        LOG.debug("skipManagerStatus set to {}", flag);
        if (flag) {
            SouthboundConstants.SKIP_COLUMN_FROM_TABLE.get("Manager").add("status");
        } else {
            SouthboundConstants.SKIP_COLUMN_FROM_TABLE.get("Manager").remove("status");
        }
    }
}