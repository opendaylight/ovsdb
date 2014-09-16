/*
 *
 *  Copyright (C) 2014 Red Hat, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 * /
 */

package org.opendaylight.ovsdb.integrationtest.netvirt.providers;

import com.google.common.collect.ImmutableMap;
//import org.apache.felix.dm.Component;
//import org.apache.felix.dm.DependencyManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
import org.opendaylight.ovsdb.integrationtest.OvsdbIntegrationTestBase;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.Version;
//import org.opendaylight.ovsdb.openstack.netvirt.api.EgressAclProvider;
//import org.opendaylight.ovsdb.openstack.netvirt.api.IngressAclProvider;
//import org.opendaylight.ovsdb.openstack.netvirt.api.L2ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
//import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ClassifierProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
//import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
//import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.api.VlanConfigurationCache;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.MdsalConsumer;
//import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.OF13Provider;
//import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
//import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.ClassifierService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
//import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
//import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
//import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOptions;
import static org.ops4j.pax.exam.CoreOptions.when;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class Openflow13IT extends OvsdbIntegrationTestBase {
    private Logger log = LoggerFactory.getLogger(Openflow13IT.class);
    @Inject
    private BundleContext bc;

    @Inject
    private volatile NetworkingProviderManager networkingProviderManager;
    @Inject
    private volatile ConfigurationService netVirtConfigurationService;
    @Inject
    private volatile BridgeConfigurationManager bridgeConfigurationManager;
    @Inject
    private volatile SecurityServicesManager securityServicesManager;
    @Inject
    private volatile OvsdbConfigurationService ovsdbConfigurationService;
    @Inject
    private volatile OvsdbConnectionService ovsdbConnectionService;
    //@Inject
    //private volatile BindingAwareBroker broker;
    //@Inject
    //private volatile AbstractServiceInstance abstractServiceInstance;
    @Inject
    private volatile VlanConfigurationCache vlanConfigurationCache;
    @Inject
    private volatile MdsalConsumer mdsalConsumer;
    @Inject
    private volatile PipelineOrchestrator pipelineOrchestrator;
    @Inject
    private volatile ClassifierProvider classifierProvider;

    //@Inject
    //private volatile IngressAclProvider ingressAclProvider;
    //@Inject
    //private volatile EgressAclProvider egressAclProvider;
    //@Inject
    //private volatile L2ForwardingProvider l2ForwardingProvider;


/*
    @Inject
    private volatile TenantNetworkManager tenantNetworkManager;
    @Inject
    private volatile INeutronNetworkCRUD iNeutronNetworkCRUD;
    @Inject
    private volatile INeutronPortCRUD iNeutronPortCRUD;
*/
    //@Inject @Filter(timeout = 1000)
    //private ClassifierService classifierService;
    //@Inject
    //private NetworkingProviderManager networkingProvider;
    //private ClassifierService classifierProvider;
    //@Inject
    //private volatile OF13Provider of13Service = new OF13Provider();
    //private ClassifierService classifierService = new ClassifierService();

    //Component of10Provider;
    //Component of13Provider;
    //Component classifierProvider;

    private Node node = null;
    Boolean tearDownBridge = false;

    // Configure the OSGi container
    @Configuration
    public Option[] config() {
        return options(
                //
                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir()
                        + "/src/test/resources/logback.xml"
                ),
                // To start OSGi console for inspection remotely
                systemProperty("osgi.console").value("2401"),

                systemProperty("ovsdb.of.version").value("1.3"),

                //propagateSystemProperty("ovsdbserver.ipaddress"),
                //propagateSystemProperty("ovsdbserver.port"),

                ConfigurationBundles.controllerBundles(),
                ConfigurationBundles.ovsdbLibraryBundles(),
                ConfigurationBundles.ovsdbDefaultSchemaBundles(),
                ConfigurationBundles.ovsdbPluginBundles(),
                ConfigurationBundles.ovsdbNeutronBundles(),
                ConfigurationBundles.ovsdbNetVirtProvidersBundles(),
                junitBundles(),
                when( Boolean.getBoolean( "debug" ) ).useOptions(
                    vmOptions("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"))
        );
    }

    private String stateToString(int state) {
        switch (state) {
            case Bundle.ACTIVE:
                return "ACTIVE";
            case Bundle.INSTALLED:
                return "INSTALLED";
            case Bundle.RESOLVED:
                return "RESOLVED";
            case Bundle.UNINSTALLED:
                return "UNINSTALLED";
            default:
                return "Not CONVERTED";
        }
    }

    @Before
    public void areWeReady() throws InterruptedException, ExecutionException, IOException, TimeoutException {
        assertNotNull(bc);
        boolean debugit = false;
        Bundle b[] = bc.getBundles();
        for (Bundle element : b) {
            int state = element.getState();
            if (state != Bundle.ACTIVE && state != Bundle.RESOLVED) {
                log.info("Bundle:" + element.getSymbolicName() + " state:"
                         + stateToString(state));
                debugit = true;
            }
        }
        if (debugit) {
            log.debug("Do some debugging because some bundle is unresolved");
        }

        assertFalse(debugit);

        if (node == null) {
            try {
                node = getPluginTestConnection();
            } catch (Exception e) {
                fail("Exception : " + e.getMessage());
            }
        }

        log.info(">>>>> of13: {}", networkingProviderManager.getProvider(node).getName());
    }
