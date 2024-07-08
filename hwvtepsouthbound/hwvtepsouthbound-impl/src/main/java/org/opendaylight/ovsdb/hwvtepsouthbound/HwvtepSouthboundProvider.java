/*
 * Copyright © 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import com.google.common.util.concurrent.FluentFuture;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
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
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration.HwvtepReconciliationManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.utils.mdsal.utils.Scheduler;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionHistory;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = HwvtepSouthboundProviderInfo.class)
public final class HwvtepSouthboundProvider
        implements HwvtepSouthboundProviderInfo, DataTreeChangeListener<Topology>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundProvider.class);
    private static final String ENTITY_TYPE = "ovsdb-hwvtepsouthbound-provider";

    private final DataBroker dataBroker;
    private final EntityOwnershipService entityOwnershipService;
    private final OvsdbConnection ovsdbConnection;

    private HwvtepConnectionManager cm;
    private Registration registration;
    private HwvtepsbPluginInstanceEntityOwnershipListener providerOwnershipChangeListener;
    private HwvtepDataChangeListener hwvtepDTListener;
    private HwvtepReconciliationManager hwvtepReconciliationManager;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private Registration operTopologyRegistration;

    @Inject
    @Activate
    public HwvtepSouthboundProvider(@Reference final DataBroker dataBroker,
            @Reference final EntityOwnershipService entityOwnership,
            @Reference final OvsdbConnection ovsdbConnection, @Reference final DOMSchemaService schemaService,
            @Reference final BindingNormalizedNodeSerializer serializer,
            @Reference final TransactionInvoker txInvoker, @Reference final Operations ops) {
        this.dataBroker = dataBroker;
        entityOwnershipService = entityOwnership;
        registration = null;
        this.ovsdbConnection = ovsdbConnection;
        // FIXME: eliminate this static wiring
        HwvtepSouthboundUtil.setInstanceIdentifierCodec(new InstanceIdentifierCodec(schemaService, serializer));
        LOG.info("HwvtepSouthboundProvider ovsdbConnectionService: {}", ovsdbConnection);
        cm = new HwvtepConnectionManager(dataBroker, txInvoker, ops, entityOwnershipService, ovsdbConnection);
        hwvtepDTListener = new HwvtepDataChangeListener(dataBroker, cm);
        hwvtepReconciliationManager = new HwvtepReconciliationManager(dataBroker, cm);
        //Register listener for entityOnwership changes
        providerOwnershipChangeListener =
                new HwvtepsbPluginInstanceEntityOwnershipListener(this,entityOwnershipService);

        //register instance entity to get the ownership of the provider
        Entity instanceEntity = new Entity(ENTITY_TYPE, ENTITY_TYPE);
        try {
            registration = entityOwnershipService.registerCandidate(instanceEntity);
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.warn("HWVTEP Southbound Provider instance entity {} was already "
                    + "registered for ownership", instanceEntity, e);
        }
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID));
        DataTreeIdentifier<Topology> treeId = DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL, path);

        LOG.trace("Registering listener for path {}", treeId);
        operTopologyRegistration = dataBroker.registerTreeChangeListener(treeId, this);
        Scheduler.getScheduledExecutorService().schedule(() -> {
            if (!registered.get()) {
                openOvsdbPort();
                LOG.error("Timed out to get eos notification opening the port now");
            }
        }, HwvtepSouthboundConstants.PORT_OPEN_MAX_DELAY_IN_MINS, TimeUnit.MINUTES);

        LOG.info("HwvtepSouthboundProvider Session Initiated");
    }

    @PreDestroy
    @Deactivate
    @Override
    public void close() {
        if (cm != null) {
            cm.close();
            cm = null;
        }
        if (registration != null) {
            registration.close();
            registration = null;
        }
        if (providerOwnershipChangeListener != null) {
            providerOwnershipChangeListener.close();
            providerOwnershipChangeListener = null;
        }
        if (hwvtepDTListener != null) {
            hwvtepDTListener.close();
            hwvtepDTListener = null;
        }
        if (hwvtepReconciliationManager != null) {
            hwvtepReconciliationManager.close();
            hwvtepReconciliationManager = null;
        }
        if (operTopologyRegistration != null) {
            operTopologyRegistration.close();
            operTopologyRegistration = null;
        }
        LOG.info("HwvtepSouthboundProvider Closed");
    }

    private void initializeHwvtepTopology(final LogicalDatastoreType type) {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID));
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        FluentFuture<Boolean> hwvtepTp = transaction.exists(type, path);
        try {
            if (!hwvtepTp.get().booleanValue()) {
                TopologyBuilder tpb = new TopologyBuilder();
                tpb.setTopologyId(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
                transaction.mergeParentStructurePut(type, path, tpb.build());
                transaction.commit();
            } else {
                transaction.cancel();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error initializing hwvtep topology", e);
        }
    }

    public void handleOwnershipChange(final EntityOwnershipStateChange change) {
        if (change.isOwner()) {
            LOG.info("*This* instance of HWVTEP southbound provider is set as a MASTER instance");
            LOG.info("Initialize HWVTEP topology {} in operational and config data store if not already present",
                    HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
            initializeHwvtepTopology(LogicalDatastoreType.OPERATIONAL);
            initializeHwvtepTopology(LogicalDatastoreType.CONFIGURATION);
        } else {
            LOG.info("*This* instance of HWVTEP southbound provider is set as a SLAVE instance");
        }
    }

    @Override
    public void onDataTreeChanged(final List<DataTreeModification<Topology>> collection) {
        openOvsdbPort();

        if (operTopologyRegistration != null) {
            operTopologyRegistration.close();
            operTopologyRegistration = null;
        }
    }

    private void openOvsdbPort() {
        if (!registered.getAndSet(true)) {
            LOG.info("Starting the ovsdb port");
            ovsdbConnection.registerConnectionListener(cm);
            ovsdbConnection.startOvsdbManager();
        }
    }

    private static final class HwvtepsbPluginInstanceEntityOwnershipListener implements EntityOwnershipListener {
        private final HwvtepSouthboundProvider hsp;
        private final Registration listenerRegistration;

        HwvtepsbPluginInstanceEntityOwnershipListener(final HwvtepSouthboundProvider hsp,
                final EntityOwnershipService entityOwnershipService) {
            this.hsp = hsp;
            listenerRegistration = entityOwnershipService.registerListener(ENTITY_TYPE, this);
        }

        public void close() {
            listenerRegistration.close();
        }

        @Override
        public void ownershipChanged(final Entity entity, final EntityOwnershipStateChange change,
                final boolean inJeopardy) {
            hsp.handleOwnershipChange(change);
        }
    }

    @Override
    public Map<InstanceIdentifier<Node>, HwvtepDeviceInfo> getAllConnectedInstances() {
        return cm.allConnectedInstances();
    }

    @Override
    public Map<InstanceIdentifier<Node>, TransactionHistory> getControllerTxHistory() {
        return cm.controllerTxHistory();
    }

    @Override
    public Map<InstanceIdentifier<Node>, TransactionHistory> getDeviceUpdateHistory() {
        return cm.deviceUpdateHistory();
    }
}
