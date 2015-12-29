/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperties;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSecurityGroupCRUD;
import org.opendaylight.neutron.spi.INeutronSecurityRuleCRUD;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.openstack.netvirt.NetworkHandler;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for netvirt
 *
 * @author Sam Hague (shague@redhat.com)
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NetvirtIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtIT.class);
    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    private static DataBroker dataBroker = null;
    private static String addressStr;
    private static String portStr;
    private static String connectionType;
    private static String controllerStr;
    private static AtomicBoolean setup = new AtomicBoolean(false);
    private static MdsalUtils mdsalUtils = null;
    private static Southbound southbound = null;
    private static SouthboundUtils southboundUtils;
    private static NeutronUtils neutronUtils = new NeutronUtils();
    private static final String NETVIRT_TOPOLOGY_ID = "netvirt:1";

    @Override
    public String getModuleName() {
        return "netvirt-providers-impl";
    }

    @Override
    public String getInstanceName() {
        return "netvirt-providers-default";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("features-ovsdb")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-ovsdb-openstack";
    }

    @Configuration
    @Override
    public Option[] config() {
        Option[] parentOptions = super.config();
        Option[] propertiesOptions = getPropertiesOptions();
        Option[] otherOptions = getOtherOptions();
        Option[] options = new Option[parentOptions.length + propertiesOptions.length + otherOptions.length];
        System.arraycopy(parentOptions, 0, options, 0, parentOptions.length);
        System.arraycopy(propertiesOptions, 0, options, parentOptions.length, propertiesOptions.length);
        System.arraycopy(otherOptions, 0, options, parentOptions.length + propertiesOptions.length,
                otherOptions.length);
        return options;
    }

    private Option[] getOtherOptions() {
        return new Option[] {
                wrappedBundle(
                        mavenBundle("org.opendaylight.ovsdb", "utils.mdsal-openflow")
                                .version(asInProject())
                                .type("jar")),
                wrappedBundle(
                        mavenBundle("org.opendaylight.ovsdb", "utils.config")
                                .version(asInProject())
                                .type("jar")),
                configureConsole().startLocalConsole(),
                vmOption("-javaagent:../jars/org.jacoco.agent.jar=destfile=../../jacoco-it.exec"),
                keepRuntimeFolder()
        };
    }

    public Option[] getPropertiesOptions() {
        return new Option[] {
                propagateSystemProperties(NetvirtITConstants.SERVER_IPADDRESS,
                        NetvirtITConstants.SERVER_PORT, NetvirtITConstants.CONNECTION_TYPE,
                        NetvirtITConstants.CONTROLLER_IPADDRESS,
                        NetvirtITConstants.USERSPACE_ENABLED)
        };
    }

    @Override
    public Option getLoggingOption() {
        return composite(
                //editConfigurationFilePut(NetvirtITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                //        "log4j.logger.org.opendaylight.controller",
                //        LogLevelOption.LogLevel.TRACE.name()),
                editConfigurationFilePut(NetvirtITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb",
                        LogLevelOption.LogLevel.TRACE.name()),
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(NetvirtIT.class),
                        LogLevelOption.LogLevel.INFO.name()),
                editConfigurationFilePut(NetvirtITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb.lib",
                        LogLevelOption.LogLevel.INFO.name()),
                editConfigurationFilePut(NetvirtITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.openflowjava",
                        LogLevelOption.LogLevel.INFO.name()),
                editConfigurationFilePut(NetvirtITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.openflowplugin",
                        LogLevelOption.LogLevel.INFO.name()),
                super.getLoggingOption());
    }

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n"
                + "active connection : mvn -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"
                + "passive connection : mvn -Dovsdbserver.connection=passive verify\n";
    }

    private void getProperties() {
        Properties props = System.getProperties();
        addressStr = props.getProperty(NetvirtITConstants.SERVER_IPADDRESS);
        portStr = props.getProperty(NetvirtITConstants.SERVER_PORT, NetvirtITConstants.DEFAULT_SERVER_PORT);
        connectionType = props.getProperty(NetvirtITConstants.CONNECTION_TYPE, "active");
        controllerStr = props.getProperty(NetvirtITConstants.CONTROLLER_IPADDRESS, "0.0.0.0");
        String userSpaceEnabled = props.getProperty(NetvirtITConstants.USERSPACE_ENABLED, "no");
        LOG.info("setUp: Using the following properties: mode= {}, ip:port= {}:{}, controller ip: {}, " +
                "userspace.enabled: {}",
                connectionType, addressStr, portStr, controllerStr, userSpaceEnabled);
        if (connectionType.equalsIgnoreCase(NetvirtITConstants.CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }
    }

    @Before
    @Override
    public void setup() throws InterruptedException {
        if (setup.get()) {
            LOG.info("Skipping setUp, already initialized");
            return;
        }

        try {
            super.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }

        getProperties();

        if (connectionType.equalsIgnoreCase(NetvirtITConstants.CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }

        dataBroker = getDatabroker(getProviderContext());
        mdsalUtils = new MdsalUtils(dataBroker);
        assertNotNull("mdsalUtils should not be null", mdsalUtils);
        assertTrue("Did not find " + NETVIRT_TOPOLOGY_ID, getNetvirtTopology());
        southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        assertNotNull("southbound should not be null", southbound);
        southboundUtils = new SouthboundUtils(mdsalUtils);
        setup.set(true);
    }

    private BindingAwareBroker.ProviderContext getProviderContext() {
        BindingAwareBroker.ProviderContext providerContext = null;
        for (int i=0; i < 60; i++) {
            providerContext = getSession();
            if (providerContext != null) {
                break;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        assertNotNull("providercontext should not be null", providerContext);
        /* One more second to let the provider finish initialization */
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return providerContext;
    }

    private DataBroker getDatabroker(BindingAwareBroker.ProviderContext providerContext) {
        DataBroker dataBroker = providerContext.getSALService(DataBroker.class);
        assertNotNull("dataBroker should not be null", dataBroker);
        return dataBroker;
    }

    private Boolean getNetvirtTopology() {
        LOG.info("getNetvirtTopology: looking for {}...", NETVIRT_TOPOLOGY_ID);
        Boolean found = false;
        final TopologyId topologyId = new TopologyId(new Uri(NETVIRT_TOPOLOGY_ID));
        InstanceIdentifier<Topology> path =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, new TopologyKey(topologyId));
        for (int i = 0; i < 60; i++) {
            Topology topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, path);
            if (topology != null) {
                LOG.info("getNetvirtTopology: found {}...", NETVIRT_TOPOLOGY_ID);
                found = true;
                break;
            } else {
                LOG.info("getNetvirtTopology: still looking ({})...", i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return found;
    }

    /**
     * Test passive connection mode. The southbound starts in a listening mode waiting for connections on port
     * 6640. This test will wait for incoming connections for {@link NetvirtITConstants#CONNECTION_INIT_TIMEOUT} ms.
     *
     * @throws InterruptedException
     */
    @Ignore
    @Test
    public void testPassiveNode() throws InterruptedException {
        if (connectionType.equalsIgnoreCase(NetvirtITConstants.CONNECTION_TYPE_PASSIVE)) {
            //Wait for CONNECTION_INIT_TIMEOUT for the Passive connection to be initiated by the ovsdb-server.
            Thread.sleep(NetvirtITConstants.CONNECTION_INIT_TIMEOUT);
        }
    }

    private Node connectOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        LOG.info("connectOvsdbNode enter");
        Assert.assertTrue(southboundUtils.addOvsdbNode(connectionInfo));
        Node node = southboundUtils.getOvsdbNode(connectionInfo);
        Assert.assertNotNull("Should find OVSDB node after connect", node);
        LOG.info("Connected to {}", SouthboundUtils.connectionInfoToString(connectionInfo));
        return node;
    }

    private boolean disconnectOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        LOG.info("disconnectOvsdbNode enter");
        Assert.assertTrue(southboundUtils.deleteOvsdbNode(connectionInfo));
        Node node = southboundUtils.getOvsdbNode(connectionInfo);
        Assert.assertNull("Should not find OVSDB node after disconnect", node);
        LOG.info("Disconnected from {}", SouthboundUtils.connectionInfoToString(connectionInfo));
        return true;
    }

    @Test
    public void testAddDeleteOvsdbNode() throws InterruptedException {
        LOG.info("testAddDeleteOvsdbNode enter");
        ConnectionInfo connectionInfo = SouthboundUtils.getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);

        assertTrue("Controller " + SouthboundUtils.connectionInfoToString(connectionInfo)
                + " is not connected", isControllerConnected(connectionInfo));

        Assert.assertTrue(southboundUtils.deleteBridge(connectionInfo, NetvirtITConstants.INTEGRATION_BRIDGE_NAME));
        Thread.sleep(1000);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
        LOG.info("testAddDeleteOvsdbNode exit");
    }

    private boolean isControllerConnected(ConnectionInfo connectionInfo) throws InterruptedException {
        LOG.info("isControllerConnected enter");
        Boolean connected = false;
        ControllerEntry controllerEntry;
        Node ovsdbNode = southboundUtils.getOvsdbNode(connectionInfo);
        assertNotNull("ovsdb node not found", ovsdbNode);

        BridgeConfigurationManager bridgeConfigurationManager =
                (BridgeConfigurationManager) ServiceHelper.getGlobalInstance(BridgeConfigurationManager.class, this);
        assertNotNull("Could not find PipelineOrchestrator Service", bridgeConfigurationManager);
        String controllerTarget = bridgeConfigurationManager.getControllersFromOvsdbNode(ovsdbNode).get(0);
        Assert.assertNotNull("Failed to get controller target", controllerTarget);

        for (int i = 0; i < 10; i++) {
            LOG.info("isControllerConnected try {}: looking for controller: {}", i, controllerTarget);
            OvsdbBridgeAugmentation bridge =
                    southboundUtils.getBridge(connectionInfo, NetvirtITConstants.INTEGRATION_BRIDGE_NAME);
            Assert.assertNotNull(bridge);
            Assert.assertNotNull(bridge.getControllerEntry());
            controllerEntry = bridge.getControllerEntry().iterator().next();
            Assert.assertEquals(controllerTarget, controllerEntry.getTarget().getValue());
            if (controllerEntry.isIsConnected()) {
                Assert.assertTrue("Controller is not connected", controllerEntry.isIsConnected());
                connected = true;
                break;
            }
            Thread.sleep(1000);
        }
        LOG.info("isControllerConnected exit: {} - {}", connected, controllerTarget);
        return connected;
    }

    @Ignore
    @Test
    public void testOpenVSwitchOtherConfig() throws InterruptedException {
        ConnectionInfo connectionInfo = SouthboundUtils.getConnectionInfo(addressStr, portStr);
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

    /**
     * Test for basic southbound events to netvirt.
     * <pre>The test will:
     * - connect to an OVSDB node and verify it is added to operational
     * - then verify that br-int was created on the node and stored in operational
     * - a port is then added to the bridge to verify that it is ignored by netvirt
     * - remove the bridge
     * - remove the node and verify it is not in operational
     * </pre>
     * @throws InterruptedException
     */
    @Test
    public void testNetVirt() throws InterruptedException {
        LOG.info("testNetVirt: starting test");
        ConnectionInfo connectionInfo = SouthboundUtils.getConnectionInfo(addressStr, portStr);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);
        LOG.info("testNetVirt: should be connected");

        //TODO use controller value rather that ovsdb connectionInfo or change log
        assertTrue("Controller " + SouthboundUtils.connectionInfoToString(connectionInfo)
                + " is not connected", isControllerConnected(connectionInfo));

        // Verify the pipeline flows were installed
        PipelineOrchestrator pipelineOrchestrator =
                (PipelineOrchestrator) ServiceHelper.getGlobalInstance(PipelineOrchestrator.class, this);
        assertNotNull("Could not find PipelineOrchestrator Service", pipelineOrchestrator);
        Node bridgeNode = southbound.getBridgeNode(ovsdbNode, NetvirtITConstants.INTEGRATION_BRIDGE_NAME);
        assertNotNull("bridge " + NetvirtITConstants.INTEGRATION_BRIDGE_NAME + " was not found", bridgeNode);
        LOG.info("testNetVirt: bridgeNode: {}", bridgeNode);
        long datapathId = southbound.getDataPathId(bridgeNode);
        assertNotEquals("datapathId was not found", datapathId, 0);

        List<Service> staticPipeline = pipelineOrchestrator.getStaticPipeline();
        List<Service> staticPipelineFound = Lists.newArrayList();
        for (Service service : pipelineOrchestrator.getServiceRegistry().keySet()) {
            if (staticPipeline.contains(service)) {
                staticPipelineFound.add(service);
            }
            String flowId = "DEFAULT_PIPELINE_FLOW_" + service.getTable();
            verifyFlow(datapathId, flowId, service.getTable());
        }
        assertEquals("did not find all expected flows in static pipeline",
                staticPipeline.size(), staticPipelineFound.size());

        southboundUtils.addTerminationPoint(bridgeNode, NetvirtITConstants.PORT_NAME, "internal", null, null, 0L);
        Thread.sleep(1000);
        OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                southbound.getTerminationPointOfBridge(bridgeNode, NetvirtITConstants.PORT_NAME);
        Assert.assertNotNull("Did not find " + NetvirtITConstants.PORT_NAME, ovsdbTerminationPointAugmentation);
        Thread.sleep(1000);
        Assert.assertTrue(southboundUtils.deleteBridge(connectionInfo, NetvirtITConstants.INTEGRATION_BRIDGE_NAME));
        Thread.sleep(1000);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testNetVirtFixedSG() throws InterruptedException {
        final Version minSGOvsVersion = Version.fromString("1.10.2");
        final String portName = "sg1";
        final String networkId = "521e29d6-67b8-4b3c-8633-027d21195111";
        final String tenantId = "521e29d6-67b8-4b3c-8633-027d21195100";
        final String subnetId = "521e29d6-67b8-4b3c-8633-027d21195112";
        final String portId = "521e29d6-67b8-4b3c-8633-027d21195113";
        final String dhcpPortId ="521e29d6-67b8-4b3c-8633-027d21195115";

        ConnectionInfo connectionInfo = SouthboundUtils.getConnectionInfo(addressStr, portStr);
        assertNotNull("connection failed", southboundUtils.connectOvsdbNode(connectionInfo));
        Node ovsdbNode = connectOvsdbNode(connectionInfo);
        assertNotNull("node is not connected", ovsdbNode);

        // Verify the minimum version required for this test
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        Assert.assertNotNull(ovsdbNodeAugmentation);
        assertNotNull(ovsdbNodeAugmentation.getOvsVersion());
        String ovsVersion = ovsdbNodeAugmentation.getOvsVersion();
        Version version = Version.fromString(ovsVersion);
        if (version.compareTo(minSGOvsVersion) < 0) {
            LOG.warn("{} minimum version is required", minSGOvsVersion);
            Assert.assertTrue(southboundUtils.deleteBridge(connectionInfo,
                    NetvirtITConstants.INTEGRATION_BRIDGE_NAME));
            Thread.sleep(1000);
            Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
            return;
        }

        assertTrue("Controller " + SouthboundUtils.connectionInfoToString(connectionInfo)
                + " is not connected", isControllerConnected(connectionInfo));

        Node bridgeNode = southbound.getBridgeNode(ovsdbNode, NetvirtITConstants.INTEGRATION_BRIDGE_NAME);
        assertNotNull("bridge " + NetvirtITConstants.INTEGRATION_BRIDGE_NAME + " was not found", bridgeNode);
        long datapathId = southbound.getDataPathId(bridgeNode);
        assertNotEquals("datapathId was not found", datapathId, 0);

        NeutronNetwork nn = neutronUtils.createNeutronNetwork(networkId, tenantId,
                NetworkHandler.NETWORK_TYPE_VXLAN, "100");
        NeutronSubnet ns = neutronUtils.createNeutronSubnet(subnetId, tenantId, networkId, "10.0.0.0/24");
        NeutronPort nport = neutronUtils.createNeutronPort(networkId, subnetId, portId,
                "compute", "10.0.0.10", "f6:00:00:0f:00:01");
        NeutronPort dhcp = neutronUtils.createNeutronPort(networkId, subnetId, dhcpPortId,
                "dhcp", "10.0.0.1", "f6:00:00:0f:00:02");

        Thread.sleep(1000);
        Map<String, String> externalIds = Maps.newHashMap();
        externalIds.put("attached-mac", "f6:00:00:0f:00:01");
        externalIds.put("iface-id", portId);
        southboundUtils.addTerminationPoint(bridgeNode, portName, "internal", null, externalIds, 3L);
        southboundUtils.addTerminationPoint(bridgeNode, "vm1", "internal", null, null, 0L);
        southboundUtils.addTerminationPoint(bridgeNode, "vm2", "internal", null, null, 0L);
        Map<String, String> options = Maps.newHashMap();
        options.put("key", "flow");
        options.put("remote_ip", "192.168.120.32");
        southboundUtils.addTerminationPoint(bridgeNode, "vx", "vxlan", options, null, 4L);
        Thread.sleep(1000);

        String flowId = "Egress_DHCP_Client"  + "_Permit_";
        verifyFlow(datapathId, flowId, Service.EGRESS_ACL.getTable());

        testDefaultSG(nport, datapathId, nn, tenantId, portId);
        Thread.sleep(1000);
        Assert.assertTrue(southboundUtils.deleteBridge(connectionInfo, NetvirtITConstants.INTEGRATION_BRIDGE_NAME));
        Thread.sleep(1000);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    private void testDefaultSG(NeutronPort nport, long datapathId, NeutronNetwork nn, String tenantId, String portId)
            throws InterruptedException {
        INeutronSecurityGroupCRUD ineutronSecurityGroupCRUD =
                (INeutronSecurityGroupCRUD) ServiceHelper.getGlobalInstance(INeutronSecurityGroupCRUD.class, this);
        assertNotNull("Could not find ineutronSecurityGroupCRUD Service", ineutronSecurityGroupCRUD);
        INeutronSecurityRuleCRUD ineutronSecurityRuleCRUD =
                (INeutronSecurityRuleCRUD) ServiceHelper.getGlobalInstance(INeutronSecurityRuleCRUD.class, this);
        assertNotNull("Could not find ineutronSecurityRuleCRUD Service", ineutronSecurityRuleCRUD);

        NeutronSecurityGroup neutronSG = new NeutronSecurityGroup();
        neutronSG.setSecurityGroupDescription("testig defaultSG-IT");
        neutronSG.setSecurityGroupName("DefaultSG");
        neutronSG.setSecurityGroupUUID("d3329053-bae5-4bf4-a2d1-7330f11ba5db");
        neutronSG.setTenantID(tenantId);

        List<NeutronSecurityRule> nsrs = new ArrayList<>();
        NeutronSecurityRule nsrIN = new NeutronSecurityRule();
        nsrIN.setSecurityRemoteGroupID(null);
        nsrIN.setSecurityRuleDirection("ingress");
        nsrIN.setSecurityRuleEthertype("IPv4");
        nsrIN.setSecurityRuleGroupID("d3329053-bae5-4bf4-a2d1-7330f11ba5db");
        nsrIN.setSecurityRuleProtocol("TCP");
        nsrIN.setSecurityRuleRemoteIpPrefix("10.0.0.0/24");
        nsrIN.setSecurityRuleUUID("823faaf7-175d-4f01-a271-0bf56fb1e7e6");
        nsrIN.setTenantID(tenantId);

        NeutronSecurityRule nsrEG = new NeutronSecurityRule();
        nsrEG.setSecurityRemoteGroupID(null);
        nsrEG.setSecurityRuleDirection("egress");
        nsrEG.setSecurityRuleEthertype("IPv4");
        nsrEG.setSecurityRuleGroupID("d3329053-bae5-4bf4-a2d1-7330f11ba5db");
        nsrEG.setSecurityRuleProtocol("TCP");
        nsrEG.setSecurityRuleRemoteIpPrefix("10.0.0.0/24");
        nsrEG.setSecurityRuleUUID("823faaf7-175d-4f01-a271-0bf56fb1e7e1");
        nsrEG.setTenantID(tenantId);

        nsrs.add(nsrIN);
        nsrs.add(nsrEG);

        neutronSG.setSecurityRules(nsrs);
        ineutronSecurityRuleCRUD.addNeutronSecurityRule(nsrIN);
        ineutronSecurityRuleCRUD.addNeutronSecurityRule(nsrEG);
        ineutronSecurityGroupCRUD.add(neutronSG);

        List<NeutronSecurityGroup> sgs = new ArrayList<>();
        sgs.add(neutronSG);
        nport.setSecurityGroups(sgs);

        INeutronPortCRUD iNeutronPortCRUD =
                (INeutronPortCRUD) ServiceHelper.getGlobalInstance(INeutronPortCRUD.class, this);
        iNeutronPortCRUD.update(portId, nport);

        Thread.sleep(10000);
        String flowId = "Egress_IP" + nn.getProviderSegmentationID() + "_" + nport.getMacAddress() + "_Permit_";
        verifyFlow(datapathId, flowId, Service.EGRESS_ACL.getTable());

        flowId = "Ingress_IP" + nn.getProviderSegmentationID() + "_" + nport.getMacAddress() + "_Permit_";
        verifyFlow(datapathId, flowId, Service.INGRESS_ACL.getTable());
    }

    private Flow getFlow (
            FlowBuilder flowBuilder,
            org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder nodeBuilder,
            LogicalDatastoreType store) throws InterruptedException {

        Flow flow = null;
        for (int i = 0; i < 10; i++) {
            LOG.info("getFlow try {} from {}: looking for flow: {}, node: {}",
                    i, store, flowBuilder.build(), nodeBuilder.build());
            flow = FlowUtils.getFlow(flowBuilder, nodeBuilder, dataBroker.newReadOnlyTransaction(), store);
            if (flow != null) {
                LOG.info("getFlow try {} from {}: found flow: {}", i, store, flow);
                break;
            }
            Thread.sleep(1000);
        }
        return flow;
    }

    private void verifyFlow(long datapathId, String flowId, short table) throws InterruptedException {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder nodeBuilder =
                FlowUtils.createNodeBuilder(datapathId);
        FlowBuilder flowBuilder =
                FlowUtils.initFlowBuilder(new FlowBuilder(), flowId, table);
        Flow flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.CONFIGURATION);
        assertNotNull("Could not find flow in config: " + flowBuilder.build() + "--" + nodeBuilder.build(), flow);
        flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.OPERATIONAL);
        assertNotNull("Could not find flow in operational: " + flowBuilder.build() + "--" + nodeBuilder.build(), flow);
    }
}
