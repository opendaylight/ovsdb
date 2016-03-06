/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.BridgeOperationalState;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.DataChangesManagedByOvsdbNodeEvent;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactCommandAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest({InstanceIdentifier.class, OvsdbDataChangeListener.class, SouthboundMapper.class})
@RunWith(PowerMockRunner.class)
public class OvsdbDataChangeListenerTest {
    @Mock private ListenerRegistration<DataChangeListener> registration;
    @Mock private OvsdbConnectionManager cm;
    @Mock private DataBroker db;
    @Mock private OvsdbDataChangeListener ovsdbDataChangeListener;

    @Before
    public void setUp() throws Exception {
        ovsdbDataChangeListener = PowerMockito.mock(OvsdbDataChangeListener.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(OvsdbDataChangeListener.class, "cm").set(ovsdbDataChangeListener, cm);
        MemberModifier.field(OvsdbDataChangeListener.class, "db").set(ovsdbDataChangeListener, db);
        MemberModifier.field(OvsdbDataChangeListener.class, "registration").set(ovsdbDataChangeListener, registration);
    }

    @Test
    public void testClose() throws Exception {
        doNothing().when(registration).close();
        ovsdbDataChangeListener.close();
        verify(registration).close();
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testOnDataChanged() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes = mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> map = new HashMap<>();
        InstanceIdentifier<?> iid1 = mock(InstanceIdentifier.class);
        OvsdbNodeAugmentation ovsdbNode = mock(OvsdbNodeAugmentation.class);
        map.put(iid1, ovsdbNode);
        when(changes.getCreatedData()).thenReturn(map);
        ConnectionInfo key = mock(ConnectionInfo.class);
        when(ovsdbNode.getConnectionInfo()).thenReturn(key);

        //suppress calls to these functions
        MemberModifier.suppress(MemberMatcher.method(OvsdbDataChangeListener.class, "connect", AsyncDataChangeEvent.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbDataChangeListener.class, "updateConnections", AsyncDataChangeEvent.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbDataChangeListener.class, "updateData", AsyncDataChangeEvent.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbDataChangeListener.class, "disconnect", AsyncDataChangeEvent.class));

        //iid null case
        when(cm.getInstanceIdentifier(any(ConnectionInfo.class))).thenReturn(null);
        ovsdbDataChangeListener.onDataChanged(changes);

        PowerMockito.verifyPrivate(ovsdbDataChangeListener, times(1)).invoke("connect", any(AsyncDataChangeEvent.class));
        PowerMockito.verifyPrivate(ovsdbDataChangeListener, times(1)).invoke("updateConnections", any(AsyncDataChangeEvent.class));
        PowerMockito.verifyPrivate(ovsdbDataChangeListener, times(1)).invoke("updateData", any(AsyncDataChangeEvent.class));
        PowerMockito.verifyPrivate(ovsdbDataChangeListener, times(1)).invoke("disconnect", any(AsyncDataChangeEvent.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateData() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes = mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<Node>, OvsdbConnectionInstance> map = new HashMap<>();
        InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);
        OvsdbConnectionInstance connectionInstance = mock(OvsdbConnectionInstance.class);
        map.put(iid, connectionInstance);

        MemberModifier.suppress(MemberMatcher.method(OvsdbDataChangeListener.class, "connectionInstancesFromChanges", AsyncDataChangeEvent.class));
        when(ovsdbDataChangeListener.connectionInstancesFromChanges(any(AsyncDataChangeEvent.class))).thenReturn(map);
        TransactCommandAggregator transactCommandAggregator = mock(TransactCommandAggregator.class);
        BridgeOperationalState bridgeOperationalState = mock(BridgeOperationalState.class);
        DataChangesManagedByOvsdbNodeEvent dataChangesManagedByOvsdbNodeEvent = mock(DataChangesManagedByOvsdbNodeEvent.class);
        PowerMockito.whenNew(DataChangesManagedByOvsdbNodeEvent.class).withArguments(any(InstanceIdentifier.class), any(AsyncDataChangeEvent.class)).thenReturn(dataChangesManagedByOvsdbNodeEvent);
        PowerMockito.whenNew(BridgeOperationalState.class).withArguments(any(DataBroker.class), any(AsyncDataChangeEvent.class)).thenReturn(bridgeOperationalState);
        PowerMockito.whenNew(TransactCommandAggregator.class).withArguments(any(BridgeOperationalState.class), any(AsyncDataChangeEvent.class)).thenReturn(transactCommandAggregator);

        when(connectionInstance.getInstanceIdentifier()).thenReturn(iid);
        doNothing().when(connectionInstance).transact(transactCommandAggregator);

        Whitebox.invokeMethod(ovsdbDataChangeListener, "updateData", changes);
        verify(connectionInstance).transact(transactCommandAggregator);
        verify(ovsdbDataChangeListener).connectionInstancesFromChanges(changes);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testDisconnectAndConnectAndInit() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes = mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> originalDataObject = new HashMap<>();
        Set<InstanceIdentifier<?>> iiD = new HashSet<>();
        InstanceIdentifier instanceIdentifier = mock(InstanceIdentifier.class);
        OvsdbNodeAugmentation ovsdbNode = mock(OvsdbNodeAugmentation.class);
        iiD.add(instanceIdentifier);
        originalDataObject.put(instanceIdentifier, ovsdbNode);
        when(changes.getRemovedPaths()).thenReturn(iiD);
        when(changes.getOriginalData()).thenReturn(originalDataObject);
        doNothing().when(cm).disconnect(any(OvsdbNodeAugmentation.class));

        //test disconnect()
        Whitebox.invokeMethod(ovsdbDataChangeListener, "disconnect", changes);
        verify(cm).disconnect(any(OvsdbNodeAugmentation.class));

        //test connect()
        when(changes.getCreatedData()).thenReturn(originalDataObject);
        Whitebox.invokeMethod(ovsdbDataChangeListener, "connect", changes);
        verify(cm).disconnect(any(OvsdbNodeAugmentation.class));

        // test init
        /**
         * ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
         * when(ovsdbNode.getConnectionInfo()).thenReturn(connectionInfo);
         * Whitebox.invokeMethod(ovsdbDataChangeListener, "init", changes);
         * verify(cm).init(any(ConnectionInfo.class));
         */
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testUpdateConnections() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes = mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> map = new HashMap<>();
        InstanceIdentifier instanceIdentifier = mock(InstanceIdentifier.class);
        OvsdbNodeAugmentation value = mock(OvsdbNodeAugmentation.class);
        map.put(instanceIdentifier, value);

        when(changes.getUpdatedData()).thenReturn(map);
        OvsdbClient client = mock(OvsdbClient.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        when(value.getConnectionInfo()).thenReturn(connectionInfo);
        when(cm.getClient(any(ConnectionInfo.class))).thenReturn(null);

        when(changes.getOriginalData()).thenReturn(map);
        doNothing().when(cm).disconnect(any(OvsdbNodeAugmentation.class));
        when(cm.connect(any(InstanceIdentifier.class), any(OvsdbNodeAugmentation.class))).thenReturn(client);
        Whitebox.invokeMethod(ovsdbDataChangeListener, "updateConnections", changes);
        verify(cm).connect(any(InstanceIdentifier.class), any(OvsdbNodeAugmentation.class));
        verify(cm).disconnect(any(OvsdbNodeAugmentation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConnectionInstancesFromChanges() throws Exception {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes = mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<Node>,OvsdbConnectionInstance> testResultMap = new HashMap<>();
        Map<InstanceIdentifier<Node>,OvsdbConnectionInstance> map1 = new HashMap<>();
        InstanceIdentifier<Node> key1 = mock(InstanceIdentifier.class);
        OvsdbConnectionInstance value1 = mock(OvsdbConnectionInstance.class);
        map1.put(key1, value1);

        MemberModifier.suppress(MemberMatcher.method(OvsdbDataChangeListener.class, "connectionInstancesFromMap", HashMap.class));
        when(ovsdbDataChangeListener.connectionInstancesFromMap(any(HashMap.class))).thenReturn(map1);

        testResultMap.put(key1, value1);
        reset(ovsdbDataChangeListener);
        assertEquals("Error returning correct Map", testResultMap, ovsdbDataChangeListener.connectionInstancesFromChanges(changes));
        verify(ovsdbDataChangeListener, times(3)).connectionInstancesFromMap(any(HashMap.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConnectionInstancesFromMap() {
        reset(cm);
        Map<InstanceIdentifier<?>,DataObject> map = new HashMap<>();
        Node node = mock(Node.class);
        InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);
        map.put(iid, node);
        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class);

        Map<InstanceIdentifier<Node>,OvsdbConnectionInstance> testResultMap = new HashMap<>();
        testResultMap.put(iid, client);

        //bridge and client not null case
        when(cm.getConnectionInstance(any(OvsdbBridgeAugmentation.class))).thenReturn(client);
        OvsdbBridgeAugmentation bridge = mock(OvsdbBridgeAugmentation.class);
        when(node.getAugmentation(OvsdbBridgeAugmentation.class)).thenReturn(bridge);
        when(cm.getHasDeviceOwnership(any(ConnectionInfo.class))).thenReturn(true);
        assertEquals("Error returning correct Map", testResultMap, ovsdbDataChangeListener.connectionInstancesFromMap(map));
        verify(cm).getConnectionInstance(any(OvsdbBridgeAugmentation.class));

        //bridge null, ovsnode not null and client not null case
        when(node.getAugmentation(OvsdbBridgeAugmentation.class)).thenReturn(null);
        OvsdbNodeAugmentation ovsNode = mock(OvsdbNodeAugmentation.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsNode);
        when(ovsNode.getConnectionInfo()).thenReturn(connectionInfo);
        when(cm.getConnectionInstance(any(ConnectionInfo.class))).thenReturn(client);
        assertEquals("Error returning correct Map", testResultMap, ovsdbDataChangeListener.connectionInstancesFromMap(map));
        verify(cm).getConnectionInstance(any(ConnectionInfo.class));

        //bridge null, ovsnode null, and client not null case
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(null);
        List<TerminationPoint> terminationPoint = new ArrayList<>();
        terminationPoint.add(0, mock(TerminationPoint.class));
        when(node.getTerminationPoint()).thenReturn(terminationPoint);
        PowerMockito.mockStatic(SouthboundMapper.class);
        InstanceIdentifier<Node> nodeIid = mock(InstanceIdentifier.class);
        when(node.getNodeId()).thenReturn(mock(NodeId.class));
        when(SouthboundMapper.createInstanceIdentifier(any(NodeId.class))).thenReturn(nodeIid);
        when(cm.getConnectionInstance(any(InstanceIdentifier.class))).thenReturn(client);
        assertEquals("Error returning correct Map", testResultMap, ovsdbDataChangeListener.connectionInstancesFromMap(map));
        verify(node).getTerminationPoint();
        verify(cm).getConnectionInstance(any(InstanceIdentifier.class));
    }
}
