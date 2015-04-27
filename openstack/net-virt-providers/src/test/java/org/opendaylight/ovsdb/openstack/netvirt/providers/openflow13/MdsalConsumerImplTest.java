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
import static org.junit.Assert.assertSame;
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
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.NeutronCacheUtils;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.impl.EventDispatcherImpl;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;

/**
 * Unit test for {@link MdsalConsumerImpl}
 */
/* TODO SB_MIGRATION */
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class MdsalConsumerImplTest {

    private MdsalConsumerImpl mdsalConsumer;

    @Mock
    private DataBroker dataBroker;

    @Mock
    private ConsumerContext session;

    @Mock
    private ProviderContext providerSession;

    @Mock
    private NotificationProviderService notificationService;

    @Before
    public void setUp(){

        mdsalConsumer = new MdsalConsumerImpl();
        dataBroker = Mockito.mock(DataBroker.class);
        session = Mockito.mock(ConsumerContext.class);
        providerSession = Mockito.mock(ProviderContext.class);
        notificationService = Mockito.mock(NotificationProviderService.class);

        when(session.getSALService(DataBroker.class)).thenReturn(dataBroker);
        when(providerSession.getSALService(NotificationProviderService.class)).thenReturn(notificationService);

        //Initialize the Mdsal consumer
        mdsalConsumer.onSessionInitialized(session);
        mdsalConsumer.onSessionInitiated(providerSession);
    }

    /**
     * Test method for
     *          {@link MdsalConsumerImpl#getConsumerContext()}
     *          {@link MdsalConsumerImpl#getDataBroker()}
     *          {@link MdsalConsumerImpl#getNotificationService()}
     *
     */
    @Test
    public void InitializationsTests(){

        assertSame("Error, getConsumerContext - returned context is invalid", session, mdsalConsumer.getConsumerContext());
        assertSame("Error, getDataBroker - returned broker is invalid", dataBroker, mdsalConsumer.getDataBroker());
        assertSame("Error, getNotificationService - returned notification service provider is invalid", notificationService, mdsalConsumer.getNotificationService());

    }

    /**
     * Test method {@link MdsalConsumerImpl#notifyFlowCapableNodeCreateEvent(String, Action)}
     */
    @Test
    public void notifyFlowCapableNodeCreateEventTest() throws Exception{

        FlowCapableNodeDataChangeListener nodeChangeListener = (FlowCapableNodeDataChangeListener) getClassField(mdsalConsumer, "flowCapableNodeChangeListener");

        //Send a notification
        mdsalConsumer.notifyFlowCapableNodeCreateEvent("flowId1", Action.ADD);

        List<Node> nodeCache = (List<Node>) getClassField(nodeChangeListener, "nodeCache");
        assertEquals("Error, notifyFlowCapableNodeEvent() - MdsalConsumerImpl NodeDataChangeLister inventory size after an ADD operation is incorrect", 1, nodeCache.size());
    }

    /**
     * Get the specified field from MdsalConsumerImpl using reflection
     * @param instancee - the class instance
     * @param fieldName - the field to retrieve
     *
     * @return the desired field
     */
    private Object getClassField(MdsalConsumerImpl instance, String fieldName) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
        Field field = MdsalConsumerImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
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
