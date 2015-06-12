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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * Unit test for {@link AbstractServiceInstance}
 */
//@PrepareForTest(ServiceHelper.class)
@RunWith(PowerMockRunner.class)
@SuppressWarnings("unchecked")
public class AbstractServiceInstanceTest {

    @InjectMocks private AbstractServiceInstance abstractServiceInstance = mock(AbstractServiceInstance.class, Mockito.CALLS_REAL_METHODS);

    @Mock private DataBroker dataBroker;
    @Mock private PipelineOrchestrator orchestrator;
    @Mock private Southbound southbound;

    private Service service = Service.L3_FORWARDING;

    private final String ID = "5710881121";
    private final String NODE_ID = Constants.INTEGRATION_BRIDGE + ":" +  ID;

    /**
     * Test method {@link AbstractServiceInstance#isBridgeInPipeline(String)}
     */
    @Test
    public void testIsBridgeInPipeline() {
        when(southbound.getBridgeName(any(Node.class))).thenReturn(Constants.INTEGRATION_BRIDGE);
        assertTrue("Error, isBridgeInPipeline() did not return the correct value", abstractServiceInstance.isBridgeInPipeline(mock(Node.class)));
    }

    /**
     * Test method {@link AbstractServiceInstance#getTable()}
     */
    @Test
    public void testGetTable() {
        abstractServiceInstance.setService(service);
        assertEquals("Error, getTable() did not return the correct value", 70, abstractServiceInstance.getTable());
    }

    @Test
    public void testGetService() {
        abstractServiceInstance.setService(service);
        assertEquals("Error, getService() did not return the correct value", service, abstractServiceInstance.getService());
    }

    /**
     * Test method {@link AbstractServiceInstance#createNodeBuilder(String)}
     */
    @Test
    public void testCreateNodeBuilder() {
        NodeId nodeId = mock(NodeId.class);
        when(nodeId.getValue()).thenReturn(NODE_ID);

        NodeBuilder nodeBuilder = abstractServiceInstance.createNodeBuilder(NODE_ID);
        assertNotNull("Error, createNodeBuilder() did not return the correct value", nodeBuilder);
        assertEquals("Error, createNodeBuilder() did not return the correct ID", NODE_ID, nodeBuilder.getId().getValue());
        assertEquals("Error, createNodeBuilder() did not return the correct Key", new NodeKey(nodeBuilder.getId()), nodeBuilder.getKey());
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
        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
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
        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
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
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        //when(mdsalConsumer.getDataBroker()).thenReturn(dataBrocker);

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
        when(southbound.getBridgeName(any(Node.class))).thenReturn(Constants.INTEGRATION_BRIDGE);
        when(southbound.getDataPathId(any(Node.class))).thenReturn(Long.valueOf(261));

        when(orchestrator.getNextServiceInPipeline(any(Service.class))).thenReturn(Service.ARP_RESPONDER);

        abstractServiceInstance.setService(service);

        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = mock(CheckedFuture.class);
        when(transaction.submit()).thenReturn(commitFuture);

        NodeBuilder nodeBuilder = mock(NodeBuilder.class);
        when(nodeBuilder.getKey()).thenReturn(mock(NodeKey.class));

        FlowBuilder flowBuilder = mock(FlowBuilder.class);
        when(flowBuilder.getKey()).thenReturn(mock(FlowKey.class));

        abstractServiceInstance.programDefaultPipelineRule(mock(Node.class));

        verify(abstractServiceInstance, times(1)).isBridgeInPipeline(any(Node.class));
        verify(abstractServiceInstance, times(1)).createNodeBuilder(anyString());
        verify(abstractServiceInstance, times(1)).writeFlow(any(FlowBuilder.class), any(NodeBuilder.class));
    }

//    @Test TODO - re-activate test
    public void testSetDependencies() throws Exception {
        PipelineOrchestrator pipelineOrchestrator = mock(PipelineOrchestrator.class);
        Southbound southbound = mock(Southbound.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(PipelineOrchestrator.class, abstractServiceInstance)).thenReturn(pipelineOrchestrator);
        PowerMockito.when(ServiceHelper.getGlobalInstance(Southbound.class, abstractServiceInstance)).thenReturn(southbound);

        abstractServiceInstance.setDependencies(mock(ServiceReference.class), mock(AbstractServiceInstance.class));

        assertEquals("Error, did not return the correct object", getField("pipelineOrchestrator"), pipelineOrchestrator);
        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = AbstractServiceInstance.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(abstractServiceInstance);
    }
}
