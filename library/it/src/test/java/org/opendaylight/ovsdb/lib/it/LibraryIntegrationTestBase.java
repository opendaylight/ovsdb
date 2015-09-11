/*
 * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.it;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperties;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenUrlReference;

/**
 * Base class for library IT.
 */
public abstract class LibraryIntegrationTestBase extends AbstractMdsalTestBase {
    @Override
    public String getModuleName() {
        return "library";
    }

    @Override
    public String getInstanceName() {
        return "library-default";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("library-features")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-ovsdb-library";
    }

    @Configuration
    @Override
    public Option[] config() {
        Option[] parentOptions = super.config();
        Option[] propertiesOptions = getPropertiesOptions();
        Option[] options = new Option[parentOptions.length + propertiesOptions.length];
        System.arraycopy(parentOptions, 0, options, 0, parentOptions.length);
        System.arraycopy(propertiesOptions, 0, options, parentOptions.length, propertiesOptions.length);
        return options;
    }

    public Option[] getPropertiesOptions() {
        return new Option[] {
                propagateSystemProperties(
                        LibraryIntegrationTestUtils.SERVER_IPADDRESS,
                        LibraryIntegrationTestUtils.SERVER_PORT,
                        LibraryIntegrationTestUtils.CONNECTION_TYPE),
        };
    }

    @Override
    public Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                logConfiguration(getClass()),
                LogLevelOption.LogLevel.INFO.name());
        option = composite(option, super.getLoggingOption());
        return option;
    }
}
