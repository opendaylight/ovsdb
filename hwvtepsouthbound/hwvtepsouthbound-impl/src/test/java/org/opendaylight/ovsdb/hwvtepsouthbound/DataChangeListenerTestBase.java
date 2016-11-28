/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvokerImpl;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.operations.Comment;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.powermock.api.support.membermodification.MemberMatcher.field;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

public class DataChangeListenerTestBase extends AbstractDataBrokerTest {

    static Logger LOG = LoggerFactory.getLogger(DataChangeListenerTestBase.class);

    EntityOwnershipService entityOwnershipService;
    OvsdbClient ovsdbClient;
    static DataBroker dataBroker;
    DatabaseSchema dbSchema;
    ListenableFuture<DatabaseSchema> listenableDbSchema = mock(ListenableFuture.class);
    TransactionInvoker transactionInvoker;
    OvsdbConnectionInfo connectionInfo;
    Operations operations;
    HwvtepDataChangeListener hwvtepDataChangeListener;
    HwvtepConnectionManager hwvtepConnectionManager;
    HwvtepConnectionInstance connectionInstance;

    ArgumentCaptor<TypedBaseTable> insertOpCapture;
    ArgumentCaptor<List> transactCaptor;

    String nodeUuid;
    InstanceIdentifier<Node> nodeIid;

    @Before
    public void setupTest() throws Exception {
        /**
         *  Use the same databroker across tests ,otherwise the following exception is thrown
         *  Caused by: java.lang.RuntimeException: org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.
         *  topology.rev131021.node.attributes.SupportingNode$StreamWriter: frozen class (cannot edit)
         */
        if (dataBroker == null) {
            dataBroker = getDataBroker();
        }
        entityOwnershipService = mock(EntityOwnershipService.class);
        loadSchema();
        mockOperations();
        mockConnectionInstance();
        mockConnectionManager();

        nodeUuid = java.util.UUID.randomUUID().toString();
        nodeIid = createInstanceIdentifier(nodeUuid);
        addNode(OPERATIONAL);
        addNode(CONFIGURATION);
        hwvtepDataChangeListener = new HwvtepDataChangeListener(dataBroker, hwvtepConnectionManager);
    }

    @After
    public void tearDown() throws Exception {
        hwvtepDataChangeListener.close();
        deleteNode(OPERATIONAL);
        deleteNode(CONFIGURATION);
    }

