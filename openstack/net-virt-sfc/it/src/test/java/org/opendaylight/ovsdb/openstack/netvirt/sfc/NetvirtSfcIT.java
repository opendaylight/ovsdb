/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperties;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.AclUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.ClassifierUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.SfcUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.AccessListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.AccessListEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.Classifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.ClassifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.ClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.SffsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.SffBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev150105.Sfc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev150105.SfcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NetvirtSfcIT extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcIT.class);
    private static AclUtils aclUtils = new AclUtils();
    private static ClassifierUtils classifierUtils = new ClassifierUtils();
    private static SfcUtils sfcUtils = new SfcUtils();
    private static MdsalUtils mdsalUtils;
    private static AtomicBoolean setup = new AtomicBoolean(false);
    private static SouthboundUtils southboundUtils;
    private static String addressStr;
    private static String portStr;
    private static String connectionType;
    public static final String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    public static final String SERVER_PORT = "ovsdbserver.port";
    public static final String CONNECTION_TYPE = "ovsdbserver.connection";
    public static final String CONNECTION_TYPE_ACTIVE = "active";
    public static final String CONNECTION_TYPE_PASSIVE = "passive";
    public static final String DEFAULT_SERVER_PORT = "6640";

    @Override
    public String getModuleName() {
        return "netvirt-sfc";
    }

    @Override
    public String getInstanceName() {
        return "netvirt-sfc-default";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("openstack.net-virt-sfc-features")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-ovsdb-sfc-ui";
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
                vmOption("-javaagent:../jars/org.jacoco.agent.jar=destfile=../../jacoco-it.exec"),
                keepRuntimeFolder()
        };
    }

    public Option[] getPropertiesOptions() {
        return new Option[] {
                propagateSystemProperties(SERVER_IPADDRESS, SERVER_PORT, CONNECTION_TYPE),
        };
    }

    @Override
    public Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                logConfiguration(NetvirtSfcIT.class),
                LogLevel.INFO.name());
        option = composite(option, super.getLoggingOption());
        return option;
    }

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n"
                + "active connection : mvn -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"
                + "passive connection : mvn -Dovsdbserver.connection=passive verify\n";
    }

    private void getProperties() {
        Properties props = System.getProperties();
        addressStr = props.getProperty(SERVER_IPADDRESS);
        portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        connectionType = props.getProperty(CONNECTION_TYPE, "active");
        LOG.info("setUp: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);
        if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }
        }
    }

    @Before
    @Override
    public void setup() {
        if (setup.get()) {
            LOG.info("Skipping setUp, already initialized");
            return;
        }

        try {
            super.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }

        DataBroker dataBroker = getDatabroker(getProviderContext());
        mdsalUtils = new MdsalUtils(dataBroker);
        assertNotNull("mdsalUtils should not be null", mdsalUtils);
        southboundUtils = new SouthboundUtils(mdsalUtils);
        getProperties();
        setup.set(true);
    }

    private ProviderContext getProviderContext() {
        ProviderContext providerContext = null;
        for (int i=0; i < 20; i++) {
            providerContext = getSession();
            if (providerContext != null) {
                break;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        assertNotNull("providercontext should not be null", providerContext);
        /* One more second to let the provider finish initialization */
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return providerContext;
    }

    private DataBroker getDatabroker(ProviderContext providerContext) {
        DataBroker dataBroker = providerContext.getSALService(DataBroker.class);
        assertNotNull("dataBroker should not be null", dataBroker);
        return dataBroker;
    }

    @Test
    public void testNetvirtSfcFeatureLoad() {
        assertTrue(true);
    }

    private AccessListsBuilder setAccessLists () {
        MatchesBuilder matchesBuilder = aclUtils.createMatches(new MatchesBuilder(), 80);
        ActionsBuilder actionsBuilder = aclUtils.createActions(new ActionsBuilder(), Boolean.TRUE);
        AccessListEntryBuilder accessListEntryBuilder = aclUtils.createAccessListEntryBuilder(
                new AccessListEntryBuilder(), "http", matchesBuilder, actionsBuilder);
        AccessListEntriesBuilder accessListEntriesBuilder = aclUtils.createAccessListEntries(
                new AccessListEntriesBuilder(), accessListEntryBuilder);
        AccessListBuilder accessListBuilder = aclUtils.createAccessList(new AccessListBuilder(),
                "http", accessListEntriesBuilder);
        AccessListsBuilder accessListsBuilder = aclUtils.createAccessLists(new AccessListsBuilder(),
                accessListBuilder);
        LOG.info("AccessLists: {}", accessListsBuilder.build());
        return accessListsBuilder;
    }

    @Test
    public void testAccessLists() {
        testModel(setAccessLists(), AccessLists.class);
    }

    private ClassifiersBuilder setClassifiers() {
        SffBuilder sffBuilder = classifierUtils.createSff(new SffBuilder(), "sffname");
        SffsBuilder sffsBuilder = classifierUtils.createSffs(new SffsBuilder(), sffBuilder);
        ClassifierBuilder classifierBuilder = classifierUtils.createClassifier(new ClassifierBuilder(),
                "classifierName", "aclName", sffsBuilder);
        ClassifiersBuilder classifiersBuilder = classifierUtils.createClassifiers(new ClassifiersBuilder(),
                classifierBuilder);
        LOG.info("Classifiers: {}", classifiersBuilder.build());
        return classifiersBuilder;
    }

    @Test
    public void testClassifiers() {
        testModel(setClassifiers(), Classifiers.class);
    }

    private SfcBuilder setSfc() {
        SfcBuilder sfcBuilder = sfcUtils.createSfc(new SfcBuilder(), "sfc");
        return sfcBuilder;
    }

    @Test
    public void testSfc() {
        testModel(setSfc(), Sfc.class);
    }

    private <T extends DataObject> void testModel(Builder<T> builder, Class<T> clazz) {
        InstanceIdentifier<T> path = InstanceIdentifier.create(clazz);
        assertTrue(mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, path, builder.build()));
        T result = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNotNull(clazz.getSimpleName() + " should not be null", result);
        assertTrue("Failed to remove " + clazz.getSimpleName(),
                mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, path));
        result = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNull(clazz.getSimpleName() + " should be null", result);
    }

    @Test
    public void testDoIt() throws InterruptedException {
        ConnectionInfo connectionInfo = southboundUtils.getConnectionInfo(addressStr, portStr);
        southboundUtils.connectOvsdbNode(connectionInfo);
        Thread.sleep(1000);
        southboundUtils.disconnectOvsdbNode(connectionInfo);
    }
}
