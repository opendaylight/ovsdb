/*
 * Copyright (c) 2015 Inocybe Technologies.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronLoadBalancerPool;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler;
import org.opendaylight.ovsdb.openstack.netvirt.LBaaSHandler;
import org.opendaylight.ovsdb.openstack.netvirt.LBaaSPoolHandler;
import org.opendaylight.ovsdb.openstack.netvirt.NetworkHandler;
import org.opendaylight.ovsdb.openstack.netvirt.NeutronCacheUtils;
import org.opendaylight.ovsdb.openstack.netvirt.api.*;
import org.opendaylight.ovsdb.openstack.netvirt.impl.EventDispatcherImpl;
//import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
//import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.utils.mdsal.node.StringConvertor;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link OF13Provider}
 */
@PrepareForTest(OF13Provider.class)
@RunWith(PowerMockRunner.class)
public class OF13ProviderTest {

    @InjectMocks private OF13Provider of13Provider;
    @Mock private NeutronNetwork network;
    @Mock private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node openflowNode;
    @Mock private Node node;
    @Mock private Node node2;
    @Mock private Node node3;
    @Mock private Interface intf;

    @Mock private ConfigurationService configurationService;
    @Mock private BridgeConfigurationManager bridgeConfigurationManager;
    @Mock private TenantNetworkManager tenantNetworkManager;
    /* TODO SB_MIGRATION */
    //@Mock private OvsdbConfigurationService ovsdbConfigurationService;
    //@Mock private OvsdbConnectionService connectionService;
    @Mock private MdsalConsumer mdsalConsumer;
    @Mock private SecurityServicesManager securityServicesManager;
    @Mock private IngressAclProvider ingressAclProvider;
    @Mock private EgressAclProvider egressAclProvider;
    @Mock private ClassifierProvider classifierProvider;
    @Mock private L2ForwardingProvider l2ForwardingProvider;
    @Mock private DataBroker dataBroker;


    @Before
    public void setUp() throws Exception{
        of13Provider = new OF13Provider();

        //Setup mock dependency services.
        configurationService = Mockito.mock(ConfigurationService.class);
        bridgeConfigurationManager = Mockito.mock(BridgeConfigurationManager.class);
        tenantNetworkManager = Mockito.mock(TenantNetworkManager.class);
        /* TODO SB_MIGRATION */
        //ovsdbConfigurationService = Mockito.mock(OvsdbConfigurationService.class);
        //connectionService = Mockito.mock(OvsdbConnectionService.class);
        mdsalConsumer = Mockito.mock(MdsalConsumer.class);
        securityServicesManager = Mockito.mock(SecurityServicesManager.class);
        ingressAclProvider = Mockito.mock(IngressAclProvider.class);
        egressAclProvider = Mockito.mock(EgressAclProvider.class);
        classifierProvider = Mockito.mock(ClassifierProvider.class);
        l2ForwardingProvider = Mockito.mock(L2ForwardingProvider.class);
        dataBroker = Mockito.mock(DataBroker.class);

        this.SeedMockDependencies();

        List<Node> nodeList = new ArrayList();
        NodeId nodeId = new NodeId("Node1");
        NodeKey nodeKey = new NodeKey(nodeId);

        node = Mockito.mock(Node.class);
        when(node.getNodeId()).thenReturn(nodeId);
        when(node.getKey()).thenReturn(new NodeKey(nodeId));
        when(configurationService.getTunnelEndPoint(node)).thenReturn(InetAddress.getByName("192.168.0.1"));
        nodeList.add(node);

        nodeId = new NodeId("Node2");
        node2 = Mockito.mock(Node.class);
        when(node2.getNodeId()).thenReturn(nodeId);
        when(node2.getKey()).thenReturn(new NodeKey(nodeId));
        when(configurationService.getTunnelEndPoint(node2)).thenReturn(InetAddress.getByName("192.168.0.2"));
        nodeList.add(node2);

        nodeId = new NodeId("Node3");
        node3 = Mockito.mock(Node.class);
        when(node3.getNodeId()).thenReturn(nodeId);
        when(node3.getKey()).thenReturn(new NodeKey(nodeId));
        when(configurationService.getTunnelEndPoint(node3)).thenReturn(InetAddress.getByName("192.168.0.3"));
        nodeList.add(node3);

        /* TODO SB_MIGRATION */
        //when(connectionService.getNodes()).thenReturn(nodeList);

        final String key = "key";
        ConcurrentHashMap<String, Row> bridgeTable = new ConcurrentHashMap();
        bridgeTable.put(key, new Row());

        Row bridgeRow = Mockito.mock(Row.class);
        Bridge bridge = Mockito.mock(Bridge.class);


        Set<String> paths = new HashSet<String>(Arrays.asList(new String[] { "100"}));
        Column<GenericTableSchema, Set<String>> dataPathIdColumns = Mockito.mock(Column.class);

        when(dataPathIdColumns.getData()).thenReturn(paths);
        when(bridge.getDatapathIdColumn()).thenReturn(dataPathIdColumns);

        /* TODO SB_MIGRATION */
        when(configurationService.getIntegrationBridgeName()).thenReturn(key);
        //when(ovsdbConfigurationService.getTableName(node, Bridge.class)).thenReturn(key);
        //when(ovsdbConfigurationService.getRows(node, key)).thenReturn(bridgeTable);
        //when(ovsdbConfigurationService.getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), key)).thenReturn(bridgeRow);
        //when(ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeRow)).thenReturn(bridge);

        Bridge bridge1 = Mockito.mock(Bridge.class);
        when(bridge1.getName()).thenReturn(key);
        //when(ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeTable.get(key))).thenReturn(bridge1);

        Port port = mock(Port.class);
        Column<GenericTableSchema, Set<UUID>> itfaceColumns = mock(Column.class);
        when(port.getInterfacesColumn()).thenReturn(itfaceColumns);
        Set<UUID> ifaceUUIDs = new HashSet();
        ifaceUUIDs.add(mock(UUID.class));
        when(itfaceColumns.getData()).thenReturn(ifaceUUIDs );
        //when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Port.class), any(Row.class))).thenReturn(port);

        intf = mock(Interface.class);

        Set<Long> ports = new HashSet<Long>(Arrays.asList(new Long[] { 21L, 23L ,80L}));
        Column<GenericTableSchema, Set<Long>> openFlowPortColumns = Mockito.mock(Column.class);
        when(openFlowPortColumns.getData()).thenReturn(ports);

        when(intf.getName()).thenReturn("intf1");
        when(intf.getOpenFlowPortColumn()).thenReturn(openFlowPortColumns);

        Column<GenericTableSchema, Map<String, String>> externalIdColumns = mock(Column.class);
        Map<String, String> externalIds = new HashMap();
        externalIds.put(Constants.EXTERNAL_ID_INTERFACE_ID, "portUUID");
        externalIds.put(Constants.EXTERNAL_ID_VM_MAC, "extMac");
        when(externalIdColumns.getData()).thenReturn(externalIds);

        when(intf.getExternalIdsColumn()).thenReturn(externalIdColumns);
        //when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Interface.class), any(Row.class))).thenReturn(intf);

    }


