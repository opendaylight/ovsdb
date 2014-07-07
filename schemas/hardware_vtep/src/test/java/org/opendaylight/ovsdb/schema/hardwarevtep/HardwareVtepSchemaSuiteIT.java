/*
 * Copyright (C) 2014 Matt Oswalt
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Matt Oswalt
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;

import java.util.HashMap;
import java.util.Map;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        MonitorTestCases.class,
        ManagerTestCases.class
})

public class HardwareVtepSchemaSuiteIT {
    // Keep this data between test runs
    static OvsdbClient ovsdbClient;
    static DatabaseSchema dbSchema;
    static UUID testManagerUuid;
    static UUID testLogicalSwitchUuid;
    static Map<String, Map<UUID, Row>> tableCache = new HashMap<String, Map<UUID, Row>>();

    public static OvsdbClient getOvsdbClient() {
        return ovsdbClient;
    }

    public static void setOvsdbClient(OvsdbClient ovsdbClient) {
        HardwareVtepSchemaSuiteIT.ovsdbClient = ovsdbClient;
    }

    public static UUID getTestManagerUuid() {
        return testManagerUuid;
    }

    public static void setTestManagerUuid(UUID testManagerUuid) {
        HardwareVtepSchemaSuiteIT.testManagerUuid = testManagerUuid;
    }

    public static UUID getTestLogicalSwitchUuid() {
        return testLogicalSwitchUuid;
    }

    public static void setTestLogicalSwitchUuid(UUID testLogicalSwitchUuid) {
        HardwareVtepSchemaSuiteIT.testLogicalSwitchUuid = testLogicalSwitchUuid;
    }

    public static Map<String, Map<UUID, Row>> getTableCache() {
        return tableCache;
    }
}
