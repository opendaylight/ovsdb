package org.opendaylight.ovsdb;

import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.operations.OperationResult;
import org.opendaylight.ovsdb.lib.meta.BaseType;
import org.opendaylight.ovsdb.lib.meta.ColumnSchema;
import org.opendaylight.ovsdb.lib.meta.TableSchema;
import org.opendaylight.ovsdb.lib.meta.temp.Reference;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.plugin.OvsdbTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.opendaylight.ovsdb.OpenVswitch.Operations.op;

/**
 * @author Ashwin Raveendran
 */
public class OpenVswitchTestITTyped extends OvsdbTestBase {

    Logger logger = LoggerFactory.getLogger(OpenVswitchTestITTyped.class);


    static class Bridge extends TableSchema<Bridge> {

        Bridge(String name, Map<String, ColumnSchema> columns) {
            super(name, columns);
        }

        public ColumnSchema<Bridge, String> name() {
            return column("name");
        }

        public ColumnSchema<Bridge, Integer> floodVlans() {
            return column("flood_vlans");
        }

        public ColumnSchema<Bridge, String> status() {
            return column("status");
        }

        public ColumnSchema<Bridge, Reference> netflow() {
            return column("netflow");
        }
    }


    @Test
    public void test() throws IOException, InterruptedException, ExecutionException {
        OpenVswitch ovs = getVswitch();

        Bridge bridge = ovs.schema().table("Bridge", Bridge.class);

        ListenableFuture<List<OperationResult>> results = ovs.transact()
                .add(op.insert(bridge).value(bridge.name(), "br-int"))
                .add(op.update(bridge)
                        .set(bridge.status(), "br-blah")
                        .set(bridge.floodVlans(), 34)
                        .where(bridge.name().opEqual("br-int"))
                        .and(bridge.name().opEqual("br-int")).operation())
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