    /**
     * Tests for defaults
     *      getName()
     *      supportsServices()
     *      hasPerTenantTunneling()
     */
    @Test
    public void verifyObjectDefaultSettings(){
        assertEquals("Error, getName() - Default provider name is invalid","OF13Provider",of13Provider.getName());
        assertEquals("Error, supportsServices() - Support services is disabled", true, of13Provider.supportsServices());
        assertEquals("Error, hasPerTenantTunneling() - Support for per tenant tunnelling is enabled", false, of13Provider.hasPerTenantTunneling());
    }

    /**
     * Test method
     * {@link OF13Provider#notifyFlowCapableNodeEventTest(Long, Action)}
     */
    @Test
    public void notifyFlowCapableNodeEventTest(){

        long flowId = 100;
        Action action = Action.ADD;

        of13Provider.notifyFlowCapableNodeEvent(flowId, action);
        verify(mdsalConsumer, times(1)).notifyFlowCapableNodeCreateEvent(Constants.OPENFLOW_NODE_PREFIX + flowId, action);
    }

    /**
     * Test method
     * {@link OF13Provider#initializeFlowRules(Node)}
     */
    @Test
    public void initializeFlowRulesTest(){

        Row row = Mockito.mock(Row.class);
        //when(ovsdbConfigurationService.getTypedRow(node, Interface.class, row)).thenReturn(intf);

        ConcurrentHashMap<String, Row> intfs = new ConcurrentHashMap();
        intfs.put("intf1", row);

        NeutronNetwork network = Mockito.mock(NeutronNetwork.class);
        when(network.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_VLAN);
        when(tenantNetworkManager.getTenantNetwork(intf)).thenReturn(network);
        //when(ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class))).thenReturn(intfs);

        of13Provider.initializeFlowRules(node);

        /**
         * The final phase of the initialization process is to call triggerInterfaceUpdates(node)
         * This must call tenantNetworkManager.getTenantNetwork(Interface) for each interface.
         * Verify that this is called once since we are initializing flow rules for only one interface.
         */
        /* TODO SB_MIGRATION */
        //verify(tenantNetworkManager, times(1)).getTenantNetwork(intf);
    }

    /**
     * Test method
     * {@link OF13Provider#initializeOFFlowRulesTest(Node)}
     */
    @Test
    public void initializeOFFlowRulesTest(){

        of13Provider.initializeOFFlowRules(openflowNode);
        //verify(connectionService, times(1)).getNodes();
    }

