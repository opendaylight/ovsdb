package org.opendaylight.ovsdb.lib.message;

import java.util.List;

import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.message.operations.OperationResult;

public interface OVSDB {
    public static final String REGISTER_CALLBACK_METHOD = "registerCallback";

    public ListenableFuture<DatabaseSchema> get_schema(List<String> db_names);

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
        public void locked(Node node, Object json_value);
        // public void echo(Node node, Object json_value);
    }
}
