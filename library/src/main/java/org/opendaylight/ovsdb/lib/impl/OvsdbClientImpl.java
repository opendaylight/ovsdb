/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.impl;

import io.netty.channel.Channel;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.opendaylight.ovsdb.lib.EchoServiceCallbackFilters;
import org.opendaylight.ovsdb.lib.LockAquisitionCallback;
import org.opendaylight.ovsdb.lib.LockStolenCallback;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.MonitorHandle;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo.ConnectionType;
import org.opendaylight.ovsdb.lib.jsonrpc.Params;
import org.opendaylight.ovsdb.lib.message.MonitorRequest;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.message.TransactBuilder;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;


public class OvsdbClientImpl implements OvsdbClient {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbClientImpl.class);
    private ExecutorService executorService;
    private OvsdbRPC rpc;
    private Map<String, DatabaseSchema> schema = Maps.newHashMap();
    private Map<String, CallbackContext> monitorCallbacks = Maps.newHashMap();
    private OvsdbRPC.Callback rpcCallback;
    private OvsdbConnectionInfo connectionInfo;
    private Channel channel;

    public OvsdbClientImpl(OvsdbRPC rpc, Channel channel, ConnectionType type, ExecutorService executorService) {
        this.rpc = rpc;
        this.executorService = executorService;
        this.channel = channel;

        this.connectionInfo = new OvsdbConnectionInfo(channel, type);
    }

    OvsdbClientImpl() {
    }

    void setupUpdateListener() {
        if (rpcCallback == null) {
            OvsdbRPC.Callback temp = new OvsdbRPC.Callback() {
                @Override
                public void update(Object node, UpdateNotification updateNotification) {
                    Object key = updateNotification.getContext();
                    CallbackContext callbackContext = monitorCallbacks.get(key);
                    MonitorCallBack monitorCallBack = callbackContext.monitorCallBack;
                    if (monitorCallBack == null) {
                        //ignore ?
                        LOG.info("callback received with context {}, but no known handler. Ignoring!", key);
                        return;
                    }
                    TableUpdates updates = transformingCallback(updateNotification.getUpdates(),
                            callbackContext.schema);
                    monitorCallBack.update(updates, callbackContext.schema);
                }

                @Override
                public void locked(Object node, List<String> ids) {

                }

                @Override
                public void stolen(Object node, List<String> ids) {

                }
            };
            this.rpcCallback = temp;
            rpc.registerCallback(temp);
        }
    }


    protected TableUpdates transformingCallback(JsonNode tableUpdatesJson, DatabaseSchema dbSchema) {
        //todo(ashwin): we should move all the JSON parsing logic to a utility class
        if (tableUpdatesJson instanceof ObjectNode) {
            Map<String, TableUpdate> tableUpdateMap = Maps.newHashMap();
            ObjectNode updatesJson = (ObjectNode) tableUpdatesJson;
            for (Iterator<Map.Entry<String,JsonNode>> itr = updatesJson.fields(); itr.hasNext();) {
                Map.Entry<String, JsonNode> entry = itr.next();

                DatabaseSchema databaseSchema = this.schema.get(dbSchema.getName());
                TableSchema table = databaseSchema.table(entry.getKey(), TableSchema.class);
                tableUpdateMap.put(entry.getKey(), table.updatesFromJson(entry.getValue()));

            }
            return new TableUpdates(tableUpdateMap);
        }
        return null;
    }

    @Override
    public ListenableFuture<List<OperationResult>> transact(DatabaseSchema dbSchema, List<Operation> operations) {

        //todo, we may not need transactionbuilder if we can have JSON objects
        TransactBuilder builder = new TransactBuilder(dbSchema);
        for (Operation operation : operations) {
            builder.addOperation(operation);
        }

        return FutureTransformUtils.transformTransactResponse(rpc.transact(builder), operations);
    }

    @Override
    public <E extends TableSchema<E>> TableUpdates monitor(final DatabaseSchema dbSchema,
                                                            List<MonitorRequest<E>> monitorRequest,
                                                            final MonitorCallBack callback) {

        final ImmutableMap<String, MonitorRequest<E>> reqMap = Maps.uniqueIndex(monitorRequest,
                new Function<MonitorRequest<E>, String>() {
                    @Override
                    public String apply(MonitorRequest<E> input) {
                        return input.getTableName();
                    }
                });

        final MonitorHandle monitorHandle = new MonitorHandle(UUID.randomUUID().toString());
        registerCallback(monitorHandle, callback, dbSchema);

        ListenableFuture<JsonNode> monitor = rpc.monitor(new Params() {
            @Override
            public List<Object> params() {
                return Lists.<Object>newArrayList(dbSchema.getName(), monitorHandle.getId(), reqMap);
            }
        });
        JsonNode result;
        try {
            result = monitor.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
        return transformingCallback(result, dbSchema);
    }

    @Override
    public <E extends TableSchema<E>> TableUpdates monitor(final DatabaseSchema dbSchema,
                                                           List<MonitorRequest<E>> monitorRequest,
                                                           final MonitorHandle monitorHandle,
                                                           final MonitorCallBack callback) {

        final ImmutableMap<String, MonitorRequest<E>> reqMap = Maps.uniqueIndex(monitorRequest,
                new Function<MonitorRequest<E>, String>() {
                    @Override
                    public String apply(MonitorRequest<E> input) {
                        return input.getTableName();
                    }
                });

        registerCallback(monitorHandle, callback, dbSchema);

        ListenableFuture<JsonNode> monitor = rpc.monitor(new Params() {
            @Override
            public List<Object> params() {
                return Lists.<Object>newArrayList(dbSchema.getName(), monitorHandle.getId(), reqMap);
            }
        });
        JsonNode result;
        try {
            result = monitor.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
        TableUpdates updates = transformingCallback(result, dbSchema);
        return updates;
    }

    private void registerCallback(MonitorHandle monitorHandle, MonitorCallBack callback, DatabaseSchema schema) {
        this.monitorCallbacks.put(monitorHandle.getId(), new CallbackContext(callback, schema));
        setupUpdateListener();
    }

    @Override
    public void cancelMonitor(final MonitorHandle handler) {
        ListenableFuture<JsonNode> cancelMonitor = rpc.monitor_cancel(new Params() {
            @Override
            public List<Object> params() {
                return Lists.<Object>newArrayList(handler.getId());
            }
        });

        JsonNode result = null;
        try {
            result = cancelMonitor.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Exception when canceling monitor handler {}", handler.getId());
        }

        if (result == null) {
            LOG.error("Fail to cancel monitor with handler {}", handler.getId());
        } else {
            LOG.debug("Successfully cancel monitoring for handler {}", handler.getId());
        }
    }

    @Override
    public void lock(String lockId, LockAquisitionCallback lockedCallBack, LockStolenCallback stolenCallback) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public ListenableFuture<Boolean> steal(String lockId) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public ListenableFuture<Boolean> unLock(String lockId) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void startEchoService(EchoServiceCallbackFilters callbackFilters) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void stopEchoService() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public TransactionBuilder transactBuilder(DatabaseSchema dbSchema) {
        return new TransactionBuilder(this, dbSchema);
    }


    public boolean isReady(int timeout) throws InterruptedException {
        while (timeout > 0) {
            if (!schema.isEmpty()) {
                return true;
            }
            Thread.sleep(1000);
            timeout--;
        }
        return false;
    }

    @Override
    public ListenableFuture<List<String>> getDatabases() {
        return rpc.list_dbs();
    }

    @Override
    public ListenableFuture<DatabaseSchema> getSchema(final String database) {

        DatabaseSchema databaseSchema = schema.get(database);

        if (databaseSchema == null) {
            return Futures.transform(
                    getSchemaFromDevice(Lists.newArrayList(database)),
                    new Function<Map<String, DatabaseSchema>, DatabaseSchema>() {
                        @Override
                        public DatabaseSchema apply(Map<String, DatabaseSchema> result) {
                            if (result.containsKey(database)) {
                                DatabaseSchema dbSchema = result.get(database);
                                dbSchema.populateInternallyGeneratedColumns();
                                OvsdbClientImpl.this.schema.put(database, dbSchema);
                                return dbSchema;
                            } else {
                                return null;
                            }
                        }
                    }, executorService);
        } else {
            return Futures.immediateFuture(databaseSchema);
        }
    }

    private ListenableFuture<Map<String, DatabaseSchema>> getSchemaFromDevice(final List<String> dbNames) {
        Map<String, DatabaseSchema> schema = Maps.newHashMap();
        SettableFuture<Map<String, DatabaseSchema>> future = SettableFuture.create();
        populateSchema(dbNames, schema, future);
        return future;
    }

    private void populateSchema(final List<String> dbNames,
                                 final Map<String, DatabaseSchema> schema,
                                 final SettableFuture<Map<String, DatabaseSchema>> sfuture) {

        if (dbNames == null || dbNames.isEmpty()) {
            return;
        }

        Futures.transform(rpc.get_schema(Lists.newArrayList(dbNames.get(0))),
                new com.google.common.base.Function<JsonNode, Void>() {
                    @Override
                    public Void apply(JsonNode jsonNode) {
                        try {
                            schema.put(dbNames.get(0), DatabaseSchema.fromJson(dbNames.get(0), jsonNode));
                            if (schema.size() > 1 && !sfuture.isCancelled()) {
                                populateSchema(dbNames.subList(1, dbNames.size()), schema, sfuture);
                            } else if (schema.size() == 1) {
                                sfuture.set(schema);
                            }
                        } catch (Exception e) {
                            sfuture.setException(e);
                        }
                        return null;
                    }
                });
    }

    public void setRpc(OvsdbRPC rpc) {
        this.rpc = rpc;
    }

    static class CallbackContext {
        MonitorCallBack monitorCallBack;
        DatabaseSchema schema;

        CallbackContext(MonitorCallBack monitorCallBack, DatabaseSchema schema) {
            this.monitorCallBack = monitorCallBack;
            this.schema = schema;
        }
    }

    @Override
    public DatabaseSchema getDatabaseSchema(String dbName) {
        return schema.get(dbName);
    }

    /**
     * This method finds the DatabaseSchema that matches a given Typed Table Class.
     * With the introduction of TypedTable and TypedColumn annotations, it is possible to express
     * the Database Name, Table Name & the Database Versions within which the Table is defined and maintained.
     *
     * @param klazz Typed Class that represents a Table
     * @return DatabaseSchema that matches a Typed Table Class
     */
    private <T> DatabaseSchema getDatabaseSchemaForTypedTable(Class<T> klazz) {
        TypedTable typedTable = klazz.getAnnotation(TypedTable.class);
        if (typedTable != null) {
            return this.getDatabaseSchema(typedTable.database());
        }
        return null;
    }

    /**
     * User friendly convenient method that make use of TyperUtils.getTypedRowWrapper to create a Typed Row Proxy
     * given the Typed Table Class
     *
     * @param klazz Typed Interface
     * @return Proxy wrapper for the actual raw Row class.
     */
    @Override
    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(Class<T> klazz) {
        DatabaseSchema dbSchema = getDatabaseSchemaForTypedTable(klazz);
        return this.createTypedRowWrapper(dbSchema, klazz);
    }

    /**
     * User friendly convenient method that make use of getTypedRowWrapper to create a Typed Row Proxy given
     * DatabaseSchema and Typed Table Class.
     *
     * @param dbSchema Database Schema of interest
     * @param klazz Typed Interface
     * @return Proxy wrapper for the actual raw Row class.
     */
    @Override
    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(DatabaseSchema dbSchema, Class<T> klazz) {
        return TyperUtils.getTypedRowWrapper(dbSchema, klazz, new Row<GenericTableSchema>());
    }

    /**
     * User friendly convenient method to get a Typed Row Proxy given a Typed Table Class and the Row to be wrapped.
     *
     * @param klazz Typed Interface
     * @param row The actual Row that the wrapper is operating on.
     *            It can be null if the caller is just interested in getting ColumnSchema.
     * @return Proxy wrapper for the actual raw Row class.
     */
    @Override

    public <T extends TypedBaseTable<?>> T getTypedRowWrapper(final Class<T> klazz, final Row<GenericTableSchema> row) {
        DatabaseSchema dbSchema = getDatabaseSchemaForTypedTable(klazz);
        return TyperUtils.getTypedRowWrapper(dbSchema, klazz, row);
    }

    @Override
    public OvsdbConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public void disconnect() {
        channel.disconnect();
    }
}
