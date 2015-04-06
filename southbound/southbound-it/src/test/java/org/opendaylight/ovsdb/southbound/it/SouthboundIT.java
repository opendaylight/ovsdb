/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test for
 * {@link org.opendaylight.ovsdb.southbound}
 *
 * @author Sam Hague (shague@redhat.com)
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SouthboundIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundIT.class);
    private static final String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    private static final String SERVER_PORT = "ovsdbserver.port";
    private static final String CONNECTION_TYPE = "ovsdbserver.connection";
    private static final String CONNECTION_TYPE_ACTIVE = "active";
    private static final String CONNECTION_TYPE_PASSIVE = "passive";
    private static final int CONNECTION_INIT_TIMEOUT = 10000;
    private static final String DEFAULT_SERVER_IPADDRESS = "127.0.0.1";
    private static final String DEFAULT_SERVER_PORT = "6640";
    private static Boolean writeStatus = false;
    private static Boolean readStatus = false;
    private static Boolean deleteStatus = false;
    private static DataBroker dataBroker = null;
    private static String addressStr;
    private static String portStr;
    private static String connectionType;
    private static Boolean setup = false;

    @Inject
    private BundleContext bc;

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
        return "odl-ovsdb-southbound-impl-ui";
    }

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n"
                + "active connection : mvn -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"
                + "passive connection : mvn -Dovsdbserver.connection=passive verify\n";
    }

    @Override
    public Option[] getPropertiesOptions() {
        Properties props = new Properties(System.getProperties());
        String addressStr = props.getProperty(SERVER_IPADDRESS, DEFAULT_SERVER_IPADDRESS);
        String portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(CONNECTION_TYPE, CONNECTION_TYPE_ACTIVE);

        LOG.info("1: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);

        Option[] options = new Option[] {
                editConfigurationFilePut(CUSTOM_PROPERTIES, SERVER_IPADDRESS, addressStr),
                editConfigurationFilePut(CUSTOM_PROPERTIES, SERVER_PORT, portStr),
                editConfigurationFilePut(CUSTOM_PROPERTIES, CONNECTION_TYPE, connectionType)
        };
        return options;
    }

    @Before
    public void setUp() {
        if (setup == true) {
            LOG.info("Skipping setUp, already initialized");
            return;
        }

        try {
            super.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        dataBroker = getSession().getSALService(DataBroker.class);

        addressStr = bc.getProperty(SERVER_IPADDRESS);
        portStr = bc.getProperty(SERVER_PORT);
        connectionType = bc.getProperty(CONNECTION_TYPE);

        LOG.info("Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);
        if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }

        setup = true;
    }

    @Test
    public void testPassiveNode() throws InterruptedException {
        if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_PASSIVE)) {
            //Wait for CONNECTION_INIT_TIMEOUT for the Passive connection to be initiated by the ovsdb-server.
            Thread.sleep(CONNECTION_INIT_TIMEOUT);
        }
    }

    @Test
    public void testAddRemoveOvsdbNode() throws InterruptedException {
        OvsdbClientKey ovsdbClientKey = getOvsdbClientKey(addressStr, portStr);
        DataBroker dataBroker = getSession().getSALService(DataBroker.class);

        // Write OVSDB node to configuration
        final ReadWriteTransaction configNodeTx = dataBroker.newReadWriteTransaction();
        configNodeTx.put(LogicalDatastoreType.CONFIGURATION, ovsdbClientKey.toInstanceIndentifier(),
                SouthboundMapper.createNode(ovsdbClientKey));
        Futures.addCallback(configNodeTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.info("success writing node to configuration: " + configNodeTx);
                writeStatus = true;
            }

            @Override
            public void onFailure(final Throwable throwable) {
                fail("failed writing node to configuration: " + configNodeTx);
            }
        });

        Thread.sleep(1000);

        assertTrue("Failed to write node to configuration", writeStatus);

        // Read from operational to verify if the OVSDB node is connected
        final ReadOnlyTransaction readNodeTx = dataBroker.newReadOnlyTransaction();
        ListenableFuture<Optional<Node>> dataFuture = readNodeTx.read(
                LogicalDatastoreType.OPERATIONAL, ovsdbClientKey.toInstanceIndentifier());
        Futures.addCallback(dataFuture, new FutureCallback<Optional<Node>>() {
            @Override
            public void onSuccess(final Optional<Node> result) {
                LOG.info("success reading node from operational: " + readNodeTx);
                LOG.info("Optional result: {}", result);
                if (result.isPresent()) {
                    LOG.info("node: {}", result.get());
                    readStatus = true;
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                fail("failed reading node from operational: " + readNodeTx);
            }
        });

        Thread.sleep(1000);

        assertTrue("Failed to read node from operational", readStatus);

        // Delete OVSDB node from configuration
        final ReadWriteTransaction deleteNodeTx = dataBroker.newReadWriteTransaction();
        deleteNodeTx.delete(LogicalDatastoreType.CONFIGURATION, ovsdbClientKey.toInstanceIndentifier());
        Futures.addCallback(deleteNodeTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.info("success deleting node from configuration: " + deleteNodeTx);
                deleteStatus = true;
            }

            @Override
            public void onFailure(final Throwable throwable) {
                fail("failed deleting node from configuration: " + deleteNodeTx);
            }
        });

        Thread.sleep(1000);

        assertTrue("Failed to delete node from configuration", deleteStatus);

        // Read from operational to verify if the OVSDB node is disconnected
        // Similar to the earlier read, but this time synchronously
        final ReadOnlyTransaction readNodeTx2 = dataBroker.newReadOnlyTransaction();
        Optional<Node> node = Optional.absent();
        try {
            node = readNodeTx2.read(LogicalDatastoreType.OPERATIONAL,
                    ovsdbClientKey.toInstanceIndentifier()).checkedGet();
            assertFalse("Failed to delete node from configuration and node is still connected",
                    node.isPresent());
        } catch (final ReadFailedException e) {
            LOG.debug("Read Operational/DS for Node fail! {}", ovsdbClientKey.toInstanceIndentifier(), e);
            fail("failed reading node from operational: " + readNodeTx2 + e);
        }
    }

    @Test
    public void testAddRemoveOvsdbNode2() throws InterruptedException {
        addNode("192.168.120.31", "6640");
        Thread.sleep(1000);
        Node node = readNode("192.168.120.31", "6640", LogicalDatastoreType.OPERATIONAL);
        assertNotNull(node);
        LOG.info("Connected node: {}", node);
        deleteNode("192.168.120.31", "6640");
        Thread.sleep(1000);
        node = readNode("192.168.120.31", "6640", LogicalDatastoreType.OPERATIONAL);
        assertNull(node);
    }

    private OvsdbClientKey getOvsdbClientKey(String addressStr, String portStr) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(addressStr);
        } catch (UnknownHostException e) {
            fail("Could not allocate InetAddress: " + e);
        }

        IpAddress address = SouthboundMapper.createIpAddress(inetAddress);
        PortNumber port = new PortNumber(Integer.parseInt(portStr));

        return new OvsdbClientKey(address, port);
    }

    private void addNode(String addressStr, String portStr) {
        OvsdbClientKey ovsdbClientKey = getOvsdbClientKey(addressStr, portStr);

        final ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, ovsdbClientKey.toInstanceIndentifier(),
                SouthboundMapper.createNode(ovsdbClientKey));
        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = rwTx.submit();
        try {
            commitFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            fail("Failed transaction: " + rwTx + e);
        }
    }

    private Node readNode(String addressStr, String portStr, LogicalDatastoreType type) {
        OvsdbClientKey ovsdbClientKey = getOvsdbClientKey(addressStr, portStr);

        final ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        Optional<Node> node = Optional.absent();
        CheckedFuture<Optional<Node>, ReadFailedException> read;
        read = rwTx.read(type, ovsdbClientKey.toInstanceIndentifier());
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
        OvsdbClientKey ovsdbClientKey = getOvsdbClientKey(addressStr, portStr);

        final ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        rwTx.delete(LogicalDatastoreType.CONFIGURATION, ovsdbClientKey.toInstanceIndentifier());
        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = rwTx.submit();
        try {
            commitFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            fail("Failed transaction: " + rwTx + e);
        }
    }
}
