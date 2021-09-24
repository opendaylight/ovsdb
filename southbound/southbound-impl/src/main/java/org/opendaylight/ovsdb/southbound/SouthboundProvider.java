/*
 * Copyright (c) 2014, 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.opendaylight.ovsdb.southbound.reconciliation.OvsdbUpgradeStateListener;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvokerImpl;
import org.opendaylight.serviceutils.upgrade.UpgradeState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = {}, configurationPid = "org.opendaylight.ovsdb.southbound")
@Designate(ocd = SouthboundProvider.Configuration.class)
@RequireServiceComponentRuntime
public class SouthboundProvider implements ClusteredDataTreeChangeListener<Topology>, AutoCloseable {
    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition(name = "skip-monitoring-manager-status")
        boolean skipMonitoringManagerStatus() default false;
        @AttributeDefinition(name = "bridge-reconciliation-inclusion-list")
        String[] bridgesReconciliationInclusionList() default { };
        @AttributeDefinition(name = "bridge-reconciliation-exclusion-list")
        String[] bridgesReconciliationExclusionList() default { };
    }

    private static final Logger LOG = LoggerFactory.getLogger(SouthboundProvider.class);
    private static final String ENTITY_TYPE = "ovsdb-southbound-provider";

    // FIXME: get rid of this statics
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    private static DataBroker db;
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    private static volatile List<String> reconcileBridgeInclusionList = List.of();
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    private static volatile List<String> reconcileBridgeExclusionList = List.of();

    public static DataBroker getDb() {
        return db;
    }

    private final AtomicBoolean registered = new AtomicBoolean();
    private final OvsdbConnectionManager cm;
    private final TransactionInvoker txInvoker;
    private final OvsdbDataTreeChangeListener ovsdbDataTreeChangeListener;
    private final OvsdbOperGlobalListener ovsdbOperGlobalListener;
    private final SouthboundPluginInstanceEntityOwnershipListener providerOwnershipChangeListener;
    private final OvsdbConnection ovsdbConnection;
    private final SystemReadyMonitor systemReadyMonitor;
    private final UpgradeState upgradeState;
    private final OvsdbDiagStatusProvider ovsdbStatusProvider;
    private final OvsdbUpgradeStateListener ovsdbUpgradeStateListener;

    private ListenerRegistration<SouthboundProvider> operTopologyRegistration;
    private EntityOwnershipCandidateRegistration registration;

    @Inject
    @Activate
    public SouthboundProvider(@Reference final DataBroker dataBroker,
                              @Reference final EntityOwnershipService entityOwnershipService,
                              @Reference final OvsdbConnection ovsdbConnection,
                              @Reference final DOMSchemaService schemaService,
                              @Reference final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer,
                              @Reference final SystemReadyMonitor systemReadyMonitor,
                              @Reference final DiagStatusService diagStatusService,
                              @Reference final UpgradeState upgradeState, final Configuration configuration) {
        SouthboundProvider.db = dataBroker;
        updateConfiguration(configuration);

        this.ovsdbConnection = ovsdbConnection;
        ovsdbStatusProvider = new OvsdbDiagStatusProvider(diagStatusService);
        this.systemReadyMonitor = systemReadyMonitor;
        this.upgradeState = upgradeState;
        LOG.info("SouthboundProvider ovsdbConnectionService Initialized");

        LOG.info("SouthboundProvider Session Initiated");
        ovsdbStatusProvider.reportStatus(ServiceState.STARTING, "OVSDB initialization in progress");
        txInvoker = new TransactionInvokerImpl(db);
        final InstanceIdentifierCodec instanceIdentifierCodec = new InstanceIdentifierCodec(schemaService,
            bindingNormalizedNodeSerializer);
        cm = new OvsdbConnectionManager(db, txInvoker, entityOwnershipService, ovsdbConnection,
                instanceIdentifierCodec, upgradeState);
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
            LOG.warn("OVSDB Southbound Provider instance entity {} was already registered for ownership",
                instanceEntity, e);
            registration = null;
        }
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));
        DataTreeIdentifier<Topology> treeId =
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, path);

        LOG.trace("Registering listener for path {}", treeId);
        operTopologyRegistration = db.registerDataTreeChangeListener(treeId, this);
        ovsdbUpgradeStateListener = new OvsdbUpgradeStateListener(db, cm);
    }

    @Override
    @Deactivate
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
        if (ovsdbUpgradeStateListener != null) {
            ovsdbUpgradeStateListener.close();
        }
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

    private static class SouthboundPluginInstanceEntityOwnershipListener implements EntityOwnershipListener {
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

    public UpgradeState getUpgradeState() {
        return upgradeState;
    }

    @Modified
    public void updateConfiguration(final Configuration configuration) {
        final var skipManagerStatus = configuration.skipMonitoringManagerStatus();
        LOG.debug("skipManagerStatus set to {}", skipManagerStatus);
        if (skipManagerStatus) {
            // FIXME: this appears to be modifying global state which appears to be otherwise immutable
            SouthboundConstants.SKIP_COLUMN_FROM_TABLE.get("Manager").add("status");
        } else {
            SouthboundConstants.SKIP_COLUMN_FROM_TABLE.get("Manager").remove("status");
        }

        reconcileBridgeInclusionList = List.of(configuration.bridgesReconciliationInclusionList());
        reconcileBridgeExclusionList = List.of(configuration.bridgesReconciliationExclusionList());
    }

    @VisibleForTesting
    public static void setBridgesReconciliationInclusionList(final List<String> list) {
        reconcileBridgeInclusionList = requireNonNull(list);
    }
}
