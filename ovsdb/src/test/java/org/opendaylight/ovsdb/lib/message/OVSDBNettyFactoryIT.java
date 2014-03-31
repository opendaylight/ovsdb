/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Madhu Venugopal, Aswin Raveendran
 */
package org.opendaylight.ovsdb.lib.message;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections.MapUtils;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.message.operations.InsertOperation;
import org.opendaylight.ovsdb.lib.message.operations.MutateOperation;
import org.opendaylight.ovsdb.lib.message.operations.Operation;
import org.opendaylight.ovsdb.lib.message.operations.OperationResult;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;
import org.opendaylight.ovsdb.lib.notation.Mutation;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.lib.table.internal.Tables;
import org.opendaylight.ovsdb.plugin.Connection;
import org.opendaylight.ovsdb.plugin.ConnectionService;
import org.opendaylight.ovsdb.plugin.InventoryService;

import com.google.common.util.concurrent.ListenableFuture;

public class OVSDBNettyFactoryIT {
    InventoryService inventoryService;
    private static String bridgeIdentifier = "br1";
    private Properties props;

    @Before
    public void initialize() throws IOException {
        InputStream is = this.getClass().getClassLoader()
                .getResourceAsStream(
                        "org/opendaylight/ovsdb/lib/message/integration-test.properties");
        if (is == null) {
            throw new IOException("Unable to load integration-test.properties");
        }
        props = new Properties();
        props.load(is);

    }

    @Test
    public void testSome() throws InterruptedException, ExecutionException,
            IOException {
        ConnectionService connectionService = new ConnectionService();
        connectionService.init();
        inventoryService = new InventoryService();
        inventoryService.init();
        connectionService.setInventoryServiceInternal(inventoryService);
        Node.NodeIDType.registerIDType("OVS", String.class);
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS,
                props.getProperty("ovsdbserver.ipaddress"));
        params.put(ConnectionConstants.PORT,
                props.getProperty("ovsdbserver.port", "6640"));
        Node node = connectionService.connect("TEST", params);
        if (node == null) {
            throw new IOException("Unable to connect to the host");
        }

        Connection connection = connectionService.getConnection(node);
        if (connection == null) {
            throw new IOException("Unable to connect to the host");
        }

        OvsdbRPC ovsdb = connection.getRpc();
        if (ovsdb == null) {
            throw new IOException("Unable to obtain RPC instance");
        }

        //GET DB-SCHEMA
        List<String> dbNames = Arrays.asList(Open_vSwitch.NAME.getName());
        ListenableFuture<DatabaseSchema> dbSchemaF = null; //ovsdb.get_schema(dbNames);
        DatabaseSchema databaseSchema = dbSchemaF.get();
        MapUtils.debugPrint(System.out, null, databaseSchema.getTables());

        // TEST MONITOR
        // YES it is expected to fail with "duplicate monitor ID" as we have a perpetual monitor in Inventory Service
        MonitorRequestBuilder monitorReq = new MonitorRequestBuilder();
        for (Table<?> table : Tables.getTables()) {
            monitorReq.monitor(table);
        }

        ListenableFuture<TableUpdates> monResponse = ovsdb.monitor(monitorReq);
        System.out.println("Monitor Request sent :");
        TableUpdates updates = monResponse.get();
        inventoryService.processTableUpdates(node, updates);
        inventoryService.printCache(node);

        // TRANSACT INSERT TEST

        Map<String, Table<?>> ovsTable = inventoryService.getTableCache(node, Open_vSwitch.NAME.getName());
        String newBridge = "new_bridge";
        String newInterface = "new_interface";
        String newPort = "new_port";
        String newSwitch = "new_switch";

        Operation addSwitchRequest = null;

        if(ovsTable != null){
            String ovsTableUUID = (String) ovsTable.keySet().toArray()[0];
            UUID bridgeUuidPair = new UUID(newBridge);
            Mutation bm = new Mutation("bridges", Mutator.INSERT, bridgeUuidPair);
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(bm);

            UUID uuid = new UUID(ovsTableUUID);
            Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            addSwitchRequest = new MutateOperation(Open_vSwitch.NAME.getName(), where, mutations);
        }
        else{
            Open_vSwitch ovsTableRow = new Open_vSwitch();
            OvsDBSet<UUID> bridges = new OvsDBSet<UUID>();
            UUID bridgeUuidPair = new UUID(newBridge);
            bridges.add(bridgeUuidPair);
            ovsTableRow.setBridges(bridges);
            addSwitchRequest = new InsertOperation(Open_vSwitch.NAME.getName(), newSwitch, ovsTableRow);
        }

        Bridge bridgeRow = new Bridge();
        bridgeRow.setName(bridgeIdentifier);
        OvsDBSet<UUID> ports = new OvsDBSet<UUID>();
        UUID port = new UUID(newPort);
        ports.add(port);
        bridgeRow.setPorts(ports);
        InsertOperation addBridgeRequest = new InsertOperation(Bridge.NAME.getName(), newBridge, bridgeRow);

        Port portRow = new Port();
        portRow.setName(bridgeIdentifier);
        OvsDBSet<UUID> interfaces = new OvsDBSet<UUID>();
        UUID interfaceid = new UUID(newInterface);
        interfaces.add(interfaceid);
        portRow.setInterfaces(interfaces);
        InsertOperation addPortRequest = new InsertOperation(Port.NAME.getName(), newPort, portRow);

        Interface interfaceRow = new Interface();
        interfaceRow.setName(bridgeIdentifier);
        interfaceRow.setType("internal");
        InsertOperation addIntfRequest = new InsertOperation(Interface.NAME.getName(), newInterface, interfaceRow);

        TransactBuilder transaction = new TransactBuilder();
        transaction.addOperations(new ArrayList<Operation>(
                Arrays.asList(addSwitchRequest, addIntfRequest, addPortRequest, addBridgeRequest)));

        ListenableFuture<List<OperationResult>> transResponse = ovsdb.transact(transaction);
        System.out.println("Transcation sent :");
        List<OperationResult> tr = transResponse.get();
        System.out.println("Transaction response : "+transResponse.toString());
        List<Operation> requests = transaction.getRequests();
        for (int i = 0; i < tr.size() ; i++) {
            if (i < requests.size()) requests.get(i).setResult(tr.get(i));
        }

        System.out.println("Request + Response : "+requests.toString());
        if (tr.size() > requests.size()) {
            System.out.println("ERROR : "+tr.get(tr.size()-1).getError());
            System.out.println("Details : "+tr.get(tr.size()-1).getDetails());
        }

        // TEST ECHO

        ListenableFuture<List<String>> some = ovsdb.echo();
        Object s = some.get();
        System.out.printf("Result of echo is %s \n", s);

        // TEST ECHO REQUEST/REPLY
        Thread.sleep(10000);

        connectionService.disconnect(node);
    }
}
