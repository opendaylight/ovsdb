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

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.schema.ATableSchema;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;


public class OvsDBClientTestIT extends OvsdbTestBase {
    Logger logger = LoggerFactory.getLogger(OvsDBClientTestIT.class);

    OvsDBClientImpl ovs;



    @Test
    public void testTransact() throws IOException, InterruptedException, ExecutionException {

        ListenableFuture<DatabaseSchema> schema = ovs.getSchema(OvsDBClient.OPEN_VSWITCH_SCHEMA, true);
        TableSchema<ATableSchema> bridge = schema.get().table("Bridge");

        for (Map.Entry<String, ColumnSchema> names : bridge.getColumnSchemas().entrySet()) {
            System.out.println("names = " + names.getKey());
            System.out.println("names.getValue().getType() = " + names.getValue().getType().getBaseType());
        }

        ColumnSchema<ATableSchema, String> name = bridge.column("name", String.class);
        ColumnSchema<ATableSchema, String> fail_mode = bridge.column("fail_mode", String.class);

        ListenableFuture<List<OperationResult>> results = ovs.transactBuilder()
                .add(op.insert(bridge).value(name, "br-int"))
                .add(op.update(bridge)
                        .set(fail_mode, "secure")
                        .where(name.opEqual("br-int"))
                        //.and(name.opEqual("br-int"))
                        .operation())
                .execute();

        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        System.out.println("operationResults = " + operationResults);
    }

    @Test
    public void testGetDBs() throws ExecutionException, InterruptedException {
        ListenableFuture<List<String>> databases = ovs.getDatabases();
        List<String> dbNames = databases.get();
        Assert.assertNotNull(dbNames);
        Assert.assertTrue(dbNames.size() > 0);
    }

    @Before
    public  void initalize() throws IOException {
        if (ovs != null) {
            return;
        }
        OvsdbRPC rpc = getTestConnection();
        if (rpc == null) {
            System.out.println("Unable to Establish Test Connection");
        }
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        ovs = new OvsDBClientImpl(rpc, executorService);
    }


    @Override
    public void update(Object node, UpdateNotification upadateNotification) {
        // TODO Auto-generated method stub

    }
    @Override
    public void locked(Object node, List<String> ids) {
        // TODO Auto-generated method stub

    }
    @Override
    public void stolen(Object node, List<String> ids) {
        // TODO Auto-generated method stub

    }
}
