/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.Channel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.ovsdb.lib.EchoServiceCallbackFilters;
import org.opendaylight.ovsdb.lib.LockAquisitionCallback;
import org.opendaylight.ovsdb.lib.LockStolenCallback;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.MonitorHandle;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo.ConnectionType;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo.SocketConnectionType;
import org.opendaylight.ovsdb.lib.error.ParsingException;
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
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedReflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbClientImpl implements OvsdbClient {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbClientImpl.class);
    private ExecutorService executorService;
    private OvsdbRPC rpc;
    private final Map<String, TypedDatabaseSchema> schemas = new HashMap<>();
    private final Map<String, CallbackContext> monitorCallbacks = new HashMap<>();
    private OvsdbRPC.Callback rpcCallback;
    private OvsdbConnectionInfo connectionInfo;
    private Channel channel;
    private boolean isConnectionPublished;
    private static final int NO_TIMEOUT = -1;

    private static final ThreadFactory THREAD_FACTORY_SSL =
        new ThreadFactoryBuilder().setNameFormat("OVSDB-PassiveConnection-SSL-%d").build();
    private static final ThreadFactory THREAD_FACTORY_NON_SSL =
        new ThreadFactoryBuilder().setNameFormat("OVSDB-PassiveConnection-Non-SSL-%d").build();

    public OvsdbClientImpl(final OvsdbRPC rpc, final Channel channel, final ConnectionType type,
        final SocketConnectionType socketConnType) {
        this.rpc = rpc;
        ThreadFactory threadFactory =
            getThreadFactory(type, socketConnType, channel.remoteAddress().toString());
        this.executorService = Executors.newCachedThreadPool(threadFactory);
        this.channel = channel;
        this.connectionInfo = new OvsdbConnectionInfo(channel, type);
    }

    /**
     * Generate the threadFactory based on ACTIVE, PASSIVE (SSL/NON-SSL) connection type.
     * @param type ACTIVE or PASSIVE {@link ConnectionType}
     * @param socketConnType SSL or NON-SSL {@link SocketConnectionType}
     * @param executorNameArgs Additional args to append to thread name format
     * @return {@link ThreadFactory}
     */
    private ThreadFactory getThreadFactory(final ConnectionType type,
        final SocketConnectionType socketConnType, final String executorNameArgs) {
        if (type == ConnectionType.PASSIVE) {
            switch (socketConnType) {
                case SSL:
                    return THREAD_FACTORY_SSL;
                case NON_SSL:
                    return THREAD_FACTORY_NON_SSL;
                default:
                    return Executors.defaultThreadFactory();
            }
        } else if (type == ConnectionType.ACTIVE) {
            ThreadFactory threadFactorySSL =
                new ThreadFactoryBuilder().setNameFormat("OVSDB-ActiveConn-" + executorNameArgs + "-%d")
                    .build();
            return threadFactorySSL;
        }
        // Default case
        return Executors.defaultThreadFactory();
    }

    OvsdbClientImpl() {
    }

    void setupUpdateListener() {
        if (rpcCallback == null) {
            OvsdbRPC.Callback temp = new OvsdbRPC.Callback() {
                @Override
                public void update(final Object node, final UpdateNotification updateNotification) {
                    String key = updateNotification.getContext();
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
                public void locked(final Object node, final List<String> ids) {

                }

                @Override
                public void stolen(final Object node, final List<String> ids) {

                }
            };
            this.rpcCallback = temp;
            rpc.registerCallback(temp);
        }
    }


    protected TableUpdates transformingCallback(final JsonNode tableUpdatesJson, final DatabaseSchema dbSchema) {
        //todo(ashwin): we should move all the JSON parsing logic to a utility class
        if (tableUpdatesJson instanceof ObjectNode) {
            Map<String, TableUpdate> tableUpdateMap = new HashMap<>();
            ObjectNode updatesJson = (ObjectNode) tableUpdatesJson;
            for (Iterator<Map.Entry<String,JsonNode>> itr = updatesJson.fields(); itr.hasNext();) {
                Map.Entry<String, JsonNode> entry = itr.next();

                DatabaseSchema databaseSchema = this.schemas.get(dbSchema.getName());
                TableSchema table = databaseSchema.table(entry.getKey(), TableSchema.class);
                tableUpdateMap.put(entry.getKey(), table.updatesFromJson(entry.getValue()));

            }
            return new TableUpdates(tableUpdateMap);
        }
        return null;
    }

    @Override
    public ListenableFuture<List<OperationResult>> transact(final DatabaseSchema dbSchema,
            final List<Operation> operations) {

        //todo, we may not need transactionbuilder if we can have JSON objects
        TransactBuilder builder = new TransactBuilder(dbSchema);
        for (Operation operation : operations) {
            builder.addOperation(operation);
        }

        return FutureTransformUtils.transformTransactResponse(rpc.transact(builder), operations);
    }

    @Override
    public <E extends TableSchema<E>> TableUpdates monitor(final DatabaseSchema dbSchema,
                                                           final List<MonitorRequest> monitorRequest,
                                                           final MonitorCallBack callback) {
        return monitor(dbSchema, monitorRequest, callback, NO_TIMEOUT);
    }

    @Override
    public <E extends TableSchema<E>> TableUpdates monitor(final DatabaseSchema dbSchema,
                                                           final List<MonitorRequest> monitorRequest,
                                                           final MonitorCallBack callback,
                                                           final int timeout) {
        return monitor(dbSchema, monitorRequest, new MonitorHandle(UUID.randomUUID().toString()), callback, timeout);
    }

    @Override
    public <E extends TableSchema<E>> TableUpdates monitor(final DatabaseSchema dbSchema,
                                                           final List<MonitorRequest> monitorRequest,
                                                           final MonitorHandle monitorHandle,
                                                           final MonitorCallBack callback) {
        return monitor(dbSchema, monitorRequest, monitorHandle, callback, NO_TIMEOUT);
    }

    @Override
    public <E extends TableSchema<E>> TableUpdates monitor(final DatabaseSchema dbSchema,
                                                           final List<MonitorRequest> monitorRequest,
                                                           final MonitorHandle monitorHandle,
                                                           final MonitorCallBack callback,
                                                           final int timeout) {
        final ImmutableMap<String, MonitorRequest> reqMap = Maps.uniqueIndex(monitorRequest,
                MonitorRequest::getTableName);

        registerCallback(monitorHandle, callback, dbSchema);

        final ListenableFuture<JsonNode> monitor = rpc.monitor(
            () -> Arrays.asList(dbSchema.getName(), monitorHandle.getId(), reqMap));
        final JsonNode result;
        try {
            if (timeout == NO_TIMEOUT) {
                result = monitor.get();
            } else {
                result = monitor.get(timeout, TimeUnit.SECONDS);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Failed to monitor {}", dbSchema, e);
            return null;
        }
        return transformingCallback(result, dbSchema);
    }

    private void registerCallback(final MonitorHandle monitorHandle, final MonitorCallBack callback,
            final DatabaseSchema schema) {
        this.monitorCallbacks.put(monitorHandle.getId(), new CallbackContext(callback, schema));
        setupUpdateListener();
    }

    @Override
    public void cancelMonitor(final MonitorHandle handler) {
        cancelMonitor(handler, NO_TIMEOUT);
    }

    @Override
    public void cancelMonitor(final MonitorHandle handler, final int timeout) {
        ListenableFuture<JsonNode> cancelMonitor = rpc.monitor_cancel(() -> Collections.singletonList(handler.getId()));

        JsonNode result = null;
        try {
            if (timeout == NO_TIMEOUT) {
                result = cancelMonitor.get();
            } else {
                result = cancelMonitor.get(timeout, TimeUnit.SECONDS);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Exception when canceling monitor handler {}", handler.getId(), e);
        }

        if (result == null) {
            LOG.error("Fail to cancel monitor with handler {}", handler.getId());
        } else {
            LOG.debug("Successfully cancel monitoring for handler {}", handler.getId());
        }
    }

    @Override
    public ListenableFuture<List<String>> echo() {
        return rpc.echo();
    }

    @Override
    public void lock(final String lockId, final LockAquisitionCallback lockedCallBack,
            final LockStolenCallback stolenCallback) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public ListenableFuture<Boolean> steal(final String lockId) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public ListenableFuture<Boolean> unLock(final String lockId) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void startEchoService(final EchoServiceCallbackFilters callbackFilters) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void stopEchoService() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public TransactionBuilder transactBuilder(final DatabaseSchema dbSchema) {
        return new TransactionBuilder(this, dbSchema);
    }


    public boolean isReady(int timeout) throws InterruptedException {
        while (timeout > 0) {
            if (!schemas.isEmpty()) {
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
    public ListenableFuture<TypedDatabaseSchema> getSchema(final String database) {
        final TypedDatabaseSchema existing = schemas.get(database);
        if (existing != null) {
            return Futures.immediateFuture(existing);
        }

        return Futures.transform(getSchemaFromDevice(Collections.singletonList(database)), result -> {
            final DatabaseSchema dbSchema = result.get(database);
            if (dbSchema == null) {
                return null;
            }

            final TypedDatabaseSchema typedSchema = TypedDatabaseSchema.of(dbSchema.withInternallyGeneratedColumns());
            final TypedDatabaseSchema raced = schemas.putIfAbsent(database, typedSchema);
            return raced != null ? raced : typedSchema;
        }, executorService);
    }

    private ListenableFuture<Map<String, DatabaseSchema>> getSchemaFromDevice(final List<String> dbNames) {
        Map<String, DatabaseSchema> schema = new HashMap<>();
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

        Futures.transform(rpc.get_schema(Collections.singletonList(dbNames.get(0))), jsonNode -> {
            try {
                schema.put(dbNames.get(0), DatabaseSchema.fromJson(dbNames.get(0), jsonNode));
                if (schema.size() > 1 && !sfuture.isCancelled()) {
                    populateSchema(dbNames.subList(1, dbNames.size()), schema, sfuture);
                } else if (schema.size() == 1) {
                    sfuture.set(schema);
                }
            } catch (ParsingException e) {
                LOG.warn("Failed to populate schema {}:{}", dbNames, schema, e);
                sfuture.setException(e);
            }
            return null;
        }, MoreExecutors.directExecutor());
    }

    public void setRpc(final OvsdbRPC rpc) {
        this.rpc = rpc;
    }

    static class CallbackContext {
        MonitorCallBack monitorCallBack;
        DatabaseSchema schema;

        CallbackContext(final MonitorCallBack monitorCallBack, final DatabaseSchema schema) {
            this.monitorCallBack = monitorCallBack;
            this.schema = schema;
        }
    }

    @Override
    public TypedDatabaseSchema getDatabaseSchema(final String dbName) {
        return schemas.get(dbName);
    }

    /**
     * This method finds the DatabaseSchema that matches a given Typed Table Class.
     * With the introduction of TypedTable and TypedColumn annotations, it is possible to express
     * the Database Name, Table Name & the Database Versions within which the Table is defined and maintained.
     *
     * @param klazz Typed Class that represents a Table
     * @return DatabaseSchema that matches a Typed Table Class
     */
    private <T> TypedDatabaseSchema getDatabaseSchemaForTypedTable(final Class<T> klazz) {
        final String dbName = TypedReflections.getTableDatabase(klazz);
        return dbName == null ? null : getDatabaseSchema(dbName);
    }

    /**
     * User friendly convenient method that make use of TyperUtils.getTypedRowWrapper to create a Typed Row Proxy
     * given the Typed Table Class.
     *
     * @param klazz Typed Interface
     * @return Proxy wrapper for the actual raw Row class.
     */
    @Override
    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(final Class<T> klazz) {
        return getTypedRowWrapper(klazz, new Row<>());
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
    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(final DatabaseSchema dbSchema, final Class<T> klazz) {
        return dbSchema == null ? null : TypedDatabaseSchema.of(dbSchema).getTypedRowWrapper(klazz, new Row<>());
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
        final TypedDatabaseSchema dbSchema = getDatabaseSchemaForTypedTable(klazz);
        return dbSchema == null ? null : dbSchema.getTypedRowWrapper(klazz, row);
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
        executorService.shutdown();
    }

    @Override
    public boolean isConnectionPublished() {
        return isConnectionPublished;
    }

    @Override
    public void setConnectionPublished(final boolean connectionPublished) {
        isConnectionPublished = connectionPublished;
    }
}
