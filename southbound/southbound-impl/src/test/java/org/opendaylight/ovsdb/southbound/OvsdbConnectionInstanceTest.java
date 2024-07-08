/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.getField;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.ovsdb.lib.LockAquisitionCallback;
import org.opendaylight.ovsdb.lib.LockStolenCallback;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.MonitorHandle;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.operations.DefaultOperations;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.BridgeOperationalState;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.DataChangeEvent;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactCommand;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactInvoker;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.class)
public class OvsdbConnectionInstanceTest {

    @Mock private OvsdbConnectionInstance ovsdbConnectionInstance;
    @Mock private OvsdbClient client;
    @Mock private ConnectionInfo connectionInfo;
    @Mock private TransactionInvoker txInvoker;
    @Mock private MonitorCallBack callback;
    @Mock private ConnectionInfo key;

    private final InstanceIdentifier<Node> instanceIdentifier = InstanceIdentifier.create(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
        .child(Node.class, new NodeKey(new NodeId("testNode")));
    private Map<DatabaseSchema, TransactInvoker> transactInvokers;

    @Before
    public void setUp() throws Exception {
        ovsdbConnectionInstance = spy(new OvsdbConnectionInstance(key, client, new DefaultOperations(), txInvoker,
            instanceIdentifier));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testTransact() throws Exception {
        transactInvokers = new HashMap();

        // init instance variables
        TransactInvoker transactInvoker1 = mock(TransactInvoker.class);
        TransactInvoker transactInvoker2 = mock(TransactInvoker.class);
        transactInvokers.put(mock(DatabaseSchema.class), transactInvoker1);
        transactInvokers.put(mock(DatabaseSchema.class), transactInvoker2);
        getField(OvsdbConnectionInstance.class, "transactInvokers").set(ovsdbConnectionInstance , transactInvokers);

        TransactCommand command = mock(TransactCommand.class);
        ovsdbConnectionInstance.transact(command, mock(BridgeOperationalState.class), mock(DataChangeEvent.class),
                mock(InstanceIdentifierCodec.class));
        verify(transactInvoker1).invoke(any(TransactCommand.class), any(BridgeOperationalState.class),
                any(DataChangeEvent.class), any(InstanceIdentifierCodec.class));
        verify(transactInvoker2).invoke(any(TransactCommand.class), any(BridgeOperationalState.class),
                any(DataChangeEvent.class), any(InstanceIdentifierCodec.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterCallbacks() throws Exception {
        InstanceIdentifierCodec instanceIdentifierCodec = mock(InstanceIdentifierCodec.class);

        // callback not null case
        getField(OvsdbConnectionInstance.class, "callback").set(ovsdbConnectionInstance , callback);
        ovsdbConnectionInstance.registerCallbacks(instanceIdentifierCodec);
        verify(ovsdbConnectionInstance, times(0)).getDatabases();

        // callback null case
        getField(OvsdbConnectionInstance.class, "callback").set(ovsdbConnectionInstance , null);
        List<String> databases = new ArrayList<>();
        databases.add("Open_vSwitch");
        databases.add("");

        ListenableFuture<DatabaseSchema> listenableDbSchema = mock(ListenableFuture.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        doReturn(listenableDbSchema).when(ovsdbConnectionInstance).getSchema(anyString());
        doReturn(dbSchema).when(listenableDbSchema).get();

        doNothing().when(ovsdbConnectionInstance).monitorTables(anyString(), any(DatabaseSchema.class));
        ovsdbConnectionInstance.registerCallbacks(instanceIdentifierCodec);
        verify(ovsdbConnectionInstance, times(1)).monitorTables(anyString(), any(DatabaseSchema.class));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testCreateTransactInvokers() throws Exception {
        // transactInvokers not null case
        transactInvokers = new HashMap();
        getField(OvsdbConnectionInstance.class, "transactInvokers").set(ovsdbConnectionInstance , transactInvokers);
        ovsdbConnectionInstance.createTransactInvokers();
        verify(ovsdbConnectionInstance, times(0)).getSchema(anyString());

        // transactInvokers null case
        getField(OvsdbConnectionInstance.class, "transactInvokers").set(ovsdbConnectionInstance , null);

        ListenableFuture<DatabaseSchema> listenableDbSchema = mock(ListenableFuture.class);
        TypedDatabaseSchema dbSchema = mock(TypedDatabaseSchema.class);
        doReturn(listenableDbSchema).when(ovsdbConnectionInstance).getSchema(anyString());
        when(listenableDbSchema.get()).thenReturn(dbSchema);

        ovsdbConnectionInstance.createTransactInvokers();
        verify(ovsdbConnectionInstance).getSchema(anyString());

        Map<DatabaseSchema, TransactInvoker> testTransactInvokers = Whitebox.getInternalState(ovsdbConnectionInstance,
                "transactInvokers");
        assertEquals("Error, size of the hashmap is incorrect", 1, testTransactInvokers.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMonitorAllTables() throws Exception {
        Set<String> tables = new HashSet<>();
        tables.add("Open_vSwitch");
        tables.add("Port");
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        when(dbSchema.getTables()).thenReturn(tables);
        GenericTableSchema tableSchema = mock(GenericTableSchema.class);
        when(dbSchema.table(anyString(), eq(GenericTableSchema.class))).thenReturn(tableSchema);

        Set<String> columns = new HashSet<>();
        columns.add("columnName1");
        columns.add("columnName2");
        columns.add("_version");
        columns.add("statistics");
        when(tableSchema.getColumns()).thenReturn(columns);

        TableUpdates tableUpdates = mock(TableUpdates.class);
        doReturn(tableUpdates).when(ovsdbConnectionInstance).monitor(any(DatabaseSchema.class), any(List.class),
            any(MonitorCallBack.class));
        getField(OvsdbConnectionInstance.class, "callback").set(ovsdbConnectionInstance, callback);
        doNothing().when(callback).update(any(TableUpdates.class), any(DatabaseSchema.class));

        Whitebox.invokeMethod(ovsdbConnectionInstance, "monitorTables", "database", dbSchema);
        verify(ovsdbConnectionInstance, times(1)).monitorTables(anyString(), any(DatabaseSchema.class));
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testOvsdbConnectionInstance() throws Exception {
        getField(OvsdbConnectionInstance.class, "client").set(ovsdbConnectionInstance, client);

        // test getDatabases()
        ListenableFuture<List<String>> listenableFuture = mock(ListenableFuture.class);
        when(client.getDatabases()).thenReturn(listenableFuture);
        assertEquals("Error, did not return correct ListenableFuture<List<String>> object", listenableFuture,
                ovsdbConnectionInstance.getDatabases());
        verify(client).getDatabases();

        // test getSchema()
        ListenableFuture<TypedDatabaseSchema> futureDatabaseSchema = mock(ListenableFuture.class);
        when(client.getSchema(anyString())).thenReturn(futureDatabaseSchema);
        assertEquals("Error, did not return correct ListenableFuture<DatabaseSchema> object", futureDatabaseSchema,
                ovsdbConnectionInstance.getSchema(anyString()));
        verify(client).getSchema(anyString());

        // test transactBuilder()
        TransactionBuilder transactionBuilder = mock(TransactionBuilder.class);
        doReturn(transactionBuilder).when(client).transactBuilder(any(DatabaseSchema.class));
        assertEquals("Error, did not return correct TransactionBuilder object", transactionBuilder,
                ovsdbConnectionInstance.transactBuilder(mock(DatabaseSchema.class)));
        verify(client).transactBuilder(any(DatabaseSchema.class));

        // test transact()
        ListenableFuture<List<OperationResult>> futureOperationResult = mock(ListenableFuture.class);
        doReturn(futureOperationResult).when(client).transact(any(DatabaseSchema.class), any(List.class));
        assertEquals("Error, did not return correct ListenableFuture<List<OperationResult>> object",
                futureOperationResult, ovsdbConnectionInstance.transact(mock(DatabaseSchema.class), mock(List.class)));
        verify(client).transact(any(DatabaseSchema.class), any(List.class));

        // test monitor()
        TableUpdates tableUpdates = mock(TableUpdates.class);
        doReturn(tableUpdates).when(client).monitor(any(DatabaseSchema.class), any(List.class),
            any(MonitorCallBack.class));
        assertEquals("Error, did not return correct TableUpdates object", tableUpdates, ovsdbConnectionInstance
                .monitor(mock(DatabaseSchema.class), mock(List.class), mock(MonitorCallBack.class)));
        verify(client).monitor(any(DatabaseSchema.class), any(List.class), any(MonitorCallBack.class));

        // test cancelMonitor()
        doNothing().when(client).cancelMonitor(any(MonitorHandle.class));
        MonitorHandle monitorHandle = mock(MonitorHandle.class);
        ovsdbConnectionInstance.cancelMonitor(monitorHandle);
        verify(client).cancelMonitor(any(MonitorHandle.class));

        // test lock()
        doNothing().when(client).lock(anyString(), any(LockAquisitionCallback.class), any(LockStolenCallback.class));
        LockAquisitionCallback lockAquisitionCallback = mock(LockAquisitionCallback.class);
        LockStolenCallback lockStolenCallback = mock(LockStolenCallback.class);
        ovsdbConnectionInstance.lock("lockId", lockAquisitionCallback, lockStolenCallback);
        verify(client).lock(anyString(), any(LockAquisitionCallback.class), any(LockStolenCallback.class));

        // test steal()
        ListenableFuture<Boolean> futureBoolean = mock(ListenableFuture.class);
        when(client.steal(anyString())).thenReturn(futureBoolean);
        assertEquals("Error, did not return correct ListenableFuture<Boolean> object", futureBoolean,
                ovsdbConnectionInstance.steal(anyString()));
        verify(client).steal(anyString());

        // test unLock()
        when(client.unLock(anyString())).thenReturn(futureBoolean);
        assertEquals("Error, did not return correct ListenableFuture<Boolean> object", futureBoolean,
                ovsdbConnectionInstance.unLock(anyString()));
        verify(client).unLock(anyString());

        // test isActive()
        when(client.isActive()).thenReturn(true);
        assertEquals("Error, does not match isActive()", true, ovsdbConnectionInstance.isActive());
        verify(client).isActive();

        // test disconnect()
        doNothing().when(client).disconnect();
        ovsdbConnectionInstance.disconnect();
        verify(client).disconnect();

        // test getDatabaseSchema()
        TypedDatabaseSchema databaseSchema = mock(TypedDatabaseSchema.class);
        when(client.getDatabaseSchema(anyString())).thenReturn(databaseSchema);
        assertEquals("Error, did not return correct DatabaseSchema object", databaseSchema,
                ovsdbConnectionInstance.getDatabaseSchema(anyString()));
        verify(client).getDatabaseSchema(anyString());

        // test getConnectionInfo()
        OvsdbConnectionInfo ovsdbConnectionInfo = mock(OvsdbConnectionInfo.class);
        when(client.getConnectionInfo()).thenReturn(ovsdbConnectionInfo);
        assertEquals("Error, did not return correct OvsdbConnectionInfo object", ovsdbConnectionInfo,
                ovsdbConnectionInstance.getConnectionInfo());
        verify(client).getConnectionInfo();

        // test getMDConnectionInfo()
        assertEquals("Error, incorrect connectionInfo", key, ovsdbConnectionInstance.getMDConnectionInfo());

        // test setMDConnectionInfo()
        ovsdbConnectionInstance.setMDConnectionInfo(key);
        assertEquals("Error, incorrect ConnectionInfo", key,
                Whitebox.getInternalState(ovsdbConnectionInstance, "connectionInfo"));

        // test getInstanceIdentifier()
        assertEquals("Error, incorrect instanceIdentifier", instanceIdentifier,
                ovsdbConnectionInstance.getInstanceIdentifier());

        // test getNodeId()
        NodeId nodeId = mock(NodeId.class);
        NodeKey nodeKey = new NodeKey(nodeId);
        doReturn(nodeKey).when(ovsdbConnectionInstance).getNodeKey();
        when(ovsdbConnectionInstance.getNodeKey()).thenReturn(nodeKey);
        assertEquals("Error, incorrect NodeId object", nodeId, ovsdbConnectionInstance.getNodeId());

        // test setInstanceIdentifier()
        ovsdbConnectionInstance.setInstanceIdentifier(instanceIdentifier);
        assertEquals("Error, incorrect instanceIdentifier", instanceIdentifier,
                Whitebox.getInternalState(ovsdbConnectionInstance, "instanceIdentifier"));

        // test monitor()
        assertNull(ovsdbConnectionInstance.monitor(any(DatabaseSchema.class), any(List.class), any(MonitorHandle.class),
                any(MonitorCallBack.class)));
    }
}
