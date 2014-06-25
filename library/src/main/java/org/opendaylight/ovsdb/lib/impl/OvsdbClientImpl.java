/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.opendaylight.ovsdb.lib.EchoServiceCallbackFilters;
import org.opendaylight.ovsdb.lib.LockAquisitionCallback;
import org.opendaylight.ovsdb.lib.LockStolenCallback;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.MonitorHandle;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;


public class OvsdbClientImpl implements OvsdbClient {

    protected static final Logger logger = LoggerFactory.getLogger(OvsdbClientImpl.class);
    private ExecutorService executorService;
    private OvsdbRPC rpc;
    private Map<String, DatabaseSchema> schema = Maps.newHashMap();
    private HashMap<String, CallbackContext> monitorCallbacks = Maps.newHashMap();
    private Queue<Throwable> exceptions;
    private OvsdbRPC.Callback rpcCallback;
    private OvsdbConnectionInfo connectionInfo;

    public OvsdbClientImpl(OvsdbRPC rpc, OvsdbConnectionInfo connectionInfo, ExecutorService executorService) {
        this.rpc = rpc;
        this.executorService = executorService;
        this.connectionInfo = connectionInfo;
    }

    OvsdbClientImpl() {
    }

    void setupUpdateListener() {
        if (rpcCallback == null) {
            OvsdbRPC.Callback temp = new OvsdbRPC.Callback() {
                @Override
                public void update(Object node, UpdateNotification upadateNotification) {
                    Object key = upadateNotification.getContext();
                    CallbackContext callbackContext = monitorCallbacks.get(key);
                    MonitorCallBack monitorCallBack = callbackContext.monitorCallBack;
                    if (monitorCallBack == null) {
                        //ignore ?
                        logger.info("callback received with context {}, but no known handler. Ignoring!", key);
                        return;
                    }
                    _transformingCallback(upadateNotification.getUpdates(), monitorCallBack, callbackContext.schema);
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


    protected void _transformingCallback(JsonNode tableUpdatesJson, MonitorCallBack monitorCallBack, DatabaseSchema dbSchema) {
        //todo(ashwin): we should move all the JSON parsing logic to a utility class
        if (tableUpdatesJson instanceof ObjectNode) {
            Map<String, TableUpdate> tableUpdateMap = Maps.newHashMap();
            ObjectNode updatesJson = (ObjectNode) tableUpdatesJson;
            for (Iterator<Map.Entry<String,JsonNode>> itr = updatesJson.fields(); itr.hasNext();){
                Map.Entry<String, JsonNode> entry = itr.next();

                DatabaseSchema databaseSchema = this.schema.get(dbSchema.getName());
                TableSchema table = databaseSchema.table(entry.getKey(), TableSchema.class);
                tableUpdateMap.put(entry.getKey(), table.updatesFromJson(entry.getValue()));

            }
            TableUpdates updates = new TableUpdates(tableUpdateMap);
            monitorCallBack.update(updates);
        }
    }

    @Override
    public ListenableFuture<List<OperationResult>> transact(List<Operation> operations) {

        //todo, we may not need transactionbuilder if we can have JSON objects
        TransactBuilder builder = new TransactBuilder();
        for (Operation o : operations) {
            builder.addOperation(o);
        }

        return rpc.transact(builder);
    }

    @Override
    public <E extends TableSchema<E>> MonitorHandle monitor(final DatabaseSchema dbSchema,
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
        Futures.addCallback(monitor, new FutureCallback<JsonNode>() {
            @Override
            public void onSuccess(JsonNode result) {
                _transformingCallback(result, callback, dbSchema);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.exception(t);
            }
        });

        return monitorHandle;
    }

    private void registerCallback(MonitorHandle monitorHandle, MonitorCallBack callback, DatabaseSchema schema) {
        this.monitorCallbacks.put(monitorHandle.getId(), new CallbackContext(callback, schema));
        setupUpdateListener();
    }

    @Override
    public void cancelMonitor(MonitorHandle handler) {
        throw new UnsupportedOperationException("not yet implemented");
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
    public TransactionBuilder transactBuilder() {
        return new TransactionBuilder(this);
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
    public ListenableFuture<DatabaseSchema> getSchema(final String database, final boolean cacheResult) {

        DatabaseSchema databaseSchema = schema.get(database);

        if (databaseSchema == null || cacheResult) {
            return Futures.transform(
                    getSchemaFromDevice(Lists.newArrayList(database)),
                    new Function<Map<String, DatabaseSchema>, DatabaseSchema>() {
                        @Override
                        public DatabaseSchema apply(Map<String, DatabaseSchema> result) {
                            if (result.containsKey(database)) {
                                DatabaseSchema s = result.get(database);
                                s.populateInternallyGeneratedColumns();
                                if (cacheResult) {
                                    OvsdbClientImpl.this.schema.put(database, s);
                                }
                                return s;
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
        _populateSchema(dbNames, schema, future);
        return future;
    }

    private void _populateSchema(final List<String> dbNames,
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
                                _populateSchema(dbNames.subList(1, dbNames.size()), schema, sfuture);
                            } else if (schema.size() == 1) {
                                sfuture.set(schema);
                            }
                        } catch (Throwable e) {
                            sfuture.setException(e);
                        }
                        return null;
                    }
                });
    }

    public void setRpc(OvsdbRPC rpc) {
        this.rpc = rpc;
    }

    public Queue<Throwable> getExceptions() {
        return exceptions;
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
    public DatabaseSchema getDatabaseSchema (String dbName) {
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
    private <T> DatabaseSchema getDatabaseSchemaForTypedTable (Class <T> klazz) {
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
    public <T> T createTypedRowWrapper(Class<T> klazz) {
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
    public <T> T createTypedRowWrapper(DatabaseSchema dbSchema, Class<T> klazz) {
        return TyperUtils.getTypedRowWrapper(dbSchema, klazz, new Row<GenericTableSchema>());
    }

    /**
     * User friendly convenient method to get a Typed Row Proxy given a Typed Table Class and the Row to be wrapped.
     *
     * @param klazz Typed Interface
     * @param row The actual Row that the wrapper is operating on. It can be null if the caller is just interested in getting ColumnSchema.
     * @return Proxy wrapper for the actual raw Row class.
     */
    @Override
    public <T> T getTypedRowWrapper(final Class<T> klazz, final Row<GenericTableSchema> row) {
        DatabaseSchema dbSchema = getDatabaseSchemaForTypedTable(klazz);
        return TyperUtils.getTypedRowWrapper(dbSchema, klazz, row);
    }

    @Override
    public OvsdbConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }
}
