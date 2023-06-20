/*
 * Copyright (c) 2014, 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipChange;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipListener;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvokerImpl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SouthboundProvider implements ClusteredDataTreeChangeListener<Topology>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundProvider.class);
    private static final String ENTITY_TYPE = "ovsdb-southbound-provider";

    // FIXME: get rid of this static
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    private static DataBroker db;
    // FIXME: get rid of this static
    private static List<String> reconcileBridgeInclusionList = new ArrayList<>();
    // FIXME: get rid of this static
    private static List<String> reconcileBridgeExclusionList = new ArrayList<>();

    public static DataBroker getDb() {
        return db;
    }

    private OvsdbConnectionManager cm;
    private TransactionInvoker txInvoker;
    private OvsdbDataTreeChangeListener ovsdbDataTreeChangeListener;
    private OvsdbOperGlobalListener ovsdbOperGlobalListener;
    private final EntityOwnershipService entityOwnershipService;
    private EntityOwnershipCandidateRegistration registration;
    private SouthboundPluginInstanceEntityOwnershipListener providerOwnershipChangeListener;
    private final OvsdbConnection ovsdbConnection;
    private final InstanceIdentifierCodec instanceIdentifierCodec;
    private final SystemReadyMonitor systemReadyMonitor;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private ListenerRegistration<SouthboundProvider> operTopologyRegistration;
    private final OvsdbDiagStatusProvider ovsdbStatusProvider;

    @Inject
    public SouthboundProvider(final DataBroker dataBroker,
                              final EntityOwnershipService entityOwnershipServiceDependency,
                              final OvsdbConnection ovsdbConnection,
                              final DOMSchemaService schemaService,
                              final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer,
                              final SystemReadyMonitor systemReadyMonitor,
                              final DiagStatusService diagStatusService) {
        SouthboundProvider.db = dataBroker;
        entityOwnershipService = entityOwnershipServiceDependency;
        registration = null;
        this.ovsdbConnection = ovsdbConnection;
        ovsdbStatusProvider = new OvsdbDiagStatusProvider(diagStatusService);
        instanceIdentifierCodec = new InstanceIdentifierCodec(schemaService, bindingNormalizedNodeSerializer);
        this.systemReadyMonitor = systemReadyMonitor;
        LOG.info("SouthboundProvider ovsdbConnectionService Initialized");
    }

    /**
     * Used by blueprint when starting the container.
     */
    @PostConstruct
    public void init() {
        LOG.info("SouthboundProvider Session Initiated");
        ovsdbStatusProvider.reportStatus(ServiceState.STARTING, "OVSDB initialization in progress");
        txInvoker = new TransactionInvokerImpl(db);
        cm = new OvsdbConnectionManager(db, txInvoker, entityOwnershipService, ovsdbConnection,
                instanceIdentifierCodec);
        ovsdbDataTreeChangeListener = new OvsdbDataTreeChangeListener(db, cm, instanceIdentifierCodec);
        ovsdbOperGlobalListener = new OvsdbOperGlobalListener(db, cm, txInvoker);

        //Register listener for entityOnwership changes
        providerOwnershipChangeListener =
                new SouthboundPluginInstanceEntityOwnershipListener(this, entityOwnershipService);

        //register instance entity to get the ownership of the provider
        Entity instanceEntity = new Entity(ENTITY_TYPE, ENTITY_TYPE);
        try {
            registration = entityOwnershipService.registerCandidate(instanceEntity);
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.warn("OVSDB Southbound Provider instance entity {} was already "
                    + "registered for ownership", instanceEntity, e);
        }
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));
        DataTreeIdentifier<Topology> treeId =
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, path);

        LOG.trace("Registering listener for path {}", treeId);
        operTopologyRegistration = db.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    @PreDestroy
    public void close() {
        LOG.info("SouthboundProvider Closed");
        try {
            txInvoker.close();
        } catch (InterruptedException e) {
            ovsdbStatusProvider.reportStatus(ServiceState.ERROR, "OVSDB service shutdown error");
            LOG.debug("SouthboundProvider failed to close TransactionInvoker.");
        }
        cm.close();
        ovsdbDataTreeChangeListener.close();
        ovsdbOperGlobalListener.close();
        registration.close();
        providerOwnershipChangeListener.close();
        if (operTopologyRegistration != null) {
            operTopologyRegistration.close();
            operTopologyRegistration = null;
        }
        ovsdbStatusProvider.reportStatus(ServiceState.UNREGISTERED, "OVSDB Service stopped");
        ovsdbStatusProvider.close();
    }

    private static void initializeOvsdbTopology(final @NonNull LogicalDatastoreType type) {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));
        ReadWriteTransaction transaction = db.newReadWriteTransaction();
        FluentFuture<Boolean> ovsdbTp = transaction.exists(type, path);
        try {
            if (!ovsdbTp.get().booleanValue()) {
                TopologyBuilder tpb = new TopologyBuilder();
                tpb.setTopologyId(SouthboundConstants.OVSDB_TOPOLOGY_ID);
                transaction.mergeParentStructurePut(type, path, tpb.build());
                transaction.commit();
            } else {
                transaction.cancel();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error initializing ovsdb topology", e);
        }
    }

    public void handleOwnershipChange(final EntityOwnershipChange ownershipChange) {
        if (ownershipChange.getState().isOwner()) {
            LOG.info("*This* instance of OVSDB southbound provider is set as a MASTER instance");
            LOG.info("Initialize OVSDB topology {} in operational and config data store if not already present",
                    SouthboundConstants.OVSDB_TOPOLOGY_ID);
            initializeOvsdbTopology(LogicalDatastoreType.OPERATIONAL);
            initializeOvsdbTopology(LogicalDatastoreType.CONFIGURATION);
        } else {
            LOG.info("*This* instance of OVSDB southbound provider is set as a SLAVE instance");
        }
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Topology>> collection) {
        if (!registered.getAndSet(true)) {
            LOG.info("Starting the ovsdb port");
            ovsdbConnection.registerConnectionListener(cm);
            LOG.info("Registering deferred system ready listener to start OVSDB Manager later");
            systemReadyMonitor.registerListener(() -> {
                ovsdbConnection.startOvsdbManager();
                LOG.info("Started OVSDB Manager (in system ready listener)");
            });

            if (operTopologyRegistration != null) {
                operTopologyRegistration.close();
                operTopologyRegistration = null;
            }
            ovsdbStatusProvider.reportStatus(ServiceState.OPERATIONAL, "OVSDB initialization complete");
        }
    }

    private static final class SouthboundPluginInstanceEntityOwnershipListener implements EntityOwnershipListener {
        private final SouthboundProvider sp;
        private final EntityOwnershipListenerRegistration listenerRegistration;

        SouthboundPluginInstanceEntityOwnershipListener(final SouthboundProvider sp,
                final EntityOwnershipService entityOwnershipService) {
            this.sp = sp;
            listenerRegistration = entityOwnershipService.registerListener(ENTITY_TYPE, this);
        }

        public void close() {
            listenerRegistration.close();
        }

        @Override
        public void ownershipChanged(final EntityOwnershipChange ownershipChange) {
            sp.handleOwnershipChange(ownershipChange);
        }
    }

    public void setSkipMonitoringManagerStatus(final boolean flag) {
        LOG.debug("skipManagerStatus set to {}", flag);
        if (flag) {
            SouthboundConstants.SKIP_COLUMN_FROM_TABLE.get("Manager").add("status");
        } else {
            SouthboundConstants.SKIP_COLUMN_FROM_TABLE.get("Manager").remove("status");
        }
    }

    @SuppressFBWarnings("EI_EXPOSE_STATIC_REP2")
    public static void setBridgesReconciliationInclusionList(final List<String> bridgeList) {
        reconcileBridgeInclusionList = bridgeList;
    }

    @SuppressFBWarnings("EI_EXPOSE_STATIC_REP2")
    public static void setBridgesReconciliationExclusionList(final List<String> bridgeList) {
        reconcileBridgeExclusionList = bridgeList;
    }

    public static List<String> getBridgesReconciliationInclusionList() {
        return reconcileBridgeInclusionList;
    }

    public static List<String> getBridgesReconciliationExclusionList() {
        return reconcileBridgeExclusionList;
    }

    @VisibleForTesting
    boolean isRegistered() {
        return registered.get();
    }
}
