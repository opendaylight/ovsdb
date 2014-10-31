/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Flavio Fernandes
 */

package org.opendaylight.ovsdb.integrationtest.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
import org.opendaylight.ovsdb.integrationtest.OvsdbIntegrationTestBase;

// import org.junit.AfterClass;
// import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class UtilsConfigIT extends OvsdbIntegrationTestBase {
    private Logger log = LoggerFactory.getLogger(UtilsConfigIT.class);

    @Inject
    private BundleContext bc;

    private static final String TEST_PROPERTY_KEY = "foobar34465$3467";
    private static final String TEST_PROPERTY_VALUE = "foobar-value";
    private static final String DEFAULT_PROPERTY_VALUE = "xbar";

    // Configure the OSGi container
    @Configuration
    public Option[] config() {
        return options(
                // To start OSGi console for inspection remotely
                systemProperty("osgi.console").value("2401"),

                // Expected key value for test
                systemProperty(TEST_PROPERTY_KEY).value(TEST_PROPERTY_VALUE),

                propagateSystemProperty("ovsdbserver.ipaddress"),
                propagateSystemProperty("ovsdbserver.port"),

                ConfigurationBundles.ovsdbDefaultUtilsConfigBundles(),
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

        log.info("utilsConfig is started");
    }

    @Test
    public void testGetDefaultValue() {
        log.info("utilsConfig is testing");
    }

    @After
    public void tearDown() throws InterruptedException {
        log.info("utilsConfig is finished");
    }

}
