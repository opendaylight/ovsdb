package org.opendaylight.ovsdb.internal.ovsdb;

import java.util.List;

import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.ovsdb.database.DatabaseSchema;
import org.opendaylight.ovsdb.message.EchoResponse;
import org.opendaylight.ovsdb.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.message.TableUpdates;


public interface OVSDB {

    public ListenableFuture<DatabaseSchema> get_schema(List<String> db_names);

    public ListenableFuture<List<String>> echo();

    public ListenableFuture<TableUpdates> monitor(MonitorRequestBuilder request);

    public ListenableFuture<List<String>> list_dbs();

    public ListenableFuture<List<Object>> transact();
    /*
    public void registerListener(Callback callback);

    public static interface Callback {
        public void monitorResponse(TableUpdates upadate);
    }
    */
}