/*
    private void startOF13Service () {
        DependencyManager dm = new DependencyManager(bc);

        //OF13Provider of13 = new OF13Provider();
        Properties of13Properties = new Properties();
        of13Properties.put(Constants.SOUTHBOUND_PROTOCOL_PROPERTY, "ovsdb");
        of13Properties.put(Constants.OPENFLOW_VERSION_PROPERTY, Constants.OPENFLOW13);
        of13Provider = dm.createComponent();
        of13Provider.setInterface(NetworkingProvider.class.getName(), of13Properties);

        of13Provider.add(dm.createServiceDependency().setService(BridgeConfigurationManager.class).setRequired(true));
        //of13Provider.add(dm.createServiceDependency().setService(TenantNetworkManager.class).setRequired(true));
        of13Provider.add(dm.createServiceDependency().setService(SecurityServicesManager.class).setRequired(true));
        of13Provider.add(dm.createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true));
        //of13Provider.add(dm.createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true));
        //of13Provider.add(dm.createServiceDependency().setService(MdsalConsumer.class).setRequired(true));
        //of13Provider.add(dm.createServiceDependency().setService(ClassifierProvider.class).setRequired(true));
        //of13Provider.add(dm.createServiceDependency().setService(IngressAclProvider.class).setRequired(true));
        //of13Provider.add(dm.createServiceDependency().setService(EgressAclProvider.class).setRequired(true));
        //of13Provider.add(dm.createServiceDependency().setService(L2ForwardingProvider.class).setRequired(true));
        of13Provider.add(dm.createServiceDependency().setService(ConfigurationService.class).setRequired(true));
        of13Provider.setImplementation(new OF13Provider());

        //of13Provider.setImplementation(of13Service);
        dm.add(of13Provider);
    }
*/
/*
    private void startClassifierService () {
        DependencyManager dm = new DependencyManager(bc);

        Properties properties = new Properties();
        properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.CLASSIFIER);
        properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
        classifierProvider = dm.createComponent();
        classifierProvider.setInterface(new String[]{AbstractServiceInstance.class.getName(),
                ClassifierProvider.class.getName()}, properties);
        ClassifierService classifierService = new ClassifierService();
        classifierProvider.setImplementation(classifierService);
        dm.add(classifierProvider);
        log.info(">>>>>classifier");
    }
*/
    @Test
    public void testPrepareNode() throws Exception {
        log.info("get ready...");
        Thread.sleep(1000);//5000);

        Thread.sleep(1000);//5000);
        log.info("too late...");
        // Create the integration bridge
        bridgeConfigurationManager.prepareNode(node);
        log.info("after prepareNode");
        Map<String, Row>
                bridgeRows =
                ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Bridge.class));
        Assert.assertEquals(1, bridgeRows.size());

        Bridge bridgeRow = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeRows.values().iterator().next());
        Assert.assertEquals(netVirtConfigurationService.getIntegrationBridgeName(), bridgeRow.getName());

        String uuid = bridgeConfigurationManager.getBridgeUuid(node, netVirtConfigurationService.getIntegrationBridgeName());
        Assert.assertEquals(uuid, bridgeRow.getUuid().toString());

        bridgeRow.setOtherConfig(ImmutableMap.of("hwaddr", "00:00:00:00:00:01"));

        //classifierService.programDropSrcIface(1L, 1L, true);
        tearDownBridge = true;
    }

    @Test
    public void testGetOpenflowVersion() throws Exception {
        Thread.sleep(1000);//5000);

        Version ovsVersion = this.getOvsVersion();
        log.info("ovsVersion: {}", ovsVersion);

        if (ovsVersion.compareTo(Constants.OPENFLOW13_SUPPORTED) < 0) {
            Assert.assertEquals(Constants.OPENFLOW10, netVirtConfigurationService.getOpenflowVersion(node));
            log.info("ovsVersion 1.0: {}", netVirtConfigurationService.getOpenflowVersion(node));
        } else {
            Assert.assertEquals(Constants.OPENFLOW13, netVirtConfigurationService.getOpenflowVersion(node));
            log.info("ovsVersion 1.3: {}", netVirtConfigurationService.getOpenflowVersion(node));
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        Thread.sleep(5000);

        if (tearDownBridge) {
            String uuid = bridgeConfigurationManager.getBridgeUuid(node,
                                                                   netVirtConfigurationService.getIntegrationBridgeName());
            ovsdbConfigurationService.deleteRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), uuid);
            tearDownBridge = false;
        }

        //DependencyManager dm = new DependencyManager(bc);
        //dm.remove(of10Provider);
        //dm.remove(of13Provider);
    }

    private Version getOvsVersion(){
        Map<String, Row> ovsRows = ovsdbConfigurationService.getRows(node,
                                                              ovsdbConfigurationService.getTableName(node, OpenVSwitch.class));
        OpenVSwitch ovsRow = ovsdbConfigurationService.getTypedRow(node,
                                                            OpenVSwitch.class,
                                                            ovsRows.values().iterator().next());
        return Version.fromString(ovsRow.getOvsVersionColumn().getData().iterator().next());
    }

    private class FakeOF10Provider implements NetworkingProvider {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean supportsServices() {
            return false;
        }

        @Override
        public boolean hasPerTenantTunneling() {
            return true;
        }

        @Override
        public Status handleInterfaceUpdate(String tunnelType, String tunnelKey) {
            return null;
        }

        @Override
        public Status handleInterfaceUpdate(NeutronNetwork network, Node source, Interface intf) {
            return null;
        }

        @Override
        public Status handleInterfaceDelete(String tunnelType, NeutronNetwork network, Node source, Interface intf,
                                            boolean isLastInstanceOnNode) {
            return null;
        }

        @Override
        public void initializeFlowRules(Node node) {

        }

        @Override
        public void initializeOFFlowRules(Node openflowNode) {

        }
    }

    private class FakeOF13Provider implements NetworkingProvider {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean supportsServices() {
            return false;
        }

        @Override
        public boolean hasPerTenantTunneling() {
            return false;
        }

        @Override
        public Status handleInterfaceUpdate(String tunnelType, String tunnelKey) {
            return null;
        }

        @Override
        public Status handleInterfaceUpdate(NeutronNetwork network, Node source, Interface intf) {
            return null;
        }

        @Override
        public Status handleInterfaceDelete(String tunnelType, NeutronNetwork network, Node source, Interface intf,
                                            boolean isLastInstanceOnNode) {
            return null;
        }

        @Override
        public void initializeFlowRules(Node node) {

        }

        @Override
        public void initializeOFFlowRules(Node openflowNode) {

        }
    }
}
