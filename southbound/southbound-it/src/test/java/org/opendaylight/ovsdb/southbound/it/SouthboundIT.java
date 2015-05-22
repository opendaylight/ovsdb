/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.it;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbFailModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Trunks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.TrunksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for southbound-impl
 *
 * @author Sam Hague (shague@redhat.com)
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SouthboundIT extends AbstractMdsalTestBase {
    private static final String EXPECTED_VALUES_KEY = "ExpectedValuesKey";
    private static final String INPUT_VALUES_KEY = "InputValuesKey";
    private static final String NETDEV_DP_TYPE = "netdev";
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundIT.class);
    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    private static DataBroker dataBroker = null;
    private static String addressStr;
    private static String portStr;
    private static String connectionType;
    private static Boolean setup = false;
    private static MdsalUtils mdsalUtils = null;
    private static final String NETVIRT = "org.opendaylight.ovsdb.openstack.net-virt";
    private static final String NETVIRTPROVIDERS = "org.opendaylight.ovsdb.openstack.net-virt-providers";

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        return super.config();
    }

    @Override
    public String getModuleName() {
        return "southbound-impl";
    }

    @Override
    public String getInstanceName() {
        return "southbound-default";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("southbound-features")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-ovsdb-southbound-impl-ui";
    }

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n"
                + "active connection : mvn -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"
                + "passive connection : mvn -Dovsdbserver.connection=passive verify\n";
    }

    @Override
    public Option[] getFeaturesOptions() {
        return new Option[]{};
    }

    @Override
    public Option[] getLoggingOptions() {
        Option[] options = new Option[] {
                editConfigurationFilePut(SouthboundITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb.southbound-impl",
                        LogLevelOption.LogLevel.DEBUG.name())
        };

        options = ObjectArrays.concat(options, super.getLoggingOptions(), Option.class);
        return options;
    }

    @Override
    public Option[] getPropertiesOptions() {
        Properties props = new Properties(System.getProperties());
        String addressStr = props.getProperty(SouthboundITConstants.SERVER_IPADDRESS,
                SouthboundITConstants.DEFAULT_SERVER_IPADDRESS);
        String portStr = props.getProperty(SouthboundITConstants.SERVER_PORT,
                SouthboundITConstants.DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(SouthboundITConstants.CONNECTION_TYPE,
                SouthboundITConstants.CONNECTION_TYPE_ACTIVE);

        LOG.info("getPropertiesOptions: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);

        Option[] options = new Option[] {
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.SERVER_IPADDRESS, addressStr),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.SERVER_PORT, portStr),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.CONNECTION_TYPE, connectionType),
        };
        return options;
    }

    @Before
    public void setUp() throws InterruptedException {
        if (setup == true) {
            LOG.info("Skipping setUp, already initialized");
            return;
        }

        try {
            super.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //dataBroker = getSession().getSALService(DataBroker.class);
        Thread.sleep(3000);
        dataBroker = SouthboundProvider.getDb();
        Assert.assertNotNull("db should not be null", dataBroker);

        addressStr = bundleContext.getProperty(SouthboundITConstants.SERVER_IPADDRESS);
        portStr = bundleContext.getProperty(SouthboundITConstants.SERVER_PORT);
        connectionType = bundleContext.getProperty(SouthboundITConstants.CONNECTION_TYPE);

        LOG.info("setUp: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);
        if (connectionType.equalsIgnoreCase(SouthboundITConstants.CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }

        mdsalUtils = new MdsalUtils(dataBroker);
        setup = true;
    }

    /**
     * Test passive connection mode. The southbound starts in a listening mode waiting for connections on port
     * 6640. This test will wait for incoming connections for {@link SouthboundITConstants.CONNECTION_INIT_TIMEOUT} ms.
     *
     * @throws InterruptedException
     */
    @Test
    public void testPassiveNode() throws InterruptedException {
        if (connectionType.equalsIgnoreCase(SouthboundITConstants.CONNECTION_TYPE_PASSIVE)) {
            //Wait for CONNECTION_INIT_TIMEOUT for the Passive connection to be initiated by the ovsdb-server.
            Thread.sleep(SouthboundITConstants.CONNECTION_INIT_TIMEOUT);
        }
    }

    private ConnectionInfo getConnectionInfo(final String addressStr, final String portStr) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(addressStr);
        } catch (UnknownHostException e) {
            fail("Could not allocate InetAddress: " + e);
        }

        IpAddress address = SouthboundMapper.createIpAddress(inetAddress);
        PortNumber port = new PortNumber(Integer.parseInt(portStr));

        LOG.info("connectionInfo: {}", new ConnectionInfoBuilder()
                .setRemoteIp(address)
                .setRemotePort(port)
                .build());
        return new ConnectionInfoBuilder()
                       .setRemoteIp(address)
                       .setRemotePort(port)
                       .build();
    }

    private String connectionInfoToString(final ConnectionInfo connectionInfo) {
        return new String(connectionInfo.getRemoteIp().getValue()) + ":" + connectionInfo.getRemotePort().getValue();
    }

    @Test
    public void testNetworkTopology() throws InterruptedException {
        NetworkTopology networkTopology = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(NetworkTopology.class));
        Assert.assertNotNull("NetworkTopology could not be found in " + LogicalDatastoreType.CONFIGURATION,
                networkTopology);

        networkTopology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class));
        Assert.assertNotNull("NetworkTopology could not be found in " + LogicalDatastoreType.OPERATIONAL,
                networkTopology);
    }

    @Test
    public void testOvsdbTopology() throws InterruptedException {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        Topology topology = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        Assert.assertNotNull("Topology could not be found in " + LogicalDatastoreType.CONFIGURATION,
                topology);

        topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, path);

        Assert.assertNotNull("Topology could not be found in " + LogicalDatastoreType.OPERATIONAL,
                topology);
    }

    private boolean addOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION,
                SouthboundMapper.createInstanceIdentifier(connectionInfo),
                SouthboundMapper.createNode(connectionInfo));
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    private Node getOvsdbNode(final ConnectionInfo connectionInfo) {
        Node node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                SouthboundMapper.createInstanceIdentifier(connectionInfo));
        return node;
    }

    private boolean deleteOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
                SouthboundMapper.createInstanceIdentifier(connectionInfo));
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    private Node connectOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        Assert.assertTrue(addOvsdbNode(connectionInfo));
        Node node = getOvsdbNode(connectionInfo);
        Assert.assertNotNull(node);
        LOG.info("Connected to {}", connectionInfoToString(connectionInfo));
        return node;
    }

    private boolean disconnectOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        Assert.assertTrue(deleteOvsdbNode(connectionInfo));
        Node node = getOvsdbNode(connectionInfo);
        Assert.assertNull(node);
        //Assume.assumeNotNull(node); // Using assumeNotNull because there is no assumeNull
        LOG.info("Disconnected from {}", connectionInfoToString(connectionInfo));
        return true;
    }

    @Test
    public void testAddDeleteOvsdbNode() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
        //Assume.assumeTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testDpdkSwitch() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);
        List<DatapathTypeEntry> datapathTypeEntries = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class)
                .getDatapathTypeEntry();
        if (datapathTypeEntries == null) {
            LOG.info("DPDK not supported on this node.");
        } else {
            Class<? extends DatapathTypeBase> dpType = null;
            String dpTypeStr = null;
            for (DatapathTypeEntry dpTypeEntry : datapathTypeEntries) {
                dpType = dpTypeEntry.getDatapathType();
                dpTypeStr = SouthboundConstants.DATAPATH_TYPE_MAP.get(dpType);
                LOG.info("dp type is {}", dpTypeStr);
                if (dpTypeStr.equals(NETDEV_DP_TYPE)) {
                    LOG.info("Found a DPDK node; adding a corresponding netdev device");
                    InstanceIdentifier<Node> bridgeIid = SouthboundMapper.createInstanceIdentifier(connectionInfo,
                            new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME));
                    NodeId bridgeNodeId = SouthboundMapper.createManagedNodeId(bridgeIid);
                    addBridge(connectionInfo, bridgeIid, SouthboundITConstants.BRIDGE_NAME, bridgeNodeId, false, null,
                            true, dpType, null, null);

                    // Verify that the device is netdev
                    OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
                    Assert.assertNotNull(bridge);
                    Assert.assertEquals(dpTypeStr, bridge.getDatapathType());

                    // Add dpdk port
                    final String TEST_PORT_NAME = "testDPDKPort";
                    OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                            createGenericDpdkOvsdbTerminationPointAugmentationBuilder(TEST_PORT_NAME);
                    Assert.assertTrue(addTerminationPoint(bridgeNodeId, TEST_PORT_NAME, ovsdbTerminationBuilder));

                    // Verify that DPDK port was created
                    InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
                    Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                            terminationPointIid);
                    Assert.assertNotNull(terminationPointNode);

                    // Verify that each termination point has DPDK ifType
                    Class<? extends InterfaceTypeBase> dpdkIfType = SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP
                            .get("dpdk");
                    Class<? extends InterfaceTypeBase> opPort = null;
                    List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
                    for (TerminationPoint terminationPoint : terminationPoints) {
                        OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation = terminationPoint
                                .getAugmentation(OvsdbTerminationPointAugmentation.class);
                        if (ovsdbTerminationPointAugmentation.getName().equals(TEST_PORT_NAME)) {
                            opPort = ovsdbTerminationPointAugmentation
                                    .getInterfaceType();
                            Assert.assertEquals(dpdkIfType, opPort);
                        }
                    }
                    Assert.assertTrue(deleteBridge(connectionInfo));
                    break;
                }
            }
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testOvsdbNodeOvsVersion() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        Assert.assertNotNull(ovsdbNodeAugmentation);
        assertNotNull(ovsdbNodeAugmentation.getOvsVersion());
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
        //Assume.assumeTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testOpenVSwitchOtherConfig() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        Assert.assertNotNull(ovsdbNodeAugmentation);
        List<OpenvswitchOtherConfigs> otherConfigsList = ovsdbNodeAugmentation.getOpenvswitchOtherConfigs();
        if (otherConfigsList != null) {
            for (OpenvswitchOtherConfigs otherConfig : otherConfigsList) {
                if (otherConfig.getOtherConfigKey().equals("local_ip")) {
                    LOG.info("local_ip: {}", otherConfig.getOtherConfigValue());
                    break;
                } else {
                    LOG.info("other_config {}:{}", otherConfig.getOtherConfigKey(), otherConfig.getOtherConfigValue());
                }
            }
        } else {
            LOG.info("other_config is not present");
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
        //Assume.assumeTrue(disconnectOvsdbNode(connectionInfo));
    }

    private void setManagedBy(final OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                              final ConnectionInfo connectionInfo) {
        InstanceIdentifier<Node> connectionNodePath = SouthboundMapper.createInstanceIdentifier(connectionInfo);
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath));
    }

    private List<ProtocolEntry> createMdsalProtocols() {
        List<ProtocolEntry> protocolList = new ArrayList<ProtocolEntry>();
        ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
        protocolList.add(new ProtocolEntryBuilder().
                setProtocol((Class<? extends OvsdbBridgeProtocolBase>) mapper.get("OpenFlow13")).build());
        return protocolList;
    }

    private OvsdbTerminationPointAugmentationBuilder createGenericOvsdbTerminationPointAugmentationBuilder() {
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationPointAugmentationBuilder.setInterfaceType(
                new InterfaceTypeEntryBuilder()
                        .setInterfaceType(
                                SouthboundMapper.createInterfaceType("internal"))
                        .build().getInterfaceType());
        return ovsdbTerminationPointAugmentationBuilder;
    }

    private OvsdbTerminationPointAugmentationBuilder createGenericDpdkOvsdbTerminationPointAugmentationBuilder(
            final String portName) {
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationBuilder.setName(portName);
        Class<? extends InterfaceTypeBase> ifType = SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP
                .get("dpdk");
        ovsdbTerminationBuilder.setInterfaceType(ifType);
        return ovsdbTerminationBuilder;
    }

    private boolean addTerminationPoint(final NodeId bridgeNodeId, final String portName,
            final OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder)
        throws InterruptedException {

        InstanceIdentifier<Node> portIid = SouthboundMapper.createInstanceIdentifier(bridgeNodeId);
        NodeBuilder portNodeBuilder = new NodeBuilder();
        NodeId portNodeId = SouthboundMapper.createManagedNodeId(portIid);
        portNodeBuilder.setNodeId(portNodeId);
        TerminationPointBuilder entry = new TerminationPointBuilder();
        entry.setKey(new TerminationPointKey(new TpId(portName)));
        entry.addAugmentation(
                OvsdbTerminationPointAugmentation.class,
                ovsdbTerminationPointAugmentationBuilder.build());
        portNodeBuilder.setTerminationPoint(Lists.newArrayList(entry.build()));
        boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                portIid, portNodeBuilder.build());
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    /*
     * base method for adding test bridges.  Other helper methods used to create bridges should utilize this method.
     *
     * @param connectionInfo
     * @param bridgeIid if passed null, one is created
     * @param bridgeName cannot be null
     * @param bridgeNodeId if passed null, one is created based on <code>bridgeIid</code>
     * @param setProtocolEntries toggles whether default protocol entries are set for the bridge
     * @param failMode toggles whether default fail mode is set for the bridge
     * @param setManagedBy toggles whether to setManagedBy for the bridge
     * @param dpType if passed null, this parameter is ignored
     * @param externalIds if passed null, this parameter is ignored
     * @param otherConfig if passed null, this parameter is ignored
     * @return success of bridge addition
     * @throws InterruptedException
     */
    private boolean addBridge(final ConnectionInfo connectionInfo, InstanceIdentifier<Node> bridgeIid,
            final String bridgeName, NodeId bridgeNodeId, final boolean setProtocolEntries,
            final Class<? extends OvsdbFailModeBase> failMode, final boolean setManagedBy,
            final Class<? extends DatapathTypeBase> dpType,
            final List<BridgeExternalIds> externalIds,
            final List<BridgeOtherConfigs> otherConfigs) throws InterruptedException {

        NodeBuilder bridgeNodeBuilder = new NodeBuilder();
        if (bridgeIid == null) {
            bridgeIid = SouthboundMapper.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
        }
        if (bridgeNodeId == null) {
            bridgeNodeId = SouthboundMapper.createManagedNodeId(bridgeIid);
        }
        bridgeNodeBuilder.setNodeId(bridgeNodeId);
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
        if (setProtocolEntries) {
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(createMdsalProtocols());
        }
        if (failMode != null) {
            ovsdbBridgeAugmentationBuilder.setFailMode(failMode);
        }
        if (setManagedBy) {
            setManagedBy(ovsdbBridgeAugmentationBuilder, connectionInfo);
        }
        if (dpType != null) {
            ovsdbBridgeAugmentationBuilder.setDatapathType(dpType);
        }
        if (externalIds != null) {
            ovsdbBridgeAugmentationBuilder.setBridgeExternalIds(externalIds);
        }
        if (otherConfigs != null) {
            ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(otherConfigs);
        }
        bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());
        LOG.debug("Built with the intent to store bridge data {}",
                ovsdbBridgeAugmentationBuilder.toString());
        boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                bridgeIid, bridgeNodeBuilder.build());
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    private boolean addBridge(final ConnectionInfo connectionInfo, final String bridgeName)
        throws InterruptedException {

        return addBridge(connectionInfo, null, bridgeName, null, true,
                SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null, null);
    }

    private OvsdbBridgeAugmentation getBridge(ConnectionInfo connectionInfo) {
        return getBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME);
    }

    /**
     * Extract the <code>store</code> type data store contents for the particular bridge identified by
     * <code>bridgeName</code>.
     *
     * @param connectionInfo
     * @param bridgeName
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @return <code>store</code> type data store contents
     */
    private OvsdbBridgeAugmentation getBridge(ConnectionInfo connectionInfo, String bridgeName,
            LogicalDatastoreType store) {
        Node bridgeNode = getBridgeNode(connectionInfo, bridgeName, store);
        Assert.assertNotNull(bridgeNode);
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
        Assert.assertNotNull(ovsdbBridgeAugmentation);
        return ovsdbBridgeAugmentation;
    }

    /**
     * extract the <code>LogicalDataStoreType.OPERATIONAL</code> type data store contents for the particular bridge
     * identified by <code>bridgeName</code>
     *
     * @param connectionInfo
     * @param bridgeName
     * @see <code>SouthboundIT.getBridge(ConnectionInfo, String, LogicalDatastoreType)</code>
     * @return <code>LogicalDatastoreType.OPERATIONAL</code> type data store contents
     */
    private OvsdbBridgeAugmentation getBridge(ConnectionInfo connectionInfo, String bridgeName) {
        return getBridge(connectionInfo, bridgeName, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Extract the node contents from <code>store</code> type data store for the
     * bridge identified by <code>bridgeName</code>
     *
     * @param connectionInfo
     * @param bridgeName
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @return <code>store</code> type data store contents
     */
    private Node getBridgeNode(ConnectionInfo connectionInfo, String bridgeName, LogicalDatastoreType store) {
        InstanceIdentifier<Node> bridgeIid =
                SouthboundMapper.createInstanceIdentifier(connectionInfo,
                    new OvsdbBridgeName(bridgeName));
        return mdsalUtils.read(store, bridgeIid);
    }

    /**
     * Extract the node contents from <code>LogicalDataStoreType.OPERATIONAL</code> data store for the
     * bridge identified by <code>bridgeName</code>
     *
     * @param connectionInfo
     * @param bridgeName
     * @return <code>LogicalDatastoreType.OPERATIONAL</code> type data store contents
     */
    private Node getBridgeNode(ConnectionInfo connectionInfo, String bridgeName) {
        return getBridgeNode(connectionInfo, bridgeName, LogicalDatastoreType.OPERATIONAL);
    }

    private boolean deleteBridge(ConnectionInfo connectionInfo) throws InterruptedException {
        return deleteBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME);
    }

    private boolean deleteBridge(final ConnectionInfo connectionInfo, final String bridgeName)
        throws InterruptedException {

        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
                SouthboundMapper.createInstanceIdentifier(connectionInfo,
                        new OvsdbBridgeName(bridgeName)));
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    @Test
    public void testAddDeleteBridge() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);

        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        LOG.info("bridge: {}", bridge);

        Assert.assertTrue(deleteBridge(connectionInfo));

        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
        //Assume.assumeTrue(disconnectOvsdbNode(connectionInfo));
    }

    private InstanceIdentifier<Node> getTpIid(ConnectionInfo connectionInfo, OvsdbBridgeAugmentation bridge) {
        return SouthboundMapper.createInstanceIdentifier(connectionInfo,
            bridge.getBridgeName());
    }

    /**
     * Extracts the <code>TerminationPointAugmentation</code> for the <code>index</code> <code>TerminationPoint</code>
     * on <code>bridgeName</code>
     *
     * @param connectionInfo
     * @param bridgeName
     * @param store
     * @param index
     * @return
     */
    private OvsdbTerminationPointAugmentation getOvsdbTerminationPointAugmentation(ConnectionInfo connectionInfo,
            String bridgeName, LogicalDatastoreType store, int index ) {

        return ((OvsdbTerminationPointAugmentation)
                getBridgeNode(connectionInfo, bridgeName, store)
                .getTerminationPoint().get(index)
                .getAugmentation(OvsdbTerminationPointAugmentation.class));
    }

    @Test
    public void testCRDTerminationPointOfPort() throws InterruptedException {
        final Long OFPORT_EXPECTED = new Long(45002);

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);

        // CREATE
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        LOG.info("bridge: {}", bridge);
        NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                connectionInfo, bridge.getBridgeName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        String portName = "testOfPort";
        ovsdbTerminationBuilder.setName(portName);

        ovsdbTerminationBuilder.setOfport(OFPORT_EXPECTED);
        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);

        // READ
        List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPoints) {
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                Long ofPort = ovsdbTerminationPointAugmentation.getOfport();
                // if ephemeral port 45002 is in use, ofPort is set to 1
                Assert.assertTrue(ofPort.equals(OFPORT_EXPECTED) || ofPort.equals(new Long(1)));
                LOG.info("ofPort: {}", ofPort);
            }
        }

        // UPDATE- Not Applicable.  From the OpenVSwitch Documentation:
        //   "A client should ideally set this column’s value in the same database transaction that it uses to create
        //   the interface."

        // DELETE
        Assert.assertTrue(deleteBridge(connectionInfo));
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testCRDTerminationPointOfPortRequest() throws InterruptedException {
        final Long OFPORT_EXPECTED = new Long(45008);
        final Long OFPORT_INPUT = new Long(45008);
        final Long OFPORT_UPDATE = new Long(45009);

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);

        // CREATE
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                connectionInfo, bridge.getBridgeName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        String portName = "testOfPortRequest";
        ovsdbTerminationBuilder.setName(portName);
        Integer ofPortRequestExpected = OFPORT_EXPECTED.intValue();
        ovsdbTerminationBuilder.setOfport(OFPORT_INPUT);
        ovsdbTerminationBuilder.setOfportRequest(ofPortRequestExpected);
        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);

        // READ
        List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPoints) {
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                Long ofPort = ovsdbTerminationPointAugmentation.getOfport();
                // if ephemeral port 45008 is in use, ofPort is set to 1
                Assert.assertTrue(ofPort.equals(OFPORT_EXPECTED) || ofPort.equals(new Long(1)));
                LOG.info("ofPort: {}", ofPort);

                Integer ofPortRequest = ovsdbTerminationPointAugmentation.getOfportRequest();
                Assert.assertTrue(ofPortRequest.equals(ofPortRequestExpected));
                LOG.info("ofPortRequest: {}", ofPortRequest);
            }
        }

        // UPDATE- Not Applicable.  From the OpenVSwitch documentation:
        //   "A client should ideally set this column’s value in the same database transaction that it uses to create
        //   the interface. "

        // DELETE
        Assert.assertTrue(deleteBridge(connectionInfo));
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    /*
     * Generates the test cases involved in testing PortExternalIds.  See inline comments for descriptions of
     * the particular cases considered.
     *
     * The return value is a Map in the form (K,V)=(testCaseName,testCase).
     * - testCaseName is a String
     * - testCase is a Map in the form (K,V) s.t. K=(EXPECTED_VALUES_KEY|INPUT_VALUES_KEY) and V is a List of
     *     either corresponding INPUT port external_ids, or EXPECTED port external_ids
     *     INPUT    is the List we use when calling
     *              <code>TerminationPointAugmentationBuilder.setPortExternalIds()</code>
     *     EXPECTED is the List we expect to receive after calling
     *              <code>TerminationPointAugmentationBuilder.getPortExternalIds()</code>
     */
    private Map<String, Map<String, List<PortExternalIds>>> generatePortExternalIdsTestCases() {
        Map<String, Map<String, List<PortExternalIds>>> testMap =
                new HashMap<String, Map<String, List<PortExternalIds>>>();

        final String PORT_EXTERNAL_ID_KEY = "PortExternalIdKey";
        final String PORT_EXTERNAL_ID_VALUE = "PortExternalIdValue";
        final String FORMAT_STR = "%s_%s_%d";
        final String GOOD_KEY = "GoodKey";
        final String GOOD_VALUE = "GoodValue";
        final String NO_VALUE_FOR_KEY = "NoValueForKey";
        final String NO_KEY_FOR_VALUE = "NoKeyForValue";

        // Test Case 1:  TestOneExternalId
        // Test Type:    Positive
        // Description:  Create a termination point with one PortExternalIds
        // Expected:     A port is created with the single external_ids specified below
        final String testOneExternalIdName = "TestOneExternalId";
        int externalIdCounter = 0;
        List<PortExternalIds> oneExternalId = (List<PortExternalIds>) Lists.newArrayList(
            (new PortExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testOneExternalIdName,
                            PORT_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testOneExternalIdName,
                            PORT_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()));
        Map<String,List<PortExternalIds>> testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, oneExternalId);
        testCase.put(INPUT_VALUES_KEY, oneExternalId);
        testMap.put(testOneExternalIdName, testCase);

        // Test Case 2:  TestFiveExternalId
        // Test Type:    Positive
        // Description:  Create a termination point with multiple (five) PortExternalIds
        // Expected:     A port is created with the five external_ids specified below
        final String testFiveExternalIdName = "TestFiveExternalId";
        externalIdCounter = 0;
        List<PortExternalIds> fiveExternalId = (List<PortExternalIds>) Lists.newArrayList(
            (new PortExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            PORT_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            PORT_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new PortExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            PORT_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            PORT_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new PortExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            PORT_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            PORT_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new PortExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            PORT_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            PORT_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new PortExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                        PORT_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            PORT_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()));
        testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, fiveExternalId);
        testCase.put(INPUT_VALUES_KEY, fiveExternalId);
        testMap.put(testOneExternalIdName, testCase);

        // Test Case 3:  TestOneGoodExternalIdOneMalformedExternalIdValue
        // Test Type:    Negative
        // Description:
        //     One perfectly fine PortExternalId
        //        (TestOneGoodExternalIdOneMalformedExternalIdValue_PortExternalIdKey_1,
        //        TestOneGoodExternalIdOneMalformedExternalId_PortExternalIdValue_1)
        //     and one malformed PortExternalId which only has key specified
        //        (TestOneGoodExternalIdOneMalformedExternalIdValue_NoValueForKey_2,
        //        UNSPECIFIED)
        // Expected:     A port is created without any external_ids
        final String testOneGoodExternalIdOneMalformedExternalIdValueName =
                "TestOneGoodExternalIdOneMalformedExternalIdValue";
        externalIdCounter = 0;
        PortExternalIds oneGood = new PortExternalIdsBuilder()
            .setExternalIdKey(String.format(FORMAT_STR, testOneGoodExternalIdOneMalformedExternalIdValueName,
                    GOOD_KEY, ++externalIdCounter))
                .setExternalIdValue(String.format("FORMAT_STR",
                        testOneGoodExternalIdOneMalformedExternalIdValueName,
                            GOOD_VALUE, externalIdCounter))
                .build();
        PortExternalIds oneBad = new PortExternalIdsBuilder()
            .setExternalIdKey(String.format(FORMAT_STR,
                    testOneGoodExternalIdOneMalformedExternalIdValueName, NO_VALUE_FOR_KEY, ++externalIdCounter))
                .build();
        List<PortExternalIds> oneGoodOneBadInput = (List<PortExternalIds>) Lists.newArrayList(
                oneGood, oneBad);
        List<PortExternalIds> oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);

        // Test Case 4:  TestOneGoodExternalIdOneMalformedExternalIdKey
        // Test Type:    Negative
        // Description:
        //     One perfectly fine PortExternalId
        //        (TestOneGoodExternalIdOneMalformedExternalIdValue_PortExternalIdKey_1,
        //        TestOneGoodExternalIdOneMalformedExternalId_PortExternalIdValue_1)
        //     and one malformed PortExternalId which only has key specified
        //        (UNSPECIFIED,
        //        TestOneGoodExternalIdOneMalformedExternalIdKey_NoKeyForValue_2)
        // Expected:     A port is created without any external_ids
        final String testOneGoodExternalIdOneMalformedExternalIdKeyName =
                "TestOneGoodExternalIdOneMalformedExternalIdKey";
        externalIdCounter = 0;
        oneGood = new PortExternalIdsBuilder()
            .setExternalIdKey(String.format(FORMAT_STR, testOneGoodExternalIdOneMalformedExternalIdKeyName,
                    GOOD_KEY, ++externalIdCounter))
                .setExternalIdValue(String.format("FORMAT_STR",
                        testOneGoodExternalIdOneMalformedExternalIdKeyName,
                            GOOD_VALUE, externalIdCounter))
                .build();
        oneBad = new PortExternalIdsBuilder()
            .setExternalIdKey(String.format(FORMAT_STR,
                    testOneGoodExternalIdOneMalformedExternalIdKeyName, NO_KEY_FOR_VALUE, ++externalIdCounter))
                .build();
        oneGoodOneBadInput = (List<PortExternalIds>) Lists.newArrayList(
                oneGood, oneBad);
        oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);

        return testMap;
    }

    /*
     * @see <code>SouthboundIT.testCRUDPortExternalIds()</code>
     * This is helper test method to compare a test "set" of BridgeExternalIds against an expected "set"
     */
    private void assertExpectedPortExternalIdsExist( List<PortExternalIds> expected,
            List<PortExternalIds> test ) {

        if (expected != null) {
            for (PortExternalIds expectedExternalId : expected) {
                Assert.assertTrue(test.contains(expectedExternalId));
            }
        } else {
            Assert.assertNull(test);
        }
    }

    /*
     * @see <code>SouthboundIT.testCRUDPortExternalIds()</code>
     * This is a helper test method.  The method only checks if
     * <code>updateFromInputExternalIds != updateToInputExternalIds</code>,  (i.e., the updateTo "set" isn't the same
     * as the updateFrom "set".  Then, the method ensures each element of erase is not an element of test, as the input
     * test cases are divergent.
     */
    private void assertPortExternalIdsErased( List<PortExternalIds> updateFromInputExternalIds,
            List<PortExternalIds> updateToInputExternalIds,
            List<PortExternalIds> updateFromExpectedExternalIds,
            List<PortExternalIds> updateToTestExternalIds ) {

        if (!updateFromInputExternalIds.containsAll(updateToInputExternalIds)) {
            for (PortExternalIds erasedExternalId : updateFromExpectedExternalIds) {
                Assert.assertTrue(!updateToTestExternalIds.contains(erasedExternalId));
            }
        }
    }

    /*
     * Tests the CRUD operations for <code>Port</code> <code>external_ids</code>.
     *
     * @see <code>SouthboundIT.generatePortExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointPortExternalIds() throws InterruptedException {
        final String TEST_PREFIX = "CRUDTPPortExternalIds";
        final int TERMINATION_POINT_TEST_INDEX = 0;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        Map<String, Map<String, List<PortExternalIds>>> updateFromTestCases = generatePortExternalIdsTestCases();
        Map<String, Map<String, List<PortExternalIds>>> updateToTestCases = generatePortExternalIdsTestCases();
        Map<String, List<PortExternalIds>> updateFromTestCase = null;
        List<PortExternalIds> updateFromInputExternalIds = null;
        List<PortExternalIds> updateFromExpectedExternalIds = null;
        List<PortExternalIds> updateFromConfigurationExternalIds = null;
        List<PortExternalIds> updateFromOperationalExternalIds = null;
        Map<String, List<PortExternalIds>> updateToTestCase = null;
        List<PortExternalIds> updateToInputExternalIds = null;
        List<PortExternalIds> updateToExpectedExternalIds = null;
        List<PortExternalIds> updateToConfigurationExternalIds = null;
        List<PortExternalIds> updateToOperationalExternalIds = null;
        String testBridgeName = null;
        String testPortName = null;
        OvsdbTerminationPointAugmentation updateFromConfigurationTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentation updateFromOperationalTerminationPointAugmenation = null;
        OvsdbTerminationPointAugmentation updateToConfigurationTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentation updateToOperationalTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentationBuilder tpCreateAugmentationBuilder = null;
        OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder = null;
        TerminationPointBuilder tpUpdateBuilder = null;
        NodeBuilder portUpdateNodeBuilder = null;
        NodeId testBridgeNodeId = null;
        NodeId portUpdateNodeId = null;
        InstanceIdentifier<Node> portIid = null;
        boolean result = false;

        for (String updateFromTestCaseKey : updateFromTestCases.keySet()) {
            updateFromTestCase = updateFromTestCases.get(updateFromTestCaseKey);
            updateFromInputExternalIds = updateFromTestCase.get(INPUT_VALUES_KEY);
            updateFromExpectedExternalIds = updateFromTestCase.get(EXPECTED_VALUES_KEY);
            for (String testCaseKey : updateToTestCases.keySet()) {
                testPortName = testBridgeName = String.format("%s_%s", TEST_PREFIX, testCaseKey);
                updateToTestCase = updateToTestCases.get(testCaseKey);
                updateToInputExternalIds = updateToTestCase.get(INPUT_VALUES_KEY);
                updateToExpectedExternalIds = updateToTestCase.get(EXPECTED_VALUES_KEY);

                // CREATE: Create the test bridge
                Assert.assertTrue(addBridge(connectionInfo, null,
                        testBridgeName, null, true, SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"),
                        true, null, null, null));
                testBridgeNodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                        connectionInfo, new OvsdbBridgeName(testBridgeName)));
                tpCreateAugmentationBuilder = createGenericOvsdbTerminationPointAugmentationBuilder();
                tpCreateAugmentationBuilder.setName(testPortName);
                tpCreateAugmentationBuilder.setPortExternalIds(updateFromInputExternalIds);
                Assert.assertTrue(addTerminationPoint(testBridgeNodeId, testPortName, tpCreateAugmentationBuilder));

                // READ: Read the test port and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                updateFromConfigurationTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                updateFromConfigurationExternalIds = updateFromConfigurationTerminationPointAugmentation
                        .getPortExternalIds();
                assertExpectedPortExternalIdsExist(updateFromExpectedExternalIds, updateFromConfigurationExternalIds);
                updateFromOperationalTerminationPointAugmenation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                updateFromOperationalExternalIds = updateFromOperationalTerminationPointAugmenation
                        .getPortExternalIds();
                assertExpectedPortExternalIdsExist(updateFromExpectedExternalIds, updateFromOperationalExternalIds);

                // UPDATE:  update the external_ids
                testBridgeNodeId = getBridgeNode(connectionInfo, testBridgeName).getNodeId();
                tpUpdateAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
                tpUpdateAugmentationBuilder.setPortExternalIds(updateToInputExternalIds);
                portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
                portUpdateNodeBuilder = new NodeBuilder();
                portUpdateNodeId = SouthboundMapper.createManagedNodeId(portIid);
                portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
                tpUpdateBuilder = new TerminationPointBuilder();
                tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(testPortName)));
                tpUpdateBuilder.addAugmentation(
                        OvsdbTerminationPointAugmentation.class,
                        tpUpdateAugmentationBuilder.build());
                portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
                result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                        portIid, portUpdateNodeBuilder.build());
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
                Assert.assertTrue(result);

                // READ: the test port and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                updateToConfigurationTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                updateToConfigurationExternalIds = updateToConfigurationTerminationPointAugmentation
                        .getPortExternalIds();
                assertExpectedPortExternalIdsExist(updateToExpectedExternalIds, updateToConfigurationExternalIds);
                updateToOperationalTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                updateToOperationalExternalIds = updateToOperationalTerminationPointAugmentation.getPortExternalIds();
                assertExpectedPortExternalIdsExist(updateToExpectedExternalIds, updateToOperationalExternalIds);

                // Make sure the old port external ids aren't present in the CONFIGURATION data store
                assertPortExternalIdsErased(updateFromInputExternalIds, updateToInputExternalIds,
                        updateFromExpectedExternalIds, updateToConfigurationExternalIds);
                assertPortExternalIdsErased(updateFromInputExternalIds, updateToInputExternalIds,
                        updateFromExpectedExternalIds, updateToConfigurationExternalIds);

                // DELETE
                Assert.assertTrue(deleteBridge(connectionInfo, testBridgeName));
            }
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    /*
     * Generates the test cases involved in testing InterfaceExternalIds.  See inline comments for descriptions of
     * the particular cases considered.
     *
     * The return value is a Map in the form (K,V)=(testCaseName,testCase).
     * - testCaseName is a String
     * - testCase is a Map in the form (K,V) s.t. K=(EXPECTED_VALUES_KEY|INPUT_VALUES_KEY) and V is a List of
     *     either corresponding INPUT interface external_ids, or EXPECTED interface external_ids
     *     INPUT    is the List we use when calling
     *              <code>TerminationPointAugmentationBuilder.setInterfaceExternalIds()</code>
     *     EXPECTED is the List we expect to receive after calling
     *              <code>TerminationPointAugmentationBuilder.getInterfaceExternalIds()</code>
     */
    private Map<String, Map<String, List<InterfaceExternalIds>>> generateInterfaceExternalIdsTestCases() {
        Map<String, Map<String, List<InterfaceExternalIds>>> testMap =
                new HashMap<String, Map<String, List<InterfaceExternalIds>>>();

        final String INTERFACE_EXTERNAL_ID_KEY = "IntExternalIdKey";
        final String INTERFACE_EXTERNAL_ID_VALUE = "IntExternalIdValue";
        final String FORMAT_STR = "%s_%s_%d";
        final String GOOD_KEY = "GoodKey";
        final String GOOD_VALUE = "GoodValue";
        final String NO_VALUE_FOR_KEY = "NoValueForKey";
        final String NO_KEY_FOR_VALUE = "NoKeyForValue";

        // Test Case 1:  TestOneExternalId
        // Test Type:    Positive
        // Description:  Create a termination point with one InterfaceExternalIds
        // Expected:     A termination point is created with the single external_ids specified below
        final String testOneExternalIdName = "TestOneExternalId";
        int externalIdCounter = 0;
        List<InterfaceExternalIds> oneExternalId = (List<InterfaceExternalIds>) Lists.newArrayList(
            (new InterfaceExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testOneExternalIdName,
                            INTERFACE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testOneExternalIdName,
                            INTERFACE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()));
        Map<String,List<InterfaceExternalIds>> testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, oneExternalId);
        testCase.put(INPUT_VALUES_KEY, oneExternalId);
        testMap.put(testOneExternalIdName, testCase);

        // Test Case 2:  TestFiveExternalId
        // Test Type:    Positive
        // Description:  Create a termination point with multiple (five) InterfaceExternalIds
        // Expected:     A termination point is created with the five external_ids specified below
        final String testFiveExternalIdName = "TestFiveExternalId";
        externalIdCounter = 0;
        List<InterfaceExternalIds> fiveExternalId = (List<InterfaceExternalIds>) Lists.newArrayList(
            (new InterfaceExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            INTERFACE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            INTERFACE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new InterfaceExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            INTERFACE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            INTERFACE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new InterfaceExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            INTERFACE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            INTERFACE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new InterfaceExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            INTERFACE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            INTERFACE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new InterfaceExternalIdsBuilder()
                .setExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                        INTERFACE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            INTERFACE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()));
        testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, fiveExternalId);
        testCase.put(INPUT_VALUES_KEY, fiveExternalId);
        testMap.put(testOneExternalIdName, testCase);

        // Test Case 3:  TestOneGoodExternalIdOneMalformedExternalIdValue
        // Test Type:    Negative
        // Description:
        //     One perfectly fine InterfaceExternalId
        //        (TestOneGoodExternalIdOneMalformedExternalIdValue_IntExternalIdKey_1,
        //        TestOneGoodExternalIdOneMalformedExternalId_IntExternalIdValue_1)
        //     and one malformed PortExternalId which only has key specified
        //        (TestOneGoodExternalIdOneMalformedExternalIdValue_NoValueForKey_2,
        //        UNSPECIFIED)
        // Expected:     A termination point is created without any external_ids
        final String testOneGoodExternalIdOneMalformedExternalIdValueName =
                "TestOneGoodExternalIdOneMalformedExternalIdValue";
        externalIdCounter = 0;
        InterfaceExternalIds oneGood = new InterfaceExternalIdsBuilder()
            .setExternalIdKey(String.format(FORMAT_STR, testOneGoodExternalIdOneMalformedExternalIdValueName,
                    GOOD_KEY, ++externalIdCounter))
                .setExternalIdValue(String.format("FORMAT_STR",
                        testOneGoodExternalIdOneMalformedExternalIdValueName,
                            GOOD_VALUE, externalIdCounter))
                .build();
        InterfaceExternalIds oneBad = new InterfaceExternalIdsBuilder()
            .setExternalIdKey(String.format(FORMAT_STR,
                    testOneGoodExternalIdOneMalformedExternalIdValueName, NO_VALUE_FOR_KEY, ++externalIdCounter))
                .build();
        List<InterfaceExternalIds> oneGoodOneBadInput = (List<InterfaceExternalIds>) Lists.newArrayList(
                oneGood, oneBad);
        List<InterfaceExternalIds> oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);

        // Test Case 4:  TestOneGoodExternalIdOneMalformedExternalIdKey
        // Test Type:    Negative
        // Description:
        //     One perfectly fine InterfaceExternalId
        //        (TestOneGoodExternalIdOneMalformedExternalIdValue_IntExternalIdKey_1,
        //        TestOneGoodExternalIdOneMalformedExternalId_IntExternalIdValue_1)
        //     and one malformed BridgeExternalId which only has key specified
        //        (UNSPECIFIED,
        //        TestOneGoodExternalIdOneMalformedExternalIdKey_NoKeyForValue_2)
        // Expected:     A termination point is created without any external_ids
        final String testOneGoodExternalIdOneMalformedExternalIdKeyName =
                "TestOneGoodExternalIdOneMalformedExternalIdKey";
        externalIdCounter = 0;
        oneGood = new InterfaceExternalIdsBuilder()
            .setExternalIdKey(String.format(FORMAT_STR, testOneGoodExternalIdOneMalformedExternalIdKeyName,
                    GOOD_KEY, ++externalIdCounter))
                .setExternalIdValue(String.format("FORMAT_STR",
                        testOneGoodExternalIdOneMalformedExternalIdKeyName,
                            GOOD_VALUE, externalIdCounter))
                .build();
        oneBad = new InterfaceExternalIdsBuilder()
            .setExternalIdKey(String.format(FORMAT_STR,
                    testOneGoodExternalIdOneMalformedExternalIdKeyName, NO_KEY_FOR_VALUE, ++externalIdCounter))
                .build();
        oneGoodOneBadInput = (List<InterfaceExternalIds>) Lists.newArrayList(
                oneGood, oneBad);
        oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);

        return testMap;
    }

    /*
     * @see <code>SouthboundIT.testCRUDInterfaceExternalIds()</code>
     * This is helper test method to compare a test "set" of InterfaceExternalIds against an expected "set"
     */
    private void assertExpectedInterfaceExternalIdsExist( List<InterfaceExternalIds> expected,
            List<InterfaceExternalIds> test ) {

        if (expected != null) {
            for (InterfaceExternalIds expectedExternalId : expected) {
                Assert.assertTrue(test.contains(expectedExternalId));
            }
        } else {
            Assert.assertNull(test);
        }
    }

    /*
     * @see <code>SouthboundIT.testCRUDInterfaceExternalIds()</code>
     * This is a helper test method.  The method only checks if
     * <code>updateFromInputExternalIds != updateToInputExternalIds</code>,  (i.e., the updateTo "set" isn't the same
     * as the updateFrom "set".  Then, the method ensures each element of erase is not an element of test, as the input
     * test cases are divergent.
     */
    private void assertInterfaceExternalIdsErased( List<InterfaceExternalIds> updateFromInputExternalIds,
            List<InterfaceExternalIds> updateToInputExternalIds,
            List<InterfaceExternalIds> updateFromExpectedExternalIds,
            List<InterfaceExternalIds> updateToTestExternalIds ) {

        if (!updateFromInputExternalIds.containsAll(updateToInputExternalIds)) {
            for (InterfaceExternalIds erasedExternalId : updateFromExpectedExternalIds) {
                Assert.assertTrue(!updateToTestExternalIds.contains(erasedExternalId));
            }
        }
    }

    /*
     * Tests the CRUD operations for <code>Interface</code> <code>external_ids</code>.
     *
     * @see <code>SouthboundIT.generateInterfaceExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointInterfaceExternalIds() throws InterruptedException {
        final String TEST_PREFIX = "CRUDTPInterfaceExternalIds";
        final int TERMINATION_POINT_TEST_INDEX = 0;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        Map<String, Map<String, List<InterfaceExternalIds>>> updateFromTestCases =
                generateInterfaceExternalIdsTestCases();
        Map<String, Map<String, List<InterfaceExternalIds>>> updateToTestCases =
                generateInterfaceExternalIdsTestCases();
        Map<String, List<InterfaceExternalIds>> updateFromTestCase = null;
        List<InterfaceExternalIds> updateFromInputExternalIds = null;
        List<InterfaceExternalIds> updateFromExpectedExternalIds = null;
        List<InterfaceExternalIds> updateFromConfigurationExternalIds = null;
        List<InterfaceExternalIds> updateFromOperationalExternalIds = null;
        Map<String, List<InterfaceExternalIds>> updateToTestCase = null;
        List<InterfaceExternalIds> updateToInputExternalIds = null;
        List<InterfaceExternalIds> updateToExpectedExternalIds = null;
        List<InterfaceExternalIds> updateToConfigurationExternalIds = null;
        List<InterfaceExternalIds> updateToOperationalExternalIds = null;
        String testBridgeName = null;
        String testPortName = null;
        OvsdbTerminationPointAugmentation updateFromConfigurationTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentation updateFromOperationalTerminationPointAugmenation = null;
        OvsdbTerminationPointAugmentation updateToConfigurationTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentation updateToOperationalTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentationBuilder tpCreateAugmentationBuilder = null;
        OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder = null;
        TerminationPointBuilder tpUpdateBuilder = null;
        NodeBuilder portUpdateNodeBuilder = null;
        NodeId testBridgeNodeId = null;
        NodeId portUpdateNodeId = null;
        InstanceIdentifier<Node> portIid = null;
        boolean result = false;

        for (String updateFromTestCaseKey : updateFromTestCases.keySet()) {
            updateFromTestCase = updateFromTestCases.get(updateFromTestCaseKey);
            updateFromInputExternalIds = updateFromTestCase.get(INPUT_VALUES_KEY);
            updateFromExpectedExternalIds = updateFromTestCase.get(EXPECTED_VALUES_KEY);
            for (String testCaseKey : updateToTestCases.keySet()) {
                testPortName = testBridgeName = String.format("%s_%s", TEST_PREFIX, testCaseKey);
                updateToTestCase = updateToTestCases.get(testCaseKey);
                updateToInputExternalIds = updateToTestCase.get(INPUT_VALUES_KEY);
                updateToExpectedExternalIds = updateToTestCase.get(EXPECTED_VALUES_KEY);

                // CREATE: Create the test interface
                Assert.assertTrue(addBridge(connectionInfo, null,
                        testBridgeName, null, true, SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"),
                        true, null, null, null));
                testBridgeNodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                        connectionInfo, new OvsdbBridgeName(testBridgeName)));
                tpCreateAugmentationBuilder = createGenericOvsdbTerminationPointAugmentationBuilder();
                tpCreateAugmentationBuilder.setName(testPortName);
                tpCreateAugmentationBuilder.setInterfaceExternalIds(updateFromInputExternalIds);
                Assert.assertTrue(addTerminationPoint(testBridgeNodeId, testPortName, tpCreateAugmentationBuilder));

                // READ: Read the test interface and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                updateFromConfigurationTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                updateFromConfigurationExternalIds = updateFromConfigurationTerminationPointAugmentation
                        .getInterfaceExternalIds();
                assertExpectedInterfaceExternalIdsExist(updateFromExpectedExternalIds,
                        updateFromConfigurationExternalIds);
                updateFromOperationalTerminationPointAugmenation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                updateFromOperationalExternalIds = updateFromOperationalTerminationPointAugmenation
                        .getInterfaceExternalIds();
                assertExpectedInterfaceExternalIdsExist(updateFromExpectedExternalIds,
                        updateFromOperationalExternalIds);

                // UPDATE:  update the external_ids
                testBridgeNodeId = getBridgeNode(connectionInfo, testBridgeName).getNodeId();
                tpUpdateAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
                tpUpdateAugmentationBuilder.setInterfaceExternalIds(updateToInputExternalIds);
                portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
                portUpdateNodeBuilder = new NodeBuilder();
                portUpdateNodeId = SouthboundMapper.createManagedNodeId(portIid);
                portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
                tpUpdateBuilder = new TerminationPointBuilder();
                tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(testPortName)));
                tpUpdateBuilder.addAugmentation(
                        OvsdbTerminationPointAugmentation.class,
                        tpUpdateAugmentationBuilder.build());
                portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
                result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                        portIid, portUpdateNodeBuilder.build());
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
                Assert.assertTrue(result);

                // READ: the test interface and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                updateToConfigurationTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                updateToConfigurationExternalIds = updateToConfigurationTerminationPointAugmentation
                        .getInterfaceExternalIds();
                assertExpectedInterfaceExternalIdsExist(updateToExpectedExternalIds, updateToConfigurationExternalIds);
                updateToOperationalTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                updateToOperationalExternalIds = updateToOperationalTerminationPointAugmentation
                        .getInterfaceExternalIds();
                assertExpectedInterfaceExternalIdsExist(updateToExpectedExternalIds, updateToOperationalExternalIds);

                // Make sure the old interface external ids aren't present in the CONFIGURATION data store
                assertInterfaceExternalIdsErased(updateFromInputExternalIds, updateToInputExternalIds,
                        updateFromExpectedExternalIds, updateToConfigurationExternalIds);
                assertInterfaceExternalIdsErased(updateFromInputExternalIds, updateToInputExternalIds,
                        updateFromExpectedExternalIds, updateToConfigurationExternalIds);

                // DELETE
                Assert.assertTrue(deleteBridge(connectionInfo, testBridgeName));
            }
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    /*
     * Generates the test cases involved in testing TP Options.  See inline comments for descriptions of
     * the particular cases considered.
     *
     * The return value is a Map in the form (K,V)=(testCaseName,testCase).
     * - testCaseName is a String
     * - testCase is a Map in the form (K,V) s.t. K=(EXPECTED_VALUES_KEY|INPUT_VALUES_KEY) and V is a List of
     *     either corresponding INPUT TP Options, or EXPECTED TP Options
     *     INPUT    is the List we use when calling
     *              <code>TerminationPointAugmentationBuilder.setOptions()</code>
     *     EXPECTED is the List we expect to receive after calling
     *              <code>TerminationPointAugmentationBuilder.getOptions()</code>
     */
    private Map<String, Map<String, List<Options>>> generateTerminationPointOptionsTestCases() {
        Map<String, Map<String, List<Options>>> testMap =
                new HashMap<String, Map<String, List<Options>>>();

        final String TP_OPTIONS_KEY = "TPOptionsKey";
        final String TP_OPTIONS_VALUE = "TPOptionsValue";
        final String FORMAT_STR = "%s_%s_%d";
        final String GOOD_KEY = "GoodKey";
        final String GOOD_VALUE = "GoodValue";
        final String NO_VALUE_FOR_KEY = "NoValueForKey";
        final String NO_KEY_FOR_VALUE = "NoKeyForValue";

        // Test Case 1:  TestOneOptions
        // Test Type:    Positive
        // Description:  Create a termination point with one Options
        // Expected:     A termination point is created with the single Options specified below
        final String testOneOptionsName = "TestOneOptions";
        int optionsCounter = 0;
        List<Options> oneOptions = (List<Options>) Lists.newArrayList(
            (new OptionsBuilder()
                .setOption(String.format(FORMAT_STR, testOneOptionsName,
                            TP_OPTIONS_KEY, ++optionsCounter))
                    .setValue(String.format(FORMAT_STR, testOneOptionsName,
                            TP_OPTIONS_VALUE, optionsCounter))
                    .build()));
        Map<String,List<Options>> testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, oneOptions);
        testCase.put(INPUT_VALUES_KEY, oneOptions);
        testMap.put(testOneOptionsName, testCase);

        // Test Case 2:  TestFiveOptions
        // Test Type:    Positive
        // Description:  Create a termination point with multiple (five) Options
        // Expected:     A termination point is created with the five options specified below
        final String testFiveOptions = "TestFiveOptions";
        optionsCounter = 0;
        List<Options> fiveOptions = (List<Options>) Lists.newArrayList(
            (new OptionsBuilder()
                .setOption(String.format(FORMAT_STR, testFiveOptions,
                            TP_OPTIONS_KEY, ++optionsCounter))
                    .setValue(String.format(FORMAT_STR, testFiveOptions,
                            TP_OPTIONS_VALUE, optionsCounter))
                    .build()),
            (new OptionsBuilder()
                .setOption(String.format(FORMAT_STR, testFiveOptions,
                            TP_OPTIONS_KEY, ++optionsCounter))
                    .setValue(String.format(FORMAT_STR, testFiveOptions,
                            TP_OPTIONS_VALUE, optionsCounter))
                    .build()),
            (new OptionsBuilder()
                .setOption(String.format(FORMAT_STR, testFiveOptions,
                            TP_OPTIONS_KEY, ++optionsCounter))
                    .setValue(String.format(FORMAT_STR, testFiveOptions,
                            TP_OPTIONS_VALUE, optionsCounter))
                    .build()),
            (new OptionsBuilder()
                .setOption(String.format(FORMAT_STR, testFiveOptions,
                            TP_OPTIONS_KEY, ++optionsCounter))
                    .setValue(String.format(FORMAT_STR, testFiveOptions,
                            TP_OPTIONS_VALUE, optionsCounter))
                    .build()),
            (new OptionsBuilder()
                .setOption(String.format(FORMAT_STR, testFiveOptions,
                        TP_OPTIONS_KEY, ++optionsCounter))
                    .setValue(String.format(FORMAT_STR, testFiveOptions,
                            TP_OPTIONS_VALUE, optionsCounter))
                    .build()));
        testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, fiveOptions);
        testCase.put(INPUT_VALUES_KEY, fiveOptions);
        testMap.put(testOneOptionsName, testCase);

        // Test Case 3:  TestOneGoodOptionsOneMalformedOptionsValue
        // Test Type:    Negative
        // Description:
        //     One perfectly fine Options
        //        (TestOneGoodOptionsOneMalformedOptionsValue_OptionsKey_1,
        //        TestOneGoodOptionsOneMalformedOptions_OptionsValue_1)
        //     and one malformed Options which only has key specified
        //        (TestOneGoodOptionsOneMalformedOptionsValue_NoValueForKey_2,
        //        UNSPECIFIED)
        // Expected:     A termination point is created without any options
        final String testOneGoodOptionsOneMalformedOptionsValueName =
                "TestOneGoodOptionsOneMalformedOptionsValue";
        optionsCounter = 0;
        Options oneGood = new OptionsBuilder()
            .setOption(String.format(FORMAT_STR, testOneGoodOptionsOneMalformedOptionsValueName,
                    GOOD_KEY, ++optionsCounter))
                .setValue(String.format("FORMAT_STR",
                        testOneGoodOptionsOneMalformedOptionsValueName,
                            GOOD_VALUE, optionsCounter))
                .build();
        Options oneBad = new OptionsBuilder()
            .setOption(String.format(FORMAT_STR,
                    testOneGoodOptionsOneMalformedOptionsValueName, NO_VALUE_FOR_KEY, ++optionsCounter))
                .build();
        List<Options> oneGoodOneBadInput = (List<Options>) Lists.newArrayList(
                oneGood, oneBad);
        List<Options> oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);

        // Test Case 4:  TestOneGoodOptionsOneMalformedOptionsKey
        // Test Type:    Negative
        // Description:
        //     One perfectly fine Options
        //        (TestOneGoodOptionsOneMalformedOptionsValue_OptionsKey_1,
        //        TestOneGoodOptionsOneMalformedOptions_OptionsValue_1)
        //     and one malformed Options which only has key specified
        //        (UNSPECIFIED,
        //        TestOneGoodOptionsOneMalformedOptionsKey_NoKeyForValue_2)
        // Expected:     A termination point is created without any options
        final String testOneGoodOptionsOneMalformedOptionsKeyName =
                "TestOneGoodOptionsOneMalformedOptionsKey";
        optionsCounter = 0;
        oneGood = new OptionsBuilder()
            .setOption(String.format(FORMAT_STR, testOneGoodOptionsOneMalformedOptionsKeyName,
                    GOOD_KEY, ++optionsCounter))
                .setValue(String.format("FORMAT_STR",
                        testOneGoodOptionsOneMalformedOptionsKeyName,
                            GOOD_VALUE, optionsCounter))
                .build();
        oneBad = new OptionsBuilder()
            .setOption(String.format(FORMAT_STR,
                    testOneGoodOptionsOneMalformedOptionsKeyName, NO_KEY_FOR_VALUE, ++optionsCounter))
                .build();
        oneGoodOneBadInput = (List<Options>) Lists.newArrayList(
                oneGood, oneBad);
        oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);

        return testMap;
    }

    /*
     * @see <code>SouthboundIT.testCRUDTerminationPointOptions()</code>
     * This is helper test method to compare a test "set" of Options against an expected "set"
     */
    private void assertExpectedOptionsExist( List<Options> expected,
            List<Options> test ) {

        if (expected != null) {
            for (Options expectedOption : expected) {
                Assert.assertTrue(test.contains(expectedOption));
            }
        } else {
            Assert.assertNull(test);
        }
    }

    /*
     * @see <code>SouthboundIT.testCRUDTPOptions()</code>
     * This is a helper test method.  The method only checks if
     * <code>updateFromInputOptions != updateToInputOptions</code>,  (i.e., the updateTo "set" isn't the same
     * as the updateFrom "set".  Then, the method ensures each element of erase is not an element of test, as the input
     * test cases are divergent.
     */
    private void assertOptionsErased( List<Options> updateFromInputOptions,
            List<Options> updateToInputOptions,
            List<Options> updateFromExpectedOptions,
            List<Options> updateToTestOptions ) {

        if (!updateFromInputOptions.containsAll(updateToInputOptions)) {
            for (Options erasedOption : updateFromExpectedOptions) {
                Assert.assertTrue(!updateToTestOptions.contains(erasedOption));
            }
        }
    }

    /*
     * Tests the CRUD operations for <code>TerminationPoint</code> <code>options</code>.
     *
     * @see <code>SouthboundIT.generateTerminationPointOptions()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointOptions() throws InterruptedException {
        final String TEST_PREFIX = "CRUDTPOptions";
        final int TERMINATION_POINT_TEST_INDEX = 0;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        Map<String, Map<String, List<Options>>> updateFromTestCases =
                generateTerminationPointOptionsTestCases();
        Map<String, Map<String, List<Options>>> updateToTestCases =
                generateTerminationPointOptionsTestCases();
        Map<String, List<Options>> updateFromTestCase = null;
        List<Options> updateFromInputOptions = null;
        List<Options> updateFromExpectedOptions = null;
        List<Options> updateFromConfigurationOptions = null;
        List<Options> updateFromOperationalOptions = null;
        Map<String, List<Options>> updateToTestCase = null;
        List<Options> updateToInputOptions = null;
        List<Options> updateToExpectedOptions = null;
        List<Options> updateToConfigurationOptions = null;
        List<Options> updateToOperationalOptions = null;
        String testBridgeName = null;
        String testPortName = null;
        OvsdbTerminationPointAugmentation updateFromConfigurationTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentation updateFromOperationalTerminationPointAugmenation = null;
        OvsdbTerminationPointAugmentation updateToConfigurationTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentation updateToOperationalTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentationBuilder tpCreateAugmentationBuilder = null;
        OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder = null;
        TerminationPointBuilder tpUpdateBuilder = null;
        NodeBuilder portUpdateNodeBuilder = null;
        NodeId testBridgeNodeId = null;
        NodeId portUpdateNodeId = null;
        InstanceIdentifier<Node> portIid = null;
        boolean result = false;

        for (String updateFromTestCaseKey : updateFromTestCases.keySet()) {
            updateFromTestCase = updateFromTestCases.get(updateFromTestCaseKey);
            updateFromInputOptions = updateFromTestCase.get(INPUT_VALUES_KEY);
            updateFromExpectedOptions = updateFromTestCase.get(EXPECTED_VALUES_KEY);
            for (String testCaseKey : updateToTestCases.keySet()) {
                testPortName = testBridgeName = String.format("%s_%s", TEST_PREFIX, testCaseKey);
                updateToTestCase = updateToTestCases.get(testCaseKey);
                updateToInputOptions = updateToTestCase.get(INPUT_VALUES_KEY);
                updateToExpectedOptions = updateToTestCase.get(EXPECTED_VALUES_KEY);

                // CREATE: Create the test interface
                Assert.assertTrue(addBridge(connectionInfo, null,
                        testBridgeName, null, true, SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"),
                        true, null, null, null));
                testBridgeNodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                        connectionInfo, new OvsdbBridgeName(testBridgeName)));
                tpCreateAugmentationBuilder = createGenericOvsdbTerminationPointAugmentationBuilder();
                tpCreateAugmentationBuilder.setName(testPortName);
                tpCreateAugmentationBuilder.setOptions(updateFromInputOptions);
                Assert.assertTrue(addTerminationPoint(testBridgeNodeId, testPortName, tpCreateAugmentationBuilder));

                // READ: Read the test interface and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                updateFromConfigurationTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                updateFromConfigurationOptions = updateFromConfigurationTerminationPointAugmentation
                        .getOptions();
                assertExpectedOptionsExist(updateFromExpectedOptions,
                        updateFromConfigurationOptions);
                updateFromOperationalTerminationPointAugmenation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                updateFromOperationalOptions = updateFromOperationalTerminationPointAugmenation
                        .getOptions();
                assertExpectedOptionsExist(updateFromExpectedOptions,
                        updateFromOperationalOptions);

                // UPDATE:  update the external_ids
                testBridgeNodeId = getBridgeNode(connectionInfo, testBridgeName).getNodeId();
                tpUpdateAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
                tpUpdateAugmentationBuilder.setOptions(updateToInputOptions);
                portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
                portUpdateNodeBuilder = new NodeBuilder();
                portUpdateNodeId = SouthboundMapper.createManagedNodeId(portIid);
                portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
                tpUpdateBuilder = new TerminationPointBuilder();
                tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(testPortName)));
                tpUpdateBuilder.addAugmentation(
                        OvsdbTerminationPointAugmentation.class,
                        tpUpdateAugmentationBuilder.build());
                portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
                result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                        portIid, portUpdateNodeBuilder.build());
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
                Assert.assertTrue(result);

                // READ: the test interface and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                updateToConfigurationTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                updateToConfigurationOptions = updateToConfigurationTerminationPointAugmentation
                        .getOptions();
                assertExpectedOptionsExist(updateToExpectedOptions, updateToConfigurationOptions);
                updateToOperationalTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                updateToOperationalOptions = updateToOperationalTerminationPointAugmentation
                        .getOptions();
                assertExpectedOptionsExist(updateToExpectedOptions, updateToOperationalOptions);

                // Make sure the old interface external ids aren't present in the CONFIGURATION data store
                assertOptionsErased(updateFromInputOptions, updateToInputOptions,
                        updateFromExpectedOptions, updateToConfigurationOptions);
                assertOptionsErased(updateFromInputOptions, updateToInputOptions,
                        updateFromExpectedOptions, updateToConfigurationOptions);

                // DELETE
                Assert.assertTrue(deleteBridge(connectionInfo, testBridgeName));
            }
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testTerminationPointInterfaceOtherConfigs() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                connectionInfo, bridge.getBridgeName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        String portName = "testInterfaceOtherConfigs";
        ovsdbTerminationBuilder.setName(portName);
        //setup
        InterfaceOtherConfigsBuilder interfaceBuilder1 = new InterfaceOtherConfigsBuilder();
        interfaceBuilder1.setOtherConfigKey("interfaceOtherConfigsKey1");
        interfaceBuilder1.setOtherConfigValue("interfaceOtherConfigsValue1");
        InterfaceOtherConfigsBuilder interfaceBuilder2 = new InterfaceOtherConfigsBuilder();
        interfaceBuilder2.setOtherConfigKey("interfaceOtherConfigsKey2");
        interfaceBuilder2.setOtherConfigValue("interfaceOtherConfigsValue2");
        List<InterfaceOtherConfigs> interfaceOtherConfigs = Lists.newArrayList(interfaceBuilder1.build(),
                interfaceBuilder2.build());
        ovsdbTerminationBuilder.setInterfaceOtherConfigs(interfaceOtherConfigs);

        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        Thread.sleep(1000);
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);

        List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPoints) {
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                List<InterfaceOtherConfigs> actualInterfaceOtherConfigs = ovsdbTerminationPointAugmentation.
                        getInterfaceOtherConfigs();
                Assert.assertNotNull(actualInterfaceOtherConfigs);
                Assert.assertNotNull(interfaceOtherConfigs);
                Assert.assertTrue(interfaceOtherConfigs.size() == actualInterfaceOtherConfigs.size());
                for (InterfaceOtherConfigs interfaceOtherConfig : interfaceOtherConfigs) {
                    Assert.assertTrue(actualInterfaceOtherConfigs.contains(interfaceOtherConfig));
                }
            }
        }
        Assert.assertTrue(deleteBridge(connectionInfo));
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    /*
     * Generates the test cases involved in testing Port other_configs.  See inline comments for descriptions of
     * the particular cases considered.
     *
     * The return value is a Map in the form (K,V)=(testCaseName,testCase).
     * - testCaseName is a String
     * - testCase is a Map in the form (K,V) s.t. K=(EXPECTED_VALUES_KEY|INPUT_VALUES_KEY) and V is a List of
     *     either corresponding INPUT port other_configs, or EXPECTED port other_configs
     *     INPUT    is the List we use when calling
     *              <code>TerminationPointAugmentationBuilder.setPortOtherConfigs()</code>
     *     EXPECTED is the List we expect to receive after calling
     *              <code>TerminationPointAugmentationBuilder.getPortOtherConfigs()</code>
     */
    private Map<String, Map<String, List<PortOtherConfigs>>> generatePortOtherConfigsTestCases() {
        Map<String, Map<String, List<PortOtherConfigs>>> testMap =
                new HashMap<String, Map<String, List<PortOtherConfigs>>>();

        final String PORT_OTHER_CONFIGS_KEY = "PortOtherConfigsKey";
        final String PORT_OTHER_CONFIGS_VALUE = "PortOtherConfigsValue";
        final String FORMAT_STR = "%s_%s_%d";
        final String GOOD_KEY = "GoodKey";
        final String GOOD_VALUE = "GoodValue";
        final String NO_VALUE_FOR_KEY = "NoValueForKey";
        final String NO_KEY_FOR_VALUE = "NoKeyForValue";

        // Test Case 1:  TestOneOtherConfigs
        // Test Type:    Positive
        // Description:  Create an port with one other_Configs
        // Expected:     A port is created with the single other_configs specified below
        final String testOneOtherConfigsName = "TestOnePortOtherConfigs";
        int otherConfigsCounter = 0;
        List<PortOtherConfigs> oneOtherConfigs = (List<PortOtherConfigs>) Lists.newArrayList(
            (new PortOtherConfigsBuilder()
                .setOtherConfigKey(String.format(FORMAT_STR, testOneOtherConfigsName,
                            PORT_OTHER_CONFIGS_KEY, ++otherConfigsCounter))
                    .setOtherConfigValue(String.format(FORMAT_STR, testOneOtherConfigsName,
                            PORT_OTHER_CONFIGS_VALUE, otherConfigsCounter))
                    .build()));
        Map<String,List<PortOtherConfigs>> testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, oneOtherConfigs);
        testCase.put(INPUT_VALUES_KEY, oneOtherConfigs);
        testMap.put(testOneOtherConfigsName, testCase);

        // Test Case 2:  TestFivePortOtherConfigs
        // Test Type:    Positive
        // Description:  Create a termination point with multiple (five) PortOtherConfigs
        // Expected:     A termination point is created with the five PortOtherConfigs specified below
        final String testFivePortOtherConfigsName = "TestFivePortOtherConfigs";
        otherConfigsCounter = 0;
        List<PortOtherConfigs> fivePortOtherConfigs = (List<PortOtherConfigs>) Lists.newArrayList(
            (new PortOtherConfigsBuilder()
                .setOtherConfigKey(String.format(FORMAT_STR, testFivePortOtherConfigsName,
                            PORT_OTHER_CONFIGS_KEY, ++otherConfigsCounter))
                    .setOtherConfigValue(String.format(FORMAT_STR, testFivePortOtherConfigsName,
                            PORT_OTHER_CONFIGS_VALUE, otherConfigsCounter))
                    .build()),
            (new PortOtherConfigsBuilder()
                .setOtherConfigKey(String.format(FORMAT_STR, testFivePortOtherConfigsName,
                            PORT_OTHER_CONFIGS_KEY, ++otherConfigsCounter))
                    .setOtherConfigValue(String.format(FORMAT_STR, testFivePortOtherConfigsName,
                            PORT_OTHER_CONFIGS_VALUE, otherConfigsCounter))
                    .build()),
            (new PortOtherConfigsBuilder()
                .setOtherConfigKey(String.format(FORMAT_STR, testFivePortOtherConfigsName,
                            PORT_OTHER_CONFIGS_KEY, ++otherConfigsCounter))
                    .setOtherConfigValue(String.format(FORMAT_STR, testFivePortOtherConfigsName,
                            PORT_OTHER_CONFIGS_VALUE, otherConfigsCounter))
                    .build()),
            (new PortOtherConfigsBuilder()
                .setOtherConfigKey(String.format(FORMAT_STR, testFivePortOtherConfigsName,
                            PORT_OTHER_CONFIGS_KEY, ++otherConfigsCounter))
                    .setOtherConfigValue(String.format(FORMAT_STR, testFivePortOtherConfigsName,
                            PORT_OTHER_CONFIGS_VALUE, otherConfigsCounter))
                    .build()),
            (new PortOtherConfigsBuilder()
                .setOtherConfigKey(String.format(FORMAT_STR, testFivePortOtherConfigsName,
                        PORT_OTHER_CONFIGS_KEY, ++otherConfigsCounter))
                    .setOtherConfigValue(String.format(FORMAT_STR, testFivePortOtherConfigsName,
                            PORT_OTHER_CONFIGS_VALUE, otherConfigsCounter))
                    .build()));
        testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, fivePortOtherConfigs);
        testCase.put(INPUT_VALUES_KEY, fivePortOtherConfigs);
        testMap.put(testFivePortOtherConfigsName, testCase);

        // Test Case 3:  TestOneGoodPortOtherConfigsOneMalformedPortOtherConfigsValue
        // Test Type:    Negative
        // Description:
        //     One perfectly fine PortOtherConfigs
        //        (TestOneGoodPortOtherConfigsOneMalformedPortOtherConfigsValue_PortOtherConfigsKey_1,
        //        TestOneGoodPortOtherConfigsOneMalformedPortOtherConfigs_PortOtherConfigsValue_1)
        //     and one malformed PortOtherConfigs which only has key specified
        //        (TestOneGoodPortOtherConfigsOneMalformedPortOtherConfigsValue_NoValueForKey_2,
        //        UNSPECIFIED)
        // Expected:     A termination point is created without any PortOtherConfigs
        final String testOneGoodPortOtherConfigsOneMalformedPortOtherConfigsValueName =
                "TestOneGoodPortOtherConfigsOneMalformedPortOtherConfigsValue";
        otherConfigsCounter = 0;
        PortOtherConfigs oneGood = new PortOtherConfigsBuilder()
            .setOtherConfigKey(String.format(FORMAT_STR,
                    testOneGoodPortOtherConfigsOneMalformedPortOtherConfigsValueName,
                    GOOD_KEY, ++otherConfigsCounter))
                .setOtherConfigValue(String.format(FORMAT_STR,
                        testOneGoodPortOtherConfigsOneMalformedPortOtherConfigsValueName,
                            GOOD_VALUE, otherConfigsCounter))
                .build();
        PortOtherConfigs oneBad = new PortOtherConfigsBuilder()
            .setOtherConfigKey(String.format(FORMAT_STR,
                    testOneGoodPortOtherConfigsOneMalformedPortOtherConfigsValueName, NO_VALUE_FOR_KEY,
                    ++otherConfigsCounter))
                .build();
        List<PortOtherConfigs> oneGoodOneBadInput = (List<PortOtherConfigs>) Lists.newArrayList(
                oneGood, oneBad);
        List<PortOtherConfigs> oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);
        testMap.put(testOneGoodPortOtherConfigsOneMalformedPortOtherConfigsValueName, testCase);

        // Test Case 4:  TestOneGoodPortOtherConfigsOneMalformedPortOtherConfigsKey
        // Test Type:    Negative
        // Description:
        //     One perfectly fine PortOtherConfigs
        //        (TestOneGoodPortOtherConfigsOneMalformedPortOtherConfigsValue_PortOtherConfigsKey_1,
        //        TestOneGoodPortOtherConfigsOneMalformedPortOtherConfigs_PortOtherConfigsValue_1)
        //     and one malformed PortOtherConfigs which only has key specified
        //        (UNSPECIFIED,
        //        TestOneGoodPortOtherConfigsOneMalformedPortOtherConfigsKey_NoKeyForValue_2)
        // Expected:     A termination point is created without any PortOtherConfigs
        final String testOneGoodPortOtherConfigsOneMalformedPortOtherConfigsKeyName =
                "TestOneGoodPortOtherConfigsOneMalformedPortOtherConfigsKey";
        otherConfigsCounter = 0;
        oneGood = new PortOtherConfigsBuilder()
            .setOtherConfigKey(String.format(FORMAT_STR,
                    testOneGoodPortOtherConfigsOneMalformedPortOtherConfigsKeyName,
                    GOOD_KEY, ++otherConfigsCounter))
                .setOtherConfigValue(String.format(FORMAT_STR,
                        testOneGoodPortOtherConfigsOneMalformedPortOtherConfigsKeyName,
                            GOOD_VALUE, otherConfigsCounter))
                .build();
        oneBad = new PortOtherConfigsBuilder()
            .setOtherConfigKey(String.format(FORMAT_STR,
                    testOneGoodPortOtherConfigsOneMalformedPortOtherConfigsKeyName, NO_KEY_FOR_VALUE,
                    ++otherConfigsCounter))
                .build();
        oneGoodOneBadInput = (List<PortOtherConfigs>) Lists.newArrayList(
                oneGood, oneBad);
        oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);
        testMap.put(testOneGoodPortOtherConfigsOneMalformedPortOtherConfigsKeyName, testCase);

        return testMap;
    }

    /*
     * @see <code>SouthboundIT.testCRUDPortOtherConfigs()</code>
     * This is helper test method to compare a test "set" of Options against an expected "set"
     */
    private void assertExpectedPortOtherConfigsExist( List<PortOtherConfigs> expected,
            List<PortOtherConfigs> test ) {

        if (expected != null && test != null) {
            for (PortOtherConfigs expectedOtherConfigs : expected) {
                Assert.assertTrue(test.contains(expectedOtherConfigs));
            }
        }
    }

    /*
     * Tests the CRUD operations for <code>Port</code> <code>other_configs</code>.
     *
     * @see <code>SouthboundIT.generatePortExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointPortOtherConfigs() throws InterruptedException {
        final String TEST_PREFIX = "CRUDTPPortOtherConfigs";
        final int TERMINATION_POINT_TEST_INDEX = 0;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        Map<String, Map<String, List<PortOtherConfigs>>> updateFromTestCases =
                generatePortOtherConfigsTestCases();
        Map<String, Map<String, List<PortOtherConfigs>>> updateToTestCases =
                generatePortOtherConfigsTestCases();
        Map<String, List<PortOtherConfigs>> updateFromTestCase = null;
        List<PortOtherConfigs> updateFromInputOtherConfigs = null;
        List<PortOtherConfigs> updateFromExpectedOtherConfigs = null;
        List<PortOtherConfigs> updateFromConfigurationOtherConfigs = null;
        List<PortOtherConfigs> updateFromOperationalOtherConfigs = null;
        Map<String, List<PortOtherConfigs>> updateToTestCase = null;
        List<PortOtherConfigs> updateToInputOtherConfigs = null;
        List<PortOtherConfigs> updateToExpectedOtherConfigs = null;
        List<PortOtherConfigs> updateToConfigurationOtherConfigs = null;
        List<PortOtherConfigs> updateToOperationalOtherConfigs = null;
        String testBridgeName = null;
        String testPortName = null;
        OvsdbTerminationPointAugmentation updateFromConfigurationTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentation updateFromOperationalTerminationPointAugmenation = null;
        OvsdbTerminationPointAugmentation updateToConfigurationTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentation updateToOperationalTerminationPointAugmentation = null;
        OvsdbTerminationPointAugmentationBuilder tpCreateAugmentationBuilder = null;
        OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder = null;
        TerminationPointBuilder tpUpdateBuilder = null;
        NodeBuilder portUpdateNodeBuilder = null;
        NodeId testBridgeNodeId = null;
        NodeId portUpdateNodeId = null;
        InstanceIdentifier<Node> portIid = null;
        boolean result = false;

        for (String updateFromTestCaseKey : updateFromTestCases.keySet()) {
            updateFromTestCase = updateFromTestCases.get(updateFromTestCaseKey);
            updateFromInputOtherConfigs = updateFromTestCase.get(INPUT_VALUES_KEY);
            updateFromExpectedOtherConfigs = updateFromTestCase.get(EXPECTED_VALUES_KEY);
            for (String testCaseKey : updateToTestCases.keySet()) {
                testPortName = testBridgeName = String.format("%s_%s", TEST_PREFIX, testCaseKey);
                updateToTestCase = updateToTestCases.get(testCaseKey);
                updateToInputOtherConfigs = updateToTestCase.get(INPUT_VALUES_KEY);
                updateToExpectedOtherConfigs = updateToTestCase.get(EXPECTED_VALUES_KEY);

                // CREATE: Create the test port
                Assert.assertTrue(addBridge(connectionInfo, null,
                        testBridgeName, null, true, SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"),
                        true, null, null, null));
                testBridgeNodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                        connectionInfo, new OvsdbBridgeName(testBridgeName)));
                tpCreateAugmentationBuilder = createGenericOvsdbTerminationPointAugmentationBuilder();
                tpCreateAugmentationBuilder.setName(testPortName);
                tpCreateAugmentationBuilder.setPortOtherConfigs(updateFromInputOtherConfigs);
                Assert.assertTrue(addTerminationPoint(testBridgeNodeId, testPortName, tpCreateAugmentationBuilder));

                // READ: Read the test port and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                updateFromConfigurationTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                if (updateFromConfigurationTerminationPointAugmentation != null) {
                    updateFromConfigurationOtherConfigs = updateFromConfigurationTerminationPointAugmentation
                        .getPortOtherConfigs();
                } else {
                    updateFromConfigurationOtherConfigs = null;
                }
                assertExpectedPortOtherConfigsExist(updateFromExpectedOtherConfigs,
                        updateFromConfigurationOtherConfigs);
                updateFromOperationalTerminationPointAugmenation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                if (updateFromOperationalOtherConfigs != null) {
                    updateFromOperationalOtherConfigs = updateFromOperationalTerminationPointAugmenation
                            .getPortOtherConfigs();
                } else {
                    updateFromOperationalOtherConfigs = null;
                }
                assertExpectedPortOtherConfigsExist(updateFromExpectedOtherConfigs,
                        updateFromOperationalOtherConfigs);

                // UPDATE:  update the other_configs
                testBridgeNodeId = getBridgeNode(connectionInfo, testBridgeName).getNodeId();
                tpUpdateAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
                tpUpdateAugmentationBuilder.setPortOtherConfigs(updateToInputOtherConfigs);
                portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
                portUpdateNodeBuilder = new NodeBuilder();
                portUpdateNodeId = SouthboundMapper.createManagedNodeId(portIid);
                portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
                tpUpdateBuilder = new TerminationPointBuilder();
                tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(testPortName)));
                tpUpdateBuilder.addAugmentation(
                        OvsdbTerminationPointAugmentation.class,
                        tpUpdateAugmentationBuilder.build());
                portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
                result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                        portIid, portUpdateNodeBuilder.build());
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
                Assert.assertTrue(result);

                // READ: the test port and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                updateToConfigurationTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                updateToConfigurationOtherConfigs = updateToConfigurationTerminationPointAugmentation
                        .getPortOtherConfigs();
                assertExpectedPortOtherConfigsExist(updateToExpectedOtherConfigs,
                        updateToConfigurationOtherConfigs);
                assertExpectedPortOtherConfigsExist(updateFromExpectedOtherConfigs,
                        updateToConfigurationOtherConfigs);
                updateToOperationalTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                updateToOperationalOtherConfigs = updateToOperationalTerminationPointAugmentation
                        .getPortOtherConfigs();
                if (updateFromExpectedOtherConfigs != null) {
                    assertExpectedPortOtherConfigsExist(updateToExpectedOtherConfigs,
                            updateToOperationalOtherConfigs);
                    assertExpectedPortOtherConfigsExist(updateFromExpectedOtherConfigs,
                            updateToOperationalOtherConfigs);
                }

                // DELETE
                Assert.assertTrue(deleteBridge(connectionInfo, testBridgeName));
            }
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testTerminationPointVlan() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                connectionInfo, bridge.getBridgeName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        String portName = "testTerminationPointVlanId";
        ovsdbTerminationBuilder.setName(portName);
        //setup
        Integer vlanId = new Integer(4000);
        ovsdbTerminationBuilder.setVlanTag(new VlanId(vlanId));

        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);

        List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPoints) {
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                //test
                VlanId actualVlanId = ovsdbTerminationPointAugmentation.getVlanTag();
                Assert.assertNotNull(actualVlanId);
                Integer actualVlanIdInt = actualVlanId.getValue();
                Assert.assertTrue(actualVlanIdInt.equals(vlanId));
            }
        }
        Assert.assertTrue(deleteBridge(connectionInfo));
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testTerminationPointVlanModes() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        VlanMode []vlanModes = VlanMode.values();
        for (VlanMode vlanMode : vlanModes) {

            Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            Assert.assertNotNull(bridge);
            NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                    connectionInfo, bridge.getBridgeName()));
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                    createGenericOvsdbTerminationPointAugmentationBuilder();
            String portName = "testTerminationPointVlanMode" + vlanMode.toString();
            ovsdbTerminationBuilder.setName(portName);
            //setup
            ovsdbTerminationBuilder.setVlanMode(vlanMode);
            Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
            InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
            Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            Assert.assertNotNull(terminationPointNode);

            List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
            for (TerminationPoint terminationPoint : terminationPoints) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    //test
                    Assert.assertTrue(ovsdbTerminationPointAugmentation.getVlanMode().equals(vlanMode));
                }
            }
            Assert.assertTrue(deleteBridge(connectionInfo));
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    private ArrayList<Set<Integer>> generateVlanSets() {
        ArrayList<Set<Integer>> vlanSets = new ArrayList<Set<Integer>>();

        Set<Integer> emptySet = new HashSet<Integer>();
        vlanSets.add(emptySet);

        Set<Integer> singleSet = new HashSet<Integer>();
        Integer single = new Integer(2222);
        singleSet.add(single);
        vlanSets.add(singleSet);

        Set<Integer> minMaxMiddleSet = new HashSet<Integer>();
        Integer min = new Integer(0);
        minMaxMiddleSet.add(min);
        Integer max = new Integer(4095);
        minMaxMiddleSet.add(max);
        Integer minPlusOne = new Integer(min + 1);
        minMaxMiddleSet.add(minPlusOne);
        Integer maxMinusOne = new Integer(max - 1);
        minMaxMiddleSet.add(maxMinusOne);
        Integer middle = new Integer((max - min) / 2);
        minMaxMiddleSet.add(middle);
        vlanSets.add(minMaxMiddleSet);

        return vlanSets;
    }

    private List<Trunks> buildTrunkList(Set<Integer> trunkSet) {
        List<Trunks> trunkList = Lists.newArrayList();
        for (Integer trunk : trunkSet) {
            TrunksBuilder trunkBuilder = new TrunksBuilder();
            trunkBuilder.setTrunk(new VlanId(trunk));
            trunkList.add(trunkBuilder.build());
        }
        return trunkList;
    }

    @Test
    public void testTerminationPointVlanTrunks() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        ArrayList<Set<Integer>> vlanSets = generateVlanSets();
        int testCase = 0;
        for (Set<Integer> vlanSet : vlanSets) {
            ++testCase;
            Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            Assert.assertNotNull(bridge);
            NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                    connectionInfo, bridge.getBridgeName()));
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                    createGenericOvsdbTerminationPointAugmentationBuilder();
            String portName = "testTerminationPointVlanTrunks" + testCase;
            ovsdbTerminationBuilder.setName(portName);
            //setup
            List<Trunks> trunks = buildTrunkList(vlanSet);
            ovsdbTerminationBuilder.setTrunks(trunks);
            Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
            InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
            Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            Assert.assertNotNull(terminationPointNode);

            List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
            for (TerminationPoint terminationPoint : terminationPoints) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    List<Trunks> actualTrunks = ovsdbTerminationPointAugmentation.getTrunks();
                    for (Trunks trunk : trunks) {
                        Assert.assertTrue(actualTrunks.contains(trunk));
                    }
                }
            }
            Assert.assertTrue(deleteBridge(connectionInfo));
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testGetOvsdbNodes() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        InstanceIdentifier<Topology> topologyPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        Topology topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, topologyPath);
        Assert.assertEquals("There should only be one node in the topology", 1, topology.getNode().size());
        InstanceIdentifier<Node> expectedNodeIid = SouthboundMapper.createInstanceIdentifier(connectionInfo);
        Node node = topology.getNode().iterator().next();
        NodeId expectedNodeId = expectedNodeIid.firstKeyOf(Node.class, NodeKey.class).getNodeId();
        Assert.assertEquals(expectedNodeId, node.getNodeId());
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    /*
     * Generates the test cases involved in testing BridgeExternalIds.  See inline comments for descriptions of
     * the particular cases considered.
     *
     * The return value is a Map in the form (K,V)=(testCaseName,testCase).
     * - testCaseName is a String
     * - testCase is a Map in the form (K,V) s.t. K=(EXPECTED_VALUES_KEY|INPUT_VALUES_KEY) and V is a List of
     *     either corresponding INPUT bridge external ids, or EXPECTED bridge external ids
     *     INPUT is the List we use when calling BridgeAugmentationBuilder.setBridgeExternalIds()
     *     EXPECTED is the List we expect to receive after calling BridgeAugmentationBuilder.getBridgeExternalIds()
     */
    private Map<String, Map<String, List<BridgeExternalIds>>> generateBridgeExternalIdsTestCases() {
        Map<String, Map<String, List<BridgeExternalIds>>> testMap =
                new HashMap<String, Map<String, List<BridgeExternalIds>>>();

        final String BRIDGE_EXTERNAL_ID_KEY = "BridgeExternalIdKey";
        final String BRIDGE_EXTERNAL_ID_VALUE = "BridgeExternalIdValue";
        final String FORMAT_STR = "%s_%s_%d";
        final String GOOD_KEY = "GoodKey";
        final String GOOD_VALUE = "GoodValue";
        final String NO_VALUE_FOR_KEY = "NoValueForKey";
        final String NO_KEY_FOR_VALUE = "NoKeyForValue";

        // Test Case 1:  TestOneExternalId
        // Test Type:    Positive
        // Description:  Create a bridge with one BridgeExternalIds
        // Expected:     A bridge is created with the single external_ids specified below
        final String testOneExternalIdName = "TestOneExternalId";
        int externalIdCounter = 0;
        List<BridgeExternalIds> oneExternalId = (List<BridgeExternalIds>) Lists.newArrayList(
            (new BridgeExternalIdsBuilder()
                .setBridgeExternalIdKey(String.format(FORMAT_STR, testOneExternalIdName,
                            BRIDGE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setBridgeExternalIdValue(String.format(FORMAT_STR, testOneExternalIdName,
                            BRIDGE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()));
        Map<String,List<BridgeExternalIds>> testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, oneExternalId);
        testCase.put(INPUT_VALUES_KEY, oneExternalId);
        testMap.put(testOneExternalIdName, testCase);

        // Test Case 2:  TestFiveExternalId
        // Test Type:    Positive
        // Description:  Create a bridge with multiple (five) BridgeExternalIds
        // Expected:     A bridge is created with the five external_ids specified below
        final String testFiveExternalIdName = "TestFiveExternalId";
        externalIdCounter = 0;
        List<BridgeExternalIds> fiveExternalId = (List<BridgeExternalIds>) Lists.newArrayList(
            (new BridgeExternalIdsBuilder()
                .setBridgeExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            BRIDGE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setBridgeExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            BRIDGE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new BridgeExternalIdsBuilder()
                .setBridgeExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            BRIDGE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setBridgeExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            BRIDGE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new BridgeExternalIdsBuilder()
                .setBridgeExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            BRIDGE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setBridgeExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            BRIDGE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new BridgeExternalIdsBuilder()
                .setBridgeExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                            BRIDGE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setBridgeExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            BRIDGE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()),
            (new BridgeExternalIdsBuilder()
                .setBridgeExternalIdKey(String.format(FORMAT_STR, testFiveExternalIdName,
                        BRIDGE_EXTERNAL_ID_KEY, ++externalIdCounter))
                    .setBridgeExternalIdValue(String.format(FORMAT_STR, testFiveExternalIdName,
                            BRIDGE_EXTERNAL_ID_VALUE, externalIdCounter))
                    .build()));
        testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, fiveExternalId);
        testCase.put(INPUT_VALUES_KEY, fiveExternalId);
        testMap.put(testOneExternalIdName, testCase);

        // Test Case 3:  TestOneGoodExternalIdOneMalformedExternalIdValue
        // Test Type:    Negative
        // Description:
        //     One perfectly fine BridgeExternalId
        //        (TestOneGoodExternalIdOneMalformedExternalIdValue_BridgeExternalIdKey_1,
        //        TestOneGoodExternalIdOneMalformedExternalId_BridgeExternalIdValue_1)
        //     and one malformed BridgeExternalId which only has key specified
        //        (TestOneGoodExternalIdOneMalformedExternalIdValue_NoValueForKey_2,
        //        UNSPECIFIED)
        // Expected:     A bridge is created without any external_ids
        final String testOneGoodExternalIdOneMalformedExternalIdValueName =
                "TestOneGoodExternalIdOneMalformedExternalIdValue";
        externalIdCounter = 0;
        BridgeExternalIds oneGood = new BridgeExternalIdsBuilder()
            .setBridgeExternalIdKey(String.format(FORMAT_STR, testOneGoodExternalIdOneMalformedExternalIdValueName,
                    GOOD_KEY, ++externalIdCounter))
                .setBridgeExternalIdValue(String.format("FORMAT_STR",
                        testOneGoodExternalIdOneMalformedExternalIdValueName,
                            GOOD_VALUE, externalIdCounter))
                .build();
        BridgeExternalIds oneBad = new BridgeExternalIdsBuilder()
            .setBridgeExternalIdKey(String.format(FORMAT_STR,
                    testOneGoodExternalIdOneMalformedExternalIdValueName, NO_VALUE_FOR_KEY, ++externalIdCounter))
                .build();
        List<BridgeExternalIds> oneGoodOneBadInput = (List<BridgeExternalIds>) Lists.newArrayList(
                oneGood, oneBad);
        List<BridgeExternalIds> oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);

        // Test Case 4:  TestOneGoodExternalIdOneMalformedExternalIdKey
        // Test Type:    Negative
        // Description:
        //     One perfectly fine BridgeExternalId
        //        (TestOneGoodExternalIdOneMalformedExternalIdValue_BridgeExternalIdKey_1,
        //        TestOneGoodExternalIdOneMalformedExternalId_BridgeExternalIdValue_1)
        //     and one malformed BridgeExternalId which only has key specified
        //        (UNSPECIFIED,
        //        TestOneGoodExternalIdOneMalformedExternalIdKey_NoKeyForValue_2)
        // Expected:     A bridge is created without any external_ids
        final String testOneGoodExternalIdOneMalformedExternalIdKeyName =
                "TestOneGoodExternalIdOneMalformedExternalIdKey";
        externalIdCounter = 0;
        oneGood = new BridgeExternalIdsBuilder()
            .setBridgeExternalIdKey(String.format(FORMAT_STR, testOneGoodExternalIdOneMalformedExternalIdKeyName,
                    GOOD_KEY, ++externalIdCounter))
                .setBridgeExternalIdValue(String.format("FORMAT_STR",
                        testOneGoodExternalIdOneMalformedExternalIdKeyName,
                            GOOD_VALUE, externalIdCounter))
                .build();
        oneBad = new BridgeExternalIdsBuilder()
            .setBridgeExternalIdKey(String.format(FORMAT_STR,
                    testOneGoodExternalIdOneMalformedExternalIdKeyName, NO_KEY_FOR_VALUE, ++externalIdCounter))
                .build();
        oneGoodOneBadInput = (List<BridgeExternalIds>) Lists.newArrayList(
                oneGood, oneBad);
        oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);

        return testMap;
    }

    /*
     * Generates the test cases involved in testing BridgeOtherConfigs.  See inline comments for descriptions of
     * the particular cases considered.
     *
     * The return value is a Map in the form (K,V)=(testCaseName,testCase).
     * - testCaseName is a String
     * - testCase is a Map in the form (K,V) s.t. K=(EXPECTED_VALUES_KEY|INPUT_VALUES_KEY) and V is a List of
     *     either corresponding INPUT bridge other_configs, or EXPECTED bridge other_configs
     *     INPUT is the List we use when calling BridgeAugmentationBuilder.setBridgeOtherConfigs()
     *     EXPECTED is the List we expect to receive after calling BridgeAugmentationBuilder.getBridgeOtherConfigs()
     */
    private Map<String, Map<String, List<BridgeOtherConfigs>>> generateBridgeOtherConfigsTestCases() {
        Map<String, Map<String, List<BridgeOtherConfigs>>> testMap =
                new HashMap<String, Map<String, List<BridgeOtherConfigs>>>();

        final String BRIDGE_OTHER_CONFIGS_KEY = "BridgeOtherConfigKey";
        final String BRIDGE_OTHER_CONFIGS_VALUE = "BridgeOtherConfigValue";
        final String FORMAT_STR = "%s_%s_%d";
        final String GOOD_KEY = "GoodKey";
        final String GOOD_VALUE = "GoodValue";
        final String NO_VALUE_FOR_KEY = "NoValueForKey";
        final String NO_KEY_FOR_VALUE = "NoKeyForValue";

        // Test Case 1:  TestOneOtherConfig
        // Test Type:    Positive
        // Description:  Create a bridge with one other_config
        // Expected:     A bridge is created with the single other_config specified below
        final String testOneOtherConfigName = "TestOneOtherConfig";
        int otherConfigCounter = 0;
        List<BridgeOtherConfigs> oneOtherConfig = (List<BridgeOtherConfigs>) Lists.newArrayList(
            (new BridgeOtherConfigsBuilder()
                .setBridgeOtherConfigKey(String.format(FORMAT_STR, testOneOtherConfigName,
                            BRIDGE_OTHER_CONFIGS_KEY, ++otherConfigCounter))
                    .setBridgeOtherConfigValue(String.format(FORMAT_STR, testOneOtherConfigName,
                            BRIDGE_OTHER_CONFIGS_VALUE, otherConfigCounter))
                    .build()));
        Map<String,List<BridgeOtherConfigs>> testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, oneOtherConfig);
        testCase.put(INPUT_VALUES_KEY, oneOtherConfig);
        testMap.put(testOneOtherConfigName, testCase);

        // Test Case 2:  TestFiveOtherConfig
        // Test Type:    Positive
        // Description:  Create a bridge with multiple (five) other_configs
        // Expected:     A bridge is created with the five other_configs specified below
        final String testFiveOtherConfigName = "TestFiveOtherConfig";
        otherConfigCounter = 0;
        List<BridgeOtherConfigs> fiveOtherConfig = (List<BridgeOtherConfigs>) Lists.newArrayList(
            (new BridgeOtherConfigsBuilder()
                .setBridgeOtherConfigKey(String.format(FORMAT_STR, testFiveOtherConfigName,
                            BRIDGE_OTHER_CONFIGS_KEY, ++otherConfigCounter))
                    .setBridgeOtherConfigValue(String.format(FORMAT_STR, testFiveOtherConfigName,
                            BRIDGE_OTHER_CONFIGS_VALUE, otherConfigCounter))
                    .build()),
            (new BridgeOtherConfigsBuilder()
                .setBridgeOtherConfigKey(String.format(FORMAT_STR, testFiveOtherConfigName,
                            BRIDGE_OTHER_CONFIGS_KEY, ++otherConfigCounter))
                    .setBridgeOtherConfigValue(String.format(FORMAT_STR, testFiveOtherConfigName,
                            BRIDGE_OTHER_CONFIGS_VALUE, otherConfigCounter))
                    .build()),
            (new BridgeOtherConfigsBuilder()
                .setBridgeOtherConfigKey(String.format(FORMAT_STR, testFiveOtherConfigName,
                            BRIDGE_OTHER_CONFIGS_KEY, ++otherConfigCounter))
                    .setBridgeOtherConfigValue(String.format(FORMAT_STR, testFiveOtherConfigName,
                            BRIDGE_OTHER_CONFIGS_VALUE, otherConfigCounter))
                    .build()),
            (new BridgeOtherConfigsBuilder()
                .setBridgeOtherConfigKey(String.format(FORMAT_STR, testFiveOtherConfigName,
                            BRIDGE_OTHER_CONFIGS_KEY, ++otherConfigCounter))
                    .setBridgeOtherConfigValue(String.format(FORMAT_STR, testFiveOtherConfigName,
                            BRIDGE_OTHER_CONFIGS_VALUE, otherConfigCounter))
                    .build()),
            (new BridgeOtherConfigsBuilder()
                .setBridgeOtherConfigKey(String.format(FORMAT_STR, testFiveOtherConfigName,
                        BRIDGE_OTHER_CONFIGS_KEY, ++otherConfigCounter))
                    .setBridgeOtherConfigValue(String.format(FORMAT_STR, testFiveOtherConfigName,
                            BRIDGE_OTHER_CONFIGS_VALUE, otherConfigCounter))
                    .build()));
        testCase = Maps.newHashMap();
        testCase.put(EXPECTED_VALUES_KEY, fiveOtherConfig);
        testCase.put(INPUT_VALUES_KEY, fiveOtherConfig);
        testMap.put(testOneOtherConfigName, testCase);

        // Test Case 3:  TestOneGoodOtherConfigOneMalformedOtherConfigValue
        // Test Type:    Negative
        // Description:
        //     One perfectly fine BridgeOtherConfig
        //        (TestOneGoodOtherConfigOneMalformedOtherConfigValue_BridgeOtherConfigKey_1,
        //        TestOneGoodOtherConfigOneMalformedOtherConfig_BridgeOtherConfigValue_1)
        //     and one malformed BridgeOtherConfig which only has key specified
        //        (TestOneGoodOtherConfigOneMalformedOtherConfigValue_NoValueForKey_2,
        //        UNSPECIFIED)
        // Expected:     A bridge is created without any other_config
        final String testOneGoodOtherConfigOneMalformedOtherConfigValueName =
                "TestOneGoodOtherConfigOneMalformedOtherConfigValue";
        otherConfigCounter = 0;
        BridgeOtherConfigs oneGood = new BridgeOtherConfigsBuilder()
            .setBridgeOtherConfigKey(String.format(FORMAT_STR, testOneGoodOtherConfigOneMalformedOtherConfigValueName,
                    GOOD_KEY, ++otherConfigCounter))
                .setBridgeOtherConfigValue(String.format("FORMAT_STR",
                        testOneGoodOtherConfigOneMalformedOtherConfigValueName,
                            GOOD_VALUE, otherConfigCounter))
                .build();
        BridgeOtherConfigs oneBad = new BridgeOtherConfigsBuilder()
            .setBridgeOtherConfigKey(String.format(FORMAT_STR,
                    testOneGoodOtherConfigOneMalformedOtherConfigValueName, NO_VALUE_FOR_KEY, ++otherConfigCounter))
                .build();
        List<BridgeOtherConfigs> oneGoodOneBadInput = (List<BridgeOtherConfigs>) Lists.newArrayList(
                oneGood, oneBad);
        List<BridgeOtherConfigs> oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);

        // Test Case 4:  TestOneGoodOtherConfigOneMalformedOtherConfigKey
        // Test Type:    Negative
        // Description:
        //     One perfectly fine BridgeOtherConfig
        //        (TestOneGoodOtherConfigOneMalformedOtherConfigValue_BridgeOtherConfigKey_1,
        //        TestOneGoodOtherConfigOneMalformedOtherConfig_BridgeOtherConfigValue_1)
        //     and one malformed BridgeOtherConfig which only has key specified
        //        (UNSPECIFIED,
        //        TestOneGoodOtherConfigOneMalformedOtherConfigKey_NoKeyForValue_2)
        // Expected:     A bridge is created without any other_config
        final String testOneGoodOtherConfigOneMalformedOtherConfigKeyName =
                "TestOneGoodOtherConfigOneMalformedOtherConfigIdKey";
        otherConfigCounter = 0;
        oneGood = new BridgeOtherConfigsBuilder()
            .setBridgeOtherConfigKey(String.format(FORMAT_STR, testOneGoodOtherConfigOneMalformedOtherConfigKeyName,
                    GOOD_KEY, ++otherConfigCounter))
                .setBridgeOtherConfigValue(String.format("FORMAT_STR",
                        testOneGoodOtherConfigOneMalformedOtherConfigKeyName,
                            GOOD_VALUE, otherConfigCounter))
                .build();
        oneBad = new BridgeOtherConfigsBuilder()
            .setBridgeOtherConfigKey(String.format(FORMAT_STR,
                    testOneGoodOtherConfigOneMalformedOtherConfigKeyName, NO_KEY_FOR_VALUE, ++otherConfigCounter))
                .build();
        oneGoodOneBadInput = (List<BridgeOtherConfigs>) Lists.newArrayList(
                oneGood, oneBad);
        oneGoodOneBadExpected = null;
        testCase = Maps.newHashMap();
        testCase.put(INPUT_VALUES_KEY, oneGoodOneBadInput);
        testCase.put(EXPECTED_VALUES_KEY, oneGoodOneBadExpected);

        return testMap;
    }

    /*
     * @see <code>SouthboundIT.generateBridgeOtherConfigsTestCases()</code> for specific test case information.
     */
    @Test
    public void testBridgeOtherConfigs() throws InterruptedException {
        final String TEST_BRIDGE_PREFIX = "BridgeOtherConfig";
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);

        Map<String,Map<String, List<BridgeOtherConfigs>>> testCases =
                generateBridgeOtherConfigsTestCases();
        List<BridgeOtherConfigs> inputBridgeOtherConfigs = null;
        List<BridgeOtherConfigs> expectedBridgeOtherConfigs = null;
        List<BridgeOtherConfigs> actualBridgeOtherConfigs = null;
        String testBridgeName = null;
        boolean bridgeAdded = false;
        for (String testCaseKey : testCases.keySet()) {
            testBridgeName = String.format("%s_%s", TEST_BRIDGE_PREFIX, testCaseKey);
            inputBridgeOtherConfigs = testCases.get(testCaseKey).get(INPUT_VALUES_KEY);
            expectedBridgeOtherConfigs = testCases.get(testCaseKey).get(EXPECTED_VALUES_KEY);
            bridgeAdded = addBridge(connectionInfo, null, testBridgeName, null, true,
                    SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null,
                    null, inputBridgeOtherConfigs);
            Assert.assertTrue(bridgeAdded);

            actualBridgeOtherConfigs = getBridge(connectionInfo, testBridgeName).getBridgeOtherConfigs();

            // Verify the expected other_config are present, or no (null) other_config are present
            if (expectedBridgeOtherConfigs != null) {
                for (BridgeOtherConfigs expectedOtherConfig : expectedBridgeOtherConfigs) {
                    Assert.assertTrue(actualBridgeOtherConfigs.contains(expectedOtherConfig));
                }
            } else {
                Assert.assertNull(actualBridgeOtherConfigs);
            }
            Assert.assertTrue(deleteBridge(connectionInfo, testBridgeName));
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    /*
     * @see <code>SouthboundIT.testCRUDBridgeExternalIds()</code>
     * This is helper test method to compare a test "set" of BridgeExternalIds against an expected "set"
     */
    private void assertExpectedBridgeExternalIdsExist( List<BridgeExternalIds> expected,
            List<BridgeExternalIds> test ) {

        if (expected != null) {
            for (BridgeExternalIds expectedExternalId : expected) {
                Assert.assertTrue(test.contains(expectedExternalId));
            }
        } else {
            Assert.assertNull(test);
        }
    }

    /*
     * @see <code>SouthboundIT.testCRUDBridgeExternalIds()</code>
     * This is a helper test method.  The method only checks if
     * <code>updateFromInputExternalIds != updateToInputExternalIds</code>,  (i.e., the updateTo "set" isn't the same
     * as the updateFrom "set".  Then, the method ensures each element of erase is not an element of test, as the input
     * test cases are divergent.
     */
    private void assertBridgeExternalIdsErased( List<BridgeExternalIds> updateFromInputExternalIds,
            List<BridgeExternalIds> updateToInputExternalIds,
            List<BridgeExternalIds> updateFromExpectedExternalIds,
            List<BridgeExternalIds> updateToTestExternalIds ) {

        if (!updateFromInputExternalIds.containsAll(updateToInputExternalIds)) {
            for (BridgeExternalIds erasedExternalId : updateFromExpectedExternalIds) {
                Assert.assertTrue(!updateToTestExternalIds.contains(erasedExternalId));
            }
        }
    }

    /*
     * @see <code>SouthboundIT.generateBridgeExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDBridgeExternalIds() throws InterruptedException {
        final String TEST_BRIDGE_PREFIX = "CRUDBridgeExternalIds";
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        Map<String, Map<String, List<BridgeExternalIds>>> updateFromTestCases = generateBridgeExternalIdsTestCases();
        Map<String, Map<String, List<BridgeExternalIds>>> updateToTestCases = generateBridgeExternalIdsTestCases();
        Map<String, List<BridgeExternalIds>> updateFromTestCase = null;
        List<BridgeExternalIds> updateFromInputExternalIds = null;
        List<BridgeExternalIds> updateFromExpectedExternalIds = null;
        List<BridgeExternalIds> updateFromConfigurationExternalIds = null;
        List<BridgeExternalIds> updateFromOperationalExternalIds = null;
        Map<String, List<BridgeExternalIds>> updateToTestCase = null;
        List<BridgeExternalIds> updateToInputExternalIds = null;
        List<BridgeExternalIds> updateToExpectedExternalIds = null;
        List<BridgeExternalIds> updateToConfigurationExternalIds = null;
        List<BridgeExternalIds> updateToOperationalExternalIds = null;
        String testBridgeName = null;
        for (String updateFromTestCaseKey : updateFromTestCases.keySet()) {
            updateFromTestCase = updateFromTestCases.get(updateFromTestCaseKey);
            updateFromInputExternalIds = updateFromTestCase.get(INPUT_VALUES_KEY);
            updateFromExpectedExternalIds = updateFromTestCase.get(EXPECTED_VALUES_KEY);
            for (String testCaseKey : updateToTestCases.keySet()) {
                testBridgeName = String.format("%s_%s", TEST_BRIDGE_PREFIX, testCaseKey);
                updateToTestCase = updateToTestCases.get(testCaseKey);
                updateToInputExternalIds = updateToTestCase.get(INPUT_VALUES_KEY);
                updateToExpectedExternalIds = updateToTestCase.get(EXPECTED_VALUES_KEY);

                // CREATE: Create the test bridge
                boolean bridgeAdded = addBridge(connectionInfo, null,
                        testBridgeName, null, true, SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"),
                        true, null, updateFromInputExternalIds, null);
                Assert.assertTrue(bridgeAdded);

                // READ: Read the test bridge and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                updateFromConfigurationExternalIds = getBridge(connectionInfo, testBridgeName,
                        LogicalDatastoreType.CONFIGURATION).getBridgeExternalIds();
                assertExpectedBridgeExternalIdsExist(updateFromExpectedExternalIds, updateFromConfigurationExternalIds);
                updateFromOperationalExternalIds = getBridge(connectionInfo, testBridgeName).getBridgeExternalIds();
                assertExpectedBridgeExternalIdsExist(updateFromExpectedExternalIds, updateFromOperationalExternalIds);

                // UPDATE:  update the external_ids
                OvsdbBridgeAugmentationBuilder bridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
                bridgeAugmentationBuilder.setBridgeExternalIds(updateToInputExternalIds);
                InstanceIdentifier<Node> bridgeIid =
                        SouthboundMapper.createInstanceIdentifier(connectionInfo,
                            new OvsdbBridgeName(testBridgeName));
                NodeBuilder bridgeNodeBuilder = new NodeBuilder();
                Node bridgeNode = getBridgeNode(connectionInfo, testBridgeName);
                bridgeNodeBuilder.setNodeId(bridgeNode.getNodeId());
                bridgeNodeBuilder.setKey(bridgeNode.getKey());
                bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, bridgeAugmentationBuilder.build());
                boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, bridgeIid, 
                        bridgeNodeBuilder.build());
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
                Assert.assertTrue(result);

                // READ: the test bridge and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                updateToConfigurationExternalIds = getBridge(connectionInfo, testBridgeName,
                        LogicalDatastoreType.CONFIGURATION).getBridgeExternalIds();
                assertExpectedBridgeExternalIdsExist(updateToExpectedExternalIds, updateToConfigurationExternalIds);
                updateToOperationalExternalIds = getBridge(connectionInfo, testBridgeName)
                        .getBridgeExternalIds();
                assertExpectedBridgeExternalIdsExist(updateToExpectedExternalIds, updateToOperationalExternalIds);

                // Make sure the old bridge external ids aren't present in the CONFIGURATION data store
                assertBridgeExternalIdsErased(updateFromInputExternalIds, updateToInputExternalIds,
                        updateFromExpectedExternalIds, updateToConfigurationExternalIds);
                assertBridgeExternalIdsErased(updateFromInputExternalIds, updateToInputExternalIds,
                        updateFromExpectedExternalIds, updateToConfigurationExternalIds);

                // DELETE
                Assert.assertTrue(deleteBridge(connectionInfo, testBridgeName));
            }
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }
}
