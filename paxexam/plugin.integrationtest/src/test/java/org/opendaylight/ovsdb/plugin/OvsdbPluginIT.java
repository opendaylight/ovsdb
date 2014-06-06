/*
 * Copyright (c) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.plugin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class OvsdbPluginIT extends OvsdbTestBase {
    private Logger log = LoggerFactory
        .getLogger(OvsdbPluginIT.class);
    // get the OSGI bundle context
    @Inject
    private BundleContext bc;
    private OVSDBConfigService ovsdbConfigService = null;
    private Node node = null;

    // Configure the OSGi container
    @Configuration
    public Option[] config() {
        return options(
            //
            systemProperty("logback.configurationFile").value(
                "file:" + PathUtils.getBaseDir()
                + "/src/test/resources/logback.xml"),
            // To start OSGi console for inspection remotely
            systemProperty("osgi.console").value("2401"),
            // Set the systemPackages (used by clustering)
            systemPackages("sun.reflect", "sun.reflect.misc", "sun.misc"),
            // List framework bundles
            mavenBundle("equinoxSDK381",
                        "org.eclipse.equinox.console").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.eclipse.equinox.util").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.eclipse.osgi.services").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.eclipse.equinox.ds").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.apache.felix.gogo.command").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.apache.felix.gogo.runtime").versionAsInProject(),
            mavenBundle("equinoxSDK381",
                        "org.apache.felix.gogo.shell").versionAsInProject(),
            // List logger bundles
            mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
            mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
            mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
            mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
            // List all the bundles on which the test case depends
            mavenBundle("org.opendaylight.ovsdb", "ovsdb").versionAsInProject(),
            mavenBundle("org.opendaylight.controller",
                        "clustering.services").versionAsInProject(),
            mavenBundle("org.opendaylight.controller",
                        "clustering.services-implementation").versionAsInProject(),
            mavenBundle("org.opendaylight.controller", "sal").versionAsInProject(),
            mavenBundle("org.opendaylight.controller",
                        "sal.implementation").versionAsInProject(),
            mavenBundle("org.opendaylight.controller", "sal.connection").versionAsInProject(),
            mavenBundle("org.opendaylight.controller",
                        "sal.connection.implementation").versionAsInProject(),
            mavenBundle("org.opendaylight.controller",
                        "sal.networkconfiguration").versionAsInProject(),
            mavenBundle("org.opendaylight.controller",
                        "sal.networkconfiguration.implementation").versionAsInProject(),
            mavenBundle("org.opendaylight.controller", "configuration").versionAsInProject(),
            mavenBundle("org.opendaylight.controller", "containermanager").versionAsInProject(),
            mavenBundle("org.opendaylight.controller",
                        "containermanager.it.implementation").versionAsInProject(),
            mavenBundle("com.fasterxml.jackson.core", "jackson-annotations").versionAsInProject(),
            mavenBundle("com.fasterxml.jackson.core", "jackson-core").versionAsInProject(),
            mavenBundle("com.fasterxml.jackson.core", "jackson-databind").versionAsInProject(),
            mavenBundle("io.netty", "netty-all").versionAsInProject(),
            mavenBundle("com.google.code.gson", "gson").versionAsInProject(),
            mavenBundle("org.jboss.spec.javax.transaction",
                        "jboss-transaction-api_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
            mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager").versionAsInProject(),
            mavenBundle("org.apache.felix",
                        "org.apache.felix.dependencymanager.shell").versionAsInProject(),
            mavenBundle("eclipselink", "javax.resource").versionAsInProject(),
            junitBundles());
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
    public void areWeReady() {
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
            log.debug("Do some debugging because some bundle is "
                      + "unresolved");
        }

        // Assert if true, if false we are good to go!
        assertFalse(debugit);
        try {
            Node node = getTestConnection();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertNull("Connection Failed", node);
        }

        this.ovsdbConfigService = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class,
                                                                                      this);
    }

    @Test
    public void tableTest() throws Exception {

        String container1 = "Container1";
        String container2 = "Container2";
        String cache1 = "Cache1";
        String cache2 = "Cache2";
        String cache3 = "Cache3";

        assertNull("Invalid Node", node);
        List<String> tables = ovsdbConfigService.getTables(node);
        assertNotNull(tables);
    }
}
