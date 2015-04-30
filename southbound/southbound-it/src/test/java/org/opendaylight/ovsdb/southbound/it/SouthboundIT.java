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
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
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
import org.osgi.framework.Bundle;
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
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundIT.class);
    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    private static DataBroker dataBroker = null;
    private static String addressStr;
    private static String portStr;
    private static String connectionType;
    private static String extrasStr;
    private static Boolean setup = false;
    private static MdsalUtils mdsalUtils = null;
    //private static String extras = "false";
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
        //setExtras();
        return "odl-ovsdb-southbound-impl-ui";
    }

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n"
                + "active connection : mvn -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"
                + "passive connection : mvn -Dovsdbserver.connection=passive verify\n";
    }

    @Override
    public Option[] getFeaturesOptions(boolean extras) {
        if (extras == true) {
            Option[] options = new Option[] {
                    features("mvn:org.opendaylight.ovsdb/features-ovsdb/1.1.0-SNAPSHOT/xml/features",
                            "odl-ovsdb-openstack-sb")};
            return options;
        } else {
            return new Option[]{};
        }
    }

    @Override
    public Option[] getLoggingOptions(boolean extras) {
        Option[] options = new Option[] {
                /*editConfigurationFilePut(SouthboundITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb",
                        LogLevelOption.LogLevel.DEBUG.name()),*/
                editConfigurationFilePut(SouthboundITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb.southbound-impl",
                        LogLevelOption.LogLevel.DEBUG.name())
        };

        if (extras == true) {
            Option[] extraOptions = new Option[] {
                editConfigurationFilePut(SouthboundITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb",
                        LogLevelOption.LogLevel.DEBUG.name()),
                editConfigurationFilePut(SouthboundITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb.openstack.net-virt",
                        LogLevelOption.LogLevel.DEBUG.name())
            };
            options = ObjectArrays.concat(options, extraOptions, Option.class);
        }

        options = ObjectArrays.concat(options, super.getLoggingOptions(extras), Option.class);
        return options;
    }

    @Override
    public Option[] getPropertiesOptions(boolean extras) {
        Properties props = new Properties(System.getProperties());
        String addressStr = props.getProperty(SouthboundITConstants.SERVER_IPADDRESS,
                SouthboundITConstants.DEFAULT_SERVER_IPADDRESS);
        String portStr = props.getProperty(SouthboundITConstants.SERVER_PORT,
                SouthboundITConstants.DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(SouthboundITConstants.CONNECTION_TYPE,
                SouthboundITConstants.CONNECTION_TYPE_ACTIVE);
        String extrasStr = props.getProperty(SouthboundITConstants.SERVER_EXTRAS,
                SouthboundITConstants.DEFAULT_SERVER_EXTRAS);

        LOG.info("getPropertiesOptions: Using the following properties: mode= {}, ip:port= {}:{}, extras= {}",
                connectionType, addressStr, portStr, extrasStr);

        Option[] options = new Option[] {
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.SERVER_IPADDRESS, addressStr),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.SERVER_PORT, portStr),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.CONNECTION_TYPE, connectionType),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.SERVER_EXTRAS, extrasStr)
        };
        return options;
    }


    public boolean setExtras() {
        Properties props = new Properties(System.getProperties());
        boolean extras = props.getProperty(SouthboundITConstants.SERVER_EXTRAS,
                SouthboundITConstants.DEFAULT_SERVER_EXTRAS).equals("true");
        LOG.info("setExtras: {}", extras);
        return extras;
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
        extrasStr = bundleContext.getProperty(SouthboundITConstants.SERVER_EXTRAS);

        LOG.info("setUp: Using the following properties: mode= {}, ip:port= {}:{}, extras= {}",
                connectionType, addressStr, portStr, extrasStr);
        if (connectionType.equalsIgnoreCase(SouthboundITConstants.CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }

        mdsalUtils = new MdsalUtils(dataBroker);
        setup = true;

        if (extrasStr.equals("true")) {
            isBundleReady(bundleContext, NETVIRT);
            isBundleReady(bundleContext, NETVIRTPROVIDERS);
        }
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

    private ConnectionInfo getConnectionInfo(String addressStr, String portStr) {
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

    private String connectionInfoToString(ConnectionInfo connectionInfo) {
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

    private boolean addOvsdbNode(ConnectionInfo connectionInfo) throws InterruptedException {
        boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION,
                SouthboundMapper.createInstanceIdentifier(connectionInfo),
                SouthboundMapper.createNode(connectionInfo));
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    private Node getOvsdbNode(ConnectionInfo connectionInfo) {
        Node node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                SouthboundMapper.createInstanceIdentifier(connectionInfo));
        return node;
    }

    private boolean deleteOvsdbNode(ConnectionInfo connectionInfo) throws InterruptedException {
        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
                SouthboundMapper.createInstanceIdentifier(connectionInfo));
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    private Node connectOvsdbNode(ConnectionInfo connectionInfo) throws InterruptedException {
        Assert.assertTrue(addOvsdbNode(connectionInfo));
        Node node = getOvsdbNode(connectionInfo);
        Assert.assertNotNull(node);
        LOG.info("Connected to {}", connectionInfoToString(connectionInfo));
        return node;
    }

    private boolean disconnectOvsdbNode(ConnectionInfo connectionInfo) throws InterruptedException {
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
        List<DatapathTypeEntry> nodeAugment = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class)
                .getDatapathTypeEntry();
        if (nodeAugment == null) {
            LOG.info("DPDK not supported on this node.");
        } else {

            // Verify if DPDK is supported.
            ListIterator<DatapathTypeEntry> literator = nodeAugment.listIterator();
            while (literator.hasNext()) {
                Class<? extends DatapathTypeBase> dpType = literator.next().getDatapathType();
                String dpName = SouthboundConstants.DATAPATH_TYPE_MAP.get(dpType);
                LOG.info("dp type is {}", dpName);
                if (dpName.equals("netdev")) {

                    // Since this is a DPDK node, lets add a netdev device.
                    LOG.info("It is a DPDK node");
                    NodeBuilder bridgeNodeBuilder = new NodeBuilder();
                    InstanceIdentifier<Node> bridgeIid = SouthboundMapper.createInstanceIdentifier(
                            connectionInfo, new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME));
                    NodeId bridgeNodeId = SouthboundMapper.createManagedNodeId(bridgeIid);
                    bridgeNodeBuilder.setNodeId(bridgeNodeId);
                    OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new 
                            OvsdbBridgeAugmentationBuilder();
                    ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(
                            SouthboundITConstants.BRIDGE_NAME));
                    ovsdbBridgeAugmentationBuilder.setDatapathType(dpType);
                    setManagedBy(ovsdbBridgeAugmentationBuilder, connectionInfo);
                    bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class,
                            ovsdbBridgeAugmentationBuilder.build());
                    mdsalUtils
                            .merge(LogicalDatastoreType.CONFIGURATION, bridgeIid, bridgeNodeBuilder.build());
                    Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                    // Verify that the device is netdev
                    OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
                    LOG.info("new datapath is {}", bridge.getDatapathType());
                    LOG.info("dpType  is {}", dpType);
                    Assert.assertTrue(bridge.getDatapathType().equals(dpType));

                    // Add dpdk port.
                    OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder = 
                            createGenericOvsdbTerminationPointAugmentationBuilder();
                    String portName = "testDPDKPort";
                    ovsdbTerminationBuilder.setName(portName);
                    Class<? extends InterfaceTypeBase> ifType = SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP
                            .get("dpdk");
                    ovsdbTerminationBuilder.setInterfaceType(ifType);
                    LOG.info("ifType is {}", ifType);
                    Assert.assertTrue(addTerminationPoint(bridgeNodeId, portName, ovsdbTerminationBuilder));

                    // Verify that DPDK port was created.
                    InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
                    Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                            terminationPointIid);
                    Assert.assertNotNull(terminationPointNode);
                    List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
                    for (TerminationPoint terminationPoint : terminationPoints) {
                        OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation = terminationPoint
                                .getAugmentation(OvsdbTerminationPointAugmentation.class);
                        if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                            Class<? extends InterfaceTypeBase> opPort = ovsdbTerminationPointAugmentation
                                    .getInterfaceType();
                            LOG.info("ifType from oper : {}", opPort);
                            Assert.assertTrue(ifType.equals(opPort));
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

    private void setManagedBy(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                              ConnectionInfo connectionInfo) {
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

    private boolean addTerminationPoint(NodeId bridgeNodeId, String portName,
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointAugmentationBuilder)
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

    private boolean addBridge(ConnectionInfo connectionInfo, String bridgeName) throws InterruptedException {
        NodeBuilder bridgeNodeBuilder = new NodeBuilder();
        InstanceIdentifier<Node> bridgeIid =
                SouthboundMapper.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
        NodeId bridgeNodeId = SouthboundMapper.createManagedNodeId(bridgeIid);
        bridgeNodeBuilder.setNodeId(bridgeNodeId);
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
        ovsdbBridgeAugmentationBuilder.setProtocolEntry(createMdsalProtocols());
        ovsdbBridgeAugmentationBuilder.setFailMode(
                SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"));
        setManagedBy(ovsdbBridgeAugmentationBuilder, connectionInfo);
        bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());

        LOG.debug("Built with the intent to store bridge data {}",
                ovsdbBridgeAugmentationBuilder.toString());

        boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                bridgeIid, bridgeNodeBuilder.build());
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    private OvsdbBridgeAugmentation getBridge(ConnectionInfo connectionInfo) {
        InstanceIdentifier<Node> bridgeIid =
                SouthboundMapper.createInstanceIdentifier(connectionInfo,
                        new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME));
        Node bridgeNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeIid);
        Assert.assertNotNull(bridgeNode);
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
        Assert.assertNotNull(ovsdbBridgeAugmentation);
        return ovsdbBridgeAugmentation;
    }

    private boolean deleteBridge(ConnectionInfo connectionInfo) throws InterruptedException {
        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
                SouthboundMapper.createInstanceIdentifier(connectionInfo,
                        new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME)));
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

    @Test
    public void testTerminationPointOfPort() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);

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
        Long ofPortExpected = new Long(45002);
        ovsdbTerminationBuilder.setOfport(ofPortExpected);
        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);

        List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPoints) {
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                Long ofPort = ovsdbTerminationPointAugmentation.getOfport();
                // if ephemeral port 45002 is in use, ofPort is set to 1
                Assert.assertTrue(ofPort.equals(ofPortExpected) || ofPort.equals(new Long(1)));
                LOG.info("ofPort: {}", ofPort);
            }
        }
        Assert.assertTrue(deleteBridge(connectionInfo));
    }

    @Test
    public void testTerminationPointOfPortRequest() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                connectionInfo, bridge.getBridgeName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        String portName = "testOfPortRequest";
        ovsdbTerminationBuilder.setName(portName);
        Long ofPortExpected = new Long(45008);
        Integer ofPortRequestExpected = ofPortExpected.intValue();
        Long ofPortInput = new Long(45008);
        ovsdbTerminationBuilder.setOfport(ofPortInput);
        ovsdbTerminationBuilder.setOfportRequest(ofPortRequestExpected);
        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);

        List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPoints) {
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                Long ofPort = ovsdbTerminationPointAugmentation.getOfport();
                // if ephemeral port 45002 is in use, ofPort is set to 1
                Assert.assertTrue(ofPort.equals(ofPortExpected) || ofPort.equals(new Long(1)));
                LOG.info("ofPort: {}", ofPort);

                Integer ofPortRequest = ovsdbTerminationPointAugmentation.getOfportRequest();
                Assert.assertTrue(ofPortRequest.equals(ofPortRequestExpected));
                LOG.info("ofPortRequest: {}", ofPortRequest);
            }
        }
        Assert.assertTrue(deleteBridge(connectionInfo));
    }

    @Test
    public void testTerminationPointPortExternalIds() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                connectionInfo, bridge.getBridgeName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        String portName = "testPortExternalIds";
        ovsdbTerminationBuilder.setName(portName);
        //setup
        PortExternalIdsBuilder externalIdsBuilder1 = new PortExternalIdsBuilder();
        externalIdsBuilder1.setExternalIdKey("portExternalIdKey1");
        externalIdsBuilder1.setExternalIdValue("portExternalIdValue1");
        PortExternalIdsBuilder externalIdsBuilder2 = new PortExternalIdsBuilder();
        externalIdsBuilder2.setExternalIdKey("portExternalIdKey2");
        externalIdsBuilder2.setExternalIdValue("portExternalIdValue2");
        List<PortExternalIds> portExternalIds = Lists.newArrayList(externalIdsBuilder1.build(),
                externalIdsBuilder2.build());
        ovsdbTerminationBuilder.setPortExternalIds(portExternalIds);

        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);

        List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPoints) {
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                List<PortExternalIds> actualPortExternalIds = ovsdbTerminationPointAugmentation.getPortExternalIds();
                Assert.assertTrue((portExternalIds.size() == actualPortExternalIds.size()));
                for (PortExternalIds portExternalId : portExternalIds) {
                    Assert.assertTrue(actualPortExternalIds.contains(portExternalId));
                }
            }
        }
        Assert.assertTrue(deleteBridge(connectionInfo));
    }

    @Test
    public void testTerminationPointInterfaceExternalIds() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                connectionInfo, bridge.getBridgeName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        String portName = "testInterfaceExternalIds";
        ovsdbTerminationBuilder.setName(portName);
        //setup
        InterfaceExternalIdsBuilder externalIdsBuilder1 = new InterfaceExternalIdsBuilder();
        externalIdsBuilder1.setExternalIdKey("interfaceExternalIdKey1");
        externalIdsBuilder1.setExternalIdValue("interfaceExternalIdValue1");
        InterfaceExternalIdsBuilder externalIdsBuilder2 = new InterfaceExternalIdsBuilder();
        externalIdsBuilder2.setExternalIdKey("interfaceExternalIdKey2");
        externalIdsBuilder2.setExternalIdValue("interfaceExternalIdValue2");
        List<InterfaceExternalIds> interfaceExternalIds = Lists.newArrayList(externalIdsBuilder1.build(),
                externalIdsBuilder2.build());
        ovsdbTerminationBuilder.setInterfaceExternalIds(interfaceExternalIds);

        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);

        List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPoints) {
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                List<InterfaceExternalIds> actualInterfaceExternalIds = ovsdbTerminationPointAugmentation.
                        getInterfaceExternalIds();
                Assert.assertTrue((interfaceExternalIds.size() == actualInterfaceExternalIds.size()));
                for (InterfaceExternalIds interfaceExternalId : interfaceExternalIds) {
                    Assert.assertTrue(actualInterfaceExternalIds.contains(interfaceExternalId));
                }
            }
        }
        Assert.assertTrue(deleteBridge(connectionInfo));
    }

    @Test
    public void testTerminationPointOptions() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                connectionInfo, bridge.getBridgeName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        String portName = "testInterfaceOptions";
        ovsdbTerminationBuilder.setName(portName);
        //setup
        OptionsBuilder optionsBuilder1 = new OptionsBuilder();
        optionsBuilder1.setOption("option1");
        optionsBuilder1.setValue("optionValue1");
        OptionsBuilder optionsBuilder2 = new OptionsBuilder();
        optionsBuilder2.setOption("option2");
        optionsBuilder2.setValue("optionValue2");
        List<Options> options = Lists.newArrayList(optionsBuilder1.build(),
                optionsBuilder2.build());
        ovsdbTerminationBuilder.setOptions(options);

        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);

        List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPoints) {
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                List<Options> actualOptions = ovsdbTerminationPointAugmentation.
                        getOptions();
                Assert.assertTrue((options.size() == actualOptions.size()));
                for (Options option : options) {
                    Assert.assertTrue(actualOptions.contains(option));
                }
            }
        }
        Assert.assertTrue(deleteBridge(connectionInfo));
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
    }

    @Test
    public void testTerminationPointPortOtherConfigs() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                connectionInfo, bridge.getBridgeName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        String portName = "testPortOtherConfigs";
        ovsdbTerminationBuilder.setName(portName);
        //setup
        PortOtherConfigsBuilder portBuilder1 = new PortOtherConfigsBuilder();
        portBuilder1.setOtherConfigKey("portOtherConfigsKey1");
        portBuilder1.setOtherConfigValue("portOtherConfigsValue1");
        PortOtherConfigsBuilder portBuilder2 = new PortOtherConfigsBuilder();
        portBuilder2.setOtherConfigKey("portOtherConfigsKey2");
        portBuilder2.setOtherConfigValue("portOtherConfigsValue2");
        List<PortOtherConfigs> portOtherConfigs = Lists.newArrayList(portBuilder1.build(),
                portBuilder2.build());
        ovsdbTerminationBuilder.setPortOtherConfigs(portOtherConfigs);

        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);

        List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPoints) {
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                List<PortOtherConfigs> actualPortOtherConfigs = ovsdbTerminationPointAugmentation.
                        getPortOtherConfigs();
                Assert.assertTrue((portOtherConfigs.size() == actualPortOtherConfigs.size()));
                for (PortOtherConfigs portOtherConfig : portOtherConfigs) {
                    Assert.assertTrue(actualPortOtherConfigs.contains(portOtherConfig));
                }
            }
        }
        Assert.assertTrue(deleteBridge(connectionInfo));
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
    }

    @Test
    public void testTerminationPointVlanModes() throws InterruptedException {
        VlanMode []vlanModes = VlanMode.values();
        for (VlanMode vlanMode : vlanModes) {
            ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
            connectOvsdbNode(connectionInfo);
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
        ArrayList<Set<Integer>> vlanSets = generateVlanSets();
        int testCase = 0;
        for (Set<Integer> vlanSet : vlanSets) {
            ++testCase;
            ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
            connectOvsdbNode(connectionInfo);
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

    /**
     * isBundleReady is used to check if the requested bundle is Active
     */
    public void isBundleReady(BundleContext bundleContext, String bundleName) throws InterruptedException {
        boolean ready = false;

        while (!ready) {
            int state = Bundle.UNINSTALLED;
            Bundle[] bundles = bundleContext.getBundles();
            for (Bundle element : bundles) {
                if (element.getSymbolicName().equals(bundleName)) {
                    state = element.getState();
                    LOG.info(">>>>> bundle is ready {}", bundleName);
                    break;
                }
            }
            if (state != Bundle.ACTIVE) {
                LOG.info(">>>>> bundle not ready {}", bundleName);
                Thread.sleep(5000);
            } else {
                ready = true;
            }
        }
    }
}
