/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.it;

import static org.junit.Assert.*;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundProvider;
import org.opendaylight.ovsdb.lib.notation.*;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.utils.hwvtepsouthbound.utils.HwvtepSouthboundUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
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
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
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
    public static final String PS_NAME = "ps0";

    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    private static final int OVSDB_ROUNDTRIP_TIMEOUT = 10000;

    public static MdsalUtils mdsalUtils = null;
    private static HwvtepBuilderUtils hwvtepBuilderUtils = null;
    private static boolean setup = false;
    private static int testMethodsRemaining;
    private static String addressStr;
    private static int portNumber;
    private static String connectionType;
    private static Node hwvtepNode;
    @Inject
    @Filter(timeout = 60000)
    public static DataBroker dataBroker = null;
    @Inject
    private BundleContext bundleContext;

    static InstanceIdentifier<Node> dId;

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
        return new Option[]{
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
                logConfiguration(HwvtepSouthboundIT.class),
                LogLevel.INFO.name());
        option = composite(option, super.getLoggingOption());
        return option;
    }

    private Option[] getPropertiesOptions() {
        Properties props = new Properties(System.getProperties());
        String addressStr = props.getProperty(SERVER_IPADDRESS, DEFAULT_SERVER_IPADDRESS);
        String portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(CONNECTION_TYPE, CONNECTION_TYPE_ACTIVE);

        LOG.info("getPropertiesOptions: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);

        return new Option[]{
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
            LOG.warn("Failed to setup test", e);
            fail("Failed to setup test: " + e);
        }

        addressStr = bundleContext.getProperty(SERVER_IPADDRESS);
        String portStr = bundleContext.getProperty(SERVER_PORT);
        try {
            portNumber = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
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
        hwvtepBuilderUtils = new HwvtepBuilderUtils();

        assertTrue("Did not find " + HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID.getValue(), getHwvtepTopology());
        final ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        final InstanceIdentifier<Node> iid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
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

    private Boolean getHwvtepTopology() {
        LOG.info("getHwvtepTopology: looking for {}...", HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID.getValue());
        Boolean found = false;
        final TopologyId topologyId = HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID;
        InstanceIdentifier<Topology> path =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, new TopologyKey(topologyId));
        for (int i = 0; i < 60; i++) {
            Topology topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, path);
            if (topology != null) {
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

    private void waitForOperationalCreation(InstanceIdentifier<?> iid) throws InterruptedException {
        synchronized (OPERATIONAL_LISTENER) {
            long _start = System.currentTimeMillis();
            LOG.info("Waiting for OPERATIONAL DataChanged creation on {}", iid);
            while (mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid) == null
                    && (System.currentTimeMillis() - _start) < OVSDB_ROUNDTRIP_TIMEOUT) {
                OPERATIONAL_LISTENER.wait(OVSDB_UPDATE_TIMEOUT);
            }
            LOG.info("Woke up, waited {} for creation of {}", (System.currentTimeMillis() - _start), iid);
        }
    }

    private static void waitForOperationalDeletion(InstanceIdentifier<?> iid) throws InterruptedException {
        synchronized (OPERATIONAL_LISTENER) {
            long _start = System.currentTimeMillis();
            LOG.info("Waiting for OPERATIONAL DataChanged deletion on {}", iid);
            while (mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid) != null && (System.currentTimeMillis() - _start) < OVSDB_ROUNDTRIP_TIMEOUT) {
                OPERATIONAL_LISTENER.wait(OVSDB_UPDATE_TIMEOUT);
            }
            LOG.info("Woke up, waited {} for deletion of {}", (System.currentTimeMillis() - _start), iid);
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

        public TestPhysicalSwitch(final ConnectionInfo connectionInfo, final String name,
                                  @Nullable InstanceIdentifier<Node> psIid, @Nullable NodeId psNodeId,
                                  @Nullable final String description, final boolean setManagedBy,
                                  @Nullable final List<ManagementIps> managementIps,
                                  @Nullable final List<TunnelIps> tunnelIps,
                                  @Nullable final List<Tunnels> tunnels) {
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
            PhysicalSwitchAugmentation pSwitch = hwvtepBuilderUtils.getPhysicalSwitch(connectionInfo);
            Assert.assertNotNull("Adding PhysicalSwitch", pSwitch);
            LOG.info("PhysicalSwitch: {}", pSwitch);

            InstanceIdentifier<Node> psIid =
                    HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo, new HwvtepNodeName(PS_NAME));
            Assert.assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, psIid));
            waitForOperationalDeletion(psIid);
            Node nodeDel = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, psIid);
            Assert.assertNull("Deletion of PhysicalSwitch", nodeDel);
        }
    }

    @Test
    public void testAddLogicalSwitch() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        final InstanceIdentifier<Node> iid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
        NodeId hwvtepNodeId = iid.firstKeyOf(Node.class).getNodeId();
        LogicalSwitches logicalSwitch = hwvtepBuilderUtils.createLogicalSwitch("ls0", "", "100");
        InstanceIdentifier<LogicalSwitches> lsiid = hwvtepBuilderUtils.createLogicalSwitchesInstanceIdentifier(hwvtepNodeId,
                logicalSwitch.getHwvtepNodeName());
        ListenableFuture<Void> lsCreateFuture = hwvtepBuilderUtils.addLogicalSwitch(dataBroker, LogicalDatastoreType.CONFIGURATION,
                lsiid, logicalSwitch);

        waitForOperationalCreation(lsiid);

        LogicalSwitches ls = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, lsiid);
        assertNotNull("Adding LogicalSwitch", ls);
    }

    @Test
    public void testAddRemoteUcastMac() throws InterruptedException, ExecutionException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        final InstanceIdentifier<Node> iid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
        NodeId hwvtepNodeId = iid.firstKeyOf(Node.class).getNodeId();
        Node node = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, iid);
        HwvtepGlobalAugmentation globalAugmentation = node.getAugmentation(HwvtepGlobalAugmentation.class);
        String remoteUcasteMacDataD1 = "20:00:00:00:00:01,11.10.10.1,12.0.0.1,ls0";

        IpAddress ipaddr = new IpAddress(new Ipv4Address("12.0.0.1"));
        hwvtepBuilderUtils.createRemotePhysicalLocatorEntry(dataBroker.newReadWriteTransaction(), iid, ipaddr);

        LogicalSwitches logicalSwitch = hwvtepBuilderUtils.createLogicalSwitch("ls0", "", "100");
        InstanceIdentifier<LogicalSwitches> lsiid =
                hwvtepBuilderUtils.createLogicalSwitchesInstanceIdentifier(hwvtepNodeId,
                        logicalSwitch.getHwvtepNodeName());
        hwvtepBuilderUtils.addLogicalSwitch(dataBroker, LogicalDatastoreType.CONFIGURATION, lsiid, logicalSwitch);

        waitForOperationalCreation(lsiid);

        NodeBuilder nodeBuilder = new NodeBuilder(node);
        nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, hwvtepBuilderUtils.addRemoteUcastMacs(iid, globalAugmentation,
                hwvtepBuilderUtils.getData(remoteUcasteMacDataD1)));
        node = nodeBuilder.build();
        mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, iid, node);

        List<RemoteUcastMacs> configUcastMacs = node.getAugmentation(HwvtepGlobalAugmentation.class).getRemoteUcastMacs();
        assertNotEquals("Reading Remote Ucast Mac from CFG DS", 0, configUcastMacs.size());
        for (RemoteUcastMacs mac : configUcastMacs) {
            InstanceIdentifier<RemoteUcastMacs> rumId = iid.augmentation(HwvtepGlobalAugmentation.class).child(RemoteUcastMacs.class,
                    mac.getKey());
            waitForOperationalCreation(rumId);
        }

        node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
        HwvtepGlobalAugmentation augmentation = node.getAugmentation(HwvtepGlobalAugmentation.class);
        List<RemoteUcastMacs> remoteUcastMacs = augmentation.getRemoteUcastMacs();
        assertNotNull("remote ucast should be created in operational datastore", remoteUcastMacs);
        assertNotNull("Reading Remote Ucast Mac from CFG DS", configUcastMacs);
        assertEquals("comparing Remote Ucast Mac from CFG DS to operational",
                configUcastMacs.size(), remoteUcastMacs.size());


        /*Trying to delete when Remote Ucast is present. deletion of ls should fail*/
        NodeId hwvtepNodeId1 = iid.firstKeyOf(Node.class).getNodeId();
        InstanceIdentifier<LogicalSwitches> lsiid1 =
                hwvtepBuilderUtils.createLogicalSwitchesInstanceIdentifier(hwvtepNodeId1,
                        new HwvtepNodeName("ls0"));

        LOG.error("lsiid to del ls {}" + lsiid1);

        mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, lsiid1);
        waitForOperationalDeletion(lsiid1);

        LogicalSwitches ls = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, lsiid1);
        Assert.assertNotNull("Logical switch should NOT be NULL, as deletion of ls will fail", ls);


        for (RemoteUcastMacs mac : configUcastMacs) {
            InstanceIdentifier<RemoteUcastMacs> rumId = iid.augmentation(HwvtepGlobalAugmentation.class).child(RemoteUcastMacs.class,
                    mac.getKey());
            mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, rumId);
            waitForOperationalDeletion(rumId);
        }

        node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
        assertNotNull("Node should not be NULL", node);
        augmentation = node.getAugmentation(HwvtepGlobalAugmentation.class);
        remoteUcastMacs = augmentation.getRemoteUcastMacs();
        assertNotNull("Reading Remote Ucast Mac from OP DS", remoteUcastMacs);
        assertEquals("comparing Remote Ucast Mac from CFG DS to operational", 0, remoteUcastMacs.size());
    }

    @Test
    public void testAddLocalUcastMac() throws InterruptedException, ExecutionException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        final InstanceIdentifier<Node> iid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
        Node node = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, iid);
        HwvtepGlobalAugmentation globalAugmentation = node.getAugmentation(HwvtepGlobalAugmentation.class);
        String localUcasteMacDataD1 = "10:00:00:00:00:01,12.10.10.1,12.0.0.1,ls0";
        NodeId hwvtepNodeId = iid.firstKeyOf(Node.class).getNodeId();

