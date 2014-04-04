/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb;

import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.OvsDBClient;
import org.opendaylight.ovsdb.lib.OvsDBClientImpl;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.notation.operations.OperationResult;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.temp.Reference;
import org.opendaylight.ovsdb.plugin.OvsdbTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.opendaylight.ovsdb.lib.notation.operations.Operations.op;

public class OpenVswitchTestITTyped extends OvsdbTestBase {

    Logger logger = LoggerFactory.getLogger(OpenVswitchTestITTyped.class);


    static class Bridge extends TableSchema<Bridge> {

        Bridge(String name, Map<String, ColumnSchema> columns) {
            super(name, columns);
        }

        public ColumnSchema<Bridge, String> name() {
            return column("name", String.class);
        }

        public ColumnSchema<Bridge, Integer> floodVlans() {
            return column("flood_vlans", Integer.class);
        }

        public ColumnSchema<Bridge, String> status() {
            return column("status", String.class);
        }

        public ColumnSchema<Bridge, Reference> netflow() {
            return column("netflow", Reference.class);
        }
    }


    @Test
    public void test() throws IOException, InterruptedException, ExecutionException {
        OvsDBClientImpl ovs = getVswitch();

        Bridge bridge = ovs.getSchema(OvsDBClient.OPEN_VSWITCH_SCHEMA, true).get().table("Bridge", Bridge.class);

        ListenableFuture<List<OperationResult>> results = ovs.transactBuilder()
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




    private OvsDBClientImpl getVswitch() throws IOException, InterruptedException {
        TestObjects testConnection = getTestConnection();
        OvsdbRPC rpc = testConnection.connectionService.getConnection(testConnection.node).getRpc();

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        OvsDBClientImpl ovs = new OvsDBClientImpl(rpc, executorService);

        for (int i = 0; i < 100; i++) {
           if (ovs.isReady(0)) {
              break;
           }
           Thread.sleep(1000);
        }
        return ovs;
    }

}
