/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.message;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.message.operations.OperationResult;

public interface OvsdbRPC {
    public static final String REGISTER_CALLBACK_METHOD = "registerCallback";

    //public ListenableFuture<DatabaseSchema> get_schema(List<String> db_names);
    public ListenableFuture<JsonNode> get_schema(List<String> db_names);

    public ListenableFuture<List<String>> echo();

    public ListenableFuture<TableUpdates> monitor(MonitorRequestBuilder request);

    public ListenableFuture<List<String>> list_dbs();

    public ListenableFuture<List<OperationResult>> transact(TransactBuilder transact);

    public ListenableFuture<Response> cancel(String id);

    public ListenableFuture<Object> monitor_cancel(Object json_value);

    public ListenableFuture<Object> lock(List<String> id);

    public ListenableFuture<Object> steal(List<String> id);

    public ListenableFuture<Object> unlock(List<String> id);

    public boolean registerCallback(Callback callback);

    public static interface Callback {
        public void update(Node node, UpdateNotification upadateNotification);
        public void locked(Node node, List<String> ids);
        public void stolen(Node node, List<String> ids);
        // ECHO is handled by JsonRPCEndpoint directly.
        // We can add Echo request here if there is a need for clients to handle it.
    }
}
