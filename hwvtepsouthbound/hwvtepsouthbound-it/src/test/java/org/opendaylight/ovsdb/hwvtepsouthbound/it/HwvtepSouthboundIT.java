/*
 * Copyright © 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.it;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.utils.hwvtepsouthbound.utils.HwvtepSouthboundUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIpsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIpsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class HwvtepSouthboundIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundIT.class);

    //Constants

    public static final String CUSTOM_PROPERTIES = "etc/custom.properties";
    public static final String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    public static final String DEFAULT_SERVER_IPADDRESS = "127.0.0.1";
    public static final String SERVER_PORT = "ovsdbserver.port";
    public static final String DEFAULT_SERVER_PORT = "6640";
    public static final String CONNECTION_TYPE = "ovsdbserver.connection";
    public static final String CONNECTION_TYPE_ACTIVE = "active";
    public static final String CONNECTION_TYPE_PASSIVE = "passive";
    public static final int CONNECTION_INIT_TIMEOUT = 10000;
    public static final String OPENFLOW_CONNECTION_PROTOCOL = "tcp";
    private static final String PS_NAME = "ps0";

    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    private static final int OVSDB_ROUNDTRIP_TIMEOUT = 10000;

    private static MdsalUtils mdsalUtils = null;
    private static boolean setup = false;
    private static int testMethodsRemaining;
    private static String addressStr;
    private static Uint16 portNumber;
    private static String connectionType;
    private static Node hwvtepNode;
    @Inject @Filter(timeout = 60000)
    private static DataBroker dataBroker = null;
    @Inject
    private BundleContext bundleContext;

    private static final NotifyingDataChangeListener OPERATIONAL_LISTENER =
            new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL);

    private static final class NotifyingDataChangeListener implements DataTreeChangeListener<Node> {
        private final LogicalDatastoreType type;
        private final Set<InstanceIdentifier<Node>> createdNodes = new HashSet<>();
        private final Set<InstanceIdentifier<Node>> removedNodes = new HashSet<>();
        private final Set<InstanceIdentifier<Node>> updatedNodes = new HashSet<>();

        private NotifyingDataChangeListener(LogicalDatastoreType type) {
            this.type = type;
        }

        @Override
        public void onDataTreeChanged(List<DataTreeModification<Node>> changes) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().path();
                final DataObjectModification<Node> mod = change.getRootNode();
                switch (mod.modificationType()) {
                    case DELETE:
                        removedNodes.add(key);
                        break;
                    case SUBTREE_MODIFIED:
                        updatedNodes.add(key);
                        break;
                    case WRITE:
                        if (mod.dataBefore() == null) {
                            LOG.trace("Data added: {}", mod.dataAfter());
                            createdNodes.add(key);
                        } else {
                            updatedNodes.add(key);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled modification type " + mod.modificationType());
                }
            }
        }

        public boolean isCreated(InstanceIdentifier<Node> iid) {
            return createdNodes.remove(iid);
        }

        public boolean isRemoved(InstanceIdentifier<Node> iid) {
            return removedNodes.remove(iid);
        }

        public boolean isUpdated(InstanceIdentifier<Node> iid) {
            return updatedNodes.remove(iid);
        }
    }

    @Override
    @Configuration
    public Option[] config() {
        Option[] options = super.config();
        Option[] propertyOptions = getPropertiesOptions();
        Option[] otherOptions = getOtherOptions();
        Option[] combinedOptions = new Option[options.length + propertyOptions.length + otherOptions.length];
        System.arraycopy(options, 0, combinedOptions, 0, options.length);
        System.arraycopy(propertyOptions, 0, combinedOptions, options.length, propertyOptions.length);
        System.arraycopy(otherOptions, 0, combinedOptions, options.length + propertyOptions.length,
                otherOptions.length);
        return combinedOptions;
    }

    private static Option[] getOtherOptions() {
        return new Option[] {
                vmOption("-javaagent:../jars/org.jacoco.agent.jar=destfile=../../jacoco-it.exec"),
                keepRuntimeFolder()
        };
    }

    @Override
    public String getKarafDistro() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("hwvtepsouthbound-karaf")
                .versionAsInProject()
                .type("zip")
                .getURL();
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("hwvtepsouthbound-features")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-ovsdb-hwvtepsouthbound-test";
    }

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n"
                + "active connection : mvn -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"
                + "passive connection : mvn -Dovsdbserver.connection=passive verify\n";
    }

    @Override
    public Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                "log4j2.logger.hwvtepsouthbound-it.name",
                HwvtepSouthboundIT.class.getPackage().getName());
        option = composite(option, editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                "log4j2.logger.hwvtepsouthbound-it.level",
                LogLevel.INFO.name()));
        option = composite(option, super.getLoggingOption());
        return option;
    }

    private static Option[] getPropertiesOptions() {
        Properties props = new Properties(System.getProperties());
        String ipAddressStr = props.getProperty(SERVER_IPADDRESS, DEFAULT_SERVER_IPADDRESS);
        String portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        String connectionTypeStr = props.getProperty(CONNECTION_TYPE, CONNECTION_TYPE_ACTIVE);

        LOG.info("getPropertiesOptions: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionTypeStr, ipAddressStr, portStr);

        return new Option[] {
                editConfigurationFilePut(CUSTOM_PROPERTIES, SERVER_IPADDRESS, ipAddressStr),
                editConfigurationFilePut(CUSTOM_PROPERTIES, SERVER_PORT, portStr),
                editConfigurationFilePut(CUSTOM_PROPERTIES, CONNECTION_TYPE, connectionTypeStr),
        };
    }

    @Before
    @Override
    public void setup() throws Exception {
        if (setup) {
            LOG.info("Skipping setup, already initialized");
            return;
        }

        super.setup();

        addressStr = bundleContext.getProperty(SERVER_IPADDRESS);
        String portStr = bundleContext.getProperty(SERVER_PORT);
        try {
            portNumber = Uint16.valueOf(portStr);
        } catch (IllegalArgumentException e) {
            fail("Invalid port number " + portStr + System.lineSeparator() + usage() + e);
        }

        connectionType = bundleContext.getProperty(CONNECTION_TYPE);

        LOG.info("setUp: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portNumber);
        if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }

        mdsalUtils = new MdsalUtils(dataBroker);
        assertTrue("Did not find " + HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID.getValue(), getHwvtepTopology());
        final ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        final InstanceIdentifier<Node> iid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
        final DataTreeIdentifier<Node> treeId = DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL, iid);

        dataBroker.registerDataTreeChangeListener(treeId, OPERATIONAL_LISTENER);

        hwvtepNode = connectHwvtepNode(connectionInfo);
        // Let's count the test methods (we need to use this instead of @AfterClass on teardown() since the latter is
        // useless with pax-exam)
        for (Method method : getClass().getMethods()) {
            boolean testMethod = false;
            boolean ignoreMethod = false;
            for (Annotation annotation : method.getAnnotations()) {
                if (Test.class.equals(annotation.annotationType())) {
                    testMethod = true;
                }
                if (Ignore.class.equals(annotation.annotationType())) {
                    ignoreMethod = true;
                }
            }
            if (testMethod && !ignoreMethod) {
                testMethodsRemaining++;
            }
        }
        LOG.info("{} test methods to run", testMethodsRemaining);

        setup = true;
    }

    private static Boolean getHwvtepTopology() {
        LOG.info("getHwvtepTopology: looking for {}...", HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID.getValue());
        Boolean found = false;
        final TopologyId topologyId = HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID;
        InstanceIdentifier<Topology> path =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, new TopologyKey(topologyId));
        for (int i = 0; i < 60; i++) {
            Boolean topology = mdsalUtils.exists(LogicalDatastoreType.OPERATIONAL, path);
            if (topology) {
                LOG.info("getHwvtepTopology: found {}...", HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID.getValue());
                found = true;
                break;
            } else {
                LOG.info("getHwvtepTopology: still looking ({})...", i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while waiting for {}",
                            HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID.getValue(), e);
                }
            }
        }
        return found;
    }

    private static Node connectHwvtepNode(ConnectionInfo connectionInfo) throws InterruptedException {
        final InstanceIdentifier<Node> iid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
        Assert.assertTrue(mdsalUtils.put(LogicalDatastoreType.CONFIGURATION,
                        iid, HwvtepSouthboundUtils.createNode(connectionInfo)));
        waitForOperationalCreation(iid);
        Node node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
        Assert.assertNotNull(node);
        LOG.info("Connected to {}", HwvtepSouthboundUtils.connectionInfoToString(connectionInfo));
        return node;
    }

    private static void disconnectHwvtepNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        final InstanceIdentifier<Node> iid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
        Assert.assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, iid));
        waitForOperationalDeletion(iid);
        Node node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
        Assert.assertNull(node);
        LOG.info("Disconnected from {}", HwvtepSouthboundUtils.connectionInfoToString(connectionInfo));
    }

    private static void waitForOperationalCreation(InstanceIdentifier<Node> iid) throws InterruptedException {
        synchronized (OPERATIONAL_LISTENER) {
            long start = System.currentTimeMillis();
            LOG.info("Waiting for OPERATIONAL DataChanged creation on {}", iid);
            while (!OPERATIONAL_LISTENER.isCreated(
                    iid) && System.currentTimeMillis() - start < OVSDB_ROUNDTRIP_TIMEOUT) {
                OPERATIONAL_LISTENER.wait(OVSDB_UPDATE_TIMEOUT);
            }
            LOG.info("Woke up, waited {} for creation of {}", System.currentTimeMillis() - start, iid);
        }
    }

    private static void waitForOperationalDeletion(InstanceIdentifier<Node> iid) throws InterruptedException {
        synchronized (OPERATIONAL_LISTENER) {
            long start = System.currentTimeMillis();
            LOG.info("Waiting for OPERATIONAL DataChanged deletion on {}", iid);
            while (!OPERATIONAL_LISTENER.isRemoved(
                    iid) && System.currentTimeMillis() - start < OVSDB_ROUNDTRIP_TIMEOUT) {
                OPERATIONAL_LISTENER.wait(OVSDB_UPDATE_TIMEOUT);
            }
            LOG.info("Woke up, waited {} for deletion of {}", System.currentTimeMillis() - start, iid);
        }
    }

    private static ConnectionInfo getConnectionInfo(String ipAddressStr, Uint16 portNum) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(ipAddressStr);
        } catch (UnknownHostException e) {
            fail("Could not resolve " + ipAddressStr + ": " + e);
        }

        IpAddress address = HwvtepSouthboundMapper.createIpAddress(inetAddress);
        PortNumber port = new PortNumber(portNum);

        final ConnectionInfo connectionInfo = new ConnectionInfoBuilder()
                .setRemoteIp(address)
                .setRemotePort(port)
                .build();
        LOG.info("connectionInfo: {}", connectionInfo);
        return connectionInfo;
    }

    private static class TestPhysicalSwitch implements AutoCloseable {
        private final ConnectionInfo connectionInfo;
        private final String psName;


        TestPhysicalSwitch(final ConnectionInfo connectionInfo, String psName) {
            this(connectionInfo, psName, null, null, null, true, null, null, null);
        }

        TestPhysicalSwitch(final ConnectionInfo connectionInfo, final String name,
                        @Nullable InstanceIdentifier<Node> psIid, @Nullable NodeId psNodeId,
                        @Nullable final String description, final boolean setManagedBy,
                        @Nullable final Map<ManagementIpsKey, ManagementIps> managementIps,
                        @Nullable final Map<TunnelIpsKey, TunnelIps> tunnelIps,
                        @Nullable final Map<TunnelsKey, Tunnels> tunnels) {
            this.connectionInfo = connectionInfo;
            this.psName = name;
            NodeBuilder psNodeBuilder = new NodeBuilder();
            if (psIid == null) {
                psIid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo, new HwvtepNodeName(psName));
            }
            if (psNodeId == null) {
                psNodeId = HwvtepSouthboundMapper.createManagedNodeId(psIid);
            }
            psNodeBuilder.setNodeId(psNodeId);
            PhysicalSwitchAugmentationBuilder psAugBuilder = new PhysicalSwitchAugmentationBuilder();
            psAugBuilder.setHwvtepNodeName(new HwvtepNodeName(psName));
            if (description != null) {
                psAugBuilder.setHwvtepNodeDescription(description);
            }
            if (setManagedBy) {
                InstanceIdentifier<Node> nodePath = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
                psAugBuilder.setManagedBy(new HwvtepGlobalRef(nodePath));
            }
            psAugBuilder.setManagementIps(managementIps);
            psAugBuilder.setTunnelIps(tunnelIps);
            psAugBuilder.setTunnels(tunnels);
            psNodeBuilder.addAugmentation(psAugBuilder.build());
            LOG.debug("Built with intent to store PhysicalSwitch data {}", psAugBuilder);
            Assert.assertTrue(
                    mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, psIid, psNodeBuilder.build()));
            try {
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for bridge creation (bridge {})", psName, e);
            }
        }

        @Override
        public void close() {
            final InstanceIdentifier<Node> iid =
                            HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo, new HwvtepNodeName(psName));
            Assert.assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, iid));
            try {
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for bridge deletion (bridge {})", psName, e);
            }
        }
    }

    @After
    public void teardown() {
        testMethodsRemaining--;
        LOG.info("{} test methods remaining", testMethodsRemaining);
    }

    @Test
    public void testhwvtepsouthboundFeatureLoad() {
        Assert.assertTrue(true);
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
    public void testHwvtepTopology() throws InterruptedException {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID));

        Topology topology = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        Assert.assertNotNull("Topology could not be found in " + LogicalDatastoreType.CONFIGURATION,
                topology);

        topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, path);

        Assert.assertNotNull("Topology could not be found in " + LogicalDatastoreType.OPERATIONAL,
                topology);
    }

    @Test
    public void testAddDeleteHwvtepNode() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        // At this point we're connected, disconnect and reconnect (the connection will be removed at the very end)
        disconnectHwvtepNode(connectionInfo);
        connectHwvtepNode(connectionInfo);
    }

    @Test
    public void testAddDeletePhysicalSwitch() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        try (TestPhysicalSwitch testPSwitch = new TestPhysicalSwitch(connectionInfo, PS_NAME)) {
            PhysicalSwitchAugmentation phySwitch = getPhysicalSwitch(connectionInfo);
            Assert.assertNotNull(phySwitch);
            LOG.info("PhysicalSwitch: {}", phySwitch);
        }
    }

    private static PhysicalSwitchAugmentation getPhysicalSwitch(ConnectionInfo connectionInfo) {
        return getPhysicalSwitch(connectionInfo, PS_NAME);
    }

    private static PhysicalSwitchAugmentation getPhysicalSwitch(ConnectionInfo connectionInfo, String psName) {
        return getPhysicalSwitch(connectionInfo, psName, LogicalDatastoreType.OPERATIONAL);
    }

    private static PhysicalSwitchAugmentation getPhysicalSwitch(ConnectionInfo connectionInfo, String psName,
                    LogicalDatastoreType dataStore) {
        Node psNode = getPhysicalSwitchNode(connectionInfo, psName, dataStore);
        Assert.assertNotNull(psNode);
        PhysicalSwitchAugmentation psAugmentation = psNode.augmentation(PhysicalSwitchAugmentation.class);
        Assert.assertNotNull(psAugmentation);
        return psAugmentation;
    }

    private static Node getPhysicalSwitchNode(ConnectionInfo connectionInfo, String psName,
            LogicalDatastoreType dataStore) {
        InstanceIdentifier<Node> psIid =
                        HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo, new HwvtepNodeName(psName));
        return mdsalUtils.read(dataStore, psIid);
    }
}
