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

import java.util.HashMap;
import java.util.Map;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ManagerTestCases.class
})

public class HardwareVtepSchemaSuiteIT {  //TODO: The below was all copied from the OVS equivalent. Need to make sure this all is updated if needed
    // Keep this data between test runs
    static OvsdbClient ovsdbClient;
    static UUID testManagerUuid;
    static Map<String, Map<UUID, Row>> tableCache = new HashMap<String, Map<UUID, Row>>();

    public static OvsdbClient getOvsdbClient() {
        return ovsdbClient;
    }

    public static void setOvsdbClient(OvsdbClient ovsdbClient) {
        HardwareVtepSchemaSuiteIT.ovsdbClient = ovsdbClient;
    }

    public static UUID getTestManagerUuid() {
        return testManagerUuid;  //needs changed
    }

    public static void setTestManagerUuid(UUID testManagerUuid) {
        HardwareVtepSchemaSuiteIT.testManagerUuid = testManagerUuid; //needs changed
    }

    public static Map<String, Map<UUID, Row>> getTableCache() {
        return tableCache;
    }
}