/*       String localUcasteMacDataD1="10:00:00:00:00:01,12.10.10.1,12.0.0.1,ls0," +
                "10:00:00:00:00:02,12.10.10.2,12.0.0.1,ls0," +
                "10:00:00:00:00:03,12.10.10.3,12.0.0.1,ls0," +
                "10:00:00:00:00:04,12.10.10.4,12.0.0.1,ls0";*/

        IpAddress ipaddr = new IpAddress(new Ipv4Address("12.0.0.1"));
        hwvtepBuilderUtils.createRemotePhysicalLocatorEntry(dataBroker.newReadWriteTransaction(), iid, ipaddr);
        //Thread.sleep(10000);
        LOG.error("physical locator added");

        LogicalSwitches logicalSwitch = hwvtepBuilderUtils.createLogicalSwitch("ls0", "", "100");
        InstanceIdentifier<LogicalSwitches> lsiid =
                hwvtepBuilderUtils.createLogicalSwitchesInstanceIdentifier(hwvtepNodeId,
                        logicalSwitch.getHwvtepNodeName());
        ListenableFuture<Void> lsCreateFuture =
                hwvtepBuilderUtils.addLogicalSwitch(dataBroker, LogicalDatastoreType.CONFIGURATION,
                        lsiid, logicalSwitch);

        LOG.error("Checking iid for Op {}" + iid);
        //Thread.sleep(10000);
        waitForOperationalCreation(lsiid);

        NodeBuilder nodeBuilder = new NodeBuilder(node);
        nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, hwvtepBuilderUtils.addLocalUcastMacs(iid, globalAugmentation,
                hwvtepBuilderUtils.getData(localUcasteMacDataD1)));
        node = nodeBuilder.build();
        mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, iid, node);
        List<LocalUcastMacs> configUcastMacs = node.getAugmentation(HwvtepGlobalAugmentation.class).getLocalUcastMacs();
        assertNotEquals("comparing Local Ucast Mac from CFG DS to operational", 0, configUcastMacs.size());
        for (LocalUcastMacs mac : configUcastMacs) {
            InstanceIdentifier<LocalUcastMacs> rumId = iid.augmentation(HwvtepGlobalAugmentation.class).child(LocalUcastMacs.class,
                    mac.getKey());
            waitForOperationalCreation(rumId);
        }

        node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
        assertNotNull("Node should not be NULL", node);
        HwvtepGlobalAugmentation augmentation = node.getAugmentation(HwvtepGlobalAugmentation.class);
        List<LocalUcastMacs> localUcastMacs = augmentation.getLocalUcastMacs();
        assertNotNull("Reading Local Ucast Mac from OP DS", localUcastMacs);
        assertEquals("comparing Remote Ucast Mac from CFG DS to operational",
                configUcastMacs.size(), localUcastMacs.size());

        for (LocalUcastMacs mac : configUcastMacs) {
            InstanceIdentifier<LocalUcastMacs> rumId = iid.augmentation(HwvtepGlobalAugmentation.class).child(LocalUcastMacs.class,
                    mac.getKey());
            mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, rumId);
            waitForOperationalDeletion(rumId);
        }

        node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
        assertNotNull("Node should not be NULL", node);
        augmentation = node.getAugmentation(HwvtepGlobalAugmentation.class);
        localUcastMacs = augmentation.getLocalUcastMacs();
        assertNotNull("Reading Local Ucast Mac from OP DS", localUcastMacs);
        assertEquals("comparing Remote Ucast Mac from CFG DS to operational", 0, localUcastMacs.size());
    }

    @Test
    public void testAddPhysicalPort() throws Exception {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        final InstanceIdentifier<Node> iid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
        NodeId hwvtepNodeId = iid.firstKeyOf(Node.class).getNodeId();

        List<String> portNameList = hwvtepBuilderUtils.getPortNameListD1();
        String d1PsNodeIdVal = iid.firstKeyOf(Node.class).getNodeId().getValue() + "/physicalswitch/" + "s3";
        InstanceIdentifier<Node> PsId = hwvtepBuilderUtils.getInstanceIdentifier(d1PsNodeIdVal);
        hwvtepBuilderUtils.addPsNode(PsId, iid, portNameList);
        LOG.error("Before sleep ");
        Thread.sleep(10000);
        LOG.error("After sleep");
        LOG.error("Checking iid for Op {}" + iid);
    }

    @Test
    public void testDeleteLogicalSwitch() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        final InstanceIdentifier<Node> iid = HwvtepSouthboundUtils.createInstanceIdentifier(connectionInfo);
        NodeId hwvtepNodeId = iid.firstKeyOf(Node.class).getNodeId();
        InstanceIdentifier<LogicalSwitches> lsiid =
                hwvtepBuilderUtils.createLogicalSwitchesInstanceIdentifier(hwvtepNodeId,
                        new HwvtepNodeName("ls0"));

        LOG.error("lsiid to del ls {}" + lsiid);

        mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, lsiid);
        waitForOperationalDeletion(lsiid);

        LogicalSwitches ls = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, lsiid);
        Assert.assertNull("Logical switch deletion should be success to operational", ls);

    }
}
