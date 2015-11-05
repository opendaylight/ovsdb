/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import java.math.BigInteger;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13.NshUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13.SfcClassifier;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.AclUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.ClassifierUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.SfcUtils;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.oxm.container.match.entry.value.Nshc1CaseValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.ClassifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.ClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.SffsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.SffBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev150105.Sfc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev150105.SfcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NetvirtSfcIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcIT.class);
    private static AclUtils aclUtils = new AclUtils();
    private static ClassifierUtils classifierUtils = new ClassifierUtils();
    private static SfcUtils sfcUtils = new SfcUtils();
    private static MdsalUtils mdsalUtils;
    private static AtomicBoolean setup = new AtomicBoolean(false);
    private static SouthboundUtils southboundUtils;
    private static String addressStr;
    private static String portStr;
    private static String connectionType;
    private static Southbound southbound;
    private static DataBroker dataBroker;
    public static final String CONTROLLER_IPADDRESS = "ovsdb.controller.address";
    public static final String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    public static final String SERVER_PORT = "ovsdbserver.port";
    public static final String CONNECTION_TYPE = "ovsdbserver.connection";
    public static final String CONNECTION_TYPE_ACTIVE = "active";
    public static final String CONNECTION_TYPE_PASSIVE = "passive";
    public static final String DEFAULT_SERVER_PORT = "6640";
    public static final String INTEGRATION_BRIDGE_NAME = "br-int";
    private static final String NETVIRT_TOPOLOGY_ID = "netvirt:1";

    @Override
    public String getModuleName() {
        return "netvirt-sfc";
    }

    @Override
    public String getInstanceName() {
        return "netvirt-sfc-default";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("openstack.net-virt-sfc-features-test")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-ovsdb-sfc-test";
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
                configureConsole().startLocalConsole(),
                vmOption("-javaagent:../jars/org.jacoco.agent.jar=destfile=../../jacoco-it.exec"),
                        keepRuntimeFolder()
        };
    }

    public Option[] getPropertiesOptions() {
        return new Option[] {
                propagateSystemProperties(SERVER_IPADDRESS, SERVER_PORT, CONNECTION_TYPE, CONTROLLER_IPADDRESS),
        };
    }

    @Override
    public Option getLoggingOption() {
        return composite(
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(NetvirtSfcIT.class),
                        LogLevel.INFO.name()),
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb.openstack.netvirt.sfc",
                        LogLevel.TRACE.name()),
                /*editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb",
                        LogLevelOption.LogLevel.TRACE.name()),*/
                super.getLoggingOption());
    }

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n"
                + "active connection : mvn -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"
                + "passive connection : mvn -Dovsdbserver.connection=passive verify\n";
    }

    private void getProperties() {
        Properties props = System.getProperties();
        addressStr = props.getProperty(SERVER_IPADDRESS);
        portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        connectionType = props.getProperty(CONNECTION_TYPE, "active");
        LOG.info("setUp: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);
        if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }
    }

    @Before
    @Override
    public void setup() {
        if (setup.get()) {
            LOG.info("Skipping setUp, already initialized");
            return;
        }

        try {
            Thread.sleep(1000);
            super.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }

        getProperties();

        dataBroker = getDatabroker(getProviderContext());
        mdsalUtils = new MdsalUtils(dataBroker);
        assertNotNull("mdsalUtils should not be null", mdsalUtils);
        southboundUtils = new SouthboundUtils(mdsalUtils);
        assertTrue("Did not find " + NETVIRT_TOPOLOGY_ID, getNetvirtTopology());
        southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        assertNotNull("southbound should not be null", southbound);
        setup.set(true);
    }

    private ProviderContext getProviderContext() {
        ProviderContext providerContext = null;
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

    private DataBroker getDatabroker(ProviderContext providerContext) {
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

    @Test
    public void testNetvirtSfcFeatureLoad() {
        assertTrue(true);
    }

    private AccessListsBuilder setAccessLists () {
        MatchesBuilder matchesBuilder = aclUtils.createMatches(new MatchesBuilder(), 80);
        ActionsBuilder actionsBuilder = aclUtils.createActions(new ActionsBuilder(), Boolean.TRUE);
        AceBuilder accessListEntryBuilder = aclUtils.createAccessListEntryBuilder(
                new AceBuilder(), "http", matchesBuilder, actionsBuilder);
        AccessListEntriesBuilder accessListEntriesBuilder = aclUtils.createAccessListEntries(
                new AccessListEntriesBuilder(), accessListEntryBuilder);
        AclBuilder accessListBuilder = aclUtils.createAccessList(new AclBuilder(),
                "http", accessListEntriesBuilder);
        AccessListsBuilder accessListsBuilder = aclUtils.createAccessLists(new AccessListsBuilder(),
                accessListBuilder);
        LOG.info("AccessLists: {}", accessListsBuilder.build());
        return accessListsBuilder;
    }

    @Test
    public void testAccessLists() {
        testModel(setAccessLists(), AccessLists.class);
    }

    private ClassifiersBuilder setClassifiers() {
        SffBuilder sffBuilder = classifierUtils.createSff(new SffBuilder(), "sffname");
        SffsBuilder sffsBuilder = classifierUtils.createSffs(new SffsBuilder(), sffBuilder);
        ClassifierBuilder classifierBuilder = classifierUtils.createClassifier(new ClassifierBuilder(),
                "classifierName", "aclName", sffsBuilder);
        ClassifiersBuilder classifiersBuilder = classifierUtils.createClassifiers(new ClassifiersBuilder(),
                classifierBuilder);
        LOG.info("Classifiers: {}", classifiersBuilder.build());
        return classifiersBuilder;
    }

    @Test
    public void testClassifiers() {
        testModel(setClassifiers(), Classifiers.class);
    }

    private SfcBuilder setSfc() {
        return sfcUtils.createSfc(new SfcBuilder(), "sfc");
    }

    @Test
    public void testSfc() {
        testModel(setSfc(), Sfc.class);
    }

    private <T extends DataObject> void testModel(Builder<T> builder, Class<T> clazz) {
        InstanceIdentifier<T> path = InstanceIdentifier.create(clazz);
        assertTrue(mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, path, builder.build()));
        T result = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNotNull(clazz.getSimpleName() + " should not be null", result);
        assertTrue("Failed to remove " + clazz.getSimpleName(),
                mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, path));
        result = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNull(clazz.getSimpleName() + " should be null", result);
    }

    /*
     * Connect to an ovsdb node. Netvirt should add br-int, add the controller address
     * and program the pipeline flows.
     */
    @Test
    public void testDoIt() throws InterruptedException {
        String bridgeName = INTEGRATION_BRIDGE_NAME;
        ConnectionInfo connectionInfo = southboundUtils.getConnectionInfo(addressStr, portStr);
        assertNotNull("connection failed", southboundUtils.connectOvsdbNode(connectionInfo));
        Node ovsdbNode = southboundUtils.getOvsdbNode(connectionInfo);
        assertNotNull("node is not connected", ovsdbNode);
        ControllerEntry controllerEntry;
        // Loop 10s checking if the controller was added
        for (int i = 0; i < 10; i++) {
            ovsdbNode = southboundUtils.getOvsdbNode(connectionInfo);
            assertNotNull("ovsdb node not found", ovsdbNode);
            String controllerTarget = "tcp:192.168.50.1:6653";//SouthboundUtil.getControllerTarget(ovsdbNode);
            assertNotNull("Failed to get controller target", controllerTarget);
            OvsdbBridgeAugmentation bridge = southboundUtils.getBridge(connectionInfo, bridgeName);
            assertNotNull(bridge);
            assertNotNull(bridge.getControllerEntry());
            controllerEntry = bridge.getControllerEntry().iterator().next();
            assertEquals(controllerTarget, controllerEntry.getTarget().getValue());
            if (controllerEntry.isIsConnected()) {
                Assert.assertTrue(controllerEntry.isIsConnected());
                break;
            }
            Thread.sleep(1000);
        }

        /* TODO: add code to write to mdsal to exercise the sfc dataChangeListener */
        /* allow some time to let the impl code do it's work to push flows */
        /* or just comment out below lines and just manually verify on the bridges and reset them */
        //Thread.sleep(10000);

        assertTrue(southboundUtils.deleteBridge(connectionInfo, bridgeName));
        Thread.sleep(1000);
        assertTrue(southboundUtils.disconnectOvsdbNode(connectionInfo));
    }

    @Test
    public void testDemo() throws InterruptedException {
        for (DemoVm vm : demoVms){
            ConnectionInfo connectionInfo = southboundUtils.getConnectionInfo(vm.ipAddr, vm.ipPort);
            assertNotNull("connection failed", southboundUtils.connectOvsdbNode(connectionInfo));
            Node ovsdbNode = southboundUtils.getOvsdbNode(connectionInfo);
            assertNotNull("node is not connected", ovsdbNode);
            String controllerTarget = SouthboundUtil.getControllerTarget(ovsdbNode);
            assertNotNull("Failed to get controller target", controllerTarget);
            List<ControllerEntry> setControllerEntry = southboundUtils.createControllerEntry(controllerTarget);
            Uri setUri = new Uri(controllerTarget);
            assertTrue(southboundUtils.addBridge(connectionInfo, null, vm.name, null, true,
                    SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null,
                    setControllerEntry, null));

            for (int i = 0; i < 10; i++) {
                OvsdbBridgeAugmentation bridge = southboundUtils.getBridge(connectionInfo, vm.name);
                assertNotNull("bridge was not found: " + vm.name, bridge);
                assertNotNull("ControllerEntry was not found: "
                                + southboundUtils.createControllerEntry(controllerTarget),
                        bridge.getControllerEntry());
                List<ControllerEntry> getControllerEntries = bridge.getControllerEntry();
                for (ControllerEntry entry : getControllerEntries) {
                    if (entry.isIsConnected()) {
                        assertTrue(entry.isIsConnected());
                        break;
                    }
                }
                Thread.sleep(1000);
            }

            assertTrue(southboundUtils.deleteBridge(connectionInfo, vm.name));
            Thread.sleep(1000);
            assertTrue(southboundUtils.disconnectOvsdbNode(connectionInfo));
        }
    }

    private class DemoVm {
        String name;
        String ipAddr;
        String ipPort;

        DemoVm(String name, String ipAddr, String ipPort) {
            this.name = name;
            this.ipAddr = ipAddr;
            this.ipPort = ipPort;
        }
    }

    private DemoVm[] demoVms = {
            new DemoVm("sw1", "192.168.50.70", "6640"),
            //new DemoVm("sw2", "192.168.50.71", "6640"),
    };

    @Test
    public void testDoIt2() throws InterruptedException {
        String bridgeName = "sw1";
        ConnectionInfo connectionInfo = southboundUtils.getConnectionInfo(addressStr, portStr);
        assertNotNull("connection failed", southboundUtils.connectOvsdbNode(connectionInfo));
        Node ovsdbNode = southboundUtils.getOvsdbNode(connectionInfo);
        assertNotNull("node is not connected", ovsdbNode);
        String controllerTarget = "tcp:192.168.50.1:6653";
        List<ControllerEntry> setControllerEntry = southboundUtils.createControllerEntry(controllerTarget);
        Assert.assertTrue(southboundUtils.addBridge(connectionInfo, null, bridgeName, null, true,
                SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null,
                setControllerEntry, null, "00:00:00:00:00:00:00:01"));
        // Loop 10s checking if the controller was added
        for (int i = 0; i < 10; i++) {
            ovsdbNode = southboundUtils.getOvsdbNode(connectionInfo);
            assertNotNull("ovsdb node not found", ovsdbNode);
            assertNotNull("Failed to get controller target", controllerTarget);
            OvsdbBridgeAugmentation bridge = southboundUtils.getBridge(connectionInfo, bridgeName);
            assertNotNull(bridge);
            assertNotNull(bridge.getControllerEntry());
            ControllerEntry controllerEntry = bridge.getControllerEntry().iterator().next();
            assertEquals(controllerTarget, controllerEntry.getTarget().getValue());
            if (controllerEntry.isIsConnected()) {
                Assert.assertTrue(controllerEntry.isIsConnected());
                break;
            }
            Thread.sleep(1000);
        }

        Node bridgeNode = southbound.getBridgeNode(ovsdbNode, bridgeName);
        assertNotNull("bridge " + bridgeName + " was not found", bridgeNode);
        long datapathId = southbound.getDataPathId(bridgeNode);

        SfcClassifier sfcClassifier = new SfcClassifier(dataBroker, southbound, mdsalUtils);
        //sfcClassifier.programLocalInPort(datapathId, "4096", (long)1, (short)0, (short)50, true);

        NshUtils nshUtils = new NshUtils(new Ipv4Address("192.168.50.71"), new PortNumber(6633),
                (long)10, (short)255, (long)4096, (long)4096);
        MatchesBuilder matchesBuilder = aclUtils.createMatches(new MatchesBuilder(), 80);
        sfcClassifier.programSfcClassiferFlows(datapathId, (short)0, "test", matchesBuilder.build(),
                nshUtils, (long)2, true);

        nshUtils = new NshUtils(null, null, (long)10, (short)253, 0, 0);
        //sfcClassifier.programEgressSfcClassiferFlows(datapathId, (short)0, "test", null,
        //        nshUtils, (long)2, (long)3, true);

        //try {
        //    System.in.read();
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}

        /*NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(datapathId);
        FlowBuilder flowBuilder = getLocalInPortFlow(datapathId, "4096", (long) 1, (short) 0);
        Flow flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.CONFIGURATION);
        assertNotNull("Could not find flow in config", flow);
        flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.OPERATIONAL);
        assertNotNull("Could not find flow in operational", flow);*/

        MatchBuilder matchBuilder = sfcClassifier.buildMatch(matchesBuilder.build());
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(datapathId);
        FlowBuilder flowBuilder = getSfcClassifierFlow(datapathId, (short) 0, "test", null,
                nshUtils, (long) 2, matchBuilder);
        Flow flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.CONFIGURATION);
        assertNotNull("Could not find flow in config", flow);
        flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.OPERATIONAL);
        assertNotNull("Could not find flow in operational", flow);

        //nodeBuilder = FlowUtils.createNodeBuilder(datapathId);
        //flowBuilder = getEgressSfcClassifierFlow(datapathId, (short) 0, "test", nshUtils, (long) 2);
        //flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.CONFIGURATION);
        //assertNotNull("Could not find flow in config", flow);
        //flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.OPERATIONAL);
        //assertNotNull("Could not find flow in operational", flow);

        LOG.info("***** Go look for flows *****");
        Thread.sleep(30000);
        assertTrue(southboundUtils.deleteBridge(connectionInfo, bridgeName));
        Thread.sleep(1000);
        assertTrue(southboundUtils.deleteBridge(connectionInfo, INTEGRATION_BRIDGE_NAME));
        Thread.sleep(1000);
        assertTrue(southboundUtils.disconnectOvsdbNode(connectionInfo));
    }

    private FlowBuilder getLocalInPortFlow(long dpidLong, String segmentationId, long inPort, short writeTable) {
        MatchBuilder matchBuilder = new MatchBuilder();

        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(MatchUtils.createInPortMatch(matchBuilder, dpidLong, inPort).build());
        String flowId = "sfcIngress_" + segmentationId + "_" + inPort;
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        return flowBuilder;
    }

    public FlowBuilder getSfcClassifierFlow(long dpidLong, short writeTable, String ruleName, Matches match,
                                             NshUtils nshHeader, long tunnelOfPort, MatchBuilder matchBuilder) {
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setMatch(matchBuilder.build());

        String flowId = "sfcClass_" + ruleName + "_" + nshHeader.getNshNsp();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        return flowBuilder;
    }

    private FlowBuilder getEgressSfcClassifierFlow(long dpidLong, short writeTable, String ruleName,
                                                   NshUtils nshHeader, long tunnelOfPort) {
        FlowBuilder flowBuilder = new FlowBuilder();

        MatchBuilder matchBuilder = new MatchBuilder();
        flowBuilder.setMatch(MatchUtils.createInPortMatch(matchBuilder, dpidLong, tunnelOfPort).build());
        flowBuilder.setMatch(
                MatchUtils.createTunnelIDMatch(matchBuilder, BigInteger.valueOf(nshHeader.getNshNsp())).build());
        flowBuilder.setMatch(MatchUtils.addNxNspMatch(matchBuilder, nshHeader.getNshNsp()).build());
        flowBuilder.setMatch(MatchUtils.addNxNsiMatch(matchBuilder, nshHeader.getNshNsi()).build());

        String flowId = "egressSfcClass_" + ruleName + "_" + nshHeader.getNshNsp() + "_" + nshHeader.getNshNsi();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        return flowBuilder;
    }

    private Flow getFlow (FlowBuilder flowBuilder, NodeBuilder nodeBuilder, LogicalDatastoreType store)
            throws InterruptedException {
        Flow flow = null;
        for (int i = 0; i < 10; i++) {
            flow = FlowUtils.getFlow(flowBuilder, nodeBuilder, dataBroker.newReadOnlyTransaction(), store);
            if (flow != null) {
                LOG.info("flow({}): {}", store, flow);
                break;
            }
            Thread.sleep(1000);
        }
        return flow;
    }
}
