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
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbFailModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
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
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
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
    private static final String NETDEV_DP_TYPE = "netdev";
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundIT.class);
    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    private static final String FORMAT_STR = "%s_%s_%d";
    public static final int NUM_THREADS = 1;
    private static String addressStr;
    private static int portNumber;
    private static String connectionType;
    private static Boolean setup = false;
    private static MdsalUtils mdsalUtils = null;

    // TODO Constants copied from AbstractConfigTestBase, need to be removed (see TODO below)
    private static final String PAX_EXAM_UNPACK_DIRECTORY = "target/exam";
    private static final String KARAF_DEBUG_PORT = "5005";
    private static final String KARAF_DEBUG_PROP = "karaf.debug";
    private static final String KEEP_UNPACK_DIRECTORY_PROP = "karaf.keep.unpack";

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        // TODO Figure out how to use the parent Karaf setup, then just use super.config()
        Option[] options = new Option[] {
                when(Boolean.getBoolean(KARAF_DEBUG_PROP))
                        .useOptions(KarafDistributionOption.debugConfiguration(KARAF_DEBUG_PORT, true)),
                karafDistributionConfiguration().frameworkUrl(getKarafDistro())
                        .unpackDirectory(new File(PAX_EXAM_UNPACK_DIRECTORY))
                        .useDeployFolder(false),
                when(Boolean.getBoolean(KEEP_UNPACK_DIRECTORY_PROP)).useOptions(keepRuntimeFolder()),
                // Works only if we don't specify the feature repo and name
                getLoggingOption()};
        Option[] propertyOptions = getPropertiesOptions();
        Option[] otherOptions = getOtherOptions();
        Option[] combinedOptions = new Option[options.length + propertyOptions.length + otherOptions.length];
        System.arraycopy(options, 0, combinedOptions, 0, options.length);
        System.arraycopy(propertyOptions, 0, combinedOptions, options.length, propertyOptions.length);
        System.arraycopy(otherOptions, 0, combinedOptions, options.length + propertyOptions.length,
                otherOptions.length);
        return combinedOptions;
    }

    private Option[] getOtherOptions() {
        return new Option[] {
                vmOption("-javaagent:../jars/org.jacoco.agent.jar=destfile=../../jacoco-it.exec"),
                keepRuntimeFolder()
        };
    }

    @Override
    public String getKarafDistro() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("southbound-karaf")
                .versionAsInProject()
                .type("zip")
                .getURL();
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
    public Option getLoggingOption() {
        return composite(
                editConfigurationFilePut(SouthboundITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb",
                        LogLevelOption.LogLevel.TRACE.name()),
                super.getLoggingOption());
    }

    private Option[] getPropertiesOptions() {
        Properties props = new Properties(System.getProperties());
        String addressStr = props.getProperty(SouthboundITConstants.SERVER_IPADDRESS,
                SouthboundITConstants.DEFAULT_SERVER_IPADDRESS);
        String portStr = props.getProperty(SouthboundITConstants.SERVER_PORT,
                SouthboundITConstants.DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(SouthboundITConstants.CONNECTION_TYPE,
                SouthboundITConstants.CONNECTION_TYPE_ACTIVE);

        LOG.info("getPropertiesOptions: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);

        return new Option[] {
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.SERVER_IPADDRESS, addressStr),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.SERVER_PORT, portStr),
                editConfigurationFilePut(SouthboundITConstants.CUSTOM_PROPERTIES,
                        SouthboundITConstants.CONNECTION_TYPE, connectionType),
        };
    }

    @Before
    @Override
    public void setup() throws InterruptedException {
        if (setup) {
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
        DataBroker dataBroker = SouthboundProvider.getDb();
        Assert.assertNotNull("db should not be null", dataBroker);

        addressStr = bundleContext.getProperty(SouthboundITConstants.SERVER_IPADDRESS);
        String portStr = bundleContext.getProperty(SouthboundITConstants.SERVER_PORT);
        try {
            portNumber = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            fail("Invalid port number " + portStr + System.lineSeparator() + usage());
        }
        connectionType = bundleContext.getProperty(SouthboundITConstants.CONNECTION_TYPE);

        LOG.info("setUp: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portNumber);
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
     * 6640. This test will wait for incoming connections for {@link SouthboundITConstants#CONNECTION_INIT_TIMEOUT} ms.
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

    private ConnectionInfo getConnectionInfo(final String addressStr, final int portNumber) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(addressStr);
        } catch (UnknownHostException e) {
            fail("Could not resolve " + addressStr + ": " + e);
        }

        IpAddress address = SouthboundMapper.createIpAddress(inetAddress);
        PortNumber port = new PortNumber(portNumber);

        final ConnectionInfo connectionInfo = new ConnectionInfoBuilder()
                .setRemoteIp(address)
                .setRemotePort(port)
                .build();
        LOG.info("connectionInfo: {}", connectionInfo);
        return connectionInfo;
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
        InstanceIdentifier<Node> iid = createInstanceIdentifier(connectionInfo);
        // Check that the node doesn't already exist (we don't support connecting twice)
        Assert.assertNull("The OVSDB node has already been added",
                mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, iid));
        boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION,
                iid,
                createNode(connectionInfo));
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    private InstanceIdentifier<Node> createInstanceIdentifier(
            ConnectionInfo connectionInfo) {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,
                        createNodeKey(connectionInfo.getRemoteIp(), connectionInfo.getRemotePort()));
    }

    private Node getOvsdbNode(final ConnectionInfo connectionInfo) {
        return mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                createInstanceIdentifier(connectionInfo));
    }

    private boolean deleteOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
                createInstanceIdentifier(connectionInfo));
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
        LOG.info("Disconnected from {}", connectionInfoToString(connectionInfo));
        return true;
    }

    @Test
    public void testAddDeleteOvsdbNode() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testDpdkSwitch() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);
        List<DatapathTypeEntry> datapathTypeEntries = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class)
                .getDatapathTypeEntry();
        if (datapathTypeEntries == null) {
            LOG.info("DPDK not supported on this node.");
        } else {
            for (DatapathTypeEntry dpTypeEntry : datapathTypeEntries) {
                Class<? extends DatapathTypeBase> dpType = dpTypeEntry.getDatapathType();
                String dpTypeStr = SouthboundConstants.DATAPATH_TYPE_MAP.get(dpType);
                LOG.info("dp type is {}", dpTypeStr);
                if (dpTypeStr.equals(NETDEV_DP_TYPE)) {
                    LOG.info("Found a DPDK node; adding a corresponding netdev device");
                    InstanceIdentifier<Node> bridgeIid = createInstanceIdentifier(connectionInfo,
                            new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME));
                    NodeId bridgeNodeId = createManagedNodeId(bridgeIid);
                    addBridge(connectionInfo, bridgeIid, SouthboundITConstants.BRIDGE_NAME, bridgeNodeId, false, null,
                            true, dpType, null, null, null);

                    // Verify that the device is netdev
                    OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
                    Assert.assertNotNull(bridge);
                    Assert.assertEquals(dpType, bridge.getDatapathType());

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
                    List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
                    for (TerminationPoint terminationPoint : terminationPoints) {
                        OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation = terminationPoint
                                .getAugmentation(OvsdbTerminationPointAugmentation.class);
                        if (ovsdbTerminationPointAugmentation.getName().equals(TEST_PORT_NAME)) {
                            Class<? extends InterfaceTypeBase> opPort = ovsdbTerminationPointAugmentation
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
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        Assert.assertNotNull(ovsdbNodeAugmentation);
        assertNotNull(ovsdbNodeAugmentation.getOvsVersion());
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testOpenVSwitchOtherConfig() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
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
    }

    @Test
    public void testOvsdbBridgeControllerInfo() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);
        String controllerTarget = SouthboundUtil.getControllerTarget(ovsdbNode);
        assertNotNull("Failed to get controller target", controllerTarget);
        List<ControllerEntry> setControllerEntry = createControllerEntry(controllerTarget);
        Uri setUri = new Uri(controllerTarget);
        Assert.assertTrue(addBridge(connectionInfo, null, SouthboundITConstants.BRIDGE_NAME,null, true,
                SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null,
                setControllerEntry, null));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull("bridge was not found: " + SouthboundITConstants.BRIDGE_NAME,  bridge);
        Assert.assertNotNull("ControllerEntry was not found: " + setControllerEntry.iterator().next(),
                bridge.getControllerEntry());
        List<ControllerEntry> getControllerEntries = bridge.getControllerEntry();
        for (ControllerEntry entry : getControllerEntries) {
            if (entry.getTarget() != null) {
                Assert.assertEquals(setUri.toString(), entry.getTarget().toString());
            }
        }

        Assert.assertTrue(deleteBridge(connectionInfo));
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    private List<ControllerEntry> createControllerEntry(String controllerTarget) {
        List<ControllerEntry> controllerEntriesList = new ArrayList<>();
        controllerEntriesList.add(new ControllerEntryBuilder()
                .setTarget(new Uri(controllerTarget))
                .build());
        return controllerEntriesList;
    }

    private void setManagedBy(final OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                              final ConnectionInfo connectionInfo) {
        InstanceIdentifier<Node> connectionNodePath = createInstanceIdentifier(connectionInfo);
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath));
    }

    private List<ProtocolEntry> createMdsalProtocols() {
        List<ProtocolEntry> protocolList = new ArrayList<>();
        ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
        protocolList.add(new ProtocolEntryBuilder().setProtocol(mapper.get("OpenFlow13")).build());
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
                                        final OvsdbTerminationPointAugmentationBuilder
                                                ovsdbTerminationPointAugmentationBuilder)
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
                              final List<ControllerEntry> controllerEntries,
                              final List<BridgeOtherConfigs> otherConfigs) throws InterruptedException {

        NodeBuilder bridgeNodeBuilder = new NodeBuilder();
        if (bridgeIid == null) {
            bridgeIid = createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
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
        if (controllerEntries != null) {
            ovsdbBridgeAugmentationBuilder.setControllerEntry(controllerEntries);
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
                SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null, null, null);
    }

    private OvsdbBridgeAugmentation getBridge(ConnectionInfo connectionInfo) {
        return getBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME);
    }

    /**
     * Extract the <code>store</code> type data store contents for the particular bridge identified by
     * <code>bridgeName</code>.
     *
     * @param connectionInfo the connection information
     * @param bridgeName the bridge name
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
     * @param connectionInfo the connection information
     * @param bridgeName the bridge name
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
     * @param connectionInfo the connection information
     * @param bridgeName the bridge name
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @return <code>store</code> type data store contents
     */
    private Node getBridgeNode(ConnectionInfo connectionInfo, String bridgeName, LogicalDatastoreType store) {
        InstanceIdentifier<Node> bridgeIid =
                createInstanceIdentifier(connectionInfo,
                        new OvsdbBridgeName(bridgeName));
        return mdsalUtils.read(store, bridgeIid);
    }

    /**
     * Extract the node contents from <code>LogicalDataStoreType.OPERATIONAL</code> data store for the
     * bridge identified by <code>bridgeName</code>
     *
     * @param connectionInfo the connection information
     * @param bridgeName the bridge name
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
                createInstanceIdentifier(connectionInfo,
                        new OvsdbBridgeName(bridgeName)));
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    @Test
    public void testAddDeleteBridge() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);

        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        LOG.info("bridge: {}", bridge);

        Assert.assertTrue(deleteBridge(connectionInfo));

        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    private InstanceIdentifier<Node> getTpIid(ConnectionInfo connectionInfo, OvsdbBridgeAugmentation bridge) {
        return createInstanceIdentifier(connectionInfo,
                bridge.getBridgeName());
    }

    /**
     * Extracts the <code>TerminationPointAugmentation</code> for the <code>index</code> <code>TerminationPoint</code>
     * on <code>bridgeName</code>
     *
     * @param connectionInfo the connection information
     * @param bridgeName the bridge name
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @param index the index we're interested in
     * @return the augmentation (or {@code null} if none)
     */
    private OvsdbTerminationPointAugmentation getOvsdbTerminationPointAugmentation(
            ConnectionInfo connectionInfo, String bridgeName, LogicalDatastoreType store, int index ) {

        List<TerminationPoint> tpList = getBridgeNode(connectionInfo, bridgeName, store).getTerminationPoint();
        if (tpList == null) {
            return null;
        }
        return tpList.get(index).getAugmentation(OvsdbTerminationPointAugmentation.class);
    }

    @Test
    public void testCRDTerminationPointOfPort() throws InterruptedException {
        final Long OFPORT_EXPECTED = 45002L;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);

        // CREATE
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        LOG.info("bridge: {}", bridge);
        NodeId nodeId = SouthboundMapper.createManagedNodeId(createInstanceIdentifier(
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
        final Long OFPORT_EXPECTED = 45008L;
        final Long OFPORT_INPUT = 45008L;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);

        // CREATE
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
        Assert.assertNotNull(bridge);
        NodeId nodeId = createManagedNodeId(createInstanceIdentifier(
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

    private interface KeyValueBuilder<T> {
        T build(String testName, String key, String value);
        T[] build(String testName, int count, String key, String value);
        void reset();
    }

    /**
     * Find the real type arguments for the given class (in its hierarchy), with at least {@code nb} arguments.
     *
     * @param clazz The class to start with.
     * @param nb The minimum number of type arguments.
     * @param <T> The type of the starting class.
     * @return The matching type arguments (a {@link RuntimeException} is thrown if none match).
     */
    private static <T> Type[] findTypeArguments(final Class<T> clazz, final int nb) {
        if (clazz == null || clazz.getSuperclass() == null) {
            throw new RuntimeException("Missing type parameters");
        }
        Type superClassType = clazz.getGenericSuperclass();
        if (superClassType instanceof ParameterizedType && ((ParameterizedType) superClassType)
                .getActualTypeArguments().length >= nb) {
            return ((ParameterizedType) superClassType).getActualTypeArguments();
        }
        return findTypeArguments(clazz.getSuperclass(), nb);
    }

    private abstract static class BaseKeyValueBuilder<T> implements KeyValueBuilder<T> {
        private static final int COUNTER_START = 0;
        private int counter = COUNTER_START;
        @SuppressWarnings("unchecked")
        private final Class<T> builtClass = (Class<T>) findTypeArguments(getClass(), 1)[0];

        protected abstract Builder<T> builder();

        protected abstract void setKey(Builder<T> builder, String key);

        protected abstract void setValue(Builder<T> builder, String value);

        @Override
        public final T build(final String testName, final String key, final String value) {
            final Builder<T> builder = builder();
            this.counter++;
            if (key != null) {
                setKey(builder, String.format(FORMAT_STR, testName, key, this.counter));
            }
            if (value != null) {
                setValue(builder, String.format(FORMAT_STR, testName, value, this.counter));
            }
            return builder.build();
        }

        @SuppressWarnings("unchecked")
        @Override
        public final T[] build(final String testName, final int count, final String key, final String value) {
            final T[] instances = (T[]) Array.newInstance(builtClass, count);
            for (int idx = 0; idx < count; idx++) {
                try {
                    instances[idx] = build(testName, key, value);
                } catch (ArrayStoreException e) {
                    LOG.error("Error storing a value; we think we're managing {}", builtClass, e);
                    throw e;
                }
            }
            return instances;
        }

        @Override
        public final void reset() {
            this.counter = COUNTER_START;
        }
    }

    private static final class SouthboundPortExternalIdsBuilder extends BaseKeyValueBuilder<PortExternalIds> {
        @Override
        protected Builder<PortExternalIds> builder() {
            return new PortExternalIdsBuilder();
        }

        @Override
        protected void setKey(Builder<PortExternalIds> builder, String key) {
            ((PortExternalIdsBuilder) builder).setExternalIdKey(key);
        }

        @Override
        protected void setValue(Builder<PortExternalIds> builder, String value) {
            ((PortExternalIdsBuilder) builder).setExternalIdValue(value);
        }
    }

    private static final class SouthboundInterfaceExternalIdsBuilder extends BaseKeyValueBuilder<InterfaceExternalIds> {
        @Override
        protected Builder<InterfaceExternalIds> builder() {
            return new InterfaceExternalIdsBuilder();
        }

        @Override
        protected void setKey(Builder<InterfaceExternalIds> builder, String key) {
            ((InterfaceExternalIdsBuilder) builder).setExternalIdKey(key);
        }

        @Override
        protected void setValue(Builder<InterfaceExternalIds> builder, String value) {
            ((InterfaceExternalIdsBuilder) builder).setExternalIdValue(value);
        }
    }

    private static final class SouthboundOptionsBuilder extends BaseKeyValueBuilder<Options> {
        @Override
        protected Builder<Options> builder() {
            return new OptionsBuilder();
        }

        @Override
        protected void setKey(Builder<Options> builder, String key) {
            ((OptionsBuilder) builder).setOption(key);
        }

        @Override
        protected void setValue(Builder<Options> builder, String value) {
            ((OptionsBuilder) builder).setValue(value);
        }
    }

    private static final class SouthboundInterfaceOtherConfigsBuilder extends BaseKeyValueBuilder<InterfaceOtherConfigs> {
        @Override
        protected Builder<InterfaceOtherConfigs> builder() {
            return new InterfaceOtherConfigsBuilder();
        }

        @Override
        protected void setKey(Builder<InterfaceOtherConfigs> builder, String key) {
            ((InterfaceOtherConfigsBuilder) builder).setOtherConfigKey(key);
        }

        @Override
        protected void setValue(Builder<InterfaceOtherConfigs> builder, String value) {
            ((InterfaceOtherConfigsBuilder) builder).setOtherConfigValue(value);
        }
    }

    private static final class SouthboundPortOtherConfigsBuilder extends BaseKeyValueBuilder<PortOtherConfigs> {
        @Override
        protected Builder<PortOtherConfigs> builder() {
            return new PortOtherConfigsBuilder();
        }

        @Override
        protected void setKey(Builder<PortOtherConfigs> builder, String key) {
            ((PortOtherConfigsBuilder) builder).setOtherConfigKey(key);
        }

        @Override
        protected void setValue(Builder<PortOtherConfigs> builder, String value) {
            ((PortOtherConfigsBuilder) builder).setOtherConfigValue(value);
        }
    }

    private static final class SouthboundBridgeOtherConfigsBuilder extends BaseKeyValueBuilder<BridgeOtherConfigs> {
        @Override
        protected Builder<BridgeOtherConfigs> builder() {
            return new BridgeOtherConfigsBuilder();
        }

        @Override
        protected void setKey(Builder<BridgeOtherConfigs> builder, String key) {
            ((BridgeOtherConfigsBuilder) builder).setBridgeOtherConfigKey(key);
        }

        @Override
        protected void setValue(Builder<BridgeOtherConfigs> builder, String value) {
            ((BridgeOtherConfigsBuilder) builder).setBridgeOtherConfigValue(value);
        }
    }

    private static final class SouthboundBridgeExternalIdsBuilder extends BaseKeyValueBuilder<BridgeExternalIds> {
        @Override
        protected Builder<BridgeExternalIds> builder() {
            return new BridgeExternalIdsBuilder();
        }

        @Override
        protected void setKey(Builder<BridgeExternalIds> builder, String key) {
            ((BridgeExternalIdsBuilder) builder).setBridgeExternalIdKey(key);
        }

        @Override
        protected void setValue(Builder<BridgeExternalIds> builder, String value) {
            ((BridgeExternalIdsBuilder) builder).setBridgeExternalIdValue(value);
        }
    }

    /*
     * Generates the test cases involved in testing key-value-based data.  See inline comments for descriptions of
     * the particular cases considered.
     */
    private static <T> List<SouthboundTestCase<T>> generateKeyValueTestCases(
            KeyValueBuilder<T> builder, String idKey, String idValue) {
        List<SouthboundTestCase<T>> testCases = new ArrayList<>();

        final String GOOD_KEY = "GoodKey";
        final String GOOD_VALUE = "GoodValue";
        final String NO_VALUE_FOR_KEY = "NoValueForKey";
        final String NO_KEY_FOR_VALUE = "NoKeyForValue";

        // Test Case 1:  TestOne
        // Test Type:    Positive
        // Description:  Create a termination point with one value
        // Expected:     A port is created with the single value specified below
        final String testOneName = "TestOne";
        testCases.add(new SouthboundTestCaseBuilder<T>()
                .name(testOneName)
                .input(builder.build(testOneName, idKey, idValue))
                .expectInputAsOutput()
                .build());
        builder.reset();

        // Test Case 2:  TestFive
        // Test Type:    Positive
        // Description:  Create a termination point with multiple (five) values
        // Expected:     A port is created with the five values specified below
        final String testFiveName = "TestFive";
        testCases.add(new SouthboundTestCaseBuilder<T>()
                .name(testFiveName)
                .input(builder.build(testFiveName, 5, idKey, idValue))
                .expectInputAsOutput()
                .build());
        builder.reset();

        // Test Case 3:  TestOneGoodOneMalformedValue
        // Test Type:    Negative
        // Description:
        //     One perfectly fine input
        //        (TestOneGoodOneMalformedValue_GoodKey_1,
        //        TestOneGoodOneMalformedValue_GoodValue_1)
        //     and one malformed input which only has key specified
        //        (TestOneGoodOneMalformedValue_NoValueForKey_2,
        //        UNSPECIFIED)
        // Expected:     A port is created without any values
        final String testOneGoodOneMalformedValueName = "TestOneGoodOneMalformedValue";
        testCases.add(new SouthboundTestCaseBuilder<T>()
                .name(testOneGoodOneMalformedValueName)
                .input(
                        builder.build(testOneGoodOneMalformedValueName, GOOD_KEY, GOOD_VALUE),
                        builder.build(testOneGoodOneMalformedValueName, NO_VALUE_FOR_KEY, null)
                )
                .expect()
                .build());
        builder.reset();

        // Test Case 4:  TestOneGoodOneMalformedKey
        // Test Type:    Negative
        // Description:
        //     One perfectly fine input
        //        (TestOneGoodOneMalformedKey_GoodKey_1,
        //        TestOneGoodOneMalformedKey_GoodValue_1)
        //     and one malformed input which only has value specified
        //        (UNSPECIFIED,
        //        TestOneGoodOneMalformedKey_NoKeyForValue_2)
        // Expected:     A port is created without any values
        final String testOneGoodOneMalformedKeyName = "TestOneGoodOneMalformedKey";
        testCases.add(new SouthboundTestCaseBuilder<T>()
                .name(testOneGoodOneMalformedKeyName)
                .input(
                        builder.build(testOneGoodOneMalformedKeyName, GOOD_KEY, GOOD_VALUE),
                        builder.build(testOneGoodOneMalformedKeyName, null, NO_KEY_FOR_VALUE)
                )
                .expect()
                .build());
        builder.reset();

        return testCases;
    }

    /*
     * Generates the test cases involved in testing PortExternalIds.  See inline comments for descriptions of
     * the particular cases considered.
     */
    private List<SouthboundTestCase<PortExternalIds>> generatePortExternalIdsTestCases() {
        return generateKeyValueTestCases(new SouthboundPortExternalIdsBuilder(), "PortExternalIdKey",
                "PortExternalIdValue");
    }

    /*
     * Tests the CRUD operations for <code>Port</code>
     * <code>external_ids</code>.
     *
     * @see <code>SouthboundIT.generatePortExternalIdsTestCases()</code> for
     * specific test case information
     */
    @Test
    public void testCRUDTerminationPointPortExternalIds()
            throws InterruptedException, ExecutionException {

        final String TEST_PREFIX = "CRUDTPPortExternalIds";

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);

        // updateFromTestCases represent the original test case value.
        // updateToTestCases represent the new value after the update has been
        // performed.
        List<SouthboundTestCase<PortExternalIds>> updateFromTestCases = generatePortExternalIdsTestCases();
        List<SouthboundTestCase<PortExternalIds>> updateToTestCases = generatePortExternalIdsTestCases();
        String testBridgeName;
        String testPortName;

        int counter = 1;
        // multihreads the test using NUM_THREADS
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (SouthboundTestCase<PortExternalIds> fromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<PortExternalIds> toTestCase : updateToTestCases) {
                testPortName = testBridgeName = String.format(FORMAT_STR,
                        TEST_PREFIX, toTestCase.name, counter);
                counter += 1;
                executor.submit(new TestCRUDTerminationPointRunnable<>(
                        new SouthboundTestHelper<PortExternalIds>() {
                            @Override
                            public List<PortExternalIds> readValues(
                                    OvsdbTerminationPointAugmentation augmentation) {
                                return augmentation.getPortExternalIds();
                            }

                            @Override
                            public void writeValues(
                                    OvsdbTerminationPointAugmentationBuilder augmentationBuilder,
                                    List<PortExternalIds> updateFromInput) {
                                augmentationBuilder.setPortExternalIds(updateFromInput);
                            }
                        },
                        connectionInfo, testBridgeName, testPortName,
                        fromTestCase.inputValues,
                        fromTestCase.expectedValues,
                        toTestCase.inputValues,
                        toTestCase.expectedValues));
            }
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    /**
     * Southbound test helper. Classes implementing this interface are used to provide concrete access to the input and
     * output of the underlying augmentation for the type being managed.
     *
     * @param <T> The type of data used for the test case.
     */
    private interface SouthboundTestHelper<T> {
        /**
         * Read the values from the augmentation. These would usually be checked against the expected values provided
         * for the test case.
         *
         * @param augmentation The augmentation to read from.
         * @return The values read.
         */
        List<T> readValues(OvsdbTerminationPointAugmentation augmentation);

        /**
         * Write the values to the augmentation (via its builder). This would usually be used to apply the input values
         * provided for the test case.
         *
         * @param augmentationBuilder The augmentation builder.
         * @param values The values to write.
         */
        void writeValues(OvsdbTerminationPointAugmentationBuilder augmentationBuilder, List<T> values);
    }

    /**
     * <p>
     * Test runner used to apply a suite of create/read/update/delete tests. Each instance of a runner expects:
     * </p>
     * <ul>
     * <li>a helper used to manipulate the appropriate data structures in the termination point augmentation (see
     * {@link SouthboundTestHelper});</li>
     * <li>connection information for the southbound;</li>
     * <li>a name to use for the test bridge (this allows multiple tests to be conducted in parallel against different
     * bridges);</li>
     * <li>a name to use for the test port;</li>
     * <li>the initial input values to use for the termination point augmentation;</li>
     * <li>the initial expected values to check the augmentation against;</li>
     * <li>the target input values to update the terminal point to;</li>
     * <li>the target expected values to check the augmentation point against.</li>
     * </ul>
     * <p>The following tests are performed:</p>
     * <ol>
     * <li>the bridge is added;</li>
     * <li>the termination point is added, with the provided initial input values;</li>
     * <li>the termination point is read from the <em>configuration</em> data store, and checked against the provided
     * initial expected values;</li>
     * <li>the termination point is read from the <em>operational</em> data store, and checked against the provided
     * initial expected values;</li>
     * <li>the termination point is updated by merging the provided target input values;</li>
     * <li>the termination point is read from the <em>configuration</em> data store, and checked against the provided
     * initial <b>and</b> target expected values;</li>
     * <li>the termination point is read from the <em>operational</em> data store, and checked against the provided
     * initial <b>and</b> target expected values;</li>
     * <li>the bridge is deleted.</li>
     * </ol>
     *
     * @param <T> The type of data used for the test case.
     */
    private final class TestCRUDTerminationPointRunnable<T> implements Runnable {
        private final SouthboundTestHelper<T> helper;
        private final ConnectionInfo connectionInfo;
        private final String testBridgeName;
        private final String testPortName;
        private final List<T> updateFromInput;
        private final List<T> updateFromExpected;
        private final List<T> updateToInput;
        private final List<T> updateToExpected;

        private TestCRUDTerminationPointRunnable(
                SouthboundTestHelper<T> helper, ConnectionInfo connectionInfo, String testBridgeName,
                String testPortName, List<T> updateFromInput, List<T> updateFromExpected, List<T> updateToInput,
                List<T> updateToExpected) {
            this.helper = helper;
            this.connectionInfo = connectionInfo;
            this.testBridgeName = testBridgeName;
            this.testPortName = testPortName;
            this.updateFromInput = updateFromInput;
            this.updateFromExpected = updateFromExpected;
            this.updateToInput = updateToInput;
            this.updateToExpected = updateToExpected;
        }

        @Override
        public void run() {
            try {
                final int TERMINATION_POINT_TEST_INDEX = 0;
                // CREATE: Create the test bridge
                Assert.assertTrue(addBridge(connectionInfo, null,
                        testBridgeName, null, true,
                        SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"),
                        true, null, null, null, null));
                NodeId testBridgeNodeId = createManagedNodeId(createInstanceIdentifier(
                        connectionInfo, new OvsdbBridgeName(testBridgeName)));
                OvsdbTerminationPointAugmentationBuilder tpCreateAugmentationBuilder =
                        createGenericOvsdbTerminationPointAugmentationBuilder();
                tpCreateAugmentationBuilder.setName(testPortName);
                helper.writeValues(tpCreateAugmentationBuilder, updateFromInput);
                Assert.assertTrue(addTerminationPoint(testBridgeNodeId, testPortName, tpCreateAugmentationBuilder));

                // READ: Read the test port and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                OvsdbTerminationPointAugmentation updateFromConfigurationTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                List<T> updateFromConfiguration = null;
                if (updateFromConfigurationTerminationPointAugmentation != null) {
                    updateFromConfiguration =
                        helper.readValues(updateFromConfigurationTerminationPointAugmentation);
                }
                if (updateFromConfiguration != null) {
                    Assert.assertTrue(updateFromConfiguration.containsAll(updateFromExpected));
                }
                OvsdbTerminationPointAugmentation updateFromOperationalTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                List<T> updateFromOperational = null;
                if (updateFromOperationalTerminationPointAugmentation != null) {
                    updateFromOperational = helper.readValues(updateFromOperationalTerminationPointAugmentation);
                }
                if (updateFromOperational != null) {
                    Assert.assertTrue(updateFromOperational.containsAll(updateFromExpected));
                }

                // UPDATE:  update the external_ids
                testBridgeNodeId = getBridgeNode(connectionInfo, testBridgeName).getNodeId();
                OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder =
                        new OvsdbTerminationPointAugmentationBuilder();
                helper.writeValues(tpUpdateAugmentationBuilder, updateToInput);
                InstanceIdentifier<Node> portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
                NodeBuilder portUpdateNodeBuilder = new NodeBuilder();
                NodeId portUpdateNodeId = createManagedNodeId(portIid);
                portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
                TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
                tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(testPortName)));
                tpUpdateBuilder.addAugmentation(
                        OvsdbTerminationPointAugmentation.class,
                        tpUpdateAugmentationBuilder.build());
                portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
                boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                        portIid, portUpdateNodeBuilder.build());
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
                Assert.assertTrue(result);

                // READ: the test port and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                OvsdbTerminationPointAugmentation updateToConfigurationTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                List<T> updateToConfiguration = helper.readValues(updateToConfigurationTerminationPointAugmentation);
                Assert.assertTrue(updateToConfiguration.containsAll(updateToExpected));
                Assert.assertTrue(updateToConfiguration.containsAll(updateFromExpected));
                OvsdbTerminationPointAugmentation updateToOperationalTerminationPointAugmentation =
                        getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeName,
                                LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                List<T> updateToOperational = helper.readValues(updateToOperationalTerminationPointAugmentation);
                Assert.assertTrue(updateToOperational.containsAll(updateToExpected));
                Assert.assertTrue(updateToOperational.containsAll(updateFromExpected));

                // DELETE
                Assert.assertTrue(deleteBridge(connectionInfo, testBridgeName));
            } catch (InterruptedException e) {
                LOG.error("Test interrupted", e);
            }
        }
    }


    /*
     * Generates the test cases involved in testing InterfaceExternalIds.  See inline comments for descriptions of
     * the particular cases considered.
     */
    private static List<SouthboundTestCase<InterfaceExternalIds>> generateInterfaceExternalIdsTestCases() {
        return generateKeyValueTestCases(new SouthboundInterfaceExternalIdsBuilder(), "IntExternalIdKey",
                "IntExternalIdValue");
    }

    /*
     * Tests the CRUD operations for <code>Interface</code> <code>external_ids</code>.
     *
     * @see <code>SouthboundIT.generateInterfaceExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointInterfaceExternalIds() throws InterruptedException, ExecutionException {
        final String TEST_PREFIX = "CRUDTPInterfaceExternalIds";

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<InterfaceExternalIds>> updateFromTestCases = generateInterfaceExternalIdsTestCases();
        List<SouthboundTestCase<InterfaceExternalIds>> updateToTestCases = generateInterfaceExternalIdsTestCases();
        String testBridgeName;
        String testPortName;

        int counter = 1;
        // multithreads the test using NUM_THREADS
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (SouthboundTestCase<InterfaceExternalIds> fromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<InterfaceExternalIds> toTestCase : updateToTestCases) {
                testPortName = testBridgeName = String.format(FORMAT_STR,
                        TEST_PREFIX, toTestCase.name, counter);
                counter += 1;
                executor.submit(new TestCRUDTerminationPointRunnable<>(
                        new SouthboundTestHelper<InterfaceExternalIds>() {
                            @Override
                            public List<InterfaceExternalIds> readValues(
                                    OvsdbTerminationPointAugmentation augmentation) {
                                return augmentation.getInterfaceExternalIds();
                            }

                            @Override
                            public void writeValues(
                                    OvsdbTerminationPointAugmentationBuilder augmentationBuilder,
                                    List<InterfaceExternalIds> values) {
                                augmentationBuilder.setInterfaceExternalIds(values);
                            }
                        },
                        connectionInfo, testBridgeName, testPortName,
                        fromTestCase.inputValues,
                        fromTestCase.expectedValues,
                        toTestCase.inputValues,
                        toTestCase.expectedValues));
            }
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    /*
     * Generates the test cases involved in testing TP Options.  See inline comments for descriptions of
     * the particular cases considered.
     */
    private List<SouthboundTestCase<Options>> generateTerminationPointOptionsTestCases() {
        return generateKeyValueTestCases(new SouthboundOptionsBuilder(), "TOPOptionsKey", "TPOptionsValue");
    }

    /*
     * Tests the CRUD operations for <code>TerminationPoint</code> <code>options</code>.
     *
     * @see <code>SouthboundIT.generateTerminationPointOptions()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointOptions() throws InterruptedException {
        final String TEST_PREFIX = "CRUDTPOptions";

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<Options>> updateFromTestCases = generateTerminationPointOptionsTestCases();
        List<SouthboundTestCase<Options>> updateToTestCases = generateTerminationPointOptionsTestCases();
        String testBridgeName;
        String testPortName;

        int counter = 1;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (SouthboundTestCase<Options> fromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<Options> toTestCase : updateToTestCases) {
                testPortName = testBridgeName = String.format(FORMAT_STR,
                        TEST_PREFIX, toTestCase.name, counter);
                counter += 1;
                executor.submit(new TestCRUDTerminationPointRunnable<>(
                        new SouthboundTestHelper<Options>() {
                            @Override
                            public List<Options> readValues(
                                    OvsdbTerminationPointAugmentation augmentation) {
                                return augmentation.getOptions();
                            }

                            @Override
                            public void writeValues(
                                    OvsdbTerminationPointAugmentationBuilder augmentationBuilder,
                                    List<Options> values) {
                                augmentationBuilder.setOptions(values);
                            }
                        },
                        connectionInfo, testBridgeName, testPortName,
                        fromTestCase.inputValues,
                        fromTestCase.expectedValues,
                        toTestCase.inputValues,
                        toTestCase.expectedValues));
            }
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    /*
     * Generates the test cases involved in testing Interface other_configs.  See inline comments for descriptions of
     * the particular cases considered.
     */
    private List<SouthboundTestCase<InterfaceOtherConfigs>> generateInterfaceOtherConfigsTestCases() {
        return generateKeyValueTestCases(new SouthboundInterfaceOtherConfigsBuilder(), "IntOtherConfigsKey",
                "IntOtherConfigsValue");
    }

    /*
     * Tests the CRUD operations for <code>Interface</code> <code>other_configs</code>.
     *
     * @see <code>SouthboundIT.generateInterfaceExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointInterfaceOtherConfigs() throws InterruptedException {
        final String TEST_PREFIX = "CRUDTPInterfaceOtherConfigs";

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<InterfaceOtherConfigs>> updateFromTestCases = generateInterfaceOtherConfigsTestCases();
        List<SouthboundTestCase<InterfaceOtherConfigs>> updateToTestCases = generateInterfaceOtherConfigsTestCases();
        String testBridgeName;
        String testPortName;

        int counter = 1;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (SouthboundTestCase<InterfaceOtherConfigs> fromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<InterfaceOtherConfigs> toTestCase : updateToTestCases) {
                testPortName = testBridgeName = String.format(FORMAT_STR,
                        TEST_PREFIX, toTestCase.name, counter);
                counter += 1;
                executor.submit(new TestCRUDTerminationPointRunnable<>(
                        new SouthboundTestHelper<InterfaceOtherConfigs>() {
                            @Override
                            public List<InterfaceOtherConfigs> readValues(
                                    OvsdbTerminationPointAugmentation augmentation) {
                                return augmentation.getInterfaceOtherConfigs();
                            }

                            @Override
                            public void writeValues(
                                    OvsdbTerminationPointAugmentationBuilder augmentationBuilder,
                                    List<InterfaceOtherConfigs> values) {
                                augmentationBuilder.setInterfaceOtherConfigs(values);
                            }
                        },
                        connectionInfo, testBridgeName, testPortName,
                        fromTestCase.inputValues,
                        fromTestCase.expectedValues,
                        toTestCase.inputValues,
                        toTestCase.expectedValues));
            }
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    /*
     * Generates the test cases involved in testing Port other_configs.  See inline comments for descriptions of
     * the particular cases considered.
     */
    private List<SouthboundTestCase<PortOtherConfigs>> generatePortOtherConfigsTestCases() {
        return generateKeyValueTestCases(new SouthboundPortOtherConfigsBuilder(), "PortOtherConfigsKey",
                "PortOtherConfigsValue");
    }

    /*
     * Tests the CRUD operations for <code>Port</code> <code>other_configs</code>.
     *
     * @see <code>SouthboundIT.generatePortExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointPortOtherConfigs() throws InterruptedException {
        final String TEST_PREFIX = "CRUDTPPortOtherConfigs";

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<PortOtherConfigs>> updateFromTestCases = generatePortOtherConfigsTestCases();
        List<SouthboundTestCase<PortOtherConfigs>> updateToTestCases = generatePortOtherConfigsTestCases();
        String testBridgeName;
        String testPortName;

        int counter = 1;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (SouthboundTestCase<PortOtherConfigs> fromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<PortOtherConfigs> toTestCase : updateToTestCases) {
                testPortName = testBridgeName = String.format(FORMAT_STR,
                        TEST_PREFIX, toTestCase.name, counter);
                counter += 1;
                executor.submit(new TestCRUDTerminationPointRunnable<>(
                        new SouthboundTestHelper<PortOtherConfigs>() {
                            @Override
                            public List<PortOtherConfigs> readValues(
                                    OvsdbTerminationPointAugmentation augmentation) {
                                return augmentation.getPortOtherConfigs();
                            }

                            @Override
                            public void writeValues(
                                    OvsdbTerminationPointAugmentationBuilder augmentationBuilder,
                                    List<PortOtherConfigs> values) {
                                augmentationBuilder.setPortOtherConfigs(values);
                            }
                        },
                        connectionInfo, testBridgeName, testPortName,
                        fromTestCase.inputValues,
                        fromTestCase.expectedValues,
                        toTestCase.inputValues,
                        toTestCase.expectedValues));
            }
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testCRUDTerminationPointVlan() throws InterruptedException {
        final Integer CREATED_VLAN_ID = 4000;
        final Integer UPDATED_VLAN_ID = 4001;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);

        // CREATE
        Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME);
        Assert.assertNotNull(bridge);
        NodeId nodeId = createManagedNodeId(createInstanceIdentifier(
                connectionInfo, bridge.getBridgeName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        String portName = "testTerminationPointVlanId";
        ovsdbTerminationBuilder.setName(portName);
        ovsdbTerminationBuilder.setVlanTag(new VlanId(CREATED_VLAN_ID));
        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);

        // READ
        List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
        OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation;
        for (TerminationPoint terminationPoint : terminationPoints) {
            ovsdbTerminationPointAugmentation = terminationPoint.getAugmentation(
                    OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                VlanId actualVlanId = ovsdbTerminationPointAugmentation.getVlanTag();
                Assert.assertNotNull(actualVlanId);
                Integer actualVlanIdInt = actualVlanId.getValue();
                Assert.assertEquals(CREATED_VLAN_ID, actualVlanIdInt);
            }
        }

        // UPDATE
        NodeId testBridgeNodeId = getBridgeNode(connectionInfo, SouthboundITConstants.BRIDGE_NAME).getNodeId();
        OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();
        tpUpdateAugmentationBuilder.setVlanTag(new VlanId(UPDATED_VLAN_ID));
        InstanceIdentifier<Node> portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
        NodeBuilder portUpdateNodeBuilder = new NodeBuilder();
        NodeId portUpdateNodeId = createManagedNodeId(portIid);
        portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
        TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
        tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(portName)));
        tpUpdateBuilder.addAugmentation(
                OvsdbTerminationPointAugmentation.class,
                tpUpdateAugmentationBuilder.build());
        tpUpdateBuilder.setTpId(new TpId(portName));
        portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
        boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                portIid, portUpdateNodeBuilder.build());
        Assert.assertTrue(result);
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);

        terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        terminationPoints = terminationPointNode.getTerminationPoint();
        for (TerminationPoint terminationPoint : terminationPoints) {
            ovsdbTerminationPointAugmentation = terminationPoint.getAugmentation(
                    OvsdbTerminationPointAugmentation.class);
            if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                VlanId actualVlanId = ovsdbTerminationPointAugmentation.getVlanTag();
                Assert.assertNotNull(actualVlanId);
                Integer actualVlanIdInt = actualVlanId.getValue();
                Assert.assertEquals(UPDATED_VLAN_ID, actualVlanIdInt);
            }
        }

        // DELETE
        Assert.assertTrue(deleteBridge(connectionInfo));
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testCRUDTerminationPointVlanModes() throws InterruptedException {
        final VlanMode UPDATED_VLAN_MODE = VlanMode.Access;
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);
        VlanMode []vlanModes = VlanMode.values();
        for (VlanMode vlanMode : vlanModes) {
            // CREATE
            Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            Assert.assertNotNull(bridge);
            NodeId nodeId = createManagedNodeId(createInstanceIdentifier(
                    connectionInfo, bridge.getBridgeName()));
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                    createGenericOvsdbTerminationPointAugmentationBuilder();
            String portName = "testTerminationPointVlanMode" + vlanMode.toString();
            ovsdbTerminationBuilder.setName(portName);
            ovsdbTerminationBuilder.setVlanMode(vlanMode);
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
                    //test
                    Assert.assertTrue(ovsdbTerminationPointAugmentation.getVlanMode().equals(vlanMode));
                }
            }

            // UPDATE
            NodeId testBridgeNodeId = getBridgeNode(connectionInfo, SouthboundITConstants.BRIDGE_NAME).getNodeId();
            OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder =
                    new OvsdbTerminationPointAugmentationBuilder();
            tpUpdateAugmentationBuilder.setVlanMode(UPDATED_VLAN_MODE);
            InstanceIdentifier<Node> portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
            NodeBuilder portUpdateNodeBuilder = new NodeBuilder();
            NodeId portUpdateNodeId = createManagedNodeId(portIid);
            portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
            TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
            tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(portName)));
            tpUpdateBuilder.addAugmentation(
                    OvsdbTerminationPointAugmentation.class,
                    tpUpdateAugmentationBuilder.build());
            tpUpdateBuilder.setTpId(new TpId(portName));
            portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
            boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                    portIid, portUpdateNodeBuilder.build());
            Assert.assertTrue(result);
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);

            terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            terminationPoints = terminationPointNode.getTerminationPoint();
            for (TerminationPoint terminationPoint : terminationPoints) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    //test
                    Assert.assertEquals(UPDATED_VLAN_MODE, ovsdbTerminationPointAugmentation.getVlanMode());
                }
            }

            // DELETE
            Assert.assertTrue(deleteBridge(connectionInfo));
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    private ArrayList<Set<Integer>> generateVlanSets() {
        ArrayList<Set<Integer>> vlanSets = new ArrayList<>();

        Set<Integer> emptySet = new HashSet<>();
        vlanSets.add(emptySet);

        Set<Integer> singleSet = new HashSet<>();
        Integer single = 2222;
        singleSet.add(single);
        vlanSets.add(singleSet);

        Set<Integer> minMaxMiddleSet = new HashSet<>();
        Integer min = 0;
        minMaxMiddleSet.add(min);
        Integer max = 4095;
        minMaxMiddleSet.add(max);
        Integer minPlusOne = min + 1;
        minMaxMiddleSet.add(minPlusOne);
        Integer maxMinusOne = max - 1;
        minMaxMiddleSet.add(maxMinusOne);
        Integer middle = (max - min) / 2;
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
    public void testCRUDTerminationPointVlanTrunks() throws InterruptedException {
        final List<Trunks> UPDATED_TRUNKS = buildTrunkList(Sets.newHashSet(2011));
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);
        Iterable<Set<Integer>> vlanSets = generateVlanSets();
        int testCase = 0;
        for (Set<Integer> vlanSet : vlanSets) {
            ++testCase;
            // CREATE
            Assert.assertTrue(addBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME));
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            Assert.assertNotNull(bridge);
            NodeId nodeId = createManagedNodeId(createInstanceIdentifier(
                    connectionInfo, bridge.getBridgeName()));
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                    createGenericOvsdbTerminationPointAugmentationBuilder();
            String portName = "testTerminationPointVlanTrunks" + testCase;
            ovsdbTerminationBuilder.setName(portName);
            List<Trunks> trunks = buildTrunkList(vlanSet);
            ovsdbTerminationBuilder.setTrunks(trunks);
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
                    List<Trunks> actualTrunks = ovsdbTerminationPointAugmentation.getTrunks();
                    for (Trunks trunk : trunks) {
                        Assert.assertTrue(actualTrunks.contains(trunk));
                    }
                }
            }


            // UPDATE
            NodeId testBridgeNodeId = getBridgeNode(connectionInfo, SouthboundITConstants.BRIDGE_NAME).getNodeId();
            OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder =
                    new OvsdbTerminationPointAugmentationBuilder();
            tpUpdateAugmentationBuilder.setTrunks(UPDATED_TRUNKS);
            InstanceIdentifier<Node> portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
            NodeBuilder portUpdateNodeBuilder = new NodeBuilder();
            NodeId portUpdateNodeId = createManagedNodeId(portIid);
            portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
            TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
            tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(portName)));
            tpUpdateBuilder.addAugmentation(
                    OvsdbTerminationPointAugmentation.class,
                    tpUpdateAugmentationBuilder.build());
            tpUpdateBuilder.setTpId(new TpId(portName));
            portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
            boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                    portIid, portUpdateNodeBuilder.build());
            Assert.assertTrue(result);
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);

            terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
            terminationPoints = terminationPointNode.getTerminationPoint();
            for (TerminationPoint terminationPoint : terminationPoints) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    //test
                    Assert.assertEquals(UPDATED_TRUNKS, ovsdbTerminationPointAugmentation.getTrunks());
                }
            }

            // DELETE
            Assert.assertTrue(deleteBridge(connectionInfo));
        }
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testGetOvsdbNodes() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);
        InstanceIdentifier<Topology> topologyPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        Topology topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, topologyPath);
        InstanceIdentifier<Node> expectedNodeIid = createInstanceIdentifier(connectionInfo);
        NodeId expectedNodeId = expectedNodeIid.firstKeyOf(Node.class, NodeKey.class).getNodeId();
        Node foundNode = null;
        Assert.assertNotNull("Expected to find topology: " + topologyPath, topology);
        Assert.assertNotNull("Expected to find some nodes" + topology.getNode());
        LOG.info("expectedNodeId: {}, getNode: {}", expectedNodeId, topology.getNode());
        for (Node node : topology.getNode()) {
            if (node.getNodeId().getValue().equals(expectedNodeId.getValue())) {
                foundNode = node;
                break;
            }
        }
        Assert.assertNotNull("Expected to find Node: " + expectedNodeId, foundNode);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    /*
     * Generates the test cases involved in testing BridgeOtherConfigs.  See inline comments for descriptions of
     * the particular cases considered.
     */
    private List<SouthboundTestCase<BridgeOtherConfigs>> generateBridgeOtherConfigsTestCases() {
        return generateKeyValueTestCases(new SouthboundBridgeOtherConfigsBuilder(), "BridgeOtherConfigsKey",
                "BridgeOtherConfigsValue");
    }

    /*
     * @see <code>SouthboundIT.testCRUDBridgeOtherConfigs()</code>
     * This is helper test method to compare a test "set" of BridgeExternalIds against an expected "set"
     */
    private void assertExpectedBridgeOtherConfigsExist( List<BridgeOtherConfigs> expected,
                                                        List<BridgeOtherConfigs> test ) {

        if (expected != null) {
            for (BridgeOtherConfigs expectedOtherConfig : expected) {
                Assert.assertTrue(test.contains(expectedOtherConfig));
            }
        }
    }

    /*
     * @see <code>SouthboundIT.generateBridgeOtherConfigsTestCases()</code> for specific test case information.
     */
    @Test
    public void testCRUDBridgeOtherConfigs() throws InterruptedException {
        final String TEST_BRIDGE_PREFIX = "CRUDBridgeOtherConfigs";
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);
        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<BridgeOtherConfigs>> updateFromTestCases = generateBridgeOtherConfigsTestCases();
        List<SouthboundTestCase<BridgeOtherConfigs>> updateToTestCases = generateBridgeOtherConfigsTestCases();
        String testBridgeName;

        int counter = 1;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (SouthboundTestCase<BridgeOtherConfigs> fromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<BridgeOtherConfigs> toTestCase : updateToTestCases) {
                testBridgeName = String.format(FORMAT_STR, TEST_BRIDGE_PREFIX, toTestCase.name, counter);
                counter += 1;
                TestCRUDBridgeOtherConfigsRunnable testRunnable =
                        new TestCRUDBridgeOtherConfigsRunnable(
                                connectionInfo, testBridgeName,
                                fromTestCase.inputValues,
                                fromTestCase.expectedValues,
                                toTestCase.inputValues,
                                toTestCase.expectedValues);
                executor.submit(testRunnable);
            }
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);

        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    class TestCRUDBridgeOtherConfigsRunnable implements Runnable {

        ConnectionInfo connectionInfo;
        String testBridgeName;
        List<BridgeOtherConfigs> updateFromInputOtherConfigs;
        List<BridgeOtherConfigs> updateFromExpectedOtherConfigs;
        List<BridgeOtherConfigs> updateToInputOtherConfigs;
        List<BridgeOtherConfigs> updateToExpectedOtherConfigs;

        TestCRUDBridgeOtherConfigsRunnable(
                ConnectionInfo connectionInfo, String testBridgeName,
                List<BridgeOtherConfigs> updateFromInputOtherConfigs,
                List<BridgeOtherConfigs> updateFromExpectedOtherConfigs,
                List<BridgeOtherConfigs> updateToInputOtherConfigs,
                List<BridgeOtherConfigs> updateToExpectedOtherConfigs) {

            this.connectionInfo = connectionInfo;
            this.testBridgeName = testBridgeName;
            this.updateFromInputOtherConfigs = updateFromInputOtherConfigs;
            this.updateFromExpectedOtherConfigs = updateFromExpectedOtherConfigs;
            this.updateToInputOtherConfigs = updateToInputOtherConfigs;
            this.updateToExpectedOtherConfigs = updateToExpectedOtherConfigs;
        }

        @Override
        public void run() {
            try {
                test();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void test() throws InterruptedException {
            // CREATE: Create the test bridge
            boolean bridgeAdded = addBridge(connectionInfo, null,
                    testBridgeName, null, true, SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"),
                    true, null, null, null, updateFromInputOtherConfigs);
            Assert.assertTrue(bridgeAdded);

            // READ: Read the test bridge and ensure changes are propagated to the CONFIGURATION data store,
            // then repeat for OPERATIONAL data store
            List<BridgeOtherConfigs> updateFromConfigurationOtherConfigs = getBridge(connectionInfo, testBridgeName,
                    LogicalDatastoreType.CONFIGURATION).getBridgeOtherConfigs();
            assertExpectedBridgeOtherConfigsExist(updateFromExpectedOtherConfigs,
                    updateFromConfigurationOtherConfigs);
            List<BridgeOtherConfigs> updateFromOperationalOtherConfigs = getBridge(connectionInfo, testBridgeName)
                    .getBridgeOtherConfigs();
            assertExpectedBridgeOtherConfigsExist(updateFromExpectedOtherConfigs,
                    updateFromOperationalOtherConfigs);

            // UPDATE:  update the external_ids
            OvsdbBridgeAugmentationBuilder bridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            bridgeAugmentationBuilder.setBridgeOtherConfigs(updateToInputOtherConfigs);
            InstanceIdentifier<Node> bridgeIid =
                    createInstanceIdentifier(connectionInfo,
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
            List<BridgeOtherConfigs> updateToConfigurationOtherConfigs = getBridge(connectionInfo, testBridgeName,
                    LogicalDatastoreType.CONFIGURATION).getBridgeOtherConfigs();
            assertExpectedBridgeOtherConfigsExist(updateToExpectedOtherConfigs, updateToConfigurationOtherConfigs);
            assertExpectedBridgeOtherConfigsExist(updateFromExpectedOtherConfigs,
                    updateToConfigurationOtherConfigs);
            List<BridgeOtherConfigs> updateToOperationalOtherConfigs = getBridge(connectionInfo, testBridgeName)
                    .getBridgeOtherConfigs();
            if (updateFromExpectedOtherConfigs != null) {
                assertExpectedBridgeOtherConfigsExist(updateToExpectedOtherConfigs,
                        updateToOperationalOtherConfigs);
                assertExpectedBridgeOtherConfigsExist(updateFromExpectedOtherConfigs,
                        updateToOperationalOtherConfigs);
            }

            // DELETE
            Assert.assertTrue(deleteBridge(connectionInfo, testBridgeName));
        }
    }

    /*
     * Generates the test cases involved in testing BridgeExternalIds.  See inline comments for descriptions of
     * the particular cases considered.
     */
    private List<SouthboundTestCase<BridgeExternalIds>> generateBridgeExternalIdsTestCases() {
        return generateKeyValueTestCases(new SouthboundBridgeExternalIdsBuilder(), "BridgeExternalIdsKey",
                "BridgeExternalIdsValue");
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
        }
    }

    /*
     * @see <code>SouthboundIT.generateBridgeExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDBridgeExternalIds() throws InterruptedException {
        final String TEST_BRIDGE_PREFIX = "CRUDBridgeExternalIds";
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        connectOvsdbNode(connectionInfo);
        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<BridgeExternalIds>> updateFromTestCases = generateBridgeExternalIdsTestCases();
        List<SouthboundTestCase<BridgeExternalIds>> updateToTestCases = generateBridgeExternalIdsTestCases();
        String testBridgeName;

        int counter = 1;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (SouthboundTestCase<BridgeExternalIds> fromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<BridgeExternalIds> toTestCase : updateToTestCases) {
                testBridgeName = String.format(FORMAT_STR, TEST_BRIDGE_PREFIX, toTestCase.name, counter);
                counter += 1;
                TestCRUDBridgeExternalIdsRunnable testRunnable =
                        new TestCRUDBridgeExternalIdsRunnable(
                                connectionInfo, testBridgeName,
                                fromTestCase.inputValues,
                                fromTestCase.expectedValues,
                                toTestCase.inputValues,
                                toTestCase.expectedValues);
                executor.submit(testRunnable);
            }
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);

        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    class TestCRUDBridgeExternalIdsRunnable implements Runnable {
        ConnectionInfo connectionInfo;
        String testBridgeName;
        List<BridgeExternalIds> updateFromInputExternalIds;
        List<BridgeExternalIds> updateFromExpectedExternalIds;
        List<BridgeExternalIds> updateToInputExternalIds;
        List<BridgeExternalIds> updateToExpectedExternalIds;

        TestCRUDBridgeExternalIdsRunnable(
                ConnectionInfo connectionInfo, String testBridgeName,
                List<BridgeExternalIds> updateFromInputExternalIds,
                List<BridgeExternalIds> updateFromExpectedExternalIds,
                List<BridgeExternalIds> updateToInputExternalIds,
                List<BridgeExternalIds> updateToExpectedExternalIds) {

            this.connectionInfo = connectionInfo;
            this.testBridgeName = testBridgeName;
            this.updateFromInputExternalIds = updateFromInputExternalIds;
            this.updateFromExpectedExternalIds = updateFromExpectedExternalIds;
            this.updateToInputExternalIds = updateToInputExternalIds;
            this.updateToExpectedExternalIds = updateToExpectedExternalIds;
        }

        @Override
        public void run() {
            try {
                test();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void test() throws InterruptedException {
            // CREATE: Create the test bridge
            boolean bridgeAdded = addBridge(connectionInfo, null,
                    testBridgeName, null, true, SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"),
                    true, null, updateFromInputExternalIds, null, null);
            Assert.assertTrue(bridgeAdded);

            // READ: Read the test bridge and ensure changes are propagated to the CONFIGURATION data store,
            // then repeat for OPERATIONAL data store
            List<BridgeExternalIds> updateFromConfigurationExternalIds = getBridge(connectionInfo, testBridgeName,
                    LogicalDatastoreType.CONFIGURATION).getBridgeExternalIds();
            assertExpectedBridgeExternalIdsExist(updateFromExpectedExternalIds, updateFromConfigurationExternalIds);
            List<BridgeExternalIds> updateFromOperationalExternalIds = getBridge(connectionInfo, testBridgeName)
                    .getBridgeExternalIds();
            assertExpectedBridgeExternalIdsExist(updateFromExpectedExternalIds, updateFromOperationalExternalIds);

            // UPDATE:  update the external_ids
            OvsdbBridgeAugmentationBuilder bridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            bridgeAugmentationBuilder.setBridgeExternalIds(updateToInputExternalIds);
            InstanceIdentifier<Node> bridgeIid =
                    createInstanceIdentifier(connectionInfo,
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
            List<BridgeExternalIds> updateToConfigurationExternalIds = getBridge(connectionInfo, testBridgeName,
                    LogicalDatastoreType.CONFIGURATION).getBridgeExternalIds();
            assertExpectedBridgeExternalIdsExist(updateToExpectedExternalIds, updateToConfigurationExternalIds);
            assertExpectedBridgeExternalIdsExist(updateFromExpectedExternalIds, updateToConfigurationExternalIds);
            List<BridgeExternalIds> updateToOperationalExternalIds = getBridge(connectionInfo, testBridgeName)
                    .getBridgeExternalIds();
            if (updateFromExpectedExternalIds != null) {
                assertExpectedBridgeExternalIdsExist(updateToExpectedExternalIds, updateToOperationalExternalIds);
                assertExpectedBridgeExternalIdsExist(updateFromExpectedExternalIds, updateToOperationalExternalIds);
            }

            // DELETE
            Assert.assertTrue(deleteBridge(connectionInfo, testBridgeName));
        }
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key,OvsdbBridgeName bridgeName) {
        return SouthboundMapper.createInstanceIdentifier(createManagedNodeId(key, bridgeName));
    }

    public static NodeId createManagedNodeId(ConnectionInfo key, OvsdbBridgeName bridgeName) {
        return createManagedNodeId(key.getRemoteIp(), key.getRemotePort(), bridgeName);
    }

    public static NodeId createManagedNodeId(IpAddress ip, PortNumber port, OvsdbBridgeName bridgeName) {
        return new NodeId(createNodeId(ip,port).getValue()
                + "/" + SouthboundConstants.BRIDGE_URI_PREFIX + "/" + bridgeName.getValue());
    }

    public static NodeId createNodeId(IpAddress ip, PortNumber port) {
        String uriString = SouthboundConstants.OVSDB_URI_PREFIX + "://"
                + new String(ip.getValue()) + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        return new NodeId(uri);
    }

    public static NodeKey createNodeKey(IpAddress ip, PortNumber port) {
        return new NodeKey(createNodeId(ip,port));
    }

    public static Node createNode(ConnectionInfo key) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(createNodeId(key.getRemoteIp(),key.getRemotePort()));
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, createOvsdbAugmentation(key));
        return nodeBuilder.build();
    }

    public static OvsdbNodeAugmentation createOvsdbAugmentation(ConnectionInfo key) {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
        ovsdbNodeBuilder.setConnectionInfo(key);
        return ovsdbNodeBuilder.build();
    }

    public static NodeId createManagedNodeId(InstanceIdentifier<Node> iid) {
        NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
        return nodeKey.getNodeId();
    }

    /**
     * <p>
     * Representation of a southbound test case. Each test case has a name, a list of input values and a list of
     * expected values. The input values are provided to the augmentation builder, and the expected values are checked
     * against the output of the resulting augmentation.
     * </p>
     * <p>
     * Instances of this class are immutable.
     * </p>
     *
     * @param <T> The type of data used for the test case.
     */
    private static final class SouthboundTestCase<T> {
        private final String name;
        private final List<T> inputValues;
        private final List<T> expectedValues;

        /**
         * Creates an instance of a southbound test case.
         *
         * @param name The test case's name.
         * @param inputValues The input values (provided as input to the underlying augmentation builder).
         * @param expectedValues The expected values (checked against the output of the underlying augmentation).
         */
        public SouthboundTestCase(
                final String name, final List<T> inputValues, final List<T> expectedValues) {
            this.name = name;
            this.inputValues = inputValues;
            this.expectedValues = expectedValues;
        }
    }

    /**
     * Southbound test case builder.
     *
     * @param <T> The type of data used for the test case.
     */
    private static final class SouthboundTestCaseBuilder<T> {
        private String name;
        private List<T> inputValues;
        private List<T> expectedValues;

        /**
         * Creates a builder. Builders may be reused, the generated immutable instances are independent of the
         * builders. There are no default values.
         */
        public SouthboundTestCaseBuilder() {
            // Nothing to do
        }

        /**
         * Sets the test case's name.
         *
         * @param name The test case's name.
         * @return The builder.
         */
        public SouthboundTestCaseBuilder<T> name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the input values.
         *
         * @param inputValues The input values.
         * @return The builder.
         */
        @SafeVarargs
        public final SouthboundTestCaseBuilder<T> input(final T... inputValues) {
            this.inputValues = Lists.newArrayList(inputValues);
            return this;
        }

        /**
         * Sets the expected output values.
         *
         * @param expectedValues The expected output values.
         * @return The builder.
         */
        @SafeVarargs
        public final SouthboundTestCaseBuilder<T> expect(final T... expectedValues) {
            this.expectedValues = Lists.newArrayList(expectedValues);
            return this;
        }

        /**
         * Indicates that the provided input values should be expected as output values.
         *
         * @return The builder.
         */
        public SouthboundTestCaseBuilder<T> expectInputAsOutput() {
            this.expectedValues = this.inputValues;
            return this;
        }

        /**
         * Builds an immutable instance representing the test case.
         *
         * @return The test case.
         */
        @SuppressWarnings("unchecked")
        public SouthboundTestCase<T> build() {
            return new SouthboundTestCase<>(name, inputValues, expectedValues);
        }
    }
}
