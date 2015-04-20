/*
 * Copyright (c) 2015 Inocybe Technologies.  All rights reserved.
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

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.NeutronCacheUtils;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.impl.EventDispatcherImpl;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;

/**
 * Unit test for {@link FlowCapableNodeDataChangeListener}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class FlowCapableNodeDataChangeListenerTest {

    @InjectMocks
    private FlowCapableNodeDataChangeListener nodeListener;

    @Mock
    private DataBroker dataBroker;

    @Mock
    private ListenerRegistration<DataChangeListener> registration;

    @Mock
    private NodeCacheManager nodeCacheManager;

    @Mock
    private PipelineOrchestrator orchestrator;

    @Before
    public void setUp() {

        DataBroker dataBroker = mock(DataBroker.class);
        registration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                FlowCapableNodeDataChangeListener.createFlowCapableNodePath()
                , nodeListener
                , AsyncDataBroker.DataChangeScope.BASE);

        when(dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                FlowCapableNodeDataChangeListener.createFlowCapableNodePath()
                , nodeListener
                , AsyncDataBroker.DataChangeScope.BASE)).thenReturn(registration);

        nodeListener = new FlowCapableNodeDataChangeListener(dataBroker);
        orchestrator = Mockito.mock(PipelineOrchestrator.class);
        nodeCacheManager = Mockito.mock(NodeCacheManager.class);


        PowerMockito.mockStatic(ServiceHelper.class);

        Mockito.when(ServiceHelper.getGlobalInstance(PipelineOrchestrator.class, nodeListener)).thenReturn(orchestrator);
        Mockito.when(ServiceHelper.getGlobalInstance(NodeCacheManager.class, nodeListener)).thenReturn(nodeCacheManager);

    }


    /**
     * Test method {@link FlowCapableNodeDataChangeListener#notifyFlowCapableNodeEventTest(String,Action)}
     */
    @Test
    public void notifyFlowCapableNodeEventTest() throws Exception{

        List<Node> nodeCache = (List<Node>) getClassField(nodeListener, "nodeCache");

        nodeListener.notifyFlowCapableNodeEvent("flowid1", Action.ADD);
        nodeListener.notifyFlowCapableNodeEvent("flowid2", Action.ADD);
        assertEquals("Error, notifyFlowCapableNodeEvent() - Controller's node inventory size after an ADD operation is incorrect", 2, nodeCache.size());
        verify(nodeCacheManager,times(1)).nodeAdded("flowid1");
        verify(nodeCacheManager,times(1)).nodeAdded("flowid2");
        verify(orchestrator, times(1)).enqueue("flowid1");

        nodeListener.notifyFlowCapableNodeEvent("flowid1", Action.UPDATE);
        assertEquals("Error, notifyFlowCapableNodeEvent() - Controller's node inventory size after an UPDATE operation is incorrect", 2, nodeCache.size());
        verify(nodeCacheManager, times(1)).nodeAdded("flowid1");
        verify(orchestrator, times(1)).enqueue("flowid1");

        nodeListener.notifyFlowCapableNodeEvent("flowid1", Action.DELETE);
        assertEquals("Error, notifyFlowCapableNodeEvent() - Controller's node inventory size after a DELETE operation is incorrect", 2, nodeCache.size());
        verify(nodeCacheManager, times(1)).nodeAdded("flowid1");
        verify(nodeCacheManager, times(1)).nodeRemoved("flowid1");
        verify(orchestrator, times(1)).enqueue("flowid1");

    }

    /**
     * Get the specified field from FlowCapableNodeDataChangeListener using reflection
     * @param instancee - the class instance
     * @param fieldName - the field to retrieve
     *
     * @return the desired field
     */
    private Object getClassField(FlowCapableNodeDataChangeListener instance, String fieldName) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
        Field field = FlowCapableNodeDataChangeListener.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

}
