/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import java.util.List;
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
import org.opendaylight.ovsdb.southbound.transactions.md.OvsdbNodeCreateCommand;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

public class OvsdbConnectionInstance implements OvsdbClient {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionInstance.class);
    private OvsdbClient client;
    private OvsdbClientKey key;
    private TransactionInvoker txInvoker;
    private MonitorCallBack callback;

    OvsdbConnectionInstance(OvsdbClientKey key,OvsdbClient client,TransactionInvoker txInvoker) {
        this.key = key;
        this.client = client;
        this.txInvoker = txInvoker;
        txInvoker.invoke(new OvsdbNodeCreateCommand(key, null,null));
        registerCallBack();
    }

    private void registerCallBack() {
        this.callback = new OvsdbMonitorCallback(key,txInvoker);
        try {
            List<String> databases = getDatabases().get();
            if(databases != null) {
                for (String database : databases) {
                    DatabaseSchema dbSchema = getSchema(database).get();
                    if(dbSchema != null) {
                        Set<String> tables = dbSchema.getTables();
                        if(tables != null) {
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
                            LOG.warn("No tables for schema {} for database {} for key {}",dbSchema,database,key);
                        }
                    } else {
                        LOG.warn("No schema reported for database {} for key {}",database,key);
                    }
                }
            } else {
                LOG.warn("No databases reported from {}",key);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception attempting to initialize {}: {}",key,e);
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

    public OvsdbConnectionInfo getConnectionInfo() {
        return client.getConnectionInfo();
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

    public OvsdbClientKey getKey() {
        return key;
    }

    public void setKey(OvsdbClientKey key) {
        this.key = key;
    }
}
