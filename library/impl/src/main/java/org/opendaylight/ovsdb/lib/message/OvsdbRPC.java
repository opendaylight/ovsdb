/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.ovsdb.lib.jsonrpc.Params;

public interface OvsdbRPC {

    String REGISTER_CALLBACK_METHOD = "registerCallback";

    //public ListenableFuture<DatabaseSchema> get_schema(List<String> db_names);
    ListenableFuture<JsonNode> get_schema(List<String> dbNames);

    ListenableFuture<List<String>> echo();

    ListenableFuture<JsonNode> monitor(Params equest);

    ListenableFuture<List<String>> list_dbs();

    ListenableFuture<List<JsonNode>> transact(TransactBuilder transact);

    ListenableFuture<Response> cancel(String id);

    ListenableFuture<JsonNode> monitor_cancel(Params jsonValue);

    ListenableFuture<Object> lock(List<String> id);

    ListenableFuture<Object> steal(List<String> id);

    ListenableFuture<Object> unlock(List<String> id);

    boolean registerCallback(Callback callback);


    interface Callback {
        void update(Object context, UpdateNotification upadateNotification);

        void locked(Object context, List<String> ids);

        void stolen(Object context, List<String> ids);

        // ECHO is handled by JsonRPCEndpoint directly.
        // We can add Echo request here if there is a need for clients to handle it.
    }
}
