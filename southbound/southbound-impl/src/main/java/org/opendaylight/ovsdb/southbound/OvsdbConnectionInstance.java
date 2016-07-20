/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
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
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.BridgeOperationalState;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactCommand;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactInvoker;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactInvokerImpl;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactUtils;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.utils.yang.YangUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbConnectionInstance {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionInstance.class);
    private OvsdbClient client;
    private ConnectionInfo connectionInfo;
    private TransactionInvoker txInvoker;
    private Map<DatabaseSchema,TransactInvoker> transactInvokers;
    private MonitorCallBack callback;
    private InstanceIdentifier<Node> instanceIdentifier;
    private volatile boolean hasDeviceOwnership = false;
    private Entity connectedEntity;
    private EntityOwnershipCandidateRegistration deviceOwnershipCandidateRegistration;
    private OvsdbNodeAugmentation initialCreateData = null;

    OvsdbConnectionInstance(ConnectionInfo key, OvsdbClient client, TransactionInvoker txInvoker,
                            InstanceIdentifier<Node> iid) {
        this.connectionInfo = key;
        this.client = client;
        this.txInvoker = txInvoker;
        // this.key = key;
        this.instanceIdentifier = iid;
    }

    /**
     * Apply the given command to the given events, based on the given bridge state.
     *
     * @param command The command to run.
     * @param state The current bridge state.
     * @param events The events to process.
     */
    public void transact(TransactCommand command, BridgeOperationalState state,
                 AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events) {
        for (TransactInvoker transactInvoker : transactInvokers.values()) {
            transactInvoker.invoke(command, state, events);
        }
    }

    /**
     * Apply the given command to the given modifications, based on the given bridge state.
     *
     * @param command The command to run.
     * @param state The current bridge state.
     * @param modifications The modifications to process.
     */
    public void transact(TransactCommand command, BridgeOperationalState state,
                 Collection<DataTreeModification<Node>> modifications) {
        for (TransactInvoker transactInvoker : transactInvokers.values()) {
            transactInvoker.invoke(command, state, modifications);
        }
    }

    public ListenableFuture<List<OperationResult>> transact(
            DatabaseSchema dbSchema, List<Operation> operations) {
        return client.transact(dbSchema, operations);
    }

    public void registerCallbacks() {
        if (this.callback == null) {
            if (this.initialCreateData != null ) {
                this.updateConnectionAttributes();
            }

            try {
                String database = SouthboundConstants.OPEN_V_SWITCH;
                DatabaseSchema dbSchema = getSchema(database).get();
                if (dbSchema != null) {
                    LOG.info("Monitoring database: {}", database);
                    callback = new OvsdbMonitorCallback(this, txInvoker);
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
                DatabaseSchema dbSchema = getSchema(SouthboundConstants.OPEN_V_SWITCH).get();
                if (dbSchema != null) {
                    transactInvokers.put(dbSchema, new TransactInvokerImpl(this,dbSchema));
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Exception attempting to createTransactionInvokers {}", connectionInfo, e);
            }
        }
    }

    private void monitorAllTables(String database, DatabaseSchema dbSchema) {
        Set<String> tables = dbSchema.getTables();
        if (tables != null) {
            List<MonitorRequest> monitorRequests = Lists.newArrayList();
            for (String tableName : tables) {
                LOG.info("Southbound monitoring table {} in {}", tableName, dbSchema.getName());
                GenericTableSchema tableSchema = dbSchema.table(tableName, GenericTableSchema.class);
                Set<String> columns = tableSchema.getColumns();
                MonitorRequestBuilder<GenericTableSchema> monitorBuilder = MonitorRequestBuilder.builder(tableSchema);
                for (String column : columns) {
                    monitorBuilder.addColumn(column);
                }
                monitorRequests.add(monitorBuilder.with(new MonitorSelect(true, true, true, true)).build());
            }
            this.callback.update(monitor(dbSchema, monitorRequests, callback), dbSchema);
        } else {
            LOG.warn("No tables for schema {} for database {} for key {}",dbSchema,database,connectionInfo);
        }
    }

    private void updateConnectionAttributes() {
        LOG.debug("Update attributes of ovsdb node ip: {} port: {}",
                    this.initialCreateData.getConnectionInfo().getRemoteIp(),
                    this.initialCreateData.getConnectionInfo().getRemotePort());
        for ( Map.Entry<DatabaseSchema,TransactInvoker> entry: transactInvokers.entrySet()) {

            TransactionBuilder transaction = new TransactionBuilder(this.client, entry.getKey());

            // OpenVSwitchPart
            OpenVSwitch ovs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), OpenVSwitch.class);

            List<OpenvswitchExternalIds> externalIds = this.initialCreateData.getOpenvswitchExternalIds();

            stampInstanceIdentifier(transaction,this.instanceIdentifier.firstIdentifierOf(Node.class));

            try {
                ovs.setExternalIds(
                        YangUtils.convertYangKeyValueListToMap(externalIds, OpenvswitchExternalIds::getExternalIdKey,
                                OpenvswitchExternalIds::getExternalIdValue));
                Mutate<GenericTableSchema> mutate = op.mutate(ovs)
                            .addMutation(ovs.getExternalIdsColumn().getSchema(),
                                Mutator.INSERT,
                                ovs.getExternalIdsColumn().getData());
                transaction.add(mutate);
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB Node external IDs", e);
            }



            List<OpenvswitchOtherConfigs> otherConfigs = this.initialCreateData.getOpenvswitchOtherConfigs();
            if (otherConfigs != null) {
                try {
                    ovs.setOtherConfig(YangUtils.convertYangKeyValueListToMap(otherConfigs,
                            OpenvswitchOtherConfigs::getOtherConfigKey,
                            OpenvswitchOtherConfigs::getOtherConfigValue));
                    transaction.add(op.mutate(ovs).addMutation(ovs.getOtherConfigColumn().getSchema(),
                        Mutator.INSERT,
                        ovs.getOtherConfigColumn().getData()));
                } catch (NullPointerException e) {
                    LOG.warn("Incomplete OVSDB Node other_config", e);
                }
            }

            invoke(transaction);
        }
    }

    private void stampInstanceIdentifier(TransactionBuilder transaction,InstanceIdentifier<Node> iid) {
        OpenVSwitch ovs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), OpenVSwitch.class);
        ovs.setExternalIds(Collections.<String,String>emptyMap());
        TransactUtils.stampInstanceIdentifier(transaction,
                iid,
                ovs.getSchema(),
                ovs.getExternalIdsColumn().getSchema());
    }

    private void invoke(TransactionBuilder txBuilder) {
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

    public ListenableFuture<DatabaseSchema> getSchema(String database) {
        return client.getSchema(database);
    }

    public TransactionBuilder transactBuilder(DatabaseSchema dbSchema) {
        return client.transactBuilder(dbSchema);
    }

    public <E extends TableSchema<E>> TableUpdates monitor(
            DatabaseSchema schema, List<MonitorRequest> monitorRequests,
            MonitorHandle monitorHandle, MonitorCallBack callbackArgument) {
        return null;
    }

    public <E extends TableSchema<E>> TableUpdates monitor(
            DatabaseSchema schema, List<MonitorRequest> monitorRequests,
            MonitorCallBack callbackArgument) {
        return client.monitor(schema, monitorRequests, callbackArgument);
    }

    public void cancelMonitor(MonitorHandle handler) {
        client.cancelMonitor(handler);
    }

    public void lock(String lockId, LockAquisitionCallback lockedCallBack,
            LockStolenCallback stolenCallback) {
        client.lock(lockId, lockedCallBack, stolenCallback);
    }

    public ListenableFuture<Boolean> steal(String lockId) {
        return client.steal(lockId);
    }

    public ListenableFuture<Boolean> unLock(String lockId) {
        return client.unLock(lockId);
    }

    public boolean isActive() {
        return client.isActive();
    }

    public void disconnect() {
        client.disconnect();
    }

    public DatabaseSchema getDatabaseSchema(String dbName) {
        return client.getDatabaseSchema(dbName);
    }

    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(Class<T> klazz) {
        return client.createTypedRowWrapper(klazz);
    }

    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(
            DatabaseSchema dbSchema, Class<T> klazz) {
        return client.createTypedRowWrapper(dbSchema, klazz);
    }

    public <T extends TypedBaseTable<?>> T getTypedRowWrapper(Class<T> klazz,
            Row<GenericTableSchema> row) {
        return client.getTypedRowWrapper(klazz, row);
    }

    public OvsdbConnectionInfo getConnectionInfo() {
        return client.getConnectionInfo();
    }

    public ConnectionInfo getMDConnectionInfo() {
        return connectionInfo;
    }

    public void setMDConnectionInfo(ConnectionInfo key) {
        this.connectionInfo = key;
    }

    public InstanceIdentifier<Node> getInstanceIdentifier() {
        return instanceIdentifier;
    }

    public NodeKey getNodeKey() {
        return getInstanceIdentifier().firstKeyOf(Node.class, NodeKey.class);
    }

    public NodeId getNodeId() {
        return getNodeKey().getNodeId();
    }

    public void setInstanceIdentifier(InstanceIdentifier<Node> iid) {
        this.instanceIdentifier = iid;
    }

    public Entity getConnectedEntity() {
        return this.connectedEntity;
    }

    public void setConnectedEntity(Entity entity ) {
        this.connectedEntity = entity;
    }

    public Boolean hasOvsdbClient(OvsdbClient otherClient) {
        return client.equals(otherClient);
    }

    public Boolean getHasDeviceOwnership() {
        return hasDeviceOwnership;
    }

    public void setHasDeviceOwnership(Boolean hasDeviceOwnership) {
        if (hasDeviceOwnership != null) {
            this.hasDeviceOwnership = hasDeviceOwnership;
        }
    }

    public void setDeviceOwnershipCandidateRegistration(@Nonnull EntityOwnershipCandidateRegistration registration) {
        this.deviceOwnershipCandidateRegistration = registration;
    }

    public void closeDeviceOwnershipCandidateRegistration() {
        if (deviceOwnershipCandidateRegistration != null) {
            this.deviceOwnershipCandidateRegistration.close();
            setHasDeviceOwnership(Boolean.FALSE);
        }
    }

    public OvsdbNodeAugmentation getOvsdbNodeAugmentation() {
        return this.initialCreateData;
    }

    public void setOvsdbNodeAugmentation(OvsdbNodeAugmentation ovsdbNodeCreateData) {
        this.initialCreateData = ovsdbNodeCreateData;
    }

    public OvsdbClient getOvsdbClient() {
        return client;
    }
}
