/*
 *  Copyright (C) 2014 Red Hat, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 */
package org.opendaylight.ovsdb.integrationtest.utils.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
//import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
//import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
//import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
//import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
//import org.ops4j.pax.exam.ProbeBuilder;
//import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
//import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class ConfigPropertiesIT {
    private Logger log = LoggerFactory.getLogger(ConfigPropertiesIT.class);
    private static final String TEST_PROPERTY_KEY = "foobar34465$3467";
    private static final String DEFAULT_PROPERTY_VALUE = "xbar";

    @Inject
    private BundleContext bc;

    //@Inject
    //private OvsdbConfigurationService ovsdbConfigurationService;

    //@Inject
    //BridgeConfigurationManager bridgeConfigurationManager;
    //@Inject
    //ConfigurationService netVirtConfigurationService;

    //@Inject
    //ConfigProperties configProperties;

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

                //ConfigurationBundles.controllerBundles(),
                //systemPackages("sun.reflect", "sun.reflect.misc", "sun.misc"),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                //mavenBundle("com.google.code.gson", "gson").versionAsInProject(),
                //mavenBundle("com.google.guava", "guava").versionAsInProject(),
                //mavenBundle("eclipselink", "javax.resource").versionAsInProject(),
                //mavenBundle("equinoxSDK381", "javax.servlet").versionAsInProject(),
                //mavenBundle("equinoxSDK381", "org.apache.felix.gogo.command").versionAsInProject(),
                //mavenBundle("equinoxSDK381", "org.apache.felix.gogo.runtime").versionAsInProject(),
                //mavenBundle("equinoxSDK381", "org.apache.felix.gogo.shell").versionAsInProject(),
                //mavenBundle("equinoxSDK381", "org.eclipse.equinox.console").versionAsInProject(),
                //mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds").versionAsInProject(),
                //mavenBundle("equinoxSDK381", "org.eclipse.equinox.util").versionAsInProject(),
                //mavenBundle("equinoxSDK381","org.eclipse.osgi.services").versionAsInProject(),
                //mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                //mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager").versionAsInProject(),
                //mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager.shell").versionAsInProject(),
                //mavenBundle("org.jboss.spec.javax.transaction", "jboss-transaction-api_1.1_spec").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                //mavenBundle("org.opendaylight.ovsdb", "utils.config").versionAsInProject(),
                //mavenBundle("org.opendaylight.ovsdb", "plugin").versionAsInProject(),
                //ConfigurationBundles.ovsdbLibraryBundles(),
                //ConfigurationBundles.ovsdbDefaultSchemaBundles(),
                //ConfigurationBundles.controllerBundles(),
                //ConfigurationBundles.ovsdbLibraryBundles(),
                //ConfigurationBundles.ovsdbDefaultSchemaBundles(),
                //ConfigurationBundles.ovsdbPluginBundles(),
                //ConfigurationBundles.ovsdbNeutronBundles(),
                //systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
                //mavenBundle("org.opendaylight.ovsdb", "utils.config").versionAsInProject(),
                bundle("file:/home/shague/git/ovsdb/utils/config/target/utils.config-1.1.0-SNAPSHOT.jar"),

                junitBundles()
        );
    }

    //@ProbeBuilder
    //public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
    //    //probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*;status=provisional");
    //    probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "org.opendaylight.ovsdb.utils.config");
    //    return probe;
    //}

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

        log.info("ConfigPropertiesIT is ready");
    }

    public void printClassPath () {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader)cl).getURLs();

        for(URL url: urls){
            System.out.println(url.getFile());
        }
    }

    @Test
    public void testGetProperty() {
        printClassPath();
        final String value1 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY);
        final String value2 = ConfigProperties.getProperty(this.getClass(), TEST_PROPERTY_KEY,
                DEFAULT_PROPERTY_VALUE);
        assertNull(value1);
        assertEquals(value2, DEFAULT_PROPERTY_VALUE);
    }

    @After
    public void tearDown() throws InterruptedException {
        log.info("utilsConfig is finished");
    }
}
