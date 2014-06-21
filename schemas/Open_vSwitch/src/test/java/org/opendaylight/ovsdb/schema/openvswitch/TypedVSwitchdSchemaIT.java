/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.schema.openvswitch;

import static org.junit.Assert.assertNull;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableSet;
import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.MonitorHandle;
import org.opendaylight.ovsdb.lib.OvsDBClientImpl;
import org.opendaylight.ovsdb.lib.message.MonitorRequest;
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.MonitorSelect;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;

public class TypedVSwitchdSchemaIT extends OvsdbTestBase {

    Logger logger = LoggerFactory.getLogger(TypedVSwitchdSchemaIT.class);
    OvsDBClientImpl ovs;
    DatabaseSchema dbSchema = null;
    static String testBridgeName = "br_test";
    static UUID testBridgeUuid = null;

    @Test
    public void testTypedBridgeOperations() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.monitorTables();
        this.createTypedBridge();
        this.createTypedController();
        this.testCreateTypedPortandInterface();
        this.testCreateTypedIpFix();
        this.testCreateTypeNetFlow();
        this.testCreateTypeSflow();
    }

    private void createTypedBridge() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Bridge bridge = ovs.createTypedRowWrapper(Bridge.class);
        bridge.setName(testBridgeName);
        bridge.setStatus(ImmutableMap.of("key","value"));
        bridge.setFloodVlans(Sets.newHashSet(34));

        OpenVSwitch openVSwitch = ovs.createTypedRowWrapper(OpenVSwitch.class);
        openVSwitch.setBridges(Sets.newHashSet(new UUID(testBridgeName)));

        int insertOperationIndex = 0;

        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(bridge.getSchema())
                        .withId(testBridgeName)
                        .value(bridge.getNameColumn()))
                .add(op.update(bridge.getSchema())
                        .set(bridge.getStatusColumn())
                        .set(bridge.getFloodVlansColumn())
                        .where(bridge.getNameColumn().getSchema().opEqual(bridge.getName()))
                        .and(bridge.getNameColumn().getSchema().opEqual(bridge.getName())).build())
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getBridgesColumn().getSchema(), Mutator.INSERT,
                                     openVSwitch.getBridgesColumn().getData()));

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        System.out.println("Insert & Update operation results = " + operationResults);
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }
        testBridgeUuid = operationResults.get(insertOperationIndex).getUuid();

        Row bridgeRow = tableCache.get(bridge.getSchema().getName()).get(testBridgeUuid);
        Bridge monitoredBridge = ovs.getTypedRowWrapper(Bridge.class, bridgeRow);
        Assert.assertEquals(monitoredBridge.getNameColumn().getData(), bridge.getNameColumn().getData());
        Assert.assertNotNull(monitoredBridge.getUuid());
        Assert.assertNotNull(monitoredBridge.getVersion());
        Assert.assertNotNull(this.getOpenVSwitchTableUuid());
    }

    private void createTypedController() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Controller controller1 = ovs.createTypedRowWrapper(Controller.class);
        controller1.setTarget("tcp:1.1.1.1:6640");
        Controller controller2 = ovs.createTypedRowWrapper(Controller.class);
        controller2.setTarget("tcp:2.2.2.2:6640");

        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);

        String transactionUuidStr = "controller";
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(controller1.getSchema())
                        .withId(transactionUuidStr)
                        .value(controller1.getTargetColumn()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.INSERT,
                                     Sets.newHashSet(new UUID(transactionUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(testBridgeName))
                        .build());

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        System.out.println("Insert & Mutate operation results for controller1 = " + operationResults);
        // Check for any errors
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }

        Row bridgeRow = tableCache.get(bridge.getSchema().getName()).get(testBridgeUuid);
        Bridge monitoredBridge = ovs.getTypedRowWrapper(Bridge.class, bridgeRow);
        Assert.assertEquals(1, monitoredBridge.getControllerColumn().getData().size());

        transactionBuilder = ovs.transactBuilder()
                .add(op.insert(controller2.getSchema())
                        .withId(transactionUuidStr)
                        .value(controller2.getTargetColumn()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(transactionUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(testBridgeName))
                        .build());

        results = transactionBuilder.execute();
        operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        System.out.println("Insert & Mutate operation results for controller2 = " + operationResults);
        // Check for any errors
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }

        bridgeRow = tableCache.get(bridge.getSchema().getName()).get(testBridgeUuid);
        monitoredBridge = ovs.getTypedRowWrapper(Bridge.class, bridgeRow);
        Assert.assertEquals(2, monitoredBridge.getControllerColumn().getData().size());
    }

    private void testCreateTypedPortandInterface() throws InterruptedException, ExecutionException{
        String portUuidStr = "testPort";
        String intfUuidStr = "testIntf";
        Port port = ovs.createTypedRowWrapper(Port.class);
        port.setName("testPort");
        port.setTag(ImmutableSet.of(BigInteger.ONE));
        port.setMac(ImmutableSet.of("00:00:00:00:00:01"));
        port.setInterfaces(ImmutableSet.of(new UUID(intfUuidStr)));

        Interface intf = ovs.createTypedRowWrapper(Interface.class);
        intf.setName(port.getNameColumn().getData());
        intf.setExternalIds(ImmutableMap.of("vm-id", "12345abcedf78910"));

        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(port.getSchema())
                        .withId(portUuidStr)
                        .value(port.getNameColumn())
                        .value(port.getMacColumn()))
                .add(op.insert(intf.getSchema())
                        .withId(intfUuidStr)
                        .value(intf.getNameColumn()))
                .add(op.update(port.getSchema())
                        .set(port.getTagColumn())
                        .set(port.getMacColumn())
                        .set(port.getInterfacesColumn())
                        .where(port.getNameColumn().getSchema().opEqual(port.getName()))
                        .build())
                .add(op.update(intf.getSchema())
                        .set(intf.getExternalIdsColumn())
                        .where(intf.getNameColumn().getSchema().opEqual(intf.getName()))
                        .build())
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getPortsColumn().getSchema(), Mutator.INSERT, Sets.newHashSet(new UUID(portUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(testBridgeName))
                        .build());
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        System.out.println("Insert & Mutate operation results for Port and Interface = " + operationResults);
    }

    private void testCreateTypedIpFix() throws InterruptedException, ExecutionException, IllegalArgumentException{
        String ipfixUuidStr = "testIpfix";
        String ipfixTargert = "172.16.20.1:4739";
        Integer obsDomainId = 112;
        Integer obsPointId = 358;
        Integer cacheMax = 132;
        Integer cacheTimeout = 134;
        Integer sampling = 558;
        IPFIX ipfix = ovs.createTypedRowWrapper(IPFIX.class);
        ipfix.setTargets(ImmutableSet.of(ipfixTargert));
        ipfix.setObsDomainId(ImmutableSet.of(obsDomainId));
        ipfix.setObsPointId(ImmutableSet.of(obsPointId));
        ipfix.setCacheMaxFlows(ImmutableSet.of(cacheMax));
        ipfix.setCacheActiveTimeout(ImmutableSet.of(cacheTimeout));
        ipfix.setSampling(ImmutableSet.of(sampling));
        ipfix.setExternalIds(ImmutableMap.of("<3", "ovs"));
        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(ipfix.getSchema())
                        .withId(ipfixUuidStr)
                        .value(ipfix.getTargetsColumn())
                        .value(ipfix.getObsDomainIdColumn())
                        .value(ipfix.getObsPointIdColumn())
                        .value(ipfix.getCacheMaxFlowsColumn())
                        .value(ipfix.getCacheActiveTimeoutColumn())
                        .value(ipfix.getSamplingColumn())
                        .value(ipfix.getExternalIdsColumn()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getIpfixColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(ipfixUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(testBridgeName))
                        .build());
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        for (OperationResult result : operationResults) Assert.assertNull(result.getError());
        logger.info("Insert & Mutate operation results for IPFIX = {} ", operationResults);
    }

    private void testCreateTypeSflow() throws InterruptedException, ExecutionException, IllegalArgumentException{
        String sFlowUuidStr = "testSFlow";
        String sFlowTarget = "172.16.20.200:6343";
        Integer header = 128;
        Integer obsPointId = 358;
        Integer polling = 10;
        String agent = "172.16.20.210";
        Integer sampling = 64;
        SFlow sFlow = ovs.createTypedRowWrapper(SFlow.class);
        sFlow.setTargets(ImmutableSet.of(sFlowTarget));
        sFlow.setHeader(ImmutableSet.of(header));
        sFlow.setPolling(ImmutableSet.of(obsPointId));
        sFlow.setPolling(ImmutableSet.of(polling));
        sFlow.setAgent(ImmutableSet.of(agent));
        sFlow.setSampling(ImmutableSet.of(sampling));
        sFlow.setExternalIds(ImmutableMap.of("kit", "tah"));
        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(sFlow.getSchema())
                        .withId(sFlowUuidStr)
                        .value(sFlow.getTargetsColumn())
                        .value(sFlow.getHeaderColumn())
                        .value(sFlow.getPollingColumn())
                        .value(sFlow.getAgentColumn())
                        .value(sFlow.getSamplingColumn())
                        .value(sFlow.getExternalIdsColumn()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getSflowColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(sFlowUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(testBridgeName))
                        .build());
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        for (OperationResult result : operationResults) {
            assertNull(result.getError());
        }
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Insert & Mutate operation results for SFlow = {} ", operationResults);
    }

    private void testCreateTypeNetFlow() throws InterruptedException, ExecutionException, IllegalArgumentException{
        String netFlowUuidStr = "testNetFlow";
        String sFlowTargets = "172.16.20.200:6343";
        BigInteger engineType = BigInteger.valueOf(128);
        BigInteger engineID = BigInteger.valueOf(32);
        Integer activityTimeout = 1;
        NetFlow netFlow = ovs.createTypedRowWrapper(NetFlow.class);
        netFlow.setTargets(ImmutableSet.of(sFlowTargets));
        netFlow.setEngineType(ImmutableSet.of(engineType));
        netFlow.setEngineId(ImmutableSet.of(engineID));
        netFlow.setActivityTimeout(ImmutableSet.of(activityTimeout));
        netFlow.setExternalIds(ImmutableMap.of("big", "baby"));
        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(netFlow.getSchema())
                        .withId(netFlowUuidStr)
                        .value(netFlow.getTargetsColumn())
                        .value(netFlow.getEngineTypeColumn())
                        .value(netFlow.getEngineIdColumn())
                        .value(netFlow.getActiveTimeoutColumn())
                        .value(netFlow.getExternalIdsColumn()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getNetflowColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(netFlowUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(testBridgeName))
                        .build());
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        for (OperationResult result : operationResults) Assert.assertNull(result.getError());
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Insert & Mutate operation results for NetFlow = {} ", operationResults);
    }

    public void testGetDBs() throws ExecutionException, InterruptedException {
        ListenableFuture<List<String>> databases = ovs.getDatabases();
        List<String> dbNames = databases.get();
        Assert.assertNotNull(dbNames);
        boolean hasOpenVswitchSchema = false;
        for(String dbName : dbNames) {
           if (dbName.equals(OPEN_VSWITCH_SCHEMA)) {
                hasOpenVswitchSchema = true;
                break;
           }
        }
        Assert.assertTrue(OPEN_VSWITCH_SCHEMA+" schema is not supported by the switch", hasOpenVswitchSchema);
    }

    private UUID getOpenVSwitchTableUuid() {
        OpenVSwitch openVSwitch = ovs.getTypedRowWrapper(OpenVSwitch.class, null);
        Map<UUID, Row> ovsTable = tableCache.get(openVSwitch.getSchema().getName());
        if (ovsTable != null) {
            if (ovsTable.keySet().size() >= 1) {
                return (UUID)ovsTable.keySet().toArray()[0];
            }
        }
        return null;
    }

    public void monitorTables() throws ExecutionException, InterruptedException, IOException {
        Assert.assertNotNull(dbSchema);

        List<MonitorRequest<GenericTableSchema>> monitorRequests = Lists.newArrayList();
        monitorRequests.add(this.getAllColumnsMonitorRequest(Bridge.class));
        monitorRequests.add(this.getAllColumnsMonitorRequest(OpenVSwitch.class));

        MonitorHandle monitor = ovs.monitor(dbSchema, monitorRequests, new UpdateMonitor());
        Assert.assertNotNull(monitor);
    }

    /**
     * As per RFC 7047, section 4.1.5, if a Monitor request is sent without any columns, the update response will not include
     * the _uuid column.
     * ----------------------------------------------------------------------------------------------------------------------------------
     * Each <monitor-request> specifies one or more columns and the manner in which the columns (or the entire table) are to be monitored.
     * The "columns" member specifies the columns whose values are monitored. It MUST NOT contain duplicates.
     * If "columns" is omitted, all columns in the table, except for "_uuid", are monitored.
     * ----------------------------------------------------------------------------------------------------------------------------------
     * In order to overcome this limitation, this method
     *
     * @return MonitorRequest that includes all the Bridge Columns including _uuid
     */
    public <T extends TypedBaseTable> MonitorRequest<GenericTableSchema> getAllColumnsMonitorRequest (Class <T> klazz) {
        TypedBaseTable table = ovs.createTypedRowWrapper(klazz);
        GenericTableSchema bridgeSchema = table.getSchema();
        Set<String> columns = bridgeSchema.getColumns();
        MonitorRequestBuilder<GenericTableSchema> bridgeBuilder = MonitorRequestBuilder.builder(table.getSchema());
        for (String column : columns) {
            bridgeBuilder.addColumn(column);
        }
        return bridgeBuilder.with(new MonitorSelect(true, true, true, true)).build();
    }

    static Map<String, Map<UUID, Row>> tableCache = new HashMap<String, Map<UUID, Row>>();
    private static class UpdateMonitor implements MonitorCallBack {
        @Override
        public void update(TableUpdates result) {
            for (String tableName : result.getUpdates().keySet()) {
                Map<UUID, Row> tUpdate = tableCache.get(tableName);
                TableUpdate update = result.getUpdates().get(tableName);
                if (update.getNew() != null) {
                    if (tUpdate == null) {
                        tUpdate = new HashMap<UUID, Row>();
                        tableCache.put(tableName, tUpdate);
                    }
                    tUpdate.put(update.getUuid(), update.getNew());
                } else {
                    tUpdate.remove(update.getUuid());
                }
            }
        }

        @Override
        public void exception(Throwable t) {
            System.out.println("Exception t = " + t);
        }
    }

    @Before
    public  void setUp() throws IOException, ExecutionException, InterruptedException {
        if (ovs != null) {
            return;
        }
        OvsdbRPC rpc = getTestConnection();
        if (rpc == null) {
            System.out.println("Unable to Establish Test Connection");
        }
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        ovs = new OvsDBClientImpl(rpc, executorService);
        testGetDBs();
        dbSchema = ovs.getSchema(OPEN_VSWITCH_SCHEMA, true).get();
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);
        OpenVSwitch openVSwitch = ovs.getTypedRowWrapper(OpenVSwitch.class, null);

        ListenableFuture<List<OperationResult>> results = ovs.transactBuilder()
                .add(op.delete(bridge.getSchema())
                        .where(bridge.getNameColumn().getSchema().opEqual(testBridgeName))
                        .build())
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getBridgesColumn().getSchema(), Mutator.DELETE, Sets.newHashSet(testBridgeUuid)))
                .add(op.commit(true))
                .execute();

        List<OperationResult> operationResults = results.get();
        System.out.println("Delete operation results = " + operationResults);
        tableCache = new HashMap<String, Map<UUID, Row>>();
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
