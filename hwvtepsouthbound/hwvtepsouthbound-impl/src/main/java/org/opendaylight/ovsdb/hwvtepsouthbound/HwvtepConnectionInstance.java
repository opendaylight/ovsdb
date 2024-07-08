/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactCommand;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactInvoker;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactInvokerImpl;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.lib.LockAquisitionCallback;
import org.opendaylight.ovsdb.lib.LockStolenCallback;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.MonitorHandle;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.message.MonitorRequest;
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.MonitorSelect;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionHistory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepConnectionInstance {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepConnectionInstance.class);
    private ConnectionInfo connectionInfo;
    private final OvsdbClient client;
    private final HwvtepTableReader hwvtepTableReader;
    private InstanceIdentifier<Node> instanceIdentifier;
    private final TransactionInvoker txInvoker;
    private Map<TypedDatabaseSchema, TransactInvoker> transactInvokers;
    private MonitorCallBack callback;
    private volatile boolean hasDeviceOwnership = false;
    private Entity connectedEntity;
    private Registration deviceOwnershipCandidateRegistration;
    private HwvtepGlobalAugmentation initialCreatedData = null;
    private final HwvtepDeviceInfo deviceInfo;
    private final DataBroker dataBroker;
    private final HwvtepConnectionManager hwvtepConnectionManager;
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors
        .newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
        .setNameFormat("HwvtepReconciliationFT-%d").build());
    @VisibleForTesting
    final SettableFuture<Boolean> reconciliationFt = SettableFuture.create();
    @VisibleForTesting
    final AtomicBoolean firstUpdateTriggered = new AtomicBoolean(false);
    private TransactionHistory controllerTxHistory;
    private TransactionHistory deviceUpdateHistory;

    HwvtepConnectionInstance(final HwvtepConnectionManager hwvtepConnectionManager, final ConnectionInfo key,
            final OvsdbClient client, final InstanceIdentifier<Node> iid, final TransactionInvoker txInvoker,
            final DataBroker dataBroker) {
        this.hwvtepConnectionManager = hwvtepConnectionManager;
        this.connectionInfo = key;
        this.client = client;
        this.instanceIdentifier = iid;
        this.txInvoker = txInvoker;
        this.deviceInfo = new HwvtepDeviceInfo(this);
        this.dataBroker = dataBroker;
        this.hwvtepTableReader = new HwvtepTableReader(this);
    }

    public void transact(final TransactCommand command) {
        String nodeId = getNodeId().getValue();
        boolean firstUpdate = firstUpdateTriggered.compareAndSet(false, true);
        if (reconciliationFt.isDone()) {
            transact(command, false);
        } else {
            LOG.info("Job waiting for reconciliation {}", nodeId);
            Futures.addCallback(reconciliationFt, new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(final Boolean notUsed) {
                    LOG.info("Running the job waiting for reconciliation {}", nodeId);
                    transact(command, false);
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOG.info("Running the job waiting for reconciliation {}", nodeId);
                    transact(command, false);
                }
            }, MoreExecutors.directExecutor());

            if (firstUpdate) {
                LOG.info("Scheduling the reconciliation timeout task {}", nodeId);
                SCHEDULED_EXECUTOR_SERVICE.schedule(() -> reconciliationFt.set(Boolean.TRUE),
                        HwvtepSouthboundConstants.CONFIG_NODE_UPDATE_MAX_DELAY_MS, TimeUnit.MILLISECONDS);
            }
        }
    }

    public synchronized void transact(final TransactCommand command, final boolean reconcile) {
        try {
            for (TransactInvoker transactInvoker : transactInvokers.values()) {
                transactInvoker.invoke(command);
            }
        } finally {
            if (reconcile) {
                reconciliationFt.set(Boolean.TRUE);
            }
        }
    }

    public ListenableFuture<List<OperationResult>> transact(final DatabaseSchema dbSchema,
            final List<Operation> operations) {
        return client.transact(dbSchema, operations);
    }

    public void registerCallbacks() {
        if (this.callback == null) {
            if (this.initialCreatedData != null) {
                this.updateConnectionAttributes();
            }

            try {
                String database = HwvtepSchemaConstants.HARDWARE_VTEP;
                DatabaseSchema dbSchema = getSchema(database).get();
                if (dbSchema != null) {
                    LOG.info("Monitoring database: {}", database);
                    callback = new HwvtepMonitorCallback(this, txInvoker);
                    monitorAllTables(database, dbSchema);
                } else {
                    LOG.info("No database {} found on {}", database, connectionInfo);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Exception attempting to registerCallbacks {}: ", connectionInfo, e);
            }
        }
    }

    public void createTransactInvokers() {
        if (transactInvokers == null) {
            try {
                transactInvokers = new HashMap<>();
                TypedDatabaseSchema dbSchema = getSchema(HwvtepSchemaConstants.HARDWARE_VTEP).get();
                if (dbSchema != null) {
                    transactInvokers.put(dbSchema, new TransactInvokerImpl(this, dbSchema));
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Exception attempting to createTransactionInvokers {}", connectionInfo, e);
            }
        }
    }

    private void monitorAllTables(final String database, final DatabaseSchema dbSchema) {
        Set<String> tables = dbSchema.getTables();
        if (tables != null) {
            List<MonitorRequest> monitorRequests = new ArrayList<>();
            for (String tableName : tables) {
                if (!HwvtepSouthboundConstants.SKIP_HWVTEP_TABLE.containsKey(tableName)) {
                    LOG.info("HwvtepSouthbound monitoring Hwvtep schema table {}", tableName);
                    GenericTableSchema tableSchema = dbSchema.table(tableName, GenericTableSchema.class);
                    final Set<String> columns = new HashSet<>(tableSchema.getColumns());
                    List<String> skipColumns = HwvtepSouthboundConstants.SKIP_COLUMN_FROM_HWVTEP_TABLE.get(tableName);
                    skipColumns = skipColumns == null ? new ArrayList<>() : new ArrayList<>(skipColumns);
                    skipColumns.add(HwvtepSouthboundConstants.VERSION_COLUMN);

                    LOG.info("HwvtepSouthbound NOT monitoring columns {} in table {}", skipColumns, tableName);
                    columns.removeAll(skipColumns);

                    monitorRequests.add(new MonitorRequestBuilder<>(tableSchema)
                            .addColumns(columns)
                            .with(new MonitorSelect(true, true, true, true)).build());
                }
            }
            this.callback.update(monitor(dbSchema, monitorRequests, callback), dbSchema);
        } else {
            LOG.warn("No tables for schema {} for database {} for key {}",dbSchema,database,connectionInfo);
        }
    }

    private void updateConnectionAttributes() {
        LOG.debug("Update attributes of ovsdb node ip: {} port: {}",
                    this.initialCreatedData.getConnectionInfo().getRemoteIp(),
                    this.initialCreatedData.getConnectionInfo().getRemotePort());
        /*
         * TODO: Do we have anything to update?
         * Hwvtep doesn't have other_config or external_ids like
         * Open_vSwitch. What else will be needed?
         */
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public ListenableFuture<List<String>> getDatabases() {
        return client.getDatabases();
    }

    public ListenableFuture<TypedDatabaseSchema> getSchema(final String database) {
        return client.getSchema(database);
    }

    public TransactionBuilder transactBuilder(final DatabaseSchema dbSchema) {
        return client.transactBuilder(dbSchema);
    }

    public <E extends TableSchema<E>> TableUpdates monitor(final DatabaseSchema schema,
                    final List<MonitorRequest> monitorRequests, final MonitorCallBack monitorCallBack) {
        return client.monitor(schema, monitorRequests, monitorCallBack);
    }

    public <E extends TableSchema<E>> TableUpdates monitor(final DatabaseSchema schema,
            final List<MonitorRequest> monitorRequests, final MonitorHandle monitorHandle,
            final MonitorCallBack monitorCallBack) {
        return null;
    }

    public void cancelMonitor(final MonitorHandle handler) {
        client.cancelMonitor(handler);
    }

    public void lock(final String lockId, final LockAquisitionCallback lockedCallBack,
            final LockStolenCallback stolenCallback) {
        client.lock(lockId, lockedCallBack, stolenCallback);
    }

    public ListenableFuture<Boolean> steal(final String lockId) {
        return client.steal(lockId);
    }

    public ListenableFuture<Boolean> unLock(final String lockId) {
        return client.unLock(lockId);
    }

    public OvsdbConnectionInfo getConnectionInfo() {
        return client.getConnectionInfo();
    }

    public boolean isActive() {
        return client.isActive();
    }

    public void disconnect() {
        client.disconnect();
    }

    public DatabaseSchema getDatabaseSchema(final String dbName) {
        return client.getDatabaseSchema(dbName);
    }

    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(final Class<T> klazz) {
        return client.createTypedRowWrapper(klazz);
    }

    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(final DatabaseSchema dbSchema, final Class<T> klazz) {
        return client.createTypedRowWrapper(dbSchema, klazz);
    }

    public <T extends TypedBaseTable<?>> T getTypedRowWrapper(final Class<T> klazz, final Row<GenericTableSchema> row) {
        return client.getTypedRowWrapper(klazz, row);
    }

    public ConnectionInfo getMDConnectionInfo() {
        return connectionInfo;
    }

    public void setMDConnectionInfo(final ConnectionInfo key) {
        this.connectionInfo = key;
    }

    public InstanceIdentifier<Node> getInstanceIdentifier() {
        return instanceIdentifier;
    }

    public NodeKey getNodeKey() {
        //TODO: What is the alternative here?
        return getInstanceIdentifier().firstKeyOf(Node.class);
    }

    public NodeId getNodeId() {
        return getNodeKey().getNodeId();
    }

    public void setInstanceIdentifier(final InstanceIdentifier<Node> iid) {
        this.instanceIdentifier = iid;
        hwvtepConnectionManager.putConnectionInstance(instanceIdentifier, this);
    }

    public Entity getConnectedEntity() {
        return this.connectedEntity;
    }

    public void setConnectedEntity(final Entity entity) {
        this.connectedEntity = entity;
    }

    public Boolean hasOvsdbClient(final OvsdbClient otherClient) {
        return client.equals(otherClient);
    }

    public Boolean getHasDeviceOwnership() {
        return hasDeviceOwnership;
    }

    public void setHasDeviceOwnership(final Boolean hasDeviceOwnership) {
        if (hasDeviceOwnership != null) {
            if (hasDeviceOwnership != this.hasDeviceOwnership) {
                LOG.info("Ownership status changed for {} old {} new {}", instanceIdentifier,
                        this.hasDeviceOwnership, hasDeviceOwnership);
            }
            this.hasDeviceOwnership = hasDeviceOwnership;
        }
    }

    public void setDeviceOwnershipCandidateRegistration(final @NonNull Registration registration) {
        this.deviceOwnershipCandidateRegistration = registration;
    }

    public void closeDeviceOwnershipCandidateRegistration() {
        if (deviceOwnershipCandidateRegistration != null) {
            this.deviceOwnershipCandidateRegistration.close();
            setHasDeviceOwnership(Boolean.FALSE);
        }
    }

    public void setHwvtepGlobalAugmentation(final HwvtepGlobalAugmentation hwvtepGlobalData) {
        this.initialCreatedData = hwvtepGlobalData;
    }

    public HwvtepGlobalAugmentation getHwvtepGlobalAugmentation() {
        return this.initialCreatedData;
    }

    public HwvtepDeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    public OvsdbClient getOvsdbClient() {
        return client;
    }

    public HwvtepTableReader getHwvtepTableReader() {
        return hwvtepTableReader;
    }

    public void refreshOperNode() throws ExecutionException, InterruptedException {
        TableUpdates tableUpdates = hwvtepTableReader.readAllTables();
        callback.update(tableUpdates, getDatabaseSchema(HwvtepSchemaConstants.HARDWARE_VTEP));
    }

    public MonitorCallBack getCallback() {
        return callback;
    }

    public void setCallback(final MonitorCallBack callback) {
        this.callback = callback;
    }

    public TransactionHistory getControllerTxHistory() {
        return controllerTxHistory;
    }

    public void setControllerTxHistory(final TransactionHistory controllerTxLog) {
        deviceInfo.setControllerTxHistory(controllerTxLog);
        this.controllerTxHistory = controllerTxLog;
    }

    public TransactionHistory getDeviceUpdateHistory() {
        return deviceUpdateHistory;
    }

    public void setDeviceUpdateHistory(final TransactionHistory deviceUpdateLog) {
        deviceInfo.setDeviceUpdateHistory(deviceUpdateLog);
        this.deviceUpdateHistory = deviceUpdateLog;
    }

    public TransactionInvoker getTxInvoker() {
        return txInvoker;
    }

    public Operations ops() {
        // FIXME: properly inject
        return null;
    }
}
