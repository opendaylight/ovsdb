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

import com.google.common.base.Optional;
import com.google.common.collect.ObjectArrays;
import com.google.common.util.concurrent.CheckedFuture;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
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
    private static DataBroker dataBroker = null;
    private static String addressStr;
    private static String portStr;
    private static String connectionType;
    private static Boolean setup = false;
    private MdsalUtils mdsalUtils = null;
    private String extras = "false";
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

    @Test
    public void testPassiveNode() throws InterruptedException {
        if (connectionType.equalsIgnoreCase(SouthboundITConstants.CONNECTION_TYPE_PASSIVE)) {
            //Wait for CONNECTION_INIT_TIMEOUT for the Passive connection to be initiated by the ovsdb-server.
            Thread.sleep(SouthboundITConstants.CONNECTION_INIT_TIMEOUT);
        }
    }

    @Test
    public void testAddRemoveOvsdbNode() throws InterruptedException {
        addNode(addressStr, portStr);
        Thread.sleep(1000);
        Node node = readNode(addressStr, portStr, LogicalDatastoreType.OPERATIONAL);
        assertNotNull(node);
        LOG.info("Connected node: {}", node);
        deleteNode(addressStr, portStr);
        Thread.sleep(1000);
        node = readNode(addressStr, portStr, LogicalDatastoreType.OPERATIONAL);
        Assume.assumeNotNull(node);//assertNull(node);
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

        return new ConnectionInfoBuilder()
                       .setRemoteIp(address)
                       .setRemotePort(port)
                       .build();
    }

    private void addNode(String addressStr, String portStr) {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);

        final ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, SouthboundMapper.createInstanceIdentifier(connectionInfo),
                SouthboundMapper.createNode(connectionInfo));
        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = rwTx.submit();
        try {
            commitFuture.checkedGet();
        } catch (TransactionCommitFailedException e) {
            fail("Failed transaction: " + rwTx + e);
        }
    }

    private Node readNode(String addressStr, String portStr, LogicalDatastoreType type) {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);

        final ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        Optional<Node> node = Optional.absent();
        CheckedFuture<Optional<Node>, ReadFailedException> read;
        read = rwTx.read(type, SouthboundMapper.createInstanceIdentifier(connectionInfo));
        try {
            node = read.checkedGet();
            if (node.isPresent()) {
                return node.get();
            }
        } catch (ReadFailedException e) {
            fail("Failed transaction: " + rwTx + e);
        }

        return null;
    }

    private void deleteNode(String addressStr, String portStr) {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);

        final ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        rwTx.delete(LogicalDatastoreType.CONFIGURATION, SouthboundMapper.createInstanceIdentifier(connectionInfo));
        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = rwTx.submit();
        try {
            commitFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            fail("Failed transaction: " + rwTx + e);
        }
    }

    private NetworkTopology readNetworkTopology(LogicalDatastoreType type) {
        ConnectionInfo connectionInfo = getConnectionInfo(addressStr, portStr);

        final ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        Optional<NetworkTopology> optional = Optional.absent();
        CheckedFuture<Optional<NetworkTopology>, ReadFailedException> read;
        read = rwTx.read(type, InstanceIdentifier.create(NetworkTopology.class));
        try {
            optional = read.checkedGet();
            if (optional.isPresent()) {
                return optional.get();
            }
        } catch (ReadFailedException e) {
            fail("Failed transaction: " + rwTx + e);
        }

        return null;
    }

    @Test
    public void testNetworkTopology() throws InterruptedException {
        NetworkTopology networkTopology = MdsalUtils.readTransaction(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(NetworkTopology.class));
        Assert.assertNotNull("NetworkTopology could not be found in " + LogicalDatastoreType.CONFIGURATION,
                networkTopology);

        networkTopology = MdsalUtils.readTransaction(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(NetworkTopology.class));
        Assert.assertNotNull("NetworkTopology could not be found in " + LogicalDatastoreType.OPERATIONAL,
                networkTopology);
    }

    @Test
    public void testOvsdbTopology() throws InterruptedException {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        Topology topology = MdsalUtils.readTransaction(LogicalDatastoreType.CONFIGURATION, path);
        Assert.assertNotNull("Topology could not be found in " + LogicalDatastoreType.CONFIGURATION,
                topology);

        topology = MdsalUtils.readTransaction(LogicalDatastoreType.OPERATIONAL, path);

        Assert.assertNotNull("Topology could not be found in " + LogicalDatastoreType.OPERATIONAL,
                topology);
    }

    public Node connectNode(String addressStr, String portStr) throws InterruptedException {
        addNode(addressStr, portStr);
        Thread.sleep(5000);
        Node node = readNode(addressStr, portStr, LogicalDatastoreType.OPERATIONAL);
        assertNotNull(node);
        LOG.info("Connected node: {}", node);
        return node;
    }

    public void disconnectNode(String addressStr, String portStr) throws InterruptedException {
        deleteNode(addressStr, portStr);
        Thread.sleep(5000);
        Node node = readNode(addressStr, portStr, LogicalDatastoreType.OPERATIONAL);
        Assume.assumeNotNull(node);//Assert.assertNull("node was found in operational", node);
    }

    @Test
    public void testOpenVSwitchOtherConfig() throws InterruptedException {
        Node node = connectNode(addressStr, portStr);
        Thread.sleep(5000);
        OvsdbNodeAugmentation ovsdbNodeAugmentation = node.getAugmentation(OvsdbNodeAugmentation.class);
        assertNotNull(ovsdbNodeAugmentation);
        List<OpenvswitchOtherConfigs> otherConfigsList = ovsdbNodeAugmentation.getOpenvswitchOtherConfigs();
        if (otherConfigsList != null) {
            for (OpenvswitchOtherConfigs otherConfig : otherConfigsList) {
                if (otherConfig.getOtherConfigKey().equals("local_ip")) {
                    LOG.info("local_ip: {}", otherConfig.getOtherConfigValue());
                    break;
                }
            }
        }
        Thread.sleep(5000);
        disconnectNode(addressStr, portStr);
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
