/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.it;

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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundProvider;
import org.opendaylight.ovsdb.utils.hwvtepsouthbound.utils.HwvtepSouthboundUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class HwvtepSouthboundIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundIT.class);

    //Constants

    public static final String ORG_OPS4J_PAX_LOGGING_CFG = "etc/org.ops4j.pax.logging.cfg";
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

    private static DataBroker dataBroker = null;
    private static MdsalUtils mdsalUtils = null;
    private static HwvtepSouthboundUtils hwvtepUtils = null;
    private static boolean setup = false;
    private static int testMethodsRemaining;
    private static String addressStr;
    private static int portNumber;
    private static String connectionType;
    private static Node hwvtepNode;
    private InstanceIdentifier<Node> iid;

    @Inject
    private BundleContext bundleContext;

    private static final NotifyingDataChangeListener OPERATIONAL_LISTENER =
            new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL);

    private static class NotifyingDataChangeListener implements DataTreeChangeListener<Node> {
        private final LogicalDatastoreType type;
        private final Set<InstanceIdentifier<Node>> createdNodes = new HashSet<>();
        private final Set<InstanceIdentifier<Node>> removedNodes = new HashSet<>();
        private final Set<InstanceIdentifier<Node>> updatedNodes = new HashSet<>();

        private NotifyingDataChangeListener(LogicalDatastoreType type) {
            this.type = type;
        }

        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                    switch (mod.getModificationType()) {
                    case DELETE:
                        removedNodes.add(key);
                        break;
                    case SUBTREE_MODIFIED:
                        updatedNodes.add(key);
                        break;
                    case WRITE:
                        if (mod.getDataBefore() == null) {
                            LOG.trace("Data added: {}", mod.getDataAfter());
                            createdNodes.add(key);
                        } else {
                            updatedNodes.add(key);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
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

    private Option[] getOtherOptions() {
        return new Option[] {
                vmOption("-javaagent:../jars/org.jacoco.agent.jar=destfile=../../jacoco-it.exec"),
                keepRuntimeFolder()
        };
    }

    @Override
    public String getModuleName() {
        return "hwvtepsouthbound";
    }

    @Override
    public String getInstanceName() {
        return "hwvtepsouthbound-default";
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

    @Override
    public Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                logConfiguration(HwvtepSouthboundIT.class),
                LogLevel.INFO.name());
        option = composite(option, super.getLoggingOption());
        return option;
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

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n"
                + "active connection : mvn -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"
                + "passive connection : mvn -Dovsdbserver.connection=passive verify\n";
    }

    private Option[] getPropertiesOptions() {
        Properties props = new Properties(System.getProperties());
        String addressStr = props.getProperty(SERVER_IPADDRESS, DEFAULT_SERVER_IPADDRESS);
        String portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(CONNECTION_TYPE, CONNECTION_TYPE_ACTIVE);

        LOG.info("getPropertiesOptions: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);

        return new Option[] {
                editConfigurationFilePut(CUSTOM_PROPERTIES, SERVER_IPADDRESS, addressStr),
                editConfigurationFilePut(CUSTOM_PROPERTIES, SERVER_PORT, portStr),
                editConfigurationFilePut(CUSTOM_PROPERTIES, CONNECTION_TYPE, connectionType),
        };
    }

    @Before
    @Override
    public void setup() throws InterruptedException {
        if (setup) {
            LOG.info("Skipping setup, already initialized");
            return;
        }

        try {
            super.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //dataBroker = getSession().getSALService(DataBroker.class);
        Thread.sleep(3000);
        dataBroker = HwvtepSouthboundProvider.getDb();
        Assert.assertNotNull("db should not be null", dataBroker);

        addressStr = bundleContext.getProperty(SERVER_IPADDRESS);
        String portStr = bundleContext.getProperty(SERVER_PORT);
        try {
            portNumber = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            fail("Invalid port number " + portStr + System.lineSeparator() + usage());
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
        hwvtepUtils =  new HwvtepSouthboundUtils(mdsalUtils);
        final ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        iid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
        final DataTreeIdentifier<Node> treeId =
                        new DataTreeIdentifier<Node>(LogicalDatastoreType.OPERATIONAL, iid);

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

    private Node connectHwvtepNode(ConnectionInfo connectionInfo) throws InterruptedException {
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

    private void waitForOperationalCreation(InstanceIdentifier<Node> iid) throws InterruptedException {
        synchronized (OPERATIONAL_LISTENER) {
            long _start = System.currentTimeMillis();
            LOG.info("Waiting for OPERATIONAL DataChanged creation on {}", iid);
            while (!OPERATIONAL_LISTENER.isCreated(
                    iid) && (System.currentTimeMillis() - _start) < OVSDB_ROUNDTRIP_TIMEOUT) {
                OPERATIONAL_LISTENER.wait(OVSDB_UPDATE_TIMEOUT);
            }
            LOG.info("Woke up, waited {} for creation of {}", (System.currentTimeMillis() - _start), iid);
        }
    }

    private static void waitForOperationalDeletion(InstanceIdentifier<Node> iid) throws InterruptedException {
        synchronized (OPERATIONAL_LISTENER) {
            long _start = System.currentTimeMillis();
            LOG.info("Waiting for OPERATIONAL DataChanged deletion on {}", iid);
            while (!OPERATIONAL_LISTENER.isRemoved(
                    iid) && (System.currentTimeMillis() - _start) < OVSDB_ROUNDTRIP_TIMEOUT) {
                OPERATIONAL_LISTENER.wait(OVSDB_UPDATE_TIMEOUT);
            }
            LOG.info("Woke up, waited {} for deletion of {}", (System.currentTimeMillis() - _start), iid);
        }
    }

    private static void waitForOperationalUpdation(InstanceIdentifier<Node> iid) throws InterruptedException {
        synchronized (OPERATIONAL_LISTENER) {
            long _start = System.currentTimeMillis();
            LOG.info("Waiting for OPERATIONAL DataChanged updation on {}", iid);
            while (!OPERATIONAL_LISTENER.isUpdated(iid)
                    && (System.currentTimeMillis() - _start) < OVSDB_ROUNDTRIP_TIMEOUT) {
                OPERATIONAL_LISTENER.wait(OVSDB_UPDATE_TIMEOUT);
            }
            LOG.info("Woke up, waited {} for updation of {}", (System.currentTimeMillis() - _start), iid);
        }
    }

    private ConnectionInfo getConnectionInfo(String addressStr, int portNumber) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(addressStr);
        } catch (UnknownHostException e) {
            fail("Could not resolve " + addressStr + ": " + e);
        }

        IpAddress address = HwvtepSouthboundMapper.createIpAddress(inetAddress);
        PortNumber port = new PortNumber(portNumber);

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


        public TestPhysicalSwitch(final ConnectionInfo connectionInfo, String psName) {
            this(connectionInfo, psName, null, null, null, true, null, null, null);
        }

        public TestPhysicalSwitch (final ConnectionInfo connectionInfo, final String name,
                        @Nullable InstanceIdentifier<Node> psIid, @Nullable NodeId psNodeId,
                        @Nullable final String description, final boolean setManagedBy,
                        @Nullable final List<ManagementIps> managementIps,
                        @Nullable final List<TunnelIps> tunnelIps,
                        @Nullable final List<Tunnels> tunnels) {
            this.connectionInfo = connectionInfo;
            this.psName = name;
            NodeBuilder psNodeBuilder = new NodeBuilder();
            if(psIid == null) {
                psIid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo, new HwvtepNodeName(psName));
            }
            if(psNodeId == null) {
                psNodeId = HwvtepSouthboundMapper.createManagedNodeId(psIid);
            }
            psNodeBuilder.setNodeId(psNodeId);
            PhysicalSwitchAugmentationBuilder psAugBuilder = new PhysicalSwitchAugmentationBuilder();
            psAugBuilder.setHwvtepNodeName(new HwvtepNodeName(psName));
            if(description != null) {
                psAugBuilder.setHwvtepNodeDescription(description);
            }
            if(setManagedBy) {
                InstanceIdentifier<Node> nodePath = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
                psAugBuilder.setManagedBy(new HwvtepGlobalRef(nodePath));
            }
            psAugBuilder.setManagementIps(managementIps);
            psAugBuilder.setTunnelIps(tunnelIps);
            psAugBuilder.setTunnels(tunnels);
            psNodeBuilder.addAugmentation(PhysicalSwitchAugmentation.class, psAugBuilder.build());
            LOG.debug("Built with intent to store PhysicalSwitch data {}", psAugBuilder.toString());
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
            PhysicalSwitchAugmentation pSwitch = getPhysicalSwitch(connectionInfo);
            Assert.assertNotNull(pSwitch);
            LOG.info("PhysicalSwitch: {}", pSwitch);
        }
    }

    private PhysicalSwitchAugmentation getPhysicalSwitch(ConnectionInfo connectionInfo) {
        return getPhysicalSwitch(connectionInfo, PS_NAME);
    }

    private PhysicalSwitchAugmentation getPhysicalSwitch(ConnectionInfo connectionInfo, String psName) {
        return getPhysicalSwitch(connectionInfo, psName, LogicalDatastoreType.OPERATIONAL);
    }

    private PhysicalSwitchAugmentation getPhysicalSwitch(ConnectionInfo connectionInfo, String psName,
                    LogicalDatastoreType dataStore) {
        Node psNode = getPhysicalSwitchNode(connectionInfo, psName, dataStore);
        Assert.assertNotNull(psNode);
        PhysicalSwitchAugmentation psAugmentation = psNode.getAugmentation(PhysicalSwitchAugmentation.class);
        Assert.assertNotNull(psAugmentation);
        return psAugmentation;
    }

    private Node getPhysicalSwitchNode(ConnectionInfo connectionInfo, String psName, LogicalDatastoreType dataStore) {
        InstanceIdentifier<Node> psIid =
                        HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo, new HwvtepNodeName(psName));
                return mdsalUtils.read(dataStore, psIid);
    }

    @Test
    public void testAddDeleteLogicalSwitch() throws InterruptedException {
        LOG.info("Start testing method testAddDeleteLogicalSwitch.");
        String lsName1 = "ls1";
        NodeId nodeId = hwvtepNode.getNodeId();

        addAndVerifyLogicalSwitch(nodeId, HwvtepSouthboundUtils.createLogicalSwitch(lsName1, "ls1desc", "1000"));
        deleteAndVerifyLogicalSwitch(nodeId, lsName1);
        LOG.info("End testing method testAddDeleteLogicalSwitch.");
    }

    private void addAndVerifyLogicalSwitch(NodeId nodeId, LogicalSwitches logicalSwitch) throws InterruptedException {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        HwvtepSouthboundUtils.putLogicalSwitch(transaction, nodeId, logicalSwitch);
        submitTransaction(transaction);
        waitForOperationalUpdation(iid);

        verifyGetLogicalSwitch(nodeId, logicalSwitch.getHwvtepNodeName().getValue());
    }

    @Test
    public void testAddDeleteLogicalSwitches() throws InterruptedException {
        LOG.info("Start testing method testAddDeleteLogicalSwitches.");
        NodeId nodeId = hwvtepNode.getNodeId();

        String lsName1 = "ls11";
        String lsName2 = "ls12";
        String lsName3 = "ls13";
        LogicalSwitches ls1 = HwvtepSouthboundUtils.createLogicalSwitch(lsName1, "ls11desc1", "1100");
        LogicalSwitches ls2 = HwvtepSouthboundUtils.createLogicalSwitch(lsName2, "ls12desc2", "1200");
        LogicalSwitches ls3 = HwvtepSouthboundUtils.createLogicalSwitch(lsName3, "ls13desc3", "1300");

        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        HwvtepSouthboundUtils.putLogicalSwitches(transaction, nodeId, Lists.newArrayList(ls1, ls2, ls3));
        submitTransaction(transaction);
        waitForOperationalUpdation(iid);

        verifyGetLogicalSwitch(nodeId, lsName1);
        verifyGetLogicalSwitch(nodeId, lsName2);
        verifyGetLogicalSwitch(nodeId, lsName3);

        deleteAndVerifyLogicalSwitch(nodeId, lsName1);
        deleteAndVerifyLogicalSwitch(nodeId, lsName2);
        deleteAndVerifyLogicalSwitch(nodeId, lsName3);
        LOG.info("End testing method testAddDeleteLogicalSwitches.");
    }

    private void deleteAndVerifyLogicalSwitch(NodeId nodeId, String lsName) throws InterruptedException {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        LOG.info("Deleting LogicalSwitch : name: {}", lsName);
        HwvtepSouthboundUtils.deleteLogicalSwitch(transaction, nodeId, lsName);
        submitTransaction(transaction);
        waitForOperationalUpdation(iid);

        LogicalSwitches logicalSwitch = hwvtepUtils.getLogicalSwitch(nodeId, lsName);
        LOG.info("LogicalSwitch: name: {} logicalSwitch: {}", lsName, logicalSwitch);
        Assert.assertNull(logicalSwitch);
    }

    private void verifyGetLogicalSwitch(NodeId nodeId, String lsName) {
        LogicalSwitches logicalSwitch = hwvtepUtils.getLogicalSwitch(nodeId, lsName);
        LOG.info("LogicalSwitch: name: {} logicalSwitch: {}", lsName, logicalSwitch);
        Assert.assertNotNull(logicalSwitch);
    }

    @Test
    public void testAddDeleteRemoteUcastMac() throws InterruptedException {
        LOG.info("Start testing method testAddDeleteRemoteUcastMac.");
        NodeId nodeId = hwvtepNode.getNodeId();

        String lsName1 = "ls11";
        String mac1 = "11:11:11:11:11:11";
        String ip1 = "1.1.1.1";
        String physicalLocator = "11.11.11.11";

        // FIXME: Currently adding logical switch entry and RemoteUcastMacs
        // entry cannot be done in single transaction, creating logical switch
        // before adding RemoteUcastMacs entries
        LogicalSwitches ls = HwvtepSouthboundUtils.createLogicalSwitch(lsName1, "ls11desc", "1100");
        addAndVerifyLogicalSwitch(nodeId, ls);

        WriteTransaction txPutRuMacs = dataBroker.newWriteOnlyTransaction();
        HwvtepPhysicalLocatorAugmentation phyLoc = HwvtepSouthboundUtils
                .createHwvtepPhysicalLocatorAugmentation(physicalLocator);
        HwvtepSouthboundUtils.putPhysicalLocator(txPutRuMacs, nodeId, phyLoc);

        RemoteUcastMacs remoteMac1 = HwvtepSouthboundUtils.createRemoteUcastMac(nodeId, mac1,
                new IpAddress(ip1.toCharArray()), lsName1, phyLoc);

        HwvtepSouthboundUtils.putRemoteUcastMacs(txPutRuMacs, nodeId, Lists.newArrayList(remoteMac1));

        // Submitting PhysicalLocator and RemoteUcastMacs entries in a single
        // transation.
        submitTransaction(txPutRuMacs);
        waitForOperationalUpdation(iid);

        verifyForNotNullGetRemoteUcastMac(nodeId, mac1);

        WriteTransaction txDelRuMacs = dataBroker.newWriteOnlyTransaction();
        HwvtepSouthboundUtils.deleteRemoteUcastMacs(txDelRuMacs, nodeId, Lists.newArrayList(mac1));
        submitTransaction(txDelRuMacs);
        waitForOperationalUpdation(iid);

        verifyForNullGetRemoteUcastMac(nodeId, mac1);

        deleteAndVerifyLogicalSwitch(nodeId, lsName1);
        LOG.info("End testing method testAddDeleteRemoteUcastMac.");
    }

    @Ignore("not ready yet")
    @Test
    public void testAddDeleteRemoteUcastMacs() throws InterruptedException {
        LOG.info("Start testing method testAddDeleteRemoteUcastMacs.");
        NodeId nodeId = hwvtepNode.getNodeId();

        String lsName1 = "ls22";
        String mac1 = "22:11:11:11:11:11";
        String mac2 = "22:22:22:22:22:22";
        String mac3 = "22:33:33:33:33:33";
        String ip1 = "1.1.1.1";
        String ip2 = "2.2.2.2";
        String ip3 = "3.3.3.3";
        String physicalLocator = "20.20.20.20";

        // FIXME: Currently adding logical switch entry and RemoteUcastMacs
        // entry cannot be done in single transaction, creating logical switch
        // before adding RemoteUcastMacs entries
        LogicalSwitches ls = HwvtepSouthboundUtils.createLogicalSwitch(lsName1, "ls22desc", "2200");
        addAndVerifyLogicalSwitch(nodeId, ls);

        WriteTransaction txPutRuMacs = dataBroker.newWriteOnlyTransaction();
        HwvtepPhysicalLocatorAugmentation phyLoc = HwvtepSouthboundUtils
                .createHwvtepPhysicalLocatorAugmentation(physicalLocator);
        HwvtepSouthboundUtils.putPhysicalLocator(txPutRuMacs, nodeId, phyLoc);

        RemoteUcastMacs remoteMac1 = HwvtepSouthboundUtils.createRemoteUcastMac(nodeId, mac1,
                new IpAddress(ip1.toCharArray()), lsName1, phyLoc);
        RemoteUcastMacs remoteMac2 = HwvtepSouthboundUtils.createRemoteUcastMac(nodeId, mac2,
                new IpAddress(ip2.toCharArray()), lsName1, phyLoc);
        RemoteUcastMacs remoteMac3 = HwvtepSouthboundUtils.createRemoteUcastMac(nodeId, mac3,
                new IpAddress(ip3.toCharArray()), lsName1, phyLoc);

        HwvtepSouthboundUtils.putRemoteUcastMacs(txPutRuMacs, nodeId,
                Lists.newArrayList(remoteMac1, remoteMac2, remoteMac3));

        // Submitting PhysicalLocator and RemoteUcastMacs entries in a single
        // transation.
        submitTransaction(txPutRuMacs);
        waitForOperationalUpdation(iid);

        verifyForNotNullGetRemoteUcastMac(nodeId, mac1);
        verifyForNotNullGetRemoteUcastMac(nodeId, mac2);
        verifyForNotNullGetRemoteUcastMac(nodeId, mac3);

        WriteTransaction txDelRuMacs = dataBroker.newWriteOnlyTransaction();
        HwvtepSouthboundUtils.deleteRemoteUcastMacs(txDelRuMacs, nodeId, Lists.newArrayList(mac1, mac2, mac3));
        submitTransaction(txDelRuMacs);
        waitForOperationalUpdation(iid);

        verifyForNullGetRemoteUcastMac(nodeId, mac1);
        verifyForNullGetRemoteUcastMac(nodeId, mac2);
        verifyForNullGetRemoteUcastMac(nodeId, mac3);

        deleteAndVerifyLogicalSwitch(nodeId, lsName1);
        LOG.info("End testing method testAddDeleteRemoteUcastMacs.");
    }

    private void verifyForNullGetRemoteUcastMac(NodeId nodeId, String mac) {
        RemoteUcastMacs ruMacAfterDelete = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                HwvtepSouthboundUtils.createRemoteUcastMacsInstanceIdentifier(nodeId, new MacAddress(mac)));
        LOG.info("VerifyForNotNull RemoteUcastMacs: name: {} RemoteUcastMacs: {}", mac, ruMacAfterDelete);
        Assert.assertNull(ruMacAfterDelete);
    }

    private void verifyForNotNullGetRemoteUcastMac(NodeId nodeId, String mac) {
        RemoteUcastMacs ruMac = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                HwvtepSouthboundUtils.createRemoteUcastMacsInstanceIdentifier(nodeId, new MacAddress(mac)));
        LOG.info("VerifyForNotNull RemoteUcastMacs: name: {} RemoteUcastMacs: {}", mac, ruMac);
        Assert.assertNotNull(ruMac);
    }

    @Ignore("Not ready yet. Need to dynamically add physical switch and physical port entries. Also need to cleanup created data.")
    @Test
    public void testUpdateVlanBindings() throws InterruptedException {
        LOG.info("Start testing method testUpdateVlanBindings.");

        // TODO: Need to dynamically add physical switch and physical port
        // entries.
        String phySwitchName = "s2";
        String phyPort = "s2-eth1";
        String lsName = "lsVlanVni";

        NodeId nodeId = hwvtepNode.getNodeId();
        addAndVerifyLogicalSwitch(nodeId, HwvtepSouthboundUtils.createLogicalSwitch(lsName, lsName + "desc", "2000"));

        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        VlanBindings vlanBinding = HwvtepSouthboundUtils.createVlanBinding(nodeId, 200, lsName);

        HwvtepSouthboundUtils.mergeVlanBindings(transaction, nodeId, phySwitchName, phyPort,
                Lists.newArrayList(vlanBinding));
        submitTransaction(transaction);
        waitForOperationalUpdation(iid);

        verifyForNotNullMergeVlanBinding(nodeId, phySwitchName, phyPort, vlanBinding);

        // TODO: Cleanup created data
        LOG.info("End testing method testUpdateVlanBindings.");
    }

    private void verifyForNotNullMergeVlanBinding(NodeId nodeId, String phySwitchName, String phyPortName,
            VlanBindings vlanBindingToVerify) {
        HwvtepPhysicalPortAugmentation phyPort = hwvtepUtils.getPhysicalPort(nodeId, phySwitchName, phyPortName);

        LOG.info("VerifyForNotNull PhysicalPort: phySwitchName: {}, phyPortName: {} HwvtepPhysicalPortAugmentation: {}",
                phySwitchName, phyPortName, phyPort);
        Assert.assertNotNull(phyPort);
        List<VlanBindings> lstVlanBindings = phyPort.getVlanBindings();
        LOG.info("Vlan bindings: {}", phyPort.getVlanBindings());
        Assert.assertNotNull(lstVlanBindings);
        for (VlanBindings vb : lstVlanBindings) {
            if (vb.getVlanIdKey().getValue().equals(vlanBindingToVerify.getVlanIdKey().getValue())) {
                if (!vb.getLogicalSwitchRef().getValue().equals(vlanBindingToVerify.getLogicalSwitchRef().getValue())) {
                    Assert.assertTrue(
                            "Mismatch logicalSwitchRef. Found logicalSwitchRef: " + vb.getLogicalSwitchRef().getValue()
                                    + ", expecting: " + vlanBindingToVerify.getVlanIdKey().getValue(),
                            false);
                }
            }
        }
    }

    /**
     * Submit transaction.
     *
     * @param transaction
     *            the transaction
     * @return the result of the request
     */
    public static boolean submitTransaction(final WriteTransaction transaction) {
        boolean result = false;
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            result = true;
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Transaction commit failed {} ", e);
        }
        return result;
    }

}
