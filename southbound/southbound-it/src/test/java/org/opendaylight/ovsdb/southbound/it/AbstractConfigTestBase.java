/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.it;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import com.google.common.collect.ObjectArrays;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Calendar;

import javax.management.InstanceNotFoundException;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.opendaylight.controller.config.api.ConfigRegistry;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConfigTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigTestBase.class);

    /*
     * Wait up to 10s for our configured module to come up
     */
    private static final int MODULE_TIMEOUT = 10000;

    public abstract String getModuleName();

    public abstract String getInstanceName();

    public abstract MavenUrlReference getFeatureRepo();

    public abstract String getFeatureName();

    public Option[] getLoggingOptions() {
        Option[] options = new Option[] {
                editConfigurationFilePut(SouthboundITConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(AbstractConfigTestBase.class),
                        LogLevel.INFO.name())
        };
        return options;
    }

    public String logConfiguration(Class<?> klazz) {
        return "log4j.logger." + klazz.getPackage().getName();
    }

    public Option[] getPropertiesOptions() {
        return null;
    }

    public MavenArtifactUrlReference getKarafDistro() {
        MavenArtifactUrlReference karafUrl = maven()
                .groupId("org.opendaylight.controller")
                .artifactId("opendaylight-karaf-empty")
                .version("1.5.0-SNAPSHOT")
                .type("zip");
        return karafUrl;
    }

    @Configuration
    public Option[] config() {
        Option[] options = new Option[] {
                // KarafDistributionOption.debugConfiguration("5005", true),
                karafDistributionConfiguration()
                        .frameworkUrl(getKarafDistro())
                        .unpackDirectory(new File("target/exam"))
                        .useDeployFolder(false),
                keepRuntimeFolder(),
                features(getFeatureRepo() , getFeatureName()),
        };
        options = ObjectArrays.concat(options, getLoggingOptions(), Option.class);
        options = ObjectArrays.concat(options, getPropertiesOptions(), Option.class);
        return options;
    }

    public void setup() throws Exception {
        LOG.info("Module: {} Instance: {} attempting to configure.",
                getModuleName(),getInstanceName());
        Calendar start = Calendar.getInstance();
        ConfigRegistry configRegistryClient = new ConfigRegistryJMXClient(ManagementFactory
                .getPlatformMBeanServer());
        for (int timer = 0;timer < MODULE_TIMEOUT;timer++) {
            try {
                configRegistryClient.lookupConfigBean(getModuleName(), getInstanceName());
                Thread.sleep(1);
            } catch (InstanceNotFoundException e) {
                if (timer < MODULE_TIMEOUT) {
                    continue;
                } else {
                    throw e;
                }
            } catch (InterruptedException e) {
                LOG.error("Exception: ",e);
            }
        }
        Calendar stop = Calendar.getInstance();
        LOG.info("Module: {} Instance: {} configured after {} ms",
                getModuleName(),getInstanceName(),
                stop.getTimeInMillis() - start.getTimeInMillis());
    }

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info("TestWatcher: Starting test: {}",
                    description.getDisplayName());
        }

        @Override
        protected void finished(Description description) {
            LOG.info("TestWatcher: Finished test: {}", description.getDisplayName());
        }
    };
}
