/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.ovsdb.lib.EchoServiceCallbackFilters;
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
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactCommand;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactInvoker;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactInvokerImpl;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

public class OvsdbConnectionInstance implements OvsdbClient {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionInstance.class);
    private OvsdbClient client;
    private ConnectionInfo connectionInfo;
    private TransactionInvoker txInvoker;
    private Map<DatabaseSchema,TransactInvoker> transactInvokers;
    private MonitorCallBack callback;
    private ConnectionInfo key;
    private InstanceIdentifier<Node> instanceIdentifier;

    OvsdbConnectionInstance(ConnectionInfo key,OvsdbClient client,TransactionInvoker txInvoker,
            InstanceIdentifier<Node> iid) {
        this.connectionInfo = key;
        this.client = client;
        this.txInvoker = txInvoker;
        this.key = key;
        this.instanceIdentifier = iid;
    }

    public void transact(TransactCommand command) {
        for (TransactInvoker transactInvoker: transactInvokers.values()) {
            transactInvoker.invoke(command);
        }
    }

    public void registerCallbacks() {
        if ( this.callback == null) {
            try {
                List<String> databases = getDatabases().get();
                this.callback = new OvsdbMonitorCallback(this,txInvoker);
                for (String database : databases) {
                    DatabaseSchema dbSchema = getSchema(database).get();
                    if (dbSchema != null) {
                        monitorAllTables(database, dbSchema);
                    } else {
                        LOG.warn("No schema reported for database {} for key {}",database,connectionInfo);
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Exception attempting to registerCallbacks {}: {}",connectionInfo,e);
            }
        }
    }

    public void createTransactInvokers() {
        if (transactInvokers == null) {
            try {
                transactInvokers = new HashMap<DatabaseSchema,TransactInvoker>();
                List<String> databases = getDatabases().get();
                for (String database : databases) {
                    DatabaseSchema dbSchema = getSchema(database).get();
                    if (dbSchema != null) {
                        transactInvokers.put(dbSchema, new TransactInvokerImpl(this,dbSchema));
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Exception attempting to createTransactionInvokers {}: {}",connectionInfo,e);
            }
        }
    }



    private void monitorAllTables(String database, DatabaseSchema dbSchema) {
        Set<String> tables = dbSchema.getTables();
        if (tables != null) {
            List<MonitorRequest<GenericTableSchema>> monitorRequests = Lists.newArrayList();
            for (String tableName : tables) {
                GenericTableSchema tableSchema = dbSchema.table(tableName, GenericTableSchema.class);
                Set<String> columns = tableSchema.getColumns();
                MonitorRequestBuilder<GenericTableSchema> monitorBuilder = MonitorRequestBuilder.builder(tableSchema);
                for (String column : columns) {
                    monitorBuilder.addColumn(column);
                }
                monitorRequests.add(monitorBuilder.with(new MonitorSelect(true, true, true, true)).build());
            }
            this.callback.update(monitor(dbSchema, monitorRequests, callback),dbSchema);
        } else {
            LOG.warn("No tables for schema {} for database {} for key {}",dbSchema,database,connectionInfo);
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

    public ListenableFuture<List<OperationResult>> transact(
            DatabaseSchema dbSchema, List<Operation> operations) {
        return client.transact(dbSchema, operations);
    }

    public <E extends TableSchema<E>> TableUpdates monitor(
            DatabaseSchema schema, List<MonitorRequest<E>> monitorRequests,
            MonitorCallBack callback) {
        return client.monitor(schema, monitorRequests, callback);
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

    public void startEchoService(EchoServiceCallbackFilters callbackFilters) {
        client.startEchoService(callbackFilters);
    }

    public void stopEchoService() {
        client.stopEchoService();
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
        return getInstanceIdentifier().firstKeyOf(Node.class);
    }

    public NodeId getNodeId() {
        return getNodeKey().getNodeId();
    }

    public void setInstanceIdentifier(InstanceIdentifier<Node> iid) {
        this.instanceIdentifier = iid;
    }

    @Override
    public <E extends TableSchema<E>> TableUpdates monitor(
            DatabaseSchema schema, List<MonitorRequest<E>> monitorRequests,
            MonitorHandle monitorHandle, MonitorCallBack callback) {
        return null;
    }
}
