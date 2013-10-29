package org.opendaylight.ovsdb.lib.message;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.message.operations.InsertOperation;
import org.opendaylight.ovsdb.lib.message.operations.MutateOperation;
import org.opendaylight.ovsdb.lib.message.operations.Operation;
import org.opendaylight.ovsdb.lib.message.operations.OperationResult;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;
import org.opendaylight.ovsdb.lib.notation.Mutation;
import org.opendaylight.ovsdb.lib.notation.Mutator;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class OVSDBNettyFactoryTest {
    InventoryService inventoryService;
    private static String bridgeIdentifier = "br1";

    @Test
    public void testSome() throws InterruptedException, ExecutionException {
        ConnectionService connectionService = new ConnectionService();
        connectionService.init();
        inventoryService = new InventoryService();
        inventoryService.init();
        connectionService.setInventoryServiceInternal(inventoryService);
        Node.NodeIDType.registerIDType("OVS", String.class);
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS, "192.168.56.101");
        params.put(ConnectionConstants.PORT, "6634");
        Node node = connectionService.connect("TEST", params);
        if (node == null) {
            System.out.println("ERROR : Unable to connect to the host");
            return;
        }

        Connection connection = connectionService.getConnection(node);
        if (connection == null) {
            System.out.println("ERROR : Unable to connect to the host");
            return;
        }

        OvsdbRPC ovsdb = connection.getRpc();
        if (ovsdb == null) {
            System.out.println("ERROR : Unable to obtain RPC instance");
            return;
        }

        //GET DB-SCHEMA
        List<String> dbNames = Arrays.asList(Open_vSwitch.NAME.getName());
        ListenableFuture<DatabaseSchema> dbSchemaF = ovsdb.get_schema(dbNames);
        DatabaseSchema databaseSchema = dbSchemaF.get();
        System.out.println(databaseSchema);

        //TEST MONITOR
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
            Map<String, Object> vswitchRow = new HashMap<String, Object>();
            UUID bridgeUuidPair = new UUID(newBridge);
            vswitchRow.put("bridges", bridgeUuidPair);
            addSwitchRequest = new InsertOperation(Open_vSwitch.NAME.getName(), newSwitch, vswitchRow);
        }

        Map<String, Object> bridgeRow = new HashMap<String, Object>();
        bridgeRow.put("name", bridgeIdentifier);
        UUID ports = new UUID(newPort);
        bridgeRow.put("ports", ports);
        InsertOperation addBridgeRequest = new InsertOperation(Bridge.NAME.getName(), newBridge, bridgeRow);

        Map<String, Object> portRow = new HashMap<String, Object>();
        portRow.put("name", bridgeIdentifier);
        UUID interfaces = new UUID(newInterface);
        portRow.put("interfaces", interfaces);
        InsertOperation addPortRequest = new InsertOperation(Port.NAME.getName(), newPort, portRow);

        Map<String, Object> interfaceRow = new HashMap<String, Object>();
        interfaceRow.put("name", bridgeIdentifier);
        interfaceRow.put("type", "internal");
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
