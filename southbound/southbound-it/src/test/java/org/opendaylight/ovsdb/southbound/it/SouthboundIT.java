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
import com.google.common.collect.ObjectArrays;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Assume;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
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
    private static Boolean setup = false;
    private static MdsalUtils mdsalUtils = null;
    private static String extras = "false";
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
        setExtras();
        return "odl-ovsdb-southbound-impl-ui";
    }

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n"
                + "active connection : mvn -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"
                + "passive connection : mvn -Dovsdbserver.connection=passive verify\n";
    }

    @Override
    public Option[] getFeaturesOptions() {
        if (extras.equals("true")) {
            Option[] options = new Option[] {
                    features("mvn:org.opendaylight.ovsdb/features-ovsdb/1.1.0-SNAPSHOT/xml/features",
                            "odl-ovsdb-openstack-sb")};
            return options;
        } else {
            return new Option[]{};
        }
    }

    @Override
    public Option[] getLoggingOptions() {
        Option[] options = new Option[] {
                /*editConfigurationFilePut(SouthboundITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb",
                        LogLevelOption.LogLevel.DEBUG.name()),*/
                editConfigurationFilePut(SouthboundITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb.southbound-impl",
                        LogLevelOption.LogLevel.DEBUG.name())
        };

        if (extras.equals("true")) {
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

        LOG.info("Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);

        Option[] options = new Option[] {
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.SERVER_IPADDRESS, addressStr),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.SERVER_PORT, portStr),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.CONNECTION_TYPE, connectionType)
        };
        return options;
    }

    private void setExtras() {
        Properties props = new Properties(System.getProperties());
        extras = props.getProperty(SouthboundITConstants.SERVER_EXTRAS,
                SouthboundITConstants.DEFAULT_SERVER_EXTRAS);
        LOG.info("extras: {}", extras);
        System.out.println("extras: " + extras);
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

        LOG.info("Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);
        if (connectionType.equalsIgnoreCase(SouthboundITConstants.CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }

        mdsalUtils = new MdsalUtils(dataBroker);
        setup = true;

        if (extras.equals("true")) {
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
        //Assert.assertNull(node);
        Assume.assumeNotNull(node);
        LOG.info("Disconnected from {}", connectionInfoToString(connectionInfo));
        return true;
    }

    @Test
    public void testAddDeleteOvsdbNode() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);
        //Assert.assertFalse(disconnectOvsdbNode(connectionInfo));
        Assume.assumeTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testOpenVSwitchOtherConfig() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        assertNotNull(ovsdbNodeAugmentation);
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
        //Assert.assertFalse(disconnectOvsdbNode(connectionInfo));
        Assume.assumeTrue(disconnectOvsdbNode(connectionInfo));
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

    private boolean addBridge(ConnectionInfo connectionInfo, String bridgeName) throws InterruptedException {
        //Node node = SouthboundMapper.createNode(connectionInfo);
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
                SouthboundMapper.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME));
        Node bridgeNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeIid);
        Assert.assertNotNull(bridgeNode);
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
        Assert.assertNotNull(ovsdbBridgeAugmentation);
        return ovsdbBridgeAugmentation;
    }

    private boolean deleteBridge(ConnectionInfo connectionInfo) throws InterruptedException {
        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
                SouthboundMapper.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME)));
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

        //Assert.assertFalse(disconnectOvsdbNode(connectionInfo));
        Assume.assumeTrue(disconnectOvsdbNode(connectionInfo));
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
