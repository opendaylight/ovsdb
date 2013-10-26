package org.opendaylight.ovsdb.lib.message;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import junit.framework.TestCase;

import org.junit.Test;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcDecoder;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcServiceBinderHandler;
import org.opendaylight.ovsdb.lib.message.EchoResponse;
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.OVSDB;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.message.operations.InsertOperation;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.lib.table.internal.Tables;
import org.opendaylight.ovsdb.plugin.ConnectionService;
import org.opendaylight.ovsdb.plugin.InsertRequest;
import org.opendaylight.ovsdb.plugin.MessageHandler;
import org.opendaylight.ovsdb.plugin.MutateRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class OVSDBNettyFactoryTest {

    @Test
    public void testSome() throws InterruptedException, ExecutionException {

        ConnectionService service = new ConnectionService();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonRpcEndpoint factory = new JsonRpcEndpoint(objectMapper, service);
        JsonRpcServiceBinderHandler binderHandler = new JsonRpcServiceBinderHandler(factory);

        List<ChannelHandler> _handlers = Lists.newArrayList();
        _handlers.add(new LoggingHandler(LogLevel.INFO));
        _handlers.add(new JsonRpcDecoder(100000));
        _handlers.add(new StringEncoder(CharsetUtil.UTF_8));
        _handlers.add(binderHandler);

        service.init();
        service.setHandlers(_handlers);
        String identifier = "TEST";
        Node.NodeIDType.registerIDType("OVS", String.class);
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS, "192.168.56.101");
        params.put(ConnectionConstants.PORT, "6634");
        Node node = service.connect(identifier, params);
        if (node != null) {
            binderHandler.setNode(node);
        }

        OVSDB ovsdb = factory.getClient(node, OVSDB.class);

        //GET DB-SCHEMA
        List<String> dbNames = Arrays.asList("Open_vSwitch");
        ListenableFuture<DatabaseSchema> dbSchemaF = ovsdb.get_schema(dbNames);
        DatabaseSchema databaseSchema = dbSchemaF.get();
        System.out.println(databaseSchema);

        //TEST MONITOR
        MonitorRequestBuilder monitorReq = new MonitorRequestBuilder();
        for (Table table : Tables.getTables()) {
            monitorReq.monitor(table);
        }

        ListenableFuture<TableUpdates> monResponse = ovsdb.monitor(monitorReq);
        System.out.println("Monitor Request sent :");
        TableUpdates updates = monResponse.get();

        Set<Table.Name> available = updates.availableUpdates();
        for (Table.Name name : available) {
            System.out.println(name.getName() +":"+ updates.getUpdate(name).toString());
        }

        // TRANSACT INSERT TEST

        /*
        Map<String, Object> vswitchRow = new HashMap<String, Object>();
        Map<String, Object> bridgeRow = new HashMap<String, Object>();
        bridgeRow.put("name", "br2");
        TransactBuilder transaction = new TransactBuilder();
        InsertOperation addBridge = new InsertOperation("Bridge", "br2", bridgeRow);

        transaction.addOperation(addBridge);

        ListenableFuture<List<Object>> transResponse = ovsdb.transact(transaction);
        System.out.println("Transcation sent :");
        Object tr = transResponse.get();
        System.out.println(tr.toString());
        */

        // TRANSACT MUTATE TEST
        /*
        List<String> bridgeUuidPair = new ArrayList<String>();
        bridgeUuidPair.add("named-uuid");
        bridgeUuidPair.add(newBridge);

        List<Object> mutation = new ArrayList<Object>();
        mutation.add("bridges");
        mutation.add("insert");
        mutation.add(bridgeUuidPair);

        List<Object> mutations = new ArrayList<Object>();
        mutations.add(mutation);

        List<String> ovsUuidPair = new ArrayList<String>();
        ovsUuidPair.add("uuid");
        ovsUuidPair.add(instance.getUuid());

        List<Object> whereInner = new ArrayList<Object>();
        whereInner.add("_uuid");
        whereInner.add("==");
        whereInner.add(ovsUuidPair);

        List<Object> where = new ArrayList<Object>();
        where.add(whereInner);

        addSwitchRequest = new MutateRequest("Open_vSwitch", where, mutations);
         */
        // TEST ECHO
        ListenableFuture<List<String>> some = ovsdb.echo();
        Object s = some.get();
        System.out.printf("Result of echo is %s \n", s);

        // TEST ECHO REQUEST/REPLY

        Thread.sleep(10);
        service.disconnect(node);
    }

}
