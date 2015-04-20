package org.opendaylight.ovsdb.integrationtest.mdsal;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.test.sal.binding.it.TestHelper;
import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class MdsalIT {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalIT.class);

    @Inject
    BundleContext ctx;

    @Inject
    BindingAwareBroker broker;

    @Configuration
    public Option[] config() {
        return options(
                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir()
                                + "/src/test/resources/logback.xml"
                ),
                // To start OSGi console for inspection remotely
                systemProperty("osgi.console").value("2401"),

                propagateSystemProperty("ovsdbserver.ipaddress"),
                propagateSystemProperty("ovsdbserver.port"),

                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("org.slf4j", "log4j-over-slf4j").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),

                TestHelper.junitAndMockitoBundles(),
                TestHelper.mdSalCoreBundles(),
                TestHelper.configMinumumBundles(),
                TestHelper.baseModelBundles()
        );
    }

    @Test
    public void test1() {
        LOG.info("We did it!");
    }
}
