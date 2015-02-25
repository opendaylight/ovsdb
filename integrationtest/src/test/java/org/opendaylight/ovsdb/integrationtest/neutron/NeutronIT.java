/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.integrationtest.neutron;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.neutron.neutron.spi.NeutronNetwork;
import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
import org.opendaylight.ovsdb.integrationtest.OvsdbIntegrationTestBase;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import java.util.Map;
import java.util.Properties;
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
    private OvsdbConfigurationService ovsdbConfigurationService;
    private Node node = null;

    Component of13Provider;

    @Inject
    BridgeConfigurationManager bridgeConfigurationManager;
    @Inject
    ConfigurationService netVirtConfigurationService;

    Boolean tearDownBridge = false;
    ImmutablePair<UUID, Map<String, String>> tearDownOpenVSwitchOtherConfig = null;

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
                ConfigurationBundles.ovsdbPluginBundles(),
                ConfigurationBundles.ovsdbNeutronBundles(),
                junitBundles()
        );
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
        Properties of13Properties = new Properties();
        of13Properties.put(Constants.OPENFLOW_VERSION_PROPERTY, Constants.OPENFLOW13);

        DependencyManager dm = new DependencyManager(bc);

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
                ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Bridge.class));
        Assert.assertEquals(1, bridgeRows.size());

        Bridge bridgeRow = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeRows.values().iterator().next());
        Assert.assertEquals(netVirtConfigurationService.getIntegrationBridgeName(), bridgeRow.getName());

        String uuid = bridgeConfigurationManager.getBridgeUuid(node, netVirtConfigurationService.getIntegrationBridgeName());
        Assert.assertEquals(uuid, bridgeRow.getUuid().toString());

        tearDownBridge = true;
    }

    @Test
    public void testGetTunnelEndpoint() throws Exception {
        Thread.sleep(5000);

        final String endpointAddress = "10.10.10.10";

        Map<String, Row> ovsRows = ovsdbConfigurationService.getRows(node,
                                                              ovsdbConfigurationService.getTableName(node, OpenVSwitch.class));
        OpenVSwitch ovsRow = ovsdbConfigurationService.getTypedRow(node,
                                                            OpenVSwitch.class,
                                                            ovsRows.values().iterator().next());

        Assert.assertEquals(null, netVirtConfigurationService.getTunnelEndPoint(node));
        final UUID originalVersion = ovsRow.getVersion();

        OpenVSwitch updateOvsRow = ovsdbConfigurationService.createTypedRow(node, OpenVSwitch.class);

        updateOvsRow.setOtherConfig(
                ImmutableMap.of(netVirtConfigurationService.getTunnelEndpointKey(), endpointAddress));

        ovsdbConfigurationService.updateRow(node,
                                            ovsdbConfigurationService.getTableName(node, OpenVSwitch.class),
                                            null,
                                            ovsRow.getUuid().toString(),
                                            updateOvsRow.getRow());

        // Remember original value so it can be restored on tearDown
        tearDownOpenVSwitchOtherConfig = ImmutablePair.of(ovsRow.getUuid(),
                                                          ovsRow.getOtherConfigColumn().getData());

        // Make sure tunnel end point was set
        Assert.assertEquals(InetAddress.getByName(endpointAddress), netVirtConfigurationService.getTunnelEndPoint(node));

        // Fetch rows again, and compare tunnel end point values
        ovsRows = ovsdbConfigurationService.getRows(node,
                                                    ovsdbConfigurationService.getTableName(node, OpenVSwitch.class));
        ovsRow = ovsdbConfigurationService.getTypedRow(node,
                                                       OpenVSwitch.class,
                                                       ovsRows.values().iterator().next());

        Assert.assertEquals(ovsRow.getOtherConfigColumn(), updateOvsRow.getOtherConfigColumn());

        // expect version of row to be changed, due to the update
        Assert.assertNotEquals(ovsRow.getVersion(), originalVersion);
    }

    @Test
    public void testGetOpenflowVersion() throws Exception {
        Thread.sleep(5000);

        Version ovsVersion = this.getOvsVersion();
        if (ovsVersion.compareTo(Constants.OPENFLOW13_SUPPORTED) >= 0) {
            Assert.assertEquals(Constants.OPENFLOW13, netVirtConfigurationService.getOpenflowVersion(node));
        }
    }

    @Test
    public void testGetDefaultGatewayMacAddress() throws Exception {
        // Thread.sleep(5000);
        String defaultGatewayMacAddress = netVirtConfigurationService.getDefaultGatewayMacAddress(node);

        if (defaultGatewayMacAddress != null) {
            String[] splits = defaultGatewayMacAddress.split(":");
            Assert.assertTrue("Unexpected mac format", splits.length == 6);
        }
        // log.info("testGetDefaultGatewayMacAddress got mac {}", defaultGatewayMacAddress);
    }

    @After
    public void tearDown() throws InterruptedException {
        Thread.sleep(5000);

        if (tearDownBridge) {
            try {
                String uuid = bridgeConfigurationManager.getBridgeUuid(node,
                                                                       netVirtConfigurationService.getIntegrationBridgeName());
                ovsdbConfigurationService.deleteRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), uuid);
            } catch (Exception e) {
                log.error("tearDownBridge Exception : " + e.getMessage());
            }
            tearDownBridge = false;
        }

        if (tearDownOpenVSwitchOtherConfig != null) {
            try {
                OpenVSwitch updateOvsRow = ovsdbConfigurationService.createTypedRow(node, OpenVSwitch.class);
                updateOvsRow.setOtherConfig(tearDownOpenVSwitchOtherConfig.getRight());
                ovsdbConfigurationService.updateRow(node,
                                                    ovsdbConfigurationService.getTableName(node, OpenVSwitch.class),
                                                    null,
                                                    tearDownOpenVSwitchOtherConfig.getLeft().toString(),
                                                    updateOvsRow.getRow());
            } catch (Exception e) {
                log.error("tearDownOpenVSwitchOtherConfig Exception : " + e.getMessage());
            }
            tearDownOpenVSwitchOtherConfig = null;
        }

        DependencyManager dm = new DependencyManager(bc);
        dm.remove(of13Provider);
    }

    private Version getOvsVersion(){
        Map<String, Row> ovsRows = ovsdbConfigurationService.getRows(node,
                                                              ovsdbConfigurationService.getTableName(node, OpenVSwitch.class));
        OpenVSwitch ovsRow = ovsdbConfigurationService.getTypedRow(node,
                                                            OpenVSwitch.class,
                                                            ovsRows.values().iterator().next());
        return Version.fromString(ovsRow.getOvsVersionColumn().getData().iterator().next());
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
