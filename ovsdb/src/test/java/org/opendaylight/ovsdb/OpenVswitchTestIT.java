package org.opendaylight.ovsdb;

import com.google.common.util.concurrent.ListenableFuture;
import com.sun.xml.internal.xsom.impl.WildcardImpl;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.operations.OperationResult;
import org.opendaylight.ovsdb.lib.meta.ColumnSchema;
import org.opendaylight.ovsdb.lib.meta.TableSchema;
import org.opendaylight.ovsdb.plugin.OvsdbTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.opendaylight.ovsdb.OpenVswitch.Operations.op;

/**
 * @author Ashwin Raveendran
 */
public class OpenVswitchTestIT extends OvsdbTestBase {
    Logger logger = LoggerFactory.getLogger(OpenVswitchTestIT.class);

    @Test
    public void test() throws IOException, InterruptedException, ExecutionException {
        OpenVswitch ovs = getVswitch();

        TableSchema<TableSchema.AnyTableSchema> bridge = ovs.schema().table("Bridge");
        ColumnSchema<TableSchema.AnyTableSchema, String> name = bridge.column("name");

        ListenableFuture<List<OperationResult>> results = ovs.transact()
                .add(op.insert(bridge).value(name, "br-int"))
                .add(op.update(bridge)
                        .set(name, "br-int")
                        .where(name.opEqual("br-int"))
                        .and(name.opEqual("br-int")).operation())
                .execute();

        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        System.out.println("operationResults = " + operationResults);
    }


    private OpenVswitch getVswitch() throws IOException, InterruptedException {
        TestObjects testConnection = getTestConnection();
        OvsdbRPC rpc = testConnection.connectionService.getConnection(testConnection.node).getRpc();

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        OpenVswitch ovs = new OpenVswitch(rpc, executorService);
        ovs.populateSchemaFromDevice();

        for (int i = 0; i < 100; i++) {
           if (ovs.isReady(0)) {
              break;
           }
           Thread.sleep(1000);
        }
        return ovs;
    }

}
