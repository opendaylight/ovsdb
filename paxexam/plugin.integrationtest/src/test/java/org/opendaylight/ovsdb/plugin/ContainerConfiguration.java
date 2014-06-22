/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.plugin;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

/**
 * Created by dave on 23/06/2014.
 */
public class ContainerConfiguration {
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
                mavenBundle("equinoxSDK381", "javax.servlet").versionAsInProject(),

                mavenBundle("com.google.guava",
                        "guava").versionAsInProject(),

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
                mavenBundle("io.netty", "netty-buffer").versionAsInProject(),
                mavenBundle("io.netty", "netty-common").versionAsInProject(),
                mavenBundle("io.netty", "netty-codec").versionAsInProject(),
                mavenBundle("io.netty", "netty-transport").versionAsInProject(),
                mavenBundle("io.netty", "netty-handler").versionAsInProject(),

                mavenBundle("org.apache.httpcomponents", "httpcore-nio").versionAsInProject(),
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
}
