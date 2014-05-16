/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.lib;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.opendaylight.ovsdb.lib.jsonrpc.Params;
import org.opendaylight.ovsdb.lib.message.MonitorRequest;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.message.TransactBuilder;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.ColumnType.KeyValuedColumnType;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

@RunWith(JUnit4.class)
public class OvsDBClientImplTest {

    OvsDBClientImpl ovs;
    OvsDBClientImpl mockedOvs;
    OvsdbRPC rpc;

    private static final String LOCK_ID = "testLock";
    private static final String DATABASE = "testDB";
    private static final String SCHEMA = "testSchema";

    @Before
    public void setUp() throws Exception {
        ovs = new OvsDBClientImpl();
        rpc = mock(OvsdbRPC.class);
        ovs.setRpc(rpc);
        mockedOvs = mock(OvsDBClientImpl.class);
        mockedOvs.setRpc(rpc);
    }

    @Test
    public void testTransact() throws Exception {

        TableSchema<GenericTableSchema> testTable = new GenericTableSchema();
        ColumnSchema<GenericTableSchema, String> testColumn = new ColumnSchema<>("test", new KeyValuedColumnType());

        OperationResult operationResult = new OperationResult();
        List<OperationResult> mockOperationResults = new ArrayList<>();
        mockOperationResults.add(operationResult);
        when(rpc.transact(any(TransactBuilder.class)))
                .thenReturn(Futures.immediateFuture(mockOperationResults));

        List<Operation> operationsList = new ArrayList<>();
        operationsList.add(op.insert(testTable).value(testColumn, "test"));

        ListenableFuture<List<OperationResult>> results = ovs.transact(operationsList);
        List<OperationResult> operationResults = results.get();
        assertFalse(operationResults.isEmpty());
        assertEquals(operationResult, operationResults.get(0));

    }

    public void testMonitor() throws Exception {
        DatabaseSchema dbSchema = new DatabaseSchema(new HashMap<String, TableSchema>());
        MonitorRequest<GenericTableSchema> monitorRequest = new MonitorRequest<>();
        List<MonitorRequest<GenericTableSchema>> monitorRequestList = Lists.newArrayList(monitorRequest);
        MonitorCallBack monitorCallBack = new MonitorCallBack() {
        @Override
        public void update(TableUpdates result) {
            System.out.println("result = " + result);
        }

        @Override
        public void exception(Throwable t) {
            System.out.println("t = " + t);
        }
        };

        TableUpdates tableUpdates = new TableUpdates();
        when(rpc.monitor(any(Params.class)))
                .thenReturn(Futures.immediateFuture(tableUpdates));

        MonitorHandle result = ovs.monitor(dbSchema, monitorRequestList, monitorCallBack);
        assertNotNull(result);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testCancelMonitor() throws Exception {
        MonitorHandle monitorHandle = new MonitorHandle("test");
        ovs.cancelMonitor(monitorHandle);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testLock() throws Exception {
        LockAquisitionCallback lockedCallBack = new LockAquisitionCallback() {
            @Override
            public void lockAcquired() {
                System.out.println("Lock Acquired");
            }
        };
        LockStolenCallback stolenCallback = new LockStolenCallback() {
            @Override
            public void lockStolen() {
                System.out.println("Lock Stolen");
            }
        };

        ovs.lock(LOCK_ID, lockedCallBack, stolenCallback);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testSteal() throws Exception {
        ovs.steal(LOCK_ID);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testUnLock() throws Exception {
        ovs.unLock(LOCK_ID);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testStartEchoService() throws Exception {
        EchoServiceCallbackFilters callbackFilters = new EchoServiceCallbackFilters() {};
        ovs.startEchoService(callbackFilters);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testStopEchoService() throws Exception {
        ovs.stopEchoService();
    }

    @Test
    public void testTransactBuilder() throws Exception {
        TransactionBuilder result = ovs.transactBuilder();
        assertThat(result, instanceOf(TransactionBuilder.class));
    }

    @Test
    public void testIsReady() throws Exception {
        Boolean ready = ovs.isReady(10);
        assertFalse(ready);
    }

    @Test
    public void testGetDatabases() throws Exception {
        List<String> dbList = new ArrayList<>();
        dbList.add("db1");
        dbList.add("db2");
        dbList.add("db3");

        when(rpc.list_dbs())
                .thenReturn(Futures.immediateFuture(dbList));

        ListenableFuture<List<String>> result = ovs.getDatabases();
        List<String> resultList =  result.get();

        assertEquals(dbList, resultList);

    }

    @Test
    public void testGetSchema() throws Exception {

        //ToDo: DT: Look at code smell in getSchema. Complex private methods.

        /*

        //This is an example:

        TableSchema<ATableSchema> mockTable = new TableSchema<>();
        Map<String, TableSchema> mockTableMap = new HashMap<>();
        mockTableMap.put("table1", mockTable);
        mockTableMap.put("table2", mockTable);
        Map<String, DatabaseSchema> mockSchema = new HashMap<>();
        mockSchema.put("testSchema", new DatabaseSchema(mockTableMap));

        ListenableFuture<Map<String, DatabaseSchema>> mockSchemaFuture = Futures.immediateFuture(mockSchema);

        ListenableFuture<DatabaseSchema> result;

        result = ovs.getSchema(DATABASE, false);
        assert(ovs.schema.size() == 0);
        DatabaseSchema firstResult = result.get();
        assertNotNull(firstResult);

        result = ovs.getSchema(DATABASE, true);
        assert(ovs.schema.size() == 1);
        DatabaseSchema secondResult = result.get();
        assertNotNull(secondResult);
        */
    }

}