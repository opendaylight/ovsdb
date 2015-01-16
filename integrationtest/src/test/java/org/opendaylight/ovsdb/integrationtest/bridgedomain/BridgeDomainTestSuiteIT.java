/*
 *
 *  Copyright (C) 2014 Red Hat, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 * /
 */
package org.opendaylight.ovsdb.integrationtest.bridgedomain;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    BridgeDomainConfigBridgeTestCases.class,
    BridgeDomainConfigPortTestCases.class,
    BridgeDomainConfigManagerTestCases.class,
    TearDown.class})

public class BridgeDomainTestSuiteIT {
    static PluginTestBase.TestObjects testObjects;

    public static PluginTestBase.TestObjects getTestObjects() {
        return testObjects;
    }

    public static void setTestObjects(PluginTestBase.TestObjects testObjects) {
        BridgeDomainTestSuiteIT.testObjects = testObjects;
    }

}
