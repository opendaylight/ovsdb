package org.opendaylight.ovsdb.integrationtest.netvirt;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.useOwnExamBundlesStartLevel;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.integrationtest.OvsdbIntegrationTestBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class NetVirtIT extends OvsdbIntegrationTestBase {
    private Logger LOG = LoggerFactory.getLogger(NetVirtIT.class);
    private static final String NETVIRT = "org.opendaylight.ovsdb.openstack.net-virt-providers";

    @Inject
    private BundleContext bc;

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(
            Constants.IMPORT_PACKAGE,
                "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

    @Configuration
    public Option[] config() {
        return new Option[] {
                // Provision and launch a container based on a distribution of Karaf (Apache ServiceMix).
                karafDistributionConfiguration()
                        .frameworkUrl(
                                maven()
                                        .groupId("org.opendaylight.ovsdb")
                                        .artifactId("distribution-karaf")
                                        .type("tar.gz")
                                        .version("1.3.0-SNAPSHOT"))
                        .name("OpenDaylight")
                        .unpackDirectory(new File("target/pax"))
                        .useDeployFolder(false),
                // It is really nice if the container sticks around after the test so you can check the contents
                // of the data directory when things go wrong.
                keepRuntimeFolder(),
                // Don't bother with local console output as it just ends up cluttering the logs
                configureConsole().ignoreLocalConsole(),
                // Force the log level to INFO so we have more details during the test.  It defaults to WARN.
                logLevel(LogLevel.INFO),
                // Remember that the test executes in another process.  If you want to debug it, you need
                // to tell Pax Exam to launch that process with debugging enabled.  Launching the test class itself with
                // debugging enabled (for example in Eclipse) will not get you the desired results.
                //debugConfiguration("5005", true),
                features("mvn:org.opendaylight.controller/features-base/1.5.0-SNAPSHOT/xml/features",
                       "odl-base-all"),
                features("mvn:org.opendaylight.neutron/features-neutron/0.5.0-SNAPSHOT/xml/features",
                        "odl-neutron-service"),
                features("mvn:org.opendaylight.ovsdb/features-ovsdb/1.1.0-SNAPSHOT/xml/features",
                        "odl-ovsdb-openstack"),

                useOwnExamBundlesStartLevel(4)
        };
    }

    @Before
    public void setUp () throws ExecutionException, InterruptedException, IOException, InvalidSyntaxException {
        areWeReady(bc);

        // TODO: this is where we should connect, but it fails currently because of
        // dependency issues.
        try {
           Node node = getPluginTestConnection();
        } catch (Exception e) {
            Assert.fail("Exception : " + e.getMessage());
        }
        isBundleReady(bc, NETVIRT);
        // To be certain that all the bundles are Active sleep some more.
        Thread.sleep(5000);
    }

    @Test
    public void testGetProperty ()  throws Exception {
        LOG.info(">>>>> We did it! Try to connect!");
        LOG.info(">>>>> We did it! Try to connect!");
        LOG.info(">>>>> We did it! Try to connect!");
        Thread.sleep(10000);
    }

    /**
     * isBundleReady is used to check if the requested bundle is Active
     */
    public void isBundleReady (BundleContext bc, String bundleName) throws InterruptedException {
        boolean ready = false;

        while (!ready) {
            int state = Bundle.UNINSTALLED;
            Bundle b[] = bc.getBundles();
            for (Bundle element : b) {
                if (element.getSymbolicName().equals(bundleName)) {
                    state = element.getState();
                    break;
                }
            }
            if (state != Bundle.ACTIVE) {
                LOG.info(">>>>> bundle not ready");
                Thread.sleep(5000);
            } else {
                ready = true;
            }
        }
    }
}
