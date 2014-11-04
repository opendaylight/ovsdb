/*
 *  Copyright (C) 2014 Red Hat, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 */
package org.opendaylight.ovsdb.integrationtest.utils;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
//import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
//import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;

@RunWith(PaxExam.class)
public class ConfigPropertiesIT {
    @Configuration
    public Option[] config() {
        return new Option[] {
                // Provision and launch a container based on a distribution of Karaf (Apache ServiceMix).
                karafDistributionConfiguration()
                        .frameworkUrl(
                                maven()
                                        .groupId("org.opendaylight.controller")
                                        .artifactId("distribution.opendaylight-karaf")
                                        .type("zip")
                                        .version("1.5.0-SNAPSHOT"))
                                        //.groupId("org.opendaylight.ovsdb")
                                        //.artifactId("distribution-karaf")
                                        //.type("tar.gz")
                                        //.version("1.3.0-SNAPSHOT"))
                        .name("OpenDaylight")
                        .unpackDirectory(new File("target/pax"))
                        .useDeployFolder(false),
                // It is really nice if the container sticks around after the test so you can check the contents
                // of the data directory when things go wrong.
                keepRuntimeFolder(),
                // Don't bother with local console output as it just ends up cluttering the logs
                configureConsole().ignoreLocalConsole(),
                // Force the log level to INFO so we have more details during the test.  It defaults to WARN.
                logLevel(LogLevel.WARN),
                // Remember that the test executes in another process.  If you want to debug it, you need
                // to tell Pax Exam to launch that process with debugging enabled.  Launching the test class itself with
                // debugging enabled (for example in Eclipse) will not get you the desired results.
                //debugConfiguration("5005", true),
                //features("mvn:org.opendaylight.controller/features-base/1.5.0-SNAPSHOT/xml/features",
                //        "odl-base-all")
        };
    }

    @Test
    public void testGetProperty ()  throws Exception {
        assertTrue(true);
    }
}
