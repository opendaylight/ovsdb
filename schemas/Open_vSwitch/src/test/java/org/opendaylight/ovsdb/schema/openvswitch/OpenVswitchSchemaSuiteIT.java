/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.schema.openvswitch;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;

import java.util.HashMap;
import java.util.Map;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        MonitorTestCases.class,
        BridgeTestCases.class,
        ControllerTestCases.class,
        PortAndInterfaceTestCases.class,
        NetflowTestCases.class,
        SflowTestCases.class,
        IpfixTestCases.class,
        TearDown.class
})
public class OpenVswitchSchemaSuiteIT {

    // Keep this data between test runs
    static OvsdbClient ovsdbClient;
    static UUID testBridgeUuid;
    static Map<String, Map<UUID, Row>> tableCache = new HashMap<String, Map<UUID, Row>>();

    public static OvsdbClient getOvsdbClient() {
        return ovsdbClient;
    }

    public static void setOvsdbClient(OvsdbClient ovsdbClient) {
        OpenVswitchSchemaSuiteIT.ovsdbClient = ovsdbClient;
    }

    public static UUID getTestBridgeUuid() {
        return testBridgeUuid;
    }

    public static void setTestBridgeUuid(UUID testBridgeUuid) {
        OpenVswitchSchemaSuiteIT.testBridgeUuid = testBridgeUuid;
    }

    public static Map<String, Map<UUID, Row>> getTableCache() {
        return tableCache;
    }
}
