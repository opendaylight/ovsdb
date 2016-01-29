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
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeBase;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfigBuilder;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
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
    private static final String NETDEV_DP_TYPE = "netdev";
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundIT.class);
    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    private static final int OVSDB_ROUNDTRIP_TIMEOUT = 10000;
    private static final String FORMAT_STR = "%s_%s_%d";
    private static String addressStr;
    private static int portNumber;
    private static String connectionType;
    private static boolean setup = false;
    private static MdsalUtils mdsalUtils = null;
    private static Node ovsdbNode;
    private static int testMethodsRemaining;

    @Inject
    private BundleContext bundleContext;

    private static final NotifyingDataChangeListener CONFIGURATION_LISTENER =
            new NotifyingDataChangeListener(LogicalDatastoreType.CONFIGURATION);
    private static final NotifyingDataChangeListener OPERATIONAL_LISTENER =
            new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL);

    private static class NotifyingDataChangeListener implements DataChangeListener {
        private final LogicalDatastoreType type;
        private final Set<InstanceIdentifier<?>> createdIids = new HashSet<>();
        private final Set<InstanceIdentifier<?>> removedIids = new HashSet<>();
        private final Set<InstanceIdentifier<?>> updatedIids = new HashSet<>();

        private NotifyingDataChangeListener(LogicalDatastoreType type) {
            this.type = type;
        }

        @Override
        public void onDataChanged(
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> asyncDataChangeEvent) {
            LOG.info("{} DataChanged: created {}", type, asyncDataChangeEvent.getCreatedData().keySet());
            LOG.info("{} DataChanged: removed {}", type, asyncDataChangeEvent.getRemovedPaths());
            LOG.info("{} DataChanged: updated {}", type, asyncDataChangeEvent.getUpdatedData().keySet());
            createdIids.addAll(asyncDataChangeEvent.getCreatedData().keySet());
            removedIids.addAll(asyncDataChangeEvent.getRemovedPaths());
            updatedIids.addAll(asyncDataChangeEvent.getUpdatedData().keySet());
            // Handled managed iids
            for (DataObject obj : asyncDataChangeEvent.getCreatedData().values()) {
                if (obj instanceof ManagedNodeEntry) {
                    ManagedNodeEntry managedNodeEntry = (ManagedNodeEntry) obj;
                    LOG.info("{} DataChanged: created managed {}", managedNodeEntry.getBridgeRef().getValue());
                    createdIids.add(managedNodeEntry.getBridgeRef().getValue());
                }
            }
            synchronized(this) {
                notifyAll();
            }
        }

        public boolean isCreated(InstanceIdentifier<?> iid) {
            return createdIids.remove(iid);
        }

        public boolean isRemoved(InstanceIdentifier<?> iid) {
            return removedIids.remove(iid);
        }

        public boolean isUpdated(InstanceIdentifier<?> iid) {
            return updatedIids.remove(iid);
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
        return "odl-ovsdb-southbound-test";
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
        final ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                iid, CONFIGURATION_LISTENER, AsyncDataBroker.DataChangeScope.SUBTREE);
        dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                iid, OPERATIONAL_LISTENER, AsyncDataBroker.DataChangeScope.SUBTREE);

        ovsdbNode = connectOvsdbNode(connectionInfo);

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

    @After
    public void teardown() {
        testMethodsRemaining--;
        LOG.info("{} test methods remaining", testMethodsRemaining);
        if (testMethodsRemaining == 0) {
            try {
                disconnectOvsdbNode(getConnectionInfo(addressStr, portNumber));
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while disconnecting", e);
            }
        }
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

    private static ConnectionInfo getConnectionInfo(final String addressStr, final int portNumber) {
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

    private Node connectOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        Assert.assertTrue(
                mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, iid, SouthboundUtils.createNode(connectionInfo)));
        waitForOperationalCreation(iid);
        Node node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
        Assert.assertNotNull(node);
        LOG.info("Connected to {}", SouthboundUtils.connectionInfoToString(connectionInfo));
        return node;
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

    private void waitForOperationalUpdate(InstanceIdentifier<Node> iid) throws InterruptedException {
        synchronized (OPERATIONAL_LISTENER) {
            long _start = System.currentTimeMillis();
            LOG.info("Waiting for OPERATIONAL DataChanged update on {}", iid);
            while (!OPERATIONAL_LISTENER.isUpdated(
                    iid) && (System.currentTimeMillis() - _start) < OVSDB_ROUNDTRIP_TIMEOUT) {
                OPERATIONAL_LISTENER.wait(OVSDB_UPDATE_TIMEOUT);
            }
            LOG.info("Woke up, waited {} for update of {}", (System.currentTimeMillis() - _start), iid);
        }
    }

    private static void disconnectOvsdbNode(final ConnectionInfo connectionInfo) throws InterruptedException {
        final InstanceIdentifier<Node> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        Assert.assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, iid));
        waitForOperationalDeletion(iid);
        Node node = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, iid);
        Assert.assertNull(node);
        LOG.info("Disconnected from {}", SouthboundUtils.connectionInfoToString(connectionInfo));
    }

    @Test
    public void testAddDeleteOvsdbNode() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        // At this point we're connected, disconnect and reconnect (the connection will be removed at the very end)
        disconnectOvsdbNode(connectionInfo);
        connectOvsdbNode(connectionInfo);
    }

    @Test
    public void testDpdkSwitch() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
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
                    InstanceIdentifier<Node> bridgeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo,
                            new OvsdbBridgeName(SouthboundITConstants.BRIDGE_NAME));
                    NodeId bridgeNodeId = SouthboundUtils.createManagedNodeId(bridgeIid);
                    try (TestBridge testBridge = new TestBridge(connectionInfo, bridgeIid,
                            SouthboundITConstants.BRIDGE_NAME, bridgeNodeId, false, null, true, dpType, null, null,
                            null)) {
                        // Verify that the device is netdev
                        OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
                        Assert.assertNotNull(bridge);
                        Assert.assertEquals(dpType, bridge.getDatapathType());

                        // Add port for all dpdk interface types (dpdkvhost not supported in existing dpdk ovs)
                        List<String> dpdkTypes = new ArrayList<String>();
                        dpdkTypes.add("dpdk");
                        dpdkTypes.add("dpdkr");
                        dpdkTypes.add("dpdkvhostuser");
                        //dpdkTypes.add("dpdkvhost");

                        for (String dpdkType : dpdkTypes) {
                            String testPortname = "test"+dpdkType+"port";
                            LOG.info("DPDK portname and type is {}, {}", testPortname, dpdkType);
                            Class<? extends InterfaceTypeBase> dpdkIfType = SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP
                                    .get(dpdkType);
                            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationpointBuilder =
                                    createSpecificDpdkOvsdbTerminationPointAugmentationBuilder(testPortname,
                                            dpdkIfType);
                            Assert.assertTrue(
                                    addTerminationPoint(bridgeNodeId, testPortname, ovsdbTerminationpointBuilder));
                        }

                        // Verify that all DPDK ports are created
                        InstanceIdentifier<Node> terminationPointIid = getTpIid(connectionInfo, bridge);
                        Node terminationPointNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                                terminationPointIid);
                        Assert.assertNotNull(terminationPointNode);

                        // Verify that each termination point has the specific DPDK ifType
                        for (String dpdkType : dpdkTypes) {
                            String testPortname = "test"+dpdkType+"port";
                            Class<? extends InterfaceTypeBase> dpdkIfType = SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP
                                    .get(dpdkType);
                            List<TerminationPoint> terminationPoints = terminationPointNode.getTerminationPoint();
                            for (TerminationPoint terminationPoint : terminationPoints) {
                                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation = terminationPoint
                                        .getAugmentation(OvsdbTerminationPointAugmentation.class);
                                if (ovsdbTerminationPointAugmentation.getName().equals(testPortname)) {
                                    Class<? extends InterfaceTypeBase> opPort = ovsdbTerminationPointAugmentation
                                            .getInterfaceType();
                                    Assert.assertEquals(dpdkIfType, opPort);
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    @Test
    public void testOvsdbNodeOvsVersion() throws InterruptedException {
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        Assert.assertNotNull(ovsdbNodeAugmentation);
        assertNotNull(ovsdbNodeAugmentation.getOvsVersion());
    }

    @Test
    public void testOpenVSwitchOtherConfig() throws InterruptedException {
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
    }

    @Test
    public void testOvsdbBridgeControllerInfo() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr,portNumber);
        String controllerTarget = SouthboundUtil.getControllerTarget(ovsdbNode);
        assertNotNull("Failed to get controller target", controllerTarget);
        List<ControllerEntry> setControllerEntry = createControllerEntry(controllerTarget);
        Uri setUri = new Uri(controllerTarget);
        try (TestBridge testBridge = new TestBridge(connectionInfo, null, SouthboundITConstants.BRIDGE_NAME,null, true,
                SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null,
                setControllerEntry, null)) {
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
        }
    }

    private List<ControllerEntry> createControllerEntry(String controllerTarget) {
        List<ControllerEntry> controllerEntriesList = new ArrayList<>();
        controllerEntriesList.add(new ControllerEntryBuilder()
                .setTarget(new Uri(controllerTarget))
                .build());
        return controllerEntriesList;
    }

    private static void setManagedBy(final OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                              final ConnectionInfo connectionInfo) {
        InstanceIdentifier<Node> connectionNodePath = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath));
    }

    private static List<ProtocolEntry> createMdsalProtocols() {
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

    private OvsdbTerminationPointAugmentationBuilder createSpecificDpdkOvsdbTerminationPointAugmentationBuilder(
            String testPortname,Class<? extends InterfaceTypeBase> dpdkIfType) {
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                createGenericOvsdbTerminationPointAugmentationBuilder();
        ovsdbTerminationBuilder.setName(testPortname);
        ovsdbTerminationBuilder.setInterfaceType(dpdkIfType);
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

    private static class TestBridge implements AutoCloseable {
        private final ConnectionInfo connectionInfo;
        private final String bridgeName;

        /**
         * Creates a test bridge which can be automatically removed when no longer necessary.
         *
         * @param connectionInfo The connection information.
         * @param bridgeIid The bridge identifier; if {@code null}, one is created based on {@code bridgeName}.
         * @param bridgeName The bridge name; must be provided.
         * @param bridgeNodeId The bridge node identifier; if {@code null}, one is created based on {@code bridgeIid}.
         * @param setProtocolEntries {@code true} to set default protocol entries for the bridge.
         * @param failMode The fail mode to set for the bridge.
         * @param setManagedBy {@code true} to specify {@code setManagedBy} for the bridge.
         * @param dpType The datapath type.
         * @param externalIds The external identifiers if any.
         * @param otherConfigs The other configuration items if any.
         */
        public TestBridge(final ConnectionInfo connectionInfo, @Nullable InstanceIdentifier<Node> bridgeIid,
                                  final String bridgeName, NodeId bridgeNodeId, final boolean setProtocolEntries,
                                  final Class<? extends OvsdbFailModeBase> failMode, final boolean setManagedBy,
                                  @Nullable final Class<? extends DatapathTypeBase> dpType,
                                  @Nullable final List<BridgeExternalIds> externalIds,
                                  @Nullable final List<ControllerEntry> controllerEntries,
                                  @Nullable final List<BridgeOtherConfigs> otherConfigs) {
            this.connectionInfo = connectionInfo;
            this.bridgeName = bridgeName;
            NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            if (bridgeIid == null) {
                bridgeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
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
            ovsdbBridgeAugmentationBuilder.setFailMode(failMode);
            if (setManagedBy) {
                setManagedBy(ovsdbBridgeAugmentationBuilder, connectionInfo);
            }
            ovsdbBridgeAugmentationBuilder.setDatapathType(dpType);
            ovsdbBridgeAugmentationBuilder.setBridgeExternalIds(externalIds);
            ovsdbBridgeAugmentationBuilder.setControllerEntry(controllerEntries);
            ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(otherConfigs);
            bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());
            LOG.debug("Built with the intent to store bridge data {}", ovsdbBridgeAugmentationBuilder.toString());
            Assert.assertTrue(
                    mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, bridgeIid, bridgeNodeBuilder.build()));
            try {
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for bridge creation (bridge {})", bridgeName, e);
            }
        }

        public TestBridge(final ConnectionInfo connectionInfo, final String bridgeName) {
            this(connectionInfo, null, bridgeName, null, true,
                    SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null, null, null);
        }

        @Override
        public void close() {
            final InstanceIdentifier<Node> iid =
                    SouthboundUtils.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
            Assert.assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, iid));
            try {
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for bridge deletion (bridge {})", bridgeName, e);
            }
        }
    }

    private static class TestQos implements AutoCloseable {
        private final ConnectionInfo connectionInfo;
        private final Uri qosId;

        /**
         * Creates a test qos entry which can be automatically removed when no longer necessary.
         *
         * @param connectionInfo The connection information.
         * @param qosId The Qos identifier.
         * @param qosType The qos type.
         * @param externalIds The external identifiers if any.
         * @param otherConfigs The other configuration items if any.
         */
        public TestQos(final ConnectionInfo connectionInfo,
                                  final Uri qosId,
                                  final Class<? extends QosTypeBase> qosType,
                                  @Nullable final List<QosExternalIds> externalIds,
                                  @Nullable final List<QosOtherConfig> otherConfigs) {
            this.connectionInfo = connectionInfo;
            this.qosId = qosId;

            QosEntriesBuilder qosEntryBuilder = new QosEntriesBuilder();
            qosEntryBuilder.setQosId(qosId);
            qosEntryBuilder.setQosType(qosType);
            qosEntryBuilder.setQosExternalIds(externalIds);
            qosEntryBuilder.setQosOtherConfig(otherConfigs);

            QosEntries qosEntry = qosEntryBuilder.build();
            InstanceIdentifier<QosEntries> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(QosEntries.class, qosEntry.getKey());
            Assert.assertTrue(
                    mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                    iid, qosEntry));

            try {
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for qos {}", qosId, e);
            }
        }

        @Override
        public void close() {
            final InstanceIdentifier<QosEntries> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(QosEntries.class, new QosEntriesKey(this.qosId));

            Assert.assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, iid));
            try {
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for qos deletion (qos {})", qosId, e);
            }
        }
    }

    private static class TestQueue implements AutoCloseable {
        private final ConnectionInfo connectionInfo;
        private final Uri queueId;

        /**
         * Creates a test queue entry which can be automatically removed when no longer necessary.
         *
         * @param connectionInfo The connection information.
         * @param queueId The Queue identifier.
         * @param queueDscp The queue dscp value.
         * @param externalIds The external identifiers if any.
         * @param otherConfigs The other configuration items if any.
         */
        public TestQueue(final ConnectionInfo connectionInfo,
                                  final Uri queueId,
                                  final Short queueDscp,
                                  @Nullable final List<QueuesExternalIds> externalIds,
                                  @Nullable final List<QueuesOtherConfig> otherConfigs) {
            this.connectionInfo = connectionInfo;
            this.queueId = queueId;

            QueuesBuilder queueBuilder = new QueuesBuilder();
            queueBuilder.setQueueId(queueId);
            queueBuilder.setDscp(queueDscp);
            queueBuilder.setQueuesExternalIds(externalIds);
            queueBuilder.setQueuesOtherConfig(otherConfigs);

            Queues queue = queueBuilder.build();
            InstanceIdentifier<Queues> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(Queues.class, queue.getKey());
            Assert.assertTrue(
                    mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                    iid, queue));

            try {
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for queue {}", queueId, e);
            }
        }

        @Override
        public void close() {
            final InstanceIdentifier<Queues> iid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(Queues.class, new QueuesKey(this.queueId));

            Assert.assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, iid));
            try {
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted while waiting for bridge deletion (bridge {})", queueId, e);
            }
        }
    }

    private OvsdbNodeAugmentation getOvsdbNode(ConnectionInfo connectionInfo, LogicalDatastoreType store) {
        InstanceIdentifier<Node> nodeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
        Node node = mdsalUtils.read(store, nodeIid);
        Assert.assertNotNull(node);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = node.getAugmentation(OvsdbNodeAugmentation.class);
        Assert.assertNotNull(ovsdbNodeAugmentation);
        return ovsdbNodeAugmentation;
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
                SouthboundUtils.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
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

    @Test
    public void testAddDeleteBridge() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            Assert.assertNotNull(bridge);
            LOG.info("bridge: {}", bridge);
        }
    }

    private InstanceIdentifier<Node> getTpIid(ConnectionInfo connectionInfo, OvsdbBridgeAugmentation bridge) {
        return SouthboundUtils.createInstanceIdentifier(connectionInfo, bridge.getBridgeName());
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
            ConnectionInfo connectionInfo, String bridgeName, LogicalDatastoreType store, int index) {

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

        // CREATE
        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            Assert.assertNotNull(bridge);
            LOG.info("bridge: {}", bridge);
            NodeId nodeId = SouthboundMapper.createManagedNodeId(SouthboundUtils.createInstanceIdentifier(
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
                    Assert.assertTrue(ofPort.equals(OFPORT_EXPECTED) || ofPort.equals(1L));
                    LOG.info("ofPort: {}", ofPort);
                }
            }

            // UPDATE- Not Applicable.  From the OpenVSwitch Documentation:
            //   "A client should ideally set this column’s value in the same database transaction that it uses to create
            //   the interface."

            // DELETE handled by TestBridge
        }
    }

    @Test
    public void testCRDTerminationPointOfPortRequest() throws InterruptedException {
        final Long OFPORT_EXPECTED = 45008L;
        final Long OFPORT_INPUT = 45008L;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // CREATE
        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            Assert.assertNotNull(bridge);
            NodeId nodeId = SouthboundUtils.createManagedNodeId(SouthboundUtils.createInstanceIdentifier(
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
                    Assert.assertTrue(ofPort.equals(OFPORT_EXPECTED) || ofPort.equals(1L));
                    LOG.info("ofPort: {}", ofPort);

                    Integer ofPortRequest = ovsdbTerminationPointAugmentation.getOfportRequest();
                    Assert.assertTrue(ofPortRequest.equals(ofPortRequestExpected));
                    LOG.info("ofPortRequest: {}", ofPortRequest);
                }
            }

            // UPDATE- Not Applicable.  From the OpenVSwitch documentation:
            //   "A client should ideally set this column’s value in the same database transaction that it uses to
            //   create the interface. "

            // DELETE handled by TestBridge
        }
    }

    private <T> void assertExpectedExist(List<T> expected, List<T> test) {
        if (expected != null && test != null) {
            for (T exp : expected) {
                Assert.assertTrue("The retrieved values don't contain " + exp, test.contains(exp));
            }
        }
    }

    private interface SouthboundTerminationPointHelper<T> {
        void writeValues(OvsdbTerminationPointAugmentationBuilder builder, List<T> values);
        List<T> readValues(OvsdbTerminationPointAugmentation augmentation);
    }

    /*
     * Tests the CRUD operations for <code>Port</code> <code>external_ids</code>.
     *
     * @see <code>SouthboundIT.generatePortExternalIdsTestCases()</code> for specific test case information
     */
    private <T> void testCRUDTerminationPoint(
            KeyValueBuilder<T> builder, String prefix, SouthboundTerminationPointHelper<T> helper)
            throws InterruptedException {
        final int TERMINATION_POINT_TEST_INDEX = 0;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<T>> updateFromTestCases = generateKeyValueTestCases(builder, prefix + "From");
        List<SouthboundTestCase<T>> updateToTestCases = generateKeyValueTestCases(builder, prefix + "To");

        for (SouthboundTestCase<T> updateFromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<T> updateToTestCase : updateToTestCases) {
                String testBridgeAndPortName = String.format("%s_%s", prefix, updateToTestCase.name);

                // CREATE: Create the test bridge
                try (TestBridge testBridge = new TestBridge(connectionInfo, null, testBridgeAndPortName, null, true,
                        SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null, null,
                        null)) {
                    NodeId testBridgeNodeId = SouthboundUtils.createManagedNodeId(
                            SouthboundUtils.createInstanceIdentifier(connectionInfo,
                                    new OvsdbBridgeName(testBridgeAndPortName)));
                    OvsdbTerminationPointAugmentationBuilder tpCreateAugmentationBuilder =
                            createGenericOvsdbTerminationPointAugmentationBuilder();
                    tpCreateAugmentationBuilder.setName(testBridgeAndPortName);
                    helper.writeValues(tpCreateAugmentationBuilder, updateFromTestCase.inputValues);
                    Assert.assertTrue(
                            addTerminationPoint(testBridgeNodeId, testBridgeAndPortName, tpCreateAugmentationBuilder));

                    // READ: Read the test port and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbTerminationPointAugmentation updateFromConfigurationTerminationPointAugmentation =
                            getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeAndPortName,
                                    LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                    if (updateFromConfigurationTerminationPointAugmentation != null) {
                        List<T> updateFromConfigurationValues =
                                helper.readValues(updateFromConfigurationTerminationPointAugmentation);
                        assertExpectedExist(updateFromTestCase.expectedValues, updateFromConfigurationValues);
                    }
                    OvsdbTerminationPointAugmentation updateFromOperationalTerminationPointAugmentation =
                            getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeAndPortName,
                                    LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                    if (updateFromOperationalTerminationPointAugmentation != null) {
                        List<T> updateFromOperationalValues =
                                helper.readValues(updateFromOperationalTerminationPointAugmentation);
                        assertExpectedExist(updateFromTestCase.expectedValues, updateFromOperationalValues);
                    }

                    // UPDATE:  update the values
                    testBridgeNodeId = getBridgeNode(connectionInfo, testBridgeAndPortName).getNodeId();
                    OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder =
                            new OvsdbTerminationPointAugmentationBuilder();
                    helper.writeValues(tpUpdateAugmentationBuilder, updateToTestCase.inputValues);
                    InstanceIdentifier<Node> portIid = SouthboundMapper.createInstanceIdentifier(testBridgeNodeId);
                    NodeBuilder portUpdateNodeBuilder = new NodeBuilder();
                    NodeId portUpdateNodeId = SouthboundUtils.createManagedNodeId(portIid);
                    portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
                    TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
                    tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(testBridgeAndPortName)));
                    tpUpdateBuilder.addAugmentation(
                            OvsdbTerminationPointAugmentation.class,
                            tpUpdateAugmentationBuilder.build());
                    portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
                    Assert.assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                            portIid, portUpdateNodeBuilder.build()));
                    Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                    // READ: the test port and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbTerminationPointAugmentation updateToConfigurationTerminationPointAugmentation =
                            getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeAndPortName,
                                    LogicalDatastoreType.CONFIGURATION, TERMINATION_POINT_TEST_INDEX);
                    if (updateToConfigurationTerminationPointAugmentation != null) {
                        List<T> updateToConfigurationValues =
                                helper.readValues(updateToConfigurationTerminationPointAugmentation);
                        assertExpectedExist(updateToTestCase.expectedValues, updateToConfigurationValues);
                        assertExpectedExist(updateFromTestCase.expectedValues, updateToConfigurationValues);
                    }
                    OvsdbTerminationPointAugmentation updateToOperationalTerminationPointAugmentation =
                            getOvsdbTerminationPointAugmentation(connectionInfo, testBridgeAndPortName,
                                    LogicalDatastoreType.OPERATIONAL, TERMINATION_POINT_TEST_INDEX);
                    if (updateToOperationalTerminationPointAugmentation != null) {
                        List<T> updateToOperationalValues =
                                helper.readValues(updateToOperationalTerminationPointAugmentation);
                        if (updateFromTestCase.expectedValues != null) {
                            assertExpectedExist(updateToTestCase.expectedValues, updateToOperationalValues);
                            assertExpectedExist(updateFromTestCase.expectedValues, updateToOperationalValues);
                        }
                    }

                    // DELETE handled by TestBridge
                }
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
        testCRUDTerminationPoint(new SouthboundPortExternalIdsBuilder(), "TPPortExternalIds",
                new PortExternalIdsSouthboundHelper());
    }

    /*
     * Tests the CRUD operations for <code>Interface</code> <code>external_ids</code>.
     *
     * @see <code>SouthboundIT.generateInterfaceExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointInterfaceExternalIds() throws InterruptedException {
        testCRUDTerminationPoint(new SouthboundInterfaceExternalIdsBuilder(), "TPInterfaceExternalIds",
                new InterfaceExternalIdsSouthboundHelper());
    }

    /*
     * Tests the CRUD operations for <code>TerminationPoint</code> <code>options</code>.
     *
     * @see <code>SouthboundIT.generateTerminationPointOptions()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointOptions() throws InterruptedException {
        testCRUDTerminationPoint(new SouthboundOptionsBuilder(), "TPOptions", new OptionsSouthboundHelper());
    }

    /*
     * Tests the CRUD operations for <code>Interface</code> <code>other_configs</code>.
     *
     * @see <code>SouthboundIT.generateInterfaceExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointInterfaceOtherConfigs() throws InterruptedException {
        testCRUDTerminationPoint(new SouthboundInterfaceOtherConfigsBuilder(), "TPInterfaceOtherConfigs",
                new InterfaceOtherConfigsSouthboundHelper());
    }

    /*
     * Tests the CRUD operations for <code>Port</code> <code>other_configs</code>.
     *
     * @see <code>SouthboundIT.generatePortExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDTerminationPointPortOtherConfigs() throws InterruptedException {
        testCRUDTerminationPoint(new SouthboundPortOtherConfigsBuilder(), "TPPortOtherConfigs",
                new PortOtherConfigsSouthboundHelper());
    }

    @Test
    public void testCRUDTerminationPointVlan() throws InterruptedException {
        final Integer CREATED_VLAN_ID = 4000;
        final Integer UPDATED_VLAN_ID = 4001;

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // CREATE
        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME);
            Assert.assertNotNull(bridge);
            NodeId nodeId = SouthboundUtils.createManagedNodeId(SouthboundUtils.createInstanceIdentifier(
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
            NodeId portUpdateNodeId = SouthboundUtils.createManagedNodeId(portIid);
            portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
            TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
            tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(portName)));
            tpUpdateBuilder.addAugmentation(
                    OvsdbTerminationPointAugmentation.class,
                    tpUpdateAugmentationBuilder.build());
            tpUpdateBuilder.setTpId(new TpId(portName));
            portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
            Assert.assertTrue(
                    mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, portIid, portUpdateNodeBuilder.build()));
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

            // DELETE handled by TestBridge
        }
    }

    @Test
    public void testCRUDTerminationPointVlanModes() throws InterruptedException {
        final VlanMode UPDATED_VLAN_MODE = VlanMode.Access;
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        VlanMode []vlanModes = VlanMode.values();
        for (VlanMode vlanMode : vlanModes) {
            // CREATE
            try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
                OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
                Assert.assertNotNull(bridge);
                NodeId nodeId = SouthboundUtils.createManagedNodeId(SouthboundUtils.createInstanceIdentifier(
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
                NodeId portUpdateNodeId = SouthboundUtils.createManagedNodeId(portIid);
                portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
                TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
                tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(portName)));
                tpUpdateBuilder.addAugmentation(
                        OvsdbTerminationPointAugmentation.class,
                        tpUpdateAugmentationBuilder.build());
                tpUpdateBuilder.setTpId(new TpId(portName));
                portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
                Assert.assertTrue(
                        mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, portIid, portUpdateNodeBuilder.build()));
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

                // DELETE handled by TestBridge
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Set<Integer>> generateVlanSets() {
        int min = 0;
        int max = 4095;
        return Lists.newArrayList(
                Collections.<Integer>emptySet(),
                Sets.newHashSet(2222),
                Sets.newHashSet(min, max, min + 1, max - 1, (max - min) / 2));
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
        Iterable<Set<Integer>> vlanSets = generateVlanSets();
        int testCase = 0;
        for (Set<Integer> vlanSet : vlanSets) {
            ++testCase;
            // CREATE
            try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME)) {
                OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
                Assert.assertNotNull(bridge);
                NodeId nodeId = SouthboundUtils.createManagedNodeId(connectionInfo, bridge.getBridgeName());
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
                NodeId portUpdateNodeId = SouthboundUtils.createManagedNodeId(portIid);
                portUpdateNodeBuilder.setNodeId(portUpdateNodeId);
                TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
                tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(portName)));
                tpUpdateBuilder.addAugmentation(
                        OvsdbTerminationPointAugmentation.class,
                        tpUpdateAugmentationBuilder.build());
                tpUpdateBuilder.setTpId(new TpId(portName));
                portUpdateNodeBuilder.setTerminationPoint(Lists.newArrayList(tpUpdateBuilder.build()));
                Assert.assertTrue(
                        mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, portIid, portUpdateNodeBuilder.build()));
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

                // DELETE handled by TestBridge
            }
        }
    }

    /*
     * Tests setting and deleting <code>qos</code> field in a <code>port</code>.
     */
    @Test
    public void testCRUDTerminationPointQos() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        String testQosId = new String("testQosEntry");

        // CREATE
        try (TestBridge testBridge = new TestBridge(connectionInfo, SouthboundITConstants.BRIDGE_NAME);
                TestQos testQos = new TestQos(connectionInfo, new Uri(testQosId),
                        SouthboundMapper.createQosType(SouthboundConstants.QOS_LINUX_HFSC), null, null)) {
            OvsdbNodeAugmentation ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            QosEntries operQos = getQos(new Uri(testQosId), ovsdbNodeAugmentation);
            Assert.assertNotNull(operQos);
            Uuid qosUuid = new Uuid(operQos.getQosUuid().getValue());

            OvsdbBridgeAugmentation bridge = getBridge(connectionInfo);
            Assert.assertNotNull(bridge);
            NodeId nodeId = SouthboundUtils.createManagedNodeId(connectionInfo, bridge.getBridgeName());
            OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder =
                    createGenericOvsdbTerminationPointAugmentationBuilder();
            String portName = "testTerminationPointQos";
            ovsdbTerminationBuilder.setName(portName);
            ovsdbTerminationBuilder.setQos(qosUuid);
            Assert.assertTrue(addTerminationPoint(nodeId, portName, ovsdbTerminationBuilder));

            // READ and check that qos uuid has been added to the port
            InstanceIdentifier<TerminationPoint> tpEntryIid = getTpIid(connectionInfo, bridge)
                    .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));
            TerminationPoint terminationPoint = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, tpEntryIid);
            Assert.assertNotNull(terminationPoint);
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                    terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class);
            Assert.assertEquals(ovsdbTerminationPointAugmentation.getQos(), qosUuid);

            // UPDATE - remove the qos entry from the port
            OvsdbTerminationPointAugmentationBuilder tpUpdateAugmentationBuilder =
                    new OvsdbTerminationPointAugmentationBuilder();
            tpUpdateAugmentationBuilder.setName(portName);
            TerminationPointBuilder tpUpdateBuilder = new TerminationPointBuilder();
            tpUpdateBuilder.setKey(new TerminationPointKey(new TpId(portName)));
            tpUpdateBuilder.addAugmentation(
                    OvsdbTerminationPointAugmentation.class,
                    tpUpdateAugmentationBuilder.build());
            tpUpdateBuilder.setTpId(new TpId(portName));

            Assert.assertTrue(
                    mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, tpEntryIid, tpUpdateBuilder.build()));
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);

            // READ and verify that qos uuid has been removed from port
            TerminationPoint terminationPointUpdate = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, tpEntryIid);
            Assert.assertNotNull(terminationPointUpdate);
            OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentationUpdate =
                    terminationPointUpdate.getAugmentation(OvsdbTerminationPointAugmentation.class);
            Assert.assertNull(ovsdbTerminationPointAugmentationUpdate.getQos());

            // DELETE handled by TestBridge
        }
    }

    @Test
    public void testGetOvsdbNodes() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        InstanceIdentifier<Topology> topologyPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        Topology topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, topologyPath);
        InstanceIdentifier<Node> expectedNodeIid = SouthboundUtils.createInstanceIdentifier(connectionInfo);
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
    }

    /*
     * @see <code>SouthboundIT.generateBridgeOtherConfigsTestCases()</code> for specific test case information.
     */
    @Test
    public void testCRUDBridgeOtherConfigs() throws InterruptedException {
        testCRUDBridge("BridgeOtherConfigs", new SouthboundBridgeOtherConfigsBuilder(),
                new BridgeOtherConfigsSouthboundHelper());
    }

    private interface SouthboundBridgeHelper<T> {
        void writeValues(OvsdbBridgeAugmentationBuilder builder, List<T> values);
        List<T> readValues(OvsdbBridgeAugmentation augmentation);
    }

    private <T> void testCRUDBridge(String prefix, KeyValueBuilder<T> builder, SouthboundBridgeHelper<T> helper)
            throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<T>> updateFromTestCases = generateKeyValueTestCases(builder, prefix + "From");
        List<SouthboundTestCase<T>> updateToTestCases = generateKeyValueTestCases(builder, prefix + "To");
        for (SouthboundTestCase<T> updateFromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<T> updateToTestCase : updateToTestCases) {
                String testBridgeName = String.format("%s_%s", prefix, updateToTestCase.name);

                // CREATE: Create the test bridge
                final OvsdbBridgeName ovsdbBridgeName = new OvsdbBridgeName(testBridgeName);
                final InstanceIdentifier<Node> bridgeIid =
                        SouthboundUtils.createInstanceIdentifier(connectionInfo, ovsdbBridgeName);
                final NodeId bridgeNodeId = SouthboundMapper.createManagedNodeId(bridgeIid);
                final NodeBuilder bridgeCreateNodeBuilder = new NodeBuilder();
                bridgeCreateNodeBuilder.setNodeId(bridgeNodeId);
                OvsdbBridgeAugmentationBuilder bridgeCreateAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
                bridgeCreateAugmentationBuilder.setBridgeName(ovsdbBridgeName);
                bridgeCreateAugmentationBuilder.setProtocolEntry(createMdsalProtocols());
                bridgeCreateAugmentationBuilder.setFailMode(
                        SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"));
                setManagedBy(bridgeCreateAugmentationBuilder, connectionInfo);
                helper.writeValues(bridgeCreateAugmentationBuilder, updateFromTestCase.inputValues);
                bridgeCreateNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class,
                        bridgeCreateAugmentationBuilder.build());
                LOG.debug("Built with the intent to store bridge data {}", bridgeCreateAugmentationBuilder.toString());
                Assert.assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, bridgeIid,
                        bridgeCreateNodeBuilder.build()));
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                // READ: Read the test bridge and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                List<T> updateFromConfigurationExternalIds = helper.readValues(getBridge(connectionInfo, testBridgeName,
                        LogicalDatastoreType.CONFIGURATION));
                assertExpectedExist(updateFromTestCase.expectedValues, updateFromConfigurationExternalIds);
                List<T> updateFromOperationalExternalIds = helper.readValues(getBridge(connectionInfo, testBridgeName));
                assertExpectedExist(updateFromTestCase.expectedValues, updateFromOperationalExternalIds);

                // UPDATE:  update the values
                final OvsdbBridgeAugmentationBuilder bridgeUpdateAugmentationBuilder =
                        new OvsdbBridgeAugmentationBuilder();
                helper.writeValues(bridgeUpdateAugmentationBuilder, updateToTestCase.inputValues);
                final NodeBuilder bridgeUpdateNodeBuilder = new NodeBuilder();
                final Node bridgeNode = getBridgeNode(connectionInfo, testBridgeName);
                bridgeUpdateNodeBuilder.setNodeId(bridgeNode.getNodeId());
                bridgeUpdateNodeBuilder.setKey(bridgeNode.getKey());
                bridgeUpdateNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class,
                        bridgeUpdateAugmentationBuilder.build());
                Assert.assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, bridgeIid,
                        bridgeUpdateNodeBuilder.build()));
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                // READ: the test bridge and ensure changes are propagated to the CONFIGURATION data store,
                // then repeat for OPERATIONAL data store
                List<T> updateToConfigurationExternalIds = helper.readValues(getBridge(connectionInfo, testBridgeName,
                        LogicalDatastoreType.CONFIGURATION));
                assertExpectedExist(updateToTestCase.expectedValues, updateToConfigurationExternalIds);
                assertExpectedExist(updateFromTestCase.expectedValues, updateToConfigurationExternalIds);
                List<T> updateToOperationalExternalIds = helper.readValues(getBridge(connectionInfo, testBridgeName));
                if (updateFromTestCase.expectedValues != null) {
                    assertExpectedExist(updateToTestCase.expectedValues, updateToOperationalExternalIds);
                    assertExpectedExist(updateFromTestCase.expectedValues, updateToOperationalExternalIds);
                }

                // DELETE
                Assert.assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, bridgeIid));
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            }
        }
    }

    /*
     * @see <code>SouthboundIT.generateBridgeExternalIdsTestCases()</code> for specific test case information
     */
    @Test
    public void testCRUDBridgeExternalIds() throws InterruptedException {
        testCRUDBridge("BridgeExternalIds", new SouthboundBridgeExternalIdsBuilder(),
                new BridgeExternalIdsSouthboundHelper());
    }

    @Test
    public void testAddDeleteQos() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        OvsdbNodeAugmentation ovsdbNodeAugmentation;
        Uri qosUri = new Uri("QOS-ROW");
        List<String> typeList = new ArrayList<String>();
        typeList.add(SouthboundConstants.QOS_LINUX_HTB);
        typeList.add(SouthboundConstants.QOS_LINUX_HFSC);

        for (String qosType : typeList) {
            try (TestQos testQos = new TestQos(connectionInfo, qosUri, SouthboundMapper.createQosType(qosType), null, null)) {
                ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                        LogicalDatastoreType.OPERATIONAL);
                QosEntries operQosHtb = getQos(qosUri, ovsdbNodeAugmentation);
                Assert.assertNotNull(operQosHtb);
            }
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            QosEntries operQosHtb = getQos(qosUri, ovsdbNodeAugmentation);
            Assert.assertNull(operQosHtb);
        }
    }

    @Test
    public void testAddDeleteQueue() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        Uri queueUri = new Uri("QUEUE-A1");

        try (TestQueue testQueue = new TestQueue(connectionInfo, queueUri, new Short("25"), null, null)) {
            OvsdbNodeAugmentation ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            Queues operQueue = getQueue(queueUri, ovsdbNodeAugmentation);
            Assert.assertNotNull(operQueue);
        }
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                LogicalDatastoreType.OPERATIONAL);
        Queues operQueue = getQueue(queueUri, ovsdbNodeAugmentation);
        Assert.assertNull(operQueue);
    }

    private static class SouthboundQueuesExternalIdsHelper implements SouthboundQueueHelper<QueuesExternalIds> {
        @Override
        public void writeValues(QueuesBuilder builder, List<QueuesExternalIds> values) {
            builder.setQueuesExternalIds(values);
        }

        @Override
        public List<QueuesExternalIds> readValues(Queues queue) {
            return queue.getQueuesExternalIds();
        }
    }

    private static class SouthboundQueuesOtherConfigHelper implements SouthboundQueueHelper<QueuesOtherConfig> {
        @Override
        public void writeValues(QueuesBuilder builder, List<QueuesOtherConfig> values) {
            builder.setQueuesOtherConfig(values);
        }

        @Override
        public List<QueuesOtherConfig> readValues(Queues queue) {
            return queue.getQueuesOtherConfig();
        }
    }

    private interface SouthboundQueueHelper<T> {
        void writeValues(QueuesBuilder builder, List<T> values);
        List<T> readValues(Queues queue);
    }

    private Queues getQueue(Uri queueId, OvsdbNodeAugmentation node) {
        for (Queues queue : node.getQueues()) {
            if (queue.getKey().getQueueId().equals(queueId))
                return queue;
        }
        return null;
    }

    private static class SouthboundQosExternalIdsHelper implements SouthboundQosHelper<QosExternalIds> {
        @Override
        public void writeValues(QosEntriesBuilder builder, List<QosExternalIds> values) {
            builder.setQosExternalIds(values);
        }

        @Override
        public List<QosExternalIds> readValues(QosEntries qos) {
            return qos.getQosExternalIds();
        }
    }

    private static class SouthboundQosOtherConfigHelper implements SouthboundQosHelper<QosOtherConfig> {
        @Override
        public void writeValues(QosEntriesBuilder builder, List<QosOtherConfig> values) {
            builder.setQosOtherConfig(values);
        }

        @Override
        public List<QosOtherConfig> readValues(QosEntries qos) {
            return qos.getQosOtherConfig();
        }
    }

    private interface SouthboundQosHelper<T> {
        void writeValues(QosEntriesBuilder builder, List<T> values);
        List<T> readValues(QosEntries qos);
    }

    private QosEntries getQos(Uri qosId, OvsdbNodeAugmentation node) {
        for (QosEntries qos : node.getQosEntries()) {
            if (qos.getKey().getQosId().equals(qosId))
                return qos;
        }
        return null;
    }

    private <T> void testCRUDQueue(
            KeyValueBuilder<T> builder, String prefix, SouthboundQueueHelper<T> helper)
            throws InterruptedException {

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<T>> updateFromTestCases = generateKeyValueTestCases(builder, prefix + "From");
        List<SouthboundTestCase<T>> updateToTestCases = generateKeyValueTestCases(builder, prefix + "To");

        for (SouthboundTestCase<T> updateFromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<T> updateToTestCase : updateToTestCases) {
                String testQueueId = String.format("%s_%s", prefix, updateToTestCase.name);

                // CREATE: and update the test queue with starting values.
                try (TestQueue testQueue = new TestQueue(connectionInfo, new Uri(testQueueId), new Short("45"), null, null)) {
                    QueuesBuilder queuesBuilder = new QueuesBuilder();
                    queuesBuilder.setQueueId(new Uri(testQueueId));
                    InstanceIdentifier<Queues> queueIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                            .augmentation(OvsdbNodeAugmentation.class)
                            .child(Queues.class, queuesBuilder.build().getKey());
                    helper.writeValues(queuesBuilder, updateFromTestCase.inputValues);
                    Assert.assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                            queueIid, queuesBuilder.build()));
                    Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                    // READ: Read the test queue and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbNodeAugmentation updateFromConfigurationOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.CONFIGURATION);
                    Queues queueFromConfig = getQueue(new Uri(testQueueId), updateFromConfigurationOvsdbNodeAugmentation);
                    if (queueFromConfig != null) {
                        List<T> updateFromConfigurationValues =
                                helper.readValues(queueFromConfig);
                        assertExpectedExist(updateFromTestCase.expectedValues, updateFromConfigurationValues);
                    }
                    OvsdbNodeAugmentation updateFromOperationalOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.OPERATIONAL);
                    Queues queueFromOper = getQueue(new Uri(testQueueId), updateFromOperationalOvsdbNodeAugmentation);
                    if (queueFromOper != null) {
                        List<T> updateFromOperationalValues =
                                helper.readValues(queueFromOper);
                        assertExpectedExist(updateFromTestCase.expectedValues, updateFromOperationalValues);
                    }

                    // UPDATE:  update the values
                    QueuesBuilder queuesUpdateBuilder = new QueuesBuilder();
                    queuesUpdateBuilder.setQueueId(new Uri(testQueueId));
                    helper.writeValues(queuesUpdateBuilder, updateToTestCase.inputValues);
                    Assert.assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                            queueIid, queuesUpdateBuilder.build()));
                    Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                    // READ: the test queue and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbNodeAugmentation updateToConfigurationOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.CONFIGURATION);
                    Queues queueToConfig = getQueue(new Uri(testQueueId), updateToConfigurationOvsdbNodeAugmentation);
                    if (queueToConfig != null) {
                        List<T> updateToConfigurationValues =
                                helper.readValues(queueToConfig);
                        assertExpectedExist(updateToTestCase.expectedValues, updateToConfigurationValues);
                    }
                    OvsdbNodeAugmentation updateToOperationalOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.OPERATIONAL);
                    Queues queueToOper = getQueue(new Uri(testQueueId), updateToOperationalOvsdbNodeAugmentation);
                    if (queueToOper != null) {
                        List<T> updateToOperationalValues =
                                helper.readValues(queueToOper);
                        assertExpectedExist(updateToTestCase.expectedValues, updateToOperationalValues);
                    }

                    // DELETE handled by TestQueue
                }
            }
        }
    }

    @Test
    public void testCRUDQueueExternalIds() throws InterruptedException {
        testCRUDQueue(new SouthboundQueuesExternalIdsBuilder(), "QueueExternalIds",
                new SouthboundQueuesExternalIdsHelper());
    }

    @Test
    public void testCRUDQueueOtherConfig() throws InterruptedException {
        testCRUDQueue(new SouthboundQueuesOtherConfigBuilder(), "QueueOtherConfig",
                new SouthboundQueuesOtherConfigHelper());
    }

    @Test
    public void testCRUDQueueDscp() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        String testQueueId = "testQueueDscp";

        // CREATE: and update the test queue with starting values.
        try (TestQueue testQueue = new TestQueue(connectionInfo, new Uri(testQueueId), new Short("0"), null, null)) {
            for (Short dscp = 1; dscp < 64; dscp++) {
                QueuesBuilder queuesBuilder = new QueuesBuilder();
                queuesBuilder.setQueueId(new Uri(testQueueId));
                InstanceIdentifier<Queues> queueIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(Queues.class, queuesBuilder.build().getKey());
                queuesBuilder.setDscp(dscp);
                Assert.assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                        queueIid, queuesBuilder.build()));
                Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                // READ: Read the test queue and ensure changes are propagated to the OPERATIONAL data store
                // assumption is that CONFIGURATION was updated if OPERATIONAL is correct
                OvsdbNodeAugmentation ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                        LogicalDatastoreType.OPERATIONAL);
                Queues operQueue = getQueue(new Uri(testQueueId), ovsdbNodeAugmentation);
                Assert.assertNotNull(operQueue);
                Short operDscp = operQueue.getDscp();
                Assert.assertNotNull(operDscp);
                Assert.assertEquals(dscp, operDscp);
            }

            // DELETE handled by TestQueue
        }

    }

    private <T> void testCRUDQos(
            KeyValueBuilder<T> builder, String prefix, SouthboundQosHelper<T> helper)
            throws InterruptedException {

        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);

        // updateFromTestCases represent the original test case value.  updateToTestCases represent the new value after
        // the update has been performed.
        List<SouthboundTestCase<T>> updateFromTestCases = generateKeyValueTestCases(builder, prefix + "From");
        List<SouthboundTestCase<T>> updateToTestCases = generateKeyValueTestCases(builder, prefix + "To");

        for (SouthboundTestCase<T> updateFromTestCase : updateFromTestCases) {
            for (SouthboundTestCase<T> updateToTestCase : updateToTestCases) {
                String testQosId = String.format("%s_%s", prefix, updateToTestCase.name);

                // CREATE: and update the test qos with starting values.
                try (TestQos testQos = new TestQos(connectionInfo, new Uri(testQosId), SouthboundMapper.createQosType(SouthboundConstants.QOS_LINUX_HTB), null, null)) {
                    QosEntriesBuilder qosBuilder = new QosEntriesBuilder();
                    qosBuilder.setQosId(new Uri(testQosId));
                    InstanceIdentifier<QosEntries> qosIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                            .augmentation(OvsdbNodeAugmentation.class)
                            .child(QosEntries.class, qosBuilder.build().getKey());
                    helper.writeValues(qosBuilder, updateFromTestCase.inputValues);
                    Assert.assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                            qosIid, qosBuilder.build()));
                    Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                    // READ: Read the test queue and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbNodeAugmentation updateFromConfigurationOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.CONFIGURATION);
                    QosEntries qosFromConfig = getQos(new Uri(testQosId), updateFromConfigurationOvsdbNodeAugmentation);
                    if (qosFromConfig != null) {
                        List<T> updateFromConfigurationValues =
                                helper.readValues(qosFromConfig);
                        assertExpectedExist(updateFromTestCase.expectedValues, updateFromConfigurationValues);
                    }
                    OvsdbNodeAugmentation updateFromOperationalOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.OPERATIONAL);
                    QosEntries qosFromOper = getQos(new Uri(testQosId), updateFromOperationalOvsdbNodeAugmentation);
                    if (qosFromOper != null) {
                        List<T> updateFromOperationalValues =
                                helper.readValues(qosFromOper);
                        assertExpectedExist(updateFromTestCase.expectedValues, updateFromOperationalValues);
                    }

                    // UPDATE:  update the values
                    QosEntriesBuilder qosUpdateBuilder = new QosEntriesBuilder();
                    qosUpdateBuilder.setQosId(new Uri(testQosId));
                    helper.writeValues(qosUpdateBuilder, updateToTestCase.inputValues);
                    Assert.assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                            qosIid, qosUpdateBuilder.build()));
                    Thread.sleep(OVSDB_UPDATE_TIMEOUT);

                    // READ: the test queue and ensure changes are propagated to the CONFIGURATION data store,
                    // then repeat for OPERATIONAL data store
                    OvsdbNodeAugmentation updateToConfigurationOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.CONFIGURATION);
                    QosEntries qosToConfig = getQos(new Uri(testQosId), updateToConfigurationOvsdbNodeAugmentation);
                    if (qosToConfig != null) {
                        List<T> updateToConfigurationValues =
                                helper.readValues(qosToConfig);
                        assertExpectedExist(updateToTestCase.expectedValues, updateToConfigurationValues);
                    }
                    OvsdbNodeAugmentation updateToOperationalOvsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                            LogicalDatastoreType.OPERATIONAL);
                    QosEntries qosToOper = getQos(new Uri(testQosId), updateToOperationalOvsdbNodeAugmentation);
                    if (qosToOper != null) {
                        List<T> updateToOperationalValues =
                                helper.readValues(qosToOper);
                        assertExpectedExist(updateToTestCase.expectedValues, updateToOperationalValues);
                    }

                    // DELETE handled by TestQueue
                }
            }
        }
    }

    @Test
    public void testCRUDQosExternalIds() throws InterruptedException {
        testCRUDQos(new SouthboundQosExternalIdsBuilder(), "QosExternalIds",
                new SouthboundQosExternalIdsHelper());
    }

    @Test
    public void testCRUDQosOtherConfig() throws InterruptedException {
        testCRUDQos(new SouthboundQosOtherConfigBuilder(), "QosOtherConfig",
                new SouthboundQosOtherConfigHelper());
    }
    @Test
    public void testCRUDQosQueues() throws InterruptedException {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portNumber);
        String testQosId = "testQosQueues";

        // CREATE: and update the test queue with starting values.
        try (TestQos testQos = new TestQos(connectionInfo, new Uri(testQosId),
                SouthboundMapper.createQosType(SouthboundConstants.QOS_LINUX_HTB), null, null);
                TestQueue testQueue1 = new TestQueue(connectionInfo, new Uri("queue1"), new Short("12"), null, null);
                TestQueue testQueue2 = new TestQueue(connectionInfo, new Uri("queue2"), new Short("35"), null, null)) {
            QosEntriesBuilder qosBuilder = new QosEntriesBuilder();
            qosBuilder.setQosId(new Uri(testQosId));
            InstanceIdentifier<QosEntries> qosIid = SouthboundUtils.createInstanceIdentifier(connectionInfo)
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(QosEntries.class, qosBuilder.build().getKey());

            // READ, UPDATE:  Read the UUIDs of the Queue rows and add them to the
            // configuration of the Qos row.
            OvsdbNodeAugmentation ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            Queues operQueue1 = getQueue(new Uri("queue1"), ovsdbNodeAugmentation);
            Assert.assertNotNull(operQueue1);
            Uuid queue1Uuid = new Uuid(operQueue1.getQueueUuid().getValue());
            Queues operQueue2 = getQueue(new Uri("queue2"), ovsdbNodeAugmentation);
            Assert.assertNotNull(operQueue2);
            Uuid queue2Uuid = new Uuid(operQueue2.getQueueUuid().getValue());

            List<QueueList> queueList = new ArrayList<>();
            queueList.add(new QueueListBuilder().setQueueNumber(new Long("0"))
                    .setQueueUuid(queue1Uuid).build());
            queueList.add(new QueueListBuilder().setQueueNumber(new Long("1"))
                    .setQueueUuid(queue2Uuid).build());
            qosBuilder.setQueueList(queueList);

            Assert.assertTrue(mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                    qosIid, qosBuilder.build()));
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);

            // READ: Read the test qos and ensure changes are propagated to the OPERATIONAL data store
            // assumption is that CONFIGURATION was updated if OPERATIONAL is correct
            ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            QosEntries operQos = getQos(new Uri(testQosId), ovsdbNodeAugmentation);
            Assert.assertNotNull(operQos);
            List<QueueList> operQueueList = operQos.getQueueList();
            Assert.assertNotNull(operQueueList);
            for (QueueList queueEntry : queueList) {
                Assert.assertTrue(operQueueList.contains(queueEntry));
            }

            // DELETE one queue from queue list and check that one remains
            KeyedInstanceIdentifier<QueueList, QueueListKey> qosQueueIid = qosIid
                        .child(QueueList.class, new QueueListKey(new Long("0")));
            Assert.assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, qosQueueIid));
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);

            // READ: Read the test qos and ensure changes are propagated to the OPERATIONAL data store
            // assumption is that CONFIGURATION was updated if OPERATIONAL is correct
            ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            operQos = getQos(new Uri(testQosId), ovsdbNodeAugmentation);
            Assert.assertNotNull(operQos);
            operQueueList = operQos.getQueueList();
            Assert.assertNotNull(operQueueList);
            for (QueueList queueEntry : queueList) {
                if (queueEntry.getQueueUuid().equals(queue2Uuid))
                    Assert.assertTrue(operQueueList.contains(queueEntry));
                else if (queueEntry.getQueueUuid().equals(queue1Uuid)) {
                    Assert.assertFalse(operQueueList.contains(queueEntry));
                } else {
                    Assert.assertTrue("Unknown queue entry in qos queue list", false);
                }
            }

            // DELETE  queue list and check that list is empty
            qosQueueIid = qosIid
                    .child(QueueList.class, new QueueListKey(new Long("1")));
            Assert.assertTrue(mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, qosQueueIid));
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);

            ovsdbNodeAugmentation = getOvsdbNode(connectionInfo,
                    LogicalDatastoreType.OPERATIONAL);
            operQos = getQos(new Uri(testQosId), ovsdbNodeAugmentation);
            Assert.assertNotNull(operQos);
            operQueueList = operQos.getQueueList();
            Assert.assertNotNull(operQueueList);
            Assert.assertTrue(operQueueList.isEmpty());
        }
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
         * Indicates that the provided input values should be expected as output values.
         *
         * @return The builder.
         */
        public SouthboundTestCaseBuilder<T> expectInputAsOutput() {
            this.expectedValues = this.inputValues;
            return this;
        }

        /**
         * Indicates that no output should be expected.
         *
         * @return The builder.
         */
        public SouthboundTestCaseBuilder<T> expectNoOutput() {
            this.expectedValues = null;
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

    private abstract static class KeyValueBuilder<T> {
        private static final int COUNTER_START = 0;
        private int counter = COUNTER_START;

        protected abstract Builder<T> builder();

        protected abstract void setKey(Builder<T> builder, String key);

        protected abstract void setValue(Builder<T> builder, String value);

        protected abstract boolean isValueMandatory();

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

        public final void reset() {
            this.counter = COUNTER_START;
        }
    }

    private static final class SouthboundQueuesExternalIdsBuilder extends KeyValueBuilder<QueuesExternalIds> {
        @Override
        protected Builder<QueuesExternalIds> builder() {
            return new QueuesExternalIdsBuilder();
        }

        @Override
        protected void setKey(Builder<QueuesExternalIds> builder, String key) {
            ((QueuesExternalIdsBuilder) builder).setQueuesExternalIdKey(key);
        }

        @Override
        protected void setValue(Builder<QueuesExternalIds> builder, String value) {
            ((QueuesExternalIdsBuilder) builder).setQueuesExternalIdValue(value);
        }

        @Override
        protected boolean isValueMandatory() {
            return true;
        }
    }

    private static final class SouthboundQueuesOtherConfigBuilder extends KeyValueBuilder<QueuesOtherConfig> {
        @Override
        protected Builder<QueuesOtherConfig> builder() {
            return new QueuesOtherConfigBuilder();
        }

        @Override
        protected void setKey(Builder<QueuesOtherConfig> builder, String key) {
            ((QueuesOtherConfigBuilder) builder).setQueueOtherConfigKey(key);
        }

        @Override
        protected void setValue(Builder<QueuesOtherConfig> builder, String value) {
            ((QueuesOtherConfigBuilder) builder).setQueueOtherConfigValue(value);
        }

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundQosExternalIdsBuilder extends KeyValueBuilder<QosExternalIds> {
        @Override
        protected Builder<QosExternalIds> builder() {
            return new QosExternalIdsBuilder();
        }

        @Override
        protected void setKey(Builder<QosExternalIds> builder, String key) {
            ((QosExternalIdsBuilder) builder).setQosExternalIdKey(key);
        }

        @Override
        protected void setValue(Builder<QosExternalIds> builder, String value) {
            ((QosExternalIdsBuilder) builder).setQosExternalIdValue(value);
        }

        @Override
        protected boolean isValueMandatory() {
            return true;
        }
    }

    private static final class SouthboundQosOtherConfigBuilder extends KeyValueBuilder<QosOtherConfig> {
        @Override
        protected Builder<QosOtherConfig> builder() {
            return new QosOtherConfigBuilder();
        }

        @Override
        protected void setKey(Builder<QosOtherConfig> builder, String key) {
            ((QosOtherConfigBuilder) builder).setOtherConfigKey(key);
        }

        @Override
        protected void setValue(Builder<QosOtherConfig> builder, String value) {
            ((QosOtherConfigBuilder) builder).setOtherConfigValue(value);
        }

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundPortExternalIdsBuilder extends KeyValueBuilder<PortExternalIds> {
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

        @Override
        protected boolean isValueMandatory() {
            return true;
        }
    }

    private static final class SouthboundInterfaceExternalIdsBuilder extends KeyValueBuilder<InterfaceExternalIds> {
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

        @Override
        protected boolean isValueMandatory() {
            return true;
        }
    }

    private static final class SouthboundOptionsBuilder extends KeyValueBuilder<Options> {
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

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundInterfaceOtherConfigsBuilder extends KeyValueBuilder<InterfaceOtherConfigs> {
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

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundPortOtherConfigsBuilder extends KeyValueBuilder<PortOtherConfigs> {
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

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundBridgeOtherConfigsBuilder extends KeyValueBuilder<BridgeOtherConfigs> {
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

        @Override
        protected boolean isValueMandatory() {
            return false;
        }
    }

    private static final class SouthboundBridgeExternalIdsBuilder extends KeyValueBuilder<BridgeExternalIds> {
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

        @Override
        protected boolean isValueMandatory() {
            return true;
        }
    }

    /*
     * Generates the test cases involved in testing key-value-based data.  See inline comments for descriptions of
     * the particular cases considered.
     */
    private static <T> List<SouthboundTestCase<T>> generateKeyValueTestCases(
            KeyValueBuilder<T> builder, String testName) {
        List<SouthboundTestCase<T>> testCases = new ArrayList<>();

        final String GOOD_KEY = "GoodKey";
        final String GOOD_VALUE = "GoodValue";
        final String NO_VALUE_FOR_KEY = "NoValueForKey";

        final String idKey = testName + "Key";
        final String idValue = testName + "Value";

        // Test Case 1:  TestOne
        // Test Type:    Positive
        // Description:  Create a termination point with one value
        // Expected:     A port is created with the single value specified below
        final String testOneName = "TestOne" + testName;
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
        final String testFiveName = "TestFive" + testName;
        testCases.add(new SouthboundTestCaseBuilder<T>()
                .name(testFiveName)
                .input(
                        builder.build(testFiveName, idKey, idValue),
                        builder.build(testFiveName, idKey, idValue),
                        builder.build(testFiveName, idKey, idValue),
                        builder.build(testFiveName, idKey, idValue),
                        builder.build(testFiveName, idKey, idValue))
                .expectInputAsOutput()
                .build());
        builder.reset();

        if (!builder.isValueMandatory()) {
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
            final String testOneGoodOneMalformedValueName = "TestOneGoodOneMalformedValue" + testName;
            testCases.add(new SouthboundTestCaseBuilder<T>()
                    .name(testOneGoodOneMalformedValueName)
                    .input(
                            builder.build(testOneGoodOneMalformedValueName, GOOD_KEY, GOOD_VALUE),
                            builder.build(testOneGoodOneMalformedValueName, NO_VALUE_FOR_KEY, null))
                    .expectNoOutput()
                    .build());
            builder.reset();
        } else {
            LOG.info("generateKeyValueTestCases: skipping test case 3 for {}", builder.getClass().getSimpleName());
        }

        return testCases;
    }

    private static class PortExternalIdsSouthboundHelper implements SouthboundTerminationPointHelper<PortExternalIds> {
        @Override
        public void writeValues(OvsdbTerminationPointAugmentationBuilder builder, List<PortExternalIds> values) {
            builder.setPortExternalIds(values);
        }

        @Override
        public List<PortExternalIds> readValues(OvsdbTerminationPointAugmentation augmentation) {
            return augmentation.getPortExternalIds();
        }
    }

    private static class InterfaceExternalIdsSouthboundHelper implements
            SouthboundTerminationPointHelper<InterfaceExternalIds> {
        @Override
        public void writeValues(
                OvsdbTerminationPointAugmentationBuilder builder, List<InterfaceExternalIds> values) {
            builder.setInterfaceExternalIds(values);
        }

        @Override
        public List<InterfaceExternalIds> readValues(OvsdbTerminationPointAugmentation augmentation) {
            return augmentation.getInterfaceExternalIds();
        }
    }

    private static class OptionsSouthboundHelper implements SouthboundTerminationPointHelper<Options> {
        @Override
        public void writeValues(
                OvsdbTerminationPointAugmentationBuilder builder, List<Options> values) {
            builder.setOptions(values);
        }

        @Override
        public List<Options> readValues(OvsdbTerminationPointAugmentation augmentation) {
            return augmentation.getOptions();
        }
    }

    private static class InterfaceOtherConfigsSouthboundHelper implements
            SouthboundTerminationPointHelper<InterfaceOtherConfigs> {
        @Override
        public void writeValues(
                OvsdbTerminationPointAugmentationBuilder builder, List<InterfaceOtherConfigs> values) {
            builder.setInterfaceOtherConfigs(values);
        }

        @Override
        public List<InterfaceOtherConfigs> readValues(OvsdbTerminationPointAugmentation augmentation) {
            return augmentation.getInterfaceOtherConfigs();
        }
    }

    private static class PortOtherConfigsSouthboundHelper implements
            SouthboundTerminationPointHelper<PortOtherConfigs> {
        @Override
        public void writeValues(
                OvsdbTerminationPointAugmentationBuilder builder, List<PortOtherConfigs> values) {
            builder.setPortOtherConfigs(values);
        }

        @Override
        public List<PortOtherConfigs> readValues(OvsdbTerminationPointAugmentation augmentation) {
            return augmentation.getPortOtherConfigs();
        }
    }

    private static class BridgeExternalIdsSouthboundHelper implements SouthboundBridgeHelper<BridgeExternalIds> {
        @Override
        public void writeValues(
                OvsdbBridgeAugmentationBuilder builder, List<BridgeExternalIds> values) {
            builder.setBridgeExternalIds(values);
        }

        @Override
        public List<BridgeExternalIds> readValues(OvsdbBridgeAugmentation augmentation) {
            return augmentation.getBridgeExternalIds();
        }
    }

    private static class BridgeOtherConfigsSouthboundHelper implements SouthboundBridgeHelper<BridgeOtherConfigs> {
        @Override
        public void writeValues(
                OvsdbBridgeAugmentationBuilder builder, List<BridgeOtherConfigs> values) {
            builder.setBridgeOtherConfigs(values);
        }

        @Override
        public List<BridgeOtherConfigs> readValues(OvsdbBridgeAugmentation augmentation) {
            return augmentation.getBridgeOtherConfigs();
        }
    }
}
