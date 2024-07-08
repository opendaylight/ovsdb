/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvokerImpl;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.operations.Delete;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.Select;
import org.opendaylight.ovsdb.lib.operations.Update;
import org.opendaylight.ovsdb.lib.operations.Where;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionHistory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataChangeListenerTestBase extends AbstractDataBrokerTest {
    private static final Logger LOG = LoggerFactory.getLogger(DataChangeListenerTestBase.class);

    // Hack to hack into Field.class for now
    private static final VarHandle MODIFIERS;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
            MODIFIERS = lookup.findVarHandle(Field.class, "modifiers", int.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    EntityOwnershipService entityOwnershipService;
    OvsdbClient ovsdbClient;
    TypedDatabaseSchema dbSchema;
    ListenableFuture<TypedDatabaseSchema> listenableDbSchema = mock(ListenableFuture.class);
    TransactionInvoker transactionInvoker;
    OvsdbConnectionInfo connectionInfo;
    Operations operations;
    HwvtepDataChangeListener hwvtepDataChangeListener;
    protected HwvtepConnectionManager hwvtepConnectionManager;
    protected HwvtepConnectionInstance connectionInstance;

    ArgumentCaptor<TypedBaseTable> insertOpCapture;
    ArgumentCaptor<List> transactCaptor;

    String nodeUuid;
    protected InstanceIdentifier<Node> nodeIid;
    InstanceIdentifier<LogicalSwitches> ls0Iid;
    InstanceIdentifier<LogicalSwitches> ls1Iid;

    Operations mockOp;

    @Before
    public void setupTest() throws Exception {
        entityOwnershipService = mock(EntityOwnershipService.class);
        nodeUuid = java.util.UUID.randomUUID().toString();
        nodeIid = createInstanceIdentifier(nodeUuid);
        ls0Iid = nodeIid.augmentation(HwvtepGlobalAugmentation.class).child(LogicalSwitches.class,
                new LogicalSwitchesKey(new HwvtepNodeName("ls0")));
        ls1Iid = nodeIid.augmentation(HwvtepGlobalAugmentation.class).child(LogicalSwitches.class,
                new LogicalSwitchesKey(new HwvtepNodeName("ls1")));
        loadSchema();

        mockOp = mock(Operations.class);
        transactionInvoker = new TransactionInvokerImpl(getDataBroker());
        hwvtepConnectionManager = spy(new HwvtepConnectionManager(getDataBroker(), transactionInvoker, mockOp,
            entityOwnershipService, mock(OvsdbConnection.class)));

        connectionInfo = mock(OvsdbConnectionInfo.class);
        doReturn(mock(InetAddress.class)).when(connectionInfo).getRemoteAddress();

        ovsdbClient = mock(OvsdbClient.class);
        doReturn(true).when(ovsdbClient).isActive();
        doReturn(connectionInfo).when(ovsdbClient).getConnectionInfo();
        doReturn(listenableDbSchema).when(ovsdbClient).getSchema(anyString());

        connectionInstance = new HwvtepConnectionInstance(hwvtepConnectionManager, null, ovsdbClient, nodeIid,
            transactionInvoker, getDataBroker(), mockOp);
        connectionInstance.reconciliationFt.set(Boolean.TRUE);
        connectionInstance.firstUpdateTriggered.set(true);
        connectionInstance.setControllerTxHistory(new TransactionHistory(10000, 7500));
        connectionInstance.setDeviceUpdateHistory(new TransactionHistory(10000, 7500));
        connectionInstance.createTransactInvokers();

        doReturn(connectionInstance).when(hwvtepConnectionManager).getConnectionInstance(
            any(HwvtepPhysicalSwitchAttributes.class));
        doReturn(connectionInstance).when(hwvtepConnectionManager).getConnectionInstance(any(Node.class));
        doReturn(connectionInstance).when(hwvtepConnectionManager).getConnectionInstanceFromNodeIid(
            any(InstanceIdentifier.class));


        mockOperations();

        addNode(LogicalDatastoreType.OPERATIONAL);
        addNode(LogicalDatastoreType.CONFIGURATION);
        hwvtepDataChangeListener = new HwvtepDataChangeListener(getDataBroker(), hwvtepConnectionManager);
    }

    @After
    public void tearDown() throws Exception {
        hwvtepDataChangeListener.close();
        deleteNode(LogicalDatastoreType.OPERATIONAL);
        deleteNode(LogicalDatastoreType.CONFIGURATION);
    }

    protected static final void setFinalStatic(final Class<?> cls, final String fieldName, final Object newValue)
            throws SecurityException, ReflectiveOperationException {
        Field[] fields = FieldUtils.getAllFields(cls);
        for (Field field : fields) {
            if (fieldName.equals(field.getName())) {
                field.setAccessible(true);

                final int mods = field.getModifiers();
                if (Modifier.isFinal(mods)) {
                    MODIFIERS.set(field, mods & ~Modifier.FINAL);
                }

                field.set(null, newValue);
                break;
            }
        }
    }

    void loadSchema() {
        try (InputStream resourceAsStream = DataChangeListenerTestBase.class.getResourceAsStream(
                "hwvtep_schema.json")) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(resourceAsStream);
            dbSchema = TypedDatabaseSchema.of(DatabaseSchema.fromJson(HwvtepSchemaConstants.HARDWARE_VTEP,
                    jsonNode.get("result")));
            listenableDbSchema = mock(ListenableFuture.class);
            doReturn(dbSchema).when(listenableDbSchema).get();
        } catch (IOException | ExecutionException | InterruptedException e) {
            LOG.error("Failed to load schema", e);
        }
    }

    void mockOperations() {
        resetOperations();
    }

    /**
     * Resets the captures so that we can validate the captors of the immediate next execution.
     */
    void resetOperations() {
        reset(mockOp);

        insertOpCapture = ArgumentCaptor.forClass(TypedBaseTable.class);
        Delete delete = mock(Delete.class);
        Where where = mock(Where.class);
        doReturn(where).when(delete).where(any());
        Insert insert = mock(Insert.class);
        doReturn(insert).when(insert).withId(any(String.class));

        doReturn(insert).when(mockOp).insert(insertOpCapture.capture());
        Update update = mock(Update.class);
        doReturn(update).when(mockOp).update(insertOpCapture.capture());
        Select select = mock(Select.class);
        doReturn(select).when(mockOp).select(any(GenericTableSchema.class));
        doReturn(where).when(update).where(any());
        doReturn(delete).when(mockOp).delete(any());

        ListenableFuture<List<OperationResult>> ft = mock(ListenableFuture.class);
        transactCaptor = ArgumentCaptor.forClass(List.class);
        doReturn(ft).when(ovsdbClient).transact(any(DatabaseSchema.class), transactCaptor.capture());
    }

    void addNode(final LogicalDatastoreType logicalDatastoreType) throws Exception {
        NodeBuilder nodeBuilder = prepareNode(nodeIid).addAugmentation(new HwvtepGlobalAugmentationBuilder().build());
        WriteTransaction transaction = getDataBroker().newWriteOnlyTransaction();
        transaction.mergeParentStructurePut(logicalDatastoreType, nodeIid, nodeBuilder.build());
        transaction.commit();
    }

    void deleteNode(final LogicalDatastoreType logicalDatastoreType) {
        ReadWriteTransaction tx = getDataBroker().newReadWriteTransaction();
        tx.delete(logicalDatastoreType, nodeIid);
        tx.commit();
    }

    protected final Node addData(final LogicalDatastoreType logicalDatastoreType,
            final Class<? extends DataObject> dataObject, final String[]... data) {
        NodeBuilder nodeBuilder = prepareNode(nodeIid);
        HwvtepGlobalAugmentationBuilder builder = new HwvtepGlobalAugmentationBuilder();
        if (LogicalSwitches.class == dataObject) {
            builder.setLogicalSwitches(TestBuilders.logicalSwitches(data));
        }
        if (TerminationPoint.class == dataObject) {
            nodeBuilder.setTerminationPoint(TestBuilders.globalTerminationPoints(nodeIid, data));
        }
        if (RemoteUcastMacs.class == dataObject) {
            builder.setRemoteUcastMacs(TestBuilders.remoteUcastMacs(nodeIid, data));
        }
        if (RemoteMcastMacs.class == dataObject) {
            builder.setRemoteMcastMacs(TestBuilders.remoteMcastMacs(nodeIid, data));
        }
        nodeBuilder.addAugmentation(builder.build());
        return mergeNode(logicalDatastoreType, nodeIid, nodeBuilder);
    }

    void deleteData(final LogicalDatastoreType datastoreType, final InstanceIdentifier<?>... iids) {
        WriteTransaction transaction = getDataBroker().newWriteOnlyTransaction();
        for (InstanceIdentifier<?> id : iids) {
            transaction.delete(datastoreType, id);
        }
        transaction.commit();
    }

    void deleteData(final LogicalDatastoreType logicalDatastoreType, final Class<? extends DataObject> dataObject,
            final String[]... data) {
        ReadWriteTransaction tx = getDataBroker().newReadWriteTransaction();
        if (LogicalSwitches.class == dataObject) {
            for (LogicalSwitchesKey key : TestBuilders.logicalSwitches(data).keySet()) {
                tx.delete(logicalDatastoreType,
                    nodeIid.augmentation(HwvtepGlobalAugmentation.class).child(LogicalSwitches.class, key));
            }
        }
        if (TerminationPoint.class == dataObject) {
            // FIXME: this is a no-op
            prepareNode(nodeIid)
                .setTerminationPoint(TestBuilders.globalTerminationPoints(nodeIid, data));
        }
        if (RemoteUcastMacs.class == dataObject) {
            for (RemoteUcastMacsKey key : TestBuilders.remoteUcastMacs(nodeIid, data).keySet()) {
                tx.delete(logicalDatastoreType,
                    nodeIid.augmentation(HwvtepGlobalAugmentation.class).child(RemoteUcastMacs.class, key));
            }
        }
        if (RemoteMcastMacs.class == dataObject) {
            for (RemoteMcastMacsKey key : TestBuilders.remoteMcastMacs(nodeIid, data).keySet()) {
                tx.delete(logicalDatastoreType,
                    nodeIid.augmentation(HwvtepGlobalAugmentation.class).child(RemoteMcastMacs.class, key));
            }
        }
        tx.commit();
    }

    NodeBuilder prepareNode(final InstanceIdentifier<Node> iid) {
        return new NodeBuilder().setNodeId(iid.firstKeyOf(Node.class).getNodeId());
    }

    Node mergeNode(final LogicalDatastoreType datastoreType, final InstanceIdentifier<Node> id,
            final NodeBuilder nodeBuilder) {
        Node node = nodeBuilder.build();
        WriteTransaction transaction = getDataBroker().newWriteOnlyTransaction();
        transaction.mergeParentStructureMerge(datastoreType, id, node);
        transaction.commit();
        return node;
    }

    public InstanceIdentifier<Node> createInstanceIdentifier(final String nodeIdString) {
        NodeId nodeId = new NodeId(new Uri(nodeIdString));
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }
}
