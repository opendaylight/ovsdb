/*
 * Copyright © 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.netvirtit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperties;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.itutils.ItConstants;
import org.opendaylight.ovsdb.utils.itutils.ItUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.neutron.utils.NeutronUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NetvirtIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtIT.class);
    private static final String FEATURE = "odl-netvirt-it";
    private static DataBroker dataBroker = null;
    private static ItUtils itUtils;
    private static String addressStr;
    private static String portStr;
    private static String connectionType;
    private static String controllerStr;
    private static AtomicBoolean setup = new AtomicBoolean(false);
    private static MdsalUtils mdsalUtils = null;
    private static Southbound southbound = null;
    private static SouthboundUtils southboundUtils;
    private static NeutronUtils neutronUtils = new NeutronUtils();

    @Override
    public String getModuleName() {
        return "netvirt-neutron";
    }

    @Override
    public String getInstanceName() {
        return "netvirt-neutron-default";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("features-ovsdb")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return FEATURE;
    }

    @Configuration
    @Override
    public Option[] config() {
        Option[] parentOptions = super.config();
        Option[] propertiesOptions = getPropertiesOptions();
        Option[] otherOptions = getOtherOptions();
        Option[] options = new Option[parentOptions.length + propertiesOptions.length + otherOptions.length];
        System.arraycopy(parentOptions, 0, options, 0, parentOptions.length);
        System.arraycopy(propertiesOptions, 0, options, parentOptions.length, propertiesOptions.length);
        System.arraycopy(otherOptions, 0, options, parentOptions.length + propertiesOptions.length,
                otherOptions.length);
        return options;
    }

    private Option[] getOtherOptions() {
        return new Option[] {
                wrappedBundle(
                        mavenBundle("org.opendaylight.ovsdb", "utils.mdsal-openflow")
                                .version(asInProject())
                                .type("jar")),
                configureConsole().startLocalConsole(),
                //vmOption("-verbose:class"),
                vmOption("-javaagent:../jars/org.jacoco.agent.jar=destfile=../../jacoco-it.exec"),
                keepRuntimeFolder()
        };
    }

    private Option[] getPropertiesOptions() {
        return new Option[] {
                propagateSystemProperties(ItConstants.SERVER_IPADDRESS, ItConstants.SERVER_PORT,
                        ItConstants.CONNECTION_TYPE, ItConstants.CONTROLLER_IPADDRESS,
                        ItConstants.USERSPACE_ENABLED),
        };
    }

    @Override
    public Option getLoggingOption() {
        return composite(
                editConfigurationFilePut(ItConstants.ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(NetvirtIT.class),
                        LogLevelOption.LogLevel.INFO.name()),
                super.getLoggingOption());
    }

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n"
                + "active connection : mvn -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"
                + "passive connection : mvn -Dovsdbserver.connection=passive verify\n";
    }

    private void getProperties() {
        Properties props = System.getProperties();
        addressStr = props.getProperty(ItConstants.SERVER_IPADDRESS);
        portStr = props.getProperty(ItConstants.SERVER_PORT, ItConstants.DEFAULT_SERVER_PORT);
        connectionType = props.getProperty(ItConstants.CONNECTION_TYPE, "active");
        controllerStr = props.getProperty(ItConstants.CONTROLLER_IPADDRESS, "0.0.0.0");
        String userSpaceEnabled = props.getProperty(ItConstants.USERSPACE_ENABLED, "no");
        LOG.info("setUp: Using the following properties: mode= {}, ip:port= {}:{}, controller ip: {}, " +
                        "userspace.enabled: {}",
                connectionType, addressStr, portStr, controllerStr, userSpaceEnabled);
        if (connectionType.equalsIgnoreCase(ItConstants.CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }
    }

    @Before
    @Override
    public void setup() throws InterruptedException {
        if (setup.get()) {
            LOG.info("Skipping setUp, already initialized");
            return;
        }

        try {
            super.setup();
        } catch (Exception e) {
            LOG.warn("Failed to setup test", e);
            fail("Failed to setup test: " + e);
        }

        getProperties();

        if (connectionType.equalsIgnoreCase(ItConstants.CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }

        dataBroker = ItUtils.getDatabroker(getProviderContext());
        itUtils = new ItUtils(dataBroker);
        mdsalUtils = new MdsalUtils(dataBroker);
        assertNotNull("mdsalUtils should not be null", mdsalUtils);
        assertTrue("Did not find " + ItConstants.NETVIRT_TOPOLOGY_ID, itUtils.getNetvirtTopology());
        southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        assertNotNull("southbound should not be null", southbound);
        southboundUtils = new SouthboundUtils(mdsalUtils);
        setup.set(true);
    }

    private BindingAwareBroker.ProviderContext getProviderContext() {
        BindingAwareBroker.ProviderContext providerContext = null;
        for (int i=0; i < 60; i++) {
            providerContext = getSession();
            if (providerContext != null) {
                break;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while waiting for provider context", e);
                }
            }
        }
        assertNotNull("providercontext should not be null", providerContext);
        /* One more second to let the provider finish initialization */
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for other provider", e);
        }
        return providerContext;
    }

    @Test
    public void testNetvirtFeatureLoad() {
        assertTrue("Feature " + FEATURE + " was not loaded", true);
    }
}
