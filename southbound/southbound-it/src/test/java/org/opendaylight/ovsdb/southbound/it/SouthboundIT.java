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
    private static final String EXPECTED_VALUES_KEY = "ExpectedValuesKey";
    private static final String INPUT_VALUES_KEY = "InputValuesKey";
    private static final String NETDEV_DP_TYPE = "netdev";
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
    public Option[] getFeaturesOptions(final boolean extras) {
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
    public Option[] getLoggingOptions(final boolean extras) {
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
    public Option[] getPropertiesOptions(final boolean extras) {
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
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
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
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
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
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
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
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
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
     * @see <code>SouthboundIT.generateBridgeExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testBridgeExternalIds() throws InterruptedException {
        final String TEST_BRIDGE_PREFIX = "BridgeExtIds";
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);

        Map<String,Map<String, List<BridgeExternalIds>>> testCases =
                generateBridgeExternalIdsTestCases();
        List<BridgeExternalIds> inputBridgeExternalIds = null;
        List<BridgeExternalIds> expectedBridgeExternalIds = null;
        List<BridgeExternalIds> actualBridgeExternalIds = null;
        String testBridgeName = null;
        boolean bridgeAdded = false;
        for (String testCaseKey : testCases.keySet()) {
            testBridgeName = String.format("%s_%s", TEST_BRIDGE_PREFIX, testCaseKey);
            inputBridgeExternalIds = testCases.get(testCaseKey).get(INPUT_VALUES_KEY);
            expectedBridgeExternalIds = testCases.get(testCaseKey).get(EXPECTED_VALUES_KEY);
            bridgeAdded = addBridge(connectionInfo, null, testBridgeName, null, true,
                    SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null,
                    inputBridgeExternalIds, null);
            Assert.assertTrue(bridgeAdded);

            actualBridgeExternalIds = getBridge(connectionInfo, testBridgeName).getBridgeExternalIds();

            // Verify the expected external_ids are present, or no (null) external_ids are present
            if (expectedBridgeExternalIds != null) {
                for (BridgeExternalIds expectedExternalId : expectedBridgeExternalIds) {
                    Assert.assertTrue(actualBridgeExternalIds.contains(expectedExternalId));
                }
            } else {
                Assert.assertNull(actualBridgeExternalIds);
            }
            Assert.assertTrue(deleteBridge(connectionInfo, testBridgeName));
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
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

    @Test
    public void testUpdateExistingBridgeExternalIds() throws InterruptedException {
        final String TEST_BRIDGE_PREFIX = "UpdateBridgeExternalIds";
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        Map<String, Map<String, List<BridgeExternalIds>>> testCases = generateBridgeExternalIdsTestCases();
        String testBridgeName = null;
        for (String testCaseKey : testCases.keySet()) {

            // Create the test bridge
            testBridgeName = String.format("%s_%s", TEST_BRIDGE_PREFIX, testCaseKey);
            Map<String, List<BridgeExternalIds>> testCase = testCases.get(testCaseKey);
            List<BridgeExternalIds> inputBridgeExternalIds = testCase.get(INPUT_VALUES_KEY);
            List<BridgeExternalIds> expectedBridgeExternalIds = testCase.get(EXPECTED_VALUES_KEY);
            boolean bridgeAdded = addBridge(connectionInfo, testBridgeName);
            Assert.assertTrue(bridgeAdded);

            // Modify the test bridge with the appropriate inputExternalIds in the config datastore
            OvsdbBridgeAugmentation bridgeAugmentation = getBridge(connectionInfo, testBridgeName);
            Assert.assertNotNull(bridgeAugmentation);
            OvsdbBridgeAugmentationBuilder bridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            bridgeAugmentationBuilder.setBridgeExternalIds(inputBridgeExternalIds);
            InstanceIdentifier<Node> bridgeIid =
                    SouthboundMapper.createInstanceIdentifier(connectionInfo,
                        new OvsdbBridgeName(testBridgeName));
            NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            Node bridgeNode = getBridgeNode(connectionInfo, testBridgeName);
            bridgeNodeBuilder.setNodeId(bridgeNode.getNodeId());
            bridgeNodeBuilder.setKey(bridgeNode.getKey());
            bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, bridgeAugmentationBuilder.build());
            boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, bridgeIid, bridgeNodeBuilder.build());
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            Assert.assertTrue(result);

            // Test that the changes are propagated to the CONFIGURATION data store
            List<BridgeExternalIds> configuredBridgeExternalIds = getBridge(connectionInfo, testBridgeName,
                    LogicalDatastoreType.CONFIGURATION).getBridgeExternalIds();
            // Verify the expected external_ids are present in the CONFIGURATION data store,
            // or no (null) external_ids are present
            if (expectedBridgeExternalIds != null) {
                for (BridgeExternalIds expectedExternalId : expectedBridgeExternalIds) {
                    Assert.assertTrue(configuredBridgeExternalIds.contains(expectedExternalId));
                }
            } else {
                Assert.assertNull(configuredBridgeExternalIds);
            }

            // Test that the changes are propagated to the OPERATIONAL data store
            List<BridgeExternalIds> actualBridgeExternalIds = getBridge(connectionInfo, testBridgeName)
                    .getBridgeExternalIds();
            // Verify the expected external_ids are present in the OPERATIONAL data store,
            // or no (null) external_ids are present
            if (expectedBridgeExternalIds != null) {
                for (BridgeExternalIds expectedExternalId : expectedBridgeExternalIds) {
                    Assert.assertTrue(actualBridgeExternalIds.contains(expectedExternalId));
                }
            } else {
                Assert.assertNull(actualBridgeExternalIds);
            }

            Assert.assertTrue(deleteBridge(connectionInfo, testBridgeName));
        }
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