    void loadSchema() {
        try (InputStream resourceAsStream = DataChangeListenerTestBase.class.getResourceAsStream("hwvtep_schema.json")) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(resourceAsStream);
            dbSchema = DatabaseSchema.fromJson(HwvtepSchemaConstants.HARDWARE_VTEP,
                    jsonNode.get("result"));
            listenableDbSchema = mock(ListenableFuture.class);
            when(listenableDbSchema.get()).thenReturn(dbSchema);
        } catch (Exception e) {
            LOG.error("Failed to load schema", e);
        }
    }

    private void mockConnectionManager() throws IllegalAccessException {
        hwvtepConnectionManager = PowerMockito.mock(HwvtepConnectionManager.class, Mockito.CALLS_REAL_METHODS);
        field(HwvtepConnectionManager.class, "db").set(hwvtepConnectionManager, dataBroker);
        field(HwvtepConnectionManager.class, "txInvoker").set(hwvtepConnectionManager, transactionInvoker);
        field(HwvtepConnectionManager.class, "entityOwnershipService").set(hwvtepConnectionManager, entityOwnershipService);
        suppress(PowerMockito.method(HwvtepConnectionManager.class, "getConnectionInstance", HwvtepPhysicalSwitchAttributes.class));
        when(hwvtepConnectionManager.getConnectionInstance(Mockito.any(HwvtepPhysicalSwitchAttributes.class))).
                thenReturn(connectionInstance);
        when(hwvtepConnectionManager.getConnectionInstance(Mockito.any(Node.class))).
                thenReturn(connectionInstance);
    }

    void mockConnectionInstance() throws IllegalAccessException {
        connectionInfo = mock(OvsdbConnectionInfo.class);
        ovsdbClient = mock(OvsdbClient.class);
        transactionInvoker =  new TransactionInvokerImpl(dataBroker);
        connectionInstance = PowerMockito.mock(HwvtepConnectionInstance.class, Mockito.CALLS_REAL_METHODS);
        field(HwvtepConnectionInstance.class, "instanceIdentifier").set(connectionInstance, nodeIid);
        field(HwvtepConnectionInstance.class, "txInvoker").set(connectionInstance, transactionInvoker);
        field(HwvtepConnectionInstance.class, "deviceInfo").set(connectionInstance, new HwvtepDeviceInfo());
        field(HwvtepConnectionInstance.class, "client").set(connectionInstance, ovsdbClient);
        when(connectionInstance.getConnectionInfo()).thenReturn(connectionInfo);
        when(connectionInstance.getConnectionInfo().getRemoteAddress()).thenReturn(mock(InetAddress.class));
        when(connectionInstance.getInstanceIdentifier()).thenReturn(nodeIid);
        doReturn(listenableDbSchema).when(connectionInstance).getSchema(anyString());
        connectionInstance.createTransactInvokers();
    }

    void mockOperations() {
        resetOperations();
    }

    /**
     * resets the captures so that we can validate the captors of the immediate next execution
     */
    void resetOperations() {
        insertOpCapture = ArgumentCaptor.forClass(TypedBaseTable.class);
        Insert insert = mock(Insert.class);
        when(insert.withId(any(String.class))).thenReturn(insert);
        Operations.op = PowerMockito.mock(Operations.class);
        when(Operations.op.comment(any(String.class))).thenReturn(mock(Comment.class));
        when(Operations.op.insert(insertOpCapture.capture())).thenReturn(insert);

        ListenableFuture<List<OperationResult>> ft = mock(ListenableFuture.class);
        transactCaptor = ArgumentCaptor.forClass(List.class);
        when(ovsdbClient.transact(any(DatabaseSchema.class), transactCaptor.capture())).thenReturn(ft);
    }

    void addNode(LogicalDatastoreType logicalDatastoreType) throws Exception {
        NodeBuilder nodeBuilder = prepareNode(nodeIid);
        HwvtepGlobalAugmentationBuilder builder = new HwvtepGlobalAugmentationBuilder();
        nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, builder.build());
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(logicalDatastoreType, nodeIid, nodeBuilder.build(), WriteTransaction.CREATE_MISSING_PARENTS);
        transaction.submit();
    }

    void deleteNode(LogicalDatastoreType logicalDatastoreType) {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        tx.delete(logicalDatastoreType, nodeIid);
        tx.submit();
    }

    void addData(LogicalDatastoreType logicalDatastoreType, Class<? extends DataObject> dataObject,
                 String[]... data) {
        NodeBuilder nodeBuilder = prepareNode(nodeIid);
        HwvtepGlobalAugmentationBuilder builder = new HwvtepGlobalAugmentationBuilder();
        if (LogicalSwitches.class == dataObject) {
            TestBuilders.addLogicalSwitches(builder, data);
        }
        if (TerminationPoint.class == dataObject) {
            TestBuilders.addGlobalTerminationPoints(nodeBuilder, nodeIid, data);
        }
        if (RemoteUcastMacs.class == dataObject) {
            TestBuilders.addRemoteUcastMacs(nodeIid, builder, data);
        }
        if (RemoteMcastMacs.class == dataObject) {
            TestBuilders.addRemoteMcastMacs(nodeIid, builder, data);
        }
        nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, builder.build());
        mergeNode(logicalDatastoreType, nodeIid, nodeBuilder);
    }

    NodeBuilder prepareNode(InstanceIdentifier<Node> iid) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(iid.firstKeyOf(Node.class).getNodeId());
        return nodeBuilder;
    }

    Node mergeNode(LogicalDatastoreType datastoreType, InstanceIdentifier<Node> id, NodeBuilder nodeBuilder) {
        Node node = nodeBuilder.build();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.merge(datastoreType, id, node, WriteTransaction.CREATE_MISSING_PARENTS);
        transaction.submit();
        return node;
    }

    public InstanceIdentifier<Node> createInstanceIdentifier(String nodeIdString) {
        NodeId nodeId = new NodeId(new Uri(nodeIdString));
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }
}
