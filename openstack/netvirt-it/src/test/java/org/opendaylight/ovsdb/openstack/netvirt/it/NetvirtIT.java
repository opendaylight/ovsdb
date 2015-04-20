/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.it;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.impl.MdsalConsumerImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
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
public class NetvirtIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtIT.class);
    private static final String NETVIRT = "org.opendaylight.ovsdb.openstack.net-virt";
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
    private static MdsalUtils mdsalUtils = null;

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
                .artifactId("features-ovsdb")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-ovsdb-openstack";
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

        LOG.info("Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);

        Option[] options = new Option[] {
                editConfigurationFilePut(CUSTOM_PROPERTIES, SERVER_IPADDRESS, addressStr),
                editConfigurationFilePut(CUSTOM_PROPERTIES, SERVER_PORT, portStr),
                editConfigurationFilePut(CUSTOM_PROPERTIES, CONNECTION_TYPE, connectionType)
        };
        return options;
    }

    public void isBundleReady (BundleContext bc, String bundleName) throws InterruptedException {
        boolean ready = false;

        while (!ready) {
            int state = Bundle.UNINSTALLED;
            Bundle b[] = bc.getBundles();
            for (Bundle element : b) {
                if (element.getSymbolicName().equals(bundleName)) {
                    state = element.getState();
                    break;
                }
            }
            if (state != Bundle.ACTIVE) {
                LOG.info(">>>>> bundle not ready");
                Thread.sleep(30000);
            } else {
                ready = true;
            }
        }

        LOG.info(">>>>> {} is ready", bundleName);
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
        //Thread.sleep(3000);
        //dataBroker = SouthboundProvider.getDb();
        isBundleReady(bc, NETVIRT);
        Thread.sleep(30000);
        dataBroker = MdsalConsumerImpl.getDataBroker();
        Assert.assertNotNull("db should not be null", dataBroker);

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

        mdsalUtils = new MdsalUtils(dataBroker);
        setup = true;
    }

    @Test
    public void getDataBroker() throws InterruptedException {
        Assert.assertNotNull(dataBroker);
    }
}
