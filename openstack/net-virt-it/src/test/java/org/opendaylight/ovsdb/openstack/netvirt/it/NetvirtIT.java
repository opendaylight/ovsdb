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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
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
                editConfigurationFilePut(NetvirtITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb",
                        LogLevelOption.LogLevel.TRACE.name()),
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(NetvirtIT.class),
                        LogLevelOption.LogLevel.INFO.name()),
                //editConfigurationFilePut(NetvirtITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                //        "log4j.logger.org.opendaylight.ovsdb.lib",
                //        LogLevelOption.LogLevel.INFO.name()),
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
        return String.valueOf(connectionInfo.getRemoteIp().getValue()) + ":" + connectionInfo.getRemotePort().getValue();
    }

    private boolean addOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION,
                SouthboundMapper.createInstanceIdentifier(connectionInfo),
                SouthboundMapper.createNode(connectionInfo));
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    private Node getOvsdbNode(final ConnectionInfo connectionInfo) {
        return mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                SouthboundMapper.createInstanceIdentifier(connectionInfo));
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
        Assert.assertNotNull("Should find OVSDB node after connect", node);
        LOG.info("Connected to {}", connectionInfoToString(connectionInfo));
        return node;
    }

    private boolean disconnectOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        Assert.assertTrue(deleteOvsdbNode(connectionInfo));
        Node node = getOvsdbNode(connectionInfo);
        Assert.assertNull("Should not find OVSDB node after disconnect", node);
        //Assume.assumeNotNull(node); // Using assumeNotNull because there is no assumeNull
        LOG.info("Disconnected from {}", connectionInfoToString(connectionInfo));
        return true;
    }

    private String getControllerIPAddress() {
        String addressString = ConfigProperties.getProperty(this.getClass(), "ovsdb.controller.address");
        if (addressString != null) {
            try {
                if (InetAddress.getByName(addressString) != null) {
                    return addressString;
                }
            } catch (UnknownHostException e) {
                LOG.error("Host {} is invalid", addressString);
            }
        }

        addressString = ConfigProperties.getProperty(this.getClass(), "of.address");
        if (addressString != null) {
            try {
                if (InetAddress.getByName(addressString) != null) {
                    return addressString;
                }
            } catch (UnknownHostException e) {
                LOG.error("Host {} is invalid", addressString);
            }
        }

        return null;
    }

    private short getControllerOFPort() {
        short openFlowPort = Constants.OPENFLOW_PORT;
        String portString = ConfigProperties.getProperty(this.getClass(), "of.listenPort");
        if (portString != null) {
            try {
                openFlowPort = Short.parseShort(portString);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid port:{}, use default({})", portString,
                        openFlowPort);
            }
        }
        return openFlowPort;
    }

    private List<String> getControllersFromOvsdbNode(Node node) {
        List<String> controllersStr = new ArrayList<>();

        String controllerIpStr = getControllerIPAddress();
        if (controllerIpStr != null) {
            // If codepath makes it here, the ip address to be used was explicitly provided.
            // Being so, also fetch openflowPort provided via ConfigProperties.
            controllersStr.add(Constants.OPENFLOW_CONNECTION_PROTOCOL
                    + ":" + controllerIpStr + ":" + getControllerOFPort());
        } else {
            // Check if ovsdb node has manager entries
            OvsdbNodeAugmentation ovsdbNodeAugmentation = southbound.extractOvsdbNode(node);
            if (ovsdbNodeAugmentation != null) {
                List<ManagerEntry> managerEntries = ovsdbNodeAugmentation.getManagerEntry();
                if (managerEntries != null && !managerEntries.isEmpty()) {
                    for (ManagerEntry managerEntry : managerEntries) {
                        if (managerEntry == null || managerEntry.getTarget() == null) {
                            continue;
                        }
                        String[] tokens = managerEntry.getTarget().getValue().split(":");
                        if (tokens.length == 3 && tokens[0].equalsIgnoreCase("tcp")) {
                            controllersStr.add(Constants.OPENFLOW_CONNECTION_PROTOCOL
                                    + ":" + tokens[1] + ":" + getControllerOFPort());
                        } else if (tokens[0].equalsIgnoreCase("ptcp")) {
                            ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
                            if (connectionInfo != null && connectionInfo.getLocalIp() != null) {
                                controllerIpStr = String.valueOf(connectionInfo.getLocalIp().getValue());
                                controllersStr.add(Constants.OPENFLOW_CONNECTION_PROTOCOL
                                        + ":" + controllerIpStr + ":" + Constants.OPENFLOW_PORT);
                            } else {
                                LOG.warn("Ovsdb Node does not contain connection info: {}", node);
                            }
                        } else {
                            LOG.trace("Skipping manager entry {} for node {}",
                                    managerEntry.getTarget(), node.getNodeId().getValue());
                        }
                    }
                } else {
                    LOG.warn("Ovsdb Node does not contain manager entries : {}", node);
                }
            }
        }

        if (controllersStr.isEmpty()) {
            // Neither user provided ip nor ovsdb node has manager entries. Lets use local machine ip address.
            LOG.debug("Use local machine ip address as a OpenFlow Controller ip address");
            controllerIpStr = getLocalControllerHostIpAddress();
            if (controllerIpStr != null) {
                controllersStr.add(Constants.OPENFLOW_CONNECTION_PROTOCOL
                        + ":" + controllerIpStr + ":" + Constants.OPENFLOW_PORT);
            }
        }

        if (controllersStr.isEmpty()) {
            LOG.warn("Failed to determine OpenFlow controller ip address");
        } else if (LOG.isDebugEnabled()) {
            controllerIpStr = "";
            for (String currControllerIpStr : controllersStr) {
                controllerIpStr += " " + currControllerIpStr;
            }
            LOG.debug("Found {} OpenFlow Controller(s) :{}", controllersStr.size(), controllerIpStr);
        }

        return controllersStr;
    }

    private String getLocalControllerHostIpAddress() {
        String ipaddress = null;
        try{
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();ifaces.hasMoreElements();){
                NetworkInterface iface = ifaces.nextElement();

                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress() && inetAddr.isSiteLocalAddress()) {
                        ipaddress = inetAddr.getHostAddress();
                        break;
                    }
                }
            }
        }catch (Exception e){
            LOG.warn("Exception while fetching local host ip address ", e);
        }
        return ipaddress;
    }

    private String getControllerTarget(Node ovsdbNode) {
        return getControllersFromOvsdbNode(ovsdbNode).get(0);
    }

    @Test
    public void testAddDeleteOvsdbNode() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        connectOvsdbNode(connectionInfo);
        ControllerEntry controllerEntry;
        for (int i = 0; i < 10; i++) {
            Node ovsdbNode = getOvsdbNode(connectionInfo);
            Assert.assertNotNull("ovsdb node not found", ovsdbNode);
            String controllerTarget = getControllerTarget(ovsdbNode);
            Assert.assertNotNull("Failed to get controller target", controllerTarget);
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo, NetvirtITConstants.INTEGRATION_BRIDGE_NAME);
            Assert.assertNotNull(bridge);
            Assert.assertNotNull(bridge.getControllerEntry());
            controllerEntry = bridge.getControllerEntry().iterator().next();
            Assert.assertEquals(controllerTarget, controllerEntry.getTarget().getValue());
            if (controllerEntry.isIsConnected()) {
                Assert.assertTrue(controllerEntry.isIsConnected());
                break;
            }
            Thread.sleep(1000);
        }

        Assert.assertTrue(deleteBridge(connectionInfo, NetvirtITConstants.INTEGRATION_BRIDGE_NAME));
        Thread.sleep(1000);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Ignore
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

    /**
     * Extract the <code>store</code> type data store contents for the particular bridge identified by
     * <code>bridgeName</code>.
     *
     * @param connectionInfo The connection information.
     * @param bridgeName The bridge name.
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
     * @param connectionInfo The connection information.
     * @param bridgeName The bridge name.
     * @see <code>NetvirtIT.getBridge(ConnectionInfo, String, LogicalDatastoreType)</code>
     * @return <code>LogicalDatastoreType.OPERATIONAL</code> type data store contents
     */
    private OvsdbBridgeAugmentation getBridge(ConnectionInfo connectionInfo, String bridgeName) {
        return getBridge(connectionInfo, bridgeName, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Extract the node contents from <code>store</code> type data store for the
     * bridge identified by <code>bridgeName</code>
     *
     * @param connectionInfo The connection information.
     * @param bridgeName The bridge name.
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @return <code>store</code> type data store contents
     */
    private Node getBridgeNode(ConnectionInfo connectionInfo, String bridgeName, LogicalDatastoreType store) {
        InstanceIdentifier<Node> bridgeIid =
                SouthboundMapper.createInstanceIdentifier(connectionInfo,
                    new OvsdbBridgeName(bridgeName));
        return mdsalUtils.read(store, bridgeIid);
    }

    private boolean deleteBridge(final ConnectionInfo connectionInfo, final String bridgeName)
        throws InterruptedException {

        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
                SouthboundMapper.createInstanceIdentifier(connectionInfo,
                        new OvsdbBridgeName(bridgeName)));
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    private InstanceIdentifier<Node> getTpIid(ConnectionInfo connectionInfo, OvsdbBridgeAugmentation bridge) {
        return SouthboundMapper.createInstanceIdentifier(connectionInfo,
            bridge.getBridgeName());
    }

    private void netVirtAddPort(ConnectionInfo connectionInfo) throws InterruptedException {
        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo, NetvirtITConstants.INTEGRATION_BRIDGE_NAME);
        Assert.assertNotNull(bridge);
        NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundMapper.createInstanceIdentifier(
                connectionInfo, bridge.getBridgeName()));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        String portName = NetvirtITConstants.PORT_NAME;
        ovsdbTerminationBuilder.setName(portName);
        Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));
        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, terminationPointIid);
        Assert.assertNotNull(terminationPointNode);
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
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);
        Node ovsdbNode = connectOvsdbNode(connectionInfo);

        Thread.sleep(15000);
        // Verify the pipeline flows were installed
        PipelineOrchestrator pipelineOrchestrator =
                (PipelineOrchestrator) ServiceHelper.getGlobalInstance(PipelineOrchestrator.class, this);
        assertNotNull("Could not find PipelineOrchestrator Service", pipelineOrchestrator);
        Node bridgeNode = southbound.getBridgeNode(ovsdbNode, NetvirtITConstants.INTEGRATION_BRIDGE_NAME);
        assertNotNull("bridge " + NetvirtITConstants.INTEGRATION_BRIDGE_NAME + " was not found", bridgeNode);
        LOG.info("testNetVirt: bridgeNode: {}", bridgeNode);
        long datapathId = southbound.getDataPathId(bridgeNode);
        assertNotEquals("datapathId was not found", datapathId, 0);
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder nodeBuilder =
                FlowUtils.createNodeBuilder(datapathId);

        List<Service> staticPipeline = pipelineOrchestrator.getStaticPipeline();
        List<Service> staticPipelineFound = Lists.newArrayList();
        for (Service service : pipelineOrchestrator.getServiceRegistry().keySet()) {
            if (staticPipeline.contains(service)) {
                staticPipelineFound.add(service);
            }
            FlowBuilder flowBuilder = FlowUtils.getPipelineFlow(service.getTable(), (short)0);
            Flow flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.CONFIGURATION);
            assertNotNull("Could not find flow in config", flow);
            flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.OPERATIONAL);
            assertNotNull("Could not find flow in operational", flow);
        }
        assertEquals("did not find all expected flows in static pipeline",
                staticPipeline.size(), staticPipelineFound.size());

        netVirtAddPort(connectionInfo);
        Thread.sleep(10000);
        Assert.assertTrue(deleteBridge(connectionInfo, NetvirtITConstants.INTEGRATION_BRIDGE_NAME));
        Thread.sleep(10000);
        Assert.assertTrue(disconnectOvsdbNode(connectionInfo));
    }

    @Ignore
    @Test
    public void testNetVirt2() throws InterruptedException {
        Thread.sleep(60000);
    }

    @Ignore
    @Test
    public void testReadOvsdbTopologyNodes() throws InterruptedException {
        Thread.sleep(10000);
        List<Node> ovsdbNodes = southbound.readOvsdbTopologyNodes();
        for (Node node : ovsdbNodes) {
            LOG.info(">>>>> node: {}", node);
        }
    }

    private Flow getFlow (
            FlowBuilder flowBuilder,
            org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder nodeBuilder,
            LogicalDatastoreType store)
            throws InterruptedException {

        Flow flow = null;
        for (int i = 0; i < 10; i++) {
            flow = FlowUtils.getFlow(flowBuilder, nodeBuilder, dataBroker.newReadOnlyTransaction(), store);
            if (flow != null) {
                LOG.info("getFlow: flow({}): {}", store, flow);
                break;
            }
            Thread.sleep(1000);
        }
        return flow;
    }
}
