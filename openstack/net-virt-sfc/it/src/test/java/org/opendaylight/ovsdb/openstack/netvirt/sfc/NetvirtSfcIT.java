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
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.AclUtils;
//import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.SfcUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.ClassifierUtils;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.utils.SfcUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
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
import org.opendaylight.yangtools.concepts.Builder;
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
            Option[] otherOptions = getOtherOptions();
            Option[] options = new Option[parentOptions.length + otherOptions.length];
            System.arraycopy(parentOptions, 0, options, 0, parentOptions.length);
            System.arraycopy(otherOptions, 0, options, parentOptions.length, otherOptions.length);
            return options;
        }

    private Option[] getOtherOptions() {
        return new Option[] {
                vmOption("-javaagent:../jars/org.jacoco.agent.jar=destfile=../../jacoco-it.exec"),
                keepRuntimeFolder()
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
    public void testAcl() {
        AccessListsBuilder accessListsBuilder = setAccessLists();
        InstanceIdentifier<AccessLists> path = InstanceIdentifier.create(AccessLists.class);
        assertTrue(mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, path, accessListsBuilder.build()));
        AccessLists accessLists = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNotNull("AccessLists should not be null", accessLists);
        assertTrue("Failed to remove AccessLists", mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, path));
        accessLists = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNull("AccessLists should be null", accessLists);
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
    public void testClassifier() {
        ClassifiersBuilder classifiersBuilder = setClassifiers();
        InstanceIdentifier<Classifiers> path = InstanceIdentifier.create(Classifiers.class);
        assertTrue(mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, path, classifiersBuilder.build()));
        Classifiers classifiers = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNotNull("Classifiers should not be null", classifiers);
        assertTrue("Failed to remove Classifiers", mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, path));
        classifiers = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNull("Classifiers should be null", classifiers);
    }

    @Test
    public void testSfc() {
        SfcBuilder sfcBuilder = sfcUtils.createSfc(new SfcBuilder(), "sfc");
        InstanceIdentifier<Sfc> path = InstanceIdentifier.create(Sfc.class);
        assertTrue(mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, path, sfcBuilder.build()));
        Sfc sfc = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNotNull("SfcUtils should not be null", sfc);
        assertTrue("Failed to remove Sfc", mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, path));
        sfc = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        assertNull("Sfc should be null", sfc);
    }
}
