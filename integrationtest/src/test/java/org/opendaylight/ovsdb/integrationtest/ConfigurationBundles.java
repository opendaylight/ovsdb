/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.integrationtest;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.opendaylight.controller.test.sal.binding.it.TestHelper;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;


public class ConfigurationBundles {

    public static Option controllerBundles() {
        return new DefaultCompositeOption(
                systemPackages("sun.reflect", "sun.reflect.misc", "sun.misc"),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("com.google.code.gson", "gson").versionAsInProject(),
                mavenBundle("com.google.guava", "guava").versionAsInProject(),
                mavenBundle("eclipselink", "javax.resource").versionAsInProject(),
                mavenBundle("equinoxSDK381", "javax.servlet").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.gogo.command").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.gogo.runtime").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.gogo.shell").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.console").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.ds").versionAsInProject(),
                mavenBundle("equinoxSDK381", "org.eclipse.equinox.util").versionAsInProject(),
                mavenBundle("equinoxSDK381","org.eclipse.osgi.services").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager.shell").versionAsInProject(),
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject()
        );
    }

        public static Option ovsdbLibraryBundles() {
        return new DefaultCompositeOption(
                mavenBundle("io.netty", "netty-buffer").versionAsInProject(),
                mavenBundle("io.netty", "netty-codec").versionAsInProject(),
                mavenBundle("io.netty", "netty-common").versionAsInProject(),
                mavenBundle("io.netty", "netty-handler").versionAsInProject(),
                mavenBundle("io.netty", "netty-transport").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.core", "jackson-annotations").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.core", "jackson-core").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.core", "jackson-databind").versionAsInProject(),
                mavenBundle("javax.portlet", "portlet-api").versionAsInProject(),
                mavenBundle("com.google.guava", "guava").versionAsInProject(),
                mavenBundle("org.opendaylight.ovsdb", "utils.servicehelper").versionAsInProject(),
                mavenBundle("org.opendaylight.ovsdb", "library").versionAsInProject()
        );
    }

    public static Option ovsdbPluginBundles() {
        return new DefaultCompositeOption(
                mavenBundle("org.opendaylight.ovsdb", "plugin").versionAsInProject(),
                mavenBundle("org.mockito", "mockito-all").versionAsInProject()
        );
    }

    public static Option ovsdbDefaultSchemaBundles() {
        return new DefaultCompositeOption(
                mavenBundle("org.opendaylight.ovsdb", "schema.openvswitch").versionAsInProject(),
                mavenBundle("org.opendaylight.ovsdb", "schema.hardwarevtep").versionAsInProject()
        );
    }

        public static Option mdsalBundles() {
        return new DefaultCompositeOption(
                TestHelper.configMinumumBundles(),
                TestHelper.baseModelBundles(),
                TestHelper.flowCapableModelBundles(),
                TestHelper.junitAndMockitoBundles(),
                TestHelper.bindingAwareSalBundles()
        );
    }
}
