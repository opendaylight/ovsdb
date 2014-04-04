/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.TransactBuilder;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;


public class OvsDBClientImpl implements OvsDBClient {

    ExecutorService executorService;
    String schemaName;
    OvsdbRPC rpc;
    Map<String, DatabaseSchema> schema = Maps.newHashMap();
    Queue<Throwable> exceptions;

    public OvsDBClientImpl(OvsdbRPC rpc, ExecutorService executorService) {
        this.rpc = rpc;
        this.executorService = executorService;
    }

    public OvsDBClientImpl() {
    }

    @Override
    public ListenableFuture<List<OperationResult>> transact(List<Operation> operations) {

        //todo, we may not need transactionbuilder if we can have JSON objects
        TransactBuilder builder = new TransactBuilder();
        for (Operation o : operations) {
            builder.addOperation(o);
        }

        ListenableFuture<List<OperationResult>> transact = rpc.transact(builder);
        return transact;
    }

    @Override
    public TransactionBuilder transactBuilder() {
        return new TransactionBuilder(this);
    }


    public boolean isReady(long timeout) {
        //todo implement timeout
        return null != schema;
    }

    @Override
    public ListenableFuture<List<String>> getDatabases() {
        return rpc.list_dbs();
    }

    @Override
    public ListenableFuture<DatabaseSchema> getSchema(final String database, final boolean cacheResult) {

        DatabaseSchema databaseSchema = schema.get(database);

        if (databaseSchema == null) {
            ListenableFuture<Map<String, DatabaseSchema>> schemaFromDevice = getSchemaFromDevice(Lists.newArrayList(database));

            final SettableFuture<DatabaseSchema> future = SettableFuture.create();
            Futures.addCallback(schemaFromDevice, new FutureCallback<Map<String, DatabaseSchema>>() {
                @Override
                public void onSuccess(Map<String, DatabaseSchema> result) {
                    if (result.containsKey(database)) {
                       DatabaseSchema s = result.get(database);
                       if (cacheResult) {
                         OvsDBClientImpl.this.schema.put(database, s);
                       }
                       future.set(s);
                    } else {
                        future.set(null);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    //todo: should wrap
                    future.setException(t);
                }
            });
          return future;

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
                try{
                schema.put(dbNames.get(0), DatabaseSchema.fromJson(jsonNode));
                if (schema.size() > 1 && !sfuture.isCancelled()) {
                    _populateSchema(dbNames.subList(1, dbNames.size()), schema, sfuture);
                } else if (schema.size() == 1) {
                    sfuture.set(schema);
                }
            } catch (Throwable e) {
               sfuture.setException(e);
            }
            return null;
        }});
    }

    public void setRpc(OvsdbRPC rpc) {
        this.rpc = rpc;
    }

    public Queue<Throwable> getExceptions() {
        return exceptions;
    }

}