    /**
     * Test method
     * {@link OF13Provider#handleInterfaceUpdateTest(NeutronNetwork, Node, Interface)}
     */
    @Test
    public void handleInterfaceUpdateTest(){
        NeutronNetwork network = Mockito.mock(NeutronNetwork.class);

        /**
         * For Vlan network type, test ensures that all parameter validations
         * passed by ensuring that ovsdbConfigurationService.getRows(node,"interface_table_name))
         * is called at least once.
         */

        when(network.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_VLAN);
        /* TODO SB_MIGRATION */
        //this.of13Provider.handleInterfaceUpdate(network, node, intf);
        //verify(ovsdbConfigurationService, times(1)).getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));

        /**
         * Ideally we want to verify that the right rule tables are constructed for
         * each type of network (vlan, gre, vxlan). However, to simplify things, we just
         * verify that configurationService.getTunnelEndPoint() is called twice for the appropriate
         * network types and for each of the two remaining nodes.
         */

        when(network.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_GRE);
        /* TODO SB_MIGRATION */
        //this.of13Provider.handleInterfaceUpdate(network, node, intf);this.of13Provider.handleInterfaceUpdate(network, node, intf);
        /* TODO SB_MIGRATION */
        //verify(configurationService, times(4)).getTunnelEndPoint(node);

        when(network.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_VXLAN);
        /* TODO SB_MIGRATION */
        //this.of13Provider.handleInterfaceUpdate(network, node, intf);this.of13Provider.handleInterfaceUpdate(network, node, intf);
        //verify(configurationService, times(8)).getTunnelEndPoint(node);

        assertEquals("Error, handleInterfaceUpdate(String, String) - is returning a non NULL value.", null, this.of13Provider.handleInterfaceUpdate("",""));
    }

    /**
     * Test method
     * {@link OF13Provider#handleInterfaceDelete(String, NeutronNetwork, Node, Interface, boolean)}
     */
    @Test
    public void handleInterfaceDeleteTest(){
        NeutronNetwork network = Mockito.mock(NeutronNetwork.class);


        when(network.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_VLAN);
        when(bridgeConfigurationManager.getAllPhysicalInterfaceNames(node)).thenReturn(Arrays.asList(new String[] { "eth0", "eth1" ,"eth2"}));

        Column<GenericTableSchema, String> typeColumn = Mockito.mock(Column.class);
        when(typeColumn.getData()).thenReturn(NetworkHandler.NETWORK_TYPE_VXLAN);
        when(intf.getTypeColumn()).thenReturn(typeColumn);

        Map<String, String> options = new HashMap();
        options.put("local_ip", "192.168.0.1");
        options.put("remote_ip", "10.0.12.0");

        Column<GenericTableSchema, Map<String, String>> optionColumns = Mockito.mock(Column.class);
        when(intf.getOptionsColumn()).thenReturn(optionColumns);

        /* TODO SB_MIGRATION */
        Status status = null;//this.of13Provider.handleInterfaceDelete("tunnel1", network, node, intf, true);

        assertEquals("Error, handleInterfaceDelete(String, NeutronNetwor, Node, Interface, boolean) - returned the wrong status.", new Status(StatusCode.SUCCESS), status);
    }


    /**
     * Test method
     * {@link OF13Provider#createNodeBuilderTest(String)}
     */
    @Test
    public void createNodeBuilderTest(){
        final String nodeId="node1";

        NodeBuilder builder = new NodeBuilder();
        builder.setId(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(nodeId));
        builder.setKey(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey(builder.getId()));

        NodeBuilder builderStatic = OF13Provider.createNodeBuilder(nodeId);

        assertEquals("Error, createNodeBuilder() returned an invalid Node Builder Id", builderStatic.getId(), builder.getId());
        assertEquals("Error, createNodeBuilder() returned an invalid Node Builder key", builderStatic.getKey(), builder.getKey());
    }

    /**
     * Seeds mock dependencies into the of13Provider object
     * @throws Exception
     */
    private void SeedMockDependencies() throws Exception{

        SeedClassFieldValue(of13Provider, "configurationService", configurationService);
        SeedClassFieldValue(of13Provider, "bridgeConfigurationManager", bridgeConfigurationManager);
        SeedClassFieldValue(of13Provider, "tenantNetworkManager", tenantNetworkManager);
        /* TODO SB_MIGRATION */
        //SeedClassFieldValue(of13Provider, "ovsdbConfigurationService", ovsdbConfigurationService);
        //SeedClassFieldValue(of13Provider, "connectionService", connectionService);
        SeedClassFieldValue(of13Provider, "mdsalConsumer", mdsalConsumer);
        SeedClassFieldValue(of13Provider, "securityServicesManager", securityServicesManager);
        SeedClassFieldValue(of13Provider, "ingressAclProvider", ingressAclProvider);
        SeedClassFieldValue(of13Provider, "egressAclProvider", egressAclProvider);
        SeedClassFieldValue(of13Provider, "classifierProvider", classifierProvider);
        SeedClassFieldValue(of13Provider, "l2ForwardingProvider", l2ForwardingProvider);
        SeedClassFieldValue(of13Provider, "dataBroker", dataBroker);
    }

    /**
     * Get the specified field from OF13Provider using reflection
     * @param instance - the class instance
     * @param fieldName - the field to retrieve
     *
     * @return the desired field
     */
    private Object getClassField(OF13Provider instance, String fieldName) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
        Field field = OF13Provider.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

    /**
     * Sets the internal value of a field from OF13Provider using reflection
     * @param instance
     * @param fieldName
     * @param value
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private void SeedClassFieldValue(OF13Provider instance, String fieldName, Object value)throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{

        Field field = OF13Provider.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
