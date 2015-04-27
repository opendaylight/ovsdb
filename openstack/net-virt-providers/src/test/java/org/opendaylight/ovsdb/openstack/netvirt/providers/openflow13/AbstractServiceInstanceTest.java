/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
/* TODO SB_MIGRATION */
//import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
//import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * Unit test for {@link AbstractServiceInstance}
 */
/* TODO SB_MIGRATION */
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class AbstractServiceInstanceTest {

    @InjectMocks AbstractServiceInstance abstractServiceInstance = mock(AbstractServiceInstance.class, Mockito.CALLS_REAL_METHODS);

    /* TODO SB_MIGRATION */
    //@Mock private OvsdbConfigurationService ovsdbConfigService;
    //@Mock private OvsdbConnectionService connectionService;
    @Mock private PipelineOrchestrator orchestrator;
    @Mock private MdsalConsumer mdsalConsumer;


    private Service service = Service.L3_FORWARDING;

    private final String ID = "5710881121";
    private final String NODE_ID = Constants.INTEGRATION_BRIDGE + ":" +  ID;
    private final String DPID = "154652161";

    /**
     * Test method {@link AbstractServiceInstance#isBridgeInPipeline(String)}
     */
    @Test
    public void testIsBridgeInPipeline() {
        Node node = mock(Node.class);
        when(node.getId()).thenReturn(mock(NodeId.class));

        List<Node> nodes = new ArrayList();
        nodes.add(node);
        /* TODO SB_MIGRATION */
        //when(connectionService.getNodes()).thenReturn(nodes);

        ConcurrentMap<String, Row> bridges = new ConcurrentHashMap();
        bridges.put("key", mock(Row.class));
        //when(ovsdbConfigService.getRows(any(Node.class), anyString())).thenReturn(bridges);

        Bridge bridge = mock(Bridge.class);
        Column<GenericTableSchema, Set<String>> datapathIdColumn = mock(Column.class);
        when(bridge.getDatapathIdColumn()).thenReturn(datapathIdColumn);
        when(bridge.getName()).thenReturn(Constants.INTEGRATION_BRIDGE);
        Set<String> dpids = new HashSet();
        dpids.add(DPID);
        when(datapathIdColumn.getData()).thenReturn(dpids);
        //when(ovsdbConfigService.getTypedRow(any(Node.class), same(Bridge.class), any(Row.class))).thenReturn(bridge);

        /* TODO SB_MIGRATION */
        //assertTrue("Error, isBridgeInPipeline() did not return the correct value", abstractServiceInstance.isBridgeInPipeline(NODE_ID));
    }

    /**
     * Test method {@link AbstractServiceInstance#getTable()}
     */
    @Test
    public void testGetTable() {
        abstractServiceInstance.setService(service);
        assertEquals("Error, getTable() did not return the correct value", 70, abstractServiceInstance.getTable());
    }

    /**
     * Test method {@link AbstractServiceInstance#createNodeBuilder(String)}
     */
    @Test
    public void testCreateNodeBuilder() {
        NodeId id = new NodeId(NODE_ID);

        NodeBuilder nodeBuilder = abstractServiceInstance.createNodeBuilder(NODE_ID);
        assertNotNull("Error, createNodeBuilder() did not return the correct value", nodeBuilder);
        assertEquals("Error, createNodeBuilder() did not return the correct ID", id, nodeBuilder.getId());
        assertEquals("Error, createNodeBuilder() did not return the correct Key", new NodeKey(id), nodeBuilder.getKey());
    }

    /**
     * Test method {@link AbstractServiceInstance#getMutablePipelineInstructionBuilder()}
     */
    @Test
    public void testGetMutablePipelineInstructionBuilder() {
        // service == null
        assertNotNull("Error, getMutablePipelineInstructionBuilder() did not return the correct value", abstractServiceInstance.getMutablePipelineInstructionBuilder());
        assertTrue("Error, getMutablePipelineInstructionBuilder() did not return a InstructionBuilder object", abstractServiceInstance.getMutablePipelineInstructionBuilder() instanceof InstructionBuilder);

        when(orchestrator.getNextServiceInPipeline(any(Service.class))).thenReturn(Service.ARP_RESPONDER);

        // service defined
        assertNotNull("Error, getMutablePipelineInstructionBuilder() did not return the correct value", abstractServiceInstance.getMutablePipelineInstructionBuilder());
        assertTrue("Error, getMutablePipelineInstructionBuilder() did not return a InstructionBuilder object", abstractServiceInstance.getMutablePipelineInstructionBuilder() instanceof InstructionBuilder);
    }

    /**
     * Test method {@link AbstractServiceInstance#writeFlow(FlowBuilder, NodeBuilder)}
     */
    @Test
    public void testWriteFlow() throws Exception {
        DataBroker dataBrocker = mock(DataBroker.class);
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        when(dataBrocker.newReadWriteTransaction()).thenReturn(transaction);
        when(mdsalConsumer.getDataBroker()).thenReturn(dataBrocker);
        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = mock(CheckedFuture.class);
        when(transaction.submit()).thenReturn(commitFuture);

        NodeBuilder nodeBuilder = mock(NodeBuilder.class);
        when(nodeBuilder.getKey()).thenReturn(mock(NodeKey.class));

        FlowBuilder flowBuilder = mock(FlowBuilder.class);
        when(flowBuilder.getKey()).thenReturn(mock(FlowKey.class));

        abstractServiceInstance.writeFlow(flowBuilder, nodeBuilder);

        verify(transaction, times(2)).put(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class), any(DataObject.class), eq(true));
        verify(commitFuture, times(1)).get();
    }

    /**
     * Test method {@link AbstractServiceInstance#removeFlow(FlowBuilder, NodeBuilder)}
     */
    @Test
    public void testRemoveFlow() throws Exception {
        DataBroker dataBrocker = mock(DataBroker.class);
        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBrocker.newWriteOnlyTransaction()).thenReturn(transaction);
        when(mdsalConsumer.getDataBroker()).thenReturn(dataBrocker);
        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = mock(CheckedFuture.class);
        when(transaction.submit()).thenReturn(commitFuture);

        NodeBuilder nodeBuilder = mock(NodeBuilder.class);
        when(nodeBuilder.getKey()).thenReturn(mock(NodeKey.class));

        FlowBuilder flowBuilder = mock(FlowBuilder.class);
        when(flowBuilder.getKey()).thenReturn(mock(FlowKey.class));

        abstractServiceInstance.removeFlow(flowBuilder, nodeBuilder);
        verify(transaction, times(1)).delete(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class));
        verify(commitFuture, times(1)).get();
    }

    /**
     * Test method {@link AbstractServiceInstance#getFlow(FlowBuilder, NodeBuilder)}
     */
    @Test
    public void testGetFlow() throws Exception {
        DataBroker dataBrocker = mock(DataBroker.class);
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataBrocker.newReadOnlyTransaction()).thenReturn(transaction);
        when(mdsalConsumer.getDataBroker()).thenReturn(dataBrocker);

        NodeBuilder nodeBuilder = mock(NodeBuilder.class);
        when(nodeBuilder.getKey()).thenReturn(mock(NodeKey.class));

        FlowBuilder flowBuilder = mock(FlowBuilder.class);
        when(flowBuilder.getKey()).thenReturn(mock(FlowKey.class));

        CheckedFuture dataRead = mock(CheckedFuture.class);
        when(transaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class))).thenReturn(dataRead);
        Optional<Flow> data = mock(Optional.class);
        when(dataRead.get()).thenReturn(data);

        abstractServiceInstance.getFlow(flowBuilder, nodeBuilder);
        verify(transaction, times(1)).read(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class));
    }

    /**
     * Test method {@link AbstractServiceInstance#programDefaultPipelineRule(String)}
     */
    @Test
    public void testProgramDefaultPipelineRule() {
        Node node = mock(Node.class);
        when(node.getId()).thenReturn(mock(NodeId.class));

        List<Node> nodes = new ArrayList();
        nodes.add(node);
        /* TODO SB_MIGRATION */
        //when(connectionService.getNodes()).thenReturn(nodes);

        ConcurrentMap<String, Row> bridges = new ConcurrentHashMap();
        bridges.put("key", mock(Row.class));
        //when(ovsdbConfigService.getRows(any(Node.class), anyString())).thenReturn(bridges);

        Bridge bridge = mock(Bridge.class);
        Column<GenericTableSchema, Set<String>> datapathIdColumn = mock(Column.class);
        when(bridge.getDatapathIdColumn()).thenReturn(datapathIdColumn);
        when(bridge.getName()).thenReturn(Constants.INTEGRATION_BRIDGE);
        Set<String> dpids = new HashSet();
        dpids.add(DPID);
        when(datapathIdColumn.getData()).thenReturn(dpids);
        //when(ovsdbConfigService.getTypedRow(any(Node.class), same(Bridge.class), any(Row.class))).thenReturn(bridge);

        abstractServiceInstance.setService(service);

        abstractServiceInstance.programDefaultPipelineRule(NODE_ID);

        /* TODO SB_MIGRATION */
        //verify(abstractServiceInstance, times(1)).isBridgeInPipeline(NODE_ID);
        //verify(abstractServiceInstance, times(1)).writeFlow(any(FlowBuilder.class), any(NodeBuilder.class));
    }
}
