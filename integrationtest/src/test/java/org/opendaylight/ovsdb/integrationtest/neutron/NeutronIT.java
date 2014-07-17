package org.opendaylight.ovsdb.integrationtest.neutron;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
import org.opendaylight.ovsdb.integrationtest.OvsdbIntegrationTestBase;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.neutron.Constants;
import org.opendaylight.ovsdb.neutron.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.neutron.api.ConfigurationService;
import org.opendaylight.ovsdb.neutron.api.NetworkingProvider;
import org.opendaylight.ovsdb.plugin.OvsdbConfigService;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;

import com.google.common.collect.ImmutableMap;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NeutronIT extends OvsdbIntegrationTestBase {
    private Logger log = LoggerFactory.getLogger(NeutronIT.class);
    @Inject
    private BundleContext bc;

    @Inject
    private OvsdbConfigService ovsdbConfigService;
    private Node node = null;

    Component of10Provider;
    Component of13Provider;

    @Inject
    BridgeConfigurationManager bridgeConfigurationManager;
    @Inject
    ConfigurationService configurationService;

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

                propagateSystemProperty("ovsdbserver.ipaddress"),
                propagateSystemProperty("ovsdbserver.port"),

                ConfigurationBundles.controllerBundles(),
                ConfigurationBundles.ovsdbLibraryBundles(),
                ConfigurationBundles.ovsdbDefaultSchemaBundles(),
                ConfigurationBundles.ovsdbNeutronBundles(),
                junitBundles()
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

        //Register fake NetworkingProviders
        Properties of10Properties = new Properties();
        of10Properties.put("openflowVersion", "OpenFlow10");

        Properties of13Properties = new Properties();
        of13Properties.put("openflowVersion", "OpenFlow13");

        DependencyManager dm = new DependencyManager(bc);

        of10Provider = dm.createComponent();
        of10Provider.setInterface(NetworkingProvider.class.getName(), of10Properties);
        of10Provider.setImplementation(new FakeOF10Provider());
        dm.add(of10Provider);

        of13Provider = dm.createComponent();
        of13Provider.setInterface(NetworkingProvider.class.getName(), of13Properties);
        of13Provider.setImplementation(new FakeOF13Provider());
        dm.add(of13Provider);
    }

    @Test
    public void testPrepareNode() throws Exception {
        Thread.sleep(5000);

        // Create the integration bridge
        bridgeConfigurationManager.prepareNode(node);

        Map<String, Row>
                bridgeRows =
                ovsdbConfigService.getRows(node, ovsdbConfigService.getTableName(node, Bridge.class));
        Assert.assertEquals(1, bridgeRows.size());

        Bridge bridgeRow = ovsdbConfigService.getTypedRow(node, Bridge.class, bridgeRows.values().iterator().next());
        Assert.assertEquals(configurationService.getIntegrationBridgeName(), bridgeRow.getName());

        String uuid = bridgeConfigurationManager.getBridgeUuid(node, configurationService.getIntegrationBridgeName());
        Assert.assertEquals(uuid, bridgeRow.getUuid().toString());

        tearDownBridge = true;
    }

    @Test
    public void testGetTunnelEndpoint() throws Exception {
        Thread.sleep(5000);

        final String endpointAddress = "10.10.10.10";

        Map<String, Row> ovsRows = ovsdbConfigService.getRows(node,
                                                              ovsdbConfigService.getTableName(node, OpenVSwitch.class));
        OpenVSwitch ovsRow = ovsdbConfigService.getTypedRow(node,
                                                            OpenVSwitch.class,
                                                            ovsRows.values().iterator().next());

        Assert.assertEquals(null, configurationService.getTunnelEndPoint(node));

        ovsRow.setOtherConfig(ImmutableMap.of(configurationService.getTunnelEndpointKey(), endpointAddress));
        ovsdbConfigService.updateRow(node,
                                     ovsdbConfigService.getTableName(node, OpenVSwitch.class),
                                     null,
                                     ovsRow.getUuid().toString(),
                                     ovsRow.getRow());

        Assert.assertEquals(InetAddress.getByName(endpointAddress), configurationService.getTunnelEndPoint(node));
    }

    @Test
    public void testGetOpenflowVersion() throws Exception {
        Thread.sleep(5000);
        // This will always return OPENFLOW13 as we have ovsdb.of.version hardcoded in config.ini
        // ToDo: Update the test when dynamic configuration is preferred
        Assert.assertEquals(Constants.OPENFLOW13, configurationService.getOpenflowVersion(node));
    }


    @After
    public void tearDown() throws InterruptedException {
        Thread.sleep(5000);

        if (tearDownBridge) {
            String uuid = bridgeConfigurationManager.getBridgeUuid(node,
                                                                   configurationService.getIntegrationBridgeName());
            ovsdbConfigService.deleteRow(node, ovsdbConfigService.getTableName(node, Bridge.class), uuid);
            tearDownBridge = false;
        }

        DependencyManager dm = new DependencyManager(bc);
        dm.remove(of10Provider);
        dm.remove(of13Provider);
    }

    private void printCache() throws Exception {
        List<String> tables = ovsdbConfigService.getTables(node);
        System.out.println("Tables = "+tables);
        assertNotNull(tables);
        for (String table : tables) {
            System.out.println("Table "+table);
            ConcurrentMap<String,Row> row = ovsdbConfigService.getRows(node, table);
            System.out.println(row);
        }
    }

    private class FakeOF10Provider implements NetworkingProvider {

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
