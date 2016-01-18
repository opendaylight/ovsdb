/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperties;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.io.IOException;
import java.util.ArrayList;
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
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.standalone.openflow13.SfcClassifier;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.AclUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.ClassifierUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.NetvirtConfigUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.ServiceFunctionChainUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.ServiceFunctionForwarderUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.ServiceFunctionPathUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.ServiceFunctionUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.SfcConfigUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.SfcUtils;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChainBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwardersBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfig;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.of.renderer.rev151123.SfcOfRendererConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.providers.config.rev160109.NetvirtProvidersConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.providers.config.rev160109.NetvirtProvidersConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.ClassifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.ClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.SffsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.SffBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev150105.Sfc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev150105.SfcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NetvirtSfcIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcIT.class);
    private static AclUtils aclUtils = new AclUtils();
    private static ClassifierUtils classifierUtils = new ClassifierUtils();
    private static SfcUtils sfcUtils = new SfcUtils();
    private static ServiceFunctionUtils serviceFunctionUtils = new ServiceFunctionUtils();
    private static ServiceFunctionForwarderUtils serviceFunctionForwarderUtils = new ServiceFunctionForwarderUtils();
    private static ServiceFunctionChainUtils serviceFunctionChainUtils = new ServiceFunctionChainUtils();
    private static ServiceFunctionPathUtils serviceFunctionPathUtils = new ServiceFunctionPathUtils();
    private static SfcConfigUtils sfcConfigUtils = new SfcConfigUtils();
    private static NetvirtConfigUtils netvirtConfigUtils = new NetvirtConfigUtils();
    private static MdsalUtils mdsalUtils;
    private static AtomicBoolean setup = new AtomicBoolean(false);
    private static SouthboundUtils southboundUtils;
    private static String addressStr;
    private static String portStr;
    private static String connectionType;
    private static String controllerStr;
    private static boolean ovsdb_wait = false;
    private static String userSpaceEnabled = "no";
    private static PipelineOrchestrator pipelineOrchestrator;
    private static Southbound southbound;
    private static DataBroker dataBroker;
    public static final String CONTROLLER_IPADDRESS = "ovsdb.controller.address";
    public static final String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    public static final String SERVER_PORT = "ovsdbserver.port";
    public static final String CONNECTION_TYPE = "ovsdbserver.connection";
    public static final String CONNECTION_TYPE_ACTIVE = "active";
    public static final String CONNECTION_TYPE_PASSIVE = "passive";
    public static final String DEFAULT_SERVER_PORT = "6640";
    public static final String USERSPACE_ENABLED = "ovsdb.userspace.enabled";
    public static final String INTEGRATION_BRIDGE_NAME = "br-int";
    private static final String NETVIRT_TOPOLOGY_ID = "netvirt:1";
    private static final String OVSDB_TRACE = "ovsdb.trace";
    private static final String OVSDB_WAIT = "ovsdb.wait";
    private static final String SF1NAME = "firewall-72";
    private static final String SF2NAME = "dpi-72";
    private static final String SF1IP = "10.2.1.1";//"192.168.50.70";//"192.168.120.31";
    private static final String SF2IP = "10.2.1.2";
    private static final String SF1DPLNAME = "sf1";
    private static final String SF2DPLNAME = "sf2";
    // Use 192.168.50.70 when running against vagrant vm for workaround testing
    // "192.168.50.70"; "127.0.0.1"; "192.168.1.129";
    private static final String SFF1IP = "192.168.1.129";
    private static final String SFF2IP = "192.168.1.129";//"127.0.0.1";
    private static final String SFF1NAME = "sff1";
    private static final String SFF2NAME = "sff2";
    private static final String SFFDPL1NAME = "vxgpe";
    private static final String SFFDPL2NAME = "vxgpe";
    private static final String SN1NAME = "ovsdb1";
    private static final String SN2NAME = "ovsdb2";
    private static final String BRIDGE1NAME= "br-int";
    private static final String BRIDGE2NAME= "br-int";
    private static final String ACLNAME= "httpAcl";
    private static final String RULENAME= "httpRule";
    private static final String SFCNAME = "sfc1";
    private static final String SFCPATH = "SFC-Path";
    private static final String SFCSF1NAME = "firewall-abstract";
    private static final SftType SFCSF1TYPE = new SftType("firewall");
    private static final int GPEUDPPORT = 6633;

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
                propagateSystemProperties(SERVER_IPADDRESS, SERVER_PORT, CONNECTION_TYPE,
                        CONTROLLER_IPADDRESS, OVSDB_TRACE, OVSDB_WAIT, USERSPACE_ENABLED),
        };
    }

    @Override
    public Option getLoggingOption() {
        return composite(
                when(Boolean.getBoolean(OVSDB_TRACE)).useOptions(
                        editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                                "log4j.logger.org.opendaylight.ovsdb",
                                LogLevelOption.LogLevel.TRACE.name())),
                //editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                //        "log4j.logger.org.opendaylight.ovsdb",
                //        LogLevelOption.LogLevel.TRACE.name()),
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb.library",
                        LogLevel.INFO.name()),
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(NetvirtSfcIT.class),
                        LogLevel.INFO.name()),
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb.openstack.netvirt.sfc",
                        LogLevel.TRACE.name()),
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13",
                        LogLevel.TRACE.name()),
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.sfc",
                        LogLevel.TRACE.name()),
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
        controllerStr = props.getProperty(CONTROLLER_IPADDRESS, "0.0.0.0");
        userSpaceEnabled = props.getProperty(USERSPACE_ENABLED, "no");
        LOG.info("setUp: Using the following properties: mode= {}, ip:port= {}:{}, controller ip: {}, " +
                        "userspace.enabled: {}",
                connectionType, addressStr, portStr, controllerStr, userSpaceEnabled);
        if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }
        LOG.info("getProperties {}: {}", OVSDB_TRACE, props.getProperty(OVSDB_TRACE));
        LOG.info("getProperties {}: {}", OVSDB_WAIT, props.getProperty(OVSDB_WAIT));
        if (props.getProperty(OVSDB_WAIT) != null && props.getProperty(OVSDB_WAIT).equals("true")) {
            ovsdb_wait = true;
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
        pipelineOrchestrator =
                (PipelineOrchestrator) ServiceHelper.getGlobalInstance(PipelineOrchestrator.class, this);
        assertNotNull("pipelineOrchestrator should not be null", pipelineOrchestrator);
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

    private AccessListsBuilder accessListsBuilder() {
        String ruleName = RULENAME;
        String sfcName = SFCNAME;
        MatchesBuilder matchesBuilder = aclUtils.matchesBuilder(new MatchesBuilder(), 80);
        LOG.info("Matches: {}", matchesBuilder.build());
        ActionsBuilder actionsBuilder = aclUtils.actionsBuilder(new ActionsBuilder(), sfcName);
        AceBuilder accessListEntryBuilder =
                aclUtils.aceBuilder(new AceBuilder(), ruleName, matchesBuilder, actionsBuilder);
        AccessListEntriesBuilder accessListEntriesBuilder =
                aclUtils.accessListEntriesBuidler(new AccessListEntriesBuilder(), accessListEntryBuilder);
        AclBuilder accessListBuilder =
                aclUtils.aclBuilder(new AclBuilder(), ACLNAME, accessListEntriesBuilder);
        AccessListsBuilder accessListsBuilder =
                aclUtils.accesslistsbuilder(new AccessListsBuilder(), accessListBuilder);
        LOG.info("AccessLists: {}", accessListsBuilder.build());
        return accessListsBuilder;
    }

    @Test
    public void testAccessLists() throws InterruptedException {
        testModel(accessListsBuilder(), AccessLists.class, 0);
    }

    private ClassifiersBuilder classifiersBuilder() {
        SffBuilder sffBuilder = classifierUtils.sffBuilder(new SffBuilder(), SFF1NAME);
        SffsBuilder sffsBuilder = classifierUtils.sffsBuilder(new SffsBuilder(), sffBuilder);
        ClassifierBuilder classifierBuilder = classifierUtils.classifierBuilder(new ClassifierBuilder(),
                "classifierName", ACLNAME, sffsBuilder);
        ClassifiersBuilder classifiersBuilder = classifierUtils.classifiersBuilder(new ClassifiersBuilder(),
                classifierBuilder);
        LOG.info("Classifiers: {}", classifiersBuilder.build());
        return classifiersBuilder;
    }

    @Test
    public void testClassifiers() throws InterruptedException {
        testModel(classifiersBuilder(), Classifiers.class, 0);
    }

    private SfcBuilder netvirtSfcBuilder() {
        return sfcUtils.sfcBuilder(new SfcBuilder(), "sfc");
    }

    @Test
    public void testNetvirtSfcModel() throws InterruptedException {
        testModel(netvirtSfcBuilder(), Sfc.class, 0);
    }

    private <T extends DataObject> void testModelPut(Builder<T> builder, Class<T> clazz) {
        InstanceIdentifier<T> path = InstanceIdentifier.create(clazz);
        assertTrue(mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, path, builder.build()));
        T result = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNotNull(clazz.getSimpleName() + " should not be null", result);
    }

    private <T extends DataObject> void testModelDelete(Builder<T> builder, Class<T> clazz)
            throws InterruptedException {
        InstanceIdentifier<T> path = InstanceIdentifier.create(clazz);
        assertTrue("Failed to remove " + clazz.getSimpleName(),
                mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, path));
        T result = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNull(clazz.getSimpleName() + " should be null", result);
    }

    private <T extends DataObject> void testModel(Builder<T> builder, Class<T> clazz, long wait)
            throws InterruptedException {
        testModelPut(builder, clazz);
        Thread.sleep(wait);
        testModelDelete(builder, clazz);
    }

    private <T extends DataObject> void testModel(Builder<T> builder, Class<T> clazz)
            throws InterruptedException {
        testModelPut(builder, clazz);
        Thread.sleep(1000);
        testModelDelete(builder, clazz);
    }

    private ServiceFunctionsBuilder serviceFunctionsBuilder() {
        String sf1Name = SF1NAME;
        String sf1Ip = SF1IP;
        String sff1Ip = SF1IP;
        String sff1Name = SFF1NAME;
        String sf1DplName = SF1DPLNAME;
        String sn1Name = SN1NAME;
        String bridge1Name= BRIDGE1NAME;
        String sf2Name = SF2NAME;
        String sf2Ip = SF2IP;
        String sff2Ip = SF2IP;
        String sff2Name = SFF2NAME;
        String sf2DplName = SF2DPLNAME;
        String sn2Name = SN2NAME;
        String bridge2Name= BRIDGE2NAME;
        int port = GPEUDPPORT;

        ServiceFunctionBuilder serviceFunctionBuilder =
                serviceFunctionUtils.serviceFunctionBuilder(sf1Ip, port, sf1DplName, sff1Name, sf1Name);
        List<ServiceFunction> serviceFunctionList = serviceFunctionUtils.list(
                new ArrayList<ServiceFunction>(), serviceFunctionBuilder);

        //serviceFunctionBuilder =
        //        serviceFunctionUtils.serviceFunctionBuilder(sf2Ip, port, sffDpl2Name, sff2Name, sf2Name);
        //serviceFunctionList = serviceFunctionUtils.list(
        //        serviceFunctionList, serviceFunctionBuilder);

        ServiceFunctionsBuilder serviceFunctionsBuilder =
                serviceFunctionUtils.serviceFunctionsBuilder(new ServiceFunctionsBuilder(),
                        serviceFunctionList);
        LOG.info("ServiceFunctions: {}", serviceFunctionsBuilder.build());
        return serviceFunctionsBuilder;
    }

    private ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder() {
        String sf1Name = SF1NAME;
        String sf1Ip = SF1IP;
        String sf1DplName = SF1DPLNAME;
        String sff1Ip = SFF1IP;
        String sff1Name = SFF1NAME;
        String sffDpl1Name = SFFDPL1NAME;
        String sn1Name = SN1NAME;
        String bridge1Name= BRIDGE1NAME;
        String sf2Name = SF2NAME;
        String sf2Ip = SF2IP;
        String sff2Ip = SFF2IP;
        String sff2Name = SFF2NAME;
        String sffDpl2Name = SFFDPL2NAME;
        String sn2Name = SN2NAME;
        String bridge2Name= BRIDGE2NAME;
        String aclName = ACLNAME;
        int port = GPEUDPPORT;

        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder =
                serviceFunctionForwarderUtils.serviceFunctionForwarderBuilder(
                        sff1Name, sff1Ip, port, sffDpl1Name, sf1Ip, sn1Name, bridge1Name, sf1Name, sf1DplName);
        List<ServiceFunctionForwarder>  serviceFunctionForwarderList = serviceFunctionForwarderUtils.list(
                new ArrayList<ServiceFunctionForwarder>(), serviceFunctionForwarderBuilder);

        //serviceFunctionForwarderBuilder =
        //        serviceFunctionForwarderUtils.serviceFunctionForwarderBuilder(
        //                sff2Name, sff2Ip, port, sffDpl2Name, sf2Name, sff2Ip, sn2Name, bridge2Name, Dpi.class);
        //serviceFunctionForwarderList = serviceFunctionForwarderUtils.list(
        //        serviceFunctionForwarderList, serviceFunctionForwarderBuilder);

        ServiceFunctionForwardersBuilder serviceFunctionForwardersBuilder =
                serviceFunctionForwarderUtils.serviceFunctionForwardersBuilder(
                        new ServiceFunctionForwardersBuilder(), serviceFunctionForwarderList);
        LOG.info("ServiceFunctionForwarders: {}", serviceFunctionForwardersBuilder.build());
        return serviceFunctionForwardersBuilder;
    }

    private ServiceFunctionChainsBuilder serviceFunctionChainsBuilder() {
        String sf1Name = SFCSF1NAME;
        SftType sfType = SFCSF1TYPE;
        String sfcName = SFCNAME;

        SfcServiceFunctionBuilder sfcServiceFunctionBuilder = serviceFunctionChainUtils.sfcServiceFunctionBuilder(
                new SfcServiceFunctionBuilder(), sf1Name, sfType);
        List<SfcServiceFunction> sfcServiceFunctionList =
                serviceFunctionChainUtils.list(new ArrayList<SfcServiceFunction>(), sfcServiceFunctionBuilder);

        //sfcServiceFunctionBuilder = serviceFunctionChainUtils.sfcServiceFunctionBuilder(
        //        sfcServiceFunctionBuilder, sf2Name, Dpi.class);
        //sfcServiceFunctionList = serviceFunctionChainUtils.list(sfcServiceFunctionList, sfcServiceFunctionBuilder);

        ServiceFunctionChainBuilder serviceFunctionChainBuilder =
                serviceFunctionChainUtils.serviceFunctionChainBuilder(
                        new ServiceFunctionChainBuilder(), sfcName, false, sfcServiceFunctionList);
        ServiceFunctionChainsBuilder serviceFunctionChainsBuilder =
                serviceFunctionChainUtils.serviceFunctionChainsBuilder(
                        new ServiceFunctionChainsBuilder(),
                        serviceFunctionChainUtils.list(new ArrayList<ServiceFunctionChain>(),
                                serviceFunctionChainBuilder));
        LOG.info("ServiceFunctionChains: {}", serviceFunctionChainBuilder.build());
        return serviceFunctionChainsBuilder;
    }

    private ServiceFunctionPathsBuilder serviceFunctionPathsBuilder() {
        String sfpName = SFCPATH;
        String sfcName = SFCNAME;
        short startingIndex = 255;

        ServiceFunctionPathBuilder serviceFunctionPathBuilder =
                serviceFunctionPathUtils.serviceFunctionPathBuilder(
                        new ServiceFunctionPathBuilder(), sfpName, sfcName, startingIndex, false);
        ServiceFunctionPathsBuilder serviceFunctionPathsBuilder =
                serviceFunctionPathUtils.serviceFunctionPathsBuilder(
                        serviceFunctionPathUtils.list(new ArrayList<ServiceFunctionPath>(),
                                serviceFunctionPathBuilder));
        LOG.info("ServiceFunctionPaths: {}", serviceFunctionPathsBuilder.build());
        return serviceFunctionPathsBuilder;
    }

    private SfcOfRendererConfigBuilder sfcOfRendererConfigBuilder(short tableOffset, short egressTable) {
        SfcOfRendererConfigBuilder sfcOfRendererConfigBuilder =
                sfcConfigUtils.sfcOfRendererConfigBuilder(new SfcOfRendererConfigBuilder(), tableOffset, egressTable);
        LOG.info("SfcOfRendererConfig: {}", sfcOfRendererConfigBuilder.build());
        return sfcOfRendererConfigBuilder;
    }

    private NetvirtProvidersConfigBuilder netvirtProvidersConfigBuilder(short tableOffset) {
        NetvirtProvidersConfigBuilder netvirtProvidersConfigBuilder =
                netvirtConfigUtils.netvirtProvidersConfigBuilder(new NetvirtProvidersConfigBuilder(), tableOffset);
        LOG.info("NetvirtProvidersConfig: {}", netvirtProvidersConfigBuilder.build());
        return netvirtProvidersConfigBuilder;
    }

    @Test
    public void testSfcModel() throws InterruptedException {
        int timeout = 1000;
        testModel(serviceFunctionsBuilder(), ServiceFunctions.class, timeout);
        testModel(serviceFunctionForwardersBuilder(), ServiceFunctionForwarders.class, timeout);
        testModel(serviceFunctionChainsBuilder(), ServiceFunctionChains.class, timeout);
        testModel(serviceFunctionPathsBuilder(), ServiceFunctionPaths.class, timeout);
    }

    @Test
    public void testSfcModels() throws InterruptedException {
        testModel(serviceFunctionsBuilder(), ServiceFunctions.class);
        testModel(serviceFunctionForwardersBuilder(), ServiceFunctionForwarders.class);
        testModel(serviceFunctionChainsBuilder(), ServiceFunctionChains.class);
        testModel(serviceFunctionPathsBuilder(), ServiceFunctionPaths.class);

        testModel(accessListsBuilder(), AccessLists.class);
        testModel(classifiersBuilder(), Classifiers.class);
    }

    /**
     * Test that the NetvirtSfc SfcClassifierService is added to the Netvirt pipeline.
     * @throws InterruptedException
     */
    @Test
    public void testNetvirtSfcPipeline() throws InterruptedException {
        String bridgeName = INTEGRATION_BRIDGE_NAME;
        ConnectionInfo connectionInfo = SouthboundUtils.getConnectionInfo(addressStr, portStr);
        assertNotNull("connection failed", southboundUtils.connectOvsdbNode(connectionInfo));
        Node ovsdbNode = southboundUtils.getOvsdbNode(connectionInfo);
        assertNotNull("node is not connected", ovsdbNode);

        assertTrue("Controller " + SouthboundUtils.connectionInfoToString(connectionInfo)
                + " is not connected", isControllerConnected(connectionInfo));

        Node bridgeNode = southbound.getBridgeNode(ovsdbNode, bridgeName);
        assertNotNull("bridge " + bridgeName + " was not found", bridgeNode);
        long datapathId = southbound.getDataPathId(bridgeNode);
        String datapathIdString = southbound.getDatapathId(bridgeNode);
        LOG.info("testNetVirt: bridgeNode: {}, datapathId: {} - {}", bridgeNode, datapathIdString, datapathId);
        assertNotEquals("datapathId was not found", datapathId, 0);

        String flowId = "DEFAULT_PIPELINE_FLOW_" + Service.SFC_CLASSIFIER.getTable();
        verifyFlow(datapathId, flowId, Service.SFC_CLASSIFIER);

        readwait();

        assertTrue(southboundUtils.deleteBridge(connectionInfo, bridgeName));
        Thread.sleep(1000);
        assertTrue(southboundUtils.disconnectOvsdbNode(connectionInfo));
    }

    /**
     * Test the full NetvirtSfc functionality by creating everything needed to realize a chain and
     * then verify all flows have been created.
     * NOTE: This test requires an OVS with the NSH v8 patch, otherwise it will fail miserably.
     * @throws InterruptedException
     */
    @Test
    public void testNetvirtSfcAll() throws InterruptedException {
        if (userSpaceEnabled.equals("yes")) {
            LOG.info("testNetvirtSfcAll: skipping test because userSpaceEnabled {}", userSpaceEnabled);
            return;
        }

        short netvirtTableOffset = 1;
        testModelPut(netvirtProvidersConfigBuilder(netvirtTableOffset), NetvirtProvidersConfig.class);
        short sfcTableoffset = 150;
        short egressTable = pipelineOrchestrator.getTable(Service.SFC_CLASSIFIER);
        testModelPut(sfcOfRendererConfigBuilder(sfcTableoffset, egressTable), SfcOfRendererConfig.class);

        String bridgeName = INTEGRATION_BRIDGE_NAME;
        ConnectionInfo connectionInfo = SouthboundUtils.getConnectionInfo(addressStr, portStr);
        assertNotNull("connection failed", southboundUtils.connectOvsdbNode(connectionInfo));
        Node ovsdbNode = southboundUtils.getOvsdbNode(connectionInfo);
        assertNotNull("node is not connected", ovsdbNode);

        assertTrue("Controller " + SouthboundUtils.connectionInfoToString(connectionInfo)
                + " is not connected", isControllerConnected(connectionInfo));

        Node bridgeNode = southbound.getBridgeNode(ovsdbNode, bridgeName);
        assertNotNull("bridge " + bridgeName + " was not found", bridgeNode);
        long datapathId = southbound.getDataPathId(bridgeNode);
        String datapathIdString = southbound.getDatapathId(bridgeNode);
        LOG.info("testNetVirt: bridgeNode: {}, datapathId: {} - {}", bridgeNode, datapathIdString, datapathId);
        assertNotEquals("datapathId was not found", datapathId, 0);

        String flowId = "DEFAULT_PIPELINE_FLOW_" + pipelineOrchestrator.getTable(Service.SFC_CLASSIFIER);
        verifyFlow(datapathId, flowId, Service.SFC_CLASSIFIER);

        Map<String, String> externalIds = Maps.newHashMap();
        externalIds.put("attached-mac", "f6:00:00:0f:00:01");
        southboundUtils.addTerminationPoint(bridgeNode, SF1DPLNAME, "internal", null, externalIds);
        externalIds.clear();
        externalIds.put("attached-mac", "f6:00:00:0c:00:01");
        southboundUtils.addTerminationPoint(bridgeNode, "vm1", "internal");
        externalIds.clear();
        externalIds.put("attached-mac", "f6:00:00:0c:00:02");
        southboundUtils.addTerminationPoint(bridgeNode, "vm2", "internal");
        // SFC will add the SFF dpl port when creating the RSP
        /*Map<String, String> options = Maps.newHashMap();
        options.put("key", "flow");
        options.put("dst_port", String.valueOf(GPEUDPPORT));
        options.put("remote_ip", "flow");
        options.put("nshc1", "flow");
        options.put("nshc2", "flow");
        options.put("nsp", "flow");
        options.put("nsi", "flow");
        southboundUtils.addTerminationPoint(bridgeNode, "vxgpe", "vxlan", options, null);
        options.clear();
        options.put("key", "flow");
        options.put("remote_ip", "192.168.120.32");
        southboundUtils.addTerminationPoint(bridgeNode, "vx", "vxlan", options, null);*/

        testModelPut(serviceFunctionsBuilder(), ServiceFunctions.class);
        testModelPut(serviceFunctionForwardersBuilder(), ServiceFunctionForwarders.class);
        testModelPut(serviceFunctionChainsBuilder(), ServiceFunctionChains.class);
        testModelPut(serviceFunctionPathsBuilder(), ServiceFunctionPaths.class);

        testModelPut(accessListsBuilder(), AccessLists.class);
        testModelPut(classifiersBuilder(), Classifiers.class);

        long vxGpeOfPort = getOFPort(bridgeNode, "vxgpe");
        assertNotEquals("vxGpePort was not found", 0, vxGpeOfPort);

        readwait();

        flowId = "sfcIngressClass_" + "httpRule";
        verifyFlow(datapathId, flowId, Service.SFC_CLASSIFIER);
        // SFC is adding these flows now
        //flowId = "sfcTable_" + vxGpeOfPort;
        //verifyFlow(datapathId, flowId, Service.CLASSIFIER);
        //flowId = "sfEgress_" + GPEUDPPORT;
        //verifyFlow(datapathId, flowId, Service.SFC_CLASSIFIER);
        //flowId = "sfIngress_" + GPEUDPPORT + "_" + SF1IP;
        //verifyFlow(datapathId, flowId, Service.CLASSIFIER);
        flowId = "ArpResponder_" + SF1IP;
        verifyFlow(datapathId, flowId, Service.ARP_RESPONDER);

        readwait();

        assertTrue(southboundUtils.deleteBridge(connectionInfo, bridgeName));
        Thread.sleep(1000);
        assertTrue(southboundUtils.disconnectOvsdbNode(connectionInfo));
    }

    // Not used yet
    private void getSffDplPort(String rspName) {
        long ofPort = 0;
        RenderedServicePathKey renderedServicePathKey =
                new RenderedServicePathKey(RspName.getDefaultInstance(rspName));
        InstanceIdentifier<RenderedServicePath> path =
                InstanceIdentifier.create(RenderedServicePaths.class)
                        .child(RenderedServicePath.class, renderedServicePathKey);
        RenderedServicePath rsp = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, path);
        if (rsp == null) {
            LOG.warn("handleRenderedServicePath: RSP {} has empty hops!!", rsp.getName());
            return;
        }
        List<RenderedServicePathHop> pathHopList = rsp.getRenderedServicePathHop();
        if (pathHopList.isEmpty()) {
            LOG.warn("handleRenderedServicePath: RSP {} has empty hops!!", rsp.getName());
            return;
        }

        for (RenderedServicePathHop hop : pathHopList) {
        }
    }

    /**
     * Test the standalone NetvirtSfc implementation
     * NOTE: This test requires an OVS with the NSH v8 patch, otherwise it will fail miserably.
     * @throws InterruptedException
     */
    @Ignore
    @Test
    public void testStandalone() throws InterruptedException {
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
        assertTrue("Controller " + SouthboundUtils.connectionInfoToString(connectionInfo)
                + " is not connected", isControllerConnected(connectionInfo));

        Node bridgeNode = southbound.getBridgeNode(ovsdbNode, bridgeName);
        assertNotNull("bridge " + bridgeName + " was not found", bridgeNode);
        long datapathId = southbound.getDataPathId(bridgeNode);
        String datapathIdString = southbound.getDatapathId(bridgeNode);
        LOG.info("testNetVirt: bridgeNode: {}, datapathId: {} - {}", bridgeNode, datapathIdString, datapathId);
        assertNotEquals("datapathId was not found", datapathId, 0);

        SfcClassifier sfcClassifier = new SfcClassifier(dataBroker, southbound, mdsalUtils);
        //sfcClassifier.programLocalInPort(datapathId, "4096", (long)1, (short)0, (short)50, true);

        NshUtils nshUtils = new NshUtils(new Ipv4Address("192.168.50.71"), new PortNumber(6633),
                (long)10, (short)255, (long)4096, (long)4096);
        MatchesBuilder matchesBuilder = aclUtils.matchesBuilder(new MatchesBuilder(), 80);
        sfcClassifier.programSfcClassiferFlows(datapathId, (short)0, "test", matchesBuilder.build(),
                nshUtils, (long)2, true);

        //nshUtils = new NshUtils(null, null, (long)10, (short)253, 0, 0);
        //sfcClassifier.programEgressSfcClassiferFlows(datapathId, (short)0, "test", null,
        //        nshUtils, (long)2, (long)3, true);

        //try {
        //    System.in.read();
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}

        //NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(datapathId);
        //FlowBuilder flowBuilder = getLocalInPortFlow(datapathId, "4096", (long) 1,
        //                                             pipelineOrchestrator.getTable(Service.CLASSIFIER));
        //Flow flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.CONFIGURATION);
        //assertNotNull("Could not find flow in config", flow);
        //flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.OPERATIONAL);
        //assertNotNull("Could not find flow in operational", flow);

        //MatchBuilder matchBuilder = sfcClassifier.buildMatch(matchesBuilder.build());
        //NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(datapathId);
        //FlowBuilder flowBuilder = getSfcClassifierFlow(datapathId,
        //        pipelineOrchestrator.getTable(Service.CLASSIFIER), "test", null,
        //        nshUtils, (long) 2, matchBuilder);
        //Flow flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.CONFIGURATION);
        //assertNotNull("Could not find flow in config", flow);
        //flow = getFlow(flowBuilder, nodeBuilder, LogicalDatastoreType.OPERATIONAL);
        //assertNotNull("Could not find flow in operational", flow);

        //nodeBuilder = FlowUtils.createNodeBuilder(datapathId);
        //flowBuilder = getEgressSfcClassifierFlow(datapathId,
                                                   //pipelineOrchestrator.getTable(Service.CLASSIFIER),
                                                   //"test", nshUtils, (long) 2);
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
        assertNotNull("Could not find flow in operational: " + flowBuilder.build() + "--" + nodeBuilder.build(),
                flow);
    }

    private void verifyFlow(long datapathId, String flowId, Service service) throws InterruptedException {
        verifyFlow(datapathId, flowId, pipelineOrchestrator.getTable(service));
    }

    private void readwait() {
        if (ovsdb_wait) {
            LOG.warn("Waiting, kill with ps -ef | grep java, kill xxx... ");
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isControllerConnected(ConnectionInfo connectionInfo) throws InterruptedException {
        LOG.info("isControllerConnected enter");
        Boolean connected = false;
        ControllerEntry controllerEntry;
        Node ovsdbNode = southboundUtils.getOvsdbNode(connectionInfo);
        assertNotNull("ovsdb node not found", ovsdbNode);

        BridgeConfigurationManager bridgeConfigurationManager =
                (BridgeConfigurationManager) ServiceHelper.getGlobalInstance(BridgeConfigurationManager.class, this);
        assertNotNull("Could not find BridgeConfigurationManager Service", bridgeConfigurationManager);
        String controllerTarget = bridgeConfigurationManager.getControllersFromOvsdbNode(ovsdbNode).get(0);
        Assert.assertNotNull("Failed to get controller target", controllerTarget);

        for (int i = 0; i < 10; i++) {
            LOG.info("isControllerConnected try {}: looking for controller: {}", i, controllerTarget);
            OvsdbBridgeAugmentation bridge =
                    southboundUtils.getBridge(connectionInfo, INTEGRATION_BRIDGE_NAME);
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

    private long getOFPort(Node bridgeNode, String portName) {
        long ofPort = 0L;
        OvsdbTerminationPointAugmentation port =
                southbound.extractTerminationPointAugmentation(bridgeNode, portName);
        if (port != null) {
            ofPort = southbound.getOFPort(port);
        }
        if (ofPort == 0L) {
            for (int i = 0; i < 10; i++) {
                LOG.info("Looking for ofPort {}, try: {}", portName, i);
                TerminationPoint tp = southbound.readTerminationPoint(bridgeNode, null, portName);
                if (tp != null) {
                    port = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                    if (port != null) {
                        ofPort = southbound.getOFPort(port);
                        LOG.info("found ofPort {} - {}, try: {}", portName, ofPort, i);
                        break;
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LOG.error("Interrupted while waiting for ofPort {}", portName, e);
                }
            }
        }
        return ofPort;
    }
}
