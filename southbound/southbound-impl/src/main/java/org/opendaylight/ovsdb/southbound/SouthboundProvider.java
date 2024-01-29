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
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipListener;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipStateChange;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvokerImpl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = { }, configurationPid = "org.opendaylight.ovsdb.southbound")
@Designate(ocd = SouthboundProvider.Configuration.class)
// non-final for testing
public class SouthboundProvider implements DataTreeChangeListener<Topology>, AutoCloseable {
    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition
        boolean skip$_$monitoring$_$manager$_$status() default false;
        @AttributeDefinition
        String[] bridge$_$reconciliation$_$inclusion$_$list();
        @AttributeDefinition
        String[] bridge$_$reconciliation$_$exclusion$_$list();
    }

    private static final Logger LOG = LoggerFactory.getLogger(SouthboundProvider.class);
    private static final String ENTITY_TYPE = "ovsdb-southbound-provider";

    // FIXME: get rid of this static
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    private static DataBroker db;
    // FIXME: get rid of this static
    private static List<String> reconcileBridgeInclusionList = List.of();
    // FIXME: get rid of this static
    private static List<String> reconcileBridgeExclusionList = List.of();

    public static DataBroker getDb() {
        return db;
    }

    public static List<String> getBridgesReconciliationInclusionList() {
        return reconcileBridgeInclusionList;
    }

    public static List<String> getBridgesReconciliationExclusionList() {
        return reconcileBridgeExclusionList;
    }

    @VisibleForTesting
    @SuppressFBWarnings("EI_EXPOSE_STATIC_REP2")
    public static void setBridgesReconciliationInclusionList(final List<String> list) {
        reconcileBridgeInclusionList = list;
    }

    private final OvsdbConnectionManager cm;
    private final TransactionInvoker txInvoker;
    private final OvsdbDataTreeChangeListener ovsdbDataTreeChangeListener;
    private final OvsdbOperGlobalListener ovsdbOperGlobalListener;
    private final EntityOwnershipService entityOwnershipService;
    private final SouthboundPluginInstanceEntityOwnershipListener providerOwnershipChangeListener;
    private final OvsdbConnection ovsdbConnection;
    private final InstanceIdentifierCodec instanceIdentifierCodec;
    private final SystemReadyMonitor systemReadyMonitor;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final OvsdbDiagStatusProvider ovsdbStatusProvider;

    private Registration registration;
    private Registration operTopologyRegistration;

    @Inject
    public SouthboundProvider(final DataBroker dataBroker,
                              final EntityOwnershipService entityOwnershipServiceDependency,
                              final OvsdbConnection ovsdbConnection,
                              final DOMSchemaService schemaService,
                              final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer,
                              final SystemReadyMonitor systemReadyMonitor,
                              final DiagStatusService diagStatusService) {
        this(dataBroker, entityOwnershipServiceDependency, ovsdbConnection, schemaService,
            bindingNormalizedNodeSerializer, systemReadyMonitor, diagStatusService, false, List.of(), List.of());
    }

    @Activate
    public SouthboundProvider(@Reference final DataBroker dataBroker,
                              @Reference final EntityOwnershipService entityOwnershipServiceDependency,
                              @Reference final OvsdbConnection ovsdbConnection,
                              @Reference final DOMSchemaService schemaService,
                              @Reference final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer,
                              @Reference final SystemReadyMonitor systemReadyMonitor,
                              @Reference final DiagStatusService diagStatusService,
                              final Configuration configuration) {
        this(dataBroker, entityOwnershipServiceDependency, ovsdbConnection, schemaService,
            bindingNormalizedNodeSerializer, systemReadyMonitor, diagStatusService,
            configuration.skip$_$monitoring$_$manager$_$status(),
            List.of(configuration.bridge$_$reconciliation$_$inclusion$_$list()),
            List.of(configuration.bridge$_$reconciliation$_$exclusion$_$list()));
    }

    @SuppressFBWarnings(
        value = { "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD" },
        justification = "This is not a final class due to deep mocking. There is a FIXME for the static wiring above")
    public SouthboundProvider(final DataBroker dataBroker,
                              final EntityOwnershipService entityOwnershipServiceDependency,
                              final OvsdbConnection ovsdbConnection,
                              final DOMSchemaService schemaService,
                              final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer,
                              final SystemReadyMonitor systemReadyMonitor,
                              final DiagStatusService diagStatusService,
                              final boolean skipMonitoringManagerStatus,
                              final List<String> bridgeReconciliationInclusions,
                              final List<String> bridgeReconciliationExclusions) {
        // FIXME: get rid of this static wiring
        db = dataBroker;
        reconcileBridgeInclusionList = bridgeReconciliationInclusions;
        reconcileBridgeExclusionList = bridgeReconciliationExclusions;
        LOG.debug("skipManagerStatus set to {}", skipMonitoringManagerStatus);
        if (skipMonitoringManagerStatus) {
            SouthboundConstants.SKIP_COLUMN_FROM_TABLE.get("Manager").add("status");
        } else {
            SouthboundConstants.SKIP_COLUMN_FROM_TABLE.get("Manager").remove("status");
        }

        entityOwnershipService = entityOwnershipServiceDependency;
        registration = null;
        this.ovsdbConnection = ovsdbConnection;
        ovsdbStatusProvider = new OvsdbDiagStatusProvider(diagStatusService);
        instanceIdentifierCodec = new InstanceIdentifierCodec(schemaService, bindingNormalizedNodeSerializer);
        this.systemReadyMonitor = systemReadyMonitor;
        LOG.info("SouthboundProvider ovsdbConnectionService Initialized");

        ovsdbStatusProvider.reportStatus(ServiceState.STARTING, "OVSDB initialization in progress");
        txInvoker = new TransactionInvokerImpl(dataBroker);
        cm = new OvsdbConnectionManager(dataBroker, txInvoker, entityOwnershipService, ovsdbConnection,
                instanceIdentifierCodec);
        ovsdbDataTreeChangeListener = new OvsdbDataTreeChangeListener(dataBroker, cm, instanceIdentifierCodec);
        ovsdbOperGlobalListener = new OvsdbOperGlobalListener(dataBroker, cm, txInvoker);

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
        }
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));
        DataTreeIdentifier<Topology> treeId = DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL, path);

        LOG.trace("Registering listener for path {}", treeId);
        operTopologyRegistration = dataBroker.registerTreeChangeListener(treeId, this);
        LOG.info("SouthboundProvider Session Initiated");
    }

    @PreDestroy
    @Deactivate
    @Override
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
                transaction.mergeParentStructurePut(type, path,
                    new TopologyBuilder().setTopologyId(SouthboundConstants.OVSDB_TOPOLOGY_ID).build());
                transaction.commit();
            } else {
                transaction.cancel();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error initializing ovsdb topology", e);
        }
    }

    public void handleOwnershipChange(final EntityOwnershipStateChange change) {
        if (change.isOwner()) {
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
    public void onDataTreeChanged(final List<DataTreeModification<Topology>> collection) {
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
        private final Registration listenerRegistration;

        SouthboundPluginInstanceEntityOwnershipListener(final SouthboundProvider sp,
                final EntityOwnershipService entityOwnershipService) {
            this.sp = sp;
            listenerRegistration = entityOwnershipService.registerListener(ENTITY_TYPE, this);
        }

        public void close() {
            listenerRegistration.close();
        }

        @Override
        public void ownershipChanged(final Entity entity, final EntityOwnershipStateChange change,
                final boolean inJeopardy) {
            sp.handleOwnershipChange(change);
        }
    }

    @VisibleForTesting
    boolean isRegistered() {
        return registered.get();
    }
}
