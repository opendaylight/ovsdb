package org.opendaylight.ovsdb.southbound.it;

import static org.ops4j.pax.exam.CoreOptions.maven;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;

@RunWith(PaxExam.class)
public class SouthboundIT extends AbstractMdsalTestBase {
    @Test
    public void PassTest() {
        Assert.assertNotNull("Missing ProviderContext session", getSession());
    }

    @Configuration
    public Option[] config() {
        return super.config();
    }

    @Override
    public String getModuleName() {
        return "southbound-impl";
    }

    @Override
    public String getInstanceName() {
        return "southbound-default";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
        .groupId("org.opendaylight.ovsdb")
        .artifactId("southbound-features")
        .classifier("features")
        .type("xml")
        .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-ovsdb-southbound-impl-ui";
    }
}
