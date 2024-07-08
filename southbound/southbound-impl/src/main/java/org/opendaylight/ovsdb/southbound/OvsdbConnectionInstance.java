/*
 * Copyright © 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.eos.binding.api.Entity;
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
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.BridgeOperationalState;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.DataChangeEvent;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactCommand;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactInvoker;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactInvokerImpl;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactUtils;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.utils.yang.YangUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbConnectionInstance {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionInstance.class);

    private static final ImmutableSet<String> SKIP_OVSDB_TABLE = ImmutableSet.of(
            "Flow_Table",
            "Mirror",
            "NetFlow",
            "sFlow",
            "IPFIX",
            "Flow_Sample_Collector_Set");

    private final OvsdbClient client;
    private ConnectionInfo connectionInfo;
    private final TransactionInvoker txInvoker;
    private Map<TypedDatabaseSchema, TransactInvoker> transactInvokers = null;
    private MonitorCallBack callback = null;
    private InstanceIdentifier<Node> instanceIdentifier;
    private volatile boolean hasDeviceOwnership = false;
    private Entity connectedEntity;
    private Registration deviceOwnershipCandidateRegistration;
    private OvsdbNodeAugmentation initialCreateData = null;
    private final Map<UUID, InstanceIdentifier<Node>> ports = new ConcurrentHashMap<>();
    private final Map<String, InstanceIdentifier<Node>> portInterfaces = new ConcurrentHashMap<>();

    OvsdbConnectionInstance(final ConnectionInfo key, final OvsdbClient client, final TransactionInvoker txInvoker,
                            final InstanceIdentifier<Node> iid) {
        connectionInfo = key;
        this.client = client;
        this.txInvoker = txInvoker;
        // this.key = key;
        instanceIdentifier = iid;
    }

    public void updatePort(final UUID uuid, final InstanceIdentifier<Node> iid) {
        ports.put(uuid, iid);
    }

    public void removePort(final UUID uuid) {
        ports.remove(uuid);
    }

    public InstanceIdentifier<Node> getPort(final UUID uuid) {
        return ports.get(uuid);
    }

    public void updatePortInterface(final String name, final InstanceIdentifier<Node> iid) {
        portInterfaces.put(name, iid);
    }

    public void removePortInterface(final String name) {
        portInterfaces.remove(name);
    }

    public InstanceIdentifier<Node> getPortInterface(final String name) {
        return portInterfaces.get(name);
    }

    /**
     * Apply the given command to the given events, based on the given bridge state.
     *
     * @param command The command to run.
     * @param state The current bridge state.
     * @param events The events to process.
     * @param instanceIdentifierCodec The instance identifier codec to use.
     */
    public void transact(final TransactCommand command, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        for (TransactInvoker transactInvoker : transactInvokers.values()) {
            transactInvoker.invoke(command, state, events, instanceIdentifierCodec);
        }
    }

    /**
     * Apply the given command to the given modifications, based on the given bridge state.
     *
     * @param command The command to run.
     * @param state The current bridge state.
     * @param modifications The modifications to process.
     * @param instanceIdentifierCodec The instance identifier codec to use.
     */
    public void transact(final TransactCommand command, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        for (TransactInvoker transactInvoker : transactInvokers.values()) {
            transactInvoker.invoke(command, state, modifications, instanceIdentifierCodec);
        }
    }

    public ListenableFuture<List<OperationResult>> transact(
            final DatabaseSchema dbSchema, final List<Operation> operations) {
        return client.transact(dbSchema, operations);
    }

    public void registerCallbacks(final InstanceIdentifierCodec instanceIdentifierCodec) {
        if (callback == null) {
            if (initialCreateData != null) {
                this.updateConnectionAttributes(instanceIdentifierCodec);
            }

            try {
                String database = SouthboundConstants.OPEN_V_SWITCH;
                DatabaseSchema dbSchema = getSchema(database).get();
                if (dbSchema != null) {
                    LOG.info("Monitoring database: {}", database);
                    callback = new OvsdbMonitorCallback(instanceIdentifierCodec, this, txInvoker);
                    monitorTables(database, dbSchema);
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
                TypedDatabaseSchema dbSchema = getSchema(SouthboundConstants.OPEN_V_SWITCH).get();
                if (dbSchema != null) {
                    transactInvokers.put(dbSchema, new TransactInvokerImpl(this,dbSchema));
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Exception attempting to createTransactionInvokers {}", connectionInfo, e);
            }
        }
    }

    @VisibleForTesting
    void monitorTables(final String database, final DatabaseSchema dbSchema) {
        Set<String> tables = dbSchema.getTables();
        if (tables != null) {
            List<MonitorRequest> monitorRequests = new ArrayList<>();
            for (String tableName : tables) {
                if (!SKIP_OVSDB_TABLE.contains(tableName)) {
                    LOG.trace("Southbound monitoring OVSDB schema table {}", tableName);
                    GenericTableSchema tableSchema = dbSchema.table(tableName, GenericTableSchema.class);
                    // We copy the columns so we can clean the set up later
                    Set<String> columns = new HashSet<>(tableSchema.getColumns());
                    List<String> skipColumns = SouthboundConstants.SKIP_COLUMN_FROM_TABLE.get(tableName);
                    if (skipColumns != null) {
                        LOG.trace("Southbound NOT monitoring columns {} in table {}", skipColumns, tableName);
                        columns.removeAll(skipColumns);
                    }
                    monitorRequests.add(new MonitorRequestBuilder<>(tableSchema)
                            .addColumns(columns)
                            .with(new MonitorSelect(true, true, true, true)).build());
                }
            }
            callback.update(monitor(dbSchema, monitorRequests, callback), dbSchema);
        } else {
            LOG.warn("No tables for schema {} for database {} for key {}",dbSchema,database,connectionInfo);
        }
    }

    private void updateConnectionAttributes(final InstanceIdentifierCodec instanceIdentifierCodec) {
        LOG.debug("Update attributes of ovsdb node ip: {} port: {}",
                    initialCreateData.getConnectionInfo().getRemoteIp(),
                    initialCreateData.getConnectionInfo().getRemotePort());
        for (Map.Entry<TypedDatabaseSchema, TransactInvoker> entry: transactInvokers.entrySet()) {

            TransactionBuilder transaction = new TransactionBuilder(client, entry.getKey());

            // OpenVSwitchPart
            OpenVSwitch ovs = transaction.getTypedRowWrapper(OpenVSwitch.class);

            Map<OpenvswitchExternalIdsKey, OpenvswitchExternalIds> externalIds =
                    initialCreateData.getOpenvswitchExternalIds();

            stampInstanceIdentifier(transaction, instanceIdentifier.firstIdentifierOf(Node.class),
                    instanceIdentifierCodec);

            ovs.setExternalIds(
                YangUtils.convertYangKeyValueListToMap(externalIds, OpenvswitchExternalIds::requireExternalIdKey,
                    OpenvswitchExternalIds::requireExternalIdValue));
            Mutate<GenericTableSchema> mutate = op.mutate(ovs)
                .addMutation(ovs.getExternalIdsColumn().getSchema(),
                    Mutator.INSERT, ovs.getExternalIdsColumn().getData());
            transaction.add(mutate);

            Map<OpenvswitchOtherConfigsKey, OpenvswitchOtherConfigs> otherConfigs =
                    initialCreateData.getOpenvswitchOtherConfigs();
            if (otherConfigs != null) {
                ovs.setOtherConfig(YangUtils.convertYangKeyValueListToMap(otherConfigs,
                    OpenvswitchOtherConfigs::requireOtherConfigKey, OpenvswitchOtherConfigs::requireOtherConfigValue));
                transaction.add(op.mutate(ovs).addMutation(ovs.getOtherConfigColumn().getSchema(),
                    Mutator.INSERT, ovs.getOtherConfigColumn().getData()));
            }

            invoke(transaction);
        }
    }

    private static void stampInstanceIdentifier(final TransactionBuilder transaction,final InstanceIdentifier<Node> iid,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        OpenVSwitch ovs = transaction.getTypedRowWrapper(OpenVSwitch.class);
        ovs.setExternalIds(Collections.emptyMap());
        TransactUtils.stampInstanceIdentifier(transaction, iid, ovs.getSchema(), ovs.getExternalIdsColumn().getSchema(),
                instanceIdentifierCodec);
    }

    private static void invoke(final TransactionBuilder txBuilder) {
        ListenableFuture<List<OperationResult>> result = txBuilder.execute();
        LOG.debug("invoke: tb: {}", txBuilder);
        if (txBuilder.getOperations().size() > 0) {
            try {
                List<OperationResult> got = result.get();
                LOG.debug("OVSDB transaction result: {}", got);
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Transact execution exception: ", e);
            }
            LOG.trace("invoke exit tb: {}", txBuilder);
        }
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

    public <E extends TableSchema<E>> TableUpdates monitor(
            final DatabaseSchema schema, final List<MonitorRequest> monitorRequests,
            final MonitorHandle monitorHandle, final MonitorCallBack callbackArgument) {
        return null;
    }

    public <E extends TableSchema<E>> TableUpdates monitor(
            final DatabaseSchema schema, final List<MonitorRequest> monitorRequests,
            final MonitorCallBack callbackArgument) {
        return client.monitor(schema, monitorRequests, callbackArgument);
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

    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(
            final DatabaseSchema dbSchema, final Class<T> klazz) {
        return client.createTypedRowWrapper(dbSchema, klazz);
    }

    public <T extends TypedBaseTable<?>> T getTypedRowWrapper(final Class<T> klazz,
            final Row<GenericTableSchema> row) {
        return client.getTypedRowWrapper(klazz, row);
    }

    public OvsdbConnectionInfo getConnectionInfo() {
        return client.getConnectionInfo();
    }

    public ConnectionInfo getMDConnectionInfo() {
        return connectionInfo;
    }

    public void setMDConnectionInfo(final ConnectionInfo key) {
        connectionInfo = key;
    }

    public InstanceIdentifier<Node> getInstanceIdentifier() {
        return instanceIdentifier;
    }

    public NodeKey getNodeKey() {
        return getInstanceIdentifier().firstKeyOf(Node.class);
    }

    public NodeId getNodeId() {
        return getNodeKey().getNodeId();
    }

    public void setInstanceIdentifier(final InstanceIdentifier<Node> iid) {
        instanceIdentifier = iid;
    }

    public Entity getConnectedEntity() {
        return connectedEntity;
    }

    public void setConnectedEntity(final Entity entity) {
        connectedEntity = entity;
    }

    public Boolean hasOvsdbClient(final OvsdbClient otherClient) {
        return client.equals(otherClient);
    }

    public Boolean getHasDeviceOwnership() {
        return hasDeviceOwnership;
    }

    public void setHasDeviceOwnership(final Boolean hasDeviceOwnership) {
        if (hasDeviceOwnership != null) {
            LOG.debug("Ownership status for {} old {} new {}",
                    instanceIdentifier, this.hasDeviceOwnership, hasDeviceOwnership);
            this.hasDeviceOwnership = hasDeviceOwnership;
        }
    }

    public void setDeviceOwnershipCandidateRegistration(final @NonNull Registration registration) {
        deviceOwnershipCandidateRegistration = registration;
    }

    public void closeDeviceOwnershipCandidateRegistration() {
        if (deviceOwnershipCandidateRegistration != null) {
            deviceOwnershipCandidateRegistration.close();
            setHasDeviceOwnership(Boolean.FALSE);
        }
    }

    public OvsdbNodeAugmentation getOvsdbNodeAugmentation() {
        return initialCreateData;
    }

    public void setOvsdbNodeAugmentation(final OvsdbNodeAugmentation ovsdbNodeCreateData) {
        initialCreateData = ovsdbNodeCreateData;
    }

    public OvsdbClient getOvsdbClient() {
        return client;
    }

}
